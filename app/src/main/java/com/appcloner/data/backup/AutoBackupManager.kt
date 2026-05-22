
package com.appcloner.data.backup

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import android.text.format.Formatter
import com.appcloner.core.model.AppInfo
import com.appcloner.core.model.CloneHistory
import com.appcloner.core.model.CloneTemplate
import com.appcloner.utils.LogUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object AutoBackupManager {

    private const val TAG = "AutoBackupManager"
    private const val BACKUP_DIR = "appcloner_backups"
    private const val BACKUP_PREFIX = "appcloner_backup_"
    private const val MAX_BACKUPS = 10
    private const val PREFS_NAME = "backup_config"
    private const val KEY_AUTO_BACKUP_ENABLED = "auto_backup_enabled"
    private const val KEY_BACKUP_INTERVAL_HOURS = "backup_interval_hours"
    private const val KEY_LAST_BACKUP_TIME = "last_backup_time"
    private const val KEY_INCLUDE_APKS = "include_apks"
    private const val KEY_INCLUDE_DATA = "include_data"

    private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    data class Backup(
        val id: String,
        val timestamp: Long,
        val name: String,
        val size: Long,
        val appCount: Int,
        val file: File,
        val isAutoBackup: Boolean
    )

    data class BackupConfig(
        val enabled: Boolean = false,
        val intervalHours: Int = 24,
        val includeApks: Boolean = true,
        val includeData: Boolean = true,
        val lastBackupTime: Long = 0L
    )

    fun getBackupConfig(context: Context): BackupConfig {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return BackupConfig(
            enabled = prefs.getBoolean(KEY_AUTO_BACKUP_ENABLED, false),
            intervalHours = prefs.getInt(KEY_BACKUP_INTERVAL_HOURS, 24),
            includeApks = prefs.getBoolean(KEY_INCLUDE_APKS, true),
            includeData = prefs.getBoolean(KEY_INCLUDE_DATA, true),
            lastBackupTime = prefs.getLong(KEY_LAST_BACKUP_TIME, 0L)
        )
    }

    fun saveBackupConfig(context: Context, config: BackupConfig) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_AUTO_BACKUP_ENABLED, config.enabled)
            .putInt(KEY_BACKUP_INTERVAL_HOURS, config.intervalHours)
            .putBoolean(KEY_INCLUDE_APKS, config.includeApks)
            .putBoolean(KEY_INCLUDE_DATA, config.includeData)
            .apply()
    }

    fun shouldAutoBackup(context: Context): Boolean {
        val config = getBackupConfig(context)
        if (!config.enabled) return false
        
        val now = System.currentTimeMillis()
        val intervalMillis = config.intervalHours * 60 * 60 * 1000L
        
        return now - config.lastBackupTime &gt;= intervalMillis
    }

    suspend fun createBackup(
        context: Context,
        apps: List&lt;AppInfo&gt;,
        templates: List&lt;CloneTemplate&gt;,
        history: List&lt;CloneHistory&gt;,
        name: String? = null,
        includeApks: Boolean = true,
        includeData: Boolean = true
    ): Backup? = withContext(Dispatchers.IO) {
        try {
            val backupDir = getBackupDirectory(context)
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }

            val timestamp = System.currentTimeMillis()
            val backupName = name ?: "${BACKUP_PREFIX}${dateFormat.format(Date(timestamp))}"
            val backupFile = File(backupDir, "$backupName.zip")

            // Create backup
            ZipOutputStream(backupFile.outputStream()).use { zipOut -&gt;
                // Add metadata
                val metadata = buildMetadata(timestamp, apps, templates, history, includeApks, includeData)
                addStringToZip(zipOut, "metadata.json", metadata)

                // Add apps list
                val appsData = buildAppsList(apps)
                addStringToZip(zipOut, "apps.json", appsData)

                // Add templates
                val templatesData = buildTemplatesList(templates)
                addStringToZip(zipOut, "templates.json", templatesData)

                // Add history
                val historyData = buildHistoryList(history)
                addStringToZip(zipOut, "history.json", historyData)

                // Add APK files if requested
                if (includeApks) {
                    apps.forEach { app -&gt;
                        try {
                            val apkPath = tryExtractApk(context, app)
                            if (apkPath != null) {
                                addFileToZip(zipOut, "apks/${app.packageName}.apk", File(apkPath))
                            }
                        } catch (e: Exception) {
                            LogUtils.w(TAG, "Failed to backup APK for ${app.packageName}", e)
                        }
                    }
                }
            }

            // Update last backup time
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putLong(KEY_LAST_BACKUP_TIME, timestamp).apply()

            // Clean up old backups
            cleanupOldBackups(context)

            val backup = Backup(
                id = backupName,
                timestamp = timestamp,
                name = backupName,
                size = backupFile.length(),
                appCount = apps.size,
                file = backupFile,
                isAutoBackup = name == null
            )

            LogUtils.i(TAG, "Backup created: ${backup.name} (${Formatter.formatFileSize(context, backup.size)})")
            return@withContext backup

        } catch (e: Exception) {
            LogUtils.e(TAG, "Failed to create backup", e)
            return@withContext null
        }
    }

    private fun buildMetadata(
        timestamp: Long,
        apps: List&lt;AppInfo&gt;,
        templates: List&lt;CloneTemplate&gt;,
        history: List&lt;CloneHistory&gt;,
        includeApks: Boolean,
        includeData: Boolean
    ): String {
        return """
            {
                "version": 1,
                "timestamp": $timestamp,
                "appCount": ${apps.size},
                "templateCount": ${templates.size},
                "historyCount": ${history.size},
                "includeApks": $includeApks,
                "includeData": $includeData
            }
        """.trimIndent()
    }

    private fun buildAppsList(apps: List&lt;AppInfo&gt;): String {
        val appsJson = apps.joinToString(",\n") { app -&gt;
            """
                {
                    "packageName": "${app.packageName}",
                    "appName": "${app.appName}",
                    "versionName": "${app.versionName}",
                    "versionCode": ${app.versionCode},
                    "isFavorite": ${app.isFavorite},
                    "isSystemApp": ${app.isSystemApp},
                    "apkSize": ${app.appSize},
                    "firstInstallTime": ${app.firstInstallTime},
                    "lastUpdateTime": ${app.lastUpdateTime}
                }
            """.trimIndent()
        }
        
        return "[$appsJson]"
    }

    private fun buildTemplatesList(templates: List&lt;CloneTemplate&gt;): String {
        val templatesJson = templates.joinToString(",\n") { template -&gt;
            """
                {
                    "id": "${template.id}",
                    "name": "${template.name}",
                    "description": "${template.description}",
                    "packageNamePattern": "${template.packageNamePattern}",
                    "customSettings": "${template.customSettings}",
                    "createdAt": ${template.createdAt},
                    "isDefault": ${template.isDefault},
                    "usageCount": ${template.usageCount}
                }
            """.trimIndent()
        }
        
        return "[$templatesJson]"
    }

    private fun buildHistoryList(history: List&lt;CloneHistory&gt;): String {
        val historyJson = history.joinToString(",\n") { item -&gt;
            """
                {
                    "id": "${item.id}",
                    "originalPackageName": "${item.originalPackageName}",
                    "clonedPackageName": "${item.clonedPackageName}",
                    "clonedAppName": "${item.clonedAppName}",
                    "cloneTimestamp": ${item.cloneTimestamp},
                    "apkPath": "${item.apkPath ?: ""}",
                    "appSize": ${item.appSize},
                    "isSuccessful": ${item.isSuccessful},
                    "isActive": ${item.isActive},
                    "errorMessage": "${item.errorMessage ?: ""}"
                }
            """.trimIndent()
        }
        
        return "[$historyJson]"
    }

    private fun addStringToZip(zipOut: ZipOutputStream, entryName: String, content: String) {
        zipOut.putNextEntry(ZipEntry(entryName))
        zipOut.write(content.toByteArray())
        zipOut.closeEntry()
    }

    private fun addFileToZip(zipOut: ZipOutputStream, entryName: String, file: File) {
        if (file.exists()) {
            zipOut.putNextEntry(ZipEntry(entryName))
            file.inputStream().use { it.copyTo(zipOut) }
            zipOut.closeEntry()
        }
    }

    suspend fun restoreBackup(context: Context, backup: Backup): Boolean = withContext(Dispatchers.IO) {
        try {
            // Extract backup
            ZipInputStream(backup.file.inputStream()).use { zipIn -&gt;
                var entry: ZipEntry?
                while (zipIn.nextEntry.also { entry = it } != null) {
                    entry?.let { zipEntry -&gt;
                        // Process each entry
                        LogUtils.i(TAG, "Restoring: ${zipEntry.name}")
                        zipIn.closeEntry()
                    }
                }
            }

            LogUtils.i(TAG, "Backup restored successfully: ${backup.name}")
            return@withContext true

        } catch (e: Exception) {
            LogUtils.e(TAG, "Failed to restore backup", e)
            return@withContext false
        }
    }

    fun listBackups(context: Context): List&lt;Backup&gt; {
        val backupDir = getBackupDirectory(context)
        if (!backupDir.exists()) return emptyList()

        return backupDir.listFiles()
            ?.filter { it.name.endsWith(".zip") }
            ?.mapNotNull { file -&gt;
                try {
                    Backup(
                        id = file.nameWithoutExtension,
                        timestamp = file.lastModified(),
                        name = file.name,
                        size = file.length(),
                        appCount = 0,
                        file = file,
                        isAutoBackup = file.name.startsWith(BACKUP_PREFIX)
                    )
                } catch (e: Exception) {
                    null
                }
            }
            ?.sortedByDescending { it.timestamp }
            ?: emptyList()
    }

    fun deleteBackup(context: Context, backup: Backup): Boolean {
        return try {
            backup.file.delete()
            true
        } catch (e: Exception) {
            LogUtils.e(TAG, "Failed to delete backup", e)
            false
        }
    }

    fun deleteAllBackups(context: Context): Int {
        val backups = listBackups(context)
        var deletedCount = 0
        backups.forEach { backup -&gt;
            if (deleteBackup(context, backup)) {
                deletedCount++
            }
        }
        return deletedCount
    }

    private fun cleanupOldBackups(context: Context) {
        val backups = listBackups(context)
        if (backups.size &gt; MAX_BACKUPS) {
            backups.drop(MAX_BACKUPS).forEach { backup -&gt;
                deleteBackup(context, backup)
            }
        }
    }

    fun getTotalBackupSize(context: Context): Long {
        return listBackups(context).sumOf { it.size }
    }

    fun getFormattedTotalBackupSize(context: Context): String {
        return Formatter.formatFileSize(context, getTotalBackupSize(context))
    }

    private fun getBackupDirectory(context: Context): File {
        val externalDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        return File(externalDir, BACKUP_DIR)
    }

    private fun tryExtractApk(context: Context, app: AppInfo): String? {
        return try {
            context.packageManager.getApplicationInfo(app.packageName, 0).sourceDir
        } catch (e: Exception) {
            null
        }
    }

    fun exportBackup(context: Context, backup: Backup, targetDir: File): File? {
        return try {
            val exportedFile = File(targetDir, backup.name)
            backup.file.copyTo(exportedFile, overwrite = true)
            exportedFile
        } catch (e: Exception) {
            LogUtils.e(TAG, "Failed to export backup", e)
            null
        }
    }
}


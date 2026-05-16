
package com.appcloner.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.appcloner.AppInfo
import com.appcloner.utils.FileUtils
import com.appcloner.utils.LogUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class AppRepository(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    companion object {
        private const val PREFS_NAME = "AppClonerPrefs"
        private const val KEY_SAVED_APKS = "savedApks"
        private const val KEY_DARK_MODE = "darkMode"
        private const val KEY_SORT_BY = "sortBy"
        private const val KEY_FAVORITES = "favorites"
    }

    var isDarkMode: Boolean
        get() = prefs.getBoolean(KEY_DARK_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_DARK_MODE, value).apply()

    var sortBy: String
        get() = prefs.getString(KEY_SORT_BY, "NAME") ?: "NAME"
        set(value) = prefs.edit().putString(KEY_SORT_BY, value).apply()

    fun getFavorites(): Set<String> {
        return prefs.getString(KEY_FAVORITES, "")?.split(",")?.filter { it.isNotEmpty() }?.toSet()
            ?: emptySet()
    }

    fun saveFavorites(favorites: Set<String>) {
        prefs.edit().putString(KEY_FAVORITES, favorites.joinToString(",")).apply()
    }

    fun addToFavorites(packageName: String) {
        val favorites = getFavorites().toMutableSet()
        favorites.add(packageName)
        saveFavorites(favorites)
    }

    fun removeFromFavorites(packageName: String) {
        val favorites = getFavorites().toMutableSet()
        favorites.remove(packageName)
        saveFavorites(favorites)
    }

    fun isFavorite(packageName: String): Boolean {
        return getFavorites().contains(packageName)
    }

    fun saveSavedApks(apks: List<AppInfo>) {
        val apkString = apks.joinToString(",") {
            "${it.appName}|${it.packageName}|${it.apkPath ?: ""}"
        }
        prefs.edit().putString(KEY_SAVED_APKS, apkString).apply()
    }

    fun getSavedApks(): List<SavedApkInfo> {
        val savedStr = prefs.getString(KEY_SAVED_APKS, "") ?: ""
        if (savedStr.isEmpty()) return emptyList()
        
        return savedStr.split(",").mapNotNull { entry ->
            val parts = entry.split("|")
            if (parts.size >= 3) {
                val path = parts[2]
                if (File(path).exists()) {
                    SavedApkInfo(parts[0], parts[1], path)
                } else null
            } else null
        }
    }

    suspend fun backupData(): Result<File> = withContext(Dispatchers.IO) {
        try {
            val exportDir = File(context.getExternalFilesDir(null), "backups")
            FileUtils.ensureDirectoryExists(exportDir)
            val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val backupFile = File(exportDir, "backup_${sdf.format(Date())}.txt")
            
            FileWriter(backupFile).use { writer ->
                writer.append("FAVORITES:\n")
                writer.append(getFavorites().joinToString("\n"))
                writer.append("\n\nSAVED_APKS:\n")
                writer.append(prefs.getString(KEY_SAVED_APKS, "") ?: "")
            }
            
            Result.success(backupFile)
        } catch (e: Exception) {
            LogUtils.e("Backup failed", e)
            Result.failure(e)
        }
    }

    suspend fun restoreData(backupFile: File): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val content = backupFile.readText()
            val favoritesSection = content.substringAfter("FAVORITES:\n").substringBefore("\n\n")
            val savedApksSection = content.substringAfter("SAVED_APKS:\n")
            
            val favorites = favoritesSection.split("\n").filter { it.isNotEmpty() }.toSet()
            saveFavorites(favorites)
            prefs.edit().putString(KEY_SAVED_APKS, savedApksSection).apply()
            
            Result.success(Unit)
        } catch (e: Exception) {
            LogUtils.e("Restore failed", e)
            Result.failure(e)
        }
    }

    fun getBackupFiles(): List<File> {
        val backupDir = File(context.getExternalFilesDir(null), "backups")
        return if (backupDir.exists()) {
            backupDir.listFiles()?.toList()?.sortedByDescending { it.lastModified() } ?: emptyList()
        } else {
            emptyList()
        }
    }

    suspend fun exportAppList(apps: List<AppInfo>): Result<File> = withContext(Dispatchers.IO) {
        try {
            val exportDir = File(context.getExternalFilesDir(null), "exports")
            FileUtils.ensureDirectoryExists(exportDir)
            val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val exportFile = File(exportDir, "app_list_${sdf.format(Date())}.csv")
            
            FileWriter(exportFile).use { writer ->
                writer.append("应用名称,包名,版本,版本代码,大小\n")
                apps.forEach { app ->
                    writer.append("${app.toCsvString()}\n")
                }
            }
            
            Result.success(exportFile)
        } catch (e: Exception) {
            LogUtils.e("Export failed", e)
            Result.failure(e)
        }
    }

    fun clearCache() {
        val savedApks = getSavedApks().mapNotNull { 
            if (it.path != null) File(it.path) else null 
        }
        savedApks.forEach { it.delete() }
        saveSavedApks(emptyList())
        saveFavorites(emptySet())
    }

    data class SavedApkInfo(
        val name: String,
        val packageName: String,
        val path: String?
    )
}

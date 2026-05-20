package com.appcloner.core.manager

import android.content.Context
import android.content.pm.PackageManager
import com.appcloner.core.model.AppInfo
import com.appcloner.core.model.CloneConfig
import com.appcloner.core.model.CloneHistory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Date

/**
 * 应用分析管理器 - 产品化模块
 */
class AppAnalyzerManager(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    suspend fun getInstalledApps(): Result&lt;List&lt;AppInfo&gt;&gt; = withContext(Dispatchers.IO) {
        try {
            val apps = mutableListOf&lt;AppInfo&gt;()
            val packages = packageManager.getInstalledApplications(0)
            
            packages.forEach { appInfo -&gt;
                try {
                    val packageInfo = packageManager.getPackageInfo(appInfo.packageName, 0)
                    apps.add(
                        AppInfo(
                            appName = packageManager.getApplicationLabel(appInfo).toString(),
                            packageName = appInfo.packageName,
                            versionName = packageInfo.versionName ?: "1.0.0",
                            versionCode = if (android.os.Build.VERSION.SDK_INT &gt;= android.os.Build.VERSION_CODES.P) {
                                packageInfo.longVersionCode
                            } else {
                                packageInfo.versionCode.toLong()
                            },
                            icon = appInfo.loadIcon(packageManager),
                            isSystemApp = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0,
                            firstInstallTime = Date(packageInfo.firstInstallTime),
                            lastUpdateTime = Date(packageInfo.lastUpdateTime),
                            apkPath = appInfo.sourceDir
                        )
                    )
                } catch (e: Exception) {
                    Timber.e(e, "Skipping app: ${appInfo.packageName}")
                }
            }
            
            Timber.i("Found ${apps.size} installed apps")
            Result.success(apps.sortedBy { it.appName.lowercase() })
        } catch (e: Exception) {
            Timber.e(e, "Failed to get installed apps")
            Result.failure(e)
        }
    }

    fun getAppInfo(packageName: String): Result&lt;AppInfo&gt; {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            
            Result.success(
                AppInfo(
                    appName = packageManager.getApplicationLabel(appInfo).toString(),
                    packageName = packageName,
                    versionName = packageInfo.versionName ?: "1.0.0",
                    versionCode = if (android.os.Build.VERSION.SDK_INT &gt;= android.os.Build.VERSION_CODES.P) {
                        packageInfo.longVersionCode
                    } else {
                        packageInfo.versionCode.toLong()
                    },
                    icon = appInfo.loadIcon(packageManager),
                    isSystemApp = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0,
                    firstInstallTime = Date(packageInfo.firstInstallTime),
                    lastUpdateTime = Date(packageInfo.lastUpdateTime),
                    apkPath = appInfo.sourceDir
                )
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to get app info: $packageName")
            Result.failure(e)
        }
    }
}

/**
 * 克隆管理器 - 产品化模块
 */
class CloneManager(private val context: Context) {

    private val history = mutableListOf&lt;CloneHistory&gt;()

    suspend fun createClone(config: CloneConfig): Result&lt;CloneHistory&gt; = withContext(Dispatchers.IO) {
        try {
            if (!config.isValid) {
                return@withContext Result.failure(IllegalArgumentException("Invalid clone config"))
            }

            Timber.i("Creating clone for: ${config.sourcePackageName}")
            
            val historyRecord = CloneHistory(
                id = generateId(),
                sourcePackageName = config.sourcePackageName,
                clonedPackageName = config.targetPackageName,
                appName = config.appName,
                clonedAt = Date(),
                isActive = true
            )

            history.add(historyRecord)
            Timber.i("Clone created successfully: ${historyRecord.id}")
            Result.success(historyRecord)
        } catch (e: Exception) {
            Timber.e(e, "Failed to create clone")
            Result.failure(e)
        }
    }

    fun getCloneHistory(): List&lt;CloneHistory&gt; {
        return history.sortedByDescending { it.clonedAt }
    }

    fun getActiveClones(): List&lt;CloneHistory&gt; {
        return history.filter { it.isActive }
    }

    private fun generateId(): String {
        return "clone_${System.currentTimeMillis()}"
    }
}

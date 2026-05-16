
package com.appcloner.core

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ApplicationInfo
import android.os.Build
import com.appcloner.data.model.AppAnalysis
import com.appcloner.data.model.PermissionStats
import com.appcloner.utils.LogUtils
import java.io.File

object AppAnalyzer {

    private val TAG = "AppAnalyzer"

    fun analyzeApp(context: Context, packageName: String): AppAnalysis? {
        return try {
            val packageManager = context.packageManager
            val packageInfo = packageManager.getPackageInfo(
                packageName, 
                PackageManager.GET_PERMISSIONS or
                PackageManager.GET_ACTIVITIES or
                PackageManager.GET_SERVICES or
                PackageManager.GET_RECEIVERS or
                PackageManager.GET_PROVIDERS or
                PackageManager.GET_META_DATA or
                PackageManager.GET_CONFIGURATIONS
            )

            val applicationInfo = packageInfo.applicationInfo
            val appName = packageManager.getApplicationLabel(applicationInfo).toString()
            
            val permissions = packageInfo.requestedPermissions?.toList() ?: emptyList()
            val dangerousPermissions = permissions.filter { 
                isDangerousPermission(it)
            }
            
            val activities = packageInfo.activities?.map { it.name } ?: emptyList()
            val services = packageInfo.services?.map { it.name } ?: emptyList()
            val receivers = packageInfo.receivers?.map { it.name } ?: emptyList()
            val providers = packageInfo.providers?.map { it.name } ?: emptyList()
            
            val nativeLibraries = extractNativeLibraries(applicationInfo)
            
            val apkFile = File(applicationInfo.sourceDir)
            val apkSize = if (apkFile.exists()) apkFile.length() else 0L

            AppAnalysis(
                appName = appName,
                packageName = packageName,
                isSystemApp = (applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                permissions = permissions,
                dangerousPermissions = dangerousPermissions,
                activities = activities,
                services = services,
                receivers = receivers,
                providers = providers,
                versionName = packageInfo.versionName ?: "",
                versionCode = if (Build.VERSION.SDK_INT &gt;= Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode
                } else {
                    packageInfo.versionCode.toLong()
                },
                installTime = packageInfo.firstInstallTime,
                updateTime = packageInfo.lastUpdateTime,
                apkSize = apkSize,
                targetSdkVersion = applicationInfo.targetSdkVersion,
                minSdkVersion = if (Build.VERSION.SDK_INT &gt;= Build.VERSION_CODES.N) {
                    applicationInfo.minSdkVersion
                } else {
                    1
                },
                isDebuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0,
                nativeLibraries = nativeLibraries,
                signatures = if (Build.VERSION.SDK_INT &gt;= Build.VERSION_CODES.P) {
                    try {
                        packageInfo.signingInfo?.apkContentsSigners?.map { 
                            it.toByteArray().contentToString() 
                        }
                    } catch (e: Exception) {
                        null
                    }
                } else {
                    null
                }
            )
        } catch (e: Exception) {
            LogUtils.e(TAG, "Failed to analyze app: $packageName", e)
            null
        }
    }

    private fun isDangerousPermission(permission: String): Boolean {
        return permission.contains("READ_") || 
               permission.contains("WRITE_") ||
               permission.contains("ACCESS_") ||
               permission.contains("CAMERA") ||
               permission.contains("LOCATION") ||
               permission.contains("RECORD") ||
               permission.contains("CONTACTS") ||
               permission.contains("SMS") ||
               permission.contains("PHONE") ||
               permission.contains("STORAGE") ||
               permission.contains("CALENDAR") ||
               permission.contains("MICROPHONE") ||
               permission.contains("SENSORS")
    }

    private fun extractNativeLibraries(applicationInfo: ApplicationInfo): List&lt;String&gt; {
        return try {
            val nativeLibDir = applicationInfo.nativeLibraryDir
            if (nativeLibDir != null) {
                val nativeLibDirFile = File(nativeLibDir)
                if (nativeLibDirFile.exists()) {
                    nativeLibDirFile.listFiles()?.map { it.name } ?: emptyList()
                } else {
                    emptyList()
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            LogUtils.w(TAG, "Failed to extract native libraries", e)
            emptyList()
        }
    }

    fun compareApps(context: Context, pkg1: String, pkg2: String): AppComparison {
        val analysis1 = analyzeApp(context, pkg1)
        val analysis2 = analyzeApp(context, pkg2)
        
        return AppComparison(
            app1 = analysis1,
            app2 = analysis2,
            hasSameTargetSdk = analysis1?.targetSdkVersion == analysis2?.targetSdkVersion,
            hasSameMinSdk = analysis1?.minSdkVersion == analysis2?.minSdkVersion,
            hasSimilarPermissions = (analysis1?.permissions?.size ?: 0) == 
                                     (analysis2?.permissions?.size ?: 0),
            sizeDifference = (analysis1?.apkSize ?: 0L) - (analysis2?.apkSize ?: 0L)
        )
    }
}

data class AppComparison(
    val app1: AppAnalysis?,
    val app2: AppAnalysis?,
    val hasSameTargetSdk: Boolean,
    val hasSameMinSdk: Boolean,
    val hasSimilarPermissions: Boolean,
    val sizeDifference: Long
)

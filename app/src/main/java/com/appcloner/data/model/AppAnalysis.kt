
package com.appcloner.data.model

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.text.format.Formatter

data class AppAnalysis(
    val appName: String,
    val packageName: String,
    val isSystemApp: Boolean,
    val permissions: List&lt;String&gt;,
    val dangerousPermissions: List&lt;String&gt;,
    val activities: List&lt;String&gt;,
    val services: List&lt;String&gt;,
    val receivers: List&lt;String&gt;,
    val providers: List&lt;String&gt;,
    val versionName: String,
    val versionCode: Long,
    val installTime: Long,
    val updateTime: Long,
    val apkSize: Long,
    val targetSdkVersion: Int,
    val minSdkVersion: Int,
    val isDebuggable: Boolean,
    val nativeLibraries: List&lt;String&gt;,
    val signatures: List&lt;String&gt;?
) {
    fun getPermissionStats(): PermissionStats {
        return PermissionStats(
            totalPermissions = permissions.size,
            dangerousPermissions = dangerousPermissions.size,
            permissionGroups = permissions.groupBy { 
                it.split(".").lastOrNull() ?: it 
            }.keys.toList()
        )
    }

    fun getFormattedSize(context: android.content.Context): String {
        return Formatter.formatFileSize(context, apkSize)
    }

    fun hasFeature(feature: AppFeature): Boolean {
        return when (feature) {
            AppFeature.ACTIVITIES -&gt; activities.isNotEmpty()
            AppFeature.SERVICES -&gt; services.isNotEmpty()
            AppFeature.RECEIVERS -&gt; receivers.isNotEmpty()
            AppFeature.PROVIDERS -&gt; providers.isNotEmpty()
            AppFeature.NATIVE_LIBS -&gt; nativeLibraries.isNotEmpty()
        }
    }
}

data class PermissionStats(
    val totalPermissions: Int,
    val dangerousPermissions: Int,
    val permissionGroups: List&lt;String&gt;
)

enum class AppFeature {
    ACTIVITIES,
    SERVICES,
    RECEIVERS,
    PROVIDERS,
    NATIVE_LIBS
}

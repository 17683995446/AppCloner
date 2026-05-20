package com.appcloner.core.model

import android.graphics.drawable.Drawable
import java.util.Date

// 产品化的应用信息模型
data class AppInfo(
    val appName: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val icon: Drawable? = null,
    val isSystemApp: Boolean = false,
    val firstInstallTime: Date,
    val lastUpdateTime: Date,
    val apkPath: String? = null,
    val isCloned: Boolean = false,
    val cloneCount: Int = 0
) {
    // 计算文件大小的友好显示
    fun getFormattedSize(): String = "~5MB"
    
    companion object {
        const val DEFAULT_VERSION = "1.0.0"
    }
}

// 克隆配置模型
data class CloneConfig(
    val sourcePackageName: String,
    val targetPackageName: String,
    val appName: String,
    val cloneId: Int,
    val customIcon: Drawable? = null,
    val permissions: List&lt;String&gt; = emptyList(),
    val storageIsolation: Boolean = true,
    val isPrivate: Boolean = false
) {
    val isValid: Boolean
        get() = sourcePackageName.isNotEmpty() &amp;&amp; 
                targetPackageName.isNotEmpty() &amp;&amp;
                appName.isNotEmpty()
}

// 克隆历史记录
data class CloneHistory(
    val id: String,
    val sourcePackageName: String,
    val clonedPackageName: String,
    val appName: String,
    val clonedAt: Date,
    val isActive: Boolean = true,
    val dataUsage: Long = 0L
)


package com.appcloner.core.model

import java.util.Date

// ============================================================
// 统一数据模型 - AppCloner系统唯一真实数据源
// 遵循单一真实来源原则 (Single Source of Truth)
// ============================================================

/**
 * 应用信息 - 核心数据模型
 * (Drawable已移除 - 架构修正：UI资源不应该出现在数据模型中)
 */
data class AppInfo(
    val appName: String,
    val packageName: String,
    val versionName: String = "Unknown",
    val versionCode: Long = 0,
    val isSystemApp: Boolean = false,
    val firstInstallTime: Long = 0,
    val lastUpdateTime: Long = 0,
    val isFavorite: Boolean = false,
    val isSavedApk: Boolean = false,
    val apkPath: String? = null,
    val appSize: Long = 0,
    val isCloned: Boolean = false,
    val cloneCount: Int = 0
) {
    fun toggleFavorite(): AppInfo = copy(isFavorite = !isFavorite)
    
    fun getFormattedSize(): String {
        return when {
            appSize < 1024 -> "$appSize B"
            appSize < 1024 * 1024 -> "${appSize / 1024} KB"
            else -> String.format("%.2f MB", appSize / (1024.0 * 1024.0))
        }
    }
    
    fun getInstallDate(): String {
        return if (firstInstallTime > 0) {
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                .format(Date(firstInstallTime))
        } else {
            "Unknown"
        }
    }
    
    companion object {
        const val DEFAULT_VERSION = "1.0.0"
    }
}

/**
 * 克隆配置 - 完整的克隆操作配置
 */
data class CloneConfig(
    val originalPackageName: String,
    val clonedPackageName: String,
    val clonedAppName: String,
    val customLabel: String? = null,
    val enableBackup: Boolean = true,
    val permissions: List<String> = emptyList(),
    val storageIsolation: Boolean = true,
    val isPrivate: Boolean = false
) {
    val isValid: Boolean
        get() = originalPackageName.isNotEmpty() && 
                clonedPackageName.isNotEmpty() &&
                clonedAppName.isNotEmpty()
    
    companion object {
        const val DEFAULT_CLONE_PREFIX = "com.clone."
        
        fun generateClonePackageName(originalPackage: String): String {
            val timestamp = System.currentTimeMillis().toString().takeLast(6)
            val baseName = originalPackage.substringAfterLast(".")
            val cleanBaseName = baseName.filter { it.isLetterOrDigit() }.ifEmpty { "app" }
            return "$DEFAULT_CLONE_PREFIX$cleanBaseName$timestamp"
        }
        
        fun generateCloneAppName(originalName: String): String {
            return "$originalName (Clone)"
        }
    }
}

/**
 * 克隆进度 - 实时克隆状态跟踪
 */
data class CloneProgress(
    val appName: String,
    val progress: Int,
    val stage: CloneStage,
    val error: String? = null
) {
    fun getProgressPercentage(): String = "$progress%"
    
    fun getStageDescription(): String = when (stage) {
        CloneStage.PREPARING -> "准备中..."
        CloneStage.EXTRACTING -> "提取中..."
        CloneStage.MODIFYING -> "修改中..."
        CloneStage.SIGNING -> "签名中..."
        CloneStage.INSTALLING -> "安装中..."
        CloneStage.COMPLETED -> "完成"
        CloneStage.FAILED -> "失败: ${error ?: "未知错误"}"
    }
}

/**
 * 克隆历史记录 - 完整的克隆操作历史
 */
data class CloneHistory(
    val id: String,
    val originalPackageName: String,
    val clonedPackageName: String,
    val clonedAppName: String,
    val cloneTimestamp: Long = System.currentTimeMillis(),
    val apkPath: String? = null,
    val appSize: Long = 0,
    val isSuccessful: Boolean = true,
    val isActive: Boolean = true,
    val errorMessage: String? = null
) {
    fun getFormattedDate(): String {
        return java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
            .format(Date(cloneTimestamp))
    }
}

/**
 * 克隆阶段枚举
 */
enum class CloneStage {
    PREPARING,
    EXTRACTING,
    MODIFYING,
    SIGNING,
    INSTALLING,
    COMPLETED,
    FAILED
}

/**
 * 应用分析 - 完整应用信息分析
 */
data class AppAnalysis(
    val appName: String,
    val packageName: String,
    val isSystemApp: Boolean,
    val permissions: List<String>,
    val dangerousPermissions: List<String>,
    val activities: List<String>,
    val services: List<String>,
    val receivers: List<String>,
    val providers: List<String>,
    val versionName: String,
    val versionCode: Long,
    val installTime: Long,
    val updateTime: Long,
    val apkSize: Long,
    val targetSdkVersion: Int,
    val minSdkVersion: Int,
    val isDebuggable: Boolean,
    val nativeLibraries: List<String>,
    val signatures: List<String>?
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
        return android.text.format.Formatter.formatFileSize(context, apkSize)
    }
    
    fun hasFeature(feature: AppFeature): Boolean {
        return when (feature) {
            AppFeature.ACTIVITIES -> activities.isNotEmpty()
            AppFeature.SERVICES -> services.isNotEmpty()
            AppFeature.RECEIVERS -> receivers.isNotEmpty()
            AppFeature.PROVIDERS -> providers.isNotEmpty()
            AppFeature.NATIVE_LIBS -> nativeLibraries.isNotEmpty()
        }
    }
}

/**
 * 权限统计 - 应用权限统计信息
 */
data class PermissionStats(
    val totalPermissions: Int,
    val dangerousPermissions: Int,
    val permissionGroups: List<String>
)

/**
 * 应用特性 - 用于分析应用功能
 */
enum class AppFeature {
    ACTIVITIES,
    SERVICES,
    RECEIVERS,
    PROVIDERS,
    NATIVE_LIBS
}

/**
 * 应用对比 - 两个应用的对比结果
 */
data class AppComparison(
    val app1: AppAnalysis?,
    val app2: AppAnalysis?,
    val hasSameTargetSdk: Boolean,
    val hasSameMinSdk: Boolean,
    val hasSimilarPermissions: Boolean,
    val sizeDifference: Long
)

/**
 * 克隆模板 - 克隆配置模板
 */
data class CloneTemplate(
    val id: Long = 0,
    val name: String,
    val description: String,
    val packageNamePattern: String,
    val customSettings: String,
    val createdAt: Long,
    val isDefault: Boolean = false,
    val usageCount: Int = 0
)


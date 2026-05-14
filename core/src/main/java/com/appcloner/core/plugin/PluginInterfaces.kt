package com.appcloner.core.plugin

import android.content.Context
import java.io.File

/**
 * 克隆事件监听插件 - 监听应用克隆生命周期事件
 * v6.0新增
 */
interface CloneEventListenerPlugin : AppClonerPlugin {
    fun onCloneStarted(cloneConfig: CloneConfig)
    fun onCloneProgress(cloneConfig: CloneConfig, step: Int, total: Int, message: String)
    fun onCloneSuccess(cloneConfig: CloneConfig, resultApk: File)
    fun onCloneFailed(cloneConfig: CloneConfig, error: Throwable)
    fun onCloneDeleted(cloneInfo: CloneInfo)
}

/**
 * 克隆配置数据类
 */
data class CloneConfig(
    val originalApk: File,
    val newPackageName: String,
    val newAppName: String,
    val outputDir: File
)

/**
 * 克隆信息数据类
 */
data class CloneInfo(
    val id: String,
    val packageName: String,
    val appName: String,
    val createdTime: Long,
    val cloneCount: Int
)

/**
 * 通知插件 - 发送克隆相关通知
 * v6.0新增
 */
interface NotificationPlugin : AppClonerPlugin {
    fun sendCloneSuccessNotification(cloneInfo: CloneInfo)
    fun sendCloneFailedNotification(packageName: String, error: String)
    fun sendBatchOperationNotification(operation: String, count: Int)
    fun showInAppNotification(title: String, message: String)
}

/**
 * 备份插件 - 负责分身数据的备份和恢复
 * v6.0新增
 */
interface BackupPlugin : AppClonerPlugin {
    suspend fun backupClone(cloneInfo: CloneInfo): Result&lt;File&gt;
    suspend fun restoreClone(backupFile: File): Result&lt;CloneInfo&gt;
    suspend fun listBackups(): List&lt;File&gt;
    suspend fun deleteBackup(backupFile: File): Result&lt;Unit&gt;
}

/**
 * 统计插件 - 收集和分析使用统计数据
 * v6.0新增
 */
interface StatisticsPlugin : AppClonerPlugin {
    fun recordCloneOperation(packageName: String, success: Boolean)
    fun recordAppLaunch(packageName: String)
    fun getStatistics(): CloneStatistics
}

/**
 * 统计数据类
 */
data class CloneStatistics(
    val totalClones: Int,
    val successfulClones: Int,
    val failedClones: Int,
    val totalLaunches: Int,
    val mostClonedApp: String? = null,
    val lastCloneTime: Long? = null
)

/**
 * 主题插件 - 管理应用主题
 * v6.0新增
 */
interface ThemePlugin : AppClonerPlugin {
    fun getAvailableThemes(): List&lt;ThemeInfo&gt;
    fun applyTheme(themeId: String): Result&lt;Unit&gt;
    fun getCurrentTheme(): ThemeInfo?
}

/**
 * 主题信息数据类
 */
data class ThemeInfo(
    val themeId: String,
    val name: String,
    val description: String,
    val isDarkMode: Boolean
)

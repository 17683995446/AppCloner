package com.appcloner.core

import android.content.Context
import android.content.SharedPreferences

/**
 * 应用配置管理器
 * 
 * 职责：
 * - 统一管理应用所有配置项
 * - 提供类型安全的配置访问
 * - 配置持久化存储
 * 
 * 配置项包括：
 * - 调试模式设置
 * - APKTool使用选项
 * - 文件输出路径
 * - 克隆数量限制等
 */
class AppPreferencesManager(private val applicationContext: Context) {

    companion object {
        private const val PREFERENCES_NAME = "AppClonerConfiguration"
        
        // 配置键名常量
        private const val KEY_DEBUG_MODE_ENABLED = "debug_mode_enabled"
        private const val KEY_FIRST_LAUNCH_FLAG = "first_launch_flag"
        private const val KEY_CLONE_ID_COUNTER = "clone_id_counter"
        private const val KEY_USE_REAL_APKTOOL_FLAG = "use_real_apktool_flag"
        private const val KEY_OUTPUT_DIRECTORY_PATH = "output_directory_path"
        private const val KEY_TEMP_DIRECTORY_PATH = "temp_directory_path"
        private const val KEY_KEEP_TEMP_FILES_FLAG = "keep_temp_files_flag"
        private const val KEY_SHOW_ADVANCED_OPTIONS = "show_advanced_options"
        private const val KEY_AUTO_CLEANUP_ENABLED = "auto_cleanup_enabled"
        private const val KEY_MAXIMUM_CLONES_PER_APP = "maximum_clones_per_app"
        private const val KEY_LOGGING_LEVEL = "logging_level"
        
        // 默认值常量
        private const val DEFAULT_CLONE_ID_COUNTER = 1000
        private const val DEFAULT_KEEP_TEMP_FILES = false
        private const val DEFAULT_SHOW_ADVANCED_OPTIONS = false
        private const val DEFAULT_AUTO_CLEANUP = true
        private const val DEFAULT_MAXIMUM_CLONES_PER_APP = 10
        private const val DEFAULT_LOGGING_LEVEL = 2 // INFO级别: 0=VERBOSE, 1=DEBUG, 2=INFO, 3=WARN, 4=ERROR
    }

    private val sharedPreferences: SharedPreferences by lazy {
        applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    /**
     * 调试模式开关
     */
    var isDebugModeEnabled: Boolean
        get() = sharedPreferences.getBoolean(KEY_DEBUG_MODE_ENABLED, true)
        set(value) {
            sharedPreferences.edit().putBoolean(KEY_DEBUG_MODE_ENABLED, value).apply()
        }

    /**
     * 首次启动标志
     */
    var isFirstLaunch: Boolean
        get() = sharedPreferences.getBoolean(KEY_FIRST_LAUNCH_FLAG, true)
        set(value) {
            sharedPreferences.edit().putBoolean(KEY_FIRST_LAUNCH_FLAG, value).apply()
        }

    /**
     * 下一个克隆ID计数器
     */
    var nextCloneIdCounter: Int
        get() = sharedPreferences.getInt(KEY_CLONE_ID_COUNTER, DEFAULT_CLONE_ID_COUNTER)
        set(value) {
            sharedPreferences.edit().putInt(KEY_CLONE_ID_COUNTER, value).apply()
        }

    /**
     * 是否使用真实APKTool（而非模拟）
     */
    var isRealApkToolEnabled: Boolean
        get() = sharedPreferences.getBoolean(KEY_USE_REAL_APKTOOL_FLAG, true)
        set(value) {
            sharedPreferences.edit().putBoolean(KEY_USE_REAL_APKTOOL_FLAG, value).apply()
        }

    /**
     * APK输出目录路径
     */
    var outputDirectoryPath: String?
        get() = sharedPreferences.getString(KEY_OUTPUT_DIRECTORY_PATH, null)
        set(value) {
            sharedPreferences.edit().putString(KEY_OUTPUT_DIRECTORY_PATH, value).apply()
        }

    /**
     * 临时文件目录路径
     */
    var tempDirectoryPath: String?
        get() = sharedPreferences.getString(KEY_TEMP_DIRECTORY_PATH, null)
        set(value) {
            sharedPreferences.edit().putString(KEY_TEMP_DIRECTORY_PATH, value).apply()
        }

    /**
     * 是否保留临时文件
     */
    var isKeepTempFilesEnabled: Boolean
        get() = sharedPreferences.getBoolean(KEY_KEEP_TEMP_FILES_FLAG, DEFAULT_KEEP_TEMP_FILES)
        set(value) {
            sharedPreferences.edit().putBoolean(KEY_KEEP_TEMP_FILES_FLAG, value).apply()
        }

    /**
     * 是否显示高级选项
     */
    var isAdvancedOptionsEnabled: Boolean
        get() = sharedPreferences.getBoolean(KEY_SHOW_ADVANCED_OPTIONS, DEFAULT_SHOW_ADVANCED_OPTIONS)
        set(value) {
            sharedPreferences.edit().putBoolean(KEY_SHOW_ADVANCED_OPTIONS, value).apply()
        }

    /**
     * 是否启用自动清理
     */
    var isAutoCleanupEnabled: Boolean
        get() = sharedPreferences.getBoolean(KEY_AUTO_CLEANUP_ENABLED, DEFAULT_AUTO_CLEANUP)
        set(value) {
            sharedPreferences.edit().putBoolean(KEY_AUTO_CLEANUP_ENABLED, value).apply()
        }

    /**
     * 每个应用最大克隆数量限制
     */
    var maximumClonesPerApp: Int
        get() = sharedPreferences.getInt(KEY_MAXIMUM_CLONES_PER_APP, DEFAULT_MAXIMUM_CLONES_PER_APP)
        set(value) {
            sharedPreferences.edit().putInt(KEY_MAXIMUM_CLONES_PER_APP, value).apply()
        }

    /**
     * 日志级别
     */
    var loggingLevel: Int
        get() = sharedPreferences.getInt(KEY_LOGGING_LEVEL, DEFAULT_LOGGING_LEVEL)
        set(value) {
            sharedPreferences.edit().putInt(KEY_LOGGING_LEVEL, value).apply()
        }

    /**
     * 获取下一个克隆ID并递增计数器
     */
    fun getAndIncrementCloneId(): Int {
        val currentId = nextCloneIdCounter
        nextCloneIdCounter = currentId + 1
        return currentId
    }

    /**
     * 重置所有配置为默认值
     */
    fun resetToDefaults() {
        sharedPreferences.edit().clear().apply()
    }

    /**
     * 获取所有配置（用于调试和日志记录）
     */
    fun getAllConfigurations(): Map<String, Any?> {
        return mapOf(
            "debugModeEnabled" to isDebugModeEnabled,
            "firstLaunchFlag" to isFirstLaunch,
            "cloneIdCounter" to nextCloneIdCounter,
            "realApkToolEnabled" to isRealApkToolEnabled,
            "outputDirectoryPath" to outputDirectoryPath,
            "tempDirectoryPath" to tempDirectoryPath,
            "keepTempFilesEnabled" to isKeepTempFilesEnabled,
            "advancedOptionsEnabled" to isAdvancedOptionsEnabled,
            "autoCleanupEnabled" to isAutoCleanupEnabled,
            "maximumClonesPerApp" to maximumClonesPerApp,
            "loggingLevel" to loggingLevel
        )
    }
}

/**
 * 向后兼容别名
 */
@Deprecated("请使用 AppPreferencesManager", ReplaceWith("AppPreferencesManager"))
typealias PreferencesManager = AppPreferencesManager

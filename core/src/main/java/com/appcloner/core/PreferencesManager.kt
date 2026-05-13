
package com.appcloner.core

import android.content.Context
import android.content.SharedPreferences

/**
 * 配置管理类 - 统一管理应用配置
 * 产品化设计
 */
class PreferencesManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "AppClonerConfig"

        // Keys
        private const val KEY_DEBUG_MODE = "debug_mode"
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_CLONE_ID_COUNTER = "clone_id_counter"
        private const val KEY_USE_REAL_APKTOOL = "use_real_apktool"
        private const val KEY_OUTPUT_DIR_PATH = "output_dir_path"
        private const val KEY_TEMP_DIR_PATH = "temp_dir_path"
        private const val KEY_KEEP_TEMP_FILES = "keep_temp_files"
        private const val KEY_SHOW_COMPLEX_OPTIONS = "show_complex_options"
        private const val KEY_AUTO_DELETE_OLD_FILES = "auto_delete_old_files"
        private const val KEY_MAX_CLONES_PER_APP = "max_clones_per_app"
        private const val KEY_LOG_LEVEL = "log_level"

        // Default values
        private const val DEFAULT_CLONE_ID_COUNTER = 1000
        private const val DEFAULT_KEEP_TEMP_FILES = false
        private const val DEFAULT_SHOW_COMPLEX_OPTIONS = false
        private const val DEFAULT_AUTO_DELETE_OLD_FILES = true
        private const val DEFAULT_MAX_CLONES_PER_APP = 10
        private const val DEFAULT_LOG_LEVEL = 2 // INFO: 0=VERBOSE, 1=DEBUG, 2=INFO, 3=WARN, 4=ERROR
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    var isDebugMode: Boolean
        get() = prefs.getBoolean(KEY_DEBUG_MODE, true)
        set(value) {
            prefs.edit().putBoolean(KEY_DEBUG_MODE, value).apply()
        }

    var isFirstLaunch: Boolean
        get() = prefs.getBoolean(KEY_FIRST_LAUNCH, true)
        set(value) {
            prefs.edit().putBoolean(KEY_FIRST_LAUNCH, value).apply()
        }

    var nextCloneId: Int
        get() = prefs.getInt(KEY_CLONE_ID_COUNTER, DEFAULT_CLONE_ID_COUNTER)
        set(value) {
            prefs.edit().putInt(KEY_CLONE_ID_COUNTER, value).apply()
        }

    var useRealApkTool: Boolean
        get() = prefs.getBoolean(KEY_USE_REAL_APKTOOL, false)
        set(value) {
            prefs.edit().putBoolean(KEY_USE_REAL_APKTOOL, value).apply()
        }

    var outputDirPath: String?
        get() = prefs.getString(KEY_OUTPUT_DIR_PATH, null)
        set(value) {
            prefs.edit().putString(KEY_OUTPUT_DIR_PATH, value).apply()
        }

    var tempDirPath: String?
        get() = prefs.getString(KEY_TEMP_DIR_PATH, null)
        set(value) {
            prefs.edit().putString(KEY_TEMP_DIR_PATH, value).apply()
        }

    var keepTempFiles: Boolean
        get() = prefs.getBoolean(KEY_KEEP_TEMP_FILES, DEFAULT_KEEP_TEMP_FILES)
        set(value) {
            prefs.edit().putBoolean(KEY_KEEP_TEMP_FILES, value).apply()
        }

    var showComplexOptions: Boolean
        get() = prefs.getBoolean(KEY_SHOW_COMPLEX_OPTIONS, DEFAULT_SHOW_COMPLEX_OPTIONS)
        set(value) {
            prefs.edit().putBoolean(KEY_SHOW_COMPLEX_OPTIONS, value).apply()
        }

    var autoDeleteOldFiles: Boolean
        get() = prefs.getBoolean(KEY_AUTO_DELETE_OLD_FILES, DEFAULT_AUTO_DELETE_OLD_FILES)
        set(value) {
            prefs.edit().putBoolean(KEY_AUTO_DELETE_OLD_FILES, value).apply()
        }

    var maxClonesPerApp: Int
        get() = prefs.getInt(KEY_MAX_CLONES_PER_APP, DEFAULT_MAX_CLONES_PER_APP)
        set(value) {
            prefs.edit().putInt(KEY_MAX_CLONES_PER_APP, value).apply()
        }

    var logLevel: Int
        get() = prefs.getInt(KEY_LOG_LEVEL, DEFAULT_LOG_LEVEL)
        set(value) {
            prefs.edit().putInt(KEY_LOG_LEVEL, value).apply()
        }

    /**
     * 获取下一个克隆ID并递增
     */
    fun getAndIncrementCloneId(): Int {
        val current = nextCloneId
        nextCloneId = current + 1
        return current
    }

    /**
     * 重置所有配置为默认值
     */
    fun resetToDefaults() {
        prefs.edit().clear().apply()
    }

    /**
     * 获取所有配置（用于调试）
     */
    fun getAllConfig(): Map&lt;String, Any?&gt; {
        return mapOf(
            "debugMode" to isDebugMode,
            "firstLaunch" to isFirstLaunch,
            "cloneIdCounter" to nextCloneId,
            "useRealApkTool" to useRealApkTool,
            "outputDirPath" to outputDirPath,
            "tempDirPath" to tempDirPath,
            "keepTempFiles" to keepTempFiles,
            "showComplexOptions" to showComplexOptions,
            "autoDeleteOldFiles" to autoDeleteOldFiles,
            "maxClonesPerApp" to maxClonesPerApp,
            "logLevel" to logLevel
        )
    }
}

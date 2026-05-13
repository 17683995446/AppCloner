
package com.appcloner.core

import android.content.Context
import android.content.SharedPreferences

/**
 * 用户引导管理器 - 产品化功能
 * 管理首次使用、教程、提示点等用户体验
 */
class OnboardingManager private constructor(
    private val context: Context,
    private val preferencesManager: PreferencesManager
) {
    companion object {
        private const val TAG = "OnboardingManager"
        private const val PREFS_NAME = "onboarding"
        
        // 关键里程碑
        const val KEY_FIRST_LAUNCH = "first_launch"
        const val KEY_MAIN_TOUR_COMPLETED = "main_tour_completed"
        const val KEY_CLONE_TUTORIAL_COMPLETED = "clone_tutorial_completed"
        const val KEY_SAVE_TUTORIAL_COMPLETED = "save_tutorial_completed"
        const val KEY_ONBOARDING_VERSION = "onboarding_version"
        const val CURRENT_ONBOARDING_VERSION = 2
        
        // 提示点
        const val KEY_TIP_CLONE = "tip_clone_shown"
        const val KEY_TIP_SETTINGS = "tip_settings_shown"
        const val KEY_TIP_SHARE = "tip_share_shown"
        
        @Volatile
        private var instance: OnboardingManager? = null
        
        fun getInstance(
            context: Context,
            preferencesManager: PreferencesManager
        ): OnboardingManager {
            return instance ?: synchronized(this) {
                instance ?: OnboardingManager(context.applicationContext, preferencesManager)
                    .also { instance = it }
            }
        }
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // 引导步骤
    enum class OnboardingStep(
        val order: Int,
        val title: String,
        val description: String
    ) {
        INTRO(0, "欢迎！", "这个应用可以帮您克隆并管理其他应用"),
        CLONE(1, "克隆应用", "选择应用后点击克隆，创建新分身"),
        MANAGE(2, "管理克隆", "在克隆标签页中管理您的所有分身"),
        SETTINGS(3, "设置", "在设置中可以调整克隆行为和存储")
    }

    val isFirstLaunch: Boolean
        get() = prefs.getBoolean(KEY_FIRST_LAUNCH, true)

    var mainTourCompleted: Boolean
        get() = prefs.getBoolean(KEY_MAIN_TOUR_COMPLETED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_MAIN_TOUR_COMPLETED, value).apply()
        }

    var cloneTutorialCompleted: Boolean
        get() = prefs.getBoolean(KEY_CLONE_TUTORIAL_COMPLETED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_CLONE_TUTORIAL_COMPLETED, value).apply()
        }

    var saveTutorialCompleted: Boolean
        get() = prefs.getBoolean(KEY_SAVE_TUTORIAL_COMPLETED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_SAVE_TUTORIAL_COMPLETED, value).apply()
        }

    fun markFirstLaunchComplete() {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
    }

    fun shouldShowTip(tipKey: String): Boolean {
        return !prefs.getBoolean(tipKey, false)
    }

    fun markTipShown(tipKey: String) {
        prefs.edit().putBoolean(tipKey, true).apply()
    }

    fun getNextOnboardingStep(): OnboardingStep? {
        if (!mainTourCompleted) {
            return OnboardingStep.INTRO
        }
        if (!cloneTutorialCompleted) {
            return OnboardingStep.CLONE
        }
        if (!saveTutorialCompleted) {
            return OnboardingStep.MANAGE
        }
        return null
    }

    fun getProgressPercentage(): Int {
        var completed = 0
        if (mainTourCompleted) completed += 25
        if (cloneTutorialCompleted) completed += 25
        if (saveTutorialCompleted) completed += 25
        return completed
    }

    fun shouldShowOnboarding(): Boolean {
        val savedVersion = prefs.getInt(KEY_ONBOARDING_VERSION, 0)
        if (savedVersion &lt; CURRENT_ONBOARDING_VERSION) {
            // 有新版本，重置引导状态
            resetOnboarding()
            return true
        }
        return isFirstLaunch
    }

    fun resetOnboarding() {
        prefs.edit()
            .putBoolean(KEY_MAIN_TOUR_COMPLETED, false)
            .putBoolean(KEY_CLONE_TUTORIAL_COMPLETED, false)
            .putBoolean(KEY_SAVE_TUTORIAL_COMPLETED, false)
            .putInt(KEY_ONBOARDING_VERSION, CURRENT_ONBOARDING_VERSION)
            .remove(KEY_TIP_CLONE)
            .remove(KEY_TIP_SETTINGS)
            .remove(KEY_TIP_SHARE)
            .apply()
    }

    fun recordOnboardingStart() {
        markFirstLaunchComplete()
    }
}

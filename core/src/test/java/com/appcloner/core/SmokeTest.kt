
package com.appcloner.core

import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import io.mockk.mockk

/**
 * 冒烟测试 - 验证核心功能
 * 快速验证系统基本功能是否正常
 */
class SmokeTest {

    private lateinit var mockContext: android.content.Context
    private lateinit var mockPrefsManager: PreferencesManager

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        mockPrefsManager = mockk(relaxed = true)
    }

    @Test
    fun `test service locator initializes successfully`() {
        // 验证ServiceLocator可以初始化
        val serviceLocator = ServiceLocator.getInstance(mockContext)
        assertNotNull(serviceLocator)
    }

    @Test
    fun `test clone sandbox manager initializes`() {
        val sandboxManager = CloneSandboxManager.getInstance(mockContext, mockPrefsManager)
        assertNotNull(sandboxManager)
    }

    @Test
    fun `test analytics manager initializes`() {
        val analyticsManager = AnalyticsManager.getInstance(mockContext)
        assertNotNull(analyticsManager)
    }

    @Test
    fun `test search filter manager initializes`() {
        val searchFilterManager = SearchFilterManager.getInstance(mockContext)
        assertNotNull(searchFilterManager)
    }

    @Test
    fun `test preferences manager stores values`() {
        val prefsManager = PreferencesManager(mockContext)
        prefsManager.isDebugMode = true
        assertTrue(prefsManager.isDebugMode)
    }

    @Test
    fun `test batch operation manager initializes`() {
        val batchManager = BatchOperationManager.getInstance(mockContext, 
            CloneSandboxManager.getInstance(mockContext, mockPrefsManager))
        assertNotNull(batchManager)
    }

    @Test
    fun `test plugin manager initializes`() {
        val pluginManager = PluginManager.getInstance(mockContext)
        assertNotNull(pluginManager)
    }

    @Test
    fun `test error handler initializes`() {
        val analyticsManager = AnalyticsManager.getInstance(mockContext)
        val errorHandler = ErrorHandler.getInstance(mockContext, analyticsManager)
        assertNotNull(errorHandler)
    }

    @Test
    fun `test onboarding manager initializes`() {
        val onboardingManager = OnboardingManager.getInstance(mockContext, mockPrefsManager)
        assertNotNull(onboardingManager)
    }

    @Test
    fun `test shortcut manager initializes`() {
        val shortcutManager = ShortcutManager.getInstance(mockContext)
        assertNotNull(shortcutManager)
    }
}

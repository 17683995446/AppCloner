package com.appcloner.core

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith

/**
 * ServiceLocator 依赖注入综合测试
 * 
 * 测试内容：
 * - 单例模式验证
 * - 依赖注入
 * - 组件初始化
 * - 向后兼容性
 */
@RunWith(AndroidJUnit4::class)
class ServiceLocatorIntegrationTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `ServiceLocator应该实现单例模式`() {
        val instance1 = ServiceLocator.getInstance(context)
        val instance2 = ServiceLocator.getInstance(context)
        
        assertSame(instance1, instance2)
    }

    @Test
    fun `ServiceLocator应该能正确获取所有核心组件`() {
        val serviceLocator = ServiceLocator.getInstance(context)
        
        // 验证所有核心组件都可以获取
        assertNotNull(serviceLocator.appCloneManager)
        assertNotNull(serviceLocator.appPreferencesManager)
        assertNotNull(serviceLocator.appSearchFilterManager)
        assertNotNull(serviceLocator.pluginManager)
        assertNotNull(serviceLocator.analyticsManager)
    }

    @Test
    fun `向后兼容别名应该正常工作`() {
        val serviceLocator = ServiceLocator.getInstance(context)
        
        // 测试旧的API别名
        val oldCloneManager = serviceLocator.cloneManager
        val oldPreferencesManager = serviceLocator.preferencesManager
        
        assertNotNull(oldCloneManager)
        assertNotNull(oldPreferencesManager)
    }

    @Test
    fun `AppCloneManager应该能正常初始化`() {
        val serviceLocator = ServiceLocator.getInstance(context)
        val cloneManager = serviceLocator.appCloneManager
        
        assertNotNull(cloneManager)
        assertTrue(cloneManager is AppCloneManager)
    }

    @Test
    fun `AppPreferencesManager应该能正常初始化`() {
        val serviceLocator = ServiceLocator.getInstance(context)
        val preferencesManager = serviceLocator.appPreferencesManager
        
        assertNotNull(preferencesManager)
        assertTrue(preferencesManager is AppPreferencesManager)
        
        // 验证配置功能
        preferencesManager.isDebugModeEnabled = true
        assertTrue(preferencesManager.isDebugModeEnabled)
    }

    @Test
    fun `AppSearchFilterManager应该能正常初始化`() {
        val serviceLocator = ServiceLocator.getInstance(context)
        val searchManager = serviceLocator.appSearchFilterManager
        
        assertNotNull(searchManager)
        assertTrue(searchManager is AppSearchFilterManager)
    }

    @Test
    fun `PluginManager应该能正常初始化`() {
        val serviceLocator = ServiceLocator.getInstance(context)
        val pluginManager = serviceLocator.pluginManager
        
        assertNotNull(pluginManager)
        
        // 验证插件注册功能
        val pluginInfoList = pluginManager.getAllPluginInfo()
        assertNotNull(pluginInfoList)
    }

    @Test
    fun `AnalyticsManager应该能正常初始化`() {
        val serviceLocator = ServiceLocator.getInstance(context)
        val analyticsManager = serviceLocator.analyticsManager
        
        assertNotNull(analyticsManager)
    }

    @Test
    fun `沙盒管理器应该能正常初始化`() {
        val serviceLocator = ServiceLocator.getInstance(context)
        val sandboxManager = serviceLocator.sandboxManager
        
        assertNotNull(sandboxManager)
        
        // 验证统计功能
        val stats = sandboxManager.getSandboxStatistics()
        assertNotNull(stats)
    }

    @Test
    fun `快捷方式管理器应该能正常初始化`() {
        val serviceLocator = ServiceLocator.getInstance(context)
        val shortcutManager = serviceLocator.shortcutManager
        
        assertNotNull(shortcutManager)
    }

    @Test
    fun `错误处理器应该能正常初始化`() {
        val serviceLocator = ServiceLocator.getInstance(context)
        val errorHandler = serviceLocator.errorHandler
        
        assertNotNull(errorHandler)
    }

    @Test
    fun `首次引导管理器应该能正常初始化`() {
        val serviceLocator = ServiceLocator.getInstance(context)
        val onboardingManager = serviceLocator.onboardingManager
        
        assertNotNull(onboardingManager)
    }

    @Test
    fun `APK处理器服务应该能正常初始化`() {
        val serviceLocator = ServiceLocator.getInstance(context)
        
        // 验证 APK 处理服务
        assertNotNull(serviceLocator.apkToolService)
        assertNotNull(serviceLocator.signerService)
        assertNotNull(serviceLocator.packageNameModifier)
    }

    @Test
    fun `文件工具应该能正常获取`() {
        val serviceLocator = ServiceLocator.getInstance(context)
        val fileUtils = serviceLocator.fileUtils
        
        assertNotNull(fileUtils)
    }

    @Test
    fun `日志工具应该能正常获取`() {
        val serviceLocator = ServiceLocator.getInstance(context)
        val logger = serviceLocator.logger
        
        assertNotNull(logger)
    }

    @Test
    fun `组件应该能正确初始化和协同工作`() {
        val serviceLocator = ServiceLocator.getInstance(context)
        
        // 获取多个组件
        val preferencesManager = serviceLocator.appPreferencesManager
        val cloneManager = serviceLocator.appCloneManager
        val searchManager = serviceLocator.appSearchFilterManager
        
        // 验证组件能够独立工作
        preferencesManager.isDebugModeEnabled = true
        assertTrue(preferencesManager.isDebugModeEnabled)
        
        // 验证组件能够协同工作
        searchManager.updateSearchOptions(
            AppSearchFilterManager.SearchOptions(
                searchQuery = "test",
                sortBy = AppSearchFilterManager.SortOption.NAME_ASCENDING,
                filterBy = AppSearchFilterManager.FilterOption.ALL_APPS
            )
        )
        
        assertEquals("test", searchManager.currentOptions.value.searchQuery)
    }
}

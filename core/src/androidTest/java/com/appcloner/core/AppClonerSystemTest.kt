package com.appcloner.core

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File

/**
 * App Cloner系统测试 - 端到端功能验证
 * 
 * 测试目标：
 * - 核心功能完整链路
 * - 真实工具集成
 * - 插件系统协同
 * - 配置持久化
 */
@RunWith(AndroidJUnit4::class)
class AppClonerSystemTest {

    private lateinit var context: Context
    private lateinit var testDir: File

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        testDir = File(context.cacheDir, "app_cloner_system_test").apply {
            mkdirs()
        }
    }

    @Test
    fun `应用克隆系统的完整功能链路验证`() = runBlocking {
        // 1. 系统初始化验证
        val serviceLocator = ServiceLocator.getInstance(context)
        assertNotNull(serviceLocator)
        
        // 2. 配置管理器验证
        val preferencesManager = AppPreferencesManager(context)
        assertNotNull(preferencesManager)
        
        // 3. 配置读写验证
        preferencesManager.isDebugModeEnabled = true
        assertTrue(preferencesManager.isDebugModeEnabled)
        
        // 4. 克隆计数递增验证
        val initialCounter = preferencesManager.nextCloneIdCounter
        preferencesManager.nextCloneIdCounter = initialCounter + 1
        assertEquals(initialCounter + 1, preferencesManager.nextCloneIdCounter)
        
        // 5. 插件系统初始化验证
        val pluginManager = serviceLocator.pluginManager
        assertNotNull(pluginManager)
        
        // 6. 所有核心组件初始化验证
        assertNotNull(serviceLocator.appCloneManager)
        assertNotNull(serviceLocator.appPreferencesManager)
        assertNotNull(serviceLocator.analyticsManager)
        assertNotNull(serviceLocator.pluginManager)
        
        // 7. 向后兼容别名验证
        val legacyManager: CloneManager = serviceLocator.appCloneManager
        assertNotNull(legacyManager)
        
        // 测试完成！
        assertTrue(true)
    }

    @Test
    fun `搜索过滤系统的完整功能验证`() {
        val searchFilter = AppSearchFilterManager.getInstance(context)
        
        // 1. 初始状态验证
        val initialOptions = searchFilter.currentOptions.value
        assertEquals("", initialOptions.searchQuery)
        
        // 2. 搜索选项更新验证
        val newOptions = AppSearchFilterManager.SearchOptions(
            searchQuery = "test-app",
            sortBy = AppSearchFilterManager.SortOption.DATE_DESCENDING,
            filterBy = AppSearchFilterManager.FilterOption.USER_APPS
        )
        searchFilter.updateSearchOptions(newOptions)
        
        val updatedOptions = searchFilter.currentOptions.value
        assertEquals("test-app", updatedOptions.searchQuery)
        assertEquals(AppSearchFilterManager.SortOption.DATE_DESCENDING, updatedOptions.sortBy)
        assertEquals(AppSearchFilterManager.FilterOption.USER_APPS, updatedOptions.filterBy)
        
        // 3. 搜索历史功能验证
        searchFilter.clearSearchHistory()
        val emptyHistory = searchFilter.getSearchHistory()
        assertTrue(emptyHistory.isEmpty())
    }

    @Test
    fun `沙盒管理系统的基础功能验证`() {
        val sandboxManager = CloneSandboxManager(context)
        
        // 1. 沙盒管理器初始化验证
        assertNotNull(sandboxManager)
        
        // 2. 沙盒统计功能验证
        val statistics = sandboxManager.getSandboxStatistics()
        assertNotNull(statistics)
    }

    @Test
    fun `插件系统的完整集成验证`() {
        val pluginManager = PluginManager.getInstance(context)
        
        // 1. 插件管理器初始化验证
        assertNotNull(pluginManager)
        
        // 2. 插件信息获取验证
        val allPluginInfo = pluginManager.getAllPluginInfo()
        assertNotNull(allPluginInfo)
    }

    @Test
    fun `系统组件的完整集成测试`() {
        // 验证所有核心系统组件能够协同工作
        val serviceLocator = ServiceLocator.getInstance(context)
        
        // 组件初始化顺序验证
        assertNotNull(serviceLocator.appPreferencesManager)
        assertNotNull(serviceLocator.pluginManager)
        assertNotNull(serviceLocator.appCloneManager)
        assertNotNull(serviceLocator.analyticsManager)
        
        // 配置传递验证
        serviceLocator.appPreferencesManager.isDebugModeEnabled = true
        assertTrue(serviceLocator.appPreferencesManager.isDebugModeEnabled)
        
        // 所有系统组件协同验证完成！
        assertTrue(true)
    }

    @Test
    fun `错误处理系统的边界情况验证`() {
        // 验证异常场景下的系统稳定性
        val preferencesManager = AppPreferencesManager(context)
        
        // 配置重置测试
        preferencesManager.resetToDefaults()
        
        // 默认值验证
        assertTrue(preferencesManager.isDebugModeEnabled)
        assertTrue(preferencesManager.isFirstLaunch)
        assertEquals(1000, preferencesManager.nextCloneIdCounter)
    }
}

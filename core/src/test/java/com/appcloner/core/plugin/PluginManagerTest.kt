
package com.appcloner.core.plugin

import io.mockk.*
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * 插件系统测试
 */
class PluginManagerTest {

    private lateinit var mockContext: android.content.Context
    private lateinit var mockAnalyticsManager: com.appcloner.core.AnalyticsManager
    private lateinit var pluginManager: PluginManager
    private lateinit var testPlugin: AnalyticsTrackingPlugin

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        mockAnalyticsManager = mockk(relaxed = true)
        
        pluginManager = PluginManager.getInstance(mockContext)
        testPlugin = AnalyticsTrackingPlugin(mockAnalyticsManager)
    }

    @Test
    fun `test plugin registration works`() {
        val registerResult = pluginManager.registerPlugin(testPlugin)
        
        assertTrue("插件注册应该成功", registerResult.isSuccess)
    }

    @Test
    fun `test duplicate plugin registration fails`() {
        pluginManager.registerPlugin(testPlugin)
        val duplicateResult = pluginManager.registerPlugin(testPlugin)
        
        assertTrue("重复注册应该失败", duplicateResult.isFailure)
    }

    @Test
    fun `test plugin initialization works`() {
        pluginManager.registerPlugin(testPlugin)
        val initResult = pluginManager.initializePlugin(testPlugin.pluginId)
        
        assertTrue("初始化应该成功", initResult.isSuccess)
    }

    @Test
    fun `test plugin activation works`() {
        pluginManager.registerPlugin(testPlugin)
        pluginManager.initializePlugin(testPlugin.pluginId)
        
        val activateResult = pluginManager.activatePlugin(testPlugin.pluginId)
        
        assertTrue("激活应该成功", activateResult.isSuccess)
    }

    @Test
    fun `test plugin deactivation works`() {
        pluginManager.registerPlugin(testPlugin)
        pluginManager.initializePlugin(testPlugin.pluginId)
        pluginManager.activatePlugin(testPlugin.pluginId)
        
        val deactivateResult = pluginManager.deactivatePlugin(testPlugin.pluginId)
        
        assertTrue("去激活应该成功", deactivateResult.isSuccess)
    }

    @Test
    fun `test get plugin info returns correct info`() {
        pluginManager.registerPlugin(testPlugin)
        
        val info = pluginManager.getPluginInfo(testPlugin.pluginId)
        
        assertNotNull("插件信息应该存在", info)
        assertEquals(testPlugin.pluginName, info!!.name)
        assertEquals(testPlugin.pluginVersion, info.version)
    }

    @Test
    fun `test get all plugins returns all registered plugins`() {
        val plugin1 = AnalyticsTrackingPlugin(mockAnalyticsManager)
        val plugin2 = AnalyticsTrackingPlugin(mockAnalyticsManager) {
            override val pluginId: String = "another_plugin"
            override val pluginName: String = "Another Plugin"
        }
        
        pluginManager.registerPlugin(plugin1)
        pluginManager.registerPlugin(plugin2)
        
        val allPlugins = pluginManager.getAllPluginInfo()
        
        assertEquals(2, allPlugins.size)
    }

    @Test
    fun `test get active plugins returns only active ones`() {
        pluginManager.registerPlugin(testPlugin)
        pluginManager.initializePlugin(testPlugin.pluginId)
        
        var activePlugins = pluginManager.getActivePlugins()
        assertEquals(0, activePlugins.size)
        
        pluginManager.activatePlugin(testPlugin.pluginId)
        
        activePlugins = pluginManager.getActivePlugins()
        assertEquals(1, activePlugins.size)
    }

    @Test
    fun `test get plugin by type works`() {
        pluginManager.registerPlugin(testPlugin)
        
        val retrieved = pluginManager.getPlugin(
            testPlugin.pluginId,
            AnalyticsTrackingPlugin::class.java
        )
        
        assertNotNull("应该能找到插件", retrieved)
        assertTrue("类型应该匹配", retrieved is AnalyticsTrackingPlugin)
    }

    @Test
    fun `test initialize all plugins works`() {
        pluginManager.registerPlugin(testPlugin)
        
        val results = pluginManager.initializeAllPlugins()
        
        assertEquals(1, results.size)
        assertTrue(results.all { it.isSuccess })
    }
}

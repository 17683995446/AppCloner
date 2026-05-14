package com.appcloner.core.plugin

import io.mockk.*
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.io.File

/**
 * 增强的插件系统综合测试 - v6.0新增
 * 测试Notification、Backup、Statistics等新插件
 */
class EnhancedPluginSystemTest {

    private lateinit var mockContext: android.content.Context
    private lateinit var pluginManager: PluginManager

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        pluginManager = PluginManager.getInstance(mockContext)
    }

    @Test
    fun `test NotificationPlugin registration and initialization`() {
        val plugin = NotificationPluginImpl()
        
        // 注册
        val registerResult = pluginManager.registerPlugin(plugin)
        assertTrue("NotificationPlugin should register successfully", registerResult.isSuccess)
        
        // 初始化
        val initResult = pluginManager.initializePlugin(plugin.pluginId)
        assertTrue("NotificationPlugin should initialize successfully", initResult.isSuccess)
    }

    @Test
    fun `test NotificationPlugin notification methods work`() {
        val plugin = NotificationPluginImpl()
        
        // 基本功能测试（不会抛出异常
        plugin.initialize(mockContext)
        
        // 测试通知方法
        val cloneInfo = CloneInfo(
            id = "test-id",
            packageName = "com.test.app",
            appName = "Test App",
            createdTime = System.currentTimeMillis(),
            cloneCount = 1
        )
        
        // 这些方法不会抛出异常
        plugin.sendCloneSuccessNotification(cloneInfo)
        plugin.sendCloneFailedNotification("com.test.failed", "Test error")
        plugin.sendBatchOperationNotification("clone", 5)
        plugin.showInAppNotification("Test", "Test message")
        
        // 测试通过，没有异常
        assertTrue(true)
    }

    @Test
    fun `test BackupPlugin interface contracts`() {
        val plugin = BackupPluginImpl()
        
        // 测试插件基本属性
        assertEquals("com.appcloner.plugin.backup", plugin.pluginId)
        assertEquals("Backup Plugin", plugin.pluginName)
        assertEquals("1.0.0", plugin.pluginVersion)
        assertNotNull(plugin.pluginDescription)
    }

    @Test
    fun `test StatisticsPlugin recording functionality`() {
        val plugin = StatisticsPluginImpl()
        
        // 初始化
        plugin.initialize(mockContext)
        
        // 记录操作
        plugin.recordCloneOperation("com.test.app1", success = true)
        plugin.recordCloneOperation("com.test.app2", success = true)
        plugin.recordCloneOperation("com.test.app1", success = false)
        plugin.recordAppLaunch("com.test.app1")
        
        // 获取统计
        val stats = plugin.getStatistics()
        
        assertEquals(3, stats.totalClones)
        assertEquals(2, stats.successfulClones)
        assertEquals(1, stats.failedClones)
        assertEquals(1, stats.totalLaunches)
    }

    @Test
    fun `test multiple plugins working together`() {
        val notificationPlugin = NotificationPluginImpl()
        val backupPlugin = BackupPluginImpl()
        val statsPlugin = StatisticsPluginImpl()
        
        // 注册多个插件
        pluginManager.registerPlugin(notificationPlugin)
        pluginManager.registerPlugin(backupPlugin)
        pluginManager.registerPlugin(statsPlugin)
        
        // 初始化所有
        val results = pluginManager.initializeAllPlugins()
        
        assertEquals(3, results.size)
        assertTrue("All plugins should initialize successfully", 
            results.all { it.isSuccess })
        
        // 获取所有插件信息
        val allPluginInfo = pluginManager.getAllPluginInfo()
        assertEquals(3, allPluginInfo.size)
    }

    @Test
    fun `test plugin lifecycle management`() {
        val plugin = NotificationPluginImpl()
        
        pluginManager.registerPlugin(plugin)
        
        // 测试完整生命周期
        pluginManager.initializePlugin(plugin.pluginId)
        pluginManager.activatePlugin(plugin.pluginId)
        
        val activePlugins = pluginManager.getActivePlugins()
        assertEquals(1, activePlugins.size)
        
        pluginManager.deactivatePlugin(plugin.pluginId)
        
        val activeAfter = pluginManager.getActivePlugins()
        assertEquals(0, activeAfter.size)
    }

    @Test
    fun `test get plugin by specific interface type`() {
        val plugin = NotificationPluginImpl()
        
        pluginManager.registerPlugin(plugin)
        
        val retrieved = pluginManager.getPlugin(
            plugin.pluginId,
            NotificationPlugin::class.java
        )
        
        assertNotNull("Should find notification plugin", retrieved)
        assertTrue("Should be notification plugin type", retrieved is NotificationPlugin)
    }

    @Test
    fun `test CloneConfig data class`() {
        val config = CloneConfig(
            originalApk = File("/test/original.apk"),
            newPackageName = "com.test.clone",
            newAppName = "Test Clone",
            outputDir = File("/test/output")
        )
        
        assertEquals("/test/original.apk", config.originalApk.absolutePath)
        assertEquals("com.test.clone", config.newPackageName)
        assertEquals("Test Clone", config.newAppName)
    }

    @Test
    fun `test CloneInfo data class`() {
        val info = CloneInfo(
            id = "test-clone-id",
            packageName = "com.test.clone",
            appName = "Test Clone App",
            createdTime = 1234567890L,
            cloneCount = 5
        )
        
        assertEquals("test-clone-id", info.id)
        assertEquals("com.test.clone", info.packageName)
        assertEquals("Test Clone App", info.appName)
        assertEquals(1234567890L, info.createdTime)
        assertEquals(5, info.cloneCount)
    }

    @Test
    fun `test CloneStatistics data class`() {
        val stats = CloneStatistics(
            totalClones = 100,
            successfulClones = 90,
            failedClones = 10,
            totalLaunches = 500,
            mostClonedApp = "com.popular.app",
            lastCloneTime = 987654321L
        )
        
        assertEquals(100, stats.totalClones)
        assertEquals(90, stats.successfulClones)
        assertEquals(10, stats.failedClones)
        assertEquals(500, stats.totalLaunches)
        assertEquals("com.popular.app", stats.mostClonedApp)
        assertEquals(987654321L, stats.lastCloneTime)
    }
}

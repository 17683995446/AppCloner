package com.appcloner.core

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * 应用配置管理器单元测试
 * 
 * 测试内容：
 * - 配置读写功能
 * - 默认值验证
 * - 特殊边界情况
 * - 配置重置功能
 */
class AppPreferencesManagerTest {

    private lateinit var mockContext: Context
    private lateinit var mockSharedPreferences: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor
    
    private lateinit var appPreferencesManager: AppPreferencesManager

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        mockSharedPreferences = mockk(relaxed = true)
        mockEditor = mockk(relaxed = true)
        
        every { mockContext.getSharedPreferences(any(), any()) } returns mockSharedPreferences
        every { mockSharedPreferences.edit() } returns mockEditor
        every { mockEditor.apply() } returns Unit
        every { mockEditor.clear() } returns mockEditor
        
        // 设置默认值
        every { mockSharedPreferences.getBoolean("debug_mode_enabled", true) } returns true
        every { mockSharedPreferences.getBoolean("first_launch_flag", true) } returns true
        every { mockSharedPreferences.getInt("clone_id_counter", 1000) } returns 1000
        every { mockSharedPreferences.getBoolean("use_real_apktool_flag", true) } returns true
        every { mockSharedPreferences.getBoolean("keep_temp_files_flag", false) } returns false
        every { mockSharedPreferences.getBoolean("show_advanced_options", false) } returns false
        every { mockSharedPreferences.getBoolean("auto_cleanup_enabled", true) } returns true
        every { mockSharedPreferences.getInt("maximum_clones_per_app", 10) } returns 10
        every { mockSharedPreferences.getInt("logging_level", 2) } returns 2
        
        appPreferencesManager = AppPreferencesManager(mockContext)
    }

    @Test
    fun `AppPreferencesManager应该能正常初始化`() {
        assertNotNull(appPreferencesManager)
    }

    @Test
    fun `向后兼容别名应该正常工作`() {
        val testAlias: PreferencesManager = appPreferencesManager
        assertNotNull(testAlias)
    }

    @Test
    fun `调试模式设置应该能正确读写`() {
        // 验证默认值
        assertTrue(appPreferencesManager.isDebugModeEnabled)
        
        // 模拟写入值
        every { mockSharedPreferences.getBoolean("debug_mode_enabled", true) } returns false
        assertFalse(appPreferencesManager.isDebugModeEnabled)
    }

    @Test
    fun `首次启动标志应该能正确读写`() {
        // 验证默认值
        assertTrue(appPreferencesManager.isFirstLaunch)
        
        // 模拟修改
        every { mockSharedPreferences.getBoolean("first_launch_flag", true) } returns false
        assertFalse(appPreferencesManager.isFirstLaunch)
    }

    @Test
    fun `克隆ID计数器应该能正确递增`() {
        val initialId = appPreferencesManager.nextCloneIdCounter
        assertEquals(1000, initialId)
        
        // 模拟递增
        every { mockSharedPreferences.getInt("clone_id_counter", 1000) } returns initialId + 1
        val newId = appPreferencesManager.nextCloneIdCounter
        assertEquals(initialId + 1, newId)
    }

    @Test
    fun `真实APKTool开关应该能正确设置`() {
        // 默认应为启用
        assertTrue(appPreferencesManager.isRealApkToolEnabled)
        
        // 模拟禁用
        every { mockSharedPreferences.getBoolean("use_real_apktool_flag", true) } returns false
        assertFalse(appPreferencesManager.isRealApkToolEnabled)
    }

    @Test
    fun `输出目录路径应该能正确保存`() {
        val testPath = "/sdcard/AppCloner/Output"
        every { mockSharedPreferences.getString("output_directory_path", null) } returns testPath
        
        val result = appPreferencesManager.outputDirectoryPath
        assertEquals(testPath, result)
    }

    @Test
    fun `所有配置应该能正确获取`() {
        val configs = appPreferencesManager.getAllConfigurations()
        
        assertNotNull(configs)
        assertTrue(configs.containsKey("debugModeEnabled"))
        assertTrue(configs.containsKey("firstLaunchFlag"))
        assertTrue(configs.containsKey("cloneIdCounter"))
        assertTrue(configs.containsKey("realApkToolEnabled"))
    }

    @Test
    fun `配置重置功能应该能正常工作`() {
        // 调用重置
        appPreferencesManager.resetToDefaults()
        
        // 验证是否调用了clear
        verify { mockEditor.clear() }
    }

    @Test
    fun `高级选项设置应该能正确读写`() {
        // 验证默认值
        assertFalse(appPreferencesManager.isAdvancedOptionsEnabled)
        
        // 模拟启用
        every { mockSharedPreferences.getBoolean("show_advanced_options", false) } returns true
        assertTrue(appPreferencesManager.isAdvancedOptionsEnabled)
    }

    @Test
    fun `自动清理设置应该能正确设置`() {
        assertTrue(appPreferencesManager.isAutoCleanupEnabled)
        
        // 模拟禁用
        every { mockSharedPreferences.getBoolean("auto_cleanup_enabled", true) } returns false
        assertFalse(appPreferencesManager.isAutoCleanupEnabled)
    }

    @Test
    fun `最大克隆数量限制应该能正确配置`() {
        // 验证默认值
        assertEquals(10, appPreferencesManager.maximumClonesPerApp)
        
        // 模拟修改
        every { mockSharedPreferences.getInt("maximum_clones_per_app", 10) } returns 5
        assertEquals(5, appPreferencesManager.maximumClonesPerApp)
    }

    @Test
    fun `日志级别应该能正确设置`() {
        // 验证默认级别（INFO）
        assertEquals(2, appPreferencesManager.loggingLevel)
        
        // 模拟设置为DEBUG级别
        every { mockSharedPreferences.getInt("logging_level", 2) } returns 1
        assertEquals(1, appPreferencesManager.loggingLevel)
    }
}

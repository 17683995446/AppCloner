
package com.appcloner.core

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.io.File

/**
 * PreferencesManager 单元测试
 */
class PreferencesManagerTest {

    private lateinit var mockContext: android.content.Context
    private lateinit var mockSharedPrefs: android.content.SharedPreferences
    private lateinit var mockEditor: android.content.SharedPreferences.Editor
    
    private lateinit var preferencesManager: PreferencesManager

    @Before
    fun setup() {
        // 创建Mock对象
        mockContext = mockk()
        mockSharedPrefs = mockk(relaxed = true)
        mockEditor = mockk(relaxed = true)
        
        // 配置Mock行为
        every { mockContext.getSharedPreferences(any(), any()) } returns mockSharedPrefs
        every { mockSharedPrefs.edit() } returns mockEditor
        every { mockEditor.putBoolean(any(), any()) } returns mockEditor
        every { mockEditor.putInt(any(), any()) } returns mockEditor
        every { mockEditor.putString(any(), any()) } returns mockEditor
        every { mockEditor.apply() } answers {}
        
        // 初始化被测试类
        preferencesManager = PreferencesManager(mockContext)
    }

    @Test
    fun `test isDebugMode default value`() {
        // Given
        every { mockSharedPrefs.getBoolean("debug_mode", true) } returns true
        
        // When
        val result = preferencesManager.isDebugMode
        
        // Then
        assertTrue(result)
    }

    @Test
    fun `test set isDebugMode updates shared preferences`() {
        // When
        preferencesManager.isDebugMode = false
        
        // Then
        verify { mockEditor.putBoolean("debug_mode", false) }
        verify { mockEditor.apply() }
    }

    @Test
    fun `test getAndIncrementCloneId`() {
        // Given
        every { mockSharedPrefs.getInt("clone_id_counter", 1000) } returns 1005
        every { mockEditor.putInt("clone_id_counter", 1006) } returns mockEditor
        
        // When
        val id = preferencesManager.getAndIncrementCloneId()
        
        // Then
        assertEquals(1005, id)
        verify { mockEditor.putInt("clone_id_counter", 1006) }
        verify { mockEditor.apply() }
    }

    @Test
    fun `test resetToDefaults clears preferences`() {
        // When
        preferencesManager.resetToDefaults()
        
        // Then
        verify { mockEditor.clear() }
        verify { mockEditor.apply() }
    }

    @Test
    fun `test getAllConfig returns all configs`() {
        // Given
        every { mockSharedPrefs.getBoolean("debug_mode", true) } returns true
        every { mockSharedPrefs.getBoolean("first_launch", true) } returns true
        every { mockSharedPrefs.getInt("clone_id_counter", 1000) } returns 1001
        every { mockSharedPrefs.getBoolean("use_real_apktool", false) } returns false
        every { mockSharedPrefs.getString("output_dir_path", null) } returns "/test/path"
        
        // When
        val config = preferencesManager.getAllConfig()
        
        // Then
        assertTrue(config.isNotEmpty())
        assertTrue(config["debugMode"] as Boolean)
        assertEquals("/test/path", config["outputDirPath"])
    }
}

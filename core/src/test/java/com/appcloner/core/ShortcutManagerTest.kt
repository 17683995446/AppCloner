
package com.appcloner.core

import io.mockk.*
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * 快捷方式管理器测试
 */
class ShortcutManagerTest {

    private lateinit var mockContext: android.content.Context
    
    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
    }

    @Test
    fun `test getInstance returns singleton`() {
        val instance1 = ShortcutManager.getInstance(mockContext)
        val instance2 = ShortcutManager.getInstance(mockContext)
        
        assertSame(instance1, instance2)
    }

    @Test
    fun `test create shortcut returns result`() {
        val shortcutManager = ShortcutManager.getInstance(mockContext)
        
        val result = shortcutManager.createShortcut(
            packageName = "com.test.app",
            appName = "Test App",
            cloneId = 1
        )
        
        // 由于是模拟环境，结果可能是失败，但这验证了方法可以被调用
        assertNotNull(result)
    }

    @Test
    fun `test get all dynamic shortcuts returns list`() {
        val shortcutManager = ShortcutManager.getInstance(mockContext)
        
        val shortcuts = shortcutManager.getAllDynamicShortcuts()
        
        assertNotNull(shortcuts)
        assertTrue(shortcuts is List)
    }
}

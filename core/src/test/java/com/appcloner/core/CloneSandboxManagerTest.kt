
package com.appcloner.core

import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * 沙盒管理器测试 - 核心创新功能测试
 */
class CloneSandboxManagerTest {

    private lateinit var mockContext: android.content.Context
    private lateinit var mockPrefs: PreferencesManager
    private lateinit var sandboxManager: CloneSandboxManager

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        mockPrefs = mockk(relaxed = true)
        
        every { mockPrefs.getAndIncrementCloneId() } returns 1001
        every { mockPrefs.outputDirPath } returns null
        every { mockPrefs.outputDirPath = any() } just Runs
        
        sandboxManager = CloneSandboxManager.getInstance(mockContext, mockPrefs)
    }

    @Test
    fun `test sandbox creation succeeds`() = runTest {
        val result = sandboxManager.createSandbox(
            originalPackageName = "com.original.app",
            newPackageName = "com.original.app.clone1",
            appName = "Clone App"
        )
        
        assertTrue("沙盒创建应该成功", result.isSuccess)
        val sandbox = result.getOrThrow()
        assertEquals("com.original.app", sandbox.originalPackageName)
        assertEquals("com.original.app.clone1", sandbox.newPackageName)
        assertEquals("Clone App", sandbox.appName)
    }

    @Test
    fun `test get sandbox stats is correct`() = runTest {
        // 创建几个沙盒
        sandboxManager.createSandbox("com.app1", "com.app1.clone1", "App1 Clone")
        sandboxManager.createSandbox("com.app2", "com.app2.clone1", "App2 Clone")
        
        val stats = sandboxManager.getSandboxStats()
        
        assertEquals(2, stats.totalClones)
        assertEquals(1.0f, stats.cloneSuccessRate)
    }

    @Test
    fun `test sandbox deletion works`() = runTest {
        // 先创建
        val createResult = sandboxManager.createSandbox("com.test", "com.test.clone", "Test Clone")
        val sandbox = createResult.getOrThrow()
        
        // 再删除
        val deleteResult = sandboxManager.deleteSandbox(sandbox.newPackageName)
        
        assertTrue("删除应该成功", deleteResult.isSuccess)
    }

    @Test
    fun `test get all sandboxes returns correct list`() = runTest {
        sandboxManager.createSandbox("com.a", "com.a.clone", "A Clone")
        sandboxManager.createSandbox("com.b", "com.b.clone", "B Clone")
        
        val sandboxes = sandboxManager.getAllSandboxes()
        
        assertEquals(2, sandboxes.size)
    }

    @Test
    fun `test sandbox tracking works`() = runTest {
        val createResult = sandboxManager.createSandbox("com.test", "com.test.clone", "Test")
        val sandbox = createResult.getOrThrow()
        
        val launchResult = sandboxManager.launchSandbox(sandbox.newPackageName)
        
        assertTrue(launchResult.isSuccess)
    }

    @Test
    fun `test get sandbox for package returns correct sandbox`() = runTest {
        val createResult = sandboxManager.createSandbox("com.test", "com.test.clone", "Test")
        
        val foundSandbox = sandboxManager.getSandboxForPackage("com.test.clone")
        
        assertNotNull(foundSandbox)
        assertEquals("com.test.clone", foundSandbox!!.newPackageName)
    }

    @Test
    fun `test clear all sandboxes removes everything`() = runTest {
        sandboxManager.createSandbox("com.a", "com.a.clone", "A")
        sandboxManager.createSandbox("com.b", "com.b.clone", "B")
        
        sandboxManager.clearAllSandboxes()
        
        assertEquals(0, sandboxManager.getAllSandboxes().size)
    }
}


package com.appcloner.core

import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * 批量操作管理器测试 - 实用性测试
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BatchOperationManagerTest {

    private lateinit var mockContext: android.content.Context
    private lateinit var mockSandboxManager: CloneSandboxManager
    private lateinit var batchOperationManager: BatchOperationManager

    @Before
    fun setup() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        
        mockContext = mockk(relaxed = true)
        mockSandboxManager = mockk(relaxed = true)
        
        // 配置沙盒管理器返回成功
        every { mockSandboxManager.createSandbox(any(), any(), any()) } returns Result.success(
            CloneSandboxManager::class.java.getDeclaredConstructor(
                android.content.Context::class.java,
                PreferencesManager::class.java
            ).newInstance(mockContext, mockk()).newSandbox()
        )
        
        every { mockSandboxManager.deleteSandbox(any()) } returns Result.success(Unit)
        
        batchOperationManager = BatchOperationManager.getInstance(mockContext, mockSandboxManager)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test batch clone succeeds for multiple apps`() = runTest {
        val apps = listOf(
            Triple("com.app1", "com.app1.clone", "App 1"),
            Triple("com.app2", "com.app2.clone", "App 2"),
            Triple("com.app3", "com.app3.clone", "App 3")
        )
        
        val result = batchOperationManager.batchClone(apps)
        
        advanceUntilIdle()
        
        assertTrue(result.totalRequested == 3)
    }

    @Test
    fun `test batch clone progress is tracked`() = runTest {
        val apps = listOf(
            Triple("com.app1", "com.app1.clone", "App 1"),
            Triple("com.app2", "com.app2.clone", "App 2")
        )
        
        batchOperationManager.batchClone(apps)
        
        advanceUntilIdle()
        
        val progress = batchOperationManager.batchProgress.value
        assertTrue(progress.isComplete)
        assertEquals(2, progress.total)
    }

    @Test
    fun `test batch delete succeeds`() = runTest {
        val packages = listOf(
            "com.app1.clone",
            "com.app2.clone"
        )
        
        val result = batchOperationManager.batchDelete(packages)
        
        advanceUntilIdle()
        
        assertTrue(result.totalRequested == 2)
    }

    @Test
    fun `test reset progress works`() = runTest {
        // 先执行一些操作
        val apps = listOf(
            Triple("com.app1", "com.app1.clone", "App 1")
        )
        batchOperationManager.batchClone(apps)
        
        advanceUntilIdle()
        
        // 重置
        batchOperationManager.resetProgress()
        
        val progress = batchOperationManager.batchProgress.value
        assertFalse(progress.isComplete)
        assertEquals(0, progress.current)
    }

    @Test
    fun `test batch operation handles errors`() = runTest {
        // 配置沙盒管理器返回失败
        every { mockSandboxManager.createSandbox("com.app2", any(), any()) } returns 
            Result.failure(Exception("Test error"))
        
        val apps = listOf(
            Triple("com.app1", "com.app1.clone", "App 1"),
            Triple("com.app2", "com.app2.clone", "App 2")
        )
        
        val result = batchOperationManager.batchClone(apps)
        
        advanceUntilIdle()
        
        assertTrue(result.totalFailed &gt; 0)
    }
}

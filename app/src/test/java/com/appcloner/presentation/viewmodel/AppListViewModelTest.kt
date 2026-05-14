
package com.appcloner.presentation.viewmodel

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
 * ViewModel测试
 * 演示专业的UI层测试
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppListViewModelTest {

    private lateinit var mockCloneManager: com.appcloner.core.CloneManager
    private lateinit var mockSandboxManager: com.appcloner.core.CloneSandboxManager
    private lateinit var mockAnalyticsManager: com.appcloner.core.AnalyticsManager
    private lateinit var mockPluginManager: com.appcloner.core.plugin.PluginManager
    
    private lateinit var viewModel: AppListViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        
        mockCloneManager = mockk(relaxed = true)
        mockSandboxManager = mockk(relaxed = true)
        mockAnalyticsManager = mockk(relaxed = true)
        mockPluginManager = mockk(relaxed = true)
        
        viewModel = AppListViewModel(
            mockCloneManager,
            mockSandboxManager,
            mockAnalyticsManager,
            mockPluginManager
        )
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test initial state is correct`() {
        val state = viewModel.appListUiState.value
        
        assertFalse(state.isLoading)
        assertTrue(state.apps.isEmpty())
        assertNull(state.error)
    }

    @Test
    fun `test loadAppList updates loading state`() = runTest {
        viewModel.loadAppList()
        
        // 初始状态应该是loading
        assertTrue(viewModel.appListUiState.value.isLoading)
        
        advanceUntilIdle()
        
        // 加载完成后，loading应该是false
        assertFalse(viewModel.appListUiState.value.isLoading)
    }

    @Test
    fun `test startClone updates clone state`() = runTest {
        every { mockSandboxManager.createSandbox(any(), any(), any()) } returns Result.success(Unit)
        
        viewModel.startClone(
            originalPackage = "com.original",
            newPackage = "com.original.clone",
            appName = "Clone App"
        )
        
        // 开始时应该是cloning状态
        assertTrue(viewModel.cloneUiState.value.isCloning)
        
        advanceUntilIdle()
        
        // 最终应该是成功状态
        assertFalse(viewModel.cloneUiState.value.isCloning)
        assertTrue(viewModel.cloneUiState.value.success)
    }

    @Test
    fun `test resetCloneState resets state correctly`() = runTest {
        // 先设置一个状态
        viewModel.startClone(
            originalPackage = "com.original",
            newPackage = "com.original.clone",
            appName = "Test"
        )
        
        advanceUntilIdle()
        
        viewModel.resetCloneState()
        
        val state = viewModel.cloneUiState.value
        assertFalse(state.isCloning)
        assertEquals(0, state.progress)
        assertFalse(state.success)
        assertNull(state.error)
    }

    @Test
    fun `test deleteClone calls sandbox manager`() = runTest {
        every { mockSandboxManager.deleteSandbox(any()) } returns Result.success(Unit)
        
        viewModel.deleteClone("com.test.package")
        
        advanceUntilIdle()
        
        verify(exactly = 1) { 
            mockSandboxManager.deleteSandbox("com.test.package") 
        }
        verify(exactly = 1) { 
            mockAnalyticsManager.trackCloneDeleted() 
        }
    }

    @Test
    fun `test clone error state is handled`() = runTest {
        val testError = RuntimeException("Test error")
        every { mockSandboxManager.createSandbox(any(), any(), any()) } returns Result.failure(testError)
        
        viewModel.startClone(
            originalPackage = "com.original",
            newPackage = "com.original.clone",
            appName = "Test App"
        )
        
        advanceUntilIdle()
        
        val state = viewModel.cloneUiState.value
        assertFalse(state.isCloning)
        assertNotNull(state.error)
        assertTrue(state.error!!.contains("Test error"))
    }
}

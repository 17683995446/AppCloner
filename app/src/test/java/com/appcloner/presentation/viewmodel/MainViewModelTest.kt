
package com.appcloner.presentation.viewmodel

import com.appcloner.core.model.AppInfo
import com.appcloner.data.repository.AppRepository
import com.appcloner.data.repository.CloneHistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

class MainViewModelTest {

    @Mock
    private lateinit var appRepository: AppRepository

    @Mock
    private lateinit var cloneHistoryRepository: CloneHistoryRepository

    private lateinit var viewModel: MainViewModel

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        
        viewModel = MainViewModel(appRepository, cloneHistoryRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should be Loading`() {
        val state = viewModel.uiState.value
        assertTrue(state is MainViewModel.UiState.Loading)
    }

    @Test
    fun `loadInstalledApps should update state to Success with apps`() = runTest {
        val testApps = listOf(
            AppInfo(
                appName = "Test App 1",
                packageName = "com.test.app1"
            ),
            AppInfo(
                appName = "Test App 2",
                packageName = "com.test.app2"
            )
        )
        
        `when`(appRepository.loadAndSyncInstalledApps()).thenReturn(Result.success(2))
        `when`(appRepository.getAllApps()).thenReturn(flowOf(testApps))
        
        viewModel.loadInstalledApps()
        
        // Give some time for state to update
        kotlinx.coroutines.delay(100)
        
        val state = viewModel.uiState.value
        assertTrue(state is MainViewModel.UiState.Success)
        val successState = state as MainViewModel.UiState.Success
        assertEquals(2, successState.apps.size)
    }

    @Test
    fun `searchApps with empty query should reload all apps`() = runTest {
        val testApps = listOf(
            AppInfo(appName = "Test App", packageName = "com.test.app")
        )
        
        `when`(appRepository.getAllApps()).thenReturn(flowOf(testApps))
        
        // First load some apps
        viewModel.loadInstalledApps()
        kotlinx.coroutines.delay(100)
        
        // Then clear search
        viewModel.searchApps("")
        kotlinx.coroutines.delay(100)
        
        verify(appRepository, atLeastOnce()).getAllApps()
    }

    @Test
    fun `toggleFavorite should call repository`() = runTest {
        val testApp = AppInfo(
            appName = "Test App",
            packageName = "com.test.app"
        )
        
        viewModel.toggleFavorite(testApp)
        
        verify(appRepository, times(1)).toggleFavorite("com.test.app")
    }

    @Test
    fun `cloneApp should update progress and save history`() = runTest {
        val testApp = AppInfo(
            appName = "Test App",
            packageName = "com.test.app"
        )
        
        viewModel.cloneApp(testApp)
        
        // Give some time for the coroutine to execute
        kotlinx.coroutines.delay(2500)
        
        verify(appRepository, times(1)).incrementCloneCount("com.test.app")
        verify(cloneHistoryRepository, times(1)).saveHistory(any())
        
        // After completion, progress should be null
        assertNull(viewModel.cloneProgress.value)
    }

    @Test
    fun `UiState Loading should be initial state`() {
        val loadingState = MainViewModel.UiState.Loading
        assertNotNull(loadingState)
    }

    @Test
    fun `UiState Success should hold apps`() {
        val testApp = AppInfo(appName = "Test", packageName = "com.test")
        val successState = MainViewModel.UiState.Success(listOf(testApp))
        
        assertEquals(1, successState.apps.size)
        assertEquals("Test", successState.apps[0].appName)
    }

    @Test
    fun `UiState Error should hold message`() {
        val errorState = MainViewModel.UiState.Error("Something went wrong")
        
        assertEquals("Something went wrong", errorState.message)
    }
}


package com.appcloner.core

import org.junit.Test
import org.junit.Assert.*
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

/**
 * 端到端测试 - 完整克隆流程测试
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EndToEndTest {

    private lateinit var mockContext: android.content.Context
    private lateinit var prefsManager: PreferencesManager
    private lateinit var sandboxManager: CloneSandboxManager
    private lateinit var analyticsManager: AnalyticsManager
    private lateinit var searchFilterManager: SearchFilterManager

    @Before
    fun setup() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        
        mockContext = mockk(relaxed = true)
        prefsManager = PreferencesManager(mockContext)
        sandboxManager = CloneSandboxManager.getInstance(mockContext, prefsManager)
        analyticsManager = AnalyticsManager.getInstance(mockContext)
        searchFilterManager = SearchFilterManager.getInstance(mockContext)
    }

    @Test
    fun `test complete clone workflow`() = runTest {
        // 1. 创建克隆
        val createResult = sandboxManager.createSandbox(
            originalPackageName = "com.test.app",
            newPackageName = "com.test.app.clone1",
            appName = "Test App Clone"
        )
        
        assertTrue("克隆创建应该成功", createResult.isSuccess)
        
        // 2. 验证克隆存在
        val sandbox = sandboxManager.getSandboxForPackage("com.test.app.clone1")
        assertNotNull("克隆应该存在", sandbox)
        assertEquals("Test App Clone", sandbox!!.appName)
        
        // 3. 启动克隆
        val launchResult = sandboxManager.launchSandbox("com.test.app.clone1")
        assertTrue("克隆启动应该成功", launchResult.isSuccess)
        
        // 4. 获取统计
        val stats = sandboxManager.getSandboxStats()
        assertTrue("应该有至少一个克隆", stats.totalClones &gt;= 1)
        
        // 5. 清理
        val deleteResult = sandboxManager.deleteSandbox("com.test.app.clone1")
        assertTrue("删除应该成功", deleteResult.isSuccess)
    }

    @Test
    fun `test search and filter workflow`() = runTest {
        // 1. 创建多个克隆
        sandboxManager.createSandbox("com.app1", "com.app1.clone", "App 1")
        sandboxManager.createSandbox("com.app2", "com.app2.clone", "App 2")
        sandboxManager.createSandbox("com.app3", "com.app3.clone", "App 3")
        
        // 2. 获取所有应用
        val allApps = sandboxManager.getAllSandboxes().map { sandbox -&gt;
            AppInfo(
                appName = sandbox.appName,
                packageName = sandbox.newPackageName,
                icon = null,
                isCloned = true,
                cloneId = sandbox.cloneId
            )
        }
        
        assertEquals("应该有3个应用", 3, allApps.size)
        
        // 3. 搜索
        val searchResult = searchFilterManager.search(
            allApps,
            SearchFilterManager.SearchOptions(query = "App 1")
        )
        
        assertEquals("应该匹配1个结果", 1, searchResult.matchCount)
        
        // 4. 过滤
        val filterResult = searchFilterManager.search(
            allApps,
            SearchFilterManager.SearchOptions(filterBy = SearchFilterManager.FilterOption.CLONED)
        )
        
        assertEquals("应该过滤出3个克隆", 3, filterResult.matchCount)
        
        // 5. 排序
        val sortResult = searchFilterManager.search(
            allApps,
            SearchFilterManager.SearchOptions(sortBy = SearchFilterManager.SortOption.NAME_DESC)
        )
        
        assertEquals("App 3", sortResult.apps[0].appName)
    }

    @Test
    fun `test analytics tracking workflow`() {
        // 1. 追踪应用启动
        analyticsManager.trackAppLaunch()
        
        // 2. 追踪克隆创建
        analyticsManager.trackCloneCreated()
        
        // 3. 追踪克隆删除
        analyticsManager.trackCloneDeleted()
        
        // 4. 获取统计
        val usageStats = analyticsManager.getUsageStats()
        
        assertTrue("至少应该有1次启动", usageStats.totalLaunches &gt;= 1)
        assertTrue("至少应该有1次克隆创建", usageStats.totalClonesCreated &gt;= 1)
        assertTrue("至少应该有1次克隆删除", usageStats.totalClonesDeleted &gt;= 1)
        
        // 5. 获取性能指标
        val perfMetrics = analyticsManager.getPerformanceMetrics()
        assertNotNull(perfMetrics)
        
        // 6. 获取错误统计
        val errorStats = analyticsManager.getErrorStats()
        assertNotNull(errorStats)
    }

    @Test
    fun `test error handling workflow`() {
        // 1. 创建错误处理器
        val errorHandler = ErrorHandler.getInstance(mockContext, analyticsManager)
        
        // 2. 模拟处理错误
        val testError = RuntimeException("Test error")
        val category = errorHandler.handleError(testError, "Test context")
        
        // 3. 验证错误分类
        assertNotNull(category)
        
        // 4. 获取用户友好消息
        val userMessage = errorHandler.getUserFriendlyMessage(category)
        assertNotNull(userMessage)
        assertTrue(userMessage.isNotBlank())
        
        // 5. 验证错误历史
        val errorHistory = errorHandler.getErrorHistory()
        assertTrue(errorHistory.isNotEmpty())
    }

    @Test
    fun `test batch operation workflow`() = runTest {
        val batchManager = BatchOperationManager.getInstance(mockContext, sandboxManager)
        
        // 准备批量克隆数据
        val appsToClone = listOf(
            Triple("com.app1", "com.app1.clone", "App 1"),
            Triple("com.app2", "com.app2.clone", "App 2")
        )
        
        // 执行批量克隆
        val result = batchManager.batchClone(appsToClone)
        
        // 验证结果
        assertTrue("应该有请求操作", result.totalRequested &gt; 0)
        assertTrue("应该至少有一个成功或失败", 
            result.totalSucceeded &gt; 0 || result.totalFailed &gt; 0)
        
        // 验证进度
        val progress = batchManager.batchProgress.value
        assertTrue("进度应该完成", progress.isComplete)
        
        // 清理
        batchManager.resetProgress()
    }

    @Test
    fun `test onboarding workflow`() {
        // 1. 检查是否首次启动
        val onboardingManager = OnboardingManager.getInstance(mockContext, prefsManager)
        
        // 2. 检查引导状态
        val shouldShowOnboarding = onboardingManager.shouldShowOnboarding()
        assertNotNull(shouldShowOnboarding)
        
        // 3. 获取下一个步骤
        val nextStep = onboardingManager.getNextOnboardingStep()
        assertNotNull(nextStep)
        
        // 4. 完成引导
        onboardingManager.recordOnboardingStart()
        assertFalse("应该标记首次启动完成", onboardingManager.isFirstLaunch)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }
}

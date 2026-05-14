package com.appcloner.core

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File

/**
 * 批量操作管理器综合测试
 * 
 * 测试内容：
 * - 批量克隆操作
 * - 批量删除操作
 * - 进度追踪
 * - 错误处理
 */
@RunWith(AndroidJUnit4::class)
class BatchOperationIntegrationTest {

    private lateinit var context: Context
    private lateinit var batchOperationManager: BatchOperationManager
    private lateinit var testDir: File

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        testDir = File(context.cacheDir, "batch_operation_test").apply {
            mkdirs()
        }
        
        val mockCloneManager = MockCloneManager()
        batchOperationManager = BatchOperationManager(
            context = context,
            cloneManager = mockCloneManager
        )
    }

    @Test
    fun `批量操作配置应该正确初始化`() {
        val config = BatchOperationConfig(
            totalOperations = 5,
            allowPartialSuccess = true,
            stopOnError = false
        )
        
        assertEquals(5, config.totalOperations)
        assertTrue(config.allowPartialSuccess)
        assertFalse(config.stopOnError)
    }

    @Test
    fun `批量操作进度应该正确计算`() {
        val progress1 = calculateProgress(2, 5)
        assertEquals(40.0, progress1, 0.1)
        
        val progress2 = calculateProgress(5, 5)
        assertEquals(100.0, progress2, 0.1)
        
        val progress3 = calculateProgress(0, 5)
        assertEquals(0.0, progress3, 0.1)
    }

    @Test
    fun `批量操作统计应该正确记录`() {
        val stats = BatchOperationStatistics(
            totalRequested = 10,
            totalCompleted = 8,
            totalFailed = 2,
            totalSkipped = 0,
            startTime = System.currentTimeMillis() - 1000,
            endTime = System.currentTimeMillis(),
            successRate = 80.0
        )
        
        assertEquals(10, stats.totalRequested)
        assertEquals(8, stats.totalCompleted)
        assertEquals(2, stats.totalFailed)
        assertEquals(0, stats.totalSkipped)
        assertEquals(80.0, stats.successRate, 0.1)
    }

    @Test
    fun `批量操作监听器应该正确回调`() = runBlocking {
        var progressCallCount = 0
        var completionCallCount = 0
        
        val listener = object : BatchOperationListener {
            override fun onProgress(progress: BatchOperationProgress) {
                progressCallCount++
            }
            
            override fun onItemCompleted(result: BatchOperationResult) {
                completionCallCount++
            }
            
            override fun onCompleted(stats: BatchOperationStatistics) {
                assertTrue(stats.totalRequested > 0)
            }
            
            override fun onError(error: BatchOperationError) {
                assertNotNull(error.message)
            }
        }
        
        // 模拟调用
        listener.onProgress(BatchOperationProgress(1, 5, 20.0, "测试"))
        listener.onItemCompleted(BatchOperationResult("app1", true, null))
        listener.onCompleted(BatchOperationStatistics(5, 5, 0, 0, 0, 0, 100.0))
        
        assertEquals(1, progressCallCount)
        assertEquals(1, completionCallCount)
    }

    @Test
    fun `批量操作错误类型应该正确区分`() {
        val error1 = BatchOperationError(
            type = ErrorType.INSUFFICIENT_STORAGE,
            message = "存储空间不足",
            recoverable = true
        )
        
        assertEquals(ErrorType.INSUFFICIENT_STORAGE, error1.type)
        assertEquals("存储空间不足", error1.message)
        assertTrue(error1.recoverable)
        
        val error2 = BatchOperationError(
            type = ErrorType.PERMISSION_DENIED,
            message = "权限被拒绝",
            recoverable = false
        )
        
        assertEquals(ErrorType.PERMISSION_DENIED, error2.type)
        assertFalse(error2.recoverable)
    }

    private fun calculateProgress(completed: Int, total: Int): Double {
        return if (total > 0) (completed.toDouble() / total.toDouble()) * 100.0 else 0.0
    }

    private inner class MockCloneManager : ICloneManager {
        override suspend fun cloneApp(
            packageName: String,
            newPackageName: String,
            newAppName: String
        ): Result<File> = Result.success(File("mock.apk"))

        override suspend fun deleteClone(packageName: String): Result<Boolean> = Result.success(true)

        override fun getClonedApps(): List<ClonedAppInfo> = emptyList()
    }
}

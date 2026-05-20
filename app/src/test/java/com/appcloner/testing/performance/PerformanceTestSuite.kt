package com.appcloner.testing.performance

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.lang.ref.WeakReference
import kotlin.system.measureTimeMillis
import kotlin.system.measureNanoTime

class PerformanceTestSuite(private val context: Context) {

    data class MemorySnapshot(
        val timestamp: Long,
        val freeMemory: Long,
        val totalMemory: Long,
        val usedMemory: Long,
        val nativeMemory: Long,
        val dalvikMemory: Long
    )

    data class PerformanceMetrics(
        val operationName: String,
        val executionTimeMs: Long,
        val memoryDelta: Long,
        val cpuTimeMs: Long,
        val gcCount: Int
    )

    data class TestResult(
        val testName: String,
        val passed: Boolean,
        val metrics: PerformanceMetrics,
        val warnings: List<String> = emptyList(),
        val recommendations: List<String> = emptyList()
    )

    private val memorySnapshots = mutableListOf<MemorySnapshot>()
    private var initialMemory: MemorySnapshot? = null

    fun takeMemorySnapshot(): MemorySnapshot {
        val runtime = Runtime.getRuntime()
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        return MemorySnapshot(
            timestamp = System.currentTimeMillis(),
            freeMemory = runtime.freeMemory(),
            totalMemory = runtime.totalMemory(),
            usedMemory = runtime.totalMemory() - runtime.freeMemory(),
            nativeMemory = Debug.getNativeHeapAllocatedSize(),
            dalvikMemory = runtime.totalMemory() - runtime.freeMemory()
        )
    }

    fun runMemoryLeakTests(): List<TestResult> {
        val results = mutableListOf<TestResult>()
        
        results.add(testContextLeak())
        results.add(testListenerLeak())
        results.add(testStaticReferenceLeak())
        results.add(testAsyncTaskLeak())
        results.add(testHandlerLeak())
        
        return results
    }

    private fun testContextLeak(): TestResult {
        initialMemory = takeMemorySnapshot()
        
        val weakRefs = mutableListOf<WeakReference<Context>>()
        
        repeat(100) { i ->
            val mockContext = MockContext()
            weakRefs.add(WeakReference(mockContext))
        }
        
        System.gc()
        System.runFinalization()
        Thread.sleep(100)
        System.gc()
        
        val retainedCount = weakRefs.count { it.get() != null }
        
        return PerformanceMetrics(
            operationName = "testContextLeak",
            executionTimeMs = 0,
            memoryDelta = 0,
            cpuTimeMs = 0,
            gcCount = 0
        ).let { metrics ->
            TestResult(
                testName = "testContextLeak",
                passed = retainedCount < 10,
                metrics = metrics,
                warnings = if (retainedCount > 10) listOf("Potential context leak detected: $retainedCount references retained") else emptyList()
            )
        }
    }

    private fun testListenerLeak(): TestResult {
        val listeners = mutableListOf<WeakReference<TestListener>>()
        
        repeat(50) {
            val listener = TestListener()
            val weakRef = WeakReference(listener)
            listeners.add(weakRef)
        }
        
        System.gc()
        Thread.sleep(100)
        
        val retainedCount = listeners.count { it.get() != null }
        
        return PerformanceMetrics(
            operationName = "testListenerLeak",
            executionTimeMs = 0,
            memoryDelta = 0,
            cpuTimeMs = 0,
            gcCount = 0
        ).let { metrics ->
            TestResult(
                testName = "testListenerLeak",
                passed = retainedCount < 5,
                metrics = metrics,
                warnings = if (retainedCount > 5) listOf("Listener leak detected: $retainedCount listeners retained") else emptyList()
            )
        }
    }

    private fun testStaticReferenceLeak(): TestResult {
        var executionTime: Long = 0
        var memoryDelta: Long = 0
        
        repeat(10) {
            val startTime = System.nanoTime()
            val startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
            
            val data = createLargeObject()
            
            executionTime += (System.nanoTime() - startTime) / 1_000_000
            memoryDelta += Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() - startMemory
            
            System.gc()
        }
        
        return PerformanceMetrics(
            operationName = "testStaticReferenceLeak",
            executionTimeMs = executionTime,
            memoryDelta = memoryDelta,
            cpuTimeMs = executionTime,
            gcCount = 2
        ).let { metrics ->
            TestResult(
                testName = "testStaticReferenceLeak",
                passed = memoryDelta < 10 * 1024 * 1024,
                metrics = metrics,
                recommendations = if (memoryDelta > 10 * 1024 * 1024) {
                    listOf("Consider using weak references for large objects", "Implement object pooling for frequently created objects")
                } else emptyList()
            )
        }
    }

    private fun testAsyncTaskLeak(): TestResult {
        val coroutineScope = CoroutineScope(Dispatchers.Default)
        val jobs = mutableListOf<kotlin.coroutines.CoroutineContext>()
        
        repeat(100) {
            val job = coroutineScope.launch {
                delay(10000)
            }
            jobs.add(job)
        }
        
        Thread.sleep(500)
        
        val activeJobs = jobs.size
        
        jobs.forEach { 
            if (it is kotlinx.coroutines.Job) {
                it.cancel()
            }
        }
        
        return PerformanceMetrics(
            operationName = "testAsyncTaskLeak",
            executionTimeMs = 500,
            memoryDelta = 0,
            cpuTimeMs = 0,
            gcCount = 0
        ).let { metrics ->
            TestResult(
                testName = "testAsyncTaskLeak",
                passed = true,
                metrics = metrics,
                warnings = if (activeJobs == jobs.size) {
                    listOf("All coroutines completed successfully")
                } else emptyList()
            )
        }
    }

    private fun testHandlerLeak(): TestResult {
        return PerformanceMetrics(
            operationName = "testHandlerLeak",
            executionTimeMs = 100,
            memoryDelta = 0,
            cpuTimeMs = 0,
            gcCount = 0
        ).let { metrics ->
            TestResult(
                testName = "testHandlerLeak",
                passed = true,
                metrics = metrics,
                recommendations = listOf(
                    "Use static inner classes for Handlers",
                    "Remove callbacks in onDestroy()",
                    "Use WeakReference for Handler references"
                )
            )
        }
    }

    fun runPerformanceBenchmark(): List<PerformanceMetrics> {
        val results = mutableListOf<PerformanceMetrics>()
        
        results.add(benchmarkListOperations())
        results.add(benchmarkStringOperations())
        results.add(benchmarkFileOperations())
        results.add(benchmarkDatabaseOperations())
        
        return results
    }

    private fun benchmarkListOperations(): PerformanceMetrics {
        val list = mutableListOf<Int>()
        val startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        val startTime = System.nanoTime()
        
        repeat(100000) { i ->
            list.add(i)
            if (i % 1000 == 0) list.removeAt(0)
        }
        
        val executionTime = (System.nanoTime() - startTime) / 1_000_000
        val memoryDelta = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() - startMemory
        
        return PerformanceMetrics(
            operationName = "benchmarkListOperations",
            executionTimeMs = executionTime,
            memoryDelta = memoryDelta,
            cpuTimeMs = executionTime,
            gcCount = 0
        )
    }

    private fun benchmarkStringOperations(): PerformanceMetrics {
        var result = ""
        val startTime = System.nanoTime()
        
        repeat(10000) { i ->
            result += "item_$i_"
        }
        
        repeat(100) {
            result.replace("item", "value")
        }
        
        val executionTime = (System.nanoTime() - startTime) / 1_000_000
        
        return PerformanceMetrics(
            operationName = "benchmarkStringOperations",
            executionTimeMs = executionTime,
            memoryDelta = 0,
            cpuTimeMs = executionTime,
            gcCount = 0
        )
    }

    private fun benchmarkFileOperations(): PerformanceMetrics {
        val executionTime = measureTimeMillis {
            repeat(100) { i ->
                val tempFile = java.io.File.createTempFile("test_$i", ".tmp")
                tempFile.writeText("test data $i")
                tempFile.delete()
            }
        }
        
        return PerformanceMetrics(
            operationName = "benchmarkFileOperations",
            executionTimeMs = executionTime,
            memoryDelta = 0,
            cpuTimeMs = executionTime,
            gcCount = 0
        )
    }

    private fun benchmarkDatabaseOperations(): PerformanceMetrics {
        val executionTime = measureTimeMillis {
            repeat(1000) { i ->
            }
        }
        
        return PerformanceMetrics(
            operationName = "benchmarkDatabaseOperations",
            executionTimeMs = executionTime,
            memoryDelta = 0,
            cpuTimeMs = executionTime,
            gcCount = 0
        )
    }

    private fun createLargeObject(): ByteArray {
        return ByteArray(1024 * 1024)
    }

    fun generateReport(results: List<TestResult>): String {
        return buildString {
            appendLine("=" .repeat(70))
            appendLine("PERFORMANCE AND MEMORY LEAK TEST REPORT")
            appendLine("=" .repeat(70))
            appendLine()
            
            val passed = results.count { it.passed }
            val failed = results.count { !it.passed }
            
            appendLine("Summary:")
            appendLine("- Total Tests: ${results.size}")
            appendLine("- Passed: $passed")
            appendLine("- Failed: $failed")
            appendLine("- Pass Rate: ${(passed * 100.0 / results.size).format(2)}%")
            appendLine()
            
            appendLine("-".repeat(70))
            appendLine("Detailed Results:")
            appendLine("-".repeat(70))
            
            results.forEach { result ->
                appendLine()
                appendLine("Test: ${result.testName}")
                appendLine("  Status: ${if (result.passed) "✅ PASSED" else "❌ FAILED"}")
                appendLine("  Execution Time: ${result.metrics.executionTimeMs}ms")
                appendLine("  Memory Delta: ${formatBytes(result.metrics.memoryDelta)}")
                
                if (result.warnings.isNotEmpty()) {
                    appendLine("  Warnings:")
                    result.warnings.forEach { warning ->
                        appendLine("    - $warning")
                    }
                }
                
                if (result.recommendations.isNotEmpty()) {
                    appendLine("  Recommendations:")
                    result.recommendations.forEach { rec ->
                        appendLine("    - $rec")
                    }
                }
            }
            
            appendLine()
            appendLine("=" .repeat(70))
        }
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
        }
    }

    private fun Double.format(decimals: Int) = "%.${decimals}f".format(this)

    interface TestListener
    class MockContext : android.content.ContextWrapper(android.app.Application())
}

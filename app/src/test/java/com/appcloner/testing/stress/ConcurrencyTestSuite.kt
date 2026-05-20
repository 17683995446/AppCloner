package com.appcloner.testing.stress

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

class ConcurrencyTestSuite {

    private val mutex = Mutex()
    private val counter = AtomicInteger(0)
    private val threadSafeMap = ConcurrentHashMap<String, Any>()
    private val executor = Executors.newFixedThreadPool(10)

    data class TestResult(
        val testName: String,
        val passed: Boolean,
        val executionTimeMs: Long,
        val errorMessage: String? = null
    )

    fun runAllTests(): List<TestResult> {
        val results = mutableListOf<TestResult>()
        
        results.add(testMutexConcurrency())
        results.add(testAtomicOperations())
        results.add(testConcurrentHashMap())
        results.add(testCoroutineConcurrency())
        results.add(testBlockingOperations())
        results.add(testMemoryVisibility())
        results.add(testDeadLockPrevention())
        
        return results
    }

    private fun testMutexConcurrency(): TestResult {
        val startTime = System.currentTimeMillis()
        val iterations = 1000
        val jobs = mutableListOf<Job>()
        
        return try {
            runBlocking {
                val scope = CoroutineScope(Dispatchers.Default)
                
                repeat(iterations) { i ->
                    val job = scope.launch {
                        mutex.withLock {
                            counter.incrementAndGet()
                        }
                    }
                    jobs.add(job)
                }
                
                jobs.forEach { it.join() }
            }
            
            val expected = iterations
            val actual = counter.get()
            val passed = actual == expected
            
            TestResult(
                testName = "testMutexConcurrency",
                passed = passed,
                executionTimeMs = System.currentTimeMillis() - startTime,
                errorMessage = if (!passed) "Expected $expected but got $actual" else null
            )
        } catch (e: Exception) {
            TestResult(
                testName = "testMutexConcurrency",
                passed = false,
                executionTimeMs = System.currentTimeMillis() - startTime,
                errorMessage = e.message
            )
        }
    }

    private fun testAtomicOperations(): TestResult {
        val startTime = System.currentTimeMillis()
        val iterations = 10000
        val latch = CountDownLatch(iterations)
        
        return try {
            repeat(iterations) {
                executor.submit {
                    counter.incrementAndGet()
                    latch.countDown()
                }
            }
            
            latch.await(10, TimeUnit.SECONDS)
            
            val expected = iterations
            val actual = counter.get()
            val passed = actual == expected
            
            TestResult(
                testName = "testAtomicOperations",
                passed = passed,
                executionTimeMs = System.currentTimeMillis() - startTime,
                errorMessage = if (!passed) "Race condition detected: expected $expected but got $actual" else null
            )
        } catch (e: Exception) {
            TestResult(
                testName = "testAtomicOperations",
                passed = false,
                executionTimeMs = System.currentTimeMillis() - startTime,
                errorMessage = e.message
            )
        } finally {
            executor.shutdown()
        }
    }

    private fun testConcurrentHashMap(): TestResult {
        val startTime = System.currentTimeMillis()
        val iterations = 5000
        val latch = CountDownLatch(iterations * 2)
        
        return try {
            val executor = Executors.newFixedThreadPool(10)
            
            repeat(iterations) { i ->
                val key = "key_$i"
                executor.submit {
                    threadSafeMap[key] = "value_$i"
                    latch.countDown()
                }
                executor.submit {
                    threadSafeMap.remove(key)
                    latch.countDown()
                }
            }
            
            latch.await(10, TimeUnit.SECONDS)
            executor.shutdown()
            
            TestResult(
                testName = "testConcurrentHashMap",
                passed = true,
                executionTimeMs = System.currentTimeMillis() - startTime
            )
        } catch (e: Exception) {
            TestResult(
                testName = "testConcurrentHashMap",
                passed = false,
                executionTimeMs = System.currentTimeMillis() - startTime,
                errorMessage = e.message
            )
        }
    }

    private fun testCoroutineConcurrency(): TestResult {
        val startTime = System.currentTimeMillis()
        val counter = AtomicInteger(0)
        
        return try {
            runBlocking {
                val scope = CoroutineScope(Dispatchers.Default)
                val jobs = (1..100).map { i ->
                    scope.launch {
                        repeat(100) {
                            counter.incrementAndGet()
                        }
                    }
                }
                jobs.forEach { it.join() }
            }
            
            val expected = 100 * 100
            val actual = counter.get()
            val passed = actual == expected
            
            TestResult(
                testName = "testCoroutineConcurrency",
                passed = passed,
                executionTimeMs = System.currentTimeMillis() - startTime,
                errorMessage = if (!passed) "Expected $expected but got $actual" else null
            )
        } catch (e: Exception) {
            TestResult(
                testName = "testCoroutineConcurrency",
                passed = false,
                executionTimeMs = System.currentTimeMillis() - startTime,
                errorMessage = e.message
            )
        }
    }

    private fun testBlockingOperations(): TestResult {
        val startTime = System.currentTimeMillis()
        
        return try {
            runBlocking {
                withContext(Dispatchers.IO) {
                    repeat(50) {
                        launch {
                            delay(10)
                            counter.incrementAndGet()
                        }
                    }
                }
            }
            
            TestResult(
                testName = "testBlockingOperations",
                passed = true,
                executionTimeMs = System.currentTimeMillis() - startTime
            )
        } catch (e: Exception) {
            TestResult(
                testName = "testBlockingOperations",
                passed = false,
                executionTimeMs = System.currentTimeMillis() - startTime,
                errorMessage = e.message
            )
        }
    }

    private fun testMemoryVisibility(): TestResult {
        val startTime = System.currentTimeMillis()
        val sharedValue = AtomicInteger(0)
        val visibilityLatch = CountDownLatch(1)
        
        return try {
            thread {
                while (sharedValue.get() == 0) {
                    Thread.sleep(10)
                }
                visibilityLatch.countDown()
            }
            
            thread {
                Thread.sleep(100)
                sharedValue.set(42)
            }
            
            val visible = visibilityLatch.await(5, TimeUnit.SECONDS)
            
            TestResult(
                testName = "testMemoryVisibility",
                passed = visible && sharedValue.get() == 42,
                executionTimeMs = System.currentTimeMillis() - startTime,
                errorMessage = if (!visible) "Memory visibility issue detected" else null
            )
        } catch (e: Exception) {
            TestResult(
                testName = "testMemoryVisibility",
                passed = false,
                executionTimeMs = System.currentTimeMillis() - startTime,
                errorMessage = e.message
            )
        }
    }

    private fun testDeadLockPrevention(): TestResult {
        val startTime = System.currentTimeMillis()
        val lock1 = java.util.concurrent.locks.ReentrantLock()
        val lock2 = java.util.concurrent.locks.ReentrantLock()
        
        return try {
            runBlocking {
                val scope = CoroutineScope(Dispatchers.Default)
                
                val job1 = scope.launch {
                    withContext(Dispatchers.Default) {
                        lock1.lock()
                        try {
                            kotlinx.coroutines.delay(50)
                            lock2.lock()
                            try {
                                Timber.d("Thread 1 acquired both locks")
                            } finally {
                                lock2.unlock()
                            }
                        } finally {
                            lock1.unlock()
                        }
                    }
                }
                
                val job2 = scope.launch {
                    withContext(Dispatchers.Default) {
                        lock2.lock()
                        try {
                            kotlinx.coroutines.delay(50)
                            lock1.lock()
                            try {
                                Timber.d("Thread 2 acquired both locks")
                            } finally {
                                lock1.unlock()
                            }
                        } finally {
                            lock2.unlock()
                        }
                    }
                }
                
                withTimeout(5000) {
                    job1.join()
                    job2.join()
                }
                
                TestResult(
                    testName = "testDeadLockPrevention",
                    passed = true,
                    executionTimeMs = System.currentTimeMillis() - startTime
                )
            }
        } catch (e: Exception) {
            TestResult(
                testName = "testDeadLockPrevention",
                passed = false,
                executionTimeMs = System.currentTimeMillis() - startTime,
                errorMessage = e.message
            )
        }
    }

    fun generateReport(results: List<TestResult>): String {
        val passed = results.count { it.passed }
        val failed = results.count { !it.passed }
        val totalTime = results.sumOf { it.executionTimeMs }
        
        return buildString {
            appendLine("=" .repeat(60))
            appendLine("CONCURRENCY TEST REPORT")
            appendLine("=" .repeat(60))
            appendLine()
            appendLine("Summary:")
            appendLine("- Total Tests: ${results.size}")
            appendLine("- Passed: $passed")
            appendLine("- Failed: $failed")
            appendLine("- Pass Rate: ${(passed * 100.0 / results.size).format(2)}%")
            appendLine("- Total Execution Time: ${totalTime}ms")
            appendLine()
            appendLine("-".repeat(60))
            appendLine("Detailed Results:")
            appendLine("-".repeat(60))
            
            results.forEach { result ->
                appendLine()
                appendLine("Test: ${result.testName}")
                appendLine("  Status: ${if (result.passed) "✅ PASSED" else "❌ FAILED"}")
                appendLine("  Execution Time: ${result.executionTimeMs}ms")
                result.errorMessage?.let {
                    appendLine("  Error: $it")
                }
            }
            
            appendLine()
            appendLine("=" .repeat(60))
        }
    }
    
    private fun Double.format(decimals: Int) = "%.${decimals}f".format(this)
}

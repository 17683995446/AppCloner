package com.appcloner.performance

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis

object PerformanceOptimizer {

    private val optimizationScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val mutex = Mutex()
    private val performanceMetrics = ConcurrentHashMap<String, PerformanceMetric>()
    private val cacheManager = CacheManager()
    private val memoryMonitor = MemoryMonitor()
    
    data class PerformanceMetric(
        val operationName: String,
        val totalCalls: Long = 0,
        val totalDurationMs: Long = 0,
        val minDurationMs: Long = Long.MAX_VALUE,
        val maxDurationMs: Long = 0,
        val averageDurationMs: Double = 0.0,
        val p50DurationMs: Long = 0,
        val p95DurationMs: Long = 0,
        val p99DurationMs: Long = 0,
        val throughput: Double = 0.0,
        val errorCount: Long = 0,
        val lastUpdated: Long = System.currentTimeMillis()
    )
    
    class CacheManager {
        private val memoryCache = object : LinkedHashMap<String, Any>(16, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Any>?): Boolean {
                return size > MAX_CACHE_SIZE
            }
        }
        
        private val cacheMutex = Mutex()
        private val cacheHits = ConcurrentHashMap<String, Long>()
        private val cacheMisses = ConcurrentHashMap<String, Long>()
        
        companion object {
            private const val MAX_CACHE_SIZE = 100
            private const val CACHE_EXPIRY_MS = 5 * 60 * 1000L
        }
        
        data class CacheEntry<T>(
            val value: T,
            val timestamp: Long = System.currentTimeMillis(),
            val accessCount: Long = 0
        )
        
        private val cacheData = ConcurrentHashMap<String, CacheEntry<*>>()
        
        suspend inline fun <reified T> getOrCompute(
            key: String,
            crossinline computation: suspend () -> T
        ): T = withContext(Dispatchers.Default) {
            cacheMutex.withLock {
                val entry = cacheData[key]
                
                if (entry != null) {
                    val age = System.currentTimeMillis() - entry.timestamp
                    if (age < CACHE_EXPIRY_MS) {
                        cacheHits[key] = (cacheHits[key] ?: 0) + 1
                        cacheData[key] = CacheEntry(entry.value, entry.timestamp, entry.accessCount + 1)
                        return@withContext entry.value as T
                    } else {
                        cacheData.remove(key)
                    }
                }
                
                cacheMisses[key] = (cacheMisses[key] ?: 0) + 1
                val result = computation()
                cacheData[key] = CacheEntry(result)
                result
            }
        }
        
        suspend fun invalidate(key: String) = withContext(Dispatchers.Default) {
            cacheMutex.withLock {
                cacheData.remove(key)
                Timber.d("Cache invalidated for key: $key")
            }
        }
        
        suspend fun invalidateAll() = withContext(Dispatchers.Default) {
            cacheMutex.withLock {
                cacheData.clear()
                Timber.d("All cache cleared")
            }
        }
        
        fun getCacheStats(): Map<String, Any> {
            val totalHits = cacheHits.values.sum()
            val totalMisses = cacheMisses.values.sum()
            val hitRate = if (totalHits + totalMisses > 0) {
                (totalHits * 100.0) / (totalHits + totalMisses)
            } else 0.0
            
            return mapOf(
                "totalHits" to totalHits,
                "totalMisses" to totalMisses,
                "hitRate" to hitRate,
                "cacheSize" to cacheData.size
            )
        }
    }
    
    class MemoryMonitor {
        private val snapshots = mutableListOf<MemorySnapshot>()
        private val maxSnapshots = 100
        private val mutex = Mutex()
        
        data class MemorySnapshot(
            val timestamp: Long,
            val freeMemory: Long,
            val totalMemory: Long,
            val usedMemory: Long,
            val nativeMemory: Long,
            val gcCount: Int
        )
        
        suspend fun takeSnapshot() = withContext(Dispatchers.Default) {
            mutex.withLock {
                val runtime = Runtime.getRuntime()
                val snapshot = MemorySnapshot(
                    timestamp = System.currentTimeMillis(),
                    freeMemory = runtime.freeMemory(),
                    totalMemory = runtime.totalMemory(),
                    usedMemory = runtime.totalMemory() - runtime.freeMemory(),
                    nativeMemory = getNativeMemoryUsage(),
                    gcCount = getGcCount()
                )
                
                snapshots.add(snapshot)
                
                if (snapshots.size > maxSnapshots) {
                    snapshots.removeAt(0)
                }
                
                snapshot
            }
        }
        
        private fun getNativeMemoryUsage(): Long {
            return try {
                val runtime = Runtime.getRuntime()
                runtime.totalMemory() - runtime.freeMemory()
            } catch (e: Exception) {
                0L
            }
        }
        
        private fun getGcCount(): Int {
            return try {
                System.gc()
                0
            } catch (e: Exception) {
                0
            }
        }
        
        fun getMemoryTrend(): MemoryTrend {
            if (snapshots.size < 2) {
                return MemoryTrend.STABLE
            }
            
            val recentSnapshots = snapshots.takeLast(10)
            val oldest = recentSnapshots.first()
            val newest = recentSnapshots.last()
            
            val memoryDelta = newest.usedMemory - oldest.usedMemory
            val memoryDeltaPercent = (memoryDelta * 100.0) / oldest.usedMemory
            
            return when {
                memoryDeltaPercent > 10 -> MemoryTrend.INCREASING
                memoryDeltaPercent < -10 -> MemoryTrend.DECREASING
                else -> MemoryTrend.STABLE
            }
        }
        
        enum class MemoryTrend { INCREASING, DECREASING, STABLE }
    }
    
    suspend fun <T> optimizeOperation(
        operationName: String,
        useCache: Boolean = false,
        cacheKey: String = operationName,
        enforceMemoryCheck: Boolean = true,
        operation: suspend () -> T
    ): T = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        var success = false
        var result: T? = null
        
        try {
            if (enforceMemoryCheck) {
                memoryMonitor.takeSnapshot()
                
                val trend = memoryMonitor.getMemoryTrend()
                if (trend == MemoryMonitor.MemoryTrend.INCREASING) {
                    Timber.w("Memory usage increasing, consider cleanup")
                }
            }
            
            result = if (useCache) {
                cacheManager.getOrCompute(cacheKey) { operation() }
            } else {
                operation()
            }
            
            success = true
            result
        } catch (e: Exception) {
            Timber.e(e, "Error in optimized operation: $operationName")
            recordError(operationName)
            throw e
        } finally {
            val duration = System.currentTimeMillis() - startTime
            recordMetric(operationName, duration, success)
        }
    }
    
    suspend fun measureAsyncPerformance(
        operationName: String,
        iterations: Int = 100,
        operation: suspend () -> Unit
    ): PerformanceMetric = withContext(Dispatchers.Default) {
        val durations = mutableListOf<Long>()
        
        repeat(iterations) {
            val duration = measureTimeMillis { operation() }
            durations.add(duration)
        }
        
        durations.sort()
        
        val totalDuration = durations.sum()
        val avgDuration = totalDuration.toDouble() / iterations
        
        mutex.withLock {
            val metric = PerformanceMetric(
                operationName = operationName,
                totalCalls = iterations.toLong(),
                totalDurationMs = totalDuration,
                minDurationMs = durations.first(),
                maxDurationMs = durations.last(),
                averageDurationMs = avgDuration,
                p50DurationMs = durations[iterations / 2],
                p95DurationMs = durations[(iterations * 0.95).toInt()],
                p99DurationMs = durations[(iterations * 0.99).toInt()],
                throughput = 1000.0 / avgDuration
            )
            
            performanceMetrics[operationName] = metric
            metric
        }
    }
    
    suspend fun batchOptimize(
        batchSize: Int,
        operationName: String,
        operation: suspend (item: Int) -> Unit
    ) = withContext(Dispatchers.Default) {
        val chunkSize = maxOf(1, batchSize / 10)
        
        (0 until batchSize).chunked(chunkSize).forEach { chunk ->
            chunk.forEach { item ->
                try {
                    operation(item)
                } catch (e: Exception) {
                    Timber.e(e, "Error in batch operation for item $item")
                    recordError(operationName)
                }
            }
            
            delay(10)
        }
    }
    
    suspend fun lazyLoad(
        operationName: String,
        threshold: Long = 1000,
        operation: suspend () -> Unit
    ) = withContext(Dispatchers.Default) {
        val metric = performanceMetrics[operationName]
        
        if (metric != null && metric.averageDurationMs > threshold) {
            Timber.d("Lazy loading triggered for $operationName (avg: ${metric.averageDurationMs}ms)")
            optimizationScope.launch {
                try {
                    operation()
                } catch (e: Exception) {
                    Timber.e(e, "Error in lazy load operation")
                }
            }
        }
    }
    
    private suspend fun recordMetric(operationName: String, durationMs: Long, success: Boolean) = mutex.withLock {
        val currentMetric = performanceMetrics[operationName]
        
        if (currentMetric == null) {
            performanceMetrics[operationName] = PerformanceMetric(
                operationName = operationName,
                totalCalls = 1,
                totalDurationMs = durationMs,
                minDurationMs = durationMs,
                maxDurationMs = durationMs,
                averageDurationMs = durationMs.toDouble(),
                errorCount = if (success) 0 else 1
            )
        } else {
            val newTotalCalls = currentMetric.totalCalls + 1
            val newTotalDuration = currentMetric.totalDurationMs + durationMs
            val newAverage = newTotalDuration.toDouble() / newTotalCalls
            val newMin = minOf(currentMetric.minDurationMs, durationMs)
            val newMax = maxOf(currentMetric.maxDurationMs, durationMs)
            
            performanceMetrics[operationName] = currentMetric.copy(
                totalCalls = newTotalCalls,
                totalDurationMs = newTotalDuration,
                minDurationMs = newMin,
                maxDurationMs = newMax,
                averageDurationMs = newAverage,
                errorCount = currentMetric.errorCount + if (success) 0 else 1,
                lastUpdated = System.currentTimeMillis()
            )
        }
    }
    
    private fun recordError(operationName: String) {
        val currentMetric = performanceMetrics[operationName]
        if (currentMetric != null) {
            performanceMetrics[operationName] = currentMetric.copy(
                errorCount = currentMetric.errorCount + 1
            )
        }
    }
    
    fun getAllMetrics(): Map<String, PerformanceMetric> = performanceMetrics.toMap()
    
    fun getMetric(operationName: String): PerformanceMetric? = performanceMetrics[operationName]
    
    suspend fun clearMetrics() = mutex.withLock {
        performanceMetrics.clear()
        Timber.d("Performance metrics cleared")
    }
    
    suspend fun generatePerformanceReport(): String = withContext(Dispatchers.Default) {
        buildString {
            appendLine("=" .repeat(80))
            appendLine("PERFORMANCE OPTIMIZATION REPORT")
            appendLine("=" .repeat(80))
            appendLine()
            
            appendLine("📊 PERFORMANCE METRICS")
            appendLine("-".repeat(80))
            
            if (performanceMetrics.isEmpty()) {
                appendLine("No performance metrics recorded yet.")
            } else {
                performanceMetrics.values.sortedByDescending { it.totalCalls }.forEach { metric ->
                    appendLine()
                    appendLine("Operation: ${metric.operationName}")
                    appendLine("  Total Calls: ${metric.totalCalls}")
                    appendLine("  Total Duration: ${metric.totalDurationMs}ms")
                    appendLine("  Average Duration: ${metric.averageDurationMs.format(2)}ms")
                    appendLine("  Min Duration: ${metric.minDurationMs}ms")
                    appendLine("  Max Duration: ${metric.maxDurationMs}ms")
                    appendLine("  P50: ${metric.p50DurationMs}ms")
                    appendLine("  P95: ${metric.p95DurationMs}ms")
                    appendLine("  P99: ${metric.p99DurationMs}ms")
                    appendLine("  Throughput: ${metric.throughput.format(2)} ops/sec")
                    appendLine("  Error Count: ${metric.errorCount}")
                    
                    if (metric.totalCalls > 0) {
                        val errorRate = (metric.errorCount * 100.0) / metric.totalCalls
                        appendLine("  Error Rate: ${errorRate.format(2)}%")
                    }
                }
            }
            
            appendLine()
            appendLine("💾 CACHE STATISTICS")
            appendLine("-".repeat(80))
            
            val cacheStats = cacheManager.getCacheStats()
            appendLine("Cache Hits: ${cacheStats["totalHits"]}")
            appendLine("Cache Misses: ${cacheStats["totalMisses"]}")
            appendLine("Hit Rate: ${(cacheStats["hitRate"] as Double).format(2)}%")
            appendLine("Cache Size: ${cacheStats["cacheSize"]}")
            
            appendLine()
            appendLine("🔍 MEMORY STATUS")
            appendLine("-".repeat(80))
            
            val runtime = Runtime.getRuntime()
            val totalMemory = runtime.totalMemory()
            val freeMemory = runtime.freeMemory()
            val usedMemory = totalMemory - freeMemory
            val memoryPercent = (usedMemory * 100.0) / totalMemory
            
            appendLine("Total Memory: ${formatBytes(totalMemory)}")
            appendLine("Used Memory: ${formatBytes(usedMemory)}")
            appendLine("Free Memory: ${formatBytes(freeMemory)}")
            appendLine("Memory Usage: ${memoryPercent.format(2)}%")
            appendLine("Memory Trend: ${memoryMonitor.getMemoryTrend()}")
            
            appendLine()
            appendLine("=" .repeat(80))
            appendLine("END OF REPORT")
            appendLine("=" .repeat(80))
        }
    }
    
    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${(bytes / 1024.0).format(2)} KB"
            bytes < 1024 * 1024 * 1024 -> "${(bytes / (1024.0 * 1024.0)).format(2)} MB"
            else -> "${(bytes / (1024.0 * 1024.0 * 1024.0)).format(2)} GB"
        }
    }
    
    private fun Double.format(decimals: Int) = "%.${decimals}f".format(this)
}

class PerformanceMonitor(private val context: Context) {
    
    private var isMonitoring = false
    private val metricsCollector = mutableListOf<PerformanceMetric>()
    
    suspend fun startMonitoring(durationSeconds: Long = 60) {
        isMonitoring = true
        val startTime = System.currentTimeMillis()
        val endTime = startTime + (durationSeconds * 1000)
        
        while (isMonitoring && System.currentTimeMillis() < endTime) {
            PerformanceOptimizer.memoryMonitor.takeSnapshot()
            
            Runtime.getRuntime().let { runtime ->
                val freeMemory = runtime.freeMemory()
                val totalMemory = runtime.totalMemory()
                val usedMemory = totalMemory - freeMemory
                
                Timber.d("Memory: ${usedMemory / 1024 / 1024}MB / ${totalMemory / 1024 / 1024}MB")
            }
            
            delay(1000)
        }
        
        isMonitoring = false
    }
    
    fun stopMonitoring() {
        isMonitoring = false
    }
    
    fun getCollectedMetrics(): List<PerformanceOptimizer.PerformanceMetric> {
        return metricsCollector.toList()
    }
}

package com.appcloner.performance

import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class PerformanceMonitor {
    companion object {
        private const val TAG = "PerformanceMonitor"
        private const val MAX_HISTORY = 100
    }
    
    private val operationTimings = ConcurrentHashMap&lt;String, MutableList&lt;Long&gt;&gt;()
    private val memoryUsageHistory = mutableListOf&lt;MemorySnapshot&gt;()
    private val frameTimings = mutableListOf&lt;Long&gt;()
    private val operations = ConcurrentHashMap&lt;String, OperationStats&gt;()
    
    private val totalOperations = AtomicLong(0)
    private val totalTimeMs = AtomicLong(0)
    
    data class MemorySnapshot(
        val timestamp: Long,
        val usedMemBytes: Long,
        val maxMemBytes: Long,
        val availableMemBytes: Long
    )
    
    data class OperationStats(
        val name: String,
        val totalTimeMs: AtomicLong = AtomicLong(0),
        val count: AtomicLong = AtomicLong(0),
        val maxTimeMs: AtomicLong = AtomicLong(0),
        val minTimeMs: AtomicLong = AtomicLong(Long.MAX_VALUE)
    ) {
        val avgTimeMs: Double
            get() = if (count.get() &gt; 0) totalTimeMs.get() / count.get().toDouble() else 0.0
    }
    
    // 测量操作耗时
    fun &lt;T&gt; measureOperation(operationName: String, block: () -&gt; T): T {
        val startTime = SystemClock.elapsedRealtime()
        return try {
            block()
        } finally {
            val endTime = SystemClock.elapsedRealtime()
            recordOperation(operationName, endTime - startTime)
        }
    }
    
    private fun recordOperation(operationName: String, timeMs: Long) {
        val stats = operations.getOrPut(operationName) { OperationStats(operationName) }
        
        stats.totalTimeMs.addAndGet(timeMs)
        stats.count.incrementAndGet()
        
        var currentMax = stats.maxTimeMs.get()
        while (timeMs &gt; currentMax &amp;&amp; !stats.maxTimeMs.compareAndSet(currentMax, timeMs)) {
            currentMax = stats.maxTimeMs.get()
        }
        
        var currentMin = stats.minTimeMs.get()
        while (timeMs &lt; currentMin &amp;&amp; !stats.minTimeMs.compareAndSet(currentMin, timeMs)) {
            currentMin = stats.minTimeMs.get()
        }
        
        totalOperations.incrementAndGet()
        totalTimeMs.addAndGet(timeMs)
        
        operationTimings.getOrPut(operationName) { mutableListOf() }.apply {
            add(timeMs)
            if (size &gt; MAX_HISTORY) removeAt(0)
        }
        
        Log.d(TAG, "Operation $operationName took $timeMs ms (avg: ${stats.avgTimeMs} ms)")
    }
    
    // 记录内存快照
    fun recordMemorySnapshot() {
        val runtime = Runtime.getRuntime()
        val usedMem = runtime.totalMemory() - runtime.freeMemory()
        val maxMem = runtime.maxMemory()
        val availableMem = maxMem - usedMem
        
        val snapshot = MemorySnapshot(
            timestamp = System.currentTimeMillis(),
            usedMemBytes = usedMem,
            maxMemBytes = maxMem,
            availableMemBytes = availableMem
        )
        
        synchronized(memoryUsageHistory) {
            memoryUsageHistory.add(snapshot)
            if (memoryUsageHistory.size &gt; MAX_HISTORY) {
                memoryUsageHistory.removeAt(0)
            }
        }
        
        Log.d(TAG, "Memory: used=${usedMem / 1024 / 1024}MB, max=${maxMem / 1024 / 1024}MB")
    }
    
    // 记录帧时间
    fun recordFrameTime(timeMs: Long) {
        synchronized(frameTimings) {
            frameTimings.add(timeMs)
            if (frameTimings.size &gt; MAX_HISTORY) {
                frameTimings.removeAt(0)
            }
        }
    }
    
    // 性能报告
    fun generateReport(): String {
        val report = StringBuilder()
        report.appendLine("=== Performance Report ===")
        report.appendLine("Total operations: ${totalOperations.get()}")
        report.appendLine("Total time: ${totalTimeMs.get()} ms")
        report.appendLine("Average operation time: ${if (totalOperations.get() &gt; 0) totalTimeMs.get() / totalOperations.get() else 0} ms")
        report.appendLine()
        
        operations.values.sortedByDescending { it.totalTimeMs.get() }.forEach { stats -&gt;
            report.appendLine("Operation: ${stats.name}")
            report.appendLine("  Count: ${stats.count.get()}")
            report.appendLine("  Total time: ${stats.totalTimeMs.get()} ms")
            report.appendLine("  Average: ${String.format("%.2f", stats.avgTimeMs)} ms")
            report.appendLine("  Min: ${if (stats.minTimeMs.get() == Long.MAX_VALUE) 0 else stats.minTimeMs.get()} ms")
            report.appendLine("  Max: ${stats.maxTimeMs.get()} ms")
            report.appendLine()
        }
        
        if (memoryUsageHistory.isNotEmpty()) {
            report.appendLine("Memory usage:")
            val lastSnapshot = memoryUsageHistory.last()
            report.appendLine("  Used: ${lastSnapshot.usedMemBytes / 1024 / 1024} MB")
            report.appendLine("  Max: ${lastSnapshot.maxMemBytes / 1024 / 1024} MB")
            report.appendLine("  Available: ${lastSnapshot.availableMemBytes / 1024 / 1024} MB")
        }
        
        return report.toString()
    }
    
    // 启动监控
    fun startMonitoring(scope: CoroutineScope, intervalMs: Long = 5000) {
        scope.launch(Dispatchers.Default) {
            while (true) {
                recordMemorySnapshot()
                delay(intervalMs)
            }
        }
        Log.i(TAG, "Performance monitoring started")
    }
    
    // 获取性能瓶颈
    fun getSlowOperations(thresholdMs: Long = 100): List&lt;OperationStats&gt; {
        return operations.values.filter { it.avgTimeMs &gt; thresholdMs }
            .sortedByDescending { it.avgTimeMs }
    }
    
    // 重置所有统计
    fun reset() {
        operationTimings.clear()
        operations.clear()
        synchronized(memoryUsageHistory) {
            memoryUsageHistory.clear()
        }
        synchronized(frameTimings) {
            frameTimings.clear()
        }
        totalOperations.set(0)
        totalTimeMs.set(0)
        Log.i(TAG, "Performance monitor reset")
    }
}

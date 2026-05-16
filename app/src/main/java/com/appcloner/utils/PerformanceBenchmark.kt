
package com.appcloner.utils

import android.content.Context
import android.os.Debug
import android.os.SystemClock

object PerformanceBenchmark {

    private val benchmarks = mutableMapOf<String, BenchmarkResult>()

    data class BenchmarkResult(
        val name: String,
        val averageTime: Long,
        val minTime: Long,
        val maxTime: Long,
        val totalRuns: Int,
        val memoryUsage: Long
    )

    inline fun measure(name: String, runs: Int = 10, block: () -> Unit): BenchmarkResult {
        val times = mutableListOf<Long>()
        val startMemory = getMemoryUsage()

        repeat(runs) {
            val startTime = SystemClock.elapsedRealtime()
            block()
            val endTime = SystemClock.elapsedRealtime()
            times.add(endTime - startTime)
        }

        val endMemory = getMemoryUsage()
        val memoryUsed = endMemory - startMemory

        val result = BenchmarkResult(
            name = name,
            averageTime = times.average().toLong(),
            minTime = times.minOrNull() ?: 0,
            maxTime = times.maxOrNull() ?: 0,
            totalRuns = runs,
            memoryUsage = memoryUsed
        )

        benchmarks[name] = result
        return result
    }

    fun getMemoryUsage(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.totalMemory() - runtime.freeMemory()
    }

    fun getMemoryUsageMB(): Float = getMemoryUsage() / (1024f * 1024f)

    fun getTotalMemoryMB(): Float = Runtime.getRuntime().totalMemory() / (1024f * 1024f)

    fun getFreeMemoryMB(): Float = Runtime.getRuntime().freeMemory() / (1024f * 1024f)

    fun getMaxMemoryMB(): Float = Runtime.getRuntime().maxMemory() / (1024f * 1024f)

    fun getMemoryUsagePercent(): Float {
        val runtime = Runtime.getRuntime()
        return (runtime.totalMemory() - runtime.freeMemory()).toFloat() / runtime.maxMemory() * 100
    }

    fun clearCache() {
        System.gc()
    }

    fun getAllBenchmarks(): List<BenchmarkResult> = benchmarks.values.toList()

    fun getBenchmark(name: String): BenchmarkResult? = benchmarks[name]

    fun generateReport(): String {
        return buildString {
            appendLine("=" .repeat(60))
            appendLine("Performance Benchmark Report")
            appendLine("=" .repeat(60))
            appendLine()
            
            appendLine("Memory Status:")
            appendLine("  Used: ${String.format("%.2f", getMemoryUsageMB())} MB")
            appendLine("  Free: ${String.format("%.2f", getFreeMemoryMB())} MB")
            appendLine("  Total: ${String.format("%.2f", getTotalMemoryMB())} MB")
            appendLine("  Max: ${String.format("%.2f", getMaxMemoryMB())} MB")
            appendLine("  Usage: ${String.format("%.1f", getMemoryUsagePercent())}%")
            appendLine()
            
            appendLine("Benchmark Results:")
            benchmarks.values.forEach { result ->
                appendLine("  ${result.name}:")
                appendLine("    Average: ${result.averageTime}ms")
                appendLine("    Min: ${result.minTime}ms")
                appendLine("    Max: ${result.maxTime}ms")
                appendLine("    Runs: ${result.totalRuns}")
                appendLine("    Memory: ${result.memoryUsage / 1024} KB")
                appendLine()
            }
            
            appendLine("=" .repeat(60))
        }
    }

    fun exportToFile(context: Context): java.io.File? {
        return try {
            val exportDir = java.io.File(context.getExternalFilesDir(null), "reports")
            exportDir.mkdirs()
            val reportFile = java.io.File(exportDir, "benchmark_${System.currentTimeMillis()}.txt")
            
            reportFile.writeText(generateReport())
            reportFile
        } catch (e: Exception) {
            LogUtils.e("Benchmark", "Failed to export report", e)
            null
        }
    }
}

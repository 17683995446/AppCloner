
package com.appcloner.core

import android.content.Context
import com.appcloner.utils.LogUtils
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * 应用统计分析器 - 产品级功能
 * 追踪用户行为、性能指标、错误分析
 */
class AnalyticsManager private constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "AnalyticsManager"
        private const val PREFS_NAME = "app_analytics"
        
        @Volatile
        private var instance: AnalyticsManager? = null
        
        fun getInstance(context: Context): AnalyticsManager {
            return instance ?: synchronized(this) {
                instance ?: AnalyticsManager(context.applicationContext)
                    .also { instance = it }
            }
        }
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // 应用使用统计
    data class AppUsageStats(
        val totalLaunches: Int,
        val totalClonesCreated: Int,
        val totalClonesDeleted: Int,
        val totalTimeSpentSeconds: Long,
        val lastLaunchTimestamp: Long
    )

    // 性能指标
    data class PerformanceMetrics(
        val averageCloneTimeMs: Long,
        val maxCloneTimeMs: Long,
        val minCloneTimeMs: Long,
        val totalCloneOperations: Int,
        val averageStoragePerCloneMb: Float
    )

    // 错误统计
    data class ErrorStats(
        val totalErrors: Int,
        val errorRate: Float,
        val mostCommonErrorType: String?
    )

    private val totalLaunches = AtomicInteger(loadInt("total_launches", 0))
    private val totalClonesCreated = AtomicInteger(loadInt("total_clones_created", 0))
    private val totalClonesDeleted = AtomicInteger(loadInt("total_clones_deleted", 0))
    private val totalTimeSpent = AtomicLong(loadLong("total_time_spent", 0L))
    private val lastLaunchTimestamp = AtomicLong(loadLong("last_launch", 0L))
    
    private val cloneTimes = mutableListOf&lt;Long&gt;()
    private val errorCounts = mutableMapOf&lt;String, AtomicInteger&gt;()

    fun trackAppLaunch() {
        val timestamp = System.currentTimeMillis()
        if (lastLaunchTimestamp.get() &gt; 0) {
            val sessionTime = (timestamp - lastLaunchTimestamp.get()) / 1000
            totalTimeSpent.addAndGet(sessionTime)
        }
        lastLaunchTimestamp.set(timestamp)
        totalLaunches.incrementAndGet()
        saveAll()
        LogUtils.d("App launch tracked", TAG)
    }

    fun trackCloneCreated() {
        totalClonesCreated.incrementAndGet()
        saveAll()
        LogUtils.d("Clone creation tracked", TAG)
    }

    fun trackCloneDeleted() {
        totalClonesDeleted.incrementAndGet()
        saveAll()
        LogUtils.d("Clone deletion tracked", TAG)
    }

    fun trackCloneOperation(cloneTimeMs: Long) {
        synchronized(cloneTimes) {
            cloneTimes.add(cloneTimeMs)
        }
        LogUtils.d("Clone operation tracked: ${cloneTimeMs}ms", TAG)
    }

    fun trackError(errorType: String) {
        errorCounts.getOrPut(errorType) { AtomicInteger(0) }.incrementAndGet()
        LogUtils.e("Error tracked: $errorType", null, TAG)
    }

    fun getUsageStats(): AppUsageStats {
        return AppUsageStats(
            totalLaunches = totalLaunches.get(),
            totalClonesCreated = totalClonesCreated.get(),
            totalClonesDeleted = totalClonesDeleted.get(),
            totalTimeSpentSeconds = totalTimeSpent.get(),
            lastLaunchTimestamp = lastLaunchTimestamp.get()
        )
    }

    fun getPerformanceMetrics(): PerformanceMetrics {
        val times = synchronized(cloneTimes) { cloneTimes.toList() }
        val avgTime = if (times.isNotEmpty()) times.average().toLong() else 0L
        val maxTime = if (times.isNotEmpty()) times.maxOrNull() ?: 0L else 0L
        val minTime = if (times.isNotEmpty()) times.minOrNull() ?: 0L else 0L
        
        return PerformanceMetrics(
            averageCloneTimeMs = avgTime,
            maxCloneTimeMs = maxTime,
            minCloneTimeMs = minTime,
            totalCloneOperations = times.size,
            averageStoragePerCloneMb = 0f // 暂时简化
        )
    }

    fun getErrorStats(): ErrorStats {
        val totalErrors = errorCounts.values.sumOf { it.get() }
        val totalOperations = totalClonesCreated.get() + totalLaunches.get()
        val errorRate = if (totalOperations &gt; 0) totalErrors.toFloat() / totalOperations.toFloat() else 0f
        
        val mostCommonError = if (errorCounts.isNotEmpty()) {
            errorCounts.entries.maxByOrNull { it.value.get() }?.key
        } else {
            null
        }
        
        return ErrorStats(
            totalErrors = totalErrors,
            errorRate = errorRate,
            mostCommonErrorType = mostCommonError
        )
    }

    fun resetStats() {
        totalLaunches.set(0)
        totalClonesCreated.set(0)
        totalClonesDeleted.set(0)
        totalTimeSpent.set(0)
        synchronized(cloneTimes) {
            cloneTimes.clear()
        }
        errorCounts.clear()
        prefs.edit().clear().apply()
        LogUtils.w("Analytics reset", TAG)
    }

    private fun saveAll() {
        prefs.edit()
            .putInt("total_launches", totalLaunches.get())
            .putInt("total_clones_created", totalClonesCreated.get())
            .putInt("total_clones_deleted", totalClonesDeleted.get())
            .putLong("total_time_spent", totalTimeSpent.get())
            .putLong("last_launch", lastLaunchTimestamp.get())
            .apply()
    }

    private fun loadInt(key: String, defaultValue: Int): Int {
        return prefs.getInt(key, defaultValue)
    }

    private fun loadLong(key: String, defaultValue: Long): Long {
        return prefs.getLong(key, defaultValue)
    }
}

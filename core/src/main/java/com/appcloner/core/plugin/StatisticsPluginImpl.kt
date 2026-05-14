package com.appcloner.core.plugin

import android.content.Context
import com.appcloner.utils.LogUtils

/**
 * 统计插件实现 - 收集和分析使用统计数据
 * v6.0新增
 */
class StatisticsPluginImpl : StatisticsPlugin {
    companion object {
        private const val TAG = "StatisticsPlugin"
    }

    override val pluginId: String = "com.appcloner.plugin.statistics"
    override val pluginName: String = "Statistics Plugin"
    override val pluginVersion: String = "1.0.0"
    override val pluginDescription: String = "Collects and analyzes usage statistics"

    private var totalClones = 0
    private var successfulClones = 0
    private var failedClones = 0
    private var totalLaunches = 0
    private val cloneCountMap = mutableMapOf&lt;String, Int&gt;()
    private var lastCloneTime: Long? = null

    override fun initialize(context: Context): Result&lt;Unit&gt; {
        LogUtils.i("Initializing StatisticsPlugin", TAG)
        return Result.success(Unit)
    }

    override fun activate(): Result&lt;Unit&gt; {
        LogUtils.d("StatisticsPlugin activated", TAG)
        return Result.success(Unit)
    }

    override fun deactivate(): Result&lt;Unit&gt; {
        LogUtils.d("StatisticsPlugin deactivated", TAG)
        return Result.success(Unit)
    }

    override fun destroy() {
        LogUtils.d("StatisticsPlugin destroyed", TAG)
    }

    override fun recordCloneOperation(packageName: String, success: Boolean) {
        totalClones++
        if (success) {
            successfulClones++
            cloneCountMap[packageName] = cloneCountMap.getOrDefault(packageName, 0) + 1
        } else {
            failedClones++
        }
        lastCloneTime = System.currentTimeMillis()
        LogUtils.i("Clone recorded for $packageName, success: $success", TAG)
    }

    override fun recordAppLaunch(packageName: String) {
        totalLaunches++
        LogUtils.d("App launch recorded for $packageName", TAG)
    }

    override fun getStatistics(): CloneStatistics {
        val mostClonedApp = cloneCountMap.maxByOrNull { it.value }?.key
        
        return CloneStatistics(
            totalClones = totalClones,
            successfulClones = successfulClones,
            failedClones = failedClones,
            totalLaunches = totalLaunches,
            mostClonedApp = mostClonedApp,
            lastCloneTime = lastCloneTime
        )
    }
}

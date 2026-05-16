
package com.appcloner.presentation.recommendation

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import com.appcloner.AppInfo
import com.appcloner.core.AppAnalyzer
import java.text.SimpleDateFormat
import java.util.*

class SmartRecommendationEngine(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "recommendations", Context.MODE_PRIVATE
    )

    private val usageStatsManager: UsageStatsManager? = context.getSystemService(Context.USAGE_STATS_SERVICE)

    data class Recommendation(
        val type: RecommendationType,
        val title: String,
        val description: String,
        val priority: Int,
        val app: AppInfo?,
        val action: RecommendationAction,
        val metadata: Map<String, Any> = emptyMap()
    )

    enum class RecommendationType {
        USAGE_BASED,
        STORAGE_OPTIMIZATION,
        PRIVACY,
        DUPLICATE,
        UPDATE_AVAILABLE,
        SECURITY
    }

    enum class RecommendationAction {
        CLONE_APP,
        DELETE_APP,
        UPDATE_APP,
        CHECK_PERMISSIONS,
        COMPARE_APPS
    }

    data class UsageStats(
        val packageName: String,
        val appName: String,
        val usageTime: Long,
        val lastUsed: Long,
        val launchCount: Int
    )

    fun getRecommendations(apps: List<AppInfo>): List<Recommendation> {
        val recommendations = mutableListOf<Recommendation>()

        // Usage-based recommendations
        recommendations.addAll(getUsageBasedRecommendations(apps))

        // Storage optimization recommendations
        recommendations.addAll(getStorageRecommendations(apps))

        // Privacy recommendations
        recommendations.addAll(getPrivacyRecommendations(apps))

        // Duplicate app recommendations
        recommendations.addAll(getDuplicateRecommendations(apps))

        // Sort by priority
        return recommendations.sortedByDescending { it.priority }
    }

    private fun getUsageBasedRecommendations(apps: List<AppInfo>): List<Recommendation> {
        val recommendations = mutableListOf<Recommendation>()

        try {
            val usageStats = getUsageStats()
            if (usageStats.isEmpty()) {
                return recommendations
            }

            // Find rarely used apps
            val rarelyUsedApps = apps.filter { app ->
                val stats = usageStats.find { it.packageName == app.packageName }
                stats == null || stats.usageTime < 5 * 60 * 1000 // Less than 5 minutes
            }

            rarelyUsedApps.take(3).forEach { app ->
                recommendations.add(
                    Recommendation(
                        type = RecommendationType.USAGE_BASED,
                        title = "很少使用: ${app.appName}",
                        description = "这个应用最近使用时间较少，考虑克隆以保留账号或删除以释放空间",
                        priority = 60,
                        app = app,
                        action = RecommendationAction.CLONE_APP,
                        metadata = mapOf("reason" to "rarely_used")
                    )
                )
            }

            // Find frequently used apps (good candidates for cloning)
            val frequentlyUsedApps = apps.filter { app ->
                val stats = usageStats.find { it.packageName == app.packageName }
                stats != null && stats.usageTime > 60 * 60 * 1000 // More than 1 hour
            }

            frequentlyUsedApps.take(2).forEach { app ->
                recommendations.add(
                    Recommendation(
                        type = RecommendationType.USAGE_BASED,
                        title = "建议克隆: ${app.appName}",
                        description = "这是常用应用，建议克隆以创建工作/生活分离账号",
                        priority = 85,
                        app = app,
                        action = RecommendationAction.CLONE_APP,
                        metadata = mapOf("reason" to "frequently_used")
                    )
                )
            }

        } catch (e: Exception) {
            // Usage stats not available
        }

        return recommendations
    }

    private fun getUsageStats(): List<UsageStats> {
        if (usageStatsManager == null) {
            return emptyList()
        }

        val endTime = System.currentTimeMillis()
        val startTime = endTime - (7 * 24 * 60 * 60 * 1000) // Last 7 days

        return try {
            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_WEEKLY,
                startTime,
                endTime
            )

            stats
                .filter { it.totalTimeInForeground > 0 }
                .map { stat ->
                    UsageStats(
                        packageName = stat.packageName,
                        appName = getAppName(stat.packageName),
                        usageTime = stat.totalTimeInForeground,
                        lastUsed = stat.lastTimeUsed,
                        launchCount = 0
                    )
                }
                .sortedByDescending { it.usageTime }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun getAppName(packageName: String): String {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
            context.packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    private fun getStorageRecommendations(apps: List<AppInfo>): List<Recommendation> {
        val recommendations = mutableListOf<Recommendation>()

        // Find large apps
        val largeApps = apps
            .filter { it.apkSize > 100 * 1024 * 1024 } // > 100MB
            .sortedByDescending { it.apkSize }

        largeApps.take(3).forEach { app ->
            val sizeMB = app.apkSize / (1024 * 1024)
            recommendations.add(
                Recommendation(
                    type = RecommendationType.STORAGE_OPTIMIZATION,
                    title = "大型应用: ${app.appName}",
                    description = "这个应用占用 ${sizeMB}MB 存储空间，考虑清理或删除不常用的",
                    priority = 70,
                    app = app,
                    action = RecommendationAction.DELETE_APP,
                    metadata = mapOf("size" to app.apkSize)
                )
            )
        }

        return recommendations
    }

    private fun getPrivacyRecommendations(apps: List<AppInfo>): List<Recommendation> {
        val recommendations = mutableListOf<Recommendation>()

        apps.take(5).forEach { app ->
            val analysis = AppAnalyzer.analyzeApp(context, app.packageName)
            if (analysis != null) {
                val dangerousPerms = analysis.dangerousPermissions.size

                if (dangerousPerms > 5) {
                    recommendations.add(
                        Recommendation(
                            type = RecommendationType.PRIVACY,
                            title = "权限检查: ${app.appName}",
                            description = "此应用请求了 $dangerousPerms 个危险权限，建议审查",
                            priority = 80,
                            app = app,
                            action = RecommendationAction.CHECK_PERMISSIONS,
                            metadata = mapOf(
                                "dangerous_permissions" to dangerousPerms,
                                "permissions" to analysis.dangerousPermissions
                            )
                        )
                    )
                }
            }
        }

        return recommendations
    }

    private fun getDuplicateRecommendations(apps: List<AppInfo>): List<Recommendation> {
        val recommendations = mutableListOf<Recommendation>()

        // Group apps by similar names
        val groupedApps = apps.groupBy { app ->
            app.appName.split(" ").firstOrNull()?.lowercase() ?: app.appName.lowercase()
        }

        groupedApps.filter { it.value.size > 1 }.forEach { (_, similarApps) ->
            if (similarApps.size >= 2) {
                recommendations.add(
                    Recommendation(
                        type = RecommendationType.DUPLICATE,
                        title = "相似应用",
                        description = "发现 ${similarApps.size} 个名称相似的应用: ${similarApps.joinToString { it.appName }}",
                        priority = 75,
                        app = similarApps.first(),
                        action = RecommendationAction.COMPARE_APPS,
                        metadata = mapOf("similar_apps" to similarApps.map { it.packageName })
                    )
                )
            }
        }

        return recommendations
    }

    fun recordAppUsage(packageName: String, duration: Long) {
        val key = "usage_$packageName"
        val currentUsage = prefs.getLong(key, 0)
        prefs.edit().putLong(key, currentUsage + duration).apply()

        val lastUsedKey = "last_used_$packageName"
        prefs.edit().putLong(lastUsedKey, System.currentTimeMillis()).apply()
    }

    fun getClonedApps(): List<String> {
        return prefs.getStringSet("cloned_apps", emptySet())?.toList() ?: emptyList()
    }

    fun markAsCloned(packageName: String) {
        val clonedApps = prefs.getStringSet("cloned_apps", mutableSetOf())?.toMutableSet()
        clonedApps?.add(packageName)
        prefs.edit().putStringSet("cloned_apps", clonedApps).apply()
    }

    fun shouldRecommendCloning(packageName: String): Boolean {
        // Recommend cloning for apps with high usage and not already cloned
        val usageKey = "usage_$packageName"
        val usage = prefs.getLong(usageKey, 0)
        return usage > 30 * 60 * 1000 && // > 30 minutes usage
               !getClonedApps().contains(packageName)
    }

    fun getPersonalizationScore(): Float {
        val usageCount = prefs.all.keys.count { it.startsWith("usage_") }
        val clonedCount = getClonedApps().size
        
        return (usageCount + clonedCount * 2).toFloat() / 50f
    }

    fun clearRecommendationHistory() {
        prefs.edit().clear().apply()
    }
}

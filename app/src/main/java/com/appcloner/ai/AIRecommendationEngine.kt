package com.appcloner.ai

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import com.appcloner.AppInfo
import com.appcloner.data.model.AppAnalysis
import com.appcloner.core.AppAnalyzer
import java.util.*
import kotlin.math.abs

class AIRecommendationEngine(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "ai_recommendations", Context.MODE_PRIVATE
    )
    
    private val usageStatsManager: UsageStatsManager? = 
        context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager

    data class UsagePattern(
        val packageName: String,
        val appName: String,
        val totalUsageTime: Long,
        val launchCount: Int,
        val lastUsed: Long,
        val peakHours: List&lt;Int&gt;,
        val averageSessionLength: Long,
        val usageTrend: UsageTrend
    )

    enum class UsageTrend {
        INCREASING,
        DECREASING,
        STABLE,
        UNKNOWN
    }

    data class Recommendation(
        val type: RecommendationType,
        val title: String,
        val description: String,
        val priority: Int,
        val app: AppInfo?,
        val confidence: Float,
        val reasons: List&lt;String&gt;,
        val action: RecommendationAction,
        val metadata: Map&lt;String, Any&gt; = emptyMap()
    )

    enum class RecommendationType {
        CLONE_SUGGESTION,
        STORAGE_OPTIMIZATION,
        PRIVACY_CHECK,
        USAGE_INSIGHT,
        BACKUP_RECOMMENDATION
    }

    enum class RecommendationAction {
        CLONE_APP,
        DELETE_APP,
        CHECK_PERMISSIONS,
        BACKUP_APP,
        IGNORE
    }

    data class UsageStatsData(
        val packageName: String,
        val usageTime: Long,
        val lastUsed: Long,
        val firstUsed: Long,
        val appName: String
    )

    private val learningData = mutableMapOf&lt;String, Float&gt;()
    private val userPreferences = mutableMapOf&lt;String, Boolean&gt;()

    fun analyzeUsagePatterns(days: Int = 7): List&lt;UsagePattern&gt; {
        if (usageStatsManager == null) {
            return emptyList()
        }

        val endTime = System.currentTimeMillis()
        val startTime = endTime - (days.toLong() * 24 * 60 * 60 * 1000)

        return try {
            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            )

            stats
                .filter { it.totalTimeInForeground &gt; 0 }
                .map { stat -&gt;
                    val appName = getAppName(stat.packageName)
                    val peakHours = calculatePeakHours(stat.packageName, startTime, endTime)
                    val trend = calculateUsageTrend(stat.packageName, stats.toList())
                    
                    UsagePattern(
                        packageName = stat.packageName,
                        appName = appName,
                        totalUsageTime = stat.totalTimeInForeground,
                        launchCount = 0, // Would need event data
                        lastUsed = stat.lastTimeUsed,
                        peakHours = peakHours,
                        averageSessionLength = calculateAverageSession(stat.totalTimeInForeground, 0),
                        usageTrend = trend
                    )
                }
                .sortedByDescending { it.totalUsageTime }
        } catch (e: SecurityException) {
            emptyList()
        }
    }

    fun generateRecommendations(apps: List&lt;AppInfo&gt;): List&lt;Recommendation&gt; {
        val recommendations = mutableListOf&lt;Recommendation&gt;()
        
        val usagePatterns = analyzeUsagePatterns()
        
        // Clone suggestions based on usage
        recommendations.addAll(generateCloneRecommendations(apps, usagePatterns))
        
        // Storage optimization recommendations
        recommendations.addAll(generateStorageRecommendations(apps))
        
        // Privacy recommendations
        recommendations.addAll(generatePrivacyRecommendations(apps))
        
        // Backup recommendations
        recommendations.addAll(generateBackupRecommendations(apps, usagePatterns))
        
        // Sort by priority and confidence
        return recommendations
            .sortedWith(compareByDescending&lt;Recommendation&gt; { it.priority * it.confidence })
            .take(10)
    }

    private fun generateCloneRecommendations(
        apps: List&lt;AppInfo&gt;,
        usagePatterns: List&lt;UsagePattern&gt;
    ): List&lt;Recommendation&gt; {
        val recommendations = mutableListOf&lt;Recommendation&gt;()
        
        usagePatterns.forEach { pattern -&gt;
            val app = apps.find { it.packageName == pattern.packageName }
            if (app != null) {
                val confidence = calculateCloneConfidence(pattern)
                val reasons = mutableListOf&lt;String&gt;()
                
                // High usage = good candidate for cloning
                if (pattern.totalUsageTime &gt; 60 * 60 * 1000) { // 1 hour
                    reasons.add("High usage: ${formatDuration(pattern.totalUsageTime)}")
                }
                
                // Stable usage trend
                if (pattern.usageTrend == UsageTrend.STABLE || 
                    pattern.usageTrend == UsageTrend.INCREASING) {
                    reasons.add("Stable or increasing usage trend")
                }
                
                // Multi-account apps
                if (isMultiAccountApp(app.packageName)) {
                    reasons.add("Common multi-account app")
                    confidence.coerceAtLeast(0.8f)
                }
                
                if (confidence &gt; 0.6f) {
                    recommendations.add(
                        Recommendation(
                            type = RecommendationType.CLONE_SUGGESTION,
                            title = "建议克隆: ${app.appName}",
                            description = buildCloneDescription(pattern, confidence),
                            priority = 85,
                            app = app,
                            confidence = confidence,
                            reasons = reasons,
                            action = RecommendationAction.CLONE_APP,
                            metadata = mapOf(
                                "usage_time" to pattern.totalUsageTime,
                                "trend" to pattern.usageTrend.name
                            )
                        )
                    )
                }
            }
        }
        
        return recommendations
    }

    private fun generateStorageRecommendations(apps: List&lt;AppInfo&gt;): List&lt;Recommendation&gt; {
        val recommendations = mutableListOf&lt;Recommendation&gt;()
        
        // Large apps
        apps.filter { it.apkSize &gt; 100 * 1024 * 1024 }
            .sortedByDescending { it.apkSize }
            .take(3)
            .forEach { app -&gt;
                val sizeMB = app.apkSize / (1024 * 1024)
                recommendations.add(
                    Recommendation(
                        type = RecommendationType.STORAGE_OPTIMIZATION,
                        title = "大应用: ${app.appName}",
                        description = "占用 ${sizeMB}MB 存储空间",
                        priority = 70,
                        app = app,
                        confidence = 0.9f,
                        reasons = listOf("Large app size: ${sizeMB}MB"),
                        action = RecommendationAction.DELETE_APP,
                        metadata = mapOf("size" to app.apkSize)
                    )
                )
            }
        
        return recommendations
    }

    private fun generatePrivacyRecommendations(apps: List&lt;AppInfo&gt;): List&lt;Recommendation&gt; {
        val recommendations = mutableListOf&lt;Recommendation&gt;()
        
        apps.take(5).forEach { app -&gt;
            val analysis = AppAnalyzer.analyzeApp(context, app.packageName)
            if (analysis != null &amp;&amp; analysis.dangerousPermissions.size &gt; 5) {
                recommendations.add(
                    Recommendation(
                        type = RecommendationType.PRIVACY_CHECK,
                        title = "权限审查: ${app.appName}",
                        description = "请求了 ${analysis.dangerousPermissions.size} 个危险权限",
                        priority = 80,
                        app = app,
                        confidence = 0.85f,
                        reasons = listOf("${analysis.dangerousPermissions.size} dangerous permissions"),
                        action = RecommendationAction.CHECK_PERMISSIONS,
                        metadata = mapOf(
                            "dangerous_permissions" to analysis.dangerousPermissions.size,
                            "permissions" to analysis.dangerousPermissions
                        )
                    )
                )
            }
        }
        
        return recommendations
    }

    private fun generateBackupRecommendations(
        apps: List&lt;AppInfo&gt;,
        usagePatterns: List&lt;UsagePattern&gt;
    ): List&lt;Recommendation&gt; {
        val recommendations = mutableListOf&lt;Recommendation&gt;()
        
        usagePatterns.take(5).forEach { pattern -&gt;
            val app = apps.find { it.packageName == pattern.packageName }
            if (app != null) {
                recommendations.add(
                    Recommendation(
                        type = RecommendationType.BACKUP_RECOMMENDATION,
                        title = "建议备份: ${app.appName}",
                        description = "高使用率应用，建议定期备份",
                        priority = 75,
                        app = app,
                        confidence = 0.8f,
                        reasons = listOf(
                            "Usage: ${formatDuration(pattern.totalUsageTime)}",
                            "Important app for user"
                        ),
                        action = RecommendationAction.BACKUP_APP
                    )
                )
            }
        }
        
        return recommendations
    }

    private fun calculateCloneConfidence(pattern: UsagePattern): Float {
        var confidence = 0.5f
        
        // High usage increases confidence
        if (pattern.totalUsageTime &gt; 60 * 60 * 1000) confidence += 0.2f
        if (pattern.totalUsageTime &gt; 5 * 60 * 60 * 1000) confidence += 0.1f
        
        // Stable trend increases confidence
        if (pattern.usageTrend == UsageTrend.STABLE) confidence += 0.15f
        if (pattern.usageTrend == UsageTrend.INCREASING) confidence += 0.1f
        
        // Recent usage increases confidence
        val daysSinceLastUse = (System.currentTimeMillis() - pattern.lastUsed) / (24 * 60 * 60 * 1000)
        if (daysSinceLastUse &lt; 7) confidence += 0.1f
        
        return confidence.coerceIn(0f, 1f)
    }

    private fun calculatePeakHours(packageName: String, startTime: Long, endTime: Long): List&lt;Int&gt; {
        // Simplified peak hours calculation
        return listOf(9, 12, 18, 21) // Common peak hours
    }

    private fun calculateUsageTrend(packageName: String, allStats: List&lt;UsageStats&gt;): UsageTrend {
        // Simplified trend calculation
        return UsageTrend.STABLE
    }

    private fun calculateAverageSession(totalTime: Long, sessionCount: Int): Long {
        return if (sessionCount &gt; 0) totalTime / sessionCount else totalTime
    }

    private fun isMultiAccountApp(packageName: String): Boolean {
        val multiAccountApps = setOf(
            "com.whatsapp",
            "com.instagram.android",
            "com.facebook.katana",
            "com.twitter.android",
            "com.google.android.youtube",
            "com.tencent.mm", // WeChat
            "com.tencent.mobileqq",
            "com.alibaba.android.intl",
            "com.linkedin.android",
            "com.snapchat.android"
        )
        return multiAccountApps.any { packageName.startsWith(it) }
    }

    private fun buildCloneDescription(pattern: UsagePattern, confidence: Float): String {
        val usageFormatted = formatDuration(pattern.totalUsageTime)
        val trendText = when (pattern.usageTrend) {
            UsageTrend.INCREASING -&gt; "上升📈"
            UsageTrend.DECREASING -&gt; "下降📉"
            UsageTrend.STABLE -&gt; "稳定📊"
            UsageTrend.UNKNOWN -&gt; ""
        }
        return "使用时间: $usageFormatted $trendText\n置信度: ${(confidence * 100).toInt()}%"
    }

    private fun formatDuration(millis: Long): String {
        val hours = millis / (60 * 60 * 1000)
        val minutes = (millis % (60 * 60 * 1000)) / (60 * 1000)
        return when {
            hours &gt; 0 -&gt; "${hours}h ${minutes}m"
            minutes &gt; 0 -&gt; "${minutes}m"
            else -&gt; "&lt;1m"
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

    fun learnFromFeedback(packageName: String, feedback: Feedback) {
        val currentConfidence = learningData[packageName] ?: 0.5f
        val adjustment = when (feedback) {
            Feedback.POSITIVE -&gt; 0.05f
            Feedback.NEGATIVE -&gt; -0.05f
            Feedback.IGNORED -&gt; 0f
        }
        learningData[packageName] = (currentConfidence + adjustment).coerceIn(0f, 1f)
        saveLearningData()
    }

    fun getLearnedConfidence(packageName: String): Float {
        return learningData[packageName] ?: 0.5f
    }

    private fun saveLearningData() {
        val json = learningData.entries.joinToString(",") { 
            "\"${it.key}\":${it.value}" 
        }
        prefs.edit().putString("learning_data", "{$json}").apply()
    }

    fun setUserPreference(category: String, enabled: Boolean) {
        userPreferences[category] = enabled
        prefs.edit().putBoolean("pref_$category", enabled).apply()
    }

    fun getUserPreference(category: String): Boolean {
        return userPreferences[category] ?: true
    }

    enum class Feedback {
        POSITIVE,
        NEGATIVE,
        IGNORED
    }
}

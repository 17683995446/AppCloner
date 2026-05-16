
package com.appcloner.data.model

import android.content.Context
import android.content.SharedPreferences
import android.graphics.drawable.Drawable
import android.text.format.Formatter
import com.appcloner.AppInfo
import java.text.SimpleDateFormat
import java.util.*

data class CloneHistory(
    val id: String,
    val originalAppName: String,
    val originalPackageName: String,
    val originalIcon: Drawable?,
    val clonedAppName: String,
    val clonedPackageName: String,
    val clonedApkPath: String?,
    val cloneTime: Long,
    val templateUsed: String?,
    val cloneStatus: CloneStatus
)

enum class CloneStatus {
    SUCCESS,
    FAILED,
    IN_PROGRESS,
    CANCELLED
}

class CloneHistoryManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "clone_history", Context.MODE_PRIVATE
    )

    private val history = mutableListOf<CloneHistory>()
    private val maxHistorySize = 100

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    init {
        loadHistory()
    }

    private fun loadHistory() {
        history.clear()
        val historyJson = prefs.getString("history", "") ?: ""
        if (historyJson.isNotEmpty()) {
            try {
                history.addAll(parseHistory(historyJson))
            } catch (e: Exception) {
                // Ignore parsing errors
            }
        }
    }

    private fun parseHistory(json: String): List<CloneHistory> {
        return json.split("###").filter { it.isNotEmpty() }.mapNotNull { entry ->
            try {
                val parts = entry.split("|||")
                if (parts.size >= 9) {
                    CloneHistory(
                        id = parts[0],
                        originalAppName = parts[1],
                        originalPackageName = parts[2],
                        originalIcon = null, // Icons can't be serialized
                        clonedAppName = parts[4],
                        clonedPackageName = parts[5],
                        clonedApkPath = parts[6].ifEmpty { null },
                        cloneTime = parts[7].toLong(),
                        templateUsed = parts[8].ifEmpty { null },
                        cloneStatus = CloneStatus.valueOf(parts[9])
                    )
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun serializeHistory(): String {
        return history.joinToString("###") { item ->
            listOf(
                item.id,
                item.originalAppName,
                item.originalPackageName,
                "", // Original icon (can't be serialized)
                item.clonedAppName,
                item.clonedPackageName,
                item.clonedApkPath ?: "",
                item.cloneTime.toString(),
                item.templateUsed ?: "",
                item.cloneStatus.name
            ).joinToString("|||")
        }
    }

    fun addToHistory(
        originalApp: AppInfo,
        clonedPackageName: String,
        clonedApkPath: String?,
        template: CloneTemplate?,
        status: CloneStatus
    ) {
        val clonedAppName = "${originalApp.appName} (Clone)"
        
        val entry = CloneHistory(
            id = UUID.randomUUID().toString(),
            originalAppName = originalApp.appName,
            originalPackageName = originalApp.packageName,
            originalIcon = originalApp.icon,
            clonedAppName = clonedAppName,
            clonedPackageName = clonedPackageName,
            clonedApkPath = clonedApkPath,
            cloneTime = System.currentTimeMillis(),
            templateUsed = template?.name,
            cloneStatus = status
        )

        history.add(0, entry)

        // Trim history to max size
        while (history.size > maxHistorySize) {
            history.removeAt(history.size - 1)
        }

        saveHistory()
    }

    private fun saveHistory() {
        val json = serializeHistory()
        prefs.edit().putString("history", json).apply()
    }

    fun getAllHistory(): List<CloneHistory> = history.toList()

    fun getHistoryByStatus(status: CloneStatus): List<CloneHistory> {
        return history.filter { it.cloneStatus == status }
    }

    fun getSuccessfulClones(): List<CloneHistory> {
        return history.filter { it.cloneStatus == CloneStatus.SUCCESS }
    }

    fun getFailedClones(): List<CloneHistory> {
        return history.filter { it.cloneStatus == CloneStatus.FAILED }
    }

    fun getRecentHistory(limit: Int = 10): List<CloneHistory> {
        return history.take(limit)
    }

    fun searchHistory(query: String): List<CloneHistory> {
        val lowerQuery = query.lowercase()
        return history.filter { item ->
            item.originalAppName.lowercase().contains(lowerQuery) ||
            item.originalPackageName.lowercase().contains(lowerQuery) ||
            item.clonedAppName.lowercase().contains(lowerQuery) ||
            item.clonedPackageName.lowercase().contains(lowerQuery)
        }
    }

    fun deleteHistoryItem(id: String) {
        history.removeAll { it.id == id }
        saveHistory()
    }

    fun clearHistory() {
        history.clear()
        saveHistory()
    }

    fun clearFailedHistory() {
        history.removeAll { it.cloneStatus == CloneStatus.FAILED }
        saveHistory()
    }

    fun getHistoryStats(): HistoryStats {
        val total = history.size
        val successful = history.count { it.cloneStatus == CloneStatus.SUCCESS }
        val failed = history.count { it.cloneStatus == CloneStatus.FAILED }
        val inProgress = history.count { it.cloneStatus == CloneStatus.IN_PROGRESS }

        val successRate = if (total > 0) (successful.toFloat() / total * 100) else 0f

        val lastCloneTime = history.firstOrNull()?.cloneTime

        return HistoryStats(
            totalClones = total,
            successfulClones = successful,
            failedClones = failed,
            inProgressClones = inProgress,
            successRate = successRate,
            lastCloneTime = lastCloneTime
        )
    }

    fun exportHistory(): String {
        val sb = StringBuilder()
        sb.appendLine("AppCloner Clone History Export")
        sb.appendLine("Generated: ${dateFormat.format(Date())}")
        sb.appendLine("=" .repeat(50))
        sb.appendLine()

        history.forEachIndexed { index, item ->
            sb.appendLine("Clone #${index + 1}")
            sb.appendLine("Original App: ${item.originalAppName}")
            sb.appendLine("Original Package: ${item.originalPackageName}")
            sb.appendLine("Cloned App: ${item.clonedAppName}")
            sb.appendLine("Cloned Package: ${item.clonedPackageName}")
            sb.appendLine("Clone Time: ${dateFormat.format(Date(item.cloneTime))}")
            sb.appendLine("Status: ${item.cloneStatus.name}")
            if (item.templateUsed != null) {
                sb.appendLine("Template: ${item.templateUsed}")
            }
            sb.appendLine("-".repeat(50))
            sb.appendLine()
        }

        val stats = getHistoryStats()
        sb.appendLine("Statistics:")
        sb.appendLine("Total Clones: ${stats.totalClones}")
        sb.appendLine("Successful: ${stats.successfulClones}")
        sb.appendLine("Failed: ${stats.failedClones}")
        sb.appendLine("Success Rate: ${String.format("%.1f", stats.successRate)}%")

        return sb.toString()
    }

    data class HistoryStats(
        val totalClones: Int,
        val successfulClones: Int,
        val failedClones: Int,
        val inProgressClones: Int,
        val successRate: Float,
        val lastCloneTime: Long?
    ) {
        fun getFormattedLastCloneTime(): String {
            return lastCloneTime?.let { dateFormat.format(Date(it)) } ?: "Never"
        }
    }
}

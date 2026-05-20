package com.appcloner.data.model

import android.graphics.drawable.Drawable

data class CloneHistory(
    val id: Long = 0,
    val originalPackageName: String,
    val clonedPackageName: String,
    val clonedAppName: String,
    val cloneTimestamp: Long = System.currentTimeMillis(),
    val apkPath: String,
    val appSize: Long = 0,
    val isSuccessful: Boolean = true,
    val errorMessage: String? = null,
    val originalIcon: Drawable? = null,
    val originalAppName: String? = null,
    val cloneStatus: CloneStatus = CloneStatus.SUCCESS,
    val templateUsed: String? = null
)

enum class CloneStatus {
    SUCCESS,
    FAILED,
    IN_PROGRESS,
    CANCELLED
}

data class CloneTemplate(
    val id: Long = 0,
    val name: String,
    val description: String,
    val packageNamePattern: String,
    val customSettings: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isDefault: Boolean = false,
    val usageCount: Int = 0
)

data class AppAnalysis(
    val packageName: String,
    val appName: String,
    val permissions: List<String> = emptyList(),
    val activities: List<String> = emptyList(),
    val services: List<String> = emptyList(),
    val receivers: List<String> = emptyList(),
    val providers: List<String> = emptyList(),
    val minSdkVersion: Int = 0,
    val targetSdkVersion: Int = 0,
    val isSystemApp: Boolean = false,
    val isCloneable: Boolean = true,
    val cloneDifficulty: CloneDifficulty = CloneDifficulty.EASY,
    val warnings: List<String> = emptyList(),
    val recommendations: List<String> = emptyList()
)

enum class CloneDifficulty {
    EASY,
    MEDIUM,
    HARD,
    NOT_SUPPORTED
}

class CloneHistoryManager(
    private val historyList: MutableList<CloneHistory> = mutableListOf()
) {
    private val maxHistorySize = 100

    fun addToHistory(
        originalPackageName: String,
        originalAppName: String?,
        clonedPackageName: String,
        clonedAppName: String,
        apkPath: String?,
        templateName: String?,
        status: CloneStatus
    ) {
        val entry = CloneHistory(
            id = System.currentTimeMillis(),
            originalPackageName = originalPackageName,
            clonedPackageName = clonedPackageName,
            clonedAppName = clonedAppName,
            cloneTimestamp = System.currentTimeMillis(),
            apkPath = apkPath ?: "",
            appSize = 0,
            isSuccessful = status == CloneStatus.SUCCESS,
            originalAppName = originalAppName,
            cloneStatus = status,
            templateUsed = templateName
        )

        historyList.add(0, entry)

        while (historyList.size > maxHistorySize) {
            historyList.removeAt(historyList.size - 1)
        }
    }

    fun getAllHistory(): List<CloneHistory> = historyList.toList()

    fun getHistoryByStatus(status: CloneStatus): List<CloneHistory> {
        return historyList.filter { it.cloneStatus == status }
    }

    fun getSuccessfulClones(): List<CloneHistory> {
        return historyList.filter { it.cloneStatus == CloneStatus.SUCCESS }
    }

    fun getFailedClones(): List<CloneHistory> {
        return historyList.filter { it.cloneStatus == CloneStatus.FAILED }
    }

    fun getRecentHistory(limit: Int = 10): List<CloneHistory> {
        return historyList.take(limit)
    }

    fun searchHistory(query: String): List<CloneHistory> {
        val lowerQuery = query.lowercase()
        return historyList.filter { item ->
            item.originalAppName?.lowercase()?.contains(lowerQuery) == true ||
            item.originalPackageName.lowercase().contains(lowerQuery) ||
            item.clonedAppName.lowercase().contains(lowerQuery) ||
            item.clonedPackageName.lowercase().contains(lowerQuery)
        }
    }

    fun deleteHistoryItem(id: Long) {
        historyList.removeAll { it.id == id }
    }

    fun clearHistory() {
        historyList.clear()
    }

    fun clearFailedHistory() {
        historyList.removeAll { it.cloneStatus == CloneStatus.FAILED }
    }

    data class HistoryStats(
        val totalClones: Int,
        val successfulClones: Int,
        val failedClones: Int,
        val inProgressClones: Int,
        val successRate: Float
    )

    fun getHistoryStats(): HistoryStats {
        val total = historyList.size
        val successful = historyList.count { it.cloneStatus == CloneStatus.SUCCESS }
        val failed = historyList.count { it.cloneStatus == CloneStatus.FAILED }
        val inProgress = historyList.count { it.cloneStatus == CloneStatus.IN_PROGRESS }
        val successRate = if (total > 0) (successful.toFloat() / total * 100) else 0f

        return HistoryStats(
            totalClones = total,
            successfulClones = successful,
            failedClones = failed,
            inProgressClones = inProgress,
            successRate = successRate
        )
    }
}

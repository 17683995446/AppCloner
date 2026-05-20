package com.appcloner.data.model

import android.graphics.drawable.Drawable

data class CloneHistory(
    val id: Long = 0,
    val originalPackageName: String,
    val clonedPackageName: String,
    val clonedAppName: String,
    val cloneTimestamp: Long,
    val apkPath: String,
    val appSize: Long,
    val isSuccessful: Boolean,
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
    val createdAt: Long,
    val isDefault: Boolean = false,
    val usageCount: Int = 0
)

data class AppAnalysis(
    val packageName: String,
    val appName: String,
    val permissions: List<String>,
    val activities: List<String>,
    val services: List<String>,
    val receivers: List<String>,
    val providers: List<String>,
    val minSdkVersion: Int,
    val targetSdkVersion: Int,
    val isSystemApp: Boolean,
    val isCloneable: Boolean,
    val cloneDifficulty: CloneDifficulty,
    val warnings: List<String>,
    val recommendations: List<String>
)

enum class CloneDifficulty {
    EASY,
    MEDIUM,
    HARD,
    NOT_SUPPORTED
}

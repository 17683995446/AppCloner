package com.appcloner.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clone_history")
data class CloneHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val originalPackageName: String,
    val clonedPackageName: String,
    val clonedAppName: String,
    val cloneTimestamp: Long,
    val apkPath: String,
    val appSize: Long,
    val isSuccessful: Boolean,
    val errorMessage: String? = null
)

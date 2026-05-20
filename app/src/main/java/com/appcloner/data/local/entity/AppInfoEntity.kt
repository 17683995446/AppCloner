
package com.appcloner.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "apps",
    indices = [
        Index(value = ["packageName"], unique = true)
    ]
)
data class AppInfoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val appName: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val isSystemApp: Boolean,
    val firstInstallTime: Long,
    val lastUpdateTime: Long,
    val isFavorite: Boolean = false,
    val isSavedApk: Boolean = false,
    val apkPath: String? = null,
    val appSize: Long = 0,
    val isCloned: Boolean = false,
    val cloneCount: Int = 0,
    val lastAccessedTime: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

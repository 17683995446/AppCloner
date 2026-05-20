package com.appcloner.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clone_templates")
data class CloneTemplateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String,
    val packageNamePattern: String,
    val customSettings: String,
    val createdAt: Long,
    val isDefault: Boolean = false,
    val usageCount: Int = 0
)

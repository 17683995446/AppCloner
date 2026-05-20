package com.appcloner.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.appcloner.data.local.dao.AppInfoDao
import com.appcloner.data.local.dao.CloneHistoryDao
import com.appcloner.data.local.dao.CloneTemplateDao
import com.appcloner.data.local.entity.AppInfoEntity
import com.appcloner.data.local.entity.CloneHistoryEntity
import com.appcloner.data.local.entity.CloneTemplateEntity

@Database(
    entities = [
        AppInfoEntity::class,
        CloneHistoryEntity::class,
        CloneTemplateEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class AppClonerDatabase : RoomDatabase() {
    abstract fun appInfoDao(): AppInfoDao
    abstract fun cloneHistoryDao(): CloneHistoryDao
    abstract fun cloneTemplateDao(): CloneTemplateDao
}

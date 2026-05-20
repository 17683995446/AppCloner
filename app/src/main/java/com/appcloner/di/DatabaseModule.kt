package com.appcloner.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.appcloner.data.local.AppClonerDatabase
import com.appcloner.data.local.dao.AppInfoDao
import com.appcloner.data.local.dao.CloneHistoryDao
import com.appcloner.data.local.dao.CloneTemplateDao
import com.appcloner.data.repository.AppRepository
import com.appcloner.data.repository.CloneHistoryRepository
import com.appcloner.data.repository.CloneTemplateRepository
import com.appcloner.data.repository.AppRepositoryImpl
import com.appcloner.data.repository.CloneHistoryRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS apps (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    appName TEXT NOT NULL,
                    packageName TEXT NOT NULL,
                    versionName TEXT NOT NULL,
                    versionCode INTEGER NOT NULL,
                    isSystemApp INTEGER NOT NULL,
                    firstInstallTime INTEGER NOT NULL,
                    lastUpdateTime INTEGER NOT NULL,
                    isFavorite INTEGER NOT NULL DEFAULT 0,
                    isSavedApk INTEGER NOT NULL DEFAULT 0,
                    apkPath TEXT,
                    appSize INTEGER NOT NULL DEFAULT 0,
                    isCloned INTEGER NOT NULL DEFAULT 0,
                    cloneCount INTEGER NOT NULL DEFAULT 0,
                    lastAccessedTime INTEGER NOT NULL DEFAULT 0,
                    createdAt INTEGER NOT NULL DEFAULT 0,
                    updatedAt INTEGER NOT NULL DEFAULT 0
                )
                """
            )
            database.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS index_apps_packageName ON apps(packageName)
                """
            )
        }
    }

    @Provides
    @Singleton
    fun provideAppClonerDatabase(
        @ApplicationContext context: Context
    ): AppClonerDatabase {
        return Room.databaseBuilder(
            context,
            AppClonerDatabase::class.java,
            "appcloner_database"
        )
            .addMigrations(MIGRATION_1_2)
            .build()
    }

    @Provides
    @Singleton
    fun provideAppInfoDao(database: AppClonerDatabase): AppInfoDao {
        return database.appInfoDao()
    }

    @Provides
    @Singleton
    fun provideCloneHistoryDao(database: AppClonerDatabase): CloneHistoryDao {
        return database.cloneHistoryDao()
    }

    @Provides
    @Singleton
    fun provideCloneTemplateDao(database: AppClonerDatabase): CloneTemplateDao {
        return database.cloneTemplateDao()
    }

    @Provides
    @Singleton
    fun provideAppRepository(
        @ApplicationContext context: Context,
        appInfoDao: AppInfoDao
    ): AppRepository {
        return AppRepositoryImpl(context, appInfoDao)
    }

    @Provides
    @Singleton
    fun provideCloneHistoryRepository(
        cloneHistoryDao: CloneHistoryDao
    ): CloneHistoryRepository {
        return CloneHistoryRepositoryImpl(cloneHistoryDao)
    }

    @Provides
    @Singleton
    fun provideCloneTemplateRepository(
        cloneTemplateDao: CloneTemplateDao
    ): CloneTemplateRepository {
        return CloneTemplateRepository(cloneTemplateDao)
    }
}

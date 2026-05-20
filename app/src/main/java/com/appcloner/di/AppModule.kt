
package com.appcloner.di

import android.content.Context
import com.appcloner.core.AppCloneManager
import com.appcloner.apkprocessor.ApkToolService
import com.appcloner.apkprocessor.RealApkToolService
import com.appcloner.apkprocessor.PackageNameModifier
import com.appcloner.data.repository.AppRepository
import com.appcloner.data.repository.CloneHistoryRepository
import com.appcloner.data.repository.AppRepositoryImpl
import com.appcloner.data.repository.CloneHistoryRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// ============================================================
// 统一Hilt依赖注入模块 - AppCloner核心依赖
// ============================================================

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * 提供ApkToolService - APK处理服务
     */
    @Provides
    @Singleton
    fun provideApkToolService(
        @ApplicationContext context: Context
    ): ApkToolService {
        return RealApkToolService(context)
    }

    /**
     * 提供PackageNameModifier - 包名修改服务
     */
    @Provides
    @Singleton
    fun providePackageNameModifier(): PackageNameModifier {
        return PackageNameModifier()
    }

    /**
     * 提供AppCloneManager - 应用克隆管理器
     */
    @Provides
    @Singleton
    fun provideAppCloneManager(
        @ApplicationContext context: Context,
        apkToolService: ApkToolService,
        packageNameModifier: PackageNameModifier
    ): AppCloneManager {
        return AppCloneManager(
            applicationContext = context,
            apkDecompiler = apkToolService,
            packageModifier = packageNameModifier
        )
    }

    /**
     * 提供AppRepository - 应用仓库
     */
    @Provides
    @Singleton
    fun provideAppRepository(
        @ApplicationContext context: Context
    ): AppRepository {
        return AppRepositoryImpl(context)
    }

    /**
     * 提供CloneHistoryRepository - 克隆历史仓库
     */
    @Provides
    @Singleton
    fun provideCloneHistoryRepository(): CloneHistoryRepository {
        return CloneHistoryRepositoryImpl()
    }
}


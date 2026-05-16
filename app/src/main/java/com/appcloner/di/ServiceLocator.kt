
package com.appcloner.di

import android.content.Context
import com.appcloner.data.repository.AppRepository
import com.appcloner.core.CloneManager
import com.appcloner.core.AppAnalyzer
import com.appcloner.core.BatchCloneManager
import com.appcloner.data.apk.ApkResourceViewer
import com.appcloner.data.backup.AutoBackupManager
import com.appcloner.presentation.viewmodel.MainViewModel
import com.appcloner.security.PermissionAuditTool

object ServiceLocator {
    private var instance: AppRepository? = null
    private var cloneManager: CloneManager? = null
    private var appAnalyzer: AppAnalyzer? = null
    private var apkViewer: ApkResourceViewer? = null
    private var batchCloneManager: BatchCloneManager? = null
    private var permissionAuditTool: PermissionAuditTool? = null

    fun provideRepository(context: Context): AppRepository {
        return instance ?: synchronized(this) {
            instance ?: AppRepository(context).also { instance = it }
        }
    }

    fun provideCloneManager(context: Context): CloneManager {
        return cloneManager ?: synchronized(this) {
            cloneManager ?: CloneManager(context).also { cloneManager = it }
        }
    }

    fun provideAppAnalyzer(): AppAnalyzer {
        return appAnalyzer ?: synchronized(this) {
            AppAnalyzer.also { appAnalyzer = it }
        }
    }

    fun provideApkResourceViewer(): ApkResourceViewer {
        return apkViewer ?: synchronized(this) {
            ApkResourceViewer.also { apkViewer = it }
        }
    }

    fun provideBatchCloneManager(context: Context): BatchCloneManager {
        return batchCloneManager ?: synchronized(this) {
            BatchCloneManager(context).also { batchCloneManager = it }
        }
    }

    fun providePermissionAuditTool(): PermissionAuditTool {
        return permissionAuditTool ?: synchronized(this) {
            PermissionAuditTool.also { permissionAuditTool = it }
        }
    }

    fun reset() {
        instance = null
        cloneManager = null
        appAnalyzer = null
        apkViewer = null
        batchCloneManager = null
        permissionAuditTool = null
    }
}

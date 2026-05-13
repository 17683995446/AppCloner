
package com.appcloner.core

import android.content.Context
import com.appcloner.apkprocessor.*
import com.appcloner.utils.FileUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/**
 * 服务定位器 - 依赖注入容器
 * 轻量级的DI实现，替代复杂的DI框架
 */
class ServiceLocator(private val context: Context) {

    // 工具服务
    val fileUtils: FileUtils = FileUtils
    
    // APK处理服务
    val apkToolService: IApkToolService by lazy {
        RealApkToolService(context)
    }

    val signerService: ISignerService by lazy {
        RealSignerService(context)
    }
    
    val packageNameModifier: PackageNameModifier by lazy {
        PackageNameModifier()
    }
    
    // 业务逻辑服务
    val cloneManager: CloneManager by lazy {
        CloneManager(
            apkToolService = apkToolService,
            signerService = signerService,
            packageNameModifier = packageNameModifier
        )
    }
    
    // 应用存储服务
    val preferencesManager: PreferencesManager by lazy {
        PreferencesManager(context)
    }
    
    // 产品化服务
    val analyticsManager: AnalyticsManager by lazy {
        AnalyticsManager.getInstance(context)
    }
    
    val errorHandler: ErrorHandler by lazy {
        ErrorHandler.getInstance(context, analyticsManager)
    }
    
    val onboardingManager: OnboardingManager by lazy {
        OnboardingManager.getInstance(context, preferencesManager)
    }
    
    val sandboxManager: CloneSandboxManager by lazy {
        CloneSandboxManager.getInstance(context, preferencesManager)
    }

    companion object {
        @Volatile
        private var instance: ServiceLocator? = null
        
        fun getInstance(context: Context): ServiceLocator {
            return instance ?: synchronized(this) {
                instance ?: ServiceLocator(context).also { instance = it }
            }
        }
    }
}

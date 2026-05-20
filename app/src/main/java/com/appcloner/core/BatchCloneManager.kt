
package com.appcloner.core

import com.appcloner.core.model.AppInfo
import com.appcloner.core.model.CloneTemplate
import com.appcloner.utils.LogUtils
import kotlinx.coroutines.*
import java.io.File

class BatchCloneManager(private val context: android.content.Context) {

    private val cloneManager = CloneManager(context)
    private var isCancelled = false
    private var currentJob: Job? = null

    interface CloneProgressListener {
        fun onStart(totalApps: Int)
        fun onProgress(currentIndex: Int, totalApps: Int, appName: String, step: String)
        fun onSuccess(appName: String, clonedApk: File)
        fun onFailure(appName: String, error: String)
        fun onComplete(results: BatchCloneResults)
        fun onCancelled()
    }

    data class BatchCloneResults(
        val totalApps: Int,
        val successfulClones: Int,
        val failedClones: Int,
        val results: List<CloneResult>
    )

    data class CloneResult(
        val app: AppInfo,
        val isSuccess: Boolean,
        val clonedApk: File?,
        val errorMessage: String?,
        val duration: Long
    )

    fun startBatchClone(
        apps: List<AppInfo>,
        template: CloneTemplate,
        listener: CloneProgressListener
    ) {
        isCancelled = false
        
        currentJob = CoroutineScope(Dispatchers.Main).launch {
            listener.onStart(apps.size)
            
            val results = mutableListOf<CloneResult>()
            var successCount = 0
            var failCount = 0
            
            apps.forEachIndexed { index, app ->
                if (isCancelled) {
                    listener.onCancelled()
                    return@forEachIndexed
                }
                
                val startTime = System.currentTimeMillis()
                
                try {
                    listener.onProgress(index + 1, apps.size, app.appName, "准备中...")
                    
                    val packageName = "${app.packageName}${template.packageSuffix}"
                    val appName = "${app.appName}${template.appNameSuffix}"
                    val outputDir = File(context.getExternalFilesDir(null), "cloned_apks")
                    
                    val config = CloneConfig(
                        originalApk = File(context.packageManager.getApplicationInfo(app.packageName, 0).sourceDir),
                        newPackageName = packageName,
                        newAppName = appName,
                        outputDir = outputDir,
                        copyData = template.copyData,
                        keepOriginalIcon = template.keepIcon
                    )
                    
                    listener.onProgress(index + 1, apps.size, app.appName, "正在克隆...")
                    
                    val result = withContext(Dispatchers.IO) {
                        cloneManager.cloneApp(config, object : CloneProgressListener {
                            override fun onProgress(step: Int, total: Int, message: String) {
                                // Progress updates
                            }
                            override fun onSuccess(clonedApk: File) {}
                            override fun onError(error: String) {}
                        }).getOrNull()
                    }
                    
                    val duration = System.currentTimeMillis() - startTime
                    
                    if (result != null) {
                        successCount++
                        results.add(
                            CloneResult(
                                app = app,
                                isSuccess = true,
                                clonedApk = result,
                                errorMessage = null,
                                duration = duration
                            )
                        )
                        listener.onSuccess(app.appName, result)
                    } else {
                        failCount++
                        val errorMsg = "克隆失败"
                        results.add(
                            CloneResult(
                                app = app,
                                isSuccess = false,
                                clonedApk = null,
                                errorMessage = errorMsg,
                                duration = duration
                            )
                        )
                        listener.onFailure(app.appName, errorMsg)
                    }
                    
                } catch (e: Exception) {
                    val duration = System.currentTimeMillis() - startTime
                    failCount++
                    val errorMsg = e.message ?: "未知错误"
                    results.add(
                        CloneResult(
                            app = app,
                            isSuccess = false,
                            clonedApk = null,
                            errorMessage = errorMsg,
                            duration = duration
                        )
                    )
                    listener.onFailure(app.appName, errorMsg)
                    LogUtils.e("BatchClone", "Failed to clone ${app.appName}", e)
                }
                
                delay(100) // Small delay between clones
            }
            
            listener.onComplete(
                BatchCloneResults(
                    totalApps = apps.size,
                    successfulClones = successCount,
                    failedClones = failCount,
                    results = results
                )
            )
        }
    }

    fun cancelBatchClone() {
        isCancelled = true
        currentJob?.cancel()
    }

    fun isRunning(): Boolean = currentJob?.isActive ?: false
}

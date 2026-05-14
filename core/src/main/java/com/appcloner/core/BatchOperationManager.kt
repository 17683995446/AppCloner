
package com.appcloner.core

import android.content.Context
import com.appcloner.utils.LogUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 批量操作管理器 - 实用性功能
 * 支持克隆应用的批量创建、删除、管理
 */
class BatchOperationManager private constructor(
    private val context: Context,
    private val sandboxManager: CloneSandboxManager
) {
    companion object {
        private const val TAG = "BatchOperationManager"
        
        @Volatile
        private var instance: BatchOperationManager? = null
        
        fun getInstance(
            context: Context,
            sandboxManager: CloneSandboxManager
        ): BatchOperationManager {
            return instance ?: synchronized(this) {
                instance ?: BatchOperationManager(context.applicationContext, sandboxManager)
                    .also { instance = it }
            }
        }
    }

    /**
     * 批量操作结果
     */
    data class BatchOperationResult(
        val totalRequested: Int,
        val totalSucceeded: Int,
        val totalFailed: Int,
        val errors: List&lt;String&gt; = emptyList()
    )

    /**
     * 批量操作进度
     */
    data class BatchProgress(
        val current: Int,
        val total: Int,
        val currentItem: String,
        val isComplete: Boolean
    )

    private val _batchProgress = MutableStateFlow(
        BatchProgress(0, 0, "", false)
    )
    val batchProgress: StateFlow&lt;BatchProgress&gt; = _batchProgress.asStateFlow()

    /**
     * 批量克隆多个应用
     */
    suspend fun batchClone(
        apps: List&lt;Triple&lt;String, String, String&gt;&gt; // (originalPkg, newPkg, appName)
    ): BatchOperationResult {
        LogUtils.i("Starting batch clone for ${apps.size} apps", TAG)
        
        var succeeded = 0
        var failed = 0
        val errors = mutableListOf&lt;String&gt;()
        
        _batchProgress.value = BatchProgress(0, apps.size, "", false)
        
        apps.forEachIndexed { index, (original, new, name) ->
            _batchProgress.value = BatchProgress(
                current = index + 1,
                total = apps.size,
                currentItem = name,
                isComplete = false
            )
            
            try {
                val result = sandboxManager.createSandbox(original, new, name)
                if (result.isSuccess) {
                    succeeded++
                    LogUtils.d("Batch clone success: $name", TAG)
                } else {
                    failed++
                    errors.add("$name: ${result.exceptionOrNull()?.message}")
                    LogUtils.e("Batch clone failed: $name", result.exceptionOrNull(), TAG)
                }
            } catch (e: Exception) {
                failed++
                errors.add("$name: ${e.message}")
            }
        }
        
        _batchProgress.value = BatchProgress(
            current = apps.size,
            total = apps.size,
            currentItem = "",
            isComplete = true
        )
        
        val batchResult = BatchOperationResult(
            totalRequested = apps.size,
            totalSucceeded = succeeded,
            totalFailed = failed,
            errors = errors
        )
        
        LogUtils.i("Batch clone completed: $succeeded succeeded, $failed failed", TAG)
        return batchResult
    }

    /**
     * 批量删除克隆应用
     */
    suspend fun batchDelete(
        packageNames: List&lt;String&gt;
    ): BatchOperationResult {
        LogUtils.i("Starting batch delete for ${packageNames.size} apps", TAG)
        
        var succeeded = 0
        var failed = 0
        val errors = mutableListOf&lt;String&gt;()
        
        _batchProgress.value = BatchProgress(0, packageNames.size, "", false)
        
        packageNames.forEachIndexed { index, packageName ->
            _batchProgress.value = BatchProgress(
                current = index + 1,
                total = packageNames.size,
                currentItem = packageName,
                isComplete = false
            )
            
            try {
                val result = sandboxManager.deleteSandbox(packageName)
                if (result.isSuccess) {
                    succeeded++
                    LogUtils.d("Batch delete success: $packageName", TAG)
                } else {
                    failed++
                    errors.add("$packageName: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                failed++
                errors.add("$packageName: ${e.message}")
            }
        }
        
        _batchProgress.value = BatchProgress(
            current = packageNames.size,
            total = packageNames.size,
            currentItem = "",
            isComplete = true
        )
        
        return BatchOperationResult(
            totalRequested = packageNames.size,
            totalSucceeded = succeeded,
            totalFailed = failed,
            errors = errors
        )
    }

    /**
     * 重置进度
     */
    fun resetProgress() {
        _batchProgress.value = BatchProgress(0, 0, "", false)
    }
}

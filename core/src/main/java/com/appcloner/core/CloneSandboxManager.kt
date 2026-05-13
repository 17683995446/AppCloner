
package com.appcloner.core

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.appcloner.utils.LogUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * 克隆应用管理器 - 创新的沙盒管理
 * 管理所有克隆应用的生命周期、统计、数据隔离
 */
class CloneSandboxManager private constructor(
    private val context: Context,
    private val preferencesManager: PreferencesManager
) {
    companion object {
        private const val TAG = "CloneSandboxManager"
        private const val DATA_DIR_NAME = "clone_data"
        
        @Volatile
        private var instance: CloneSandboxManager? = null
        
        fun getInstance(
            context: Context,
            preferencesManager: PreferencesManager
        ): CloneSandboxManager {
            return instance ?: synchronized(this) {
                instance ?: CloneSandboxManager(context.applicationContext, preferencesManager)
                    .also { instance = it }
            }
        }
    }

    // 沙盒统计数据
    data class SandboxStats(
        val totalClones: Int,
        val activeClones: Int,
        val totalStorageUsed: Long,
        val lastActivityTimestamp: Long,
        val cloneSuccessRate: Float
    )

    data class CloneSandbox(
        val cloneId: Int,
        val originalPackageName: String,
        val newPackageName: String,
        val appName: String,
        val creationTimestamp: Long,
        val sandboxDir: File,
        val isActive: Boolean = true,
        val lastLaunchedTimestamp: Long? = null
    )

    private val sandboxMap = ConcurrentHashMap&lt;String, CloneSandbox&gt;()
    private val cloneCounter = AtomicInteger(0)
    private val successAttempts = AtomicInteger(0)
    private val totalAttempts = AtomicInteger(0)

    init {
        loadExistingSandboxes()
    }

    fun getSandboxStats(): SandboxStats {
        val totalStorage = sandboxMap.values.sumOf { 
            getDirectorySize(it.sandboxDir) 
        }
        val successRate = if (totalAttempts.get() &gt; 0) {
            successAttempts.get().toFloat() / totalAttempts.get().toFloat()
        } else {
            1.0f
        }
        return SandboxStats(
            totalClones = sandboxMap.size,
            activeClones = sandboxMap.values.count { it.isActive },
            totalStorageUsed = totalStorage,
            lastActivityTimestamp = System.currentTimeMillis(),
            cloneSuccessRate = successRate
        )
    }

    fun createSandbox(
        originalPackageName: String,
        newPackageName: String,
        appName: String
    ): Result&lt;CloneSandbox&gt; {
        LogUtils.i("Creating sandbox for $newPackageName", TAG)
        totalAttempts.incrementAndGet()
        
        return try {
            val cloneId = preferencesManager.getAndIncrementCloneId()
            val sandboxDir = getSandboxDirectory(cloneId)
            
            // 创建隔离目录
            if (!sandboxDir.exists()) {
                sandboxDir.mkdirs()
            }
            
            // 创建子目录结构
            val cacheDir = File(sandboxDir, "cache").apply { mkdirs() }
            val dataDir = File(sandboxDir, "data").apply { mkdirs() }
            val filesDir = File(sandboxDir, "files").apply { mkdirs() }
            
            val sandbox = CloneSandbox(
                cloneId = cloneId,
                originalPackageName = originalPackageName,
                newPackageName = newPackageName,
                appName = appName,
                creationTimestamp = System.currentTimeMillis(),
                sandboxDir = sandboxDir
            )
            
            sandboxMap[newPackageName] = sandbox
            successAttempts.incrementAndGet()
            saveSandboxList()
            
            LogUtils.i("Sandbox created successfully: ${sandbox.sandboxDir}", TAG)
            Result.success(sandbox)
        } catch (e: Exception) {
            LogUtils.e("Failed to create sandbox", e, TAG)
            Result.failure(e)
        }
    }

    fun launchSandbox(clonePackageName: String): Result&lt;Unit&gt; {
        LogUtils.i("Launching sandbox: $clonePackageName", TAG)
        
        val sandbox = sandboxMap[clonePackageName]
        return if (sandbox != null) {
            // 更新最后启动时间
            sandboxMap[clonePackageName] = sandbox.copy(
                lastLaunchedTimestamp = System.currentTimeMillis()
            )
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException("Sandbox not found: $clonePackageName"))
        }
    }

    fun deleteSandbox(clonePackageName: String): Result&lt;Unit&gt; {
        LogUtils.i("Deleting sandbox: $clonePackageName", TAG)
        
        val sandbox = sandboxMap[clonePackageName]
        return if (sandbox != null) {
            try {
                // 删除沙盒目录
                sandbox.sandboxDir.deleteRecursively()
                sandboxMap.remove(clonePackageName)
                saveSandboxList()
                Result.success(Unit)
            } catch (e: Exception) {
                LogUtils.e("Failed to delete sandbox", e, TAG)
                Result.failure(e)
            }
        } else {
            Result.failure(IllegalStateException("Sandbox not found: $clonePackageName"))
        }
    }

    fun getAllSandboxes(): List&lt;CloneSandbox&gt; {
        return sandboxMap.values.toList()
    }

    fun getSandboxForPackage(packageName: String): CloneSandbox? {
        return sandboxMap[packageName]
    }

    fun clearAllSandboxes(): Result&lt;Unit&gt; {
        LogUtils.w("Clearing all sandboxes", TAG)
        return try {
            sandboxMap.values.forEach { sandbox -&gt;
                sandbox.sandboxDir.deleteRecursively()
            }
            sandboxMap.clear()
            saveSandboxList()
            Result.success(Unit)
        } catch (e: Exception) {
            LogUtils.e("Failed to clear sandboxes", e, TAG)
            Result.failure(e)
        }
    }

    fun getStorageSummary(): Map&lt;String, Long&gt; {
        return sandboxMap.mapValues { (_, sandbox) -&gt;
            getDirectorySize(sandbox.sandboxDir)
        }
    }

    private fun getSandboxDirectory(cloneId: Int): File {
        val baseDir = File(context.getExternalFilesDir(null), DATA_DIR_NAME)
        return File(baseDir, "clone_$cloneId")
    }

    private fun getDirectorySize(dir: File): Long {
        var size = 0L
        dir.walkTopDown().forEach { file -&gt;
            if (file.isFile) {
                size += file.length()
            }
        }
        return size
    }

    private fun saveSandboxList() {
        val sandboxList = sandboxMap.values.joinToString("|") { sandbox -&gt;
            "${sandbox.cloneId},${sandbox.originalPackageName}," +
            "${sandbox.newPackageName},${sandbox.appName}," +
            "${sandbox.creationTimestamp}"
        }
        
        preferencesManager.outputDirPath = sandboxList
    }

    private fun loadExistingSandboxes() {
        val savedList = preferencesManager.outputDirPath ?: return
        val sandboxEntries = savedList.split("|").filter { it.isNotEmpty() }
        
        sandboxEntries.forEach { entry -&gt;
            val parts = entry.split(",")
            if (parts.size &gt;= 5) {
                try {
                    val cloneId = parts[0].toInt()
                    val sandbox = CloneSandbox(
                        cloneId = cloneId,
                        originalPackageName = parts[1],
                        newPackageName = parts[2],
                        appName = parts[3],
                        creationTimestamp = parts[4].toLong(),
                        sandboxDir = getSandboxDirectory(cloneId)
                    )
                    sandboxMap[sandbox.newPackageName] = sandbox
                } catch (e: Exception) {
                    LogUtils.e("Failed to load sandbox entry", e, TAG)
                }
            }
        }
    }
}

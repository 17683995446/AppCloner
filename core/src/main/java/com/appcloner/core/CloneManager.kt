
package com.appcloner.core

import com.appcloner.apkprocessor.*
import com.appcloner.utils.LogUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 克隆进度回调
 */
interface CloneProgressListener {
    fun onProgress(step: Int, total: Int, message: String)
    fun onSuccess(clonedApk: File)
    fun onError(error: String)
}

/**
 * 克隆状态
 */
data class CloneConfig(
    val originalApk: File,
    val newPackageName: String,
    val newAppName: String,
    val outputDir: File
)

/**
 * 克隆管理器 - 核心业务逻辑
 * 整合所有APK处理模块，提供统一的克隆服务
 */
class CloneManager(
    private val apkToolService: IApkToolService = ApkToolService(),
    private val signerService: ISignerService = SignerService(),
    private val packageNameModifier: PackageNameModifier = PackageNameModifier()
) {

    companion object {
        private const val TAG = "CloneManager"
        private const val TOTAL_STEPS = 7
    }

    /**
     * 执行完整的克隆流程
     */
    suspend fun cloneApp(
        config: CloneConfig,
        progressListener: CloneProgressListener? = null
    ): Result&lt;File&gt; = withContext(Dispatchers.IO) {
        try {
            LogUtils.i("Starting app clone process: ${config.originalApk.name}", TAG)

            // Step 1: 准备工作目录
            progressListener?.onProgress(1, TOTAL_STEPS, "Preparing workspace")
            val workDir = File(config.outputDir, "work_${System.currentTimeMillis()}")
            val decompiledDir = File(workDir, "decompiled")
            
            // Step 2: 反编译APK
            progressListener?.onProgress(2, TOTAL_STEPS, "Decompiling APK")
            val decodeResult = apkToolService.decode(config.originalApk, decompiledDir)
            if (decodeResult.isFailure) {
                return@withContext Result.failure(decodeResult.exceptionOrNull()!!)
            }

            // Step 3: 修改包名
            progressListener?.onProgress(3, TOTAL_STEPS, "Modifying package name")
            val oldPackage = extractPackageName(decompiledDir)
            val modifyResult = packageNameModifier.modifyPackageName(
                decompiledDir,
                oldPackage,
                config.newPackageName
            )
            if (modifyResult.isFailure) {
                return@withContext Result.failure(modifyResult.exceptionOrNull()!!)
            }

            // Step 4: 修改应用名称
            progressListener?.onProgress(4, TOTAL_STEPS, "Modifying app name")
            modifyAppName(decompiledDir, config.newAppName)

            // Step 5: 重新打包
            progressListener?.onProgress(5, TOTAL_STEPS, "Repackaging APK")
            val unsignedApk = File(workDir, "unsigned.apk")
            val buildResult = apkToolService.build(decompiledDir, unsignedApk)
            if (buildResult.isFailure) {
                return@withContext Result.failure(buildResult.exceptionOrNull()!!)
            }

            // Step 6: 签名
            progressListener?.onProgress(6, TOTAL_STEPS, "Signing APK")
            val signedApk = File(config.outputDir, "${config.newAppName.replace(" ", "_")}_cloned.apk")
            val signResult = signerService.signApk(unsignedApk, signedApk)
            if (signResult.isFailure) {
                return@withContext Result.failure(signResult.exceptionOrNull()!!)
            }

            // Step 7: 完成
            progressListener?.onProgress(7, TOTAL_STEPS, "Completed")
            progressListener?.onSuccess(signedApk)
            
            LogUtils.i("App clone completed successfully: ${signedApk.name}", TAG)
            Result.success(signedApk)
        } catch (e: Exception) {
            LogUtils.e("App clone failed", e, TAG)
            progressListener?.onError(e.message ?: "Unknown error")
            Result.failure(e)
        }
    }

    /**
     * 从反编译目录提取包名
     */
    private fun extractPackageName(decompiledDir: File): String {
        val manifest = File(decompiledDir, "AndroidManifest.xml")
        if (!manifest.exists()) {
            throw IllegalStateException("AndroidManifest.xml not found")
        }
        
        // 简单的包名提取（真实实现会解析XML）
        val content = manifest.readText()
        val pattern = Regex("package=\"([^\"]+)\"")
        return pattern.find(content)?.groupValues?.get(1) ?: ""
    }

    /**
     * 修改应用名称
     */
    private fun modifyAppName(decompiledDir: File, newName: String) {
        val resDir = File(decompiledDir, "res")
        val valuesDir = File(resDir, "values")
        val stringsXml = File(valuesDir, "strings.xml")
        
        if (stringsXml.exists()) {
            var content = stringsXml.readText()
            content = content.replace(Regex("name=\"app_name\"[^&gt;]*&gt;[^&lt;]*&lt;/string&gt;"),
                "name=\"app_name\"&gt;$newName&lt;/string&gt;")
            stringsXml.writeText(content)
        }
    }
}

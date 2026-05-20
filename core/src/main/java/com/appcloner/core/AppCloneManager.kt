
package com.appcloner.core

import android.content.Context
import com.appcloner.apkprocessor.*
import com.appcloner.utils.LogUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 应用克隆进度监听器
 * 监听克隆操作的进度、成功和失败事件
 */
interface AppCloneProgressListener {
    fun onProgress(step: Int, total: Int, message: String)
    fun onSuccess(clonedApk: File)
    fun onError(error: String)
}

/**
 * 应用克隆配置数据类
 * 定义克隆操作的所有参数
 */
data class AppCloneConfiguration(
    val originalApk: File,
    val newPackageName: String,
    val newAppName: String,
    val outputDirectory: File
)

/**
 * 应用克隆管理器 - 核心业务逻辑
 * 
 * 职责：
 * - 协调APK反编译流程
 * - 修改包名和应用名称
 * - 重新打包APK
 * - 对APK进行签名
 * 
 * 使用真实APKTool和uber-apk-signer确保克隆应用可安装
 */
class AppCloneManager(
    private val applicationContext: Context,
    private val apkDecompiler: IApkToolService = ApkToolService(applicationContext),
    private val apkSigner: ISignerService = SignerService(applicationContext),
    private val packageModifier: PackageNameModifier = PackageNameModifier()
) {
    companion object {
        private const val TAG = "AppCloneManager"
        private const val TOTAL_PROCESSING_STEPS = 7
        private val INVALID_FILENAME_CHARS = Regex("[\\\\/:*?\"&lt;&gt;|]")
    }

    /**
     * 执行完整的应用克隆流程
     * @param configuration 克隆配置
     * @param progressListener 进度监听器
     * @return 克隆成功的APK文件
     */
    suspend fun executeClone(
        configuration: AppCloneConfiguration,
        progressListener: AppCloneProgressListener? = null
    ): Result&lt;File&gt; = withContext(Dispatchers.IO) {
        try {
            // 预验证：检查输入参数
            validateConfiguration(configuration).getOrElse { error -&gt;
                return@withContext Result.failure(error)
            }

            LogUtils.i("开始克隆应用: ${configuration.originalApk.name}", TAG)

            val workingDirectory = File(configuration.outputDirectory, "work_${System.currentTimeMillis()}")
            val decompiledDirectory = File(workingDirectory, "decompiled")
            
            // Step 1: 准备工作目录
            progressListener?.onProgress(1, TOTAL_PROCESSING_STEPS, "准备工作中...")
            prepareWorkingDirectory(workingDirectory, decompiledDirectory)
            
            // Step 2: 反编译APK
            progressListener?.onProgress(2, TOTAL_PROCESSING_STEPS, "反编译APK...")
            val decodeResult = apkDecompiler.decode(configuration.originalApk, decompiledDirectory)
            if (decodeResult.isFailure) {
                return@withContext Result.failure(decodeResult.exceptionOrNull()!!)
            }

            // Step 3: 修改包名
            progressListener?.onProgress(3, TOTAL_PROCESSING_STEPS, "修改包名...")
            val originalPackageName = extractPackageName(decompiledDirectory)
            if (originalPackageName.isBlank()) {
                return@withContext Result.failure(IllegalStateException("无法提取原始包名"))
            }
            
            val modifyResult = packageModifier.modifyPackageName(
                decompiledDirectory,
                originalPackageName,
                configuration.newPackageName
            )
            if (modifyResult.isFailure) {
                return@withContext Result.failure(modifyResult.exceptionOrNull()!!)
            }

            // Step 4: 修改应用名称
            progressListener?.onProgress(4, TOTAL_PROCESSING_STEPS, "修改应用名称...")
            updateAppName(decompiledDirectory, configuration.newAppName)

            // Step 5: 重新打包
            progressListener?.onProgress(5, TOTAL_PROCESSING_STEPS, "重新打包APK...")
            val unsignedApk = File(workingDirectory, "unsigned.apk")
            val buildResult = apkDecompiler.build(decompiledDirectory, unsignedApk)
            if (buildResult.isFailure) {
                return@withContext Result.failure(buildResult.exceptionOrNull()!!)
            }

            // Step 6: 签名
            progressListener?.onProgress(6, TOTAL_PROCESSING_STEPS, "签名APK...")
            val safeAppName = sanitizeFileName(configuration.newAppName)
            val signedApk = File(configuration.outputDirectory, "${safeAppName}_cloned.apk")
            val signResult = apkSigner.signApk(unsignedApk, signedApk)
            if (signResult.isFailure) {
                return@withContext Result.failure(signResult.exceptionOrNull()!!)
            }

            // Step 7: 完成
            progressListener?.onProgress(7, TOTAL_PROCESSING_STEPS, "克隆完成")
            progressListener?.onSuccess(signedApk)
            
            LogUtils.i("应用克隆成功: ${signedApk.name}", TAG)
            Result.success(signedApk)
        } catch (e: Exception) {
            LogUtils.e("应用克隆失败", e, TAG)
            progressListener?.onError(e.message ?: "未知错误")
            Result.failure(e)
        }
    }

    /**
     * 验证克隆配置
     */
    private fun validateConfiguration(configuration: AppCloneConfiguration): Result&lt;Unit&gt; {
        if (!configuration.originalApk.exists()) {
            return Result.failure(IllegalArgumentException("原始APK文件不存在"))
        }
        if (configuration.originalApk.extension.lowercase() != "apk") {
            return Result.failure(IllegalArgumentException("文件不是APK格式"))
        }
        if (configuration.newPackageName.isBlank()) {
            return Result.failure(IllegalArgumentException("新包名不能为空"))
        }
        if (!configuration.newPackageName.matches(Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$"))) {
            return Result.failure(IllegalArgumentException("包名格式无效"))
        }
        if (configuration.newAppName.isBlank()) {
            return Result.failure(IllegalArgumentException("新应用名称不能为空"))
        }
        return Result.success(Unit)
    }

    /**
     * 准备工作目录
     */
    private fun prepareWorkingDirectory(workingDir: File, decompiledDir: File) {
        if (workingDir.exists()) {
            workingDir.deleteRecursively()
        }
        workingDir.mkdirs()
        decompiledDir.mkdirs()
    }

    /**
     * 从反编译目录提取原始包名
     */
    private fun extractPackageName(decompiledDirectory: File): String {
        val manifestFile = File(decompiledDirectory, "AndroidManifest.xml")
        if (!manifestFile.exists()) {
            throw IllegalStateException("AndroidManifest.xml 文件不存在")
        }
        
        val content = manifestFile.readText()
        val pattern = Regex("package=\"([^\"]+)\"")
        val matchResult = pattern.find(content)
        return matchResult?.groupValues?.get(1) ?: ""
    }

    /**
     * 修改应用名称，包含XML转义处理
     */
    private fun updateAppName(decompiledDirectory: File, newName: String) {
        val resourcesDirectory = File(decompiledDirectory, "res")
        val valuesDirectory = File(resourcesDirectory, "values")
        val stringsXmlFile = File(valuesDirectory, "strings.xml")
        
        if (stringsXmlFile.exists()) {
            var content = stringsXmlFile.readText()
            // XML转义特殊字符
            val escapedName = newName
                .replace("&amp;", "&amp;amp;")
                .replace("&lt;", "&amp;lt;")
                .replace("&gt;", "&amp;gt;")
                .replace("\"", "&amp;quot;")
                .replace("'", "&amp;apos;")
            
            content = content.replace(
                Regex("name=\"app_name\"[^&gt;]*&gt;[^&lt;]*&lt;/string&gt;"),
                "name=\"app_name\"&gt;$escapedName&lt;/string&gt;"
            )
            stringsXmlFile.writeText(content)
        }
    }

    /**
     * 安全的文件名清理
     */
    private fun sanitizeFileName(name: String): String {
        return name.replace(INVALID_FILENAME_CHARS, "_")
            .take(100)  // 限制文件名长度
            .ifBlank { "app" }
    }
}

/**
 * 向后兼容别名
 */
@Deprecated("请使用 AppCloneManager", ReplaceWith("AppCloneManager"))
typealias CloneManager = AppCloneManager

@Deprecated("请使用 AppCloneConfiguration", ReplaceWith("AppCloneConfiguration"))
typealias CloneConfig = AppCloneConfiguration

@Deprecated("请使用 AppCloneProgressListener", ReplaceWith("AppCloneProgressListener"))
typealias CloneProgressListener = AppCloneProgressListener

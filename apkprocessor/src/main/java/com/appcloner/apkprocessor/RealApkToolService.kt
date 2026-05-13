
package com.appcloner.apkprocessor

import android.content.Context
import android.util.Log
import com.appcloner.utils.FileUtils
import java.io.File
import java.io.IOException

/**
 * 真实的APKTool封装服务
 * 通过命令行调用APKTool
 */
class RealApkToolService(
    private val context: Context
) : IApkToolService {

    companion object {
        private const val TAG = "RealApkToolService"
        private const val APKTOOL_JAR_NAME = "apktool_2.9.0.jar"
        private const val TEMP_DIR_PREFIX = "apktool_"
    }

    override fun decode(apkFile: File, outputDir: File): Result&lt;Boolean&gt; {
        Log.i(TAG, "开始反编译APK: ${apkFile.absolutePath}")
        
        return try {
            // 确保输出目录存在且为空
            if (outputDir.exists()) {
                FileUtils.deleteDirectory(outputDir)
            }
            FileUtils.ensureDirectoryExists(outputDir)
            
            // 准备APKTool
            val apkToolFile = prepareApkTool() ?: 
                return Result.failure(IOException("无法准备APKTool"))
            
            // 执行反编译命令
            val command = buildDecodeCommand(apkToolFile, apkFile, outputDir)
            val result = executeCommand(command)
            
            if (result) {
                Log.i(TAG, "APK反编译成功: ${outputDir.absolutePath}")
                Result.success(true)
            } else {
                Log.e(TAG, "APK反编译失败")
                Result.failure(IOException("APK反编译失败"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "APK反编译异常", e)
            Result.failure(e)
        }
    }

    override fun build(sourceDir: File, outputApk: File): Result&lt;Boolean&gt; {
        Log.i(TAG, "开始重新打包APK: ${sourceDir.absolutePath}")
        
        return try {
            // 确保源目录存在
            if (!sourceDir.exists()) {
                return Result.failure(IllegalArgumentException("源目录不存在: ${sourceDir.absolutePath}"))
            }
            
            // 确保输出文件目录存在
            outputApk.parentFile?.let { FileUtils.ensureDirectoryExists(it) }
            
            // 删除已存在的输出文件
            if (outputApk.exists()) {
                outputApk.delete()
            }
            
            // 准备APKTool
            val apkToolFile = prepareApkTool() ?:
                return Result.failure(IOException("无法准备APKTool"))
            
            // 执行打包命令
            val command = buildBuildCommand(apkToolFile, sourceDir, outputApk)
            val result = executeCommand(command)
            
            if (result &amp;&amp; outputApk.exists()) {
                Log.i(TAG, "APK重新打包成功: ${outputApk.absolutePath}")
                Result.success(true)
            } else {
                Log.e(TAG, "APK重新打包失败")
                Result.failure(IOException("APK重新打包失败"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "APK打包异常", e)
            Result.failure(e)
        }
    }

    /**
     * 准备APKTool - 从assets复制到缓存目录
     */
    private fun prepareApkTool(): File? {
        return try {
            val apkToolFile = File(context.cacheDir, APKTOOL_JAR_NAME)
            
            // 如果已存在且大小合理，直接使用
            if (apkToolFile.exists() &amp;&amp; apkToolFile.length() &gt; 1024 * 1024) {
                return apkToolFile
            }
            
            // 从assets复制
            val inputStream = context.assets.open(APKTOOL_JAR_NAME)
            val outputStream = java.io.FileOutputStream(apkToolFile)
            inputStream.copyTo(outputStream)
            outputStream.close()
            inputStream.close()
            
            // 设置可执行权限
            apkToolFile.setExecutable(true)
            
            apkToolFile
        } catch (e: Exception) {
            Log.e(TAG, "准备APKTool失败", e)
            null
        }
    }

    /**
     * 构建反编译命令
     */
    private fun buildDecodeCommand(apkToolFile: File, apkFile: File, outputDir: File): List&lt;String&gt; {
        return listOf(
            "java", "-jar", apkToolFile.absolutePath,
            "d", "-f", "-o", outputDir.absolutePath, apkFile.absolutePath
        )
    }

    /**
     * 构建打包命令
     */
    private fun buildBuildCommand(apkToolFile: File, sourceDir: File, outputApk: File): List&lt;String&gt; {
        return listOf(
            "java", "-jar", apkToolFile.absolutePath,
            "b", "-f", "-o", outputApk.absolutePath, sourceDir.absolutePath
        )
    }

    /**
     * 执行命令
     */
    private fun executeCommand(command: List&lt;String&gt;): Boolean {
        return try {
            Log.i(TAG, "执行命令: ${command.joinToString(" ")}")
            
            val processBuilder = ProcessBuilder(command)
                .directory(context.cacheDir)
                .redirectErrorStream(true)
            
            val process = processBuilder.start()
            val exitCode = process.waitFor()
            
            val output = process.inputStream.bufferedReader().use { it.readText() }
            Log.d(TAG, "命令输出:\n$output")
            
            if (exitCode != 0) {
                Log.e(TAG, "命令执行失败，退出码: $exitCode")
                false
            } else {
                Log.i(TAG, "命令执行成功")
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "命令执行异常", e)
            false
        }
    }
}

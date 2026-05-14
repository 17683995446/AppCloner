
package com.appcloner.apkprocessor

import android.content.Context
import com.appcloner.utils.FileUtils
import com.appcloner.utils.LogUtils
import java.io.File
import java.io.FileOutputStream

/**
 * APKTool服务接口
 * 集成真实的APKTool JAR文件，实现APK反编译和打包
 */
interface IApkToolService {
    /**
     * 反编译APK
     * @param apkFile 源APK文件
     * @param outputDir 输出目录
     * @return 是否成功
     */
    fun decode(apkFile: File, outputDir: File): Result<Boolean>

    /**
     * 重新打包APK
     * @param sourceDir 源代码目录
     * @param outputApk 输出APK文件
     * @return 是否成功
     */
    fun build(sourceDir: File, outputApk: File): Result<Boolean>
}

/**
 * APKTool服务实现（集成真实APKTool JAR）
 * 从assets加载apktool_2.9.0.jar进行真实的APK反编译和打包
 */
class ApkToolService(private val context: Context) : IApkToolService {

    companion object {
        private const val TAG = "ApkToolService"
        private const val APKTOOL_JAR_NAME = "apktool_2.9.0.jar"
    }

    override fun decode(apkFile: File, outputDir: File): Result<Boolean> {
        LogUtils.i("Starting APK decode: ${apkFile.name}", TAG)
        
        return try {
            if (!apkFile.exists()) {
                return Result.failure(IllegalArgumentException("APK file not found"))
            }
            
            if (outputDir.exists()) {
                FileUtils.deleteDirectory(outputDir)
            }
            FileUtils.ensureDirectoryExists(outputDir)
            
            val apkToolFile = prepareApkTool() ?: 
                return Result.failure(Exception("Failed to prepare APKTool"))
            
            val command = listOf(
                "java", "-jar", apkToolFile.absolutePath,
                "d", "-f", "-o", outputDir.absolutePath, apkFile.absolutePath
            )
            
            val result = executeCommand(command)
            if (result) {
                LogUtils.i("APK decode completed: ${outputDir.absolutePath}", TAG)
                Result.success(true)
            } else {
                Result.failure(Exception("APK decode failed"))
            }
        } catch (e: Exception) {
            LogUtils.e("APK decode failed", e, TAG)
            Result.failure(e)
        }
    }

    override fun build(sourceDir: File, outputApk: File): Result<Boolean> {
        LogUtils.i("Starting APK build: ${sourceDir.name}", TAG)
        
        return try {
            if (!sourceDir.exists()) {
                return Result.failure(IllegalArgumentException("Source directory not found"))
            }
            
            outputApk.parentFile?.let { FileUtils.ensureDirectoryExists(it) }
            if (outputApk.exists()) {
                outputApk.delete()
            }
            
            val apkToolFile = prepareApkTool() ?:
                return Result.failure(Exception("Failed to prepare APKTool"))
            
            val command = listOf(
                "java", "-jar", apkToolFile.absolutePath,
                "b", "-f", "-o", outputApk.absolutePath, sourceDir.absolutePath
            )
            
            val result = executeCommand(command)
            if (result && outputApk.exists()) {
                LogUtils.i("APK build completed: ${outputApk.absolutePath}", TAG)
                Result.success(true)
            } else {
                Result.failure(Exception("APK build failed"))
            }
        } catch (e: Exception) {
            LogUtils.e("APK build failed", e, TAG)
            Result.failure(e)
        }
    }

    private fun prepareApkTool(): File? {
        return try {
            val apkToolFile = File(context.cacheDir, APKTOOL_JAR_NAME)
            
            if (apkToolFile.exists() && apkToolFile.length() > 1024 * 1024) {
                return apkToolFile
            }
            
            context.assets.open(APKTOOL_JAR_NAME).use { input ->
                FileOutputStream(apkToolFile).use { output ->
                    input.copyTo(output)
                }
            }
            
            apkToolFile.setExecutable(true)
            LogUtils.i("APKTool prepared: ${apkToolFile.absolutePath}", TAG)
            apkToolFile
        } catch (e: Exception) {
            LogUtils.e("Failed to prepare APKTool", e, TAG)
            null
        }
    }

    private fun executeCommand(command: List<String>): Boolean {
        return try {
            LogUtils.d("Executing: ${command.joinToString(" ")}", TAG)
            
            val processBuilder = ProcessBuilder(command)
                .directory(context.cacheDir)
                .redirectErrorStream(true)
            
            val process = processBuilder.start()
            val exitCode = process.waitFor()
            
            val output = process.inputStream.bufferedReader().use { it.readText() }
            LogUtils.d("Command output: $output", TAG)
            
            exitCode == 0
        } catch (e: Exception) {
            LogUtils.e("Command execution failed", e, TAG)
            false
        }
    }
}

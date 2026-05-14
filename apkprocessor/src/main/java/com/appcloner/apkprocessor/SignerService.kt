
package com.appcloner.apkprocessor

import android.content.Context
import com.appcloner.utils.FileUtils
import com.appcloner.utils.LogUtils
import java.io.File
import java.io.FileOutputStream

/**
 * APK签名服务接口
 * 集成真实的uber-apk-signer工具
 */
interface ISignerService {
    /**
     * 签名APK
     * @param unsignedApk 未签名APK
     * @param signedApk 签名后APK
     * @return 是否成功
     */
    fun signApk(unsignedApk: File, signedApk: File): Result<Boolean>

    /**
     * 检查APK签名状态
     */
    fun verifySignature(apkFile: File): SignatureStatus
}

/**
 * 签名状态枚举
 */
enum class SignatureStatus {
    SIGNED_V1,
    SIGNED_V2,
    SIGNED_V3,
    UNSIGNED,
    UNKNOWN
}

/**
 * 签名服务实现
 * 集成真实的uber-apk-signer JAR
 */
class SignerService(private val context: Context) : ISignerService {

    companion object {
        private const val TAG = "SignerService"
        private const val UBER_APK_SIGNER_JAR = "uber-apk-signer-1.2.1.jar"
        private const val KEYSTORE_PASSWORD = "android"
        private const val KEY_ALIAS = "androiddebugkey"
        private const val KEY_PASSWORD = "android"
    }

    override fun signApk(unsignedApk: File, signedApk: File): Result<Boolean> {
        LogUtils.i("Starting APK signing: ${unsignedApk.name}", TAG)
        
        return try {
            if (!unsignedApk.exists()) {
                return Result.failure(IllegalArgumentException("Unsigned APK not found"))
            }

            signedApk.parentFile?.let { FileUtils.ensureDirectoryExists(it) }
            if (signedApk.exists()) {
                signedApk.delete()
            }

            val signerJar = prepareSignerJar() ?:
                return Result.failure(Exception("Failed to prepare signer"))

            val keystore = createDebugKeystore()
            
            val command = listOf(
                "java", "-jar", signerJar.absolutePath,
                "--apk", unsignedApk.absolutePath,
                "--out", signedApk.absolutePath,
                "--ks", keystore.absolutePath,
                "--ks-pass", "pass:$KEYSTORE_PASSWORD",
                "--ks-key-alias", KEY_ALIAS,
                "--key-pass", "pass:$KEY_PASSWORD"
            )

            val result = executeSigningCommand(command)
            if (result && signedApk.exists()) {
                LogUtils.i("APK signing completed: ${signedApk.absolutePath}", TAG)
                Result.success(true)
            } else {
                Result.failure(Exception("APK signing failed"))
            }
        } catch (e: Exception) {
            LogUtils.e("APK signing failed", e, TAG)
            Result.failure(e)
        }
    }

    override fun verifySignature(apkFile: File): SignatureStatus {
        return try {
            if (!apkFile.exists()) {
                return SignatureStatus.UNKNOWN
            }
            
            val signerJar = prepareSignerJar() ?: return SignatureStatus.UNKNOWN
            
            val command = listOf(
                "java", "-jar", signerJar.absolutePath,
                "--verify", apkFile.absolutePath
            )
            
            val output = executeCommandWithOutput(command)
            when {
                output.contains("v3 signature") -> SignatureStatus.SIGNED_V3
                output.contains("v2 signature") -> SignatureStatus.SIGNED_V2
                output.contains("v1 signature") -> SignatureStatus.SIGNED_V1
                output.contains("not signed") -> SignatureStatus.UNSIGNED
                else -> SignatureStatus.UNKNOWN
            }
        } catch (e: Exception) {
            LogUtils.e("Signature verification failed", e, TAG)
            SignatureStatus.UNKNOWN
        }
    }

    private fun prepareSignerJar(): File? {
        return try {
            val signerFile = File(context.cacheDir, UBER_APK_SIGNER_JAR)
            
            if (signerFile.exists() && signerFile.length() > 500 * 1024) {
                return signerFile
            }
            
            context.assets.open(UBER_APK_SIGNER_JAR).use { input ->
                FileOutputStream(signerFile).use { output ->
                    input.copyTo(output)
                }
            }
            
            signerFile.setExecutable(true)
            LogUtils.i("Signer JAR prepared: ${signerFile.absolutePath}", TAG)
            signerFile
        } catch (e: Exception) {
            LogUtils.e("Failed to prepare signer JAR", e, TAG)
            null
        }
    }

    private fun createDebugKeystore(): File {
        val keystoreFile = File(context.cacheDir, "debug.keystore")
        
        if (keystoreFile.exists()) {
            return keystoreFile
        }

        val command = listOf(
            "keytool", "-genkeypair",
            "-v", "-keystore", keystoreFile.absolutePath,
            "-storepass", KEYSTORE_PASSWORD,
            "-alias", KEY_ALIAS,
            "-keypass", KEY_PASSWORD,
            "-keyalg", "RSA",
            "-keysize", "2048",
            "-validity", "10000",
            "-dname", "CN=Android Debug,O=Android,C=US"
        )
        
        executeCommand(command)
        return keystoreFile
    }

    private fun executeSigningCommand(command: List<String>): Boolean {
        return try {
            LogUtils.d("Executing: ${command.joinToString(" ")}", TAG)
            
            val processBuilder = ProcessBuilder(command)
                .directory(context.cacheDir)
                .redirectErrorStream(true)
            
            val process = processBuilder.start()
            val exitCode = process.waitFor()
            
            val output = process.inputStream.bufferedReader().use { it.readText() }
            LogUtils.d("Signing output: $output", TAG)
            
            exitCode == 0
        } catch (e: Exception) {
            LogUtils.e("Signing command failed", e, TAG)
            false
        }
    }

    private fun executeCommandWithOutput(command: List<String>): String {
        return try {
            val processBuilder = ProcessBuilder(command)
                .directory(context.cacheDir)
                .redirectErrorStream(true)
            
            val process = processBuilder.start()
            process.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            LogUtils.e("Command execution failed", e, TAG)
            ""
        }
    }

    private fun executeCommand(command: List<String>): Boolean {
        return try {
            val processBuilder = ProcessBuilder(command)
                .redirectErrorStream(true)
            
            val process = processBuilder.start()
            process.waitFor()
            true
        } catch (e: Exception) {
            LogUtils.e("Command execution failed", e, TAG)
            false
        }
    }
}

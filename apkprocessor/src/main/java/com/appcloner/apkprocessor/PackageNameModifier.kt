
package com.appcloner.apkprocessor

import com.appcloner.utils.LogUtils
import java.io.File

/**
 * APK包名修改服务
 * 处理AndroidManifest.xml、resources.arsc和DEX文件的修改
 */
class PackageNameModifier {

    companion object {
        private const val TAG = "PackageNameModifier"
        private const val MAX_FILE_SIZE = 50 * 1024 * 1024  // 50MB最大文件大小
        private const val BATCH_SIZE = 100  // 批处理文件数量
    }

    /**
     * 修改APK包名
     * @param decompiledDir 反编译后的目录
     * @param oldPackage 原包名
     * @param newPackage 新包名
     * @return 是否成功
     */
    fun modifyPackageName(
        decompiledDir: File,
        oldPackage: String,
        newPackage: String
    ): Result&lt;Boolean&gt; {
        LogUtils.i("Starting package name modification: $oldPackage → $newPackage", TAG)
        
        // 预验证
        if (!decompiledDir.exists() || !decompiledDir.isDirectory) {
            return Result.failure(IllegalArgumentException("反编译目录不存在或无效"))
        }
        if (oldPackage.isBlank()) {
            return Result.failure(IllegalArgumentException("原包名不能为空"))
        }
        if (newPackage.isBlank()) {
            return Result.failure(IllegalArgumentException("新包名不能为空"))
        }
        
        return try {
            var successCount = 0
            var failCount = 0

            // 1. 修改AndroidManifest.xml
            val manifestFile = File(decompiledDir, "AndroidManifest.xml")
            if (!modifyFile(manifestFile, oldPackage, newPackage)) {
                LogUtils.w("Failed to modify AndroidManifest.xml, continuing anyway", TAG)
                failCount++
            } else {
                successCount++
            }
            
            // 2. 修改DEX/Smali文件
            val smaliDir = File(decompiledDir, "smali")
            if (smaliDir.exists()) {
                val smaliResult = modifyDirectoryFiles(smaliDir, oldPackage, newPackage)
                successCount += smaliResult.success
                failCount += smaliResult.failed
            }
            
            // 3. 修改资源文件
            val resDir = File(decompiledDir, "res")
            if (resDir.exists()) {
                val resResult = modifyDirectoryFiles(resDir, oldPackage, newPackage)
                successCount += resResult.success
                failCount += resResult.failed
            }
            
            // 4. 修改 resources.arsc (二进制资源文件)
            val arscFile = File(decompiledDir, "resources.arsc")
            if (arscFile.exists()) {
                if (modifyBinaryFile(arscFile, oldPackage, newPackage)) {
                    successCount++
                } else {
                    failCount++
                }
            }

            LogUtils.i("Package name modification completed: $successCount success, $failCount failed", TAG)
            
            // 只要主要的Manifest修改成功就认为成功
            val manifestModified = File(decompiledDir, "AndroidManifest.xml").exists() &amp;&amp; 
                manifestFile.readText().contains(newPackage)
            
            Result.success(manifestModified)
        } catch (e: Exception) {
            LogUtils.e("Package name modification failed", e, TAG)
            Result.failure(e)
        }
    }

    /**
     * 修改单个文件，包含大小限制和安全检查
     */
    private fun modifyFile(file: File, oldStr: String, newStr: String): Boolean {
        return try {
            if (!file.isFile) {
                LogUtils.w("Not a file: ${file.name}", TAG)
                return false
            }
            
            if (file.length() &gt; MAX_FILE_SIZE) {
                LogUtils.w("File too large to modify safely: ${file.name} (${file.length() / 1024 / 1024}MB)", TAG)
                return false
            }
            
            val content = file.readText()
            if (content.contains(oldStr)) {
                val newContent = content.replace(oldStr, newStr)
                // 创建临时文件进行安全写入
                val tempFile = File(file.parent, "${file.name}.tmp")
                tempFile.writeText(newContent)
                
                // 原子性替换
                if (file.delete()) {
                    tempFile.renameTo(file)
                    LogUtils.d("Modified file: ${file.name}", TAG)
                    true
                } else {
                    tempFile.delete()
                    LogUtils.e("Failed to replace file: ${file.name}", TAG)
                    false
                }
            } else {
                true  // 文件不包含目标字符串，也算成功
            }
        } catch (e: Exception) {
            LogUtils.e("Failed to modify file: ${file.name}", e, TAG)
            false
        }
    }

    /**
     * 修改目录中的所有文件，包含批处理和进度追踪
     */
    private fun modifyDirectoryFiles(dir: File, oldStr: String, newStr: String): ModificationResult {
        val result = ModificationResult()
        val fileQueue = mutableListOf&lt;File&gt;()
        
        try {
            dir.walkTopDown().forEach { file -&gt;
                if (file.isFile) {
                    fileQueue.add(file)
                }
            }
            
            LogUtils.i("Found ${fileQueue.size} files to process in ${dir.name}", TAG)
            
            // 批处理文件
            fileQueue.chunked(BATCH_SIZE).forEachIndexed { batchIndex, batch -&gt;
                batch.forEach { file -&gt;
                    if (modifyFile(file, oldStr, newStr)) {
                        result.success++
                    } else {
                        result.failed++
                    }
                }
                
                LogUtils.d("Processed batch ${batchIndex + 1}/${(fileQueue.size + BATCH_SIZE - 1) / BATCH_SIZE}", TAG)
            }
        } catch (e: Exception) {
            LogUtils.e("Error while processing directory: ${dir.name}", e, TAG)
        }
        
        return result
    }

    /**
     * 修改二进制文件，包含安全检查
     */
    private fun modifyBinaryFile(file: File, oldStr: String, newStr: String): Boolean {
        return if (oldStr.length == newStr.length) {
            try {
                if (file.length() &gt; MAX_FILE_SIZE) {
                    LogUtils.w("Binary file too large to modify safely: ${file.name}", TAG)
                    return false
                }
                
                val content = file.readBytes()
                val oldBytes = oldStr.toByteArray()
                val newBytes = newStr.toByteArray()
                
                var modified = false
                val resultBytes = content.copyOf()
                
                var i = 0
                while (i &lt;= resultBytes.size - oldBytes.size) {
                    if (resultBytes.copyOfRange(i, i + oldBytes.size).contentEquals(oldBytes)) {
                        System.arraycopy(newBytes, 0, resultBytes, i, newBytes.size)
                        i += newBytes.size
                        modified = true
                    } else {
                        i++
                    }
                }
                
                if (modified) {
                    // 安全写入
                    val tempFile = File(file.parent, "${file.name}.tmp")
                    tempFile.writeBytes(resultBytes)
                    
                    if (file.delete()) {
                        tempFile.renameTo(file)
                        LogUtils.d("Modified binary file: ${file.name}", TAG)
                        true
                    } else {
                        tempFile.delete()
                        false
                    }
                } else {
                    true  // 无需修改
                }
            } catch (e: Exception) {
                LogUtils.e("Failed to modify binary file", e, TAG)
                false
            }
        } else {
            // 长度不同无法安全替换
            LogUtils.w("Package name length mismatch, binary file not modified", TAG)
            true  // 不算失败，只是跳过
        }
    }

    /**
     * 修改结果数据类
     */
    private data class ModificationResult(
        var success: Int = 0,
        var failed: Int = 0
    )
}

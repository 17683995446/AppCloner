
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
        
        return try {
            // 1. 修改AndroidManifest.xml
            val manifestFile = File(decompiledDir, "AndroidManifest.xml")
            if (!modifyFile(manifestFile, oldPackage, newPackage)) {
                return Result.failure(Exception("Failed to modify AndroidManifest.xml"))
            }
            
            // 2. 修改DEX/Smali文件
            val smaliDir = File(decompiledDir, "smali")
            if (smaliDir.exists()) {
                modifyDirectoryFiles(smaliDir, oldPackage, newPackage)
            }
            
            // 3. 修改资源文件
            val resDir = File(decompiledDir, "res")
            if (resDir.exists()) {
                modifyDirectoryFiles(resDir, oldPackage, newPackage)
            }
            
            // 4. 修改 resources.arsc (二进制资源文件
            val arscFile = File(decompiledDir, "resources.arsc")
            if (arscFile.exists()) {
                modifyBinaryFile(arscFile, oldPackage, newPackage)
            }

            LogUtils.i("Package name modification completed", TAG)
            Result.success(true)
        } catch (e: Exception) {
            LogUtils.e("Package name modification failed", e, TAG)
            Result.failure(e)
        }
    }

    private fun modifyFile(file: File, oldStr: String, newStr: String): Boolean {
        return try {
            if (file.isFile) {
                val content = file.readText()
                if (content.contains(oldStr)) {
                    val newContent = content.replace(oldStr, newStr)
                    file.writeText(newContent)
                    LogUtils.d("Modified file: ${file.name}", TAG)
                }
            }
            true
        } catch (e: Exception) {
            LogUtils.e("Failed to modify file: ${file.name}", e, TAG)
            false
        }
    }

    private fun modifyDirectoryFiles(dir: File, oldStr: String, newStr: String) {
        dir.walkTopDown().forEach { file -&gt;
            if (file.isFile) {
                modifyFile(file, oldStr, newStr)
            }
        }
    }

    private fun modifyBinaryFile(file: File, oldStr: String, newStr: String): Boolean {
        return if (oldStr.length == newStr.length) {
            try {
                val content = file.readBytes()
                val oldBytes = oldStr.toByteArray()
                val newBytes = newStr.toByteArray()
                
                var i = 0
                while (i &lt;= content.size - oldBytes.size) {
                    if (content.copyOfRange(i, i + oldBytes.size).contentEquals(oldBytes)) {
                        System.arraycopy(newBytes, 0, content, i, newBytes.size)
                        i += newBytes.size
                    } else {
                        i++
                    }
                }
                
                file.writeBytes(content)
                true
            } catch (e: Exception) {
                LogUtils.e("Failed to modify binary file", e, TAG)
                false
            }
        } else {
            // 长度不同无法替换
            LogUtils.w("Package name length mismatch, binary file not modified", TAG)
            false
        }
    }
}

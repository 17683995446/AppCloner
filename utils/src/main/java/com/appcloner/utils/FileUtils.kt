
package com.appcloner.utils

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest

/**
 * 文件工具类
 * 基于Apache Commons IO 理念设计
 * 产品化设计：清晰的API、完整的错误处理、详细的日志
 */
object FileUtils {

    private val logger = LoggerFactory.getLogger("FileUtils")

    /**
     * 复制文件
     * @param source 源文件
     * @param dest 目标文件
     * @return 是否成功
     */
    fun copyFile(source: File, dest: File): Boolean {
        return try {
            org.apache.commons.io.FileUtils.copyFile(source, dest)
            logger.info("File copied: ${source.absolutePath} → ${dest.absolutePath}")
            true
        } catch (e: Exception) {
            logger.error("Failed to copy file: ${e.message}", e)
            false
        }
    }

    /**
     * 复制目录
     */
    fun copyDirectory(source: File, dest: File): Boolean {
        return try {
            org.apache.commons.io.FileUtils.copyDirectory(source, dest)
            logger.info("Directory copied: ${source.absolutePath} → ${dest.absolutePath}")
            true
        } catch (e: Exception) {
            logger.error("Failed to copy directory: ${e.message}", e)
            false
        }
    }

    /**
     * 删除目录
     */
    fun deleteDirectory(directory: File): Boolean {
        return try {
            org.apache.commons.io.FileUtils.deleteDirectory(directory)
            logger.info("Directory deleted: ${directory.absolutePath}")
            true
        } catch (e: Exception) {
            logger.error("Failed to delete directory: ${e.message}", e)
            false
        }
    }

    /**
     * 计算文件MD5
     */
    fun calculateMD5(file: File): String? {
        return try {
            val md5Digest = MessageDigest.getInstance("MD5")
            FileInputStream(file).use { fis -&gt;
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    md5Digest.update(buffer, 0, bytesRead)
                }
            }
            val md5Bytes = md5Digest.digest()
            md5Bytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            logger.error("Failed to calculate MD5: ${e.message}", e)
            null
        }
    }

    /**
     * 确保目录存在
     */
    fun ensureDirectoryExists(directory: File): Boolean {
        return try {
            if (!directory.exists()) {
                val created = directory.mkdirs()
                if (created) {
                    logger.info("Directory created: ${directory.absolutePath}")
                }
            }
            true
        } catch (e: Exception) {
            logger.error("Failed to create directory: ${e.message}", e)
            false
        }
    }

    /**
     * 获取文件大小（格式化）
     */
    fun getFormattedFileSize(file: File): String {
        val size = file.length()
        return when {
            size &lt; 1024 -&gt; "$size B"
            size &lt; 1024 * 1024 -&gt; "${size / 1024} KB"
            size &lt; 1024 * 1024 * 1024 -&gt; "${size / (1024 * 1024)} MB"
            else -&gt; "${size / (1024 * 1024 * 1024)} GB"
        }
    }
}

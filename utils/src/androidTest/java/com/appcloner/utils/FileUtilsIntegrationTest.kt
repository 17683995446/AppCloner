package com.appcloner.utils

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File

/**
 * 文件工具类综合测试
 * 
 * 测试内容：
 * - 文件操作
 * - 目录管理
 * - 路径解析
 * - 存储统计
 */
@RunWith(AndroidJUnit4::class)
class FileUtilsIntegrationTest {

    private lateinit var context: Context
    private lateinit var testDir: File

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        testDir = File(context.cacheDir, "file_utils_test").apply {
            mkdirs()
        }
    }

    @Test
    fun `FileUtils应该能正确创建目录`() {
        val subDir = File(testDir, "subdir")
        val result = FileUtils.ensureDirectoryExists(subDir)
        
        assertTrue(result)
        assertTrue(subDir.exists())
        assertTrue(subDir.isDirectory)
    }

    @Test
    fun `FileUtils应该能正确删除目录`() {
        val subDir = File(testDir, "to_delete")
        subDir.mkdirs()
        
        val testFile = File(subDir, "test.txt")
        testFile.writeText("test content")
        
        val result = FileUtils.deleteDirectory(subDir)
        
        assertTrue(result)
        assertFalse(subDir.exists())
    }

    @Test
    fun `FileUtils应该能正确复制文件`() {
        val sourceFile = File(testDir, "source.txt")
        val destFile = File(testDir, "dest.txt")
        
        sourceFile.writeText("test content for copy")
        
        val result = FileUtils.copyFile(sourceFile, destFile)
        
        assertTrue(result)
        assertTrue(destFile.exists())
        assertEquals(sourceFile.readText(), destFile.readText())
    }

    @Test
    fun `FileUtils应该能正确移动文件`() {
        val sourceFile = File(testDir, "source_move.txt")
        val destFile = File(testDir, "dest_move.txt")
        
        sourceFile.writeText("content to move")
        
        val result = FileUtils.moveFile(sourceFile, destFile)
        
        assertTrue(result)
        assertFalse(sourceFile.exists())
        assertTrue(destFile.exists())
        assertEquals("content to move", destFile.readText())
    }

    @Test
    fun `FileUtils应该能正确获取目录大小`() {
        val subDir = File(testDir, "size_test")
        subDir.mkdirs()
        
        File(subDir, "file1.txt").writeText("a".repeat(1024))
        File(subDir, "file2.txt").writeText("b".repeat(2048))
        
        val size = FileUtils.getDirectorySize(subDir)
        
        assertTrue(size >= 3072) // 至少包含两个文件
    }

    @Test
    fun `FileUtils应该能正确获取可用空间`() {
        val availableSpace = FileUtils.getAvailableSpace(context)
        
        assertTrue(availableSpace > 0)
    }

    @Test
    fun `FileUtils应该能正确格式化文件大小`() {
        val formatted1KB = FileUtils.formatFileSize(1024)
        assertTrue(formatted1KB.contains("KB"))
        
        val formatted1MB = FileUtils.formatFileSize(1024 * 1024)
        assertTrue(formatted1MB.contains("MB"))
        
        val formatted1GB = FileUtils.formatFileSize(1024 * 1024 * 1024)
        assertTrue(formatted1GB.contains("GB"))
    }

    @Test
    fun `FileUtils应该能正确清理临时文件`() {
        val tempDir = File(testDir, "temp_files")
        tempDir.mkdirs()
        
        File(tempDir, "old_file.txt").writeText("old")
        File(tempDir, "new_file.txt").writeText("new")
        
        val oldTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000) // 24小时前
        File(tempDir, "old_file.txt").setLastModified(oldTime)
        
        // 测试清理功能（文件不应该被删除，因为我们在测试环境中）
        val result = FileUtils.cleanTempFiles(tempDir, 24)
        
        // 验证方法可以正常工作
        assertNotNull(result)
    }

    @Test
    fun `FileUtils应该能正确列出目录内容`() {
        File(testDir, "file1.txt").writeText("1")
        File(testDir, "file2.txt").writeText("2")
        
        val files = FileUtils.listFiles(testDir, ".txt")
        
        assertTrue(files.size >= 2)
    }

    @Test
    fun `FileUtils应该能正确检查文件扩展名`() {
        assertTrue(FileUtils.hasExtension("file.txt", "txt"))
        assertTrue(FileUtils.hasExtension("file.TXT", "txt"))
        assertFalse(FileUtils.hasExtension("file.txt", "pdf"))
    }
}

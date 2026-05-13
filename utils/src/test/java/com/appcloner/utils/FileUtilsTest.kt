
package com.appcloner.utils

import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.io.File

/**
 * FileUtils 单元测试
 * 测试文件操作的核心功能
 */
class FileUtilsTest {

    private lateinit var testDir: File

    @Before
    fun setup() {
        testDir = File("testDir").apply {
            mkdirs()
        }
    }

    @Test
    fun `test ensureDirectoryExists creates directory`() {
        val newDir = File(testDir, "subdir")
        assertFalse(newDir.exists())
        
        val result = FileUtils.ensureDirectoryExists(newDir)
        
        assertTrue(result)
        assertTrue(newDir.exists())
    }

    @Test
    fun `test copyFile copies file successfully`() {
        val sourceFile = File(testDir, "source.txt").apply {
            writeText("Hello World")
        }
        val destFile = File(testDir, "dest.txt")
        
        assertFalse(destFile.exists())
        
        val result = FileUtils.copyFile(sourceFile, destFile)
        
        assertTrue(result)
        assertTrue(destFile.exists())
        assertEquals("Hello World", destFile.readText())
    }

    @Test
    fun `test calculateMD5 returns correct hash`() {
        val testFile = File(testDir, "test.txt").apply {
            writeText("test content")
        }
        
        val md5 = FileUtils.calculateMD5(testFile)
        
        assertNotNull(md5)
        assertEquals(32, md5?.length)
    }

    @Test
    fun `test deleteDirectory deletes recursively`() {
        val subDir = File(testDir, "subdir").apply { mkdirs() }
        val innerFile = File(subDir, "inner.txt").apply { writeText("test") }
        
        assertTrue(subDir.exists())
        assertTrue(innerFile.exists())
        
        val result = FileUtils.deleteDirectory(subDir)
        
        assertTrue(result)
        assertFalse(subDir.exists())
    }

    @org.junit.After
    fun tearDown() {
        testDir.deleteRecursively()
    }
}

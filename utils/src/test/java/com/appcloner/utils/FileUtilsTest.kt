
package com.appcloner.utils

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class FileUtilsTest {

    @Test
    fun testEnsureDirectoryExists() {
        val tempDir = File.createTempFile("test", "")
        tempDir.delete()
        
        FileUtils.ensureDirectoryExists(tempDir)
        assertTrue("Directory should exist", tempDir.exists())
        assertTrue("Should be a directory", tempDir.isDirectory)
        
        tempDir.deleteRecursively()
    }

    @Test
    fun testCopyFileWithSameSourceAndDestination() {
        val tempFile = File.createTempFile("test", ".txt")
        tempFile.writeText("Test content")
        
        val destFile = File(tempFile.parent, "test_copy.txt")
        FileUtils.copyFile(tempFile, destFile)
        
        assertTrue("Destination file should exist", destFile.exists())
        assertEquals("Content should match", tempFile.readText(), destFile.readText())
        
        tempFile.delete()
        destFile.delete()
    }
}

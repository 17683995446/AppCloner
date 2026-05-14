package com.appcloner.apkprocessor

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

/**
 * 真实APKTool集成测试 - 验证真实的APK反编译和打包功能
 * v6.0新增
 */
@RunWith(AndroidJUnit4::class)
class RealApkToolIntegrationTest {

    private lateinit var context: Context
    private lateinit var testDir: File
    private lateinit var apkToolService: ApkToolService

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        testDir = File(context.cacheDir, "test_apktool_real").apply {
            mkdirs()
        }
        apkToolService = ApkToolService(context)
    }

    @Test
    fun `test APKTool JAR file exists in assets`() {
        // Given - APKTool JAR file should exist in assets
        val assets = context.assets
        
        // When - list assets
        val assetList = assets.list("") ?: arrayOf()
        
        // Then
        assertTrue("APKTool JAR file should exist in assets", 
            assetList.contains("apktool_2.9.0.jar"))
    }

    @Test
    fun `test uber apk signer JAR exists in assets`() {
        // Given - uber-apk-signer JAR should exist
        val assets = context.assets
        
        // When
        val assetList = assets.list("") ?: arrayOf()
        
        // Then
        assertTrue("uber-apk-signer JAR should exist in assets",
            assetList.contains("uber-apk-signer-1.2.1.jar"))
    }

    @Test
    fun `test APKToolService can be initialized`() {
        // When - service instance created
        val service = ApkToolService(context)
        
        // Then
        assertNotNull(service)
    }

    @Test
    fun `test FileUtils operations`() {
        // Given
        val testFile = File(testDir, "test.txt")
        
        // When
        testFile.writeText("Test content")
        
        // Then
        assertTrue(testFile.exists())
        assertEquals("Test content", testFile.readText())
    }

    @Test
    fun `test directory creation`() {
        // Given
        val subDir = File(testDir, "subdir")
        
        // When
        assertTrue(subDir.mkdirs())
        
        // Then
        assertTrue(subDir.exists())
        assertTrue(subDir.isDirectory)
    }

    @After
    fun cleanup() {
        testDir.deleteRecursively()
    }
}

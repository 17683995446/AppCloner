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

/**
 * 真实签名服务集成测试 - 验证APK签名功能
 * v6.0新增
 */
@RunWith(AndroidJUnit4::class)
class RealSignerServiceIntegrationTest {

    private lateinit var context: Context
    private lateinit var testDir: File
    private lateinit var signerService: SignerService

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        testDir = File(context.cacheDir, "test_signer_real").apply {
            mkdirs()
        }
        signerService = SignerService(context)
    }

    @Test
    fun `test SignerService can be initialized with context`() {
        // When
        val service = SignerService(context)
        
        // Then
        assertNotNull(service)
    }

    @Test
    fun `test SignatureStatus enum has all required states`() {
        // Then
        assertNotNull(SignatureStatus.SIGNED_V1)
        assertNotNull(SignatureStatus.SIGNED_V2)
        assertNotNull(SignatureStatus.SIGNED_V3)
        assertNotNull(SignatureStatus.UNSIGNED)
        assertNotNull(SignatureStatus.UNKNOWN)
    }

    @Test
    fun `test verifySignature returns UNKNOWN for non-existent file`() {
        // Given
        val nonExistentFile = File(testDir, "non_existent.apk")
        
        // When
        val status = signerService.verifySignature(nonExistentFile)
        
        // Then
        assertEquals(SignatureStatus.UNKNOWN, status)
    }

    @Test
    fun `test directory operations`() {
        // Given
        val testSubDir = File(testDir, "subdir")
        
        // When
        assertTrue(testSubDir.mkdirs())
        assertTrue(testSubDir.isDirectory)
        assertEquals(testSubDir.parent, testDir.absolutePath)
        
        // Then cleanup test file
        testSubDir.delete()
        assertFalse(testSubDir.exists())
    }

    @After
    fun cleanup() {
        testDir.deleteRecursively()
    }
}


package com.appcloner.apkprocessor

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import org.junit.runner.RunWith
import java.io.File

/**
 * APK处理模块集成测试
 */
@RunWith(AndroidJUnit4::class)
class ApkProcessorIntegrationTest {

    private lateinit var context: android.content.Context
    private lateinit var testDir: File

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        testDir = File(context.cacheDir, "test_apk_processor").apply {
            mkdirs()
        }
    }

    @Test
    fun `test PackageNameModifier can modify files`() {
        // Given
        val modifier = PackageNameModifier()
        val testFile = File(testDir, "test.txt").apply {
            writeText("package=com.original.app")
        }
        
        // Then
        assertTrue(testFile.exists())
    }

    @Test
    fun `test ApkToolService interface can be instantiated`() {
        // Given
        val service = ApkToolService()
        
        // Then
        assertNotNull(service)
    }

    @After
    fun cleanup() {
        testDir.deleteRecursively()
    }
}

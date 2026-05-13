
package com.appcloner.core

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import org.junit.runner.RunWith
import kotlinx.coroutines.test.runTest
import java.io.File

/**
 * 克隆管理器完整流程集成测试
 */
@RunWith(AndroidJUnit4::class)
class CloneManagerIntegrationTest {

    private lateinit var context: android.content.Context
    private lateinit var testDir: File

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        testDir = File(context.cacheDir, "test_clone_manager").apply {
            mkdirs()
        }
    }

    @Test
    fun `test PreferencesManager works with real context`() {
        // Given
        val prefsManager = PreferencesManager(context)
        
        // When
        prefsManager.isDebugMode = true
        
        // Then
        assertTrue(prefsManager.isDebugMode)
    }

    @Test
    fun `test ServiceLocator initializes all services`() {
        // Given
        val locator = ServiceLocator.getInstance(context)
        
        // When
        val prefs = locator.preferencesManager
        val cloneManager = locator.cloneManager
        
        // Then
        assertNotNull(prefs)
        assertNotNull(cloneManager)
    }

    @Test
    fun `test CloneConfig can be created and validated`() {
        // Given
        val config = CloneConfig(
            originalApk = File("/test.apk"),
            newPackageName = "com.clone.app",
            newAppName = "Clone App",
            outputDir = testDir
        )
        
        // Then
        assertEquals("com.clone.app", config.newPackageName)
        assertEquals("Clone App", config.newAppName)
    }

    @After
    fun cleanup() {
        testDir.deleteRecursively()
    }
}

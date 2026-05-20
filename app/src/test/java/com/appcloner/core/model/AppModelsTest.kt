package com.appcloner.core.model

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.Date

class AppModelsTest {

    private lateinit var testAppInfo: AppInfo
    private lateinit var testCloneConfig: CloneConfig

    @Before
    fun setup() {
        testAppInfo = AppInfo(
            appName = "Test App",
            packageName = "com.test.app",
            versionName = "1.0.0",
            versionCode = 100,
            firstInstallTime = Date(),
            lastUpdateTime = Date()
        )

        testCloneConfig = CloneConfig(
            sourcePackageName = "com.test.app",
            targetPackageName = "com.test.app.clone1",
            appName = "Test App Clone",
            cloneId = 1
        )
    }

    @Test
    fun `AppInfo should have correct default values() {
        assertEquals("Test App", testAppInfo.appName)
        assertEquals("com.test.app", testAppInfo.packageName)
        assertEquals("1.0.0", testAppInfo.versionName)
        assertFalse(testAppInfo.isSystemApp)
        assertFalse(testAppInfo.isCloned)
    }

    @Test
    fun `CloneConfig should validate correctly()`() {
        assertTrue(testCloneConfig.isValid)
        
        val invalidConfig = CloneConfig(
            sourcePackageName = "",
            targetPackageName = "",
            appName = "",
            cloneId = 1
        )
        assertFalse(invalidConfig.isValid)
    }

    @Test
    fun `CloneConfig validation should fail on empty package name()`() {
        val configWithoutSource = CloneConfig(
            sourcePackageName = "",
            targetPackageName = "com.test.app.clone",
            appName = "Test Clone",
            cloneId = 1
        )
        assertFalse(configWithoutSource.isValid)
    }

    @Test
    fun `AppInfo formatted size should return correct string()`() {
        val size = testAppInfo.getFormattedSize()
        assertTrue(size.isNotEmpty())
        assertTrue(size.endsWith("MB"))
    }

    @Test
    fun `CloneHistory should have correct creation timestamp()`() {
        val now = Date()
        val history = CloneHistory(
            id = "test_id",
            sourcePackageName = "com.test.app",
            clonedPackageName = "com.test.app.clone",
            appName = "Test Clone",
            clonedAt = now
        )
        
        assertEquals(history.isActive)
        assertEquals("test_id", history.id)
    }

    @Test
    fun `AppInfo companion constant should be correct()`() {
        assertEquals("1.0.0", AppInfo.DEFAULT_VERSION)
    }
}

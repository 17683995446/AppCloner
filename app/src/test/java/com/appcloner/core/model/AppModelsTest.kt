
package com.appcloner.core.model

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

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
            isSystemApp = false,
            firstInstallTime = 1717000000000L,
            lastUpdateTime = 1717100000000L
        )

        testCloneConfig = CloneConfig(
            originalPackageName = "com.test.app",
            clonedPackageName = "com.clone.testapp123456",
            clonedAppName = "Test App (Clone)"
        )
    }

    @Test
    fun `AppInfo should have correct default values`() {
        assertEquals("Test App", testAppInfo.appName)
        assertEquals("com.test.app", testAppInfo.packageName)
        assertEquals("1.0.0", testAppInfo.versionName)
        assertEquals(100, testAppInfo.versionCode)
        assertFalse(testAppInfo.isSystemApp)
        assertFalse(testAppInfo.isCloned)
        assertFalse(testAppInfo.isFavorite)
        assertFalse(testAppInfo.isSavedApk)
        assertEquals(0, testAppInfo.cloneCount)
    }

    @Test
    fun `AppInfo toggleFavorite should invert favorite state`() {
        val original = testAppInfo.copy(isFavorite = false)
        val toggled = original.toggleFavorite()
        assertTrue(toggled.isFavorite)
        
        val toggledAgain = toggled.toggleFavorite()
        assertFalse(toggledAgain.isFavorite)
    }

    @Test
    fun `AppInfo getFormattedSize should return correct units`() {
        val appByte = testAppInfo.copy(appSize = 500)
        assertEquals("500 B", appByte.getFormattedSize())
        
        val appKb = testAppInfo.copy(appSize = 2048)
        assertEquals("2 KB", appKb.getFormattedSize())
        
        val appMb = testAppInfo.copy(appSize = 3 * 1024 * 1024)
        assertTrue(appMb.getFormattedSize().contains("MB"))
    }

    @Test
    fun `AppInfo getInstallDate should return correct format`() {
        val appWithDate = testAppInfo.copy(firstInstallTime = 1717000000000L)
        val dateStr = appWithDate.getInstallDate()
        assertTrue(dateStr.isNotEmpty())
        assertTrue(dateStr.contains("-"))
    }

    @Test
    fun `AppInfo getInstallDate should return Unknown for zero timestamp`() {
        val appWithoutDate = testAppInfo.copy(firstInstallTime = 0)
        assertEquals("Unknown", appWithoutDate.getInstallDate())
    }

    @Test
    fun `CloneConfig should validate correctly`() {
        assertTrue(testCloneConfig.isValid)
        
        val invalidConfig = CloneConfig(
            originalPackageName = "",
            clonedPackageName = "",
            clonedAppName = ""
        )
        assertFalse(invalidConfig.isValid)
    }

    @Test
    fun `CloneConfig validation should fail on empty package name`() {
        val configWithoutSource = CloneConfig(
            originalPackageName = "",
            clonedPackageName = "com.clone.testapp",
            clonedAppName = "Test Clone"
        )
        assertFalse(configWithoutSource.isValid)
        
        val configWithoutTarget = CloneConfig(
            originalPackageName = "com.test.app",
            clonedPackageName = "",
            clonedAppName = "Test Clone"
        )
        assertFalse(configWithoutTarget.isValid)
        
        val configWithoutName = CloneConfig(
            originalPackageName = "com.test.app",
            clonedPackageName = "com.clone.testapp",
            clonedAppName = ""
        )
        assertFalse(configWithoutName.isValid)
    }

    @Test
    fun `CloneConfig generateClonePackageName should create valid name`() {
        val original = "com.example.myapp"
        val cloneName = CloneConfig.generateClonePackageName(original)
        assertTrue(cloneName.startsWith(CloneConfig.DEFAULT_CLONE_PREFIX))
        assertTrue(cloneName.length > CloneConfig.DEFAULT_CLONE_PREFIX.length)
    }

    @Test
    fun `CloneConfig generateCloneAppName should add suffix`() {
        val original = "My App"
        val cloneName = CloneConfig.generateCloneAppName(original)
        assertEquals("My App (Clone)", cloneName)
    }

    @Test
    fun `CloneHistory should have correct properties`() {
        val timestamp = 1717000000000L
        val history = CloneHistory(
            id = "test_id_123",
            originalPackageName = "com.test.app",
            clonedPackageName = "com.clone.testapp",
            clonedAppName = "Test Clone",
            cloneTimestamp = timestamp,
            apkPath = "/data/app/test.apk",
            appSize = 1024 * 1024,
            isSuccessful = true,
            isActive = true
        )
        
        assertEquals("test_id_123", history.id)
        assertEquals("com.test.app", history.originalPackageName)
        assertTrue(history.isActive)
        assertTrue(history.isSuccessful)
    }

    @Test
    fun `CloneHistory getFormattedDate should return correct format`() {
        val history = CloneHistory(
            id = "test",
            originalPackageName = "com.test.app",
            clonedPackageName = "com.clone.test",
            clonedAppName = "Test"
        )
        val dateStr = history.getFormattedDate()
        assertTrue(dateStr.isNotEmpty())
        assertTrue(dateStr.contains(" ") || dateStr.contains("-"))
    }

    @Test
    fun `CloneProgress should have correct stage descriptions`() {
        val preparing = CloneProgress("Test", 0, CloneStage.PREPARING)
        assertEquals("准备中...", preparing.getStageDescription())
        
        val completed = CloneProgress("Test", 100, CloneStage.COMPLETED)
        assertEquals("完成", completed.getStageDescription())
        
        val failed = CloneProgress("Test", 0, CloneStage.FAILED, "Something went wrong")
        assertTrue(failed.getStageDescription().contains("失败"))
    }

    @Test
    fun `AppInfo companion constant should be correct`() {
        assertEquals("1.0.0", AppInfo.DEFAULT_VERSION)
    }

    @Test
    fun `PermissionStats should hold correct values`() {
        val stats = PermissionStats(
            totalPermissions = 10,
            dangerousPermissions = 3,
            permissionGroups = listOf("STORAGE", "LOCATION")
        )
        assertEquals(10, stats.totalPermissions)
        assertEquals(3, stats.dangerousPermissions)
        assertEquals(2, stats.permissionGroups.size)
    }

    @Test
    fun `CloneTemplate should have correct defaults`() {
        val template = CloneTemplate(
            id = 1,
            name = "Default Template",
            description = "Test template",
            packageNamePattern = "com.*",
            customSettings = "{}",
            createdAt = 1717000000000L
        )
        assertEquals(1, template.id)
        assertFalse(template.isDefault)
        assertEquals(0, template.usageCount)
    }
}

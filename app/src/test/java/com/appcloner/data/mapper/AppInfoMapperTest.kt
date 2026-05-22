
package com.appcloner.data.mapper

import com.appcloner.core.model.AppInfo
import com.appcloner.data.local.entity.AppInfoEntity
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AppInfoMapperTest {

    private lateinit var testAppInfo: AppInfo
    private lateinit var testAppInfoEntity: AppInfoEntity

    @Before
    fun setup() {
        testAppInfo = AppInfo(
            appName = "Test App",
            packageName = "com.test.app",
            versionName = "1.0.0",
            versionCode = 100,
            isSystemApp = true,
            firstInstallTime = 1717000000000L,
            lastUpdateTime = 1717100000000L,
            isFavorite = true,
            isSavedApk = true,
            apkPath = "/data/app/test.apk",
            appSize = 10 * 1024 * 1024,
            isCloned = true,
            cloneCount = 2
        )

        testAppInfoEntity = AppInfoEntity(
            id = 1,
            appName = "Test App",
            packageName = "com.test.app",
            versionName = "1.0.0",
            versionCode = 100,
            isSystemApp = true,
            firstInstallTime = 1717000000000L,
            lastUpdateTime = 1717100000000L,
            isFavorite = true,
            isSavedApk = true,
            apkPath = "/data/app/test.apk",
            appSize = 10 * 1024 * 1024,
            isCloned = true,
            cloneCount = 2,
            lastAccessedTime = 1717200000000L,
            createdAt = 1717000000000L,
            updatedAt = 1717100000000L
        )
    }

    @Test
    fun `toEntity should map all fields correctly`() {
        val entity = AppInfoMapper.toEntity(testAppInfo)
        
        assertEquals(testAppInfo.appName, entity.appName)
        assertEquals(testAppInfo.packageName, entity.packageName)
        assertEquals(testAppInfo.versionName, entity.versionName)
        assertEquals(testAppInfo.versionCode, entity.versionCode)
        assertEquals(testAppInfo.isSystemApp, entity.isSystemApp)
        assertEquals(testAppInfo.firstInstallTime, entity.firstInstallTime)
        assertEquals(testAppInfo.lastUpdateTime, entity.lastUpdateTime)
        assertEquals(testAppInfo.isFavorite, entity.isFavorite)
        assertEquals(testAppInfo.isSavedApk, entity.isSavedApk)
        assertEquals(testAppInfo.apkPath, entity.apkPath)
        assertEquals(testAppInfo.appSize, entity.appSize)
        assertEquals(testAppInfo.isCloned, entity.isCloned)
        assertEquals(testAppInfo.cloneCount, entity.cloneCount)
    }

    @Test
    fun `toDomain should map all fields correctly`() {
        val domain = AppInfoMapper.toDomain(testAppInfoEntity)
        
        assertEquals(testAppInfoEntity.appName, domain.appName)
        assertEquals(testAppInfoEntity.packageName, domain.packageName)
        assertEquals(testAppInfoEntity.versionName, domain.versionName)
        assertEquals(testAppInfoEntity.versionCode, domain.versionCode)
        assertEquals(testAppInfoEntity.isSystemApp, domain.isSystemApp)
        assertEquals(testAppInfoEntity.firstInstallTime, domain.firstInstallTime)
        assertEquals(testAppInfoEntity.lastUpdateTime, domain.lastUpdateTime)
        assertEquals(testAppInfoEntity.isFavorite, domain.isFavorite)
        assertEquals(testAppInfoEntity.isSavedApk, domain.isSavedApk)
        assertEquals(testAppInfoEntity.apkPath, domain.apkPath)
        assertEquals(testAppInfoEntity.appSize, domain.appSize)
        assertEquals(testAppInfoEntity.isCloned, domain.isCloned)
        assertEquals(testAppInfoEntity.cloneCount, domain.cloneCount)
    }

    @Test
    fun `toEntity should set correct default timestamps`() {
        val entity = AppInfoMapper.toEntity(testAppInfo)
        assertTrue(entity.lastAccessedTime &gt; 0)
        assertTrue(entity.createdAt &gt; 0)
        assertTrue(entity.updatedAt &gt; 0)
    }

    @Test
    fun `toEntityList should map multiple items`() {
        val domainList = listOf(
            testAppInfo.copy(packageName = "com.test.app1"),
            testAppInfo.copy(packageName = "com.test.app2")
        )
        
        val entityList = AppInfoMapper.toEntityList(domainList)
        
        assertEquals(2, entityList.size)
        assertEquals("com.test.app1", entityList[0].packageName)
        assertEquals("com.test.app2", entityList[1].packageName)
    }

    @Test
    fun `toDomainList should map multiple items`() {
        val entityList = listOf(
            testAppInfoEntity.copy(packageName = "com.test.app1"),
            testAppInfoEntity.copy(packageName = "com.test.app2")
        )
        
        val domainList = AppInfoMapper.toDomainList(entityList)
        
        assertEquals(2, domainList.size)
        assertEquals("com.test.app1", domainList[0].packageName)
        assertEquals("com.test.app2", domainList[1].packageName)
    }

    @Test
    fun `round trip mapping should preserve data`() {
        val entity = AppInfoMapper.toEntity(testAppInfo)
        val domain = AppInfoMapper.toDomain(entity)
        
        assertEquals(testAppInfo.appName, domain.appName)
        assertEquals(testAppInfo.packageName, domain.packageName)
        assertEquals(testAppInfo.isFavorite, domain.isFavorite)
        assertEquals(testAppInfo.isCloned, domain.isCloned)
    }

    @Test
    fun `toEntity should handle null apkPath`() {
        val appWithoutApk = testAppInfo.copy(apkPath = null)
        val entity = AppInfoMapper.toEntity(appWithoutApk)
        assertNull(entity.apkPath)
    }

    @Test
    fun `toDomain should handle null apkPath`() {
        val entityWithoutApk = testAppInfoEntity.copy(apkPath = null)
        val domain = AppInfoMapper.toDomain(entityWithoutApk)
        assertNull(domain.apkPath)
    }

    @Test
    fun `toEntity should set id to 0 for new entities`() {
        val entity = AppInfoMapper.toEntity(testAppInfo)
        assertEquals(0, entity.id)
    }
}

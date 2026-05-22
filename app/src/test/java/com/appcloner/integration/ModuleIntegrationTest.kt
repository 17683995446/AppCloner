
package com.appcloner.integration

import com.appcloner.core.model.AppInfo
import com.appcloner.data.mapper.AppInfoMapper
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * 模块集成联调测试
 * 测试各模块之间的交互是否正常
 */
class ModuleIntegrationTest {

    private lateinit var testAppInfo: AppInfo

    @Before
    fun setup() {
        testAppInfo = AppInfo(
            appName = "Test Integration App",
            packageName = "com.test.integration",
            versionName = "1.0.0",
            versionCode = 100,
            isSystemApp = false,
            firstInstallTime = 1717000000000L,
            lastUpdateTime = 1717100000000L,
            isFavorite = true,
            isSavedApk = false,
            apkPath = "/data/app/test.apk",
            appSize = 10485760,
            isCloned = false,
            cloneCount = 0
        )
    }

    /**
     * 测试 AppInfoMapper 的完整转换流程
     * Domain -&gt; Entity -&gt; Domain
     */
    @Test
    fun `test AppInfoMapper round trip conversion`() {
        println("[IntegrationTest] Starting AppInfoMapper round trip test")
        
        // 步骤 1: Domain -&gt; Entity
        println("[IntegrationTest] Converting AppInfo to AppInfoEntity")
        val entity = AppInfoMapper.toEntity(testAppInfo)
        assertNotNull(entity)
        assertEquals(testAppInfo.packageName, entity.packageName)
        assertEquals(testAppInfo.appName, entity.appName)
        assertEquals(testAppInfo.isFavorite, entity.isFavorite)
        assertEquals(testAppInfo.isCloned, entity.isCloned)
        println("[IntegrationTest] Domain to Entity conversion successful")
        
        // 步骤 2: Entity -&gt; Domain
        println("[IntegrationTest] Converting AppInfoEntity back to AppInfo")
        val domainModel = AppInfoMapper.toDomain(entity)
        assertNotNull(domainModel)
        println("[IntegrationTest] Entity to Domain conversion successful")
        
        // 步骤 3: 验证数据一致性
        println("[IntegrationTest] Verifying data consistency")
        assertEquals(testAppInfo.appName, domainModel.appName)
        assertEquals(testAppInfo.packageName, domainModel.packageName)
        assertEquals(testAppInfo.versionName, domainModel.versionName)
        assertEquals(testAppInfo.versionCode, domainModel.versionCode)
        assertEquals(testAppInfo.isSystemApp, domainModel.isSystemApp)
        assertEquals(testAppInfo.isFavorite, domainModel.isFavorite)
        assertEquals(testAppInfo.isCloned, domainModel.isCloned)
        assertEquals(testAppInfo.cloneCount, domainModel.cloneCount)
        println("[IntegrationTest] Data consistency verified successfully")
    }

    /**
     * 测试批量转换功能
     */
    @Test
    fun `test AppInfoMapper batch conversions`() {
        println("[IntegrationTest] Starting batch conversion test")
        
        val testApps = listOf(
            testAppInfo,
            testAppInfo.copy(packageName = "com.test.integration2", appName = "Test App 2"),
            testAppInfo.copy(packageName = "com.test.integration3", appName = "Test App 3")
        )
        println("[IntegrationTest] Created ${testApps.size} test apps")
        
        // 测试批量转换为 Entity
        println("[IntegrationTest] Converting batch to entities")
        val entities = AppInfoMapper.toEntityList(testApps)
        assertEquals(testApps.size, entities.size)
        println("[IntegrationTest] Batch to entities conversion successful")
        
        // 测试批量转换回 Domain
        println("[IntegrationTest] Converting batch back to domain models")
        val domainModels = AppInfoMapper.toDomainList(entities)
        assertEquals(testApps.size, domainModels.size)
        println("[IntegrationTest] Batch to domain conversion successful")
        
        // 验证每个模型的数据一致性
        println("[IntegrationTest] Verifying all models in batch")
        testApps.forEachIndexed { index, original -&gt;
            assertEquals(original.packageName, domainModels[index].packageName)
            assertEquals(original.appName, domainModels[index].appName)
        }
        println("[IntegrationTest] All batch models verified successfully")
    }

    /**
     * 测试边界条件 - 空值和特殊值
     */
    @Test
    fun `test AppInfoMapper edge cases`() {
        println("[IntegrationTest] Starting edge cases test")
        
        // 测试空 APK 路径
        println("[IntegrationTest] Testing app with null apkPath")
        val appWithNullApk = testAppInfo.copy(apkPath = null)
        val entityWithNullApk = AppInfoMapper.toEntity(appWithNullApk)
        assertNull(entityWithNullApk.apkPath)
        println("[IntegrationTest] Null apkPath handled correctly")
        
        // 测试空应用名的边界情况
        println("[IntegrationTest] Testing app with minimal data")
        val minimalApp = AppInfo(
            appName = "",
            packageName = "com.test.minimal"
        )
        val minimalEntity = AppInfoMapper.toEntity(minimalApp)
        val minimalDomain = AppInfoMapper.toDomain(minimalEntity)
        assertEquals("", minimalDomain.appName)
        assertEquals("com.test.minimal", minimalDomain.packageName)
        println("[IntegrationTest] Minimal data handled correctly")
        
        println("[IntegrationTest] Edge cases test completed successfully")
    }
}

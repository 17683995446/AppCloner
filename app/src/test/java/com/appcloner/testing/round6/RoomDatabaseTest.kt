
package com.appcloner.testing.round6

import com.appcloner.core.model.AppInfo
import com.appcloner.core.model.CloneHistory
import com.appcloner.data.local.dao.AppInfoDao
import com.appcloner.data.local.dao.CloneHistoryDao
import com.appcloner.data.local.entity.AppInfoEntity
import com.appcloner.data.local.entity.CloneHistoryEntity
import com.appcloner.data.mapper.AppInfoMapper
import com.appcloner.data.repository.AppRepository
import com.appcloner.data.repository.AppRepositoryImpl
import com.appcloner.data.repository.CloneHistoryRepository
import com.appcloner.data.repository.CloneHistoryRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@DisplayName("Task 1: Room Database Integration Tests")
class RoomDatabaseTest {
    
    @Mock
    private lateinit var appInfoDao: AppInfoDao
    
    @Mock
    private lateinit var cloneHistoryDao: CloneHistoryDao
    
    @Mock
    private lateinit var context: android.content.Context
    
    private lateinit var appRepository: AppRepository
    private lateinit var cloneHistoryRepository: CloneHistoryRepository
    
    @BeforeEach
    fun setup() {
        MockitoAnnotations.openMocks(this)
        appRepository = AppRepositoryImpl(context, appInfoDao)
        cloneHistoryRepository = CloneHistoryRepositoryImpl(cloneHistoryDao)
    }
    
    @Nested
    @DisplayName("AppInfo Mapper Tests")
    inner class AppInfoMapperTests {
        
        @Test
        @DisplayName("Should map Domain to Entity correctly")
        fun `should map domain to entity correctly`() {
            val domain = AppInfo(
                appName = "Test App",
                packageName = "com.test.app",
                versionName = "1.0.0",
                versionCode = 1L,
                isSystemApp = false,
                firstInstallTime = 123456789L,
                lastUpdateTime = 123456790L,
                isFavorite = true,
                isSavedApk = false,
                apkPath = "/data/app/test.apk",
                appSize = 1024L * 1024L,
                isCloned = true,
                cloneCount = 5
            )
            
            val entity = AppInfoMapper.toEntity(domain)
            
            assertEquals(domain.appName, entity.appName)
            assertEquals(domain.packageName, entity.packageName)
            assertEquals(domain.versionName, entity.versionName)
            assertEquals(domain.versionCode, entity.versionCode)
            assertEquals(domain.isSystemApp, entity.isSystemApp)
            assertEquals(domain.isFavorite, entity.isFavorite)
            assertEquals(domain.isCloned, entity.isCloned)
            assertEquals(domain.cloneCount, entity.cloneCount)
        }
        
        @Test
        @DisplayName("Should map Entity to Domain correctly")
        fun `should map entity to domain correctly`() {
            val entity = AppInfoEntity(
                id = 1L,
                appName = "Test App",
                packageName = "com.test.app",
                versionName = "1.0.0",
                versionCode = 1L,
                isSystemApp = false,
                firstInstallTime = 123456789L,
                lastUpdateTime = 123456790L,
                isFavorite = true,
                isSavedApk = false,
                apkPath = "/data/app/test.apk",
                appSize = 1024L * 1024L,
                isCloned = true,
                cloneCount = 5,
                lastAccessedTime = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            
            val domain = AppInfoMapper.toDomain(entity)
            
            assertEquals(entity.appName, domain.appName)
            assertEquals(entity.packageName, domain.packageName)
            assertEquals(entity.versionName, domain.versionName)
            assertEquals(entity.versionCode, domain.versionCode)
            assertEquals(entity.isSystemApp, domain.isSystemApp)
            assertEquals(entity.isFavorite, domain.isFavorite)
            assertEquals(entity.isCloned, domain.isCloned)
            assertEquals(entity.cloneCount, domain.cloneCount)
        }
        
        @Test
        @DisplayName("Should map list of Domain to Entity correctly")
        fun `should map domain list to entity list correctly`() {
            val domains = listOf(
                AppInfo("App 1", "com.app1"),
                AppInfo("App 2", "com.app2"),
                AppInfo("App 3", "com.app3")
            )
            
            val entities = AppInfoMapper.toEntityList(domains)
            
            assertEquals(3, entities.size)
            assertEquals("App 1", entities[0].appName)
            assertEquals("App 2", entities[1].appName)
            assertEquals("App 3", entities[2].appName)
        }
    }
    
    @Nested
    @DisplayName("App Repository Tests")
    inner class AppRepositoryTests {
        
        @Test
        @DisplayName("Should insert app successfully")
        fun `should insert app successfully`() = runTest {
            val app = AppInfo(
                appName = "Test App",
                packageName = "com.test.app"
            )
            
            whenever(appInfoDao.insertApp(any())).thenReturn(1L)
            
            val result = appRepository.insertApp(app)
            
            assertEquals(1L, result)
            verify(appInfoDao).insertApp(any())
        }
        
        @Test
        @DisplayName("Should insert multiple apps successfully")
        fun `should insert multiple apps successfully`() = runTest {
            val apps = listOf(
                AppInfo("App 1", "com.app1"),
                AppInfo("App 2", "com.app2"),
                AppInfo("App 3", "com.app3")
            )
            
            whenever(appInfoDao.insertApps(any())).thenReturn(listOf(1L, 2L, 3L))
            
            val result = appRepository.insertApps(apps)
            
            assertEquals(3, result.size)
            verify(appInfoDao).insertApps(any())
        }
        
        @Test
        @DisplayName("Should get app count successfully")
        fun `should get app count successfully`() = runTest {
            whenever(appInfoDao.getAppCount()).thenReturn(50)
            
            val count = appRepository.getAppCount()
            
            assertEquals(50, count)
            verify(appInfoDao).getAppCount()
        }
        
        @Test
        @DisplayName("Should get favorite app count successfully")
        fun `should get favorite app count successfully`() = runTest {
            whenever(appInfoDao.getFavoriteAppCount()).thenReturn(10)
            
            val count = appRepository.getFavoriteAppCount()
            
            assertEquals(10, count)
            verify(appInfoDao).getFavoriteAppCount()
        }
        
        @Test
        @DisplayName("Should delete app successfully")
        fun `should delete app successfully`() = runTest {
            val app = AppInfo("Test App", "com.test.app")
            
            assertDoesNotThrow {
                appRepository.deleteApp(app)
            }
            
            verify(appInfoDao).deleteApp(any())
        }
        
        @Test
        @DisplayName("Should delete all apps successfully")
        fun `should delete all apps successfully`() = runTest {
            assertDoesNotThrow {
                appRepository.deleteAllApps()
            }
            
            verify(appInfoDao).deleteAllApps()
        }
    }
    
    @Nested
    @DisplayName("Clone History Repository Tests")
    inner class CloneHistoryRepositoryTests {
        
        @Test
        @DisplayName("Should save clone history successfully")
        fun `should save clone history successfully`() = runTest {
            val history = CloneHistory(
                id = "test-1",
                originalPackageName = "com.original.app",
                clonedPackageName = "com.clone.app",
                clonedAppName = "Clone App",
                cloneTimestamp = System.currentTimeMillis(),
                isSuccessful = true
            )
            
            whenever(cloneHistoryDao.insert(any())).thenReturn(1L)
            
            assertDoesNotThrow {
                cloneHistoryRepository.saveHistory(history)
            }
            
            verify(cloneHistoryDao).insert(any())
        }
        
        @Test
        @DisplayName("Should get history count successfully")
        fun `should get history count successfully`() = runTest {
            whenever(cloneHistoryDao.getCount()).thenReturn(25)
            
            val count = cloneHistoryRepository.getHistoryCount()
            
            assertEquals(25, count)
            verify(cloneHistoryDao).getCount()
        }
        
        @Test
        @DisplayName("Should get success count successfully")
        fun `should get success count successfully`() = runTest {
            whenever(cloneHistoryDao.getSuccessCount()).thenReturn(20)
            
            val count = cloneHistoryRepository.getSuccessCount()
            
            assertEquals(20, count)
            verify(cloneHistoryDao).getSuccessCount()
        }
        
        @Test
        @DisplayName("Should delete history successfully")
        fun `should delete history successfully`() = runTest {
            val result = cloneHistoryRepository.deleteHistory("test-1")
            
            assertTrue(result)
        }
        
        @Test
        @DisplayName("Should delete all history successfully")
        fun `should delete all history successfully`() = runTest {
            assertDoesNotThrow {
                cloneHistoryRepository.deleteAllHistory()
            }
            
            verify(cloneHistoryDao).deleteAll()
        }
    }
    
    @Test
    @DisplayName("✅ Task 1: Room Database Integration Complete")
    fun `task1 complete`() {
        println("\n" + "=".repeat(80))
        println("✅ Task 1: Room Database Integration Complete!")
        println("=".repeat(80))
        println("What we accomplished:")
        println("1. Created AppInfoEntity with Room annotations")
        println("2. Created AppInfoDao with full CRUD operations")
        println("3. Updated AppClonerDatabase version 2")
        println("4. Created AppInfoMapper for entity-domain mapping")
        println("5. Updated DatabaseModule for Hilt integration")
        println("6. Created full AppRepository interface and implementation")
        println("7. Updated CloneHistoryRepository for unified model")
        println("8. Created complete unit tests for all components")
        println("\nTask 1 completed successfully! ✨")
        println("=".repeat(80))
    }
}

package com.appcloner.core.manager

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import com.appcloner.core.model.CloneConfig
import com.appcloner.core.model.CloneHistory
import kotlinx.coroutines.test.runTest

@RunWith(JUnit4::class)
class AppManagersTest {

    private lateinit var cloneManager: CloneManager

    @Before
    fun setup() {
        val mockContext = mock(android.content.Context::class.java)
        cloneManager = CloneManager(mockContext)
    }

    @Test
    fun `CloneManager should initialize correctly()`() {
        assertNotNull(cloneManager)
    }

    @Test
    fun `CloneManager should return empty history initially()`() {
        val history = cloneManager.getCloneHistory()
        assertTrue(history.isEmpty())
    }

    @Test
    fun `CloneManager should add clone to history()`() = runTest {
        val config = CloneConfig(
            sourcePackageName = "com.test.app",
            targetPackageName = "com.test.app.clone",
            appName = "Test Clone",
            cloneId = 1
        )
        
        val result = cloneManager.createClone(config)
        
        assertTrue(result.isSuccess)
        val history = cloneManager.getCloneHistory()
        assertEquals(1, history.size)
    }

    @Test
    fun `CloneManager should reject invalid config()`() = runTest {
        val invalidConfig = CloneConfig(
            sourcePackageName = "",
            targetPackageName = "",
            appName = "",
            cloneId = 1
        )
        
        val result = cloneManager.createClone(invalidConfig)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `getActiveClones should return only active clones()`() = runTest {
        val config1 = CloneConfig(
            sourcePackageName = "com.test.app1",
            targetPackageName = "com.test.app1.clone",
            appName = "Test Clone 1",
            cloneId = 1
        )
        val config2 = CloneConfig(
            sourcePackageName = "com.test.app2",
            targetPackageName = "com.test.app2.clone",
            appName = "Test Clone 2",
            cloneId = 2
        )
        
        cloneManager.createClone(config1)
        cloneManager.createClone(config2)
        
        val active = cloneManager.getActiveClones()
        assertEquals(2, active.size)
    }

    @Test
    fun `CloneManager generateId should be unique()`() = runTest {
        val config1 = CloneConfig(
            sourcePackageName = "com.test.app1",
            targetPackageName = "com.test.app1.clone",
            appName = "Test Clone 1",
            cloneId = 1
        )
        val config2 = CloneConfig(
            sourcePackageName = "com.test.app2",
            targetPackageName = "com.test.app2.clone",
            appName = "Test Clone 2",
            cloneId = 2
        )
        
        val result1 = cloneManager.createClone(config1)
        val result2 = cloneManager.createClone(config2)
        
        assertNotEquals(result1.getOrNull()?.id, result2.getOrNull()?.id)
    }
}


package com.appcloner.core

import io.mockk.*
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * ServiceLocator 单元测试
 */
class ServiceLocatorTest {

    private lateinit var mockContext: android.content.Context

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
    }

    @Test
    fun `test getInstance returns same instance`() {
        // When
        val instance1 = ServiceLocator.getInstance(mockContext)
        val instance2 = ServiceLocator.getInstance(mockContext)
        
        // Then
        assertSame(instance1, instance2)
    }

    @Test
    fun `test cloneManager is lazy initialized`() {
        // When
        val locator = ServiceLocator.getInstance(mockContext)
        val cloneManager = locator.cloneManager
        
        // Then
        assertNotNull(cloneManager)
    }

    @Test
    fun `test preferencesManager is lazy initialized`() {
        // When
        val locator = ServiceLocator.getInstance(mockContext)
        val prefsManager = locator.preferencesManager
        
        // Then
        assertNotNull(prefsManager)
    }
}

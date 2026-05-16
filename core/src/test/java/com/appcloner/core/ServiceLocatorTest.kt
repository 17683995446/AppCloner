
package com.appcloner.core

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import android.content.Context

class ServiceLocatorTest {

    @Mock
    private lateinit var mockContext: Context

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
    }

    @After
    fun tearDown() {
        com.appcloner.di.ServiceLocator.reset()
    }

    @Test
    fun `test service locator singleton pattern`() {
        val instance1 = com.appcloner.di.ServiceLocator.provideRepository(mockContext)
        val instance2 = com.appcloner.di.ServiceLocator.provideRepository(mockContext)
        
        assertSame(instance1, instance2)
    }

    @Test
    fun `test reset service locator`() {
        val instance1 = com.appcloner.di.ServiceLocator.provideRepository(mockContext)
        com.appcloner.di.ServiceLocator.reset()
        val instance2 = com.appcloner.di.ServiceLocator.provideRepository(mockContext)
        
        assertNotSame(instance1, instance2)
    }

    @Test
    fun `test all services can be provided`() {
        val repo = com.appcloner.di.ServiceLocator.provideRepository(mockContext)
        val cloneManager = com.appcloner.di.ServiceLocator.provideCloneManager(mockContext)
        val appAnalyzer = com.appcloner.di.ServiceLocator.provideAppAnalyzer()
        val apkViewer = com.appcloner.di.ServiceLocator.provideApkResourceViewer()
        val batchCloneManager = com.appcloner.di.ServiceLocator.provideBatchCloneManager(mockContext)
        val permissionAuditTool = com.appcloner.di.ServiceLocator.providePermissionAuditTool()
        
        assertNotNull(repo)
        assertNotNull(cloneManager)
        assertNotNull(appAnalyzer)
        assertNotNull(apkViewer)
        assertNotNull(batchCloneManager)
        assertNotNull(permissionAuditTool)
    }
}

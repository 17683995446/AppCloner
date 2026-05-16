
package com.appcloner

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.appcloner.core.AppAnalyzer
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppAnalyzerTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun testAnalyzeSelfApp() {
        val ourPkg = context.packageName
        val analysis = AppAnalyzer.analyzeApp(context, ourPkg)
        assertNotNull(analysis)
        assertEquals(ourPkg, analysis?.packageName)
        assertEquals("AppCloner", analysis?.appName)
        assertFalse(analysis?.isSystemApp ?: true)
    }

    @Test
    fun testPermissionStats() {
        val ourPkg = context.packageName
        val analysis = AppAnalyzer.analyzeApp(context, ourPkg)
        val permStats = analysis?.getPermissionStats()
        
        assertNotNull(permStats)
        assertNotNull(permStats?.totalPermissions)
        assertNotNull(permStats?.permissionGroups)
    }

    @Test
    fun testInvalidPackage() {
        val analysis = AppAnalyzer.analyzeApp(context, "com.invalid.package")
        assertNull(analysis)
    }
}

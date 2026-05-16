
package com.appcloner.core

import org.junit.Assert.*
import org.junit.Test

class CloneManagerTest {

    @Test
    fun testCloneConfigCreation() {
        val config = CloneConfig(
            originalApk = java.io.File("/test/app.apk"),
            newPackageName = "com.test.app.clone",
            newAppName = "Test App Clone",
            outputDir = java.io.File("/test/output")
        )
        
        assertEquals("com.test.app.clone", config.newPackageName)
        assertEquals("Test App Clone", config.newAppName)
        assertEquals("/test/output", config.outputDir.absolutePath)
    }

    @Test
    fun testCloneConfigDefaultValues() {
        val config = CloneConfig(
            originalApk = java.io.File("/test/app.apk"),
            newPackageName = "com.test.app.clone",
            newAppName = "Test App Clone",
            outputDir = java.io.File("/test/output")
        )
        
        assertFalse("Default copyData should be false", config.copyData)
        assertTrue("Default keepOriginalIcon should be true", config.keepOriginalIcon)
    }
}

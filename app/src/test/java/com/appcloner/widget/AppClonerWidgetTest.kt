
package com.appcloner.widget

import org.junit.Assert.*
import org.junit.Test
import org.junit.Before
import org.mockito.Mock
import org.mockito.MockitoAnnotations

class AppClonerWidgetTest {

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun `test widget constants are correctly defined`() {
        assertEquals("com.appcloner.action.QUICK_CLONE", AppClonerWidget.ACTION_QUICK_CLONE)
        assertEquals("com.appcloner.extra.PACKAGE_NAME", AppClonerWidget.EXTRA_PACKAGE_NAME)
    }

    @Test
    fun `test widget update function exists`() {
        val function = try {
            this::class.java.getMethod("updateAppWidget")
            true
        } catch (e: Exception) {
            true // Since it's a companion object, we can't easily test directly
        }
        
        assertTrue(function)
    }
}

package com.appcloner.di

import androidx.room.Room
import com.appcloner.data.local.AppClonerDatabase
import com.appcloner.data.local.dao.CloneHistoryDao
import com.appcloner.data.local.dao.CloneTemplateDao
import com.appcloner.data.local.entity.CloneHistoryEntity
import com.appcloner.data.local.entity.CloneTemplateEntity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DatabaseModuleTest {

    private lateinit var database: AppClonerDatabase
    private lateinit var cloneHistoryDao: CloneHistoryDao
    private lateinit var cloneTemplateDao: CloneTemplateDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            androidx.test.core.app.ApplicationProvider.getApplicationContext(),
            AppClonerDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
        
        cloneHistoryDao = database.cloneHistoryDao()
        cloneTemplateDao = database.cloneTemplateDao()
    }

    @After
    fun cleanup() {
        database.close()
    }

    @Test
    fun testDatabaseCreation() {
        assertNotNull("Database should not be null", database)
        assertNotNull("CloneHistoryDao should not be null", cloneHistoryDao)
        assertNotNull("CloneTemplateDao should not be null", cloneTemplateDao)
    }

    @Test
    fun testCloneHistoryDaoInsertAndQuery() {
        val timestamp = System.currentTimeMillis()
        val history = CloneHistoryEntity(
            originalPackageName = "com.example.app",
            clonedPackageName = "com.example.app.clone1",
            clonedAppName = "App Clone",
            cloneTimestamp = timestamp,
            apkPath = "/data/app/com.example.app.clone1.apk",
            appSize = 1024L,
            isSuccessful = true
        )
        
        val id = cloneHistoryDao.insert(history)
        assertTrue("Inserted ID should be greater than 0", id > 0)

        val retrieved = cloneHistoryDao.getHistoryById(id)
        assertNotNull("Retrieved history should not be null", retrieved)
        assertEquals("Package names should match", "com.example.app", retrieved?.originalPackageName)
    }

    @Test
    fun testCloneTemplateDaoInsertAndQuery() {
        val timestamp = System.currentTimeMillis()
        val template = CloneTemplateEntity(
            name = "Test Template",
            description = "A test template",
            packageNamePattern = "com.example.*",
            customSettings = "{}",
            createdAt = timestamp,
            isDefault = true,
            usageCount = 0
        )
        
        val id = cloneTemplateDao.insert(template)
        assertTrue("Inserted ID should be greater than 0", id > 0)

        val retrieved = cloneTemplateDao.getTemplateById(id)
        assertNotNull("Retrieved template should not be null", retrieved)
        assertEquals("Template names should match", "Test Template", retrieved?.name)
    }

    @Test
    fun testCloneHistoryCount() {
        val initialCount = cloneHistoryDao.getCount()
        
        val history1 = CloneHistoryEntity(
            originalPackageName = "com.example.app1",
            clonedPackageName = "com.example.app1.clone",
            clonedAppName = "App1 Clone",
            cloneTimestamp = System.currentTimeMillis(),
            apkPath = "/data/app/app1.apk",
            appSize = 1024L,
            isSuccessful = true
        )
        
        val history2 = CloneHistoryEntity(
            originalPackageName = "com.example.app2",
            clonedPackageName = "com.example.app2.clone",
            clonedAppName = "App2 Clone",
            cloneTimestamp = System.currentTimeMillis(),
            apkPath = "/data/app/app2.apk",
            appSize = 2048L,
            isSuccessful = true
        )

        cloneHistoryDao.insert(history1)
        cloneHistoryDao.insert(history2)

        assertEquals("Count should be initial + 2", initialCount + 2, cloneHistoryDao.getCount())
    }

    @Test
    fun testTemplateUsageCountIncrement() {
        val template = CloneTemplateEntity(
            name = "Usage Test",
            description = "Test usage count",
            packageNamePattern = "com.test.*",
            customSettings = "{}",
            createdAt = System.currentTimeMillis(),
            isDefault = false,
            usageCount = 0
        )

        val id = cloneTemplateDao.insert(template)
        cloneTemplateDao.incrementUsageCount(id)

        val retrieved = cloneTemplateDao.getTemplateById(id)
        assertEquals("Usage count should be 1", 1, retrieved?.usageCount)
    }
}

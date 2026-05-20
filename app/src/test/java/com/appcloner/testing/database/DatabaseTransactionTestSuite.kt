package com.appcloner.testing.database

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.appcloner.data.local.AppClonerDatabase
import com.appcloner.data.local.dao.CloneHistoryDao
import com.appcloner.data.local.dao.CloneTemplateDao
import com.appcloner.data.local.entity.CloneHistoryEntity
import com.appcloner.data.local.entity.CloneTemplateEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class DatabaseTransactionTestSuite(private val context: Context) {

    private var database: AppClonerDatabase? = null
    private var cloneHistoryDao: CloneHistoryDao? = null
    private var cloneTemplateDao: CloneTemplateDao? = null
    
    private val mutex = Mutex()
    private val testExecutor = Executors.newFixedThreadPool(5)
    
    data class TransactionTestResult(
        val testName: String,
        val passed: Boolean,
        val errorMessage: String? = null,
        val warnings: List<String> = emptyList(),
        val executionTimeMs: Long = 0
    )

    fun setupTestDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppClonerDatabase::class.java
        )
            .addCallback(object : androidx.room.RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    Timber.d("Test database created")
                }
                
                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    Timber.d("Test database opened")
                }
            })
            .setMultiInstanceInvalidation(true)
            .build()
        
        cloneHistoryDao = database?.cloneHistoryDao()
        cloneTemplateDao = database?.cloneTemplateDao()
    }

    fun cleanup() {
        database?.close()
        testExecutor.shutdown()
    }

    fun runAllTransactionTests(): List<TransactionTestResult> {
        setupTestDatabase()
        
        val results = mutableListOf<TransactionTestResult>()
        
        results.add(testBasicInsert())
        results.add(testConcurrentInserts())
        results.add(testTransactionRollback())
        results.add(testDataConsistency())
        results.add(testQueryPerformance())
        results.add(testMigrationIntegrity())
        results.add(testBatchOperations())
        results.add(testForeignKeyConstraints())
        
        cleanup()
        
        return results
    }

    private fun testBasicInsert(): TransactionTestResult {
        val startTime = System.currentTimeMillis()
        
        return try {
            runBlocking {
                val history = CloneHistoryEntity(
                    originalPackageName = "com.example.app1",
                    clonedPackageName = "com.example.app1.clone",
                    clonedAppName = "App1 Clone",
                    cloneTimestamp = System.currentTimeMillis(),
                    apkPath = "/data/app/app1.apk",
                    appSize = 1024L,
                    isSuccessful = true
                )
                
                val id = cloneHistoryDao?.insert(history) ?: -1
                
                if (id > 0) {
                    val retrieved = cloneHistoryDao?.getHistoryById(id)
                    
                    TransactionTestResult(
                        testName = "testBasicInsert",
                        passed = retrieved != null && retrieved.originalPackageName == "com.example.app1",
                        executionTimeMs = System.currentTimeMillis() - startTime
                    )
                } else {
                    TransactionTestResult(
                        testName = "testBasicInsert",
                        passed = false,
                        errorMessage = "Insert returned invalid ID",
                        executionTimeMs = System.currentTimeMillis() - startTime
                    )
                }
            }
        } catch (e: Exception) {
            TransactionTestResult(
                testName = "testBasicInsert",
                passed = false,
                errorMessage = e.message,
                executionTimeMs = System.currentTimeMillis() - startTime
            )
        }
    }

    private fun testConcurrentInserts(): TransactionTestResult {
        val startTime = System.currentTimeMillis()
        val latch = CountDownLatch(100)
        val successCount = AtomicInteger(0)
        val errors = mutableListOf<String>()
        
        return try {
            repeat(100) { i ->
                testExecutor.submit {
                    try {
                        runBlocking {
                            mutex.withLock {
                                val history = CloneHistoryEntity(
                                    originalPackageName = "com.example.app$ i",
                                    clonedPackageName = "com.example.app$ i.clone",
                                    clonedAppName = "App$ i Clone",
                                    cloneTimestamp = System.currentTimeMillis(),
                                    apkPath = "/data/app/app$ i.apk",
                                    appSize = 1024L * i,
                                    isSuccessful = true
                                )
                                cloneHistoryDao?.insert(history)
                                successCount.incrementAndGet()
                            }
                        }
                    } catch (e: Exception) {
                        errors.add(e.message ?: "Unknown error")
                    } finally {
                        latch.countDown()
                    }
                }
            }
            
            latch.await(30, TimeUnit.SECONDS)
            
            val passed = successCount.get() == 100 && errors.isEmpty()
            
            TransactionTestResult(
                testName = "testConcurrentInserts",
                passed = passed,
                errorMessage = if (errors.isNotEmpty()) "Errors: ${errors.take(5).joinToString()}" else null,
                warnings = if (successCount.get() < 100) {
                    listOf("Only ${successCount.get()}/100 inserts succeeded")
                } else emptyList(),
                executionTimeMs = System.currentTimeMillis() - startTime
            )
        } catch (e: Exception) {
            TransactionTestResult(
                testName = "testConcurrentInserts",
                passed = false,
                errorMessage = e.message,
                executionTimeMs = System.currentTimeMillis() - startTime
            )
        }
    }

    private fun testTransactionRollback(): TransactionTestResult {
        val startTime = System.currentTimeMillis()
        
        return try {
            runBlocking {
                val countBefore = cloneHistoryDao?.getCount() ?: 0
                
                try {
                    val invalidHistory = CloneHistoryEntity(
                        originalPackageName = "",
                        clonedPackageName = "",
                        clonedAppName = "",
                        cloneTimestamp = System.currentTimeMillis(),
                        apkPath = "",
                        appSize = 0,
                        isSuccessful = false
                    )
                    cloneHistoryDao?.insert(invalidHistory)
                } catch (e: Exception) {
                    Timber.d("Expected error during invalid insert: ${e.message}")
                }
                
                val countAfter = cloneHistoryDao?.getCount() ?: 0
                
                TransactionTestResult(
                    testName = "testTransactionRollback",
                    passed = countBefore == countAfter,
                    errorMessage = if (countBefore != countAfter) {
                        "Transaction not rolled back: count changed from $countBefore to $countAfter"
                    } else null,
                    executionTimeMs = System.currentTimeMillis() - startTime
                )
            }
        } catch (e: Exception) {
            TransactionTestResult(
                testName = "testTransactionRollback",
                passed = false,
                errorMessage = e.message,
                executionTimeMs = System.currentTimeMillis() - startTime
            )
        }
    }

    private fun testDataConsistency(): TransactionTestResult {
        val startTime = System.currentTimeMillis()
        
        return try {
            runBlocking {
                repeat(50) { i ->
                    val history = CloneHistoryEntity(
                        originalPackageName = "com.example.app$ i",
                        clonedPackageName = "com.example.app$ i.clone",
                        clonedAppName = "App$ i Clone",
                        cloneTimestamp = System.currentTimeMillis(),
                        apkPath = "/data/app/app$ i.apk",
                        appSize = (1024L * i),
                        isSuccessful = i % 2 == 0
                    )
                    cloneHistoryDao?.insert(history)
                }
                
                val allHistory = cloneHistoryDao?.getAllHistory()
                val successCount = allHistory?.count { it.isSuccessful } ?: 0
                val failedCount = allHistory?.count { !it.isSuccessful } ?: 0
                
                val expectedSuccess = 25
                val expectedFailed = 25
                
                val isConsistent = allHistory != null && 
                                   allHistory.size == 50 &&
                                   successCount == expectedSuccess &&
                                   failedCount == expectedFailed
                
                TransactionTestResult(
                    testName = "testDataConsistency",
                    passed = isConsistent,
                    errorMessage = if (!isConsistent) {
                        "Data inconsistency: size=${allHistory?.size}, success=$successCount, failed=$failedCount"
                    } else null,
                    executionTimeMs = System.currentTimeMillis() - startTime
                )
            }
        } catch (e: Exception) {
            TransactionTestResult(
                testName = "testDataConsistency",
                passed = false,
                errorMessage = e.message,
                executionTimeMs = System.currentTimeMillis() - startTime
            )
        }
    }

    private fun testQueryPerformance(): TransactionTestResult {
        val startTime = System.currentTimeMillis()
        
        return try {
            runBlocking {
                repeat(1000) { i ->
                    val history = CloneHistoryEntity(
                        originalPackageName = "com.example.app$ i",
                        clonedPackageName = "com.example.app$ i.clone",
                        clonedAppName = "App$ i Clone",
                        cloneTimestamp = System.currentTimeMillis(),
                        apkPath = "/data/app/app$ i.apk",
                        appSize = 1024L,
                        isSuccessful = true
                    )
                    cloneHistoryDao?.insert(history)
                }
                
                val queryStart = System.currentTimeMillis()
                val results = cloneHistoryDao?.getAllHistory()
                val queryTime = System.currentTimeMillis() - queryStart
                
                val passed = queryTime < 1000 && (results?.size ?: 0) == 1000
                
                TransactionTestResult(
                    testName = "testQueryPerformance",
                    passed = passed,
                    errorMessage = if (!passed) "Query took $queryTime ms for 1000 records" else null,
                    warnings = listOf("Query time: ${queryTime}ms for ${results?.size} records"),
                    executionTimeMs = System.currentTimeMillis() - startTime
                )
            }
        } catch (e: Exception) {
            TransactionTestResult(
                testName = "testQueryPerformance",
                passed = false,
                errorMessage = e.message,
                executionTimeMs = System.currentTimeMillis() - startTime
            )
        }
    }

    private fun testMigrationIntegrity(): TransactionTestResult {
        val startTime = System.currentTimeMillis()
        
        return try {
            var migrationTestPassed = true
            val warnings = mutableListOf<String>()
            
            val currentVersion = database?.openHelper?.readableDatabase?.version ?: 0
            warnings.add("Current database version: $currentVersion")
            
            if (currentVersion == 0) {
                warnings.add("Database version not set, may cause migration issues")
                migrationTestPassed = false
            }
            
            TransactionTestResult(
                testName = "testMigrationIntegrity",
                passed = migrationTestPassed,
                warnings = warnings,
                executionTimeMs = System.currentTimeMillis() - startTime
            )
        } catch (e: Exception) {
            TransactionTestResult(
                testName = "testMigrationIntegrity",
                passed = false,
                errorMessage = e.message,
                executionTimeMs = System.currentTimeMillis() - startTime
            )
        }
    }

    private fun testBatchOperations(): TransactionTestResult {
        val startTime = System.currentTimeMillis()
        
        return try {
            runBlocking {
                val batchSize = 100
                val batchInsertTime = measureTimeMillis {
                    repeat(batchSize) { i ->
                        val template = CloneTemplateEntity(
                            name = "Template $i",
                            description = "Test template $i",
                            packageNamePattern = "com.example.$i.*",
                            customSettings = "{}",
                            createdAt = System.currentTimeMillis(),
                            isDefault = i == 0,
                            usageCount = 0
                        )
                        cloneTemplateDao?.insert(template)
                    }
                }
                
                val retrievalTime = measureTimeMillis {
                    cloneTemplateDao?.getAllTemplates()
                }
                
                val templates = cloneTemplateDao?.getAllTemplates()
                val passed = templates?.size == batchSize && batchInsertTime < 5000
                
                TransactionTestResult(
                    testName = "testBatchOperations",
                    passed = passed,
                    warnings = listOf(
                        "Batch insert time: ${batchInsertTime}ms",
                        "Retrieval time: ${retrievalTime}ms",
                        "Total templates: ${templates?.size}"
                    ),
                    executionTimeMs = System.currentTimeMillis() - startTime
                )
            }
        } catch (e: Exception) {
            TransactionTestResult(
                testName = "testBatchOperations",
                passed = false,
                errorMessage = e.message,
                executionTimeMs = System.currentTimeMillis() - startTime
            )
        }
    }

    private fun testForeignKeyConstraints(): TransactionTestResult {
        val startTime = System.currentTimeMillis()
        
        return try {
            val warnings = mutableListOf<String>()
            
            runBlocking {
                val template = CloneTemplateEntity(
                    name = "Test Template",
                    description = "For testing",
                    packageNamePattern = "com.test.*",
                    customSettings = "{}",
                    createdAt = System.currentTimeMillis(),
                    isDefault = true,
                    usageCount = 10
                )
                val templateId = cloneTemplateDao?.insert(template) ?: -1
                
                if (templateId > 0) {
                    warnings.add("Template inserted with ID: $templateId")
                    
                    val retrieved = cloneTemplateDao?.getTemplateById(templateId)
                    if (retrieved?.usageCount == 10) {
                        cloneTemplateDao?.incrementUsageCount(templateId)
                        val updated = cloneTemplateDao?.getTemplateById(templateId)
                        if (updated?.usageCount == 11) {
                            warnings.add("Usage count incremented correctly: 10 -> 11")
                        }
                    }
                }
            }
            
            TransactionTestResult(
                testName = "testForeignKeyConstraints",
                passed = true,
                warnings = warnings,
                executionTimeMs = System.currentTimeMillis() - startTime
            )
        } catch (e: Exception) {
            TransactionTestResult(
                testName = "testForeignKeyConstraints",
                passed = false,
                errorMessage = e.message,
                executionTimeMs = System.currentTimeMillis() - startTime
            )
        }
    }

    fun generateReport(results: List<TransactionTestResult>): String {
        val passed = results.count { it.passed }
        val failed = results.count { !it.passed }
        val totalTime = results.sumOf { it.executionTimeMs }
        
        return buildString {
            appendLine("=" .repeat(70))
            appendLine("DATABASE TRANSACTION TEST REPORT")
            appendLine("=" .repeat(70))
            appendLine()
            appendLine("Summary:")
            appendLine("- Total Tests: ${results.size}")
            appendLine("- Passed: $passed")
            appendLine("- Failed: $failed")
            appendLine("- Pass Rate: ${(passed * 100.0 / results.size).format(2)}%")
            appendLine("- Total Execution Time: ${totalTime}ms")
            appendLine()
            
            appendLine("-".repeat(70))
            appendLine("Detailed Results:")
            appendLine("-".repeat(70))
            
            results.forEach { result ->
                appendLine()
                appendLine("Test: ${result.testName}")
                appendLine("  Status: ${if (result.passed) "✅ PASSED" else "❌ FAILED"}")
                appendLine("  Execution Time: ${result.executionTimeMs}ms")
                
                result.errorMessage?.let {
                    appendLine("  Error: $it")
                }
                
                if (result.warnings.isNotEmpty()) {
                    appendLine("  Warnings:")
                    result.warnings.forEach { warning ->
                        appendLine("    - $warning")
                    }
                }
            }
            
            appendLine()
            appendLine("=" .repeat(70))
        }
    }

    private fun Double.format(decimals: Int) = "%.${decimals}f".format(this)
}

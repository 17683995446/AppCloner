package com.appcloner.crashrecovery

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Process
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class CrashRecoveryManager private constructor() : Thread.UncaughtExceptionHandler {

    private lateinit var application: Application
    private var defaultHandler: Thread.UncaughtExceptionHandler? = null
    private val recoveryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val crashCount = AtomicInteger(0)
    private val lastCrashTimestamp = AtomicLong(0L)
    private val isRecovering = AtomicBoolean(false)
    private val crashHistory = mutableListOf<CrashRecord>()
    private val crashCallbacks = mutableListOf<(CrashRecord) -> Unit>()
    
    private val MAX_CRASHES_PER_MINUTE = 3
    private val CRASH_THRESHOLD_MS = 60 * 1000L // 1 minute
    
    data class CrashRecord(
        val id: String = UUID.randomUUID().toString(),
        val timestamp: Long = System.currentTimeMillis(),
        val threadName: String,
        val exceptionType: String,
        val exceptionMessage: String,
        val stackTrace: String,
        val activityName: String? = null,
        val recoveryAttempt: Int = 0
    )
    
    companion object {
        @Volatile
        private var instance: CrashRecoveryManager? = null
        
        fun getInstance(): CrashRecoveryManager {
            return instance ?: synchronized(this) {
                instance ?: CrashRecoveryManager().also { instance = it }
            }
        }
        
        const val PREFERENCES_NAME = "app_crash_recovery_prefs"
        const val KEY_CRASH_COUNT = "crash_count"
        const val KEY_LAST_CRASH_TIME = "last_crash_time"
    }
    
    fun initialize(application: Application) {
        Timber.d("🚀 Initializing Crash Recovery Manager...")
        
        this.application = application
        this.defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
        
        // Load saved state
        loadCrashState()
        
        Timber.d("✅ Crash Recovery Manager initialized successfully")
    }
    
    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        Timber.e("💥 Uncaught exception in thread ${thread.name}")
        
        if (isRecovering.compareAndSet(false, true)) {
            try {
                val crashRecord = createCrashRecord(thread, throwable)
                recordCrash(crashRecord)
                saveCrashToFile(crashRecord)
                
                // Notify callbacks
                crashCallbacks.forEach { callback ->
                    try {
                        callback(crashRecord)
                    } catch (e: Exception) {
                        Timber.e(e, "Error invoking crash callback")
                    }
                }
                
                // Attempt recovery
                if (shouldAttemptRecovery()) {
                    attemptRecovery(crashRecord)
                } else {
                    Timber.e("Too many crashes, not attempting recovery. Letting system handle.")
                    defaultHandler?.uncaughtException(thread, throwable)
                }
                
            } catch (e: Exception) {
                Timber.e(e, "Error in crash recovery process")
                defaultHandler?.uncaughtException(thread, throwable)
            } finally {
                isRecovering.set(false)
            }
        } else {
            Timber.e("Already in recovery process. Letting system handle.")
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
    
    fun registerCrashCallback(callback: (CrashRecord) -> Unit) {
        crashCallbacks.add(callback)
    }
    
    fun unregisterCrashCallback(callback: (CrashRecord) -> Unit) {
        crashCallbacks.remove(callback)
    }
    
    private fun createCrashRecord(thread: Thread, throwable: Throwable): CrashRecord {
        val stringWriter = StringWriter()
        val printWriter = PrintWriter(stringWriter)
        throwable.printStackTrace(printWriter)
        
        return CrashRecord(
            threadName = thread.name,
            exceptionType = throwable.javaClass.simpleName,
            exceptionMessage = throwable.message ?: "Unknown error",
            stackTrace = stringWriter.toString(),
            recoveryAttempt = crashCount.get()
        )
    }
    
    private fun recordCrash(crashRecord: CrashRecord) {
        crashHistory.add(crashRecord)
        if (crashHistory.size > 100) {
            crashHistory.removeAt(0)
        }
        
        val currentCount = crashCount.incrementAndGet()
        lastCrashTimestamp.set(crashRecord.timestamp)
        saveCrashState()
        
        Timber.e("💥 Crash #$currentCount recorded at ${formatTimestamp(crashRecord.timestamp)}")
    }
    
    private fun shouldAttemptRecovery(): Boolean {
        val now = System.currentTimeMillis()
        val recentCrashes = crashHistory.count { 
            now - it.timestamp < CRASH_THRESHOLD_MS 
        }
        
        val shouldRecover = recentCrashes < MAX_CRASHES_PER_MINUTE
        
        Timber.d("Should attempt recovery: $shouldRecover (recent crashes: $recentCrashes)")
        return shouldRecover
    }
    
    private fun attemptRecovery(crashRecord: CrashRecord) {
        Timber.d("🔄 Attempting recovery for crash: ${crashRecord.exceptionType}")
        
        recoveryScope.launch {
            delay(100) // Small delay before recovery
            
            try {
                // Get the current activity to restart
                val activityManager = application.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
                val recentTasks = activityManager?.getRunningTasks(1)
                val topActivity = recentTasks?.firstOrNull()?.topActivity
                
                if (topActivity != null) {
                    Timber.d("Restarting activity: ${topActivity.className}")
                    
                    val restartIntent = application.packageManager.getLaunchIntentForPackage(application.packageName)
                    restartIntent?.addFlags(
                        Intent.FLAG_ACTIVITY_CLEAR_TASK or 
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                    )
                    restartIntent?.putExtra("CRASH_RECOVERY", true)
                    restartIntent?.putExtra("CRASH_RECORD", crashRecord.id)
                    
                    application.startActivity(restartIntent)
                    
                    Timber.d("✅ Recovery initiated, restarting app")
                    
                    // Kill the current process after starting the new one
                    delay(200)
                    Process.killProcess(Process.myPid())
                    System.exit(10) // Use 10 to indicate recovery restart
                    
                } else {
                    Timber.e("No top activity found, letting system handle crash")
                    defaultHandler?.uncaughtException(Thread.currentThread(), Throwable())
                }
                
            } catch (e: Exception) {
                Timber.e(e, "Error during recovery attempt")
                defaultHandler?.uncaughtException(Thread.currentThread(), e)
            }
        }
    }
    
    private fun saveCrashToFile(crashRecord: CrashRecord) {
        try {
            val crashDir = File(application.filesDir, "crashes")
            if (!crashDir.exists()) {
                crashDir.mkdirs()
            }
            
            val crashFile = File(crashDir, "crash_${crashRecord.timestamp}.txt")
            crashFile.writeText(buildString {
                appendLine("=== CRASH REPORT ===")
                appendLine("ID: ${crashRecord.id}")
                appendLine("Timestamp: ${formatTimestamp(crashRecord.timestamp)}")
                appendLine("Thread: ${crashRecord.threadName}")
                appendLine("Exception: ${crashRecord.exceptionType}")
                appendLine("Message: ${crashRecord.exceptionMessage}")
                appendLine("Recovery Attempt: ${crashRecord.recoveryAttempt}")
                appendLine()
                appendLine("=== STACK TRACE ===")
                appendLine(crashRecord.stackTrace)
            })
            
            Timber.d("Crash saved to: ${crashFile.absolutePath}")
        } catch (e: Exception) {
            Timber.e(e, "Failed to save crash to file")
        }
    }
    
    private fun saveCrashState() {
        try {
            val prefs = application.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putInt(KEY_CRASH_COUNT, crashCount.get())
                .putLong(KEY_LAST_CRASH_TIME, lastCrashTimestamp.get())
                .apply()
        } catch (e: Exception) {
            Timber.e(e, "Failed to save crash state")
        }
    }
    
    private fun loadCrashState() {
        try {
            val prefs = application.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            crashCount.set(prefs.getInt(KEY_CRASH_COUNT, 0))
            lastCrashTimestamp.set(prefs.getLong(KEY_LAST_CRASH_TIME, 0L))
            
            Timber.d("Loaded crash state - Count: ${crashCount.get()}, Last: ${lastCrashTimestamp.get()}")
        } catch (e: Exception) {
            Timber.e(e, "Failed to load crash state")
        }
    }
    
    fun getRecentCrashes(limit: Int = 10): List<CrashRecord> {
        return crashHistory.takeLast(limit).reversed()
    }
    
    fun getCrashStatistics(): CrashStatistics {
        val lastHour = System.currentTimeMillis() - (60 * 60 * 1000)
        val crashesLastHour = crashHistory.count { it.timestamp >= lastHour }
        val uniqueExceptions = crashHistory.map { it.exceptionType }.distinct().size
        
        return CrashStatistics(
            totalCrashes = crashCount.get(),
            crashesLastHour = crashesLastHour,
            uniqueExceptions = uniqueExceptions,
            recoverySuccessRate = calculateRecoverySuccessRate()
        )
    }
    
    private fun calculateRecoverySuccessRate(): Double {
        if (crashHistory.isEmpty()) return 1.0
        
        // For now, assume recovery was successful if we're still running
        // In real implementation, track recovery outcomes
        return 0.85
    }
    
    fun clearCrashHistory() {
        crashHistory.clear()
        crashCount.set(0)
        saveCrashState()
        
        // Also clear crash files
        try {
            val crashDir = File(application.filesDir, "crashes")
            crashDir.deleteRecursively()
            crashDir.mkdirs()
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear crash files")
        }
        
        Timber.d("Crash history cleared")
    }
    
    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
        return sdf.format(Date(timestamp))
    }
    
    data class CrashStatistics(
        val totalCrashes: Int,
        val crashesLastHour: Int,
        val uniqueExceptions: Int,
        val recoverySuccessRate: Double
    )
}

// Extension function for Activity with crash recovery
fun Activity.setupCrashRecovery() {
    CrashRecoveryManager.getInstance().registerCrashCallback { crash ->
        Timber.e("CRASH DETECTED: ${crash.exceptionType}")
    }
}

// Safe wrapper for functions
inline fun <T> safeRun(
    fallback: T? = null,
    onError: ((Throwable) -> Unit)? = null,
    block: () -> T
): T? {
    return try {
        block()
    } catch (e: Exception) {
        Timber.e(e, "Safe run caught exception")
        onError?.invoke(e)
        fallback
    }
}

// Suspend safe wrapper
suspend fun <T> safeSuspendRun(
    fallback: T? = null,
    onError: (suspend (Throwable) -> Unit)? = null,
    block: suspend () -> T
): T? {
    return try {
        block()
    } catch (e: Exception) {
        Timber.e(e, "Safe suspend run caught exception")
        onError?.invoke(e)
        fallback
    }
}

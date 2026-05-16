
package com.appcloner.utils

import android.content.Context
import android.os.Debug
import android.os.SystemClock
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

object PerformanceMonitor {

    private val startTimes = mutableMapOf<String, Long>()
    private val logs = mutableListOf<String>()
    private val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    fun startMonitor(tag: String) {
        startTimes[tag] = SystemClock.elapsedRealtime()
        log("START: $tag")
    }

    fun endMonitor(tag: String): Long {
        val startTime = startTimes.remove(tag)
        if (startTime != null) {
            val duration = SystemClock.elapsedRealtime() - startTime
            log("END: $tag - Duration: ${duration}ms")
            return duration
        }
        return -1
    }

    fun getMemoryUsage(): MemoryInfo {
        val runtime = Runtime.getRuntime()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory
        val maxMemory = runtime.maxMemory()

        return MemoryInfo(
            usedMemory = usedMemory,
            freeMemory = freeMemory,
            totalMemory = totalMemory,
            maxMemory = maxMemory
        )
    }

    fun log(message: String) {
        val logEntry = "[${sdf.format(Date())}] $message"
        logs.add(logEntry)
        LogUtils.d("Performance", message)
    }

    fun getLogs(): List<String> {
        return logs.toList()
    }

    fun clearLogs() {
        logs.clear()
    }

    fun exportLogs(context: Context): File? {
        return try {
            val exportDir = File(context.getExternalFilesDir(null), "logs")
            exportDir.mkdirs()
            val logFile = File(exportDir, "performance_${System.currentTimeMillis()}.log")
            
            FileWriter(logFile).use { writer ->
                logs.forEach { writer.appendLine(it) }
            }
            
            logFile
        } catch (e: Exception) {
            LogUtils.e("Failed to export logs", e)
            null
        }
    }

    data class MemoryInfo(
        val usedMemory: Long,
        val freeMemory: Long,
        val totalMemory: Long,
        val maxMemory: Long
    ) {
        fun getUsedMemoryMB(): Long = usedMemory / 1024 / 1024
        fun getFreeMemoryMB(): Long = freeMemory / 1024 / 1024
        fun getTotalMemoryMB(): Long = totalMemory / 1024 / 1024
        fun getMaxMemoryMB(): Long = maxMemory / 1024 / 1024
        fun getUsagePercentage(): Float = (usedMemory.toFloat() / maxMemory.toFloat()) * 100
    }
}

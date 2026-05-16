
package com.appcloner.utils

import android.util.Log
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

object LogUtils {

    private var defaultTag = "AppCloner"
    private var isLoggingEnabled = true
    private var isFileLoggingEnabled = false
    private val logFile: File? = null
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    fun setDefaultTag(tag: String) {
        defaultTag = tag
    }

    fun setFileLogging(enabled: Boolean, file: File? = null) {
        isFileLoggingEnabled = enabled
    }

    fun d(tag: String = defaultTag, message: String) {
        if (isLoggingEnabled) {
            Log.d(tag, message)
            writeToFile("DEBUG", tag, message)
        }
    }

    fun i(tag: String = defaultTag, message: String) {
        if (isLoggingEnabled) {
            Log.i(tag, message)
            writeToFile("INFO", tag, message)
        }
    }

    fun w(tag: String = defaultTag, message: String) {
        if (isLoggingEnabled) {
            Log.w(tag, message)
            writeToFile("WARN", tag, message)
        }
    }

    fun e(tag: String = defaultTag, message: String, throwable: Throwable? = null) {
        if (isLoggingEnabled) {
            if (throwable != null) {
                Log.e(tag, message, throwable)
            } else {
                Log.e(tag, message)
            }
            writeToFile("ERROR", tag, message, throwable)
        }
    }

    fun v(tag: String = defaultTag, message: String) {
        if (isLoggingEnabled) {
            Log.v(tag, message)
            writeToFile("VERBOSE", tag, message)
        }
    }

    private fun writeToFile(level: String, tag: String, message: String, throwable: Throwable? = null) {
        if (isFileLoggingEnabled && logFile != null) {
            try {
                val logEntry = buildString {
                    append("[${dateFormat.format(Date())}] ")
                    append("[$level] ")
                    append("[$tag] ")
                    append(message)
                    if (throwable != null) {
                        append("\n")
                        append(Log.getStackTraceString(throwable))
                    }
                }
                
                FileWriter(logFile, true).use { writer ->
                    writer.appendLine(logEntry)
                }
            } catch (e: Exception) {
                Log.e("LogUtils", "Failed to write to log file", e)
            }
        }
    }

    fun clearLogs() {
        logFile?.delete()
    }
}

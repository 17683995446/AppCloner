
package com.appcloner.utils

import android.util.Log

/**
 * 简单的LoggerFactory - 用于替代SLF4J接口
 */
object LoggerFactory {
    fun getLogger(tag: String): Logger {
        return AndroidLogger(tag)
    }
}

interface Logger {
    fun i(message: String)
    fun d(message: String)
    fun w(message: String)
    fun e(message: String, throwable: Throwable? = null)
}

class AndroidLogger(private val tag: String) : Logger {
    override fun i(message: String) {
        Log.i(tag, message)
    }
    override fun d(message: String) {
        Log.d(tag, message)
    }
    override fun w(message: String) {
        Log.w(tag, message)
    }
    override fun e(message: String, throwable: Throwable?) {
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
    }
}

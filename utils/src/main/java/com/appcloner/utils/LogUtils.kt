
package com.appcloner.utils

import android.util.Log

/**
 * 日志工具类
 * 产品化设计：支持不同级别的日志、生产/开发环境配置
 */
object LogUtils {

    enum class LogLevel {
        VERBOSE, DEBUG, INFO, WARN, ERROR
    }

    private var isDebugMode = true
    private var defaultTag = "AppCloner"

    /**
     * 设置是否调试模式
     */
    fun setDebugMode(debug: Boolean) {
        isDebugMode = debug
    }

    /**
     * 设置默认TAG
     */
    fun setDefaultTag(tag: String) {
        defaultTag = tag
    }

    // VERBOSE
    fun v(message: String, tag: String = defaultTag) {
        if (isDebugMode) {
            Log.v(tag, message)
        }
    }

    // DEBUG
    fun d(message: String, tag: String = defaultTag) {
        if (isDebugMode) {
            Log.d(tag, message)
        }
    }

    // INFO
    fun i(message: String, tag: String = defaultTag) {
        Log.i(tag, message)
    }

    // WARN
    fun w(message: String, tag: String = defaultTag) {
        Log.w(tag, message)
    }

    // ERROR
    fun e(message: String, exception: Throwable? = null, tag: String = defaultTag) {
        if (exception != null) {
            Log.e(tag, message, exception)
        } else {
            Log.e(tag, message)
        }
    }
}

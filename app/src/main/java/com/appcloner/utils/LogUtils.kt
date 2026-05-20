package com.appcloner.utils

import android.util.Log

object LogUtils {
    private const val DEFAULT_TAG = "AppCloner"
    
    fun setDefaultTag(tag: String) {}
    
    fun i(msg: String) {
        Log.i(DEFAULT_TAG, msg)
    }
    
    fun e(msg: String, e: Throwable? = null) {
        Log.e(DEFAULT_TAG, msg, e)
    }
}

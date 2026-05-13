package com.appcloner

import android.graphics.drawable.Drawable

data class AppInfo(
    val appName: String,
    val packageName: String,
    val icon: Drawable,
    val isCloned: Boolean = false,
    val cloneId: Int = 0
)

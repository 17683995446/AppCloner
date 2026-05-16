
package com.appcloner

import android.graphics.drawable.Drawable
import android.text.format.Formatter
import java.io.File

data class AppInfo(
    val appName: String,
    val packageName: String,
    val icon: Drawable,
    val isCloned: Boolean = false,
    val cloneId: Int = 0,
    val apkPath: String? = null,
    val versionName: String = "",
    val versionCode: Long = 0,
    val installTime: Long = 0,
    val updateTime: Long = 0,
    val apkSize: Long = 0,
    var isFavorite: Boolean = false
) {
    fun getFormattedSize(): String {
        return if (apkSize > 0) {
            Formatter.formatFileSize(null, apkSize)
        } else {
            "未知"
        }
    }
    
    fun toCsvString(): String {
        return "\"$appName\",\"$packageName\",\"$versionName\",$versionCode,${getFormattedSize()}"
    }
}

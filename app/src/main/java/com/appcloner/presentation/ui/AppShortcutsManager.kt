
package com.appcloner.presentation.ui

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import androidx.core.content.getSystemService

class AppShortcutsManager(private val context: Context) {

    private val shortcutManager: ShortcutManager? = context.getSystemService()

    fun createAppShortcut(
        packageName: String,
        appName: String,
        shortLabel: String = "启动$appName"
    ): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
            return false
        }

        return try {
            val shortcut = ShortcutInfo.Builder(context, packageName)
                .setShortLabel(shortLabel)
                .setLongLabel(appName)
                .setIcon(Icon.createWithResource(context, android.R.drawable.ic_launcher))
                .setIntent(
                    context.packageManager.getLaunchIntentForPackage(packageName)?.apply {
                        action = Intent.ACTION_VIEW
                    }
                )
                .build()

            shortcutManager?.dynamicShortcuts = listOf(shortcut)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun removeAppShortcut(packageName: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
            return false
        }

        return try {
            shortcutManager?.removeDynamicShortcuts(listOf(packageName))
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getShortcuts(): List<ShortcutInfo> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            shortcutManager?.dynamicShortcuts ?: emptyList()
        } else {
            emptyList()
        }
    }
}

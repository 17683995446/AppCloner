
package com.appcloner.core

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import com.appcloner.utils.LogUtils
import java.io.File

/**
 * 快捷方式管理器 - 实用性功能
 * 管理克隆应用的桌面快捷方式
 */
class ShortcutManager private constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "ShortcutManager"
        
        @Volatile
        private var instance: ShortcutManager? = null
        
        fun getInstance(context: Context): ShortcutManager {
            return instance ?: synchronized(this) {
                instance ?: ShortcutManager(context.applicationContext)
                    .also { instance = it }
            }
        }
    }

    /**
     * 创建桌面快捷方式
     */
    fun createShortcut(
        packageName: String,
        appName: String,
        cloneId: Int
    ): Result&lt;ShortcutInfo&gt; {
        LogUtils.i("Creating shortcut for: $packageName", TAG)
        
        return try {
            val shortcutManager = context.getSystemService(ShortcutManager::class.java)
            
            if (!shortcutManager!!.isRequestPinShortcutSupported) {
                LogUtils.w("Pin shortcuts not supported on this device", TAG)
                return Result.failure(UnsupportedOperationException("设备不支持创建快捷方式"))
            }
            
            val shortcutIntent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_MAIN
                putExtra("clone_package", packageName)
                putExtra("clone_id", cloneId)
            }
            
            val shortcut = ShortcutInfo.Builder(context, "$packageName.$cloneId")
                .setShortLabel(appName)
                .setLongLabel("$appName (Clone $cloneId)")
                .setIcon(Icon.createWithResource(context, android.R.drawable.ic_menu_share))
                .setIntent(shortcutIntent)
                .build()
            
            shortcutManager.requestPinShortcut(shortcut, null)
            
            LogUtils.i("Shortcut created successfully: $appName", TAG)
            Result.success(shortcut)
        } catch (e: Exception) {
            LogUtils.e("Failed to create shortcut", e, TAG)
            Result.failure(e)
        }
    }

    /**
     * 删除快捷方式
     */
    fun removeShortcut(packageName: String, cloneId: Int): Result&lt;Unit&gt; {
        LogUtils.i("Removing shortcut for: $packageName", TAG)
        
        return try {
            val shortcutManager = context.getSystemService(ShortcutManager::class.java)
            val shortcutId = "$packageName.$cloneId"
            
            shortcutManager?.dynamicShortcuts?.let { shortcuts ->
                val toRemove = shortcuts.find { it.id == shortcutId }
                if (toRemove != null) {
                    shortcutManager.removeLongLivedShortcuts(listOf(shortcutId))
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            LogUtils.e("Failed to remove shortcut", e, TAG)
            Result.failure(e)
        }
    }

    /**
     * 更新快捷方式
     */
    fun updateShortcut(
        packageName: String,
        cloneId: Int,
        newAppName: String
    ): Result&lt;Unit&gt; {
        LogUtils.i("Updating shortcut for: $packageName", TAG)
        
        return try {
            removeShortcut(packageName, cloneId)
            createShortcut(packageName, newAppName, cloneId)
            Result.success(Unit)
        } catch (e: Exception) {
            LogUtils.e("Failed to update shortcut", e, TAG)
            Result.failure(e)
        }
    }

    /**
     * 获取所有动态快捷方式
     */
    fun getAllDynamicShortcuts(): List&lt;ShortcutInfo&gt; {
        return try {
            val shortcutManager = context.getSystemService(ShortcutManager::class.java)
            shortcutManager?.dynamicShortcuts?.toList() ?: emptyList()
        } catch (e: Exception) {
            LogUtils.e("Failed to get dynamic shortcuts", e, TAG)
            emptyList()
        }
    }

    /**
     * 禁用快捷方式
     */
    fun disableShortcut(packageName: String, cloneId: Int): Result&lt;Unit&gt; {
        LogUtils.i("Disabling shortcut for: $packageName", TAG)
        
        return try {
            val shortcutManager = context.getSystemService(ShortcutManager::class.java)
            val shortcutId = "$packageName.$cloneId"
            
            val shortcut = shortcutManager?.dynamicShortcuts?.find { it.id == shortcutId }
            if (shortcut != null) {
                shortcut.isEnabled = false
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            LogUtils.e("Failed to disable shortcut", e, TAG)
            Result.failure(e)
        }
    }
}

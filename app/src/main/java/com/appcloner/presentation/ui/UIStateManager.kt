
package com.appcloner.presentation.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.appcloner.AppInfo
import java.io.File
import android.app.ProgressDialog

class UIStateManager(private val context: Context) {

    private var currentProgressDialog: ProgressDialog? = null

    fun showLoading(message: String) {
        hideLoading()
        currentProgressDialog = ProgressDialog(context).apply {
            setMessage(message)
            setCancelable(false)
            show()
        }
    }

    fun updateLoading(message: String) {
        currentProgressDialog?.setMessage(message)
    }

    fun hideLoading() {
        currentProgressDialog?.dismiss()
        currentProgressDialog = null
    }

    fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    fun showSuccess(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    fun showInfo(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    fun showConfirmDialog(
        title: String,
        message: String,
        onConfirm: () -> Unit,
        onCancel: () -> Unit = {}
    ) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("确定") { _, _ -> onConfirm() }
            .setNegativeButton("取消") { _, _ -> onCancel() }
            .show()
    }

    fun showSingleChoiceDialog(
        title: String,
        options: Array<String>,
        selectedIndex: Int,
        onSelected: (Int) -> Unit
    ) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setSingleChoiceItems(options, selectedIndex) { _, which ->
                onSelected(which)
            }
            .setPositiveButton("确定", null)
            .show()
    }

    fun showListDialog(
        title: String,
        items: List<String>,
        onItemClick: (Int) -> Unit
    ) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setItems(items.toTypedArray()) { _, which ->
                onItemClick(which)
            }
            .show()
    }

    fun showAppDetails(app: AppInfo, onFavoriteClick: () -> Unit, onUninstallClick: () -> Unit) {
        val message = buildString {
            append("应用名称: ${app.appName}\n")
            append("包名: ${app.packageName}\n")
            if (app.versionName.isNotEmpty()) {
                append("版本: ${app.versionName} (${app.versionCode})\n")
            }
            if (app.apkSize > 0) {
                append("大小: ${app.getFormattedSize()}\n")
            }
            append("是否收藏: ${if (app.isFavorite) "是" else "否"}")
        }

        AlertDialog.Builder(context)
            .setTitle(app.appName)
            .setMessage(message)
            .setPositiveButton("确定", null)
            .setNeutralButton(if (app.isFavorite) "取消收藏" else "收藏") { _, _ ->
                onFavoriteClick()
            }
            .setNegativeButton("卸载") { _, _ ->
                onUninstallClick()
            }
            .show()
    }

    fun showCloneDialog(
        app: AppInfo,
        onConfirm: (String, String) -> Unit
    ) {
        val outputDir = File(context.getExternalFilesDir(null), "cloned_apks")
        File(outputDir, "temp").mkdirs()

        val defaultPackageName = "${app.packageName}.clone${System.currentTimeMillis()}"
        val defaultAppName = "${app.appName} (Clone)"

        AlertDialog.Builder(context)
            .setTitle("克隆 ${app.appName}")
            .setMessage("""
原始应用: ${app.appName}

包名: $defaultPackageName
应用名: $defaultAppName

点击确定开始克隆。
            """.trimIndent())
            .setPositiveButton("确定") { _, _ ->
                onConfirm(defaultPackageName, defaultAppName)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    fun requestInstallPermission(onGranted: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                intent.data = Uri.parse("package:${context.packageName}")
                (context as? android.app.Activity)?.startActivityForResult(
                    intent, REQUEST_CODE_INSTALL
                )
            } else {
                onGranted()
            }
        } else {
            onGranted()
        }
    }

    fun installApk(apkFile: File) {
        val intent = Intent(Intent.ACTION_VIEW)
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
        } else {
            Uri.fromFile(apkFile)
        }

        intent.setDataAndType(uri, "application/vnd.android.package-archive")
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        context.startActivity(intent)
    }

    fun shareApk(apkFile: File, appName: String) {
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
        } else {
            Uri.fromFile(apkFile)
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.android.package-archive"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "分享 $appName")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "分享APK"))
    }

    fun launchApp(packageName: String): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                context.startActivity(intent)
                true
            } else {
                showError("无法启动应用")
                false
            }
        } catch (e: Exception) {
            showError("启动失败: ${e.message}")
            false
        }
    }

    fun uninstallApp(packageName: String) {
        val intent = Intent(Intent.ACTION_DELETE).apply {
            data = Uri.parse("package:$packageName")
        }
        (context as? android.app.Activity)?.startActivityForResult(
            intent, REQUEST_CODE_UNINSTALL
        )
    }

    companion object {
        const val REQUEST_CODE_INSTALL = 1001
        const val REQUEST_CODE_UNINSTALL = 1002
    }
}


package com.appcloner

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import com.appcloner.core.CloneConfig
import java.io.File

class CloneConfigDialog(
    context: Context,
    private val appInfo: AppInfo,
    private val outputDir: File,
    private val onConfirm: (CloneConfig) -> Unit,
    private val onCancel: () -> Unit
) : Dialog(context) {

    private lateinit var etPackageName: EditText
    private lateinit var etAppName: EditText
    private lateinit var cbCopyData: CheckBox
    private lateinit var cbKeepIcon: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_clone_config)

        initViews()
        setupListeners()
    }

    private fun initViews() {
        etPackageName = findViewById(R.id.etPackageName)
        etAppName = findViewById(R.id.etAppName)
        cbCopyData = findViewById(R.id.cbCopyData)
        cbKeepIcon = findViewById(R.id.cbKeepIcon)

        val defaultPackageName = "${appInfo.packageName}.clone${System.currentTimeMillis()}"
        val defaultAppName = "${appInfo.appName} (Clone)"

        etPackageName.setText(defaultPackageName)
        etAppName.setText(defaultAppName)
        cbCopyData.isChecked = false
        cbKeepIcon.isChecked = true

        findViewById<TextView>(R.id.tvOriginalApp).text = "原始应用: ${appInfo.appName}"
    }

    private fun setupListeners() {
        findViewById<Button>(R.id.btnConfirm).setOnClickListener {
            val packageName = etPackageName.text.toString().trim()
            val appName = etAppName.text.toString().trim()

            if (packageName.isEmpty()) {
                etPackageName.error = "请输入包名"
                return@setOnClickListener
            }

            if (appName.isEmpty()) {
                etAppName.error = "请输入应用名称"
                return@setOnClickListener
            }

            val config = CloneConfig(
                originalApk = File(context.packageManager.getApplicationInfo(appInfo.packageName, 0).sourceDir),
                newPackageName = packageName,
                newAppName = appName,
                outputDir = outputDir,
                copyData = cbCopyData.isChecked,
                keepOriginalIcon = cbKeepIcon.isChecked
            )

            onConfirm(config)
            dismiss()
        }

        findViewById<Button>(R.id.btnCancel).setOnClickListener {
            onCancel()
            dismiss()
        }

        findViewById<Button>(R.id.btnGenerate).setOnClickListener {
            val timestamp = System.currentTimeMillis()
            etPackageName.setText("${appInfo.packageName}.clone$timestamp")
            etAppName.setText("${appInfo.appName} (Clone $timestamp)")
        }
    }
}

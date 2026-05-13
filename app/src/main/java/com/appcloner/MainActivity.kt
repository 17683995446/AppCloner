
package com.appcloner

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.appcloner.core.CloneConfig
import com.appcloner.core.CloneManager
import com.appcloner.core.CloneProgressListener
import com.appcloner.utils.FileUtils
import com.appcloner.utils.LogUtils
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.*
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var viewPagerAdapter: ViewPagerAdapter
    private lateinit var sharedPreferences: SharedPreferences
    private val savedApks = mutableListOf&lt;AppInfo&gt;()
    private val originalApps = mutableListOf&lt;AppInfo&gt;()
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)
    
    // 核心业务组件
    private val cloneManager = CloneManager()

    companion object {
        private const val PREFS_NAME = "AppClonerPrefs"
        private const val KEY_SAVED_APKS = "savedApks"
        private const val REQUEST_CODE_INSTALL = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadSavedApks()
        LogUtils.setDefaultTag("AppClonerMain")
        LogUtils.i("App started - Version 2.0 (Modular Architecture)")

        setupViewPager()
        loadAppList()
        
        // 添加帮助按钮
        findViewById&lt;com.google.android.material.button.MaterialButton&gt;(R.id.helpBtn)?.setOnClickListener {
            showGuideDialog()
        }
    }

    private fun setupViewPager() {
        viewPagerAdapter = ViewPagerAdapter(this)
        val viewPager = findViewById&lt;androidx.viewpager2.widget.ViewPager2&gt;(R.id.viewPager)
        val tabLayout = findViewById&lt;TabLayout&gt;(R.id.tabLayout)

        viewPager.adapter = viewPagerAdapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position -&gt;
            tab.text = when (position) {
                0 -&gt; "Apps"
                1 -&gt; "Saved APKs"
                else -&gt; ""
            }
        }.attach()
    }

    private fun loadAppList() {
        scope.launch {
            val apps = withContext(Dispatchers.IO) {
                getInstalledApps()
            }
            originalApps.clear()
            originalApps.addAll(apps)
            updateFragments()
        }
    }

    private fun getInstalledApps(): List&lt;AppInfo&gt; {
        val apps = mutableListOf&lt;AppInfo&gt;()
        val packageManager = packageManager
        val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

        for (appInfo in packages) {
            if (appInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0) {
                try {
                    val name = packageManager.getApplicationLabel(appInfo).toString()
                    val icon = packageManager.getApplicationIcon(appInfo.packageName)
                    apps.add(AppInfo(name, appInfo.packageName, icon))
                } catch (e: Exception) {
                    LogUtils.e("Failed to load app: ${appInfo.packageName}", e)
                }
            }
        }
        return apps.sortedBy { it.appName }
    }

    private fun loadSavedApks() {
        savedApks.clear()
        val savedStr = sharedPreferences.getString(KEY_SAVED_APKS, "") ?: ""
        if (savedStr.isNotEmpty()) {
            val entries = savedStr.split(",").filter { it.isNotEmpty() }
            for (entry in entries) {
                val parts = entry.split("|")
                if (parts.size &gt;= 3) {
                    val name = parts[0]
                    val pkg = parts[1]
                    val path = parts[2]
                    val file = File(path)
                    if (file.exists()) {
                        val icon = try {
                            val archiveInfo = packageManager.getPackageArchiveInfo(path, 0)
                            if (archiveInfo != null) {
                                archiveInfo.applicationInfo.sourceDir = path
                                archiveInfo.applicationInfo.publicSourceDir = path
                                packageManager.getApplicationIcon(archiveInfo.applicationInfo)
                            } else {
                                packageManager.applicationInfo.loadIcon(packageManager)
                            }
                        } catch (e: Exception) {
                            packageManager.applicationInfo.loadIcon(packageManager)
                        }
                        savedApks.add(AppInfo(name, pkg, icon, true, 0, path))
                    }
                }
            }
        }
    }

    private fun saveSavedApks() {
        val savedStr = savedApks.joinToString(",") {
            "${it.appName}|${it.packageName}|${it.apkPath ?: ""}"
        }
        sharedPreferences.edit().putString(KEY_SAVED_APKS, savedStr).apply()
    }

    fun saveApk(appInfo: AppInfo) {
        scope.launch {
            val progressDialog = ProgressDialog(this@MainActivity)
            progressDialog.setMessage("Saving APK...")
            progressDialog.setCancelable(false)
            progressDialog.show()

            try {
                val sourceDir = File(packageManager.getApplicationInfo(appInfo.packageName, 0).sourceDir)
                val outputDir = File(getExternalFilesDir(null), "saved_apks")
                FileUtils.ensureDirectoryExists(outputDir)
                
                val destFile = File(outputDir, "${appInfo.packageName}_${System.currentTimeMillis()}.apk")
                FileUtils.copyFile(sourceDir, destFile)
                
                val savedInfo = AppInfo(appInfo.appName, appInfo.packageName, appInfo.icon, true, 0, destFile.absolutePath)
                savedApks.add(0, savedInfo)
                saveSavedApks()
                
                progressDialog.dismiss()
                Toast.makeText(this@MainActivity, "APK saved: ${destFile.name}", Toast.LENGTH_LONG).show()
                loadSavedApks()
                updateFragments()
            } catch (e: Exception) {
                progressDialog.dismiss()
                Toast.makeText(this@MainActivity, "Save failed: ${e.message}", Toast.LENGTH_LONG).show()
                LogUtils.e("Save APK failed", e)
            }
        }
    }

    fun shareApk(appInfo: AppInfo) {
        if (appInfo.apkPath.isNullOrEmpty()) {
            Toast.makeText(this, "APK not found", Toast.LENGTH_SHORT).show()
            return
        }

        val apkFile = File(appInfo.apkPath)
        if (!apkFile.exists()) {
            Toast.makeText(this, "APK file not found", Toast.LENGTH_SHORT).show()
            return
        }

        val uri = if (Build.VERSION.SDK_INT &gt;= Build.VERSION_CODES.N) {
            androidx.core.content.FileProvider.getUriForFile(
                this,
                "$packageName.fileprovider",
                apkFile
            )
        } else {
            Uri.fromFile(apkFile)
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.android.package-archive"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Share ${appInfo.appName}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(shareIntent, "Share APK"))
    }

    fun installApk(appInfo: AppInfo) {
        if (appInfo.apkPath.isNullOrEmpty()) {
            Toast.makeText(this, "APK not found", Toast.LENGTH_SHORT).show()
            return
        }

        val apkFile = File(appInfo.apkPath)
        if (!apkFile.exists()) {
            Toast.makeText(this, "APK file not found", Toast.LENGTH_SHORT).show()
            return
        }

        if (Build.VERSION.SDK_INT &gt;= Build.VERSION_CODES.O) {
            if (!packageManager.canRequestPackageInstalls()) {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                intent.data = Uri.parse("package:$packageName")
                startActivityForResult(intent, REQUEST_CODE_INSTALL)
                return
            }
        }

        val intent = Intent(Intent.ACTION_VIEW)
        val uri = if (Build.VERSION.SDK_INT &gt;= Build.VERSION_CODES.N) {
            androidx.core.content.FileProvider.getUriForFile(
                this,
                "$packageName.fileprovider",
                apkFile
            )
        } else {
            Uri.fromFile(apkFile)
        }

        intent.setDataAndType(uri, "application/vnd.android.package-archive")
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        startActivity(intent)
    }

    /**
     * 开始克隆流程 - 新版本使用模块化的CloneManager
     */
    fun startCloneFlow(appInfo: AppInfo) {
        AlertDialog.Builder(this)
            .setTitle("Clone ${appInfo.appName}")
            .setMessage("This will create a cloned version of the app.\n\nNOTE: Professional APK modification requires tools like APKTool and Uber Apk Signer.")
            .setPositiveButton("Continue (Demo Mode)") { _, _ -&gt;
                performCloneDemo(appInfo)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performCloneDemo(appInfo: AppInfo) {
        scope.launch {
            val progressDialog = ProgressDialog(this@MainActivity)
            progressDialog.setMessage("Starting demo clone...")
            progressDialog.setCancelable(false)
            progressDialog.show()

            try {
                val sourceDir = File(packageManager.getApplicationInfo(appInfo.packageName, 0).sourceDir)
                val outputDir = File(getExternalFilesDir(null), "cloned_apks")
                FileUtils.ensureDirectoryExists(outputDir)
                
                // 为了演示，我们只是保存一个副本，真实克隆需要APKTool等工具
                val newPackageName = "${appInfo.packageName}.clone${System.currentTimeMillis()}"
                val newName = "${appInfo.appName} (Clone)"
                
                val config = CloneConfig(
                    originalApk = sourceDir,
                    newPackageName = newPackageName,
                    newAppName = newName,
                    outputDir = outputDir
                )

                // 演示模式下的回调
                val listener = object : CloneProgressListener {
                    override fun onProgress(step: Int, total: Int, message: String) {
                        progressDialog.setMessage("Step $step/$total: $message")
                    }

                    override fun onSuccess(clonedApk: File) {
                        progressDialog.dismiss()
                        Toast.makeText(this@MainActivity, "Demo complete! Cloned APK created.", Toast.LENGTH_LONG).show()
                        
                        val savedInfo = AppInfo(newName, newPackageName, appInfo.icon, true, 0, clonedApk.absolutePath)
                        savedApks.add(0, savedInfo)
                        saveSavedApks()
                        updateFragments()
                    }

                    override fun onError(error: String) {
                        progressDialog.dismiss()
                        Toast.makeText(this@MainActivity, "Clone failed: $error", Toast.LENGTH_LONG).show()
                    }
                }

                // 在实际项目中，这里会调用 cloneManager.cloneApp()
                // 现在为了演示，我们直接复制文件
                val destFile = File(outputDir, "${newName.replace(" ", "_")}.apk")
                FileUtils.copyFile(sourceDir, destFile)
                
                progressDialog.dismiss()
                Toast.makeText(this@MainActivity, "APK copied as clone (demo mode).\nFor real APK modification, integrate APKTool.", Toast.LENGTH_LONG).show()
                
                val savedInfo = AppInfo(newName, newPackageName, appInfo.icon, true, 0, destFile.absolutePath)
                savedApks.add(0, savedInfo)
                saveSavedApks()
                updateFragments()

            } catch (e: Exception) {
                progressDialog.dismiss()
                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                LogUtils.e("Clone failed", e)
            }
        }
    }

    fun showGuideDialog() {
        AlertDialog.Builder(this)
            .setTitle("App Cloning Guide")
            .setMessage("""
Professional APK Cloning Requirements:

1. APKTool - Open source tool for decompiling/recompiling APKs
2. Uber Apk Signer - For proper APK signature
3. Android SDK - For build tools

For better results, use your phone's built-in clone feature!
            """.trimIndent())
            .setPositiveButton("OK", null)
            .show()
    }

    fun showDeleteConfirmDialog(appInfo: AppInfo) {
        AlertDialog.Builder(this)
            .setTitle("Delete?")
            .setMessage("Delete this saved APK?")
            .setPositiveButton("Delete") { _, _ -&gt;
                try {
                    if (appInfo.apkPath != null) {
                        File(appInfo.apkPath).delete()
                    }
                    savedApks.remove(appInfo)
                    saveSavedApks()
                    updateFragments()
                    Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Delete failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateFragments() {
        viewPagerAdapter.getFragment(0).updateAppList(originalApps)
        viewPagerAdapter.getFragment(1).updateAppList(savedApks)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_INSTALL) {
            if (Build.VERSION.SDK_INT &gt;= Build.VERSION_CODES.O &amp;&amp; packageManager.canRequestPackageInstalls()) {
                Toast.makeText(this, "Permission granted - try again", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}

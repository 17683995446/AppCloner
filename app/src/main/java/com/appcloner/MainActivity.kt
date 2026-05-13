package com.appcloner

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var viewPagerAdapter: ViewPagerAdapter
    private lateinit var sharedPreferences: SharedPreferences
    private val savedApks = mutableListOf<AppInfo>()
    private val originalApps = mutableListOf<AppInfo>()
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)

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

        setupViewPager()
        loadAppList()

        findViewById<com.google.android.material.button.MaterialButton>(R.id.helpBtn)?.setOnClickListener {
            showGuideDialog()
        }
    }

    private fun setupViewPager() {
        viewPagerAdapter = ViewPagerAdapter(this)
        val viewPager = findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.viewPager)
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)

        viewPager.adapter = viewPagerAdapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Apps"
                1 -> "Saved APKs"
                else -> ""
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

    private fun getInstalledApps(): List<AppInfo> {
        val apps = mutableListOf<AppInfo>()
        val packageManager = packageManager
        val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

        for (appInfo in packages) {
            if (appInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0) {
                try {
                    val appName = packageManager.getApplicationLabel(appInfo).toString()
                    val icon = packageManager.getApplicationIcon(appInfo.packageName)
                    apps.add(AppInfo(appName, appInfo.packageName, icon))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return apps.sortedBy { it.appName }
    }

    private fun loadSavedApks() {
        savedApks.clear()
        val savedString = sharedPreferences.getString(KEY_SAVED_APKS, "") ?: ""
        if (savedString.isNotEmpty()) {
            val entries = savedString.split(",").filter { it.isNotEmpty() }
            for (entry in entries) {
                val parts = entry.split("|")
                if (parts.size >= 4) {
                    val appName = parts[0]
                    val packageName = parts[1]
                    val apkPath = parts[2]
                    val timestamp = parts[3]

                    try {
                        val file = File(apkPath)
                        if (file.exists()) {
                            val icon = try {
                                val info = packageManager.getPackageArchiveInfo(apkPath, 0)
                                if (info != null) {
                                    info.applicationInfo.sourceDir = apkPath
                                    info.applicationInfo.publicSourceDir = apkPath
                                    packageManager.getApplicationIcon(info.applicationInfo)
                                } else {
                                    packageManager.applicationInfo.loadIcon(packageManager)
                                }
                            } catch (e: Exception) {
                                packageManager.applicationInfo.loadIcon(packageManager)
                            }
                            savedApks.add(AppInfo("$appName ($timestamp)", packageName, icon, true, 0, apkPath))
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun saveSavedApks() {
        val savedString = savedApks.joinToString(",") { app ->
            val timestamp = try {
                File(app.apkPath ?: "").lastModified().let {
                    java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(it))
                }
            } catch (e: Exception) {
                "Unknown"
            }
            "${app.appName}|${app.packageName}|${app.apkPath ?: ""}|$timestamp"
        }
        sharedPreferences.edit().putString(KEY_SAVED_APKS, savedString).apply()
    }

    fun saveApk(appInfo: AppInfo) {
        scope.launch {
            val progressDialog = ProgressDialog(this@MainActivity)
            progressDialog.setMessage("Saving APK...")
            progressDialog.setCancelable(false)
            progressDialog.show()

            val result = withContext(Dispatchers.IO) {
                performSaveApk(appInfo)
            }

            progressDialog.dismiss()

            if (result.first) {
                Toast.makeText(this@MainActivity, "APK saved successfully!", Toast.LENGTH_LONG).show()
                loadSavedApks()
                updateFragments()
            } else {
                Toast.makeText(this@MainActivity, result.second, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun performSaveApk(appInfo: AppInfo): Pair<Boolean, String> {
        return try {
            val sourceFile = File(packageManager.getApplicationInfo(appInfo.packageName, 0).sourceDir)
            
            val outputDir = File(getExternalFilesDir(null), "saved_apks")
            if (!outputDir.exists()) outputDir.mkdirs()
            
            val destFile = File(outputDir, "${appInfo.packageName}_${System.currentTimeMillis()}.apk")
            
            FileInputStream(sourceFile).use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            
            val savedInfo = AppInfo(
                appInfo.appName,
                appInfo.packageName,
                appInfo.icon,
                true,
                0,
                destFile.absolutePath
            )
            
            savedApks.add(0, savedInfo)
            saveSavedApks()
            
            Pair(true, "Success! Saved to ${destFile.absolutePath}")
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(false, "Error: ${e.message}")
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

        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!packageManager.canRequestPackageInstalls()) {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                intent.data = Uri.parse("package:$packageName")
                startActivityForResult(intent, REQUEST_CODE_INSTALL)
                return
            }
        }

        val intent = Intent(Intent.ACTION_VIEW)
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(
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

    fun showGuideDialog() {
        AlertDialog.Builder(this)
            .setTitle("How to Use App Cloning")
            .setMessage("""
Android has built-in app cloning features! Here's how to use them:

1. Open Settings
2. Go to "Apps" or "Applications"
3. Look for "Dual apps", "App twin", or "Clone apps"
4. Select the app you want to clone
5. Enable the clone

Note: Feature name and availability varies by device manufacturer.
            """.trimIndent())
            .setPositiveButton("OK", null)
            .show()
    }

    fun showDeleteConfirmDialog(appInfo: AppInfo) {
        AlertDialog.Builder(this)
            .setTitle("Delete Saved APK?")
            .setMessage("Are you sure you want to delete this saved APK?")
            .setPositiveButton("Delete") { _, _ ->
                deleteSavedApk(appInfo)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteSavedApk(appInfo: AppInfo) {
        try {
            if (appInfo.apkPath != null) {
                val apkFile = File(appInfo.apkPath)
                if (apkFile.exists()) apkFile.delete()
            }

            savedApks.remove(appInfo)
            saveSavedApks()

            updateFragments()
            Toast.makeText(this, "APK deleted", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Delete failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateFragments() {
        viewPagerAdapter.getFragment(0).updateAppList(originalApps)
        viewPagerAdapter.getFragment(1).updateAppList(savedApks)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_INSTALL) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && packageManager.canRequestPackageInstalls()) {
                Toast.makeText(this, "Permission granted - try again", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
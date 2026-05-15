
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
    private val savedApks = mutableListOf<AppInfo>()
    private val originalApps = mutableListOf<AppInfo>()
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)
    
    private lateinit var cloneManager: CloneManager

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

        cloneManager = CloneManager(applicationContext)
        
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
        val packages = packageManager.getInstalledApplications(android.content.pm.PackageManager.GET_META_DATA)

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
                if (parts.size >= 3) {
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

        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
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

    fun startCloneFlow(appInfo: AppInfo) {
        AlertDialog.Builder(this)
            .setTitle("Clone ${appInfo.appName}")
            .setMessage("This will create a cloned version of the app.\n\nNOTE: This requires APKTool integration for real cloning.")
            .setPositiveButton("Clone") { _, _ ->
                performClone(appInfo)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performClone(appInfo: AppInfo) {
        scope.launch {
            val progressDialog = ProgressDialog(this@MainActivity)
            progressDialog.setMessage("Cloning app...")
            progressDialog.setCancelable(false)
            progressDialog.show()

            try {
                val sourceDir = File(packageManager.getApplicationInfo(appInfo.packageName, 0).sourceDir)
                val outputDir = File(getExternalFilesDir(null), "cloned_apks")
                FileUtils.ensureDirectoryExists(outputDir)
                
                val newPackageName = "${appInfo.packageName}.clone${System.currentTimeMillis()}"
                val newName = "${appInfo.appName} (Clone)"
                
                val config = CloneConfig(
                    originalApk = sourceDir,
                    newPackageName = newPackageName,
                    newAppName = newName,
                    outputDir = outputDir
                )

                val listener = object : CloneProgressListener {
                    override fun onProgress(step: Int, total: Int, message: String) {
                        progressDialog.setMessage("Step $step/$total: $message")
                    }

                    override fun onSuccess(clonedApk: File) {
                        progressDialog.dismiss()
                        Toast.makeText(this@MainActivity, "Clone successful! APK created.", Toast.LENGTH_LONG).show()
                        
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

                val result = cloneManager.cloneApp(config, listener)
                
                if (result.isSuccess) {
                    val clonedApk = result.getOrThrow()
                    val savedInfo = AppInfo(newName, newPackageName, appInfo.icon, true, 0, clonedApk.absolutePath)
                    savedApks.add(0, savedInfo)
                    saveSavedApks()
                    updateFragments()
                    
                    progressDialog.dismiss()
                    Toast.makeText(this@MainActivity, "Clone successful! You can install it now.", Toast.LENGTH_LONG).show()
                } else {
                    progressDialog.dismiss()
                    val errorMsg = result.exceptionOrNull()?.message ?: "Unknown error"
                    Toast.makeText(this@MainActivity, "Clone failed: $errorMsg\n\nTrying demo mode...", Toast.LENGTH_LONG).show()
                    
                    performCloneFallback(appInfo)
                }

            } catch (e: Exception) {
                progressDialog.dismiss()
                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                LogUtils.e("Clone failed", e)
            }
        }
    }

    private fun performCloneFallback(appInfo: AppInfo) {
        scope.launch {
            val progressDialog = ProgressDialog(this@MainActivity)
            progressDialog.setMessage("Using fallback mode...")
            progressDialog.setCancelable(false)
            progressDialog.show()

            try {
                val sourceDir = File(packageManager.getApplicationInfo(appInfo.packageName, 0).sourceDir)
                val outputDir = File(getExternalFilesDir(null), "cloned_apks")
                FileUtils.ensureDirectoryExists(outputDir)
                
                val newPackageName = "${appInfo.packageName}.clone${System.currentTimeMillis()}"
                val newName = "${appInfo.appName} (Clone)"
                
                val destFile = File(outputDir, "${newName.replace(" ", "_")}.apk")
                FileUtils.copyFile(sourceDir, destFile)
                
                progressDialog.dismiss()
                Toast.makeText(this@MainActivity, "Fallback: APK copied.\nNote: For real cloning, use your device's built-in clone feature.", Toast.LENGTH_LONG).show()
                
                val savedInfo = AppInfo(newName, newPackageName, appInfo.icon, true, 0, destFile.absolutePath)
                savedApks.add(0, savedInfo)
                saveSavedApks()
                updateFragments()

            } catch (e: Exception) {
                progressDialog.dismiss()
                Toast.makeText(this@MainActivity, "Fallback error: ${e.message}", Toast.LENGTH_LONG).show()
                LogUtils.e("Fallback clone failed", e)
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
            .setPositiveButton("Delete") { _, _ ->
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

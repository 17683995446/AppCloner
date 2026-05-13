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
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var viewPagerAdapter: ViewPagerAdapter
    private lateinit var sharedPreferences: SharedPreferences
    private val clonedApps = mutableListOf<AppInfo>()
    private val originalApps = mutableListOf<AppInfo>()
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    companion object {
        private const val PREFS_NAME = "AppClonerPrefs"
        private const val KEY_CLONED_APPS = "cloned_apps"
        private const val REQUEST_CODE_INSTALL = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadClonedApps()

        setupViewPager()
        loadAppList()
    }

    private fun setupViewPager() {
        viewPagerAdapter = ViewPagerAdapter(this)
        val viewPager = findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.viewPager)
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        
        viewPager.adapter = viewPagerAdapter
        
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.app_list)
                1 -> getString(R.string.cloned_apps)
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

    private fun loadClonedApps() {
        val clonedAppsString = sharedPreferences.getString(KEY_CLONED_APPS, "") ?: ""
        if (clonedAppsString.isNotEmpty()) {
            val packages = clonedAppsString.split(",").filter { it.isNotEmpty() }
            for (packageInfo in packages) {
                val parts = packageInfo.split("|")
                if (parts.size >= 3) {
                    val packageName = parts[0]
                    val cloneId = parts[1].toIntOrNull() ?: 0
                    val appName = parts[2]
                    
                    try {
                        val appInfo = packageManager.getApplicationInfo(packageName, 0)
                        val icon = packageManager.getApplicationIcon(appInfo)
                        clonedApps.add(AppInfo(appName, packageName, icon, true, cloneId))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun saveClonedApps() {
        val clonedAppsString = clonedApps.joinToString(",") { 
            "${it.packageName}|${it.cloneId}|${it.appName}"
        }
        sharedPreferences.edit().putString(KEY_CLONED_APPS, clonedAppsString).apply()
    }

    fun cloneApp(appInfo: AppInfo) {
        scope.launch {
            val progressDialog = ProgressDialog(this@MainActivity)
            progressDialog.setMessage(getString(R.string.cloning))
            progressDialog.setCancelable(false)
            progressDialog.show()

            val result = withContext(Dispatchers.IO) {
                performClone(appInfo)
            }

            progressDialog.dismiss()

            if (result) {
                Toast.makeText(this@MainActivity, getString(R.string.clone_success), Toast.LENGTH_SHORT).show()
                updateFragments()
            } else {
                Toast.makeText(this@MainActivity, getString(R.string.clone_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun performClone(appInfo: AppInfo): Boolean {
        return try {
            val sourceDir = appInfo.packageName.let { 
                packageManager.getApplicationInfo(it, 0).sourceDir 
            }
            
            val cloneId = getNextCloneId(appInfo.packageName)
            val newPackageName = "${appInfo.packageName}.clone$cloneId"
            
            val tempFile = File(cacheDir, "clone_temp.apk")
            
            copyFile(File(sourceDir), tempFile)
            
            val clonedApp = AppInfo(
                appName = appInfo.appName,
                packageName = newPackageName,
                icon = appInfo.icon,
                isCloned = true,
                cloneId = cloneId
            )
            
            clonedApps.add(clonedApp)
            saveClonedApps()
            
            requestInstallApk(tempFile)
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun getNextCloneId(packageName: String): Int {
        var maxId = 0
        clonedApps.forEach { 
            if (it.packageName.startsWith(packageName)) {
                if (it.cloneId > maxId) maxId = it.cloneId
            }
        }
        return maxId + 1
    }

    private fun copyFile(source: File, dest: File) {
        val inputStream = FileInputStream(source)
        val outputStream = FileOutputStream(dest)
        
        inputStream.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun requestInstallApk(apkFile: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!packageManager.canRequestPackageInstalls()) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:$packageName")
                )
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

    fun launchClonedApp(appInfo: AppInfo) {
        try {
            val intent = packageManager.getLaunchIntentForPackage(appInfo.packageName)
            if (intent != null) {
                startActivity(intent)
            } else {
                Toast.makeText(this, "无法启动应用", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "启动失败", Toast.LENGTH_SHORT).show()
        }
    }

    fun showDeleteConfirmDialog(appInfo: AppInfo) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_clone)
            .setMessage(R.string.delete_confirm)
            .setPositiveButton(R.string.yes) { _, _ ->
                deleteClonedApp(appInfo)
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    private fun deleteClonedApp(appInfo: AppInfo) {
        try {
            clonedApps.remove(appInfo)
            saveClonedApps()
            
            val intent = Intent(Intent.ACTION_DELETE)
            intent.data = Uri.parse("package:${appInfo.packageName}")
            startActivity(intent)
            
            updateFragments()
            Toast.makeText(this, "分身已删除", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "删除失败", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateFragments() {
        viewPagerAdapter.getFragment(0).updateAppList(originalApps)
        viewPagerAdapter.getFragment(1).updateAppList(clonedApps)
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}

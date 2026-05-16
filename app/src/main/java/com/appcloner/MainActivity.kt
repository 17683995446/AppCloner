
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
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.appcloner.core.CloneConfig
import com.appcloner.core.CloneManager
import com.appcloner.core.CloneProgressListener
import com.appcloner.utils.FileUtils
import com.appcloner.utils.LogUtils
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var viewPagerAdapter: ViewPagerAdapter
    private lateinit var sharedPreferences: SharedPreferences
    private val savedApks = mutableListOf<AppInfo>()
    private val originalApps = mutableListOf<AppInfo>()
    private val filteredApps = mutableListOf<AppInfo>()
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)
    
    private lateinit var cloneManager: CloneManager
    private var isDarkMode = false
    private var sortBy = SortType.NAME
    private var isMultiSelectMode = false
    private val selectedApps = mutableSetOf<String>()

    companion object {
        private const val PREFS_NAME = "AppClonerPrefs"
        private const val KEY_SAVED_APKS = "savedApks"
        private const val KEY_DARK_MODE = "darkMode"
        private const val KEY_SORT_BY = "sortBy"
        private const val REQUEST_CODE_INSTALL = 1001
    }

    enum class SortType {
        NAME,
        SIZE,
        INSTALL_TIME,
        UPDATE_TIME
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Load saved preferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        isDarkMode = sharedPreferences.getBoolean(KEY_DARK_MODE, false)
        sortBy = SortType.valueOf(sharedPreferences.getString(KEY_SORT_BY, SortType.NAME.name) ?: SortType.NAME.name)
        
        // Apply theme
        applyTheme()
        
        setContentView(R.layout.activity_main)

        loadSavedApks()
        LogUtils.setDefaultTag("AppClonerMain")
        LogUtils.i("App started - Version 3.0 (Enhanced Features)")

        cloneManager = CloneManager(applicationContext)
        
        setupUI()
        loadAppList()
    }

    private fun applyTheme() {
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    private fun setupUI() {
        setupViewPager()
        setupSearch()
        setupButtons()
    }

    private fun setupViewPager() {
        viewPagerAdapter = ViewPagerAdapter(this)
        val viewPager = findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.viewPager)
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)

        viewPager.adapter = viewPagerAdapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "安装应用"
                1 -> "保存的APK"
                else -> ""
            }
        }.attach()
    }

    private fun setupSearch() {
        val searchEditText = findViewById<EditText>(R.id.searchEditText)
        searchEditText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterApps(s?.toString() ?: "")
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupButtons() {
        // Theme toggle button
        findViewById<Button>(R.id.themeBtn)?.setOnClickListener {
            toggleTheme()
        }
        
        // Sort button
        findViewById<Button>(R.id.sortBtn)?.setOnClickListener {
            showSortDialog()
        }
        
        // Batch action button
        findViewById<Button>(R.id.batchBtn)?.setOnClickListener {
            toggleMultiSelectMode()
        }
        
        // Help button
        findViewById<Button>(R.id.helpBtn)?.setOnClickListener {
            showGuideDialog()
        }
    }

    private fun toggleTheme() {
        isDarkMode = !isDarkMode
        sharedPreferences.edit().putBoolean(KEY_DARK_MODE, isDarkMode).apply()
        recreate()
    }

    private fun showSortDialog() {
        val sortOptions = arrayOf(
            "按名称",
            "按大小",
            "按安装时间",
            "按更新时间"
        )
        
        val currentIndex = SortType.values().indexOf(sortBy)
        
        AlertDialog.Builder(this)
            .setTitle("排序方式")
            .setSingleChoiceItems(sortOptions, currentIndex) { _, which ->
                sortBy = SortType.values()[which]
                sharedPreferences.edit().putString(KEY_SORT_BY, sortBy.name).apply()
                loadAppList()
            }
            .setPositiveButton("确定", null)
            .show()
    }

    private fun toggleMultiSelectMode() {
        isMultiSelectMode = !isMultiSelectMode
        selectedApps.clear()
        
        if (isMultiSelectMode) {
            Toast.makeText(this, "多选模式已开启", Toast.LENGTH_SHORT).show()
        }
        
        updateFragments()
    }

    private fun filterApps(query: String) {
        val viewPager = findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.viewPager)
        val currentFragment = viewPagerAdapter.getFragment(viewPager.currentItem)
        
        if (viewPager.currentItem == 0) {
            filteredApps.clear()
            if (query.isEmpty()) {
                filteredApps.addAll(originalApps)
            } else {
                filteredApps.addAll(
                    originalApps.filter { 
                        it.appName.contains(query, ignoreCase = true) ||
                        it.packageName.contains(query, ignoreCase = true)
                    }
                )
            }
            currentFragment.updateAppList(filteredApps)
        } else {
            updateFragments()
        }
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
                    
                    // Get detailed info
                    val packageInfo = packageManager.getPackageInfo(appInfo.packageName, 0)
                    val apkFile = File(appInfo.sourceDir)
                    val apkSize = apkFile.length()
                    
                    apps.add(
                        AppInfo(
                            appName = name,
                            packageName = appInfo.packageName,
                            icon = icon,
                            versionName = packageInfo.versionName,
                            versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                packageInfo.longVersionCode
                            } else {
                                packageInfo.versionCode.toLong()
                            },
                            installTime = packageInfo.firstInstallTime,
                            updateTime = packageInfo.lastUpdateTime,
                            apkSize = apkSize
                        )
                    )
                } catch (e: Exception) {
                    LogUtils.e("Failed to load app: ${appInfo.packageName}", e)
                }
            }
        }
        
        // Sort apps
        return when (sortBy) {
            SortType.NAME -> apps.sortedBy { it.appName }
            SortType.SIZE -> apps.sortedByDescending { it.apkSize }
            SortType.INSTALL_TIME -> apps.sortedByDescending { it.installTime }
            SortType.UPDATE_TIME -> apps.sortedByDescending { it.updateTime }
        }
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
            progressDialog.setMessage("保存APK...")
            progressDialog.setCancelable(false)
            progressDialog.show()

            try {
                val sourceDir = File(packageManager.getApplicationInfo(appInfo.packageName, 0).sourceDir)
                val outputDir = File(getExternalFilesDir(null), "saved_apks")
                FileUtils.ensureDirectoryExists(outputDir)
                
                val destFile = File(outputDir, "${appInfo.packageName}_${System.currentTimeMillis()}.apk")
                FileUtils.copyFile(sourceDir, destFile)
                
                val savedInfo = AppInfo(
                    appName = appInfo.appName,
                    packageName = appInfo.packageName,
                    icon = appInfo.icon,
                    isCloned = true,
                    apkPath = destFile.absolutePath,
                    versionName = appInfo.versionName,
                    apkSize = destFile.length()
                )
                savedApks.add(0, savedInfo)
                saveSavedApks()
                
                progressDialog.dismiss()
                Toast.makeText(this@MainActivity, "APK已保存: ${destFile.name}", Toast.LENGTH_LONG).show()
                loadSavedApks()
                updateFragments()
            } catch (e: Exception) {
                progressDialog.dismiss()
                Toast.makeText(this@MainActivity, "保存失败: ${e.message}", Toast.LENGTH_LONG).show()
                LogUtils.e("Save APK failed", e)
            }
        }
    }

    fun shareApk(appInfo: AppInfo) {
        if (appInfo.apkPath.isNullOrEmpty()) {
            Toast.makeText(this, "APK未找到", Toast.LENGTH_SHORT).show()
            return
        }

        val apkFile = File(appInfo.apkPath)
        if (!apkFile.exists()) {
            Toast.makeText(this, "APK文件不存在", Toast.LENGTH_SHORT).show()
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
            putExtra(Intent.EXTRA_SUBJECT, "分享 ${appInfo.appName}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(shareIntent, "分享APK"))
    }

    fun installApk(appInfo: AppInfo) {
        if (appInfo.apkPath.isNullOrEmpty()) {
            Toast.makeText(this, "APK未找到", Toast.LENGTH_SHORT).show()
            return
        }

        val apkFile = File(appInfo.apkPath)
        if (!apkFile.exists()) {
            Toast.makeText(this, "APK文件不存在", Toast.LENGTH_SHORT).show()
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

    fun launchApp(appInfo: AppInfo) {
        try {
            val intent = packageManager.getLaunchIntentForPackage(appInfo.packageName)
            if (intent != null) {
                startActivity(intent)
            } else {
                Toast.makeText(this, "无法启动应用", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "启动失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun showAppDetails(appInfo: AppInfo) {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val message = StringBuilder()
            .append("应用名称: ${appInfo.appName}\n")
            .append("包名: ${appInfo.packageName}\n")
        
        if (appInfo.versionName.isNotEmpty()) {
            message.append("版本: ${appInfo.versionName} (${appInfo.versionCode})\n")
        }
        
        if (appInfo.apkSize > 0) {
            message.append("大小: ${appInfo.getFormattedSize()}\n")
        }
        
        if (appInfo.installTime > 0) {
            message.append("安装时间: ${sdf.format(Date(appInfo.installTime))}\n")
        }
        
        if (appInfo.updateTime > 0) {
            message.append("更新时间: ${sdf.format(Date(appInfo.updateTime))}\n")
        }
        
        if (!appInfo.apkPath.isNullOrEmpty()) {
            message.append("APK路径: ${appInfo.apkPath}\n")
        }

        AlertDialog.Builder(this)
            .setTitle(appInfo.appName)
            .setMessage(message.toString())
            .setPositiveButton("确定", null)
            .show()
    }

    fun startCloneFlow(appInfo: AppInfo) {
        AlertDialog.Builder(this)
            .setTitle("克隆 ${appInfo.appName}")
            .setMessage("这将创建应用的克隆版本。\n\n注意：实际克隆需要APKTool工具支持。")
            .setPositiveButton("克隆") { _, _ ->
                performClone(appInfo)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun performClone(appInfo: AppInfo) {
        scope.launch {
            val progressDialog = ProgressDialog(this@MainActivity)
            progressDialog.setMessage("克隆应用中...")
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
                        progressDialog.setMessage("步骤 $step/$total: $message")
                    }

                    override fun onSuccess(clonedApk: File) {
                        progressDialog.dismiss()
                        Toast.makeText(this@MainActivity, "克隆成功！APK已创建。", Toast.LENGTH_LONG).show()
                        
                        val savedInfo = AppInfo(newName, newPackageName, appInfo.icon, true, 0, clonedApk.absolutePath)
                        savedApks.add(0, savedInfo)
                        saveSavedApks()
                        updateFragments()
                    }

                    override fun onError(error: String) {
                        progressDialog.dismiss()
                        Toast.makeText(this@MainActivity, "克隆失败: $error", Toast.LENGTH_LONG).show()
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
                    Toast.makeText(this@MainActivity, "克隆成功！现在可以安装。", Toast.LENGTH_LONG).show()
                } else {
                    progressDialog.dismiss()
                    val errorMsg = result.exceptionOrNull()?.message ?: "未知错误"
                    Toast.makeText(this@MainActivity, "克隆失败: $errorMsg\n\n尝试演示模式...", Toast.LENGTH_LONG).show()
                    
                    performCloneFallback(appInfo)
                }

            } catch (e: Exception) {
                progressDialog.dismiss()
                Toast.makeText(this@MainActivity, "错误: ${e.message}", Toast.LENGTH_LONG).show()
                LogUtils.e("Clone failed", e)
            }
        }
    }

    private fun performCloneFallback(appInfo: AppInfo) {
        scope.launch {
            val progressDialog = ProgressDialog(this@MainActivity)
            progressDialog.setMessage("使用后备模式...")
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
                Toast.makeText(this@MainActivity, "后备模式：APK已复制。\n注意：实际克隆请使用设备的内置克隆功能。", Toast.LENGTH_LONG).show()
                
                val savedInfo = AppInfo(newName, newPackageName, appInfo.icon, true, 0, destFile.absolutePath)
                savedApks.add(0, savedInfo)
                saveSavedApks()
                updateFragments()

            } catch (e: Exception) {
                progressDialog.dismiss()
                Toast.makeText(this@MainActivity, "后备模式错误: ${e.message}", Toast.LENGTH_LONG).show()
                LogUtils.e("Fallback clone failed", e)
            }
        }
    }

    fun showGuideDialog() {
        AlertDialog.Builder(this)
            .setTitle("应用克隆指南")
            .setMessage("""
AppCloner 3.0 功能：

📱 应用管理
• 查看所有已安装应用
• 查看应用详细信息
• 快速启动应用

🔍 搜索和排序
• 按名称搜索应用
• 多种排序方式
• 深色/浅色主题

💾 APK操作
• 保存APK文件
• 分享APK给朋友
• 安装已保存的APK

🔄 克隆功能
• 创建应用克隆
• 无限数量克隆
• 批量选择操作

⚠️ 注意
专业的APK克隆需要：
1. APKTool - APK反编译工具
2. Uber Apk Signer - APK签名工具
3. Android SDK - 构建工具

更好的效果请使用设备的内置克隆功能！
            """.trimIndent())
            .setPositiveButton("确定", null)
            .show()
    }

    fun showDeleteConfirmDialog(appInfo: AppInfo) {
        AlertDialog.Builder(this)
            .setTitle("删除？")
            .setMessage("删除这个已保存的APK？")
            .setPositiveButton("删除") { _, _ ->
                try {
                    if (appInfo.apkPath != null) {
                        File(appInfo.apkPath).delete()
                    }
                    savedApks.remove(appInfo)
                    saveSavedApks()
                    updateFragments()
                    Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "删除失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun updateFragments() {
        val viewPager = findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.viewPager)
        if (viewPager != null && viewPagerAdapter != null) {
            viewPagerAdapter.getFragment(0).updateAppList(if (filteredApps.isEmpty()) originalApps else filteredApps)
            viewPagerAdapter.getFragment(1).updateAppList(savedApks)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_INSTALL) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && packageManager.canRequestPackageInstalls()) {
                Toast.makeText(this, "权限已获取 - 重试", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}

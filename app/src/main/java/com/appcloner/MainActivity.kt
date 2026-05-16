
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
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var viewPagerAdapter: ViewPagerAdapter
    private lateinit var sharedPreferences: SharedPreferences
    private val savedApks = mutableListOf&lt;AppInfo&gt;()
    private val originalApps = mutableListOf&lt;AppInfo&gt;()
    private val filteredApps = mutableListOf&lt;AppInfo&gt;()
    private val selectedApps = mutableSetOf&lt;String&gt;()
    private val favoriteApps = mutableSetOf&lt;String&gt;()
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)
    
    private lateinit var cloneManager: CloneManager
    private var isDarkMode = false
    private var sortBy = SortType.NAME
    private var isMultiSelectMode = false
    private var currentTab = 0

    companion object {
        private const val PREFS_NAME = "AppClonerPrefs"
        private const val KEY_SAVED_APKS = "savedApks"
        private const val KEY_DARK_MODE = "darkMode"
        private const val KEY_SORT_BY = "sortBy"
        private const val KEY_FAVORITES = "favorites"
        private const val REQUEST_CODE_INSTALL = 1001
    }

    enum class SortType {
        NAME,
        SIZE,
        INSTALL_TIME,
        UPDATE_TIME,
        FAVORITE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        isDarkMode = sharedPreferences.getBoolean(KEY_DARK_MODE, false)
        sortBy = SortType.valueOf(sharedPreferences.getString(KEY_SORT_BY, SortType.NAME.name) ?: SortType.NAME.name)
        favoriteApps.addAll(sharedPreferences.getString(KEY_FAVORITES, "")?.split(",")?.filter { it.isNotEmpty() } ?: emptyList())
        
        applyTheme()
        setContentView(R.layout.activity_main)

        loadSavedApks()
        LogUtils.setDefaultTag("AppClonerMain")
        LogUtils.i("App started - Version 3.1 (Enhanced Features)")

        cloneManager = CloneManager(applicationContext)
        
        setupUI()
        loadAppList()
    }

    private fun applyTheme() {
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    private fun setupUI() {
        setupViewPager()
        setupSearch()
        setupButtons()
    }

    private fun setupViewPager() {
        viewPagerAdapter = ViewPagerAdapter(this)
        val viewPager = findViewById&lt;androidx.viewpager2.widget.ViewPager2&gt;(R.id.viewPager)
        val tabLayout = findViewById&lt;TabLayout&gt;(R.id.tabLayout)

        viewPager.adapter = viewPagerAdapter
        viewPager.registerOnPageChangeCallback(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentTab = position
            }
        })

        TabLayoutMediator(tabLayout, viewPager) { tab, position -&gt;
            tab.text = when (position) {
                0 -&gt; "安装应用"
                1 -&gt; "保存的APK"
                2 -&gt; "收藏"
                else -&gt; ""
            }
        }.attach()
    }

    private fun setupSearch() {
        val searchEditText = findViewById&lt;EditText&gt;(R.id.searchEditText)
        searchEditText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterApps(s?.toString() ?: "")
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupButtons() {
        findViewById&lt;Button&gt;(R.id.themeBtn)?.setOnClickListener {
            toggleTheme()
        }
        
        findViewById&lt;Button&gt;(R.id.sortBtn)?.setOnClickListener {
            showSortDialog()
        }
        
        findViewById&lt;Button&gt;(R.id.batchBtn)?.setOnClickListener {
            showBatchActionsDialog()
        }
        
        findViewById&lt;Button&gt;(R.id.helpBtn)?.setOnClickListener {
            showHelpDialog()
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
            "按更新时间",
            "收藏优先"
        )
        
        val currentIndex = SortType.values().indexOf(sortBy)
        
        AlertDialog.Builder(this)
            .setTitle("排序方式")
            .setSingleChoiceItems(sortOptions, currentIndex) { _, which -&gt;
                sortBy = SortType.values()[which]
                sharedPreferences.edit().putString(KEY_SORT_BY, sortBy.name).apply()
                loadAppList()
            }
            .setPositiveButton("确定", null)
            .show()
    }

    private fun showBatchActionsDialog() {
        val options = if (currentTab == 0) {
            arrayOf(
                "批量收藏",
                "批量保存APK",
                "批量启动",
                "导出应用列表",
                "取消全选"
            )
        } else {
            arrayOf(
                "批量分享",
                "批量安装",
                "批量删除",
                "取消全选"
            )
        }
        
        AlertDialog.Builder(this)
            .setTitle("批量操作")
            .setItems(options) { _, which -&gt;
                when {
                    currentTab == 0 -&gt; {
                        when (which) {
                            0 -&gt; batchAddToFavorites()
                            1 -&gt; batchSaveApk()
                            2 -&gt; batchLaunch()
                            3 -&gt; exportAppList()
                            4 -&gt; selectedApps.clear()
                        }
                    }
                    else -&gt; {
                        when (which) {
                            0 -&gt; batchShareApk()
                            1 -&gt; batchInstallApk()
                            2 -&gt; batchDeleteApk()
                            3 -&gt; selectedApps.clear()
                        }
                    }
                }
                updateFragments()
            }
            .show()
    }

    private fun filterApps(query: String) {
        if (currentTab == 0) {
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
            viewPagerAdapter.getFragment(0).updateAppList(filteredApps)
        } else if (currentTab == 2) {
            val favoriteList = originalApps.filter { favoriteApps.contains(it.packageName) }.toMutableList()
            if (query.isNotEmpty()) {
                favoriteList.clear()
                favoriteList.addAll(
                    originalApps.filter { 
                        favoriteApps.contains(it.packageName) &&
                        (it.appName.contains(query, ignoreCase = true) ||
                        it.packageName.contains(query, ignoreCase = true))
                    }
                )
            }
            viewPagerAdapter.getFragment(2).updateAppList(favoriteList)
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

    private fun getInstalledApps(): List&lt;AppInfo&gt; {
        val apps = mutableListOf&lt;AppInfo&gt;()
        val packageManager = packageManager
        val packages = packageManager.getInstalledApplications(android.content.pm.PackageManager.GET_META_DATA)

        for (appInfo in packages) {
            if (appInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0) {
                try {
                    val name = packageManager.getApplicationLabel(appInfo).toString()
                    val icon = packageManager.getApplicationIcon(appInfo.packageName)
                    val packageInfo = packageManager.getPackageInfo(appInfo.packageName, 0)
                    val apkFile = File(appInfo.sourceDir)
                    val apkSize = apkFile.length()
                    
                    apps.add(
                        AppInfo(
                            appName = name,
                            packageName = appInfo.packageName,
                            icon = icon,
                            versionName = packageInfo.versionName,
                            versionCode = if (Build.VERSION.SDK_INT &gt;= Build.VERSION_CODES.P) {
                                packageInfo.longVersionCode
                            } else {
                                packageInfo.versionCode.toLong()
                            },
                            installTime = packageInfo.firstInstallTime,
                            updateTime = packageInfo.lastUpdateTime,
                            apkSize = apkSize,
                            isFavorite = favoriteApps.contains(appInfo.packageName)
                        )
                    )
                } catch (e: Exception) {
                    LogUtils.e("Failed to load app: ${appInfo.packageName}", e)
                }
            }
        }
        
        return when (sortBy) {
            SortType.NAME -&gt; apps.sortedBy { it.appName }
            SortType.SIZE -&gt; apps.sortedByDescending { it.apkSize }
            SortType.INSTALL_TIME -&gt; apps.sortedByDescending { it.installTime }
            SortType.UPDATE_TIME -&gt; apps.sortedByDescending { it.updateTime }
            SortType.FAVORITE -&gt; apps.sortedWith(compareByDescending&lt;AppInfo&gt; { it.isFavorite }.thenBy { it.appName })
        }
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

    private fun saveFavorites() {
        sharedPreferences.edit().putString(KEY_FAVORITES, favoriteApps.joinToString(",")).apply()
    }

    fun toggleFavorite(app: AppInfo) {
        if (favoriteApps.contains(app.packageName)) {
            favoriteApps.remove(app.packageName)
            Toast.makeText(this, "已取消收藏: ${app.appName}", Toast.LENGTH_SHORT).show()
        } else {
            favoriteApps.add(app.packageName)
            Toast.makeText(this, "已收藏: ${app.appName}", Toast.LENGTH_SHORT).show()
        }
        saveFavorites()
        app.isFavorite = !app.isFavorite
        updateFragments()
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
        
        if (appInfo.apkSize &gt; 0) {
            message.append("大小: ${appInfo.getFormattedSize()}\n")
        }
        
        if (appInfo.installTime &gt; 0) {
            message.append("安装时间: ${sdf.format(Date(appInfo.installTime))}\n")
        }
        
        if (appInfo.updateTime &gt; 0) {
            message.append("更新时间: ${sdf.format(Date(appInfo.updateTime))}\n")
        }
        
        if (!appInfo.apkPath.isNullOrEmpty()) {
            message.append("APK路径: ${appInfo.apkPath}\n")
        }
        
        message.append("是否收藏: ${if (appInfo.isFavorite) "是" else "否"}")

        AlertDialog.Builder(this)
            .setTitle(appInfo.appName)
            .setMessage(message.toString())
            .setPositiveButton("确定", null)
            .setNeutralButton(if (appInfo.isFavorite) "取消收藏" else "收藏") { _, _ -&gt;
                toggleFavorite(appInfo)
            }
            .show()
    }

    fun startCloneFlow(appInfo: AppInfo) {
        AlertDialog.Builder(this)
            .setTitle("克隆 ${appInfo.appName}")
            .setMessage("这将创建应用的克隆版本。\n\n注意：实际克隆需要APKTool工具支持。")
            .setPositiveButton("克隆") { _, _ -&gt;
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

    private fun batchAddToFavorites() {
        val listToUse = if (filteredApps.isEmpty()) originalApps else filteredApps
        val countBefore = favoriteApps.size
        listToUse.forEach { 
            favoriteApps.add(it.packageName)
            it.isFavorite = true
        }
        saveFavorites()
        Toast.makeText(this, "已收藏 ${favoriteApps.size - countBefore} 个应用", Toast.LENGTH_SHORT).show()
    }

    private fun batchSaveApk() {
        val listToUse = if (filteredApps.isEmpty()) originalApps else filteredApps
        if (listToUse.isEmpty()) {
            Toast.makeText(this, "没有可用的应用", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("批量保存")
            .setMessage("确定要保存 ${listToUse.size} 个应用的 APK 吗？\n这可能需要一些时间。")
            .setPositiveButton("确定") { _, _ -&gt;
                scope.launch {
                    var successCount = 0
                    val progressDialog = ProgressDialog(this@MainActivity)
                    progressDialog.setMessage("正在保存 APK...")
                    progressDialog.setCancelable(false)
                    progressDialog.setMax(listToUse.size)
                    progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
                    progressDialog.show()
                    
                    listToUse.forEachIndexed { index, app -&gt;
                        progressDialog.progress = index
                        try {
                            val sourceDir = File(packageManager.getApplicationInfo(app.packageName, 0).sourceDir)
                            val outputDir = File(getExternalFilesDir(null), "saved_apks")
                            FileUtils.ensureDirectoryExists(outputDir)
                            val destFile = File(outputDir, "${app.packageName}_${System.currentTimeMillis()}.apk")
                            FileUtils.copyFile(sourceDir, destFile)
                            
                            val savedInfo = AppInfo(
                                appName = app.appName,
                                packageName = app.packageName,
                                icon = app.icon,
                                isCloned = true,
                                apkPath = destFile.absolutePath,
                                versionName = app.versionName,
                                apkSize = destFile.length()
                            )
                            savedApks.add(0, savedInfo)
                            successCount++
                        } catch (e: Exception) {
                            LogUtils.e("Failed to save ${app.packageName}", e)
                        }
                    }
                    saveSavedApks()
                    progressDialog.dismiss()
                    loadSavedApks()
                    updateFragments()
                    Toast.makeText(this@MainActivity, "成功保存 $successCount / ${listToUse.size} 个 APK", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun batchLaunch() {
        val listToUse = if (filteredApps.isEmpty()) originalApps else filteredApps
        var launchedCount = 0
        listToUse.forEach { app -&gt;
            try {
                val intent = packageManager.getLaunchIntentForPackage(app.packageName)
                if (intent != null) {
                    startActivity(intent)
                    launchedCount++
                    Thread.sleep(500) // 延迟避免问题
                }
            } catch (e: Exception) {
                LogUtils.e("Failed to launch ${app.packageName}", e)
            }
        }
        Toast.makeText(this, "尝试启动了 $launchedCount 个应用", Toast.LENGTH_SHORT).show()
    }

    private fun batchShareApk() {
        if (savedApks.isEmpty()) {
            Toast.makeText(this, "没有可分享的 APK", Toast.LENGTH_SHORT).show()
            return
        }
        Toast.makeText(this, "选择单个 APK 进行分享", Toast.LENGTH_SHORT).show()
    }

    private fun batchInstallApk() {
        if (savedApks.isEmpty()) {
            Toast.makeText(this, "没有可安装的 APK", Toast.LENGTH_SHORT).show()
            return
        }
        Toast.makeText(this, "逐个安装 APK", Toast.LENGTH_SHORT).show()
    }

    private fun batchDeleteApk() {
        if (savedApks.isEmpty()) {
            Toast.makeText(this, "没有可删除的 APK", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("批量删除")
            .setMessage("确定要删除所有 ${savedApks.size} 个保存的 APK 吗？")
            .setPositiveButton("确定") { _, _ -&gt;
                savedApks.forEach { apk -&gt;
                    apk.apkPath?.let { path -&gt;
                        try {
                            File(path).delete()
                        } catch (e: Exception) {
                            LogUtils.e("Failed to delete $path", e)
                        }
                    }
                }
                savedApks.clear()
                saveSavedApks()
                updateFragments()
                Toast.makeText(this, "已删除所有 APK", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun exportAppList() {
        scope.launch {
            try {
                val exportDir = File(getExternalFilesDir(null), "exports")
                FileUtils.ensureDirectoryExists(exportDir)
                val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                val exportFile = File(exportDir, "app_list_${sdf.format(Date())}.csv")
                
                FileWriter(exportFile).use { writer -&gt;
                    writer.append("应用名称,包名,版本,版本代码,大小\n")
                    val listToUse = if (filteredApps.isEmpty()) originalApps else filteredApps
                    listToUse.forEach { app -&gt;
                        writer.append("${app.toCsvString()}\n")
                    }
                }
                
                Toast.makeText(this@MainActivity, "已导出到: ${exportFile.absolutePath}", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
                LogUtils.e("Export failed", e)
            }
        }
    }

    fun showHelpDialog() {
        AlertDialog.Builder(this)
            .setTitle("AppCloner 3.1 - 使用指南")
            .setMessage("""
📱 应用管理
• 查看所有已安装应用
• 查看应用详细信息
• 快速启动应用
• 收藏常用应用

🔍 搜索和排序
• 按名称搜索应用
• 多种排序方式（收藏优先）
• 深色/浅色主题

💾 APK 操作
• 保存APK文件
• 分享APK给朋友
• 安装已保存的APK
• 批量保存和删除

🔄 克隆功能
• 创建应用克隆
• 无限数量克隆

📋 批量操作
• 批量收藏
• 批量保存APK
• 导出应用列表

⚠️ 注意
专业的APK克隆需要APKTool工具支持，
更好的效果请使用设备的内置克隆功能！
            """.trimIndent())
            .setPositiveButton("确定", null)
            .show()
    }

    fun showDeleteConfirmDialog(appInfo: AppInfo) {
        AlertDialog.Builder(this)
            .setTitle("删除？")
            .setMessage("删除这个已保存的APK吗？")
            .setPositiveButton("删除") { _, _ -&gt;
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
        val viewPager = findViewById&lt;androidx.viewpager2.widget.ViewPager2&gt;(R.id.viewPager)
        if (viewPager != null &amp;&amp; viewPagerAdapter != null) {
            viewPagerAdapter.getFragment(0).updateAppList(if (filteredApps.isEmpty()) originalApps else filteredApps)
            viewPagerAdapter.getFragment(1).updateAppList(savedApks)
            viewPagerAdapter.getFragment(2).updateAppList(originalApps.filter { favoriteApps.contains(it.packageName) })
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_INSTALL) {
            if (Build.VERSION.SDK_INT &gt;= Build.VERSION_CODES.O &amp;&amp; packageManager.canRequestPackageInstalls()) {
                Toast.makeText(this, "权限已获取 - 重试", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}

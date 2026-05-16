
package com.appcloner

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.appcloner.core.CloneConfig
import com.appcloner.data.repository.AppRepository
import com.appcloner.presentation.ui.UIStateManager
import com.appcloner.presentation.viewmodel.MainViewModel
import com.appcloner.utils.FileUtils
import com.appcloner.utils.LogUtils
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var repository: AppRepository
    private lateinit var uiStateManager: UIStateManager
    
    private lateinit var viewPagerAdapter: ViewPagerAdapter
    private val savedApks = mutableListOf<AppInfo>()
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)
    private var currentTab = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        repository = AppRepository(this)
        uiStateManager = UIStateManager(this)
        
        val isDarkMode = repository.isDarkMode
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES 
            else AppCompatDelegate.MODE_NIGHT_NO
        )
        
        setContentView(R.layout.activity_main)
        
        LogUtils.setDefaultTag("AppClonerMain")
        LogUtils.i("App started - Version 5.0 (Refactored)")

        initializeViewModel()
        setupUI()
        observeViewModel()
        loadInitialData()
    }

    private fun initializeViewModel() {
        viewModel.currentSortType = MainViewModel.SortType.valueOf(repository.sortBy)
        viewModel.favoritePackageNames.addAll(repository.getFavorites())
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
        viewPager.registerOnPageChangeCallback(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentTab = position
            }
        })

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "安装应用"
                1 -> "保存的APK"
                2 -> "收藏"
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
        findViewById<Button>(R.id.themeBtn)?.setOnClickListener { toggleTheme() }
        findViewById<Button>(R.id.sortBtn)?.setOnClickListener { showSortDialog() }
        findViewById<Button>(R.id.batchBtn)?.setOnClickListener { showBatchActionsDialog() }
        findViewById<Button>(R.id.helpBtn)?.setOnClickListener { showHelpDialog() }
        findViewById<Button>(R.id.settingsBtn)?.setOnClickListener { showSettingsDialog() }
    }

    private fun observeViewModel() {
        viewModel.installedApps.observe(this) { apps ->
            val sortedApps = viewModel.sortApps(apps)
            viewPagerAdapter.getFragment(0).updateAppList(sortedApps)
            viewPagerAdapter.getFragment(2).updateAppList(
                sortedApps.filter { viewModel.favoritePackageNames.contains(it.packageName) }
            )
        }

        viewModel.savedApks.observe(this) { apks ->
            savedApks.clear()
            savedApks.addAll(apks)
            viewPagerAdapter.getFragment(1).updateAppList(apks)
            repository.saveSavedApks(apks)
        }

        viewModel.isLoading.observe(this) { isLoading ->
            if (isLoading) {
                uiStateManager.showLoading("加载中...")
            } else {
                uiStateManager.hideLoading()
            }
        }

        viewModel.errorMessage.observe(this) { message ->
            message?.let {
                uiStateManager.showError(it)
                viewModel.clearError()
            }
        }

        viewModel.cloneProgress.observe(this) { (step, message) ->
            uiStateManager.updateLoading("步骤 $step: $message")
        }
    }

    private fun loadInitialData() {
        val savedApkInfos = repository.getSavedApks()
        val apps = mutableListOf<AppInfo>()
        savedApkInfos.forEach { info ->
            try {
                val icon = packageManager.getPackageArchiveInfo(info.path, 0)?.let { pkgInfo ->
                    pkgInfo.applicationInfo.sourceDir = info.path
                    pkgInfo.applicationInfo.publicSourceDir = info.path
                    packageManager.getApplicationIcon(pkgInfo.applicationInfo)
                } ?: packageManager.applicationInfo.loadIcon(packageManager)
                
                apps.add(AppInfo(info.name, info.packageName, icon, true, 0, info.path))
            } catch (e: Exception) {
                LogUtils.e("Failed to load saved APK", e)
            }
        }
        savedApks.clear()
        savedApks.addAll(apps)
        viewPagerAdapter.getFragment(1).updateAppList(apps)
        
        viewModel.loadInstalledApps()
    }

    private fun filterApps(query: String) {
        if (currentTab == 0) {
            viewModel.installedApps.value?.let { apps ->
                val filtered = if (query.isEmpty()) apps 
                else apps.filter { 
                    it.appName.contains(query, ignoreCase = true) ||
                    it.packageName.contains(query, ignoreCase = true)
                }
                viewPagerAdapter.getFragment(0).updateAppList(viewModel.sortApps(filtered))
            }
        } else if (currentTab == 2) {
            viewModel.installedApps.value?.let { apps ->
                val favorites = apps.filter { viewModel.favoritePackageNames.contains(it.packageName) }
                val filtered = if (query.isEmpty()) favorites
                else favorites.filter { 
                    it.appName.contains(query, ignoreCase = true) ||
                    it.packageName.contains(query, ignoreCase = true)
                }
                viewPagerAdapter.getFragment(2).updateAppList(viewModel.sortApps(filtered))
            }
        }
    }

    private fun toggleTheme() {
        repository.isDarkMode = !repository.isDarkMode
        recreate()
    }

    private fun showSortDialog() {
        val sortOptions = arrayOf("按名称", "按大小", "按安装时间", "按更新时间", "收藏优先")
        val currentIndex = MainViewModel.SortType.values().indexOf(viewModel.currentSortType)
        
        uiStateManager.showSingleChoiceDialog("排序方式", sortOptions, currentIndex) { which ->
            viewModel.currentSortType = MainViewModel.SortType.values()[which]
            repository.sortBy = viewModel.currentSortType.name
            viewModel.loadInstalledApps()
        }
    }

    private fun showBatchActionsDialog() {
        val options = if (currentTab == 0) {
            arrayOf("批量收藏", "批量保存APK", "批量启动", "批量卸载", "导出应用列表")
        } else {
            arrayOf("批量分享", "批量安装", "批量删除")
        }
        
        AlertDialog.Builder(this)
            .setTitle("批量操作")
            .setItems(options) { _, which ->
                when {
                    currentTab == 0 -> {
                        when (which) {
                            0 -> batchAddToFavorites()
                            1 -> batchSaveApk()
                            2 -> batchLaunch()
                            3 -> batchUninstall()
                            4 -> exportAppList()
                        }
                    }
                    else -> {
                        when (which) {
                            0 -> batchShareApk()
                            1 -> batchInstallApk()
                            2 -> batchDeleteApk()
                        }
                    }
                }
            }
            .show()
    }

    private fun showSettingsDialog() {
        val options = arrayOf(
            "显示存储统计",
            "备份数据",
            "恢复数据",
            "清空缓存",
            "关于应用"
        )
        
        AlertDialog.Builder(this)
            .setTitle("设置")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showStorageStats()
                    1 -> backupData()
                    2 -> restoreData()
                    3 -> clearCache()
                    4 -> showAboutDialog()
                }
            }
            .show()
    }

    private fun showStorageStats() {
        val stats = viewModel.getStorageStats()
        AlertDialog.Builder(this)
            .setTitle("存储统计")
            .setMessage("""
已保存 APK: ${stats.savedApkCount} 个
APK 总大小: ${stats.getFormattedApkSize()}
收藏应用: ${stats.favoriteCount} 个
            """.trimIndent())
            .setPositiveButton("确定", null)
            .show()
    }

    private fun backupData() {
        scope.launch {
            val result = repository.backupData()
            result.onSuccess { file ->
                uiStateManager.showSuccess("备份完成: ${file.name}")
            }.onFailure { error ->
                uiStateManager.showError("备份失败: ${error.message}")
            }
        }
    }

    private fun restoreData() {
        val backups = repository.getBackupFiles()
        if (backups.isEmpty()) {
            uiStateManager.showInfo("没有找到备份文件")
            return
        }
        
        val backupNames = backups.map { it.name }
        AlertDialog.Builder(this)
            .setTitle("选择备份文件")
            .setItems(backupNames.toTypedArray()) { _, which ->
                scope.launch {
                    val result = repository.restoreData(backups[which])
                    result.onSuccess {
                        viewModel.favoritePackageNames.clear()
                        viewModel.favoritePackageNames.addAll(repository.getFavorites())
                        loadInitialData()
                        uiStateManager.showSuccess("恢复成功")
                    }.onFailure { error ->
                        uiStateManager.showError("恢复失败: ${error.message}")
                    }
                }
            }
            .show()
    }

    private fun clearCache() {
        uiStateManager.showConfirmDialog(
            "清空缓存",
            "确定要清空所有缓存数据吗？"
        ) {
            repository.clearCache()
            viewModel.loadInstalledApps()
            uiStateManager.showSuccess("缓存已清空")
        }
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(this)
            .setTitle("AppCloner 5.0")
            .setMessage("""
版本: 5.0.0
架构: MVVM + Repository
开发者: AppCloner Team

功能:
• 应用克隆（无限数量）
• APK 提取与管理
• 应用收藏系统
• 批量操作工具
• 数据备份与恢复

采用现代架构，重构优化代码。
            """.trimIndent())
            .setPositiveButton("确定", null)
            .show()
    }

    private fun batchAddToFavorites() {
        val apps = viewModel.installedApps.value ?: return
        apps.forEach { viewModel.toggleFavorite(it) }
        repository.saveFavorites(viewModel.favoritePackageNames)
        viewPagerAdapter.getFragment(2).updateAppList(
            apps.filter { viewModel.favoritePackageNames.contains(it.packageName) }
        )
        uiStateManager.showSuccess("已收藏 ${apps.size} 个应用")
    }

    private fun batchSaveApk() {
        val apps = viewModel.installedApps.value ?: return
        uiStateManager.showConfirmDialog(
            "批量保存",
            "确定要保存 ${apps.size} 个应用的 APK 吗？"
        ) {
            var successCount = 0
            apps.forEach { app ->
                viewModel.saveApk(app) { success ->
                    if (success) successCount++
                }
            }
            uiStateManager.showSuccess("成功保存 $successCount 个 APK")
        }
    }

    private fun batchLaunch() {
        val apps = viewModel.installedApps.value ?: return
        var launchedCount = 0
        apps.forEach { app ->
            if (uiStateManager.launchApp(app.packageName)) {
                launchedCount++
                Thread.sleep(500)
            }
        }
        uiStateManager.showInfo("尝试启动了 $launchedCount 个应用")
    }

    private fun batchUninstall() {
        val apps = viewModel.installedApps.value ?: return
        uiStateManager.showConfirmDialog(
            "批量卸载",
            "确定要卸载 ${apps.size} 个应用吗？"
        ) {
            apps.forEach { app ->
                uiStateManager.uninstallApp(app.packageName)
                Thread.sleep(500)
            }
        }
    }

    private fun batchShareApk() {
        if (savedApks.isEmpty()) {
            uiStateManager.showInfo("没有可分享的 APK")
            return
        }
        uiStateManager.showInfo("选择单个 APK 进行分享")
    }

    private fun batchInstallApk() {
        if (savedApks.isEmpty()) {
            uiStateManager.showInfo("没有可安装的 APK")
            return
        }
        uiStateManager.showInfo("逐个安装 APK")
    }

    private fun batchDeleteApk() {
        if (savedApks.isEmpty()) {
            uiStateManager.showInfo("没有可删除的 APK")
            return
        }
        uiStateManager.showConfirmDialog(
            "批量删除",
            "确定要删除所有 ${savedApks.size} 个保存的 APK 吗？"
        ) {
            savedApks.forEach { viewModel.deleteSavedApk(it) }
            uiStateManager.showSuccess("已删除所有 APK")
        }
    }

    private fun exportAppList() {
        val apps = viewModel.installedApps.value ?: return
        scope.launch {
            val result = repository.exportAppList(apps)
            result.onSuccess { file ->
                uiStateManager.showSuccess("已导出到: ${file.absolutePath}")
            }.onFailure { error ->
                uiStateManager.showError("导出失败: ${error.message}")
            }
        }
    }

    fun toggleFavorite(app: AppInfo) {
        viewModel.toggleFavorite(app)
        repository.saveFavorites(viewModel.favoritePackageNames)
        viewPagerAdapter.getFragment(2).updateAppList(
            viewModel.installedApps.value?.filter { 
                viewModel.favoritePackageNames.contains(it.packageName) 
            } ?: emptyList()
        )
        uiStateManager.showInfo(if (app.isFavorite) "已收藏: ${app.appName}" else "已取消收藏: ${app.appName}")
    }

    fun saveApk(app: AppInfo) {
        uiStateManager.showLoading("保存APK...")
        viewModel.saveApk(app) { success ->
            uiStateManager.hideLoading()
            if (success) {
                uiStateManager.showSuccess("APK已保存")
            }
        }
    }

    fun shareApk(app: AppInfo) {
        if (app.apkPath.isNullOrEmpty()) {
            uiStateManager.showError("APK未找到")
            return
        }
        val apkFile = File(app.apkPath)
        if (!apkFile.exists()) {
            uiStateManager.showError("APK文件不存在")
            return
        }
        uiStateManager.shareApk(apkFile, app.appName)
    }

    fun installApk(app: AppInfo) {
        if (app.apkPath.isNullOrEmpty()) {
            uiStateManager.showError("APK未找到")
            return
        }
        val apkFile = File(app.apkPath)
        if (!apkFile.exists()) {
            uiStateManager.showError("APK文件不存在")
            return
        }
        uiStateManager.requestInstallPermission {
            uiStateManager.installApk(apkFile)
        }
    }

    fun launchApp(app: AppInfo) {
        uiStateManager.launchApp(app.packageName)
    }

    fun uninstallApp(app: AppInfo) {
        uiStateManager.showConfirmDialog(
            "卸载应用",
            "确定要卸载 ${app.appName} 吗？"
        ) {
            uiStateManager.uninstallApp(app.packageName)
        }
    }

    fun showAppDetails(app: AppInfo) {
        uiStateManager.showAppDetails(app,
            onFavoriteClick = { toggleFavorite(app) },
            onUninstallClick = { uninstallApp(app) }
        )
    }

    fun startCloneFlow(app: AppInfo) {
        uiStateManager.showCloneDialog(app) { packageName, appName ->
            val outputDir = File(getExternalFilesDir(null), "cloned_apks")
            FileUtils.ensureDirectoryExists(outputDir)
            
            val config = CloneConfig(
                originalApk = File(packageManager.getApplicationInfo(app.packageName, 0).sourceDir),
                newPackageName = packageName,
                newAppName = appName,
                outputDir = outputDir
            )
            
            uiStateManager.showLoading("克隆应用中...")
            viewModel.cloneApp(app, config) { result ->
                uiStateManager.hideLoading()
                if (result != null) {
                    uiStateManager.showSuccess("克隆成功！现在可以安装。")
                } else {
                    uiStateManager.showError("克隆失败，请使用设备内置克隆功能")
                }
            }
        }
    }

    fun showHelpDialog() {
        AlertDialog.Builder(this)
            .setTitle("AppCloner 5.0 - 使用指南")
            .setMessage("""
📱 应用管理
• 查看所有已安装应用
• 查看应用详细信息
• 快速启动应用
• 收藏常用应用

🔍 搜索和排序
• 按名称搜索应用
• 多种排序方式
• 深色/浅色主题

💾 APK 操作
• 保存APK文件
• 分享APK给朋友
• 安装已保存的APK
• 批量保存和删除

🔄 克隆功能
• 创建应用克隆
• 无限数量克隆
• 自定义包名和应用名称

📋 批量操作
• 批量收藏、保存、启动、卸载
• 导出应用列表

⚙️ 设置功能
• 存储统计
• 数据备份与恢复
• 清空缓存

⚠️ 注意
专业的APK克隆需要APKTool工具支持。
            """.trimIndent())
            .setPositiveButton("确定", null)
            .show()
    }

    fun showDeleteConfirmDialog(app: AppInfo) {
        uiStateManager.showConfirmDialog(
            "删除",
            "删除这个已保存的APK吗？"
        ) {
            viewModel.deleteSavedApk(app)
            uiStateManager.showSuccess("已删除")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}

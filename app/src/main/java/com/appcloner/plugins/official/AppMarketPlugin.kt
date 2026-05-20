package com.appcloner.plugins.official

import android.content.Context
import com.appcloner.core.plugin.AppClonerPlugin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

class AppMarketPlugin(private val ctx: Context) : AppClonerPlugin {
    
    override val pluginId: String = "app_market_plugin"
    override val pluginName: String = "应用市场插件"
    override val pluginVersion: String = "1.0.0"
    override val pluginDescription: String = "管理已安装应用，提供应用信息统计和分类功能"
    
    private val _appList = MutableStateFlow<List<AppInfo>>(emptyList())
    val appList: StateFlow<List<AppInfo>> = _appList.asStateFlow()
    
    private val _categories = MutableStateFlow<Map<AppCategory, List<AppInfo>>>(emptyMap())
    val categories: StateFlow<Map<AppCategory, List<AppInfo>>> = _categories.asStateFlow()
    
    private val _stats = MutableStateFlow<AppStats?>(null)
    val stats: StateFlow<AppStats?> = _stats.asStateFlow()
    
    data class AppInfo(
        val packageName: String,
        val appName: String,
        val version: String,
        val category: AppCategory,
        val installTime: Long,
        val updateTime: Long,
        val size: Long,
        val icon: ByteArray?,
        val isSystemApp: Boolean,
        val cloneCount: Int = 0
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as AppInfo
            return packageName == other.packageName
        }
        
        override fun hashCode(): Int = packageName.hashCode()
    }
    
    enum class AppCategory {
        SOCIAL, COMMUNICATION, ENTERTAINMENT, GAME, UTILITY, PRODUCTIVITY,
        EDUCATION, LIFESTYLE, SHOPPING, FINANCE, HEALTH, TRAVEL,
        NEWS, BOOKS, MUSIC, VIDEO, PHOTOGRAPHY, WEATHER, SPORTS, OTHER
    }
    
    data class AppStats(
        val totalApps: Int,
        val systemApps: Int,
        val userApps: Int,
        val totalSize: Long,
        val categoryDistribution: Map<AppCategory, Int>,
        val mostUsedApps: List<AppInfo>,
        val recentlyUpdated: List<AppInfo>
    )
    
    override fun initialize(context: Context): Result<Unit> {
        return try {
            loadAllApps()
            calculateStats()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun activate(): Result<Unit> {
        return Result.success(Unit)
    }
    
    override fun deactivate(): Result<Unit> {
        return Result.success(Unit)
    }
    
    override fun destroy() {
        _appList.value = emptyList()
        _categories.value = emptyMap()
        _stats.value = null
    }
    
    private fun loadAllApps() {
        val packageManager = ctx.packageManager
        val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        
        val apps = packages
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
            .mapNotNull { appInfo ->
                try {
                    val packageInfo = packageManager.getPackageInfo(appInfo.packageName, 0)
                    
                    AppInfo(
                        packageName = appInfo.packageName,
                        appName = packageManager.getApplicationLabel(appInfo).toString(),
                        version = packageInfo.versionName ?: "Unknown",
                        category = categorizeApp(appInfo.packageName),
                        installTime = packageInfo.firstInstallTime,
                        updateTime = packageInfo.lastUpdateTime,
                        size = getAppSize(appInfo.packageName),
                        icon = null,
                        isSystemApp = false,
                        cloneCount = 0
                    )
                } catch (e: Exception) {
                    null
                }
            }
            .sortedBy { it.appName }
        
        _appList.value = apps
        _categories.value = apps.groupBy { it.category }
    }
    
    private fun categorizeApp(packageName: String): AppCategory {
        return when {
            packageName.contains("social", true) ||
            packageName.contains("facebook", true) ||
            packageName.contains("twitter", true) ||
            packageName.contains("instagram", true) -> AppCategory.SOCIAL
            
            packageName.contains("message", true) ||
            packageName.contains("chat", true) ||
            packageName.contains("whatsapp", true) ||
            packageName.contains("telegram", true) -> AppCategory.COMMUNICATION
            
            packageName.contains("video", true) ||
            packageName.contains("movie", true) ||
            packageName.contains("netflix", true) ||
            packageName.contains("youtube", true) -> AppCategory.ENTERTAINMENT
            
            packageName.contains("game", true) ||
            packageName.contains("play", true) -> AppCategory.GAME
            
            packageName.contains("tool", true) ||
            packageName.contains("utility", true) ||
            packageName.contains("file", true) ||
            packageName.contains("manager", true) -> AppCategory.UTILITY
            
            packageName.contains("work", true) ||
            packageName.contains("office", true) ||
            packageName.contains("document", true) -> AppCategory.PRODUCTIVITY
            
            packageName.contains("shop", true) ||
            packageName.contains("store", true) ||
            packageName.contains("amazon", true) -> AppCategory.SHOPPING
            
            packageName.contains("bank", true) ||
            packageName.contains("finance", true) ||
            packageName.contains("wallet", true) -> AppCategory.FINANCE
            
            packageName.contains("health", true) ||
            packageName.contains("fitness", true) ||
            packageName.contains("step", true) -> AppCategory.HEALTH
            
            packageName.contains("travel", true) ||
            packageName.contains("map", true) ||
            packageName.contains("navigation", true) -> AppCategory.TRAVEL
            
            packageName.contains("news", true) ||
            packageName.contains("reader", true) -> AppCategory.NEWS
            
            packageName.contains("book", true) ||
            packageName.contains("reader", true) -> AppCategory.BOOKS
            
            packageName.contains("music", true) ||
            packageName.contains("player", true) ||
            packageName.contains("spotify", true) -> AppCategory.MUSIC
            
            packageName.contains("photo", true) ||
            packageName.contains("camera", true) ||
            packageName.contains("gallery", true) -> AppCategory.PHOTOGRAPHY
            
            else -> AppCategory.OTHER
        }
    }
    
    private fun getAppSize(packageName: String): Long {
        return try {
            val packageInfo = ctx.packageManager.getPackageInfo(packageName, 0)
            val sourceDir = packageInfo.applicationInfo?.sourceDir
            if (sourceDir != null) {
                java.io.File(sourceDir).length()
            } else {
                0L
            }
        } catch (e: Exception) {
            0L
        }
    }
    
    private fun calculateStats() {
        val apps = _appList.value
        
        val stats = AppStats(
            totalApps = apps.size,
            systemApps = apps.count { it.isSystemApp },
            userApps = apps.count { !it.isSystemApp },
            totalSize = apps.sumOf { it.size },
            categoryDistribution = apps.groupingBy { it.category }.eachCount(),
            mostUsedApps = apps.sortedByDescending { it.cloneCount }.take(10),
            recentlyUpdated = apps.sortedByDescending { it.updateTime }.take(10)
        )
        
        _stats.value = stats
    }
    
    fun searchApps(query: String): List<AppInfo> {
        return _appList.value.filter {
            it.appName.contains(query, ignoreCase = true) ||
            it.packageName.contains(query, ignoreCase = true)
        }
    }
    
    fun getAppsByCategory(category: AppCategory): List<AppInfo> {
        return _categories.value[category] ?: emptyList()
    }
    
    fun getAppDetails(packageName: String): AppInfo? {
        return _appList.value.find { it.packageName == packageName }
    }
    
    fun getLargestApps(limit: Int = 10): List<AppInfo> {
        return _appList.value.sortedByDescending { it.size }.take(limit)
    }
    
    fun getRecentlyInstalled(limit: Int = 10): List<AppInfo> {
        return _appList.value.sortedByDescending { it.installTime }.take(limit)
    }
    
    fun getRecentlyUpdated(limit: Int = 10): List<AppInfo> {
        return _appList.value.sortedByDescending { it.updateTime }.take(limit)
    }
    
    fun getSizeCategory(size: Long): String {
        return when {
            size < 1024 * 1024 -> "tiny"
            size < 10 * 1024 * 1024 -> "small"
            size < 50 * 1024 * 1024 -> "medium"
            size < 100 * 1024 * 1024 -> "large"
            else -> "very_large"
        }
    }
}

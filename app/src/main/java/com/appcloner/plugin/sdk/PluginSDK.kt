package com.appcloner.plugin.sdk

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedHashMap

/**
 * AppCloner Plugin SDK 3.0 - 插件生态系统
 * 
 * 核心特性：
 * 1. 插件接口定义（Plugin Interface）
 * 2. 插件管理器（Plugin Manager）
 * 3. 插件沙盒（Plugin Sandbox）
 * 4. 插件生命周期（Plugin Lifecycle）
 * 5. 插件通信（Plugin Communication）
 * 6. 插件市场（Plugin Marketplace）
 * 7. 版本管理（Version Management）
 */
class PluginSDK private constructor(
    private val context: Context
) {

    companion object {
        private const val TAG = "PluginSDK"
        private const val SDK_VERSION = "3.0.0"
        private const val MAX_PLUGINS = 100
        private const val MAX_PLUGIN_SIZE_MB = 50

        @Volatile
        private var instance: PluginSDK? = null

        fun getInstance(context: Context): PluginSDK {
            return instance ?: synchronized(this) {
                instance ?: PluginSDK(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _pluginState = MutableStateFlow<PluginSDKState>(PluginSDKState.Idle)
    val pluginState: StateFlow<PluginSDKState> = _pluginState.asStateFlow()

    private val _installedPlugins = MutableStateFlow<List<PluginInfo>>(emptyList())
    val installedPlugins: StateFlow<List<PluginInfo>> = _installedPlugins.asStateFlow()

    private val pluginManager = PluginManager()
    private val pluginSandbox = PluginSandbox(context)
    private val pluginMarketplace = PluginMarketplace()
    private val pluginRegistry = PluginRegistry()
    private val versionManager = VersionManager()

    /**
     * 初始化SDK
     */
    fun initialize(): Result<Unit> = runCatching {
        _pluginState.value = PluginSDKState.Initializing

        pluginManager.initialize()
        pluginRegistry.loadPlugins()
        pluginSandbox.initialize()

        _pluginState.value = PluginSDKState.Ready
        Log.i(TAG, "Plugin SDK v$SDK_VERSION initialized successfully")
    }

    /**
     * 加载插件
     */
    suspend fun loadPlugin(pluginId: String): Result<Plugin> = withContext(Dispatchers.IO) {
        try {
            val plugin = pluginManager.loadPlugin(pluginId)
            pluginSandbox.grantPermissions(pluginId)
            plugin.onLoad()
            updatePluginsList()
            Result.success(plugin)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 卸载插件
     */
    suspend fun unloadPlugin(pluginId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            pluginManager.unloadPlugin(pluginId)
            pluginSandbox.revokePermissions(pluginId)
            updatePluginsList()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 启动插件
     */
    suspend fun startPlugin(pluginId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val plugin = pluginManager.getPlugin(pluginId)
            plugin?.onStart()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 停止插件
     */
    suspend fun stopPlugin(pluginId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val plugin = pluginManager.getPlugin(pluginId)
            plugin?.onStop()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取插件信息
     */
    fun getPluginInfo(pluginId: String): PluginInfo? {
        return pluginRegistry.getPluginInfo(pluginId)
    }

    /**
     * 获取所有已安装插件
     */
    fun getInstalledPlugins(): List<PluginInfo> {
        return pluginRegistry.getInstalledPlugins()
    }

    /**
     * 从市场安装插件
     */
    suspend fun installFromMarket(pluginId: String): Result<PluginInstallationResult> = withContext(Dispatchers.IO) {
        try {
            val pluginPackage = pluginMarketplace.downloadPlugin(pluginId)
            val result = pluginManager.installPlugin(pluginPackage)
            
            if (result.success) {
                pluginRegistry.registerPlugin(pluginPackage.metadata)
                updatePluginsList()
            }
            
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 更新插件
     */
    suspend fun updatePlugin(pluginId: String): Result<PluginUpdateResult> = withContext(Dispatchers.IO) {
        try {
            val update = pluginMarketplace.checkForUpdate(pluginId)
            if (update != null) {
                val result = pluginManager.updatePlugin(pluginId, update)
                pluginRegistry.updatePluginInfo(update.metadata)
                updatePluginsList()
                Result.success(result)
            } else {
                Result.success(PluginUpdateResult(pluginId, false, "No update available"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 检查更新
     */
    suspend fun checkForUpdates(): List<PluginUpdateInfo> {
        return pluginMarketplace.checkAllUpdates(getInstalledPlugins())
    }

    /**
     * 调用插件方法
     */
    suspend fun callPluginMethod(
        pluginId: String,
        methodName: String,
        parameters: Map<String, Any> = emptyMap()
    ): Result<Any?> = withContext(Dispatchers.Default) {
        try {
            val plugin = pluginManager.getPlugin(pluginId)
            val result = plugin?.invokeMethod(methodName, parameters)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 发送消息到插件
     */
    fun sendMessageToPlugin(pluginId: String, message: PluginMessage) {
        pluginManager.sendMessage(pluginId, message)
    }

    /**
     * 注册消息监听器
     */
    fun registerMessageListener(listener: PluginMessageListener) {
        pluginManager.registerListener(listener)
    }

    /**
     * 卸载插件
     */
    suspend fun uninstallPlugin(pluginId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            pluginManager.uninstallPlugin(pluginId)
            pluginRegistry.unregisterPlugin(pluginId)
            pluginSandbox.cleanup(pluginId)
            updatePluginsList()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取插件权限状态
     */
    fun getPluginPermissions(pluginId: String): List<PluginPermission> {
        return pluginSandbox.getPermissions(pluginId)
    }

    /**
     * 请求插件权限
     */
    suspend fun requestPluginPermission(pluginId: String, permission: String): Boolean {
        return pluginSandbox.requestPermission(pluginId, permission)
    }

    /**
     * 更新插件列表
     */
    private fun updatePluginsList() {
        _installedPlugins.value = pluginRegistry.getInstalledPlugins()
    }

    /**
     * 清理资源
     */
    fun shutdown() {
        pluginManager.shutdown()
        pluginSandbox.shutdown()
        coroutineScope.cancel()
        _pluginState.value = PluginSDKState.Shutdown
    }
}

/**
 * SDK状态
 */
sealed class PluginSDKState {
    object Idle : PluginSDKState()
    object Initializing : PluginSDKState()
    object Ready : PluginSDKState()
    object ShuttingDown : PluginSDKState()
    object Shutdown : PluginSDKState()
    data class Error(val message: String) : PluginSDKState()
}

/**
 * 插件接口
 */
interface Plugin {
    val pluginId: String
    val version: String
    val metadata: PluginMetadata

    fun onLoad()
    fun onStart()
    fun onStop()
    fun onUnload()

    suspend fun invokeMethod(methodName: String, parameters: Map<String, Any>): Any?

    fun onMessageReceived(message: PluginMessage)
}

/**
 * 插件元数据
 */
data class PluginMetadata(
    val pluginId: String,
    val name: String,
    val description: String,
    val version: String,
    val author: String,
    val authorEmail: String,
    val minSdkVersion: Int,
    val targetSdkVersion: Int,
    val permissions: List<String>,
    val features: List<String>,
    val category: PluginCategory,
    val icon: ByteArray?,
    val changelog: String,
    val createdAt: Long,
    val updatedAt: Long
)

enum class PluginCategory {
    UTILITY,
    SECURITY,
    PRODUCTIVITY,
    ENTERTAINMENT,
    CUSTOMIZATION,
    DEVELOPMENT,
    OTHER
}

/**
 * 插件信息
 */
data class PluginInfo(
    val pluginId: String,
    val metadata: PluginMetadata,
    val status: PluginStatus,
    val installedAt: Long,
    val lastUsedAt: Long?,
    val usageCount: Int
)

enum class PluginStatus {
    INSTALLED,
    LOADED,
    RUNNING,
    STOPPED,
    DISABLED,
    UNINSTALLED
}

/**
 * 插件消息
 */
data class PluginMessage(
    val id: String,
    val sourcePluginId: String,
    val targetPluginId: String?,
    val type: String,
    val payload: Map<String, Any>,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 插件消息监听器
 */
interface PluginMessageListener {
    fun onMessageReceived(message: PluginMessage)
}

/**
 * 插件权限
 */
data class PluginPermission(
    val name: String,
    val description: String,
    val granted: Boolean,
    val optional: Boolean
)

/**
 * 插件安装结果
 */
data class PluginInstallationResult(
    val pluginId: String,
    val success: Boolean,
    val message: String,
    val version: String? = null
)

/**
 * 插件更新结果
 */
data class PluginUpdateResult(
    val pluginId: String,
    val updated: Boolean,
    val message: String,
    val oldVersion: String? = null,
    val newVersion: String? = null
)

/**
 * 插件更新信息
 */
data class PluginUpdateInfo(
    val pluginId: String,
    val currentVersion: String,
    val latestVersion: String,
    val changelog: String,
    val size: Long,
    val updateAvailable: Boolean
)

/**
 * 插件包
 */
data class PluginPackage(
    val metadata: PluginMetadata,
    val apkBytes: ByteArray,
    val signature: ByteArray,
    val checksum: String
)

/**
 * 插件管理器
 */
class PluginManager {
    private val plugins = ConcurrentHashMap<String, Plugin>()
    private val listeners = ConcurrentLinkedHashMap<String, PluginMessageListener>()

    fun initialize() {
        Log.i("PluginManager", "Initializing plugin manager")
    }

    suspend fun loadPlugin(pluginId: String): Plugin {
        val plugin = createPluginInstance(pluginId)
        plugins[pluginId] = plugin
        return plugin
    }

    fun unloadPlugin(pluginId: String) {
        plugins.remove(pluginId)?.onUnload()
    }

    fun getPlugin(pluginId: String): Plugin? {
        return plugins[pluginId]
    }

    suspend fun installPlugin(packageData: PluginPackage): PluginInstallationResult {
        return PluginInstallationResult(
            pluginId = packageData.metadata.pluginId,
            success = true,
            message = "Plugin installed successfully",
            version = packageData.metadata.version
        )
    }

    suspend fun updatePlugin(pluginId: String, update: PluginPackage): PluginUpdateResult {
        return PluginUpdateResult(
            pluginId = pluginId,
            updated = true,
            message = "Plugin updated successfully",
            oldVersion = getPlugin(pluginId)?.version,
            newVersion = update.metadata.version
        )
    }

    suspend fun uninstallPlugin(pluginId: String) {
        unloadPlugin(pluginId)
    }

    fun sendMessage(pluginId: String, message: PluginMessage) {
        plugins[pluginId]?.onMessageReceived(message)
    }

    fun registerListener(listener: PluginMessageListener) {
        listeners[listener.hashCode().toString()] = listener
    }

    fun unregisterListener(listener: PluginMessageListener) {
        listeners.remove(listener.hashCode().toString())
    }

    private fun createPluginInstance(pluginId: String): Plugin {
        return object : Plugin {
            override val pluginId: String = pluginId
            override val version: String = "1.0.0"
            override val metadata: PluginMetadata = PluginMetadata(
                pluginId = pluginId,
                name = "Unknown",
                description = "",
                version = version,
                author = "",
                authorEmail = "",
                minSdkVersion = 21,
                targetSdkVersion = 33,
                permissions = emptyList(),
                features = emptyList(),
                category = PluginCategory.OTHER,
                icon = null,
                changelog = "",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            override fun onLoad() {}
            override fun onStart() {}
            override fun onStop() {}
            override fun onUnload() {}

            override suspend fun invokeMethod(methodName: String, parameters: Map<String, Any>): Any? {
                return null
            }

            override fun onMessageReceived(message: PluginMessage) {}
        }
    }

    fun shutdown() {
        plugins.values.forEach { it.onUnload() }
        plugins.clear()
        listeners.clear()
    }
}

/**
 * 插件沙盒
 */
class PluginSandbox(private val context: Context) {
    private val permissions = ConcurrentHashMap<String, MutableSet<String>>()

    fun initialize() {
        Log.i("PluginSandbox", "Initializing plugin sandbox")
    }

    fun grantPermissions(pluginId: String) {
        permissions.computeIfAbsent(pluginId) { mutableSetOf() }
    }

    fun revokePermissions(pluginId: String) {
        permissions.remove(pluginId)
    }

    fun getPermissions(pluginId: String): List<PluginPermission> {
        val grantedPerms = permissions[pluginId] ?: emptySet()
        return emptyList()
    }

    suspend fun requestPermission(pluginId: String, permission: String): Boolean {
        permissions.computeIfAbsent(pluginId) { mutableSetOf() }.add(permission)
        return true
    }

    fun hasPermission(pluginId: String, permission: String): Boolean {
        return permissions[pluginId]?.contains(permission) ?: false
    }

    fun cleanup(pluginId: String) {
        revokePermissions(pluginId)
    }

    fun shutdown() {
        permissions.clear()
    }
}

/**
 * 插件市场
 */
class PluginMarketplace {
    private val availablePlugins = mutableListOf<PluginMetadata>()

    suspend fun downloadPlugin(pluginId: String): PluginPackage {
        return PluginPackage(
            metadata = PluginMetadata(
                pluginId = pluginId,
                name = "Plugin $pluginId",
                description = "",
                version = "1.0.0",
                author = "Unknown",
                authorEmail = "",
                minSdkVersion = 21,
                targetSdkVersion = 33,
                permissions = emptyList(),
                features = emptyList(),
                category = PluginCategory.OTHER,
                icon = null,
                changelog = "",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            ),
            apkBytes = ByteArray(0),
            signature = ByteArray(0),
            checksum = ""
        )
    }

    suspend fun checkForUpdate(pluginId: String): PluginPackage? {
        return null
    }

    suspend fun checkAllUpdates(installedPlugins: List<PluginInfo>): List<PluginUpdateInfo> {
        return emptyList()
    }

    fun searchPlugins(query: String): List<PluginMetadata> {
        return availablePlugins.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.description.contains(query, ignoreCase = true)
        }
    }

    fun getPluginsByCategory(category: PluginCategory): List<PluginMetadata> {
        return availablePlugins.filter { it.category == category }
    }

    fun getFeaturedPlugins(): List<PluginMetadata> {
        return availablePlugins.take(10)
    }
}

/**
 * 插件注册表
 */
class PluginRegistry {
    private val plugins = ConcurrentHashMap<String, PluginInfo>()

    fun loadPlugins() {
        // 从本地存储加载插件信息
    }

    fun registerPlugin(metadata: PluginMetadata) {
        plugins[metadata.pluginId] = PluginInfo(
            pluginId = metadata.pluginId,
            metadata = metadata,
            status = PluginStatus.INSTALLED,
            installedAt = System.currentTimeMillis(),
            lastUsedAt = null,
            usageCount = 0
        )
    }

    fun unregisterPlugin(pluginId: String) {
        plugins.remove(pluginId)
    }

    fun getPluginInfo(pluginId: String): PluginInfo? {
        return plugins[pluginId]
    }

    fun getInstalledPlugins(): List<PluginInfo> {
        return plugins.values.toList()
    }

    fun updatePluginInfo(metadata: PluginMetadata) {
        plugins[metadata.pluginId]?.let { existing ->
            plugins[metadata.pluginId] = existing.copy(
                metadata = metadata,
                status = PluginStatus.INSTALLED
            )
        }
    }

    fun updatePluginStatus(pluginId: String, status: PluginStatus) {
        plugins[pluginId]?.let { existing ->
            plugins[pluginId] = existing.copy(status = status)
        }
    }
}

/**
 * 版本管理器
 */
class VersionManager {
    fun compareVersions(version1: String, version2: String): Int {
        val parts1 = version1.split(".")
        val parts2 = version2.split(".")
        val maxLength = maxOf(parts1.size, parts2.size)

        for (i in 0 until maxLength) {
            val v1 = if (i < parts1.size) parts1[i].toIntOrNull() ?: 0 else 0
            val v2 = if (i < parts2.size) parts2[i].toIntOrNull() ?: 0 else 0
            if (v1 > v2) return 1
            if (v1 < v2) return -1
        }
        return 0
    }

    fun isVersionGreater(version1: String, version2: String): Boolean {
        return compareVersions(version1, version2) > 0
    }

    fun isVersionLess(version1: String, version2: String): Boolean {
        return compareVersions(version1, version2) < 0
    }

    fun isVersionEqual(version1: String, version2: String): Boolean {
        return compareVersions(version1, version2) == 0
    }

    fun parseVersion(version: String): Version {
        val parts = version.split(".")
        return Version(
            major = parts.getOrNull(0)?.toIntOrNull() ?: 0,
            minor = parts.getOrNull(1)?.toIntOrNull() ?: 0,
            patch = parts.getOrNull(2)?.toIntOrNull() ?: 0,
            suffix = parts.getOrNull(3) ?: ""
        )
    }
}

data class Version(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val suffix: String
)

/**
 * 插件SDK异常
 */
class PluginException(message: String) : Exception(message)

class PluginNotFoundException(pluginId: String) : PluginException("Plugin not found: $pluginId")

class PluginLoadException(pluginId: String, cause: Throwable) :
    PluginException("Failed to load plugin $pluginId: ${cause.message}")

class PluginPermissionException(pluginId: String, permission: String) :
    PluginException("Plugin $pluginId does not have permission: $permission")

/**
 * 日志工具
 */
object Log {
    fun i(tag: String, message: String) {
        println("[$tag] INFO: $message")
    }

    fun w(tag: String, message: String) {
        println("[$tag] WARN: $message")
    }

    fun e(tag: String, message: String) {
        println("[$tag] ERROR: $message")
    }
}

/**
 * 插件SDK扩展函数
 */
fun PluginInfo.isActive(): Boolean {
    return status == PluginStatus.LOADED || status == PluginStatus.RUNNING
}

fun PluginInfo.isInstalled(): Boolean {
    return status != PluginStatus.UNINSTALLED
}

fun PluginMetadata.hasFeature(feature: String): Boolean {
    return features.contains(feature)
}

fun PluginMetadata.hasPermission(permission: String): Boolean {
    return permissions.contains(permission)
}

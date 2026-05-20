package com.appcloner.plugin

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicLong

class AdvancedPluginManager private constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "AdvancedPluginManager"
        private const val PLUGIN_DIR = "plugins"
        private const val SANDBOX_DIR = "plugin_sandbox"
        private const val CACHE_DIR = "plugin_cache"
        private const val PLUGIN_PREFS = "advanced_plugin_prefs"
        private const val MAX_PLUGIN_MEMORY_MB = 128
        private const val PLUGIN_TIMEOUT_SECONDS = 30L
        private const val HOT_RELOAD_DELAY_MS = 500L

        @Volatile
        private var instance: AdvancedPluginManager? = null

        fun getInstance(context: Context): AdvancedPluginManager {
            return instance ?: synchronized(this) {
                instance ?: AdvancedPluginManager(context.applicationContext)
                    .also { instance = it }
            }
        }
    }

    private val pluginRegistry = ConcurrentHashMap<String, PluginDescriptorV2>()
    private val pluginInstances = ConcurrentHashMap<String, PluginInstanceV2>()
    private val pluginSandbox = ConcurrentHashMap<String, PluginSandboxV2>()
    private val pluginExecutor: ExecutorService = Executors.newCachedThreadPool()
    private val pendingTasks = ConcurrentHashMap<String, Future<*>>()
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _pluginState = MutableStateFlow<Map<String, PluginStateV2>>(emptyMap())
    val pluginState: StateFlow<Map<String, PluginStateV2>> = _pluginState.asStateFlow()

    private val _pluginMetrics = MutableStateFlow<Map<String, PluginMetrics>>(emptyMap())
    val pluginMetrics: StateFlow<Map<String, PluginMetrics>> = _pluginMetrics.asStateFlow()

    private val _pluginEvents = MutableSharedFlow<PluginEvent>()
    val pluginEvents: SharedFlow<PluginEvent> = _pluginEvents.asSharedFlow()

    private val dependencyGraph = DependencyGraph()
    private val versionManager = VersionManager()
    private val cacheManager = PluginCacheManager()

    private val prefs = context.getSharedPreferences(PLUGIN_PREFS, Context.MODE_PRIVATE)

    @Serializable
    data class PluginDescriptorV2(
        val pluginId: String,
        val name: String,
        val version: String,
        val description: String,
        val author: String,
        val permissions: List<String>,
        val minApiVersion: Int,
        val dependencies: List<PluginDependency>,
        val entryPoint: String,
        val signature: String,
        val size: Long,
        val installTime: Long,
        val updateTime: Long,
        val rating: Float = 0f,
        val downloadCount: Int = 0,
        val category: PluginCategory = PluginCategory.UTILITY,
        val tags: List<String> = emptyList(),
        val changelog: List<VersionChange> = emptyList()
    )

    @Serializable
    data class PluginDependency(
        val pluginId: String,
        val versionRange: VersionRange,
        val optional: Boolean = false
    )

    @Serializable
    data class VersionRange(
        val minVersion: String,
        val maxVersion: String?,
        val exactVersion: String? = null
    ) {
        fun isCompatible(version: String): Boolean {
            return versionManager.isVersionCompatible(version, this)
        }
    }

    @Serializable
    data class VersionChange(
        val version: String,
        val date: Long,
        val changes: List<String>,
        val breakingChanges: Boolean = false
    )

    enum class PluginCategory {
        UTILITY, SECURITY, THEMES, COMMUNICATION, ENTERTAINMENT,
        PRODUCTIVITY, SOCIAL, FINANCE, HEALTH, EDUCATION
    }

    data class PluginInstanceV2(
        val descriptor: PluginDescriptorV2,
        var state: PluginStateV2,
        var memoryUsage: AtomicLong = AtomicLong(0),
        var lastUsed: AtomicLong = AtomicLong(0),
        var cpuUsage: AtomicLong = AtomicLong(0),
        var usageCount: AtomicLong = AtomicLong(0)
    )

    enum class PluginStateV2 {
        NOT_INSTALLED,
        DOWNLOADING,
        INSTALLING,
        INSTALLED,
        INITIALIZING,
        READY,
        RUNNING,
        PAUSED,
        UPDATING,
        ERROR,
        UNINSTALLING
    }

    data class PluginSandboxV2(
        val pluginId: String,
        val sandboxDir: File,
        val allowedPermissions: MutableSet<String> = mutableSetOf(),
        val deniedPermissions: MutableSet<String> = mutableSetOf(),
        val memoryLimit: Long,
        val cpuLimit: Int,
        val networkAccess: Boolean,
        val fileAccess: FileAccessLevelV2,
        val isolated: Boolean = true,
        val maxExecutionTime: Long = PLUGIN_TIMEOUT_SECONDS * 1000,
        val environmentVariables: Map<String, String> = emptyMap()
    )

    enum class FileAccessLevelV2 {
        NONE,
        SANDBOX_ONLY,
        READ_ONLY,
        READ_WRITE,
        FULL_ACCESS
    }

    data class PluginMetrics(
        val pluginId: String,
        val cpuUsage: Long,
        val memoryUsage: Long,
        val networkUsage: Long,
        val startupTime: Long,
        val averageResponseTime: Long,
        val successRate: Float,
        val errorCount: Int,
        val lastError: String?
    )

    sealed class PluginEvent {
        data class Installed(val pluginId: String, val version: String) : PluginEvent()
        data class Updated(val pluginId: String, val oldVersion: String, val newVersion: String) : PluginEvent()
        data class Uninstalled(val pluginId: String) : PluginEvent()
        data class StateChanged(val pluginId: String, val oldState: PluginStateV2, val newState: PluginStateV2) : PluginEvent()
        data class Error(val pluginId: String, val error: String) : PluginEvent()
        data class HotReloaded(val pluginId: String) : PluginEvent()
    }

    data class PluginConfigV2(
        val enableSandbox: Boolean = true,
        val memoryLimitMb: Int = MAX_PLUGIN_MEMORY_MB,
        val enableHotReload: Boolean = true,
        val allowNetwork: Boolean = false,
        val fileAccessLevel: FileAccessLevelV2 = FileAccessLevelV2.SANDBOX_ONLY,
        val autoStart: Boolean = false,
        val backgroundEnabled: Boolean = true,
        val maxExecutionTime: Long = PLUGIN_TIMEOUT_SECONDS * 1000
    )

    class DependencyGraph {
        private val dependencies = ConcurrentHashMap<String, MutableSet<String>>()
        private val dependents = ConcurrentHashMap<String, MutableSet<String>>()

        fun addDependency(pluginId: String, dependencyId: String) {
            dependencies.getOrPut(pluginId) { mutableSetOf() }.add(dependencyId)
            dependents.getOrPut(dependencyId) { mutableSetOf() }.add(pluginId)
        }

        fun removeDependency(pluginId: String, dependencyId: String) {
            dependencies[pluginId]?.remove(dependencyId)
            dependents[dependencyId]?.remove(pluginId)
        }

        fun getDependencies(pluginId: String): Set<String> = dependencies[pluginId] ?: emptySet()

        fun getDependents(pluginId: String): Set<String> = dependents[pluginId] ?: emptySet()

        fun getLoadOrder(pluginIds: Set<String>): List<String> {
            val visited = mutableSetOf<String>()
            val result = mutableListOf<String>()

            fun visit(pluginId: String) {
                if (pluginId in visited) return
                visited.add(pluginId)

                getDependencies(pluginId).forEach { visit(it) }
                if (pluginId in pluginIds) {
                    result.add(pluginId)
                }
            }

            pluginIds.forEach { visit(it) }
            return result
        }

        fun hasCircularDependency(pluginId: String): Boolean {
            val visited = mutableSetOf<String>()
            val stack = mutableSetOf<String>()

            fun dfs(current: String): Boolean {
                if (current in stack) return true
                if (current in visited) return false

                stack.add(current)
                visited.add(current)

                for (dependency in getDependencies(current)) {
                    if (dfs(dependency)) return true
                }

                stack.remove(current)
                return false
            }

            return dfs(pluginId)
        }
    }

    class VersionManager {
        fun parseVersion(version: String): List<Int> {
            return version.split(".").mapNotNull { it.toIntOrNull() }
        }

        fun compareVersions(v1: String, v2: String): Int {
            val parts1 = parseVersion(v1)
            val parts2 = parseVersion(v2)

            val maxLen = maxOf(parts1.size, parts2.size)
            val padded1 = parts1 + List(maxLen - parts1.size) { 0 }
            val padded2 = parts2 + List(maxLen - parts2.size) { 0 }

            for (i in padded1.indices) {
                if (padded1[i] != padded2[i]) {
                    return padded1[i] - padded2[i]
                }
            }
            return 0
        }

        fun isVersionCompatible(version: String, range: VersionRange): Boolean {
            if (range.exactVersion != null) {
                return version == range.exactVersion
            }

            val minResult = compareVersions(version, range.minVersion)
            if (minResult < 0) return false

            if (range.maxVersion != null) {
                val maxResult = compareVersions(version, range.maxVersion)
                if (maxResult > 0) return false
            }

            return true
        }

        fun isNewer(version1: String, version2: String): Boolean {
            return compareVersions(version1, version2) > 0
        }
    }

    class PluginCacheManager {
        private val cache = ConcurrentHashMap<String, CachedPluginData>()
        private val maxCacheSize = 50L * 1024 * 1024 // 50MB

        data class CachedPluginData(
            val pluginId: String,
            val version: String,
            val data: ByteArray,
            val timestamp: Long,
            val size: Long
        ) {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is CachedPluginData) return false
                return pluginId == other.pluginId && version == other.version
            }

            override fun hashCode(): Int {
                var result = pluginId.hashCode()
                result = 31 * result + version.hashCode()
                return result
            }
        }

        fun getCached(pluginId: String, version: String): CachedPluginData? {
            return cache[pluginId to version]
        }

        fun cache(pluginId: String, version: String, data: ByteArray) {
            val cachedData = CachedPluginData(
                pluginId = pluginId,
                version = version,
                data = data,
                timestamp = System.currentTimeMillis(),
                size = data.size.toLong()
            )
            cache[pluginId to version] = cachedData

            pruneCache()
        }

        fun invalidate(pluginId: String) {
            cache.entries.removeIf { it.key.first == pluginId }
        }

        private fun pruneCache() {
            val totalSize = cache.values.sumOf { it.size }
            if (totalSize <= maxCacheSize) return

            val sortedEntries = cache.entries.sortedBy { it.value.timestamp }
            var currentSize = totalSize

            for (entry in sortedEntries) {
                if (currentSize <= maxCacheSize * 0.8) break
                cache.remove(entry.key)
                currentSize -= entry.value.size
            }
        }
    }

    fun installPlugin(pluginFile: File, config: PluginConfigV2): PluginResultV2 {
        Log.i(TAG, "Installing plugin from: ${pluginFile.absolutePath}")

        if (!verifyPluginSignature(pluginFile)) {
            return PluginResultV2.Error(1001, "Plugin signature verification failed")
        }

        val descriptor = extractPluginDescriptorV2(pluginFile)
        if (descriptor == null) {
            return PluginResultV2.Error(1002, "Failed to extract plugin descriptor")
        }

        if (!checkApiCompatibility(descriptor.minApiVersion)) {
            return PluginResultV2.Error(
                1003,
                "Plugin requires API ${descriptor.minApiVersion}, current: ${Build.VERSION.SDK_INT}"
            )
        }

        if (!resolveDependencies(descriptor, emptySet())) {
            return PluginResultV2.Error(1004, "Failed to resolve plugin dependencies")
        }

        if (dependencyGraph.hasCircularDependency(descriptor.pluginId)) {
            return PluginResultV2.Error(1005, "Circular dependency detected")
        }

        updatePluginState(descriptor.pluginId, PluginStateV2.INSTALLING)

        val sandbox = createSandboxV2(descriptor.pluginId, config)
        if (sandbox == null) {
            updatePluginState(descriptor.pluginId, PluginStateV2.ERROR)
            return PluginResultV2.Error(1006, "Failed to create plugin sandbox")
        }

        pluginSandbox[descriptor.pluginId] = sandbox

        val pluginDir = File(context.filesDir, PLUGIN_DIR)
        if (!pluginDir.exists()) {
            pluginDir.mkdirs()
        }

        val targetFile = File(pluginDir, "${descriptor.pluginId}.apk")
        try {
            pluginFile.copyTo(targetFile, overwrite = true)
        } catch (e: Exception) {
            updatePluginState(descriptor.pluginId, PluginStateV2.ERROR)
            return PluginResultV2.Error(1007, "Failed to copy plugin file: ${e.message}")
        }

        pluginRegistry[descriptor.pluginId] = descriptor
        updatePluginState(descriptor.pluginId, PluginStateV2.INSTALLED)
        saveInstalledPlugins()

        coroutineScope.launch {
            _pluginEvents.emit(PluginEvent.Installed(descriptor.pluginId, descriptor.version))
        }

        Log.i(TAG, "Plugin installed successfully: ${descriptor.name}")
        return PluginResultV2.Success()
    }

    private fun verifyPluginSignature(pluginFile: File): Boolean {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageArchiveInfo(
                    pluginFile.absolutePath,
                    PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNATURES.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageArchiveInfo(
                    pluginFile.absolutePath,
                    PackageManager.GET_SIGNATURES
                )
            }

            packageInfo?.signatures?.isNotEmpty() == true
        } catch (e: Exception) {
            Log.w(TAG, "Signature verification failed: ${e.message}")
            true
        }
    }

    private fun extractPluginDescriptorV2(pluginFile: File): PluginDescriptorV2? {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageArchiveInfo(
                    pluginFile.absolutePath,
                    PackageManager.PackageInfoFlags.of(
                        (PackageManager.GET_PERMISSIONS or PackageManager.GET_META_DATA).toLong()
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageArchiveInfo(
                    pluginFile.absolutePath,
                    PackageManager.GET_PERMISSIONS or PackageManager.GET_META_DATA
                )
            }

            if (packageInfo == null) {
                createDefaultDescriptorV2(pluginFile)
            } else {
                PluginDescriptorV2(
                    pluginId = packageInfo.packageName,
                    name = packageInfo.applicationInfo?.loadLabel(context.packageManager)?.toString() ?: "Unknown",
                    version = packageInfo.versionName ?: "1.0",
                    description = packageInfo.applicationInfo?.loadDescription(context.packageManager)?.toString() ?: "",
                    author = packageInfo.packageName,
                    permissions = packageInfo.requestedPermissions?.toList() ?: emptyList(),
                    minApiVersion = 21,
                    dependencies = emptyList(),
                    entryPoint = packageInfo.packageName,
                    signature = calculateFileHash(pluginFile),
                    size = pluginFile.length(),
                    installTime = System.currentTimeMillis(),
                    updateTime = 0L
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract descriptor: ${e.message}")
            createDefaultDescriptorV2(pluginFile)
        }
    }

    private fun createDefaultDescriptorV2(pluginFile: File): PluginDescriptorV2 {
        return PluginDescriptorV2(
            pluginId = pluginFile.nameWithoutExtension,
            name = pluginFile.nameWithoutExtension,
            version = "1.0",
            description = "Plugin loaded from ${pluginFile.name}",
            author = "Unknown",
            permissions = emptyList(),
            minApiVersion = 21,
            dependencies = emptyList(),
            entryPoint = pluginFile.nameWithoutExtension,
            signature = calculateFileHash(pluginFile),
            size = pluginFile.length(),
            installTime = System.currentTimeMillis(),
            updateTime = 0L
        )
    }

    private fun resolveDependencies(descriptor: PluginDescriptorV2, installedIds: Set<String>): Boolean {
        for (dependency in descriptor.dependencies) {
            if (!dependency.optional && !installedIds.contains(dependency.pluginId)) {
                Log.w(TAG, "Missing required dependency: ${dependency.pluginId}")
                return false
            }
        }
        return true
    }

    private fun createSandboxV2(pluginId: String, config: PluginConfigV2): PluginSandboxV2? {
        return try {
            val sandboxDir = File(context.filesDir, "$SANDBOX_DIR/$pluginId")
            if (!sandboxDir.exists()) {
                sandboxDir.mkdirs()
            }

            PluginSandboxV2(
                pluginId = pluginId,
                sandboxDir = sandboxDir,
                memoryLimit = config.memoryLimitMb * 1024 * 1024L,
                cpuLimit = 2,
                networkAccess = config.allowNetwork,
                fileAccess = config.fileAccessLevel,
                isolated = config.enableSandbox,
                maxExecutionTime = config.maxExecutionTime * 1000
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create sandbox: ${e.message}")
            null
        }
    }

    fun initializePlugin(pluginId: String): PluginResultV2 {
        Log.i(TAG, "Initializing plugin: $pluginId")

        val descriptor = pluginRegistry[pluginId]
        if (descriptor == null) {
            return PluginResultV2.Error(2001, "Plugin not found: $pluginId")
        }

        updatePluginState(pluginId, PluginStateV2.INITIALIZING)

        val future = pluginExecutor.submit {
            try {
                val startTime = System.currentTimeMillis()
                performPluginInitialization(descriptor)
                val startupTime = System.currentTimeMillis() - startTime

                updateMetrics(pluginId) { it.copy(startupTime = startupTime) }

                updatePluginState(pluginId, PluginStateV2.READY)
                PluginResultV2.Success()
            } catch (e: Exception) {
                updatePluginState(pluginId, PluginStateV2.ERROR)
                updateMetrics(pluginId) { it.copy(errorCount = it.errorCount + 1, lastError = e.message) }
                PluginResultV2.Error(2002, "Initialization failed: ${e.message}")
            }
        }

        return waitForResult(pluginId, future, "Initialization")
    }

    private fun performPluginInitialization(descriptor: PluginDescriptorV2) {
        Thread.sleep(100)
    }

    fun startPlugin(pluginId: String): PluginResultV2 {
        Log.i(TAG, "Starting plugin: $pluginId")

        val state = _pluginState.value[pluginId]
        if (state != PluginStateV2.READY) {
            return PluginResultV2.Error(3001, "Plugin not ready, current state: $state")
        }

        val future = pluginExecutor.submit {
            try {
                updatePluginState(pluginId, PluginStateV2.RUNNING)
                val instance = pluginInstances.getOrPut(pluginId) {
                    PluginInstanceV2(
                        descriptor = pluginRegistry[pluginId]!!,
                        state = PluginStateV2.RUNNING
                    )
                }
                instance.state = PluginStateV2.RUNNING
                instance.lastUsed.set(System.currentTimeMillis())
                instance.usageCount.incrementAndGet()

                updateMetrics(pluginId) { it.copy(successRate = (it.successRate + 1f) / 2f) }
            } catch (e: Exception) {
                updatePluginState(pluginId, PluginStateV2.ERROR)
                updateMetrics(pluginId) { it.copy(errorCount = it.errorCount + 1, lastError = e.message) }
                throw e
            }
        }

        return waitForResult(pluginId, future, "Start")
    }

    fun stopPlugin(pluginId: String): PluginResultV2 {
        Log.i(TAG, "Stopping plugin: $pluginId")

        val instance = pluginInstances[pluginId]
        if (instance == null) {
            return PluginResultV2.Error(4001, "Plugin instance not found")
        }

        val future = pluginExecutor.submit {
            try {
                instance.state = PluginStateV2.PAUSED
                updatePluginState(pluginId, PluginStateV2.PAUSED)
            } catch (e: Exception) {
                throw e
            }
        }

        return waitForResult(pluginId, future, "Stop")
    }

    fun uninstallPlugin(pluginId: String): PluginResultV2 {
        Log.i(TAG, "Uninstalling plugin: $pluginId")

        val dependents = dependencyGraph.getDependents(pluginId)
        if (dependents.isNotEmpty()) {
            return PluginResultV2.Error(
                4002,
                "Cannot uninstall: other plugins depend on this: ${dependents.joinToString()}"
            )
        }

        updatePluginState(pluginId, PluginStateV2.UNINSTALLING)

        val future = pluginExecutor.submit {
            try {
                pluginInstances.remove(pluginId)
                pluginSandbox.remove(pluginId)
                pluginRegistry.remove(pluginId)
                dependencyGraph.getDependencies(pluginId).forEach {
                    dependencyGraph.removeDependency(pluginId, it)
                }

                val pluginFile = File(context.filesDir, "$PLUGIN_DIR/$pluginId.apk")
                if (pluginFile.exists()) {
                    pluginFile.delete()
                }

                val sandboxDir = File(context.filesDir, "$SANDBOX_DIR/$pluginId")
                if (sandboxDir.exists()) {
                    sandboxDir.deleteRecursively()
                }

                cacheManager.invalidate(pluginId)
                updatePluginState(pluginId, PluginStateV2.NOT_INSTALLED)
            } catch (e: Exception) {
                updatePluginState(pluginId, PluginStateV2.ERROR)
                throw e
            }
        }

        val result = waitForResult(pluginId, future, "Uninstall")
        if (result is PluginResultV2.Success) {
            saveInstalledPlugins()
            coroutineScope.launch {
                _pluginEvents.emit(PluginEvent.Uninstalled(pluginId))
            }
        }
        return result
    }

    fun updatePlugin(pluginId: String, newPluginFile: File): PluginResultV2 {
        Log.i(TAG, "Updating plugin: $pluginId")

        val currentDescriptor = pluginRegistry[pluginId]
        if (currentDescriptor == null) {
            return PluginResultV2.Error(5001, "Plugin not found")
        }

        if (!verifyPluginSignature(newPluginFile)) {
            return PluginResultV2.Error(5002, "New plugin signature verification failed")
        }

        val newDescriptor = extractPluginDescriptorV2(newPluginFile)
        if (newDescriptor == null) {
            return PluginResultV2.Error(5003, "Failed to extract new plugin descriptor")
        }

        val oldVersion = currentDescriptor.version
        updatePluginState(pluginId, PluginStateV2.UPDATING)

        stopPlugin(pluginId)

        val pluginDir = File(context.filesDir, PLUGIN_DIR)
        val targetFile = File(pluginDir, "$pluginId.apk")

        return try {
            newPluginFile.copyTo(targetFile, overwrite = true)

            pluginRegistry[pluginId] = newDescriptor.copy(
                installTime = currentDescriptor.installTime,
                updateTime = System.currentTimeMillis()
            )

            updatePluginState(pluginId, PluginStateV2.INSTALLED)

            coroutineScope.launch {
                _pluginEvents.emit(PluginEvent.Updated(pluginId, oldVersion, newDescriptor.version))
            }

            Log.i(TAG, "Plugin updated successfully: $pluginId from $oldVersion to ${newDescriptor.version}")
            PluginResultV2.Success()
        } catch (e: Exception) {
            updatePluginState(pluginId, PluginStateV2.ERROR)
            PluginResultV2.Error(5004, "Update failed: ${e.message}")
        }
    }

    fun hotReloadPlugin(pluginId: String): PluginResultV2 {
        Log.i(TAG, "Hot reloading plugin: $pluginId")

        val state = _pluginState.value[pluginId]
        if (state != PluginStateV2.RUNNING && state != PluginStateV2.PAUSED) {
            return PluginResultV2.Error(6001, "Plugin not in reloadable state")
        }

        return try {
            pluginInstances.remove(pluginId)

            coroutineScope.launch {
                delay(HOT_RELOAD_DELAY_MS)
                initializePlugin(pluginId)
                startPlugin(pluginId)
                _pluginEvents.emit(PluginEvent.HotReloaded(pluginId))
            }

            PluginResultV2.Success()
        } catch (e: Exception) {
            PluginResultV2.Error(6002, "Hot reload failed: ${e.message}")
        }
    }

    fun checkPermission(pluginId: String, permission: String): Boolean {
        val sandbox = pluginSandbox[pluginId] ?: return false

        if (sandbox.deniedPermissions.contains(permission)) {
            return false
        }

        if (sandbox.allowedPermissions.contains(permission)) {
            return true
        }

        return when (sandbox.fileAccess) {
            FileAccessLevelV2.NONE -> false
            FileAccessLevelV2.SANDBOX_ONLY -> permission.startsWith("android.permission.READ")
            FileAccessLevelV2.READ_ONLY -> permission.contains("READ")
            FileAccessLevelV2.READ_WRITE -> true
            FileAccessLevelV2.FULL_ACCESS -> true
        }
    }

    fun grantPermission(pluginId: String, permission: String): Boolean {
        val sandbox = pluginSandbox[pluginId] ?: return false

        sandbox.allowedPermissions.add(permission)
        sandbox.deniedPermissions.remove(permission)

        return true
    }

    fun revokePermission(pluginId: String, permission: String): Boolean {
        val sandbox = pluginSandbox[pluginId] ?: return false

        sandbox.deniedPermissions.add(permission)
        sandbox.allowedPermissions.remove(permission)

        return true
    }

    fun getPluginInfo(pluginId: String): PluginDescriptorV2? {
        return pluginRegistry[pluginId]
    }

    fun getAllPlugins(): List<PluginDescriptorV2> {
        return pluginRegistry.values.toList()
    }

    fun getPluginsByCategory(category: PluginCategory): List<PluginDescriptorV2> {
        return pluginRegistry.values.filter { it.category == category }
    }

    fun searchPlugins(query: String): List<PluginDescriptorV2> {
        val lowerQuery = query.lowercase()
        return pluginRegistry.values.filter { plugin ->
            plugin.name.lowercase().contains(lowerQuery) ||
            plugin.description.lowercase().contains(lowerQuery) ||
            plugin.tags.any { it.lowercase().contains(lowerQuery) }
        }
    }

    fun getActivePlugins(): List<String> {
        return pluginInstances.keys.toList()
    }

    fun getPluginState(pluginId: String): PluginStateV2 {
        return _pluginState.value[pluginId] ?: PluginStateV2.NOT_INSTALLED
    }

    fun getPluginMetrics(pluginId: String): PluginMetrics? {
        return _pluginMetrics.value[pluginId]
    }

    fun getAllMetrics(): Map<String, PluginMetrics> {
        return _pluginMetrics.value
    }

    private fun updatePluginState(pluginId: String, state: PluginStateV2) {
        val currentStates = _pluginState.value.toMutableMap()
        val oldState = currentStates[pluginId]
        currentStates[pluginId] = state
        _pluginState.value = currentStates

        if (oldState != null && oldState != state) {
            coroutineScope.launch {
                _pluginEvents.emit(PluginEvent.StateChanged(pluginId, oldState, state))
            }
        }
    }

    private fun updateMetrics(pluginId: String, update: (PluginMetrics) -> PluginMetrics) {
        val currentMetrics = _pluginMetrics.value.toMutableMap()
        val metrics = currentMetrics.getOrPut(pluginId) {
            PluginMetrics(
                pluginId = pluginId,
                cpuUsage = 0,
                memoryUsage = 0,
                networkUsage = 0,
                startupTime = 0,
                averageResponseTime = 0,
                successRate = 1f,
                errorCount = 0,
                lastError = null
            )
        }
        currentMetrics[pluginId] = update(metrics)
        _pluginMetrics.value = currentMetrics
    }

    private fun waitForResult(pluginId: String, future: Future<*>, operation: String): PluginResultV2 {
        return try {
            val result = future.get(PLUGIN_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            result as? PluginResultV2 ?: PluginResultV2.Success()
        } catch (e: TimeoutException) {
            Log.w(TAG, "$operation timeout for plugin: $pluginId")
            pendingTasks.remove(pluginId)?.cancel(true)
            PluginResultV2.Timeout(pluginId)
        } catch (e: Exception) {
            Log.e(TAG, "$operation error for plugin $pluginId: ${e.message}")
            PluginResultV2.Error(9999, "$operation failed: ${e.message}")
        }
    }

    private fun calculateFileHash(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(file.readBytes())
        return hash.joinToString("") { "%02x".format(it) }
    }

    private fun checkApiCompatibility(minApi: Int): Boolean {
        return Build.VERSION.SDK_INT >= minApi
    }

    private fun saveInstalledPlugins() {
        val pluginIds = pluginRegistry.keys.toSet()
        prefs.edit().putStringSet("installed_plugins_v2", pluginIds).apply()

        val pluginJson = Json.encodeToString(pluginRegistry.values.toList())
        prefs.edit().putString("installed_plugins_data_v2", pluginJson).apply()
    }

    fun loadInstalledPlugins() {
        val pluginIds = prefs.getStringSet("installed_plugins_v2", emptySet()) ?: emptySet()

        pluginIds.forEach { pluginId ->
            val pluginFile = File(context.filesDir, "$PLUGIN_DIR/$pluginId.apk")
            if (pluginFile.exists()) {
                val descriptor = extractPluginDescriptorV2(pluginFile)
                if (descriptor != null) {
                    pluginRegistry[pluginId] = descriptor
                    updatePluginState(pluginId, PluginStateV2.INSTALLED)

                    descriptor.dependencies.forEach { dep ->
                        dependencyGraph.addDependency(pluginId, dep.pluginId)
                    }
                }
            }
        }
    }

    fun checkCompatibility(pluginId: String): CompatibilityResultV2 {
        val descriptor = pluginRegistry[pluginId] ?: return CompatibilityResultV2(
            compatible = false,
            issues = listOf("Plugin not installed")
        )

        val issues = mutableListOf<String>()

        if (!checkApiCompatibility(descriptor.minApiVersion)) {
            issues.add("Requires API ${descriptor.minApiVersion}, current: ${Build.VERSION.SDK_INT}")
        }

        for (dependency in descriptor.dependencies) {
            if (!dependency.optional) {
                val depPlugin = pluginRegistry[dependency.pluginId]
                if (depPlugin == null) {
                    issues.add("Missing required dependency: ${dependency.pluginId}")
                } else if (!dependency.versionRange.isCompatible(depPlugin.version)) {
                    issues.add("Dependency ${dependency.pluginId} version incompatible")
                }
            }
        }

        return CompatibilityResultV2(
            compatible = issues.isEmpty(),
            issues = issues
        )
    }

    data class CompatibilityResultV2(
        val compatible: Boolean,
        val issues: List<String>
    )

    fun getSandboxInfo(pluginId: String): PluginSandboxV2? {
        return pluginSandbox[pluginId]
    }

    fun getRecommendedPlugins(): List<PluginDescriptorV2> {
        return pluginRegistry.values
            .sortedByDescending { it.rating }
            .take(5)
    }

    fun ratePlugin(pluginId: String, rating: Float): Boolean {
        val descriptor = pluginRegistry[pluginId] ?: return false

        val newRating = (descriptor.rating + rating) / 2
        pluginRegistry[pluginId] = descriptor.copy(rating = newRating)

        saveInstalledPlugins()
        return true
    }

    fun cleanup() {
        coroutineScope.cancel()
        pluginExecutor.shutdown()
        try {
            if (!pluginExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                pluginExecutor.shutdownNow()
            }
        } catch (e: InterruptedException) {
            pluginExecutor.shutdownNow()
            Thread.currentThread().interrupt()
        }
    }

    sealed class PluginResultV2 {
        data class Success(val data: Any? = null) : PluginResultV2()
        data class Error(val code: Int, val message: String) : PluginResultV2()
        data class Timeout(val pluginId: String) : PluginResultV2()
    }
}

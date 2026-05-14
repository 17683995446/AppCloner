
package com.appcloner.core.plugin

import android.content.Context
import com.appcloner.utils.LogUtils
import java.util.concurrent.ConcurrentHashMap

/**
 * 插件接口 - 所有插件必须实现
 */
interface AppClonerPlugin {
    val pluginId: String
    val pluginName: String
    val pluginVersion: String
    val pluginDescription: String

    fun initialize(context: Context): Result&lt;Unit&gt;
    fun activate(): Result&lt;Unit&gt;
    fun deactivate(): Result&lt;Unit&gt;
    fun destroy()
}

/**
 * 插件状态
 */
enum class PluginState {
    NOT_INITIALIZED,
    INITIALIZED,
    ACTIVE,
    INACTIVE,
    ERROR
}

/**
 * 插件信息
 */
data class PluginInfo(
    val pluginId: String,
    val name: String,
    val version: String,
    val description: String,
    val state: PluginState,
    val activationTime: Long? = null
)

/**
 * 插件管理器 - 专业的插件系统
 * 支持插件注册、加载、激活、去激活
 */
class PluginManager private constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "PluginManager"
        private const val PLUGIN_PREFS = "plugin_settings"
        
        @Volatile
        private var instance: PluginManager? = null
        
        fun getInstance(context: Context): PluginManager {
            return instance ?: synchronized(this) {
                instance ?: PluginManager(context.applicationContext)
                    .also { instance = it }
            }
        }
    }

    private val pluginMap = ConcurrentHashMap&lt;String, AppClonerPlugin&gt;()
    private val pluginStates = ConcurrentHashMap&lt;String, PluginState&gt;()
    private val pluginPrefs = context.getSharedPreferences(PLUGIN_PREFS, Context.MODE_PRIVATE)

    /**
     * 注册插件
     */
    fun registerPlugin(plugin: AppClonerPlugin): Result&lt;Unit&gt; {
        LogUtils.i("Registering plugin: ${plugin.pluginName}", TAG)
        
        if (pluginMap.containsKey(plugin.pluginId)) {
            return Result.failure(IllegalStateException("Plugin ${plugin.pluginId} already registered"))
        }
        
        pluginMap[plugin.pluginId] = plugin
        pluginStates[plugin.pluginId] = PluginState.NOT_INITIALIZED
        
        LogUtils.d("Plugin registered: ${plugin.pluginId}", TAG)
        return Result.success(Unit)
    }

    /**
     * 初始化所有插件
     */
    fun initializeAllPlugins(): List&lt;Result&lt;Unit&gt;&gt; {
        LogUtils.i("Initializing all plugins...", TAG)
        val results = mutableListOf&lt;Result&lt;Unit&gt;&gt;()
        
        for (plugin in pluginMap.values) {
            val result = initializePlugin(plugin.pluginId)
            results.add(result)
        }
        
        return results
    }

    /**
     * 初始化单个插件
     */
    fun initializePlugin(pluginId: String): Result&lt;Unit&gt; {
        val plugin = pluginMap[pluginId] ?: return Result.failure(
            IllegalArgumentException("Plugin $pluginId not found")
        )
        
        val currentState = pluginStates[pluginId] ?: PluginState.NOT_INITIALIZED
        if (currentState != PluginState.NOT_INITIALIZED) {
            return Result.success(Unit) // 已经初始化过
        }
        
        LogUtils.i("Initializing plugin: ${plugin.pluginName}", TAG)
        
        val initResult = plugin.initialize(context)
        pluginStates[pluginId] = if (initResult.isSuccess) {
            PluginState.INITIALIZED
        } else {
            PluginState.ERROR
        }
        
        return initResult
    }

    /**
     * 激活插件
     */
    fun activatePlugin(pluginId: String): Result&lt;Unit&gt; {
        val plugin = pluginMap[pluginId] ?: return Result.failure(
            IllegalArgumentException("Plugin $pluginId not found")
        )
        
        val currentState = pluginStates[pluginId] ?: PluginState.NOT_INITIALIZED
        if (currentState == PluginState.ACTIVE) {
            return Result.success(Unit) // 已经激活
        }
        
        if (currentState != PluginState.INITIALIZED &amp;&amp; currentState != PluginState.INACTIVE) {
            return Result.failure(IllegalStateException("Plugin $pluginId not in valid state"))
        }
        
        LogUtils.i("Activating plugin: ${plugin.pluginName}", TAG)
        
        val activateResult = plugin.activate()
        if (activateResult.isSuccess) {
            pluginStates[pluginId] = PluginState.ACTIVE
            savePluginState(pluginId, true)
        }
        
        return activateResult
    }

    /**
     * 去激活插件
     */
    fun deactivatePlugin(pluginId: String): Result&lt;Unit&gt; {
        val plugin = pluginMap[pluginId] ?: return Result.failure(
            IllegalArgumentException("Plugin $pluginId not found")
        )
        
        val currentState = pluginStates[pluginId] ?: PluginState.NOT_INITIALIZED
        if (currentState != PluginState.ACTIVE) {
            return Result.success(Unit) // 已经不活跃
        }
        
        LogUtils.i("Deactivating plugin: ${plugin.pluginName}", TAG)
        
        val deactivateResult = plugin.deactivate()
        if (deactivateResult.isSuccess) {
            pluginStates[pluginId] = PluginState.INACTIVE
            savePluginState(pluginId, false)
        }
        
        return deactivateResult
    }

    /**
     * 获取插件信息
     */
    fun getPluginInfo(pluginId: String): PluginInfo? {
        val plugin = pluginMap[pluginId] ?: return null
        val state = pluginStates[pluginId] ?: PluginState.NOT_INITIALIZED
        
        return PluginInfo(
            pluginId = pluginId,
            name = plugin.pluginName,
            version = plugin.pluginVersion,
            description = plugin.pluginDescription,
            state = state
        )
    }

    /**
     * 获取所有插件信息
     */
    fun getAllPluginInfo(): List&lt;PluginInfo&gt; {
        return pluginMap.keys.mapNotNull { getPluginInfo(it) }
    }

    /**
     * 获取已激活的插件
     */
    fun getActivePlugins(): List&lt;AppClonerPlugin&gt; {
        return pluginMap.entries
            .filter { (id, _) -&gt; pluginStates[id] == PluginState.ACTIVE }
            .map { it.value }
    }

    /**
     * 获取特定插件
     */
    fun &lt;T : AppClonerPlugin&gt; getPlugin(pluginId: String, clazz: Class&lt;T&gt;): T? {
        val plugin = pluginMap[pluginId] ?: return null
        @Suppress("UNCHECKED_CAST")
        return plugin as? T
    }

    private fun savePluginState(pluginId: String, active: Boolean) {
        pluginPrefs.edit().putBoolean("${pluginId}_active", active).apply()
    }
}

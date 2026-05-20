package com.appcloner.plugin

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mockito.*
import java.io.File

class EnhancedPluginManagerTest {

    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences
    private lateinit var prefsEditor: SharedPreferences.Editor
    private lateinit var pluginManager: EnhancedPluginManager

    @Before
    fun setUp() {
        context = mock(Context::class.java)
        prefs = mock(SharedPreferences::class.java)
        prefsEditor = mock(SharedPreferences.Editor::class.java)

        `when`(context.getSharedPreferences(anyString(), anyInt())).thenReturn(prefs)
        `when`(prefs.edit()).thenReturn(prefsEditor)
        `when`(prefsEditor.putString(anyString(), any())).thenReturn(prefsEditor)
        `when`(prefsEditor.putStringSet(anyString(), any())).thenReturn(prefsEditor)
        `when`(prefsEditor.apply()).then { }

        `when`(prefs.getStringSet(eq("installed_plugins"), isNull())).thenReturn(emptySet())

        pluginManager = EnhancedPluginManager.getInstance(context)
    }

    @Test
    fun testSingletonInstance() {
        val instance1 = EnhancedPluginManager.getInstance(context)
        val instance2 = EnhancedPluginManager.getInstance(context)

        assertSame(instance1, instance2)
    }

    @Test
    fun testInitialState() {
        val state = pluginManager.pluginState.value

        assertNotNull(state)
        assertTrue(state.isEmpty())
    }

    @Test
    fun testPluginDescriptorCreation() {
        val descriptor = EnhancedPluginManager.PluginDescriptor(
            pluginId = "test_plugin",
            name = "Test Plugin",
            version = "1.0.0",
            description = "A test plugin",
            author = "Test Author",
            permissions = listOf("android.permission.INTERNET"),
            minApiVersion = 21,
            dependencies = emptyList(),
            entryPoint = "com.test.plugin.Main",
            signature = "sha256_hash",
            size = 1024L,
            installTime = System.currentTimeMillis(),
            updateTime = 0L
        )

        assertEquals("test_plugin", descriptor.pluginId)
        assertEquals("Test Plugin", descriptor.name)
        assertEquals("1.0.0", descriptor.version)
        assertEquals("A test plugin", descriptor.description)
        assertEquals("Test Author", descriptor.author)
        assertEquals(1, descriptor.permissions.size)
        assertEquals(21, descriptor.minApiVersion)
        assertTrue(descriptor.dependencies.isEmpty())
    }

    @Test
    fun testPluginStateEnum() {
        val states = EnhancedPluginManager.PluginState.values()

        assertEquals(8, states.size)
        assertTrue(states.contains(EnhancedPluginManager.PluginState.NOT_INSTALLED))
        assertTrue(states.contains(EnhancedPluginManager.PluginState.INSTALLED))
        assertTrue(states.contains(EnhancedPluginManager.PluginState.INITIALIZING))
        assertTrue(states.contains(EnhancedPluginManager.PluginState.READY))
        assertTrue(states.contains(EnhancedPluginManager.PluginState.RUNNING))
        assertTrue(states.contains(EnhancedPluginManager.PluginState.PAUSED))
        assertTrue(states.contains(EnhancedPluginManager.PluginState.ERROR))
        assertTrue(states.contains(EnhancedPluginManager.PluginState.UNINSTALLING))
    }

    @Test
    fun testFileAccessLevelEnum() {
        val levels = EnhancedPluginManager.FileAccessLevel.values()

        assertEquals(4, levels.size)
        assertTrue(levels.contains(EnhancedPluginManager.FileAccessLevel.NONE))
        assertTrue(levels.contains(EnhancedPluginManager.FileAccessLevel.SANDBOX_ONLY))
        assertTrue(levels.contains(EnhancedPluginManager.FileAccessLevel.READ_ONLY))
        assertTrue(levels.contains(EnhancedPluginManager.FileAccessLevel.READ_WRITE))
    }

    @Test
    fun testPermissionAccessEnum() {
        val accesses = EnhancedPluginManager.PermissionAccess.values()

        assertEquals(3, accesses.size)
        assertTrue(accesses.contains(EnhancedPluginManager.PermissionAccess.DENIED))
        assertTrue(accesses.contains(EnhancedPluginManager.PermissionAccess.PROMPT))
        assertTrue(accesses.contains(EnhancedPluginManager.PermissionAccess.ALLOWED))
    }

    @Test
    fun testPluginConfigDefaultValues() {
        val config = EnhancedPluginManager.PluginConfig()

        assertTrue(config.enableSandbox)
        assertEquals(128, config.memoryLimitMb)
        assertTrue(config.enableHotReload)
        assertFalse(config.allowNetwork)
        assertEquals(EnhancedPluginManager.FileAccessLevel.SANDBOX_ONLY, config.fileAccessLevel)
    }

    @Test
    fun testPluginConfigCustomValues() {
        val config = EnhancedPluginManager.PluginConfig(
            enableSandbox = false,
            memoryLimitMb = 256,
            enableHotReload = false,
            allowNetwork = true,
            fileAccessLevel = EnhancedPluginManager.FileAccessLevel.READ_WRITE
        )

        assertFalse(config.enableSandbox)
        assertEquals(256, config.memoryLimitMb)
        assertFalse(config.enableHotReload)
        assertTrue(config.allowNetwork)
        assertEquals(EnhancedPluginManager.FileAccessLevel.READ_WRITE, config.fileAccessLevel)
    }

    @Test
    fun testPluginResultSuccess() {
        val result = EnhancedPluginManager.PluginResult.Success()

        assertTrue(result is EnhancedPluginManager.PluginResult.Success)
        assertNull(result.data)
    }

    @Test
    fun testPluginResultSuccessWithData() {
        val result = EnhancedPluginManager.PluginResult.Success("test data")

        assertTrue(result is EnhancedPluginManager.PluginResult.Success)
        assertEquals("test data", result.data)
    }

    @Test
    fun testPluginResultError() {
        val result = EnhancedPluginManager.PluginResult.Error(1001, "Test error")

        assertTrue(result is EnhancedPluginManager.PluginResult.Error)
        assertEquals(1001, result.code)
        assertEquals("Test error", result.message)
    }

    @Test
    fun testPluginResultTimeout() {
        val result = EnhancedPluginManager.PluginResult.Timeout("plugin_id")

        assertTrue(result is EnhancedPluginManager.PluginResult.Timeout)
        assertEquals("plugin_id", result.pluginId)
    }

    @Test
    fun testCompatibilityResultCompatible() {
        val result = EnhancedPluginManager.CompatibilityResult(
            compatible = true,
            issues = emptyList()
        )

        assertTrue(result.compatible)
        assertTrue(result.issues.isEmpty())
    }

    @Test
    fun testCompatibilityResultIncompatible() {
        val issues = listOf("Missing API 30", "Missing dependency: lib_x")
        val result = EnhancedPluginManager.CompatibilityResult(
            compatible = false,
            issues = issues
        )

        assertFalse(result.compatible)
        assertEquals(2, result.issues.size)
    }

    @Test
    fun testGetPluginStateNotInstalled() {
        val state = pluginManager.getPluginState("nonexistent_plugin")

        assertEquals(EnhancedPluginManager.PluginState.NOT_INSTALLED, state)
    }

    @Test
    fun testGetAllPluginsEmpty() {
        val plugins = pluginManager.getAllPlugins()

        assertNotNull(plugins)
        assertTrue(plugins.isEmpty())
    }

    @Test
    fun testGetActivePluginsEmpty() {
        val plugins = pluginManager.getActivePlugins()

        assertNotNull(plugins)
        assertTrue(plugins.isEmpty())
    }

    @Test
    fun testGetPluginInfoNotFound() {
        val info = pluginManager.getPluginInfo("nonexistent")

        assertNull(info)
    }

    @Test
    fun testGetSandboxInfoNotFound() {
        val sandbox = pluginManager.getSandboxInfo("nonexistent")

        assertNull(sandbox)
    }

    @Test
    fun testCheckPermissionNotInstalled() {
        val hasPermission = pluginManager.checkPermission("nonexistent", "android.permission.INTERNET")

        assertFalse(hasPermission)
    }

    @Test
    fun testGrantPermissionNotInstalled() {
        val granted = pluginManager.grantPermission("nonexistent", "android.permission.INTERNET")

        assertFalse(granted)
    }

    @Test
    fun testRevokePermissionNotInstalled() {
        val revoked = pluginManager.revokePermission("nonexistent", "android.permission.INTERNET")

        assertFalse(revoked)
    }

    @Test
    fun testLoadInstalledPlugins() {
        `when`(prefs.getStringSet(eq("installed_plugins"), isNull())).thenReturn(emptySet())

        pluginManager.loadInstalledPlugins()

        verify(prefs).getStringSet("installed_plugins", emptySet())
    }

    @Test
    fun testCleanup() {
        pluginManager.cleanup()
    }

    @Test
    fun testPluginInstanceCreation() {
        val descriptor = EnhancedPluginManager.PluginDescriptor(
            pluginId = "test",
            name = "Test",
            version = "1.0",
            description = "",
            author = "",
            permissions = emptyList(),
            minApiVersion = 21,
            dependencies = emptyList(),
            entryPoint = "",
            signature = "",
            size = 0L,
            installTime = System.currentTimeMillis(),
            updateTime = 0L
        )

        val instance = EnhancedPluginManager.PluginInstance(
            descriptor = descriptor,
            state = EnhancedPluginManager.PluginState.INSTALLED,
            memoryUsage = 0L,
            lastUsed = System.currentTimeMillis()
        )

        assertEquals(descriptor, instance.descriptor)
        assertEquals(EnhancedPluginManager.PluginState.INSTALLED, instance.state)
        assertEquals(0L, instance.memoryUsage)
        assertTrue(instance.lastUsed > 0)
    }

    @Test
    fun testSandboxCreation() {
        val sandbox = EnhancedPluginManager.PluginSandbox(
            pluginId = "test_sandbox",
            sandboxDir = File("/tmp/sandbox"),
            allowedPermissions = emptySet(),
            deniedPermissions = emptySet(),
            memoryLimit = 128 * 1024 * 1024L,
            cpuLimit = 2,
            networkAccess = false,
            fileAccess = EnhancedPluginManager.FileAccessLevel.SANDBOX_ONLY,
            isolated = true
        )

        assertEquals("test_sandbox", sandbox.pluginId)
        assertEquals(File("/tmp/sandbox"), sandbox.sandboxDir)
        assertTrue(sandbox.allowedPermissions.isEmpty())
        assertTrue(sandbox.deniedPermissions.isEmpty())
        assertEquals(128 * 1024 * 1024L, sandbox.memoryLimit)
        assertEquals(2, sandbox.cpuLimit)
        assertFalse(sandbox.networkAccess)
        assertEquals(EnhancedPluginManager.FileAccessLevel.SANDBOX_ONLY, sandbox.fileAccess)
        assertTrue(sandbox.isolated)
    }

    @Test
    fun testPermissionCreation() {
        val permission = EnhancedPluginManager.PluginPermission(
            permission = "android.permission.INTERNET",
            access = EnhancedPluginManager.PermissionAccess.ALLOWED,
            reason = "Required for network features"
        )

        assertEquals("android.permission.INTERNET", permission.permission)
        assertEquals(EnhancedPluginManager.PermissionAccess.ALLOWED, permission.access)
        assertEquals("Required for network features", permission.reason)
    }
}

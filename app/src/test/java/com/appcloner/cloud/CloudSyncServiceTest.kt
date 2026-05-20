package com.appcloner.cloud

import android.content.Context
import android.content.SharedPreferences
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mockito.*

class CloudSyncServiceTest {

    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences
    private lateinit var prefsEditor: SharedPreferences.Editor
    private lateinit var cloudSyncService: CloudSyncService

    @Before
    fun setUp() {
        context = mock(Context::class.java)
        prefs = mock(SharedPreferences::class.java)
        prefsEditor = mock(SharedPreferences.Editor::class.java)

        `when`(context.getSharedPreferences(anyString(), anyInt())).thenReturn(prefs)
        `when`(prefs.edit()).thenReturn(prefsEditor)
        `when`(prefsEditor.putString(anyString(), any())).thenReturn(prefsEditor)
        `when`(prefsEditor.putStringSet(anyString(), any())).thenReturn(prefsEditor)
        `when`(prefsEditor.putLong(anyString(), anyLong())).thenReturn(prefsEditor)
        `when`(prefsEditor.putBoolean(anyString(), anyBoolean())).thenReturn(prefsEditor)
        `when`(prefsEditor.putInt(anyString(), anyInt())).thenReturn(prefsEditor)
        `when`(prefsEditor.remove(anyString())).thenReturn(prefsEditor)
        `when`(prefsEditor.apply()).then { }

        `when`(prefs.getString(eq("device_id"), isNull())).thenReturn(null)
        `when`(prefs.getString(eq("device_id"), anyString())).thenReturn("test_device_123")
        `when`(prefs.getString(eq("sync_key"), isNull())).thenReturn(null)
        `when`(prefs.getLong(eq("last_sync_time"), eq(0L))).thenReturn(0L)
        `when`(prefs.getStringSet(eq("device_ids"), isNull())).thenReturn(emptySet())
        `when`(prefs.getBoolean(eq("auto_sync"), eq(false))).thenReturn(false)
        `when`(prefs.getInt(eq("sync_interval"), eq(30))).thenReturn(30)
        `when`(prefs.getBoolean(eq("wifi_only"), eq(true))).thenReturn(true)
        `when`(prefs.getBoolean(eq("include_backups"), eq(false))).thenReturn(false)
        `when`(prefs.getBoolean(eq("encryption_enabled"), eq(true))).thenReturn(true)

        cloudSyncService = CloudSyncService(context)
    }

    @Test
    fun testDeviceRegistration() {
        val result = cloudSyncService.registerDevice("Test Phone")

        assertTrue(result.success)
        assertNotNull(result.deviceId)
        assertNotNull(result.deviceInfo)
        assertEquals("Test Phone", result.deviceInfo?.deviceName)
        assertTrue(result.deviceInfo?.isCurrentDevice == true)
    }

    @Test
    fun testDeviceRegistrationSuccess() {
        val result = cloudSyncService.registerDevice("My Device")

        assertTrue(result.success)
        assertEquals("Device registered successfully", result.message)
    }

    @Test
    fun testGetLastSyncTime() {
        val lastSync = cloudSyncService.getLastSyncTime()

        assertTrue(lastSync >= 0L)
    }

    @Test
    fun testSyncStateInitialization() {
        val syncState = cloudSyncService.syncState.value

        assertTrue(syncState is CloudSyncService.SyncState.Idle)
    }

    @Test
    fun testConnectedDevicesInitialization() {
        val devices = cloudSyncService.connectedDevices.value

        assertNotNull(devices)
        assertTrue(devices.isEmpty())
    }

    @Test
    fun testSyncConfigDefaultValues() {
        val config = cloudSyncService.getSyncConfig()

        assertFalse(config.autoSync)
        assertEquals(30, config.syncIntervalMinutes)
        assertTrue(config.wifiOnly)
        assertFalse(config.includeBackups)
        assertTrue(config.encryptionEnabled)
    }

    @Test
    fun testUpdateSyncConfig() {
        val newConfig = CloudSyncService.SyncConfig(
            autoSync = true,
            syncIntervalMinutes = 60,
            wifiOnly = false,
            includeBackups = true,
            encryptionEnabled = false
        )

        cloudSyncService.updateSyncConfig(newConfig)

        verify(prefsEditor).putBoolean("auto_sync", true)
        verify(prefsEditor).putInt("sync_interval", 60)
        verify(prefsEditor).putBoolean("wifi_only", false)
        verify(prefsEditor).putBoolean("include_backups", true)
        verify(prefsEditor).putBoolean("encryption_enabled", false)
    }

    @Test
    fun testRemoveDevice() {
        cloudSyncService.registerDevice("Test Device")
        
        cloudSyncService.removeDevice("test_device_123")

        verify(prefsEditor).remove("device_name_test_device_123")
        verify(prefsEditor).remove("device_type_test_device_123")
        verify(prefsEditor).remove("last_sync_test_device_123")
        verify(prefsEditor).remove("device_status_test_device_123")
        verify(prefsEditor).remove("cloud_data_test_device_123")
    }

    @Test
    fun testStorageUsageCalculation() {
        `when`(prefs.getString(eq("cloud_data_test_device_123"), isNull())).thenReturn("")
        `when`(prefs.getStringSet(eq("cloud_data_backup"), isNull())).thenReturn(emptySet())

        val usage = cloudSyncService.getStorageUsage()

        assertNotNull(usage)
        assertTrue(usage.quotaBytes > 0)
        assertTrue(usage.usedBytes >= 0)
        assertTrue(usage.usagePercent >= 0f)
    }

    @Test
    fun testStorageUsagePercent() {
        `when`(prefs.getString(eq("cloud_data_test_device_123"), isNull())).thenReturn("")
        `when`(prefs.getStringSet(eq("cloud_data_backup"), isNull())).thenReturn(emptySet())

        val usage = cloudSyncService.getStorageUsage()

        val expectedPercent = (usage.usedBytes.toFloat() / usage.quotaBytes) * 100
        assertEquals(expectedPercent, usage.usagePercent, 0.01f)
    }

    @Test
    fun testDeviceInfoCreation() {
        val deviceInfo = CloudSyncService.DeviceInfo(
            deviceId = "device_001",
            deviceName = "Test Device",
            deviceType = CloudSyncService.DeviceType.PHONE,
            lastSyncTime = System.currentTimeMillis(),
            syncStatus = CloudSyncService.SyncStatus.ACTIVE,
            isCurrentDevice = true
        )

        assertEquals("device_001", deviceInfo.deviceId)
        assertEquals("Test Device", deviceInfo.deviceName)
        assertEquals(CloudSyncService.DeviceType.PHONE, deviceInfo.deviceType)
        assertEquals(CloudSyncService.SyncStatus.ACTIVE, deviceInfo.syncStatus)
        assertTrue(deviceInfo.isCurrentDevice)
    }

    @Test
    fun testSyncDataCreation() {
        val syncData = CloudSyncService.SyncData(
            id = "data_001",
            dataType = CloudSyncService.DataType.TEMPLATES,
            content = "test content",
            timestamp = System.currentTimeMillis(),
            version = 1,
            checksum = "abc123"
        )

        assertEquals("data_001", syncData.id)
        assertEquals(CloudSyncService.DataType.TEMPLATES, syncData.dataType)
        assertEquals("test content", syncData.content)
        assertEquals(1, syncData.version)
        assertEquals("abc123", syncData.checksum)
    }

    @Test
    fun testSyncConflictCreation() {
        val localData = CloudSyncService.SyncData(
            id = "conflict_data",
            dataType = CloudSyncService.DataType.SETTINGS,
            content = "local content",
            timestamp = 1000L,
            version = 1,
            checksum = "local_checksum"
        )

        val remoteData = CloudSyncService.SyncData(
            id = "conflict_data",
            dataType = CloudSyncService.DataType.SETTINGS,
            content = "remote content",
            timestamp = 2000L,
            version = 1,
            checksum = "remote_checksum"
        )

        val conflict = CloudSyncService.SyncConflict(
            dataType = CloudSyncService.DataType.SETTINGS,
            localVersion = localData,
            remoteVersion = remoteData
        )

        assertEquals(CloudSyncService.DataType.SETTINGS, conflict.dataType)
        assertEquals("local content", conflict.localVersion.content)
        assertEquals("remote content", conflict.remoteVersion.content)
        assertNotNull(conflict.conflictId)
    }

    @Test
    fun testResolveConflictKeepLocal() {
        val localData = CloudSyncService.SyncData(
            id = "conflict",
            dataType = CloudSyncService.DataType.TEMPLATES,
            content = "local",
            timestamp = 1000L,
            version = 1,
            checksum = "local"
        )

        val remoteData = CloudSyncService.SyncData(
            id = "conflict",
            dataType = CloudSyncService.DataType.TEMPLATES,
            content = "remote",
            timestamp = 2000L,
            version = 1,
            checksum = "remote"
        )

        val conflict = CloudSyncService.SyncConflict(
            dataType = CloudSyncService.DataType.TEMPLATES,
            localVersion = localData,
            remoteVersion = remoteData
        )

        val resolved = cloudSyncService.resolveConflict(
            conflict,
            CloudSyncService.ConflictResolution.KEEP_LOCAL
        )

        assertEquals("local", resolved.content)
    }

    @Test
    fun testResolveConflictKeepRemote() {
        val localData = CloudSyncService.SyncData(
            id = "conflict",
            dataType = CloudSyncService.DataType.TEMPLATES,
            content = "local",
            timestamp = 1000L,
            version = 1,
            checksum = "local"
        )

        val remoteData = CloudSyncService.SyncData(
            id = "conflict",
            dataType = CloudSyncService.DataType.TEMPLATES,
            content = "remote",
            timestamp = 2000L,
            version = 1,
            checksum = "remote"
        )

        val conflict = CloudSyncService.SyncConflict(
            dataType = CloudSyncService.DataType.TEMPLATES,
            localVersion = localData,
            remoteVersion = remoteData
        )

        val resolved = cloudSyncService.resolveConflict(
            conflict,
            CloudSyncService.ConflictResolution.KEEP_REMOTE
        )

        assertEquals("remote", resolved.content)
    }

    @Test
    fun testDeviceTypeDetection() {
        val phone = CloudSyncService.DeviceType.PHONE
        val tablet = CloudSyncService.DeviceType.TABLET
        val emulator = CloudSyncService.DeviceType.EMULATOR
        val unknown = CloudSyncService.DeviceType.UNKNOWN

        assertEquals("PHONE", phone.name)
        assertEquals("TABLET", tablet.name)
        assertEquals("EMULATOR", emulator.name)
        assertEquals("UNKNOWN", unknown.name)
    }

    @Test
    fun testSyncStatusValues() {
        val active = CloudSyncService.SyncStatus.ACTIVE
        val inactive = CloudSyncService.SyncStatus.INACTIVE
        val syncing = CloudSyncService.SyncStatus.SYNCING
        val error = CloudSyncService.SyncStatus.ERROR

        assertEquals(4, CloudSyncService.SyncStatus.values().size)
        assertTrue(active.name.isNotEmpty())
        assertTrue(inactive.name.isNotEmpty())
        assertTrue(syncing.name.isNotEmpty())
        assertTrue(error.name.isNotEmpty())
    }

    @Test
    fun testDataTypeEnum() {
        val types = CloudSyncService.DataType.values()

        assertEquals(6, types.size)
        assertTrue(types.contains(CloudSyncService.DataType.CLONE_CONFIG))
        assertTrue(types.contains(CloudSyncService.DataType.BACKUP_DATA))
        assertTrue(types.contains(CloudSyncService.DataType.APP_LIST))
        assertTrue(types.contains(CloudSyncService.DataType.SETTINGS))
        assertTrue(types.contains(CloudSyncService.DataType.TEMPLATES))
        assertTrue(types.contains(CloudSyncService.DataType.CUSTOM_DATA))
    }

    @Test
    fun testConflictResolutionEnum() {
        val resolutions = CloudSyncService.ConflictResolution.values()

        assertEquals(3, resolutions.size)
        assertTrue(resolutions.contains(CloudSyncService.ConflictResolution.KEEP_LOCAL))
        assertTrue(resolutions.contains(CloudSyncService.ConflictResolution.KEEP_REMOTE))
        assertTrue(resolutions.contains(CloudSyncService.ConflictResolution.MERGE))
    }

    @Test
    fun testSyncStateTypes() {
        val idle = CloudSyncService.SyncState.Idle
        val syncing = CloudSyncService.SyncState.Syncing
        val timestamp = System.currentTimeMillis()
        val success = CloudSyncService.SyncState.Success(timestamp)
        val error = CloudSyncService.SyncState.Error("test error")

        assertTrue(idle is CloudSyncService.SyncState.Idle)
        assertTrue(syncing is CloudSyncService.SyncState.Syncing)
        assertTrue(success is CloudSyncService.SyncState.Success)
        assertTrue(error is CloudSyncService.SyncState.Error)
    }

    @Test
    fun testSyncConfigCustomValues() {
        val config = CloudSyncService.SyncConfig(
            autoSync = true,
            syncIntervalMinutes = 120,
            wifiOnly = false,
            includeBackups = true,
            encryptionEnabled = false
        )

        assertTrue(config.autoSync)
        assertEquals(120, config.syncIntervalMinutes)
        assertFalse(config.wifiOnly)
        assertTrue(config.includeBackups)
        assertFalse(config.encryptionEnabled)
    }

    @Test
    fun testExportEncryptedBackup() {
        val backup = cloudSyncService.exportEncryptedBackup()

        assertNotNull(backup)
        assertTrue(backup.isNotEmpty())
    }

    @Test
    fun testImportEncryptedBackupInvalid() {
        val result = cloudSyncService.importEncryptedBackup("invalid_data")

        assertFalse(result)
    }

    @Test
    fun testDeviceRegistrationResultSuccess() {
        val result = CloudSyncService.DeviceRegistrationResult(
            success = true,
            deviceId = "device_123",
            deviceInfo = null,
            message = "Success"
        )

        assertTrue(result.success)
        assertEquals("device_123", result.deviceId)
        assertEquals("Success", result.message)
    }

    @Test
    fun testDeviceRegistrationResultFailure() {
        val result = CloudSyncService.DeviceRegistrationResult(
            success = false,
            deviceId = null,
            deviceInfo = null,
            message = "Failed to register"
        )

        assertFalse(result.success)
        assertNull(result.deviceId)
        assertEquals("Failed to register", result.message)
    }

    @Test
    fun testSyncResultSuccess() {
        val timestamp = System.currentTimeMillis()
        val result = CloudSyncService.SyncResult(
            success = true,
            syncedItems = 10,
            conflicts = emptyList(),
            errors = emptyList(),
            timestamp = timestamp
        )

        assertTrue(result.success)
        assertEquals(10, result.syncedItems)
        assertTrue(result.conflicts.isEmpty())
        assertTrue(result.errors.isEmpty())
        assertEquals(timestamp, result.timestamp)
    }

    @Test
    fun testSyncResultWithConflicts() {
        val timestamp = System.currentTimeMillis()
        val conflicts = listOf(
            CloudSyncService.SyncConflict(
                dataType = CloudSyncService.DataType.SETTINGS,
                localVersion = mock(CloudSyncService.SyncData::class.java),
                remoteVersion = mock(CloudSyncService.SyncData::class.java)
            )
        )

        val result = CloudSyncService.SyncResult(
            success = false,
            syncedItems = 0,
            conflicts = conflicts,
            errors = listOf("Conflict detected"),
            timestamp = timestamp
        )

        assertFalse(result.success)
        assertEquals(1, result.conflicts.size)
        assertEquals(1, result.errors.size)
    }
}

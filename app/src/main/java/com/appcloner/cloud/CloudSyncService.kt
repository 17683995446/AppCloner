package com.appcloner.cloud

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.Signature
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class CloudSyncService(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "cloud_sync_prefs", Context.MODE_PRIVATE
    )

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _connectedDevices = MutableStateFlow<List<DeviceInfo>>(emptyList())
    val connectedDevices: StateFlow<List<DeviceInfo>> = _connectedDevices.asStateFlow()

    private var deviceId: String = ""
    private var encryptionKey: SecretKey? = null

    init {
        initializeService()
    }

    private fun initializeService() {
        deviceId = prefs.getString("device_id", null) ?: UUID.randomUUID().toString().also {
            prefs.edit().putString("device_id", it).apply()
        }
        
        loadConnectedDevices()
        initializeEncryption()
    }

    private fun initializeEncryption() {
        try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            
            if (!keyStore.containsAlias("appcloner_sync_key")) {
                generateEncryptionKey()
            }
            
            encryptionKey = getEncryptionKey()
        } catch (e: Exception) {
            val existingKey = prefs.getString("sync_key", null)
            if (existingKey != null) {
                val keyBytes = Base64.decode(existingKey, Base64.DEFAULT)
                encryptionKey = SecretKeySpec(keyBytes, "AES")
            }
        }
    }

    private fun generateEncryptionKey() {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES)
        keyGenerator.init(256, SecureRandom())
        val key = keyGenerator.generateKey()
        
        val keyString = Base64.encodeToString(key.encoded, Base64.DEFAULT)
        prefs.edit().putString("sync_key", keyString).apply()
    }

    private fun getEncryptionKey(): SecretKey? {
        val keyString = prefs.getString("sync_key", null) ?: return null
        val keyBytes = Base64.decode(keyString, Base64.DEFAULT)
        return SecretKeySpec(keyBytes, "AES")
    }

    data class DeviceInfo(
        val deviceId: String,
        val deviceName: String,
        val deviceType: DeviceType,
        val lastSyncTime: Long,
        val syncStatus: SyncStatus,
        val isCurrentDevice: Boolean = false
    )

    enum class DeviceType {
        PHONE, TABLET, EMULATOR, UNKNOWN
    }

    enum class SyncStatus {
        ACTIVE, INACTIVE, SYNCING, ERROR
    }

    sealed class SyncState {
        object Idle : SyncState()
        object Syncing : SyncState()
        data class Success(val timestamp: Long) : SyncState()
        data class Error(val message: String) : SyncState()
        data class Conflict(val conflicts: List<SyncConflict>) : SyncState()
    }

    data class SyncConflict(
        val dataType: DataType,
        val localVersion: SyncData,
        val remoteVersion: SyncData,
        val conflictId: String = UUID.randomUUID().toString()
    )

    enum class DataType {
        CLONE_CONFIG, BACKUP_DATA, APP_LIST, SETTINGS, TEMPLATES, CUSTOM_DATA
    }

    data class SyncData(
        val id: String,
        val dataType: DataType,
        val content: String,
        val timestamp: Long,
        val version: Int,
        val checksum: String
    )

    data class SyncResult(
        val success: Boolean,
        val syncedItems: Int,
        val conflicts: List<SyncConflict>,
        val errors: List<String>,
        val timestamp: Long
    )

    data class SyncConfig(
        val autoSync: Boolean = false,
        val syncIntervalMinutes: Int = 30,
        val wifiOnly: Boolean = true,
        val includeBackups: Boolean = false,
        val encryptionEnabled: Boolean = true
    )

    data class CloudCredentials(
        val serverUrl: String,
        val apiKey: String,
        val userId: String
    )

    fun registerDevice(deviceName: String): DeviceRegistrationResult {
        return try {
            val publicKey = getPublicKey()
            val signature = generateDeviceSignature(deviceId, publicKey)
            
            val deviceInfo = DeviceInfo(
                deviceId = deviceId,
                deviceName = deviceName,
                deviceType = detectDeviceType(),
                lastSyncTime = System.currentTimeMillis(),
                syncStatus = SyncStatus.ACTIVE,
                isCurrentDevice = true
            )
            
            saveDeviceInfo(deviceInfo)
            
            DeviceRegistrationResult(
                success = true,
                deviceId = deviceId,
                deviceInfo = deviceInfo,
                message = "Device registered successfully"
            )
        } catch (e: Exception) {
            DeviceRegistrationResult(
                success = false,
                deviceId = null,
                deviceInfo = null,
                message = "Registration failed: ${e.message}"
            )
        }
    }

    data class DeviceRegistrationResult(
        val success: Boolean,
        val deviceId: String?,
        val deviceInfo: DeviceInfo?,
        val message: String
    )

    private fun detectDeviceType(): DeviceType {
        return when {
            context.packageManager.hasSystemFeature("android.hardware.type.pc") -> DeviceType.TABLET
            context.packageManager.hasSystemFeature("android.hardware.touchscreen") -> DeviceType.PHONE
            android.os.Build.PRODUCT.contains("emulator") -> DeviceType.EMULATOR
            else -> DeviceType.UNKNOWN
        }
    }

    private fun getPublicKey(): String {
        return try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            
            if (!keyStore.containsAlias("appcloner_signing_key")) {
                generateSigningKeyPair()
            }
            
            val entry = keyStore.getEntry("appcloner_signing_key", null) as KeyStore.PrivateKeyEntry
            Base64.encodeToString(entry.certificate.publicKey.encoded, Base64.NO_WRAP)
        } catch (e: Exception) {
            "fallback_key"
        }
    }

    private fun generateSigningKeyPair() {
        val keyPairGenerator = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore"
        )
        
        val spec = KeyGenParameterSpec.Builder(
            "appcloner_signing_key",
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        )
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
            .build()
        
        keyPairGenerator.initialize(spec)
        keyPairGenerator.generateKeyPair()
    }

    private fun generateDeviceSignature(deviceId: String, publicKey: String): String {
        return try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            
            val privateKey = (keyStore.getEntry("appcloner_signing_key", null) as KeyStore.PrivateKeyEntry).privateKey
            
            val signature = Signature.getInstance("SHA256withRSA")
            signature.initSign(privateKey)
            signature.update(deviceId.toByteArray())
            
            Base64.encodeToString(signature.sign(), Base64.NO_WRAP)
        } catch (e: Exception) {
            deviceId.hashCode().toString()
        }
    }

    suspend fun syncData(): SyncResult {
        _syncState.value = SyncState.Syncing
        
        return try {
            val localData = collectLocalData()
            val remoteData = fetchRemoteData()
            
            val conflicts = detectConflicts(localData, remoteData)
            
            if (conflicts.isNotEmpty()) {
                _syncState.value = SyncState.Conflict(conflicts)
                return SyncResult(
                    success = false,
                    syncedItems = 0,
                    conflicts = conflicts,
                    errors = emptyList(),
                    timestamp = System.currentTimeMillis()
                )
            }
            
            val mergedData = mergeData(localData, remoteData)
            val uploadResult = uploadToCloud(mergedData)
            
            val result = SyncResult(
                success = uploadResult,
                syncedItems = mergedData.size,
                conflicts = emptyList(),
                errors = if (!uploadResult) listOf("Upload failed") else emptyList(),
                timestamp = System.currentTimeMillis()
            )
            
            _syncState.value = SyncState.Success(result.timestamp)
            updateLastSyncTime()
            
            result
        } catch (e: Exception) {
            _syncState.value = SyncState.Error(e.message ?: "Unknown error")
            SyncResult(
                success = false,
                syncedItems = 0,
                conflicts = emptyList(),
                errors = listOf(e.message ?: "Unknown error"),
                timestamp = System.currentTimeMillis()
            )
        }
    }

    private fun collectLocalData(): List<SyncData> {
        val dataList = mutableListOf<SyncData>()
        
        val cloneConfigs = collectCloneConfigs()
        dataList.addAll(cloneConfigs)
        
        val templates = collectTemplates()
        dataList.addAll(templates)
        
        val settings = collectSettings()
        dataList.addAll(settings)
        
        return dataList
    }

    private fun collectCloneConfigs(): List<SyncData> {
        val configs = prefs.getStringSet("clone_configs", emptySet()) ?: emptySet()
        return configs.mapIndexed { index, config ->
            SyncData(
                id = "clone_config_$index",
                dataType = DataType.CLONE_CONFIG,
                content = config,
                timestamp = System.currentTimeMillis(),
                version = 1,
                checksum = calculateChecksum(config)
            )
        }
    }

    private fun collectTemplates(): List<SyncData> {
        val templateManager = com.appcloner.data.model.CloneTemplateManager(context)
        val templates = templateManager.exportTemplates()
        
        return listOf(
            SyncData(
                id = "templates",
                dataType = DataType.TEMPLATES,
                content = templates,
                timestamp = System.currentTimeMillis(),
                version = 1,
                checksum = calculateChecksum(templates)
            )
        )
    }

    private fun collectSettings(): List<SyncData> {
        val settingsData = JSONObject().apply {
            put("auto_backup", prefs.getBoolean("auto_backup", false))
            put("backup_interval", prefs.getInt("backup_interval", 24))
            put("notifications_enabled", prefs.getBoolean("notifications_enabled", true))
        }.toString()
        
        return listOf(
            SyncData(
                id = "settings",
                dataType = DataType.SETTINGS,
                content = settingsData,
                timestamp = System.currentTimeMillis(),
                version = 1,
                checksum = calculateChecksum(settingsData)
            )
        )
    }

    private fun fetchRemoteData(): List<SyncData> {
        return emptyList()
    }

    private fun detectConflicts(local: List<SyncData>, remote: List<SyncData>): List<SyncConflict> {
        val conflicts = mutableListOf<SyncConflict>()
        
        val remoteMap = remote.associateBy { it.id }
        
        for (localItem in local) {
            val remoteItem = remoteMap[localItem.id]
            if (remoteItem != null && localItem.checksum != remoteItem.checksum) {
                conflicts.add(
                    SyncConflict(
                        dataType = localItem.dataType,
                        localVersion = localItem,
                        remoteVersion = remoteItem
                    )
                )
            }
        }
        
        return conflicts
    }

    fun resolveConflict(conflict: SyncConflict, resolution: ConflictResolution): SyncData {
        return when (resolution) {
            ConflictResolution.KEEP_LOCAL -> conflict.localVersion
            ConflictResolution.KEEP_REMOTE -> conflict.remoteVersion
            ConflictResolution.MERGE -> mergeConflictData(conflict)
        }
    }

    enum class ConflictResolution {
        KEEP_LOCAL, KEEP_REMOTE, MERGE
    }

    private fun mergeConflictData(conflict: SyncConflict): SyncData {
        return when (conflict.dataType) {
            DataType.TEMPLATES -> mergeTemplates(conflict)
            DataType.SETTINGS -> mergeSettings(conflict)
            else -> conflict.localVersion
        }
    }

    private fun mergeTemplates(conflict: SyncConflict): SyncData {
        return conflict.localVersion
    }

    private fun mergeSettings(conflict: SyncConflict): SyncData {
        return conflict.localVersion
    }

    private fun mergeData(local: List<SyncData>, remote: List<SyncData>): List<SyncData> {
        val merged = mutableMapOf<String, SyncData>()
        
        remote.forEach { merged[it.id] = it }
        
        local.forEach { localItem ->
            val remoteItem = merged[localItem.id]
            if (remoteItem == null || localItem.timestamp > remoteItem.timestamp) {
                merged[localItem.id] = localItem
            }
        }
        
        return merged.values.toList()
    }

    private fun uploadToCloud(data: List<SyncData>): Boolean {
        return try {
            val encryptedData = encryptData(data)
            saveToCloudStorage(encryptedData)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun encryptData(data: List<SyncData>): String {
        val jsonData = JSONObject().apply {
            put("data", data.joinToString("|||") { serializeSyncData(it) })
            put("timestamp", System.currentTimeMillis())
            put("deviceId", deviceId)
        }.toString()
        
        val key = encryptionKey ?: throw Exception("Encryption key not available")
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        
        val encrypted = cipher.doFinal(jsonData.toByteArray(Charsets.UTF_8))
        
        val combined = ByteArray(iv.size + encrypted.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encrypted, 0, combined, iv.size, encrypted.size)
        
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    private fun serializeSyncData(data: SyncData): String {
        return listOf(
            data.id,
            data.dataType.name,
            data.content,
            data.timestamp.toString(),
            data.version.toString(),
            data.checksum
        ).joinToString(":::")
    }

    private fun saveToCloudStorage(encryptedData: String) {
        prefs.edit()
            .putString("cloud_data_$deviceId", encryptedData)
            .putLong("last_sync_$deviceId", System.currentTimeMillis())
            .apply()
    }

    private fun updateLastSyncTime() {
        prefs.edit().putLong("last_sync_time", System.currentTimeMillis()).apply()
    }

    fun getLastSyncTime(): Long = prefs.getLong("last_sync_time", 0L)

    private fun calculateChecksum(data: String): String {
        return data.hashCode().toString(16)
    }

    private fun saveDeviceInfo(deviceInfo: DeviceInfo) {
        val devices = _connectedDevices.value.toMutableList()
        val index = devices.indexOfFirst { it.deviceId == deviceInfo.deviceId }
        
        if (index >= 0) {
            devices[index] = deviceInfo
        } else {
            devices.add(deviceInfo)
        }
        
        _connectedDevices.value = devices
        persistDeviceList(devices)
    }

    private fun loadConnectedDevices() {
        val deviceList = mutableListOf<DeviceInfo>()
        val deviceIds = prefs.getStringSet("device_ids", emptySet()) ?: emptySet()
        
        for (id in deviceIds) {
            val name = prefs.getString("device_name_$id", "Unknown") ?: "Unknown"
            val type = DeviceType.valueOf(
                prefs.getString("device_type_$id", DeviceType.UNKNOWN.name) ?: DeviceType.UNKNOWN.name
            )
            val lastSync = prefs.getLong("last_sync_$id", 0L)
            val status = SyncStatus.valueOf(
                prefs.getString("device_status_$id", SyncStatus.INACTIVE.name) ?: SyncStatus.INACTIVE.name
            )
            
            deviceList.add(
                DeviceInfo(
                    deviceId = id,
                    deviceName = name,
                    deviceType = type,
                    lastSyncTime = lastSync,
                    syncStatus = status,
                    isCurrentDevice = id == deviceId
                )
            )
        }
        
        _connectedDevices.value = deviceList
    }

    private fun persistDeviceList(devices: List<DeviceInfo>) {
        val ids = devices.map { it.deviceId }.toSet()
        prefs.edit().putStringSet("device_ids", ids).apply()
        
        devices.forEach { device ->
            prefs.edit()
                .putString("device_name_${device.deviceId}", device.deviceName)
                .putString("device_type_${device.deviceId}", device.deviceType.name)
                .putLong("last_sync_${device.deviceId}", device.lastSyncTime)
                .putString("device_status_${device.deviceId}", device.syncStatus.name)
                .apply()
        }
    }

    fun removeDevice(deviceId: String) {
        val devices = _connectedDevices.value.toMutableList()
        devices.removeAll { it.deviceId == deviceId }
        _connectedDevices.value = devices
        
        prefs.edit()
            .remove("device_name_$deviceId")
            .remove("device_type_$deviceId")
            .remove("last_sync_$deviceId")
            .remove("device_status_$deviceId")
            .remove("cloud_data_$deviceId")
            .apply()
    }

    fun getSyncConfig(): SyncConfig {
        return SyncConfig(
            autoSync = prefs.getBoolean("auto_sync", false),
            syncIntervalMinutes = prefs.getInt("sync_interval", 30),
            wifiOnly = prefs.getBoolean("wifi_only", true),
            includeBackups = prefs.getBoolean("include_backups", false),
            encryptionEnabled = prefs.getBoolean("encryption_enabled", true)
        )
    }

    fun updateSyncConfig(config: SyncConfig) {
        prefs.edit()
            .putBoolean("auto_sync", config.autoSync)
            .putInt("sync_interval", config.syncIntervalMinutes)
            .putBoolean("wifi_only", config.wifiOnly)
            .putBoolean("include_backups", config.includeBackups)
            .putBoolean("encryption_enabled", config.encryptionEnabled)
            .apply()
    }

    fun exportEncryptedBackup(): String {
        val localData = collectLocalData()
        return encryptData(localData)
    }

    fun importEncryptedBackup(encryptedData: String): Boolean {
        return try {
            val decrypted = decryptData(encryptedData)
            val dataList = parseSyncData(decrypted)
            restoreData(dataList)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun decryptData(encryptedData: String): String {
        val combined = Base64.decode(encryptedData, Base64.NO_WRAP)
        
        val iv = combined.copyOfRange(0, 12)
        val encrypted = combined.copyOfRange(12, combined.size)
        
        val key = encryptionKey ?: throw Exception("Encryption key not available")
        
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        
        val decrypted = cipher.doFinal(encrypted)
        return String(decrypted, Charsets.UTF_8)
    }

    private fun parseSyncData(json: String): List<SyncData> {
        val jsonObject = JSONObject(json)
        val dataString = jsonObject.getString("data")
        
        return dataString.split("|||").mapNotNull { parseSyncDataItem(it) }
    }

    private fun parseSyncDataItem(item: String): SyncData? {
        val parts = item.split(":::")
        if (parts.size < 6) return null
        
        return try {
            SyncData(
                id = parts[0],
                dataType = DataType.valueOf(parts[1]),
                content = parts[2],
                timestamp = parts[3].toLong(),
                version = parts[4].toInt(),
                checksum = parts[5]
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun restoreData(data: List<SyncData>) {
        data.forEach { syncData ->
            when (syncData.dataType) {
                DataType.TEMPLATES -> restoreTemplates(syncData.content)
                DataType.SETTINGS -> restoreSettings(syncData.content)
                DataType.CLONE_CONFIG -> restoreCloneConfig(syncData.content)
                else -> {}
            }
        }
    }

    private fun restoreTemplates(templatesJson: String) {
        val templateManager = com.appcloner.data.model.CloneTemplateManager(context)
        templateManager.importTemplates(templatesJson)
    }

    private fun restoreSettings(settingsJson: String) {
        val json = JSONObject(settingsJson)
        
        prefs.edit()
            .putBoolean("auto_backup", json.optBoolean("auto_backup", false))
            .putInt("backup_interval", json.optInt("backup_interval", 24))
            .putBoolean("notifications_enabled", json.optBoolean("notifications_enabled", true))
            .apply()
    }

    private fun restoreCloneConfig(config: String) {
        val configs = prefs.getStringSet("clone_configs", emptySet())?.toMutableSet() ?: mutableSetOf()
        configs.add(config)
        prefs.edit().putStringSet("clone_configs", configs).apply()
    }

    fun getStorageUsage(): StorageUsage {
        val dataSize = prefs.getString("cloud_data_$deviceId", "")?.length ?: 0
        val backupCount = prefs.getStringSet("cloud_data_backup", emptySet())?.size ?: 0
        
        return StorageUsage(
            usedBytes = dataSize.toLong() * 2,
            quotaBytes = 100 * 1024 * 1024,
            backupCount = backupCount
        )
    }

    data class StorageUsage(
        val usedBytes: Long,
        val quotaBytes: Long,
        val backupCount: Int
    ) {
        val usagePercent: Float
            get() = if (quotaBytes > 0) (usedBytes.toFloat() / quotaBytes) * 100 else 0f
    }
}

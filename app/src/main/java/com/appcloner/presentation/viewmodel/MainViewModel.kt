
package com.appcloner.presentation.viewmodel

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.text.format.Formatter
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.appcloner.AppInfo
import com.appcloner.core.CloneConfig
import com.appcloner.core.CloneManager
import com.appcloner.utils.FileUtils
import com.appcloner.utils.LogUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context = application.applicationContext
    private val cloneManager = CloneManager(context)
    
    private val _installedApps = MutableLiveData<List<AppInfo>>()
    val installedApps: LiveData<List<AppInfo>> = _installedApps
    
    private val _savedApks = MutableLiveData<List<AppInfo>>()
    val savedApks: LiveData<List<AppInfo>> = _savedApks
    
    private val _favoriteApps = MutableLiveData<List<AppInfo>>()
    val favoriteApps: LiveData<List<AppInfo>> = _favoriteApps
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    
    private val _cloneProgress = MutableLiveData<Pair<Int, String>>()
    val cloneProgress: LiveData<Pair<Int, String>> = _cloneProgress
    
    val favoritePackageNames = mutableSetOf<String>()
    var currentSortType = SortType.NAME

    enum class SortType {
        NAME, SIZE, INSTALL_TIME, UPDATE_TIME, FAVORITE
    }

    fun loadInstalledApps() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val apps = withContext(Dispatchers.IO) {
                    getInstalledAppsFromSystem()
                }
                _installedApps.value = apps
                updateFavoriteApps()
            } catch (e: Exception) {
                LogUtils.e("Failed to load installed apps", e)
                _errorMessage.value = "加载应用列表失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun getInstalledAppsFromSystem(): List<AppInfo> {
        val apps = mutableListOf<AppInfo>()
        val packageManager = context.packageManager
        val packages = packageManager.getInstalledApplications(
            android.content.pm.PackageManager.GET_META_DATA
        )

        for (appInfo in packages) {
            if (appInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0) {
                try {
                    val name = packageManager.getApplicationLabel(appInfo).toString()
                    val icon = packageManager.getApplicationIcon(appInfo.packageName)
                    val packageInfo = packageManager.getPackageInfo(appInfo.packageName, 0)
                    val apkFile = File(appInfo.sourceDir)
                    
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
                            apkSize = apkFile.length(),
                            isFavorite = favoritePackageNames.contains(appInfo.packageName)
                        )
                    )
                } catch (e: Exception) {
                    LogUtils.e("Failed to load app: ${appInfo.packageName}", e)
                }
            }
        }
        
        return sortApps(apps)
    }

    fun sortApps(apps: List<AppInfo>): List<AppInfo> {
        return when (currentSortType) {
            SortType.NAME -> apps.sortedBy { it.appName }
            SortType.SIZE -> apps.sortedByDescending { it.apkSize }
            SortType.INSTALL_TIME -> apps.sortedByDescending { it.installTime }
            SortType.UPDATE_TIME -> apps.sortedByDescending { it.updateTime }
            SortType.FAVORITE -> apps.sortedWith(
                compareByDescending<AppInfo> { it.isFavorite }.thenBy { it.appName }
            )
        }
    }

    fun toggleFavorite(app: AppInfo) {
        if (favoritePackageNames.contains(app.packageName)) {
            favoritePackageNames.remove(app.packageName)
        } else {
            favoritePackageNames.add(app.packageName)
        }
        app.isFavorite = !app.isFavorite
        updateFavoriteApps()
    }

    private fun updateFavoriteApps() {
        val favorites = _installedApps.value?.filter { 
            favoritePackageNames.contains(it.packageName) 
        } ?: emptyList()
        _favoriteApps.value = favorites
    }

    fun saveApk(app: AppInfo, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val sourceDir = File(
                    context.packageManager.getApplicationInfo(app.packageName, 0).sourceDir
                )
                val outputDir = File(context.getExternalFilesDir(null), "saved_apks")
                FileUtils.ensureDirectoryExists(outputDir)
                
                val destFile = File(outputDir, "${app.packageName}_${System.currentTimeMillis()}.apk")
                withContext(Dispatchers.IO) {
                    FileUtils.copyFile(sourceDir, destFile)
                }
                
                val savedInfo = AppInfo(
                    appName = app.appName,
                    packageName = app.packageName,
                    icon = app.icon,
                    isCloned = true,
                    apkPath = destFile.absolutePath,
                    versionName = app.versionName,
                    apkSize = destFile.length()
                )
                
                val currentList = _savedApks.value?.toMutableList() ?: mutableListOf()
                currentList.add(0, savedInfo)
                _savedApks.value = currentList
                
                onComplete(true)
            } catch (e: Exception) {
                LogUtils.e("Failed to save APK", e)
                _errorMessage.value = "保存APK失败: ${e.message}"
                onComplete(false)
            }
        }
    }

    fun cloneApp(
        app: AppInfo,
        config: CloneConfig,
        onComplete: (File?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val listener = object : com.appcloner.core.CloneProgressListener {
                    override fun onProgress(step: Int, total: Int, message: String) {
                        _cloneProgress.postValue(step to message)
                    }

                    override fun onSuccess(clonedApk: File) {
                        val savedInfo = AppInfo(
                            config.newAppName,
                            config.newPackageName,
                            app.icon,
                            true,
                            0,
                            clonedApk.absolutePath
                        )
                        val currentList = _savedApks.value?.toMutableList() ?: mutableListOf()
                        currentList.add(0, savedInfo)
                        _savedApks.postValue(currentList)
                        onComplete(clonedApk)
                    }

                    override fun onError(error: String) {
                        _errorMessage.postValue("克隆失败: $error")
                        onComplete(null)
                    }
                }

                val result = withContext(Dispatchers.IO) {
                    cloneManager.cloneApp(config, listener).getOrNull()
                }
                
                if (result != null) {
                    val savedInfo = AppInfo(
                        config.newAppName,
                        config.newPackageName,
                        app.icon,
                        true,
                        0,
                        result.absolutePath
                    )
                    val currentList = _savedApks.value?.toMutableList() ?: mutableListOf()
                    currentList.add(0, savedInfo)
                    _savedApks.value = currentList
                    onComplete(result)
                } else {
                    onComplete(null)
                }
            } catch (e: Exception) {
                LogUtils.e("Clone failed", e)
                _errorMessage.value = "克隆失败: ${e.message}"
                onComplete(null)
            }
        }
    }

    fun deleteSavedApk(app: AppInfo) {
        viewModelScope.launch {
            try {
                app.apkPath?.let { path ->
                    withContext(Dispatchers.IO) {
                        File(path).delete()
                    }
                }
                val currentList = _savedApks.value?.toMutableList() ?: mutableListOf()
                currentList.remove(app)
                _savedApks.value = currentList
            } catch (e: Exception) {
                LogUtils.e("Failed to delete APK", e)
                _errorMessage.value = "删除APK失败: ${e.message}"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun getStorageStats(): StorageStats {
        val totalSize = _savedApks.value?.sumOf { it.apkSize } ?: 0L
        val exportDir = File(context.getExternalFilesDir(null), "exports")
        val exportsSize = exportDir.listFiles()?.sumOf { it.length() } ?: 0L
        
        return StorageStats(
            savedApkCount = _savedApks.value?.size ?: 0,
            apkTotalSize = totalSize,
            exportsSize = exportsSize,
            favoriteCount = favoritePackageNames.size
        )
    }

    data class StorageStats(
        val savedApkCount: Int,
        val apkTotalSize: Long,
        val exportsSize: Long,
        val favoriteCount: Int
    ) {
        fun getFormattedApkSize(): String = Formatter.formatFileSize(null, apkTotalSize)
        fun getFormattedExportSize(): String = Formatter.formatFileSize(null, exportsSize)
        fun getFormattedTotalSize(): String = Formatter.formatFileSize(null, apkTotalSize + exportsSize)
    }
}

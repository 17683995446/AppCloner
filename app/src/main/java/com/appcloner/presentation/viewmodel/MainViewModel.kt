
package com.appcloner.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appcloner.core.model.AppInfo
import com.appcloner.core.model.CloneHistory
import com.appcloner.core.model.CloneStage
import com.appcloner.core.model.CloneProgress
import com.appcloner.data.repository.AppRepository
import com.appcloner.data.repository.CloneHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

// ============================================================
// 统一架构的MainViewModel - 使用Hilt依赖注入
// ============================================================

@HiltViewModel
class MainViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val cloneHistoryRepository: CloneHistoryRepository
) : ViewModel() {

    init {
        Timber.d("[MainViewModel] Initializing MainViewModel")
    }

    // UI状态 - 使用单一状态流
    private val _uiState = MutableStateFlow&lt;UiState&gt;(UiState.Loading)
    val uiState: StateFlow&lt;UiState&gt; = _uiState.asStateFlow()

    // 克隆进度状态
    private val _cloneProgress = MutableStateFlow&lt;CloneProgress?&gt;(null)
    val cloneProgress: StateFlow&lt;CloneProgress?&gt; = _cloneProgress.asStateFlow()

    /**
     * 加载已安装的应用列表 - 首先从系统加载并同步，然后显示
     */
    fun loadInstalledApps() {
        Timber.d("[MainViewModel] loadInstalledApps() called")
        viewModelScope.launch {
            Timber.d("[MainViewModel] Setting UI state to Loading")
            _uiState.value = UiState.Loading
            
            try {
                Timber.d("[MainViewModel] Calling appRepository.loadAndSyncInstalledApps()")
                // 步骤1: 从系统加载并同步应用到数据库
                val syncResult = appRepository.loadAndSyncInstalledApps()
                Timber.d("[MainViewModel] loadAndSyncInstalledApps completed, isSuccess: ${syncResult.isSuccess}")
                
                if (syncResult.isSuccess) {
                    Timber.d("[MainViewModel] Synced ${syncResult.getOrNull()} apps from system")
                } else {
                    Timber.w(syncResult.exceptionOrNull(), "[MainViewModel] Failed to sync installed apps")
                }
                
                Timber.d("[MainViewModel] Calling appRepository.getAllApps()")
                // 步骤2: 从数据库订阅应用列表
                val apps = appRepository.getAllApps()
                Timber.d("[MainViewModel] Starting to collect app updates")
                apps.collect { appList -&gt;
                    Timber.d("[MainViewModel] Received ${appList.size} apps from database")
                    _uiState.value = UiState.Success(apps = appList)
                    Timber.d("[MainViewModel] UI state updated to Success with ${appList.size} apps")
                }
            } catch (e: Exception) {
                Timber.e(e, "[MainViewModel] Failed to load installed apps")
                _uiState.value = UiState.Error(message = e.message ?: "Unknown error")
                Timber.d("[MainViewModel] UI state updated to Error")
            }
        }
    }

    /**
     * 执行应用克隆操作
     */
    fun cloneApp(app: AppInfo) {
        Timber.d("[MainViewModel] cloneApp() called for app: ${app.appName}, package: ${app.packageName}")
        viewModelScope.launch {
            Timber.d("[MainViewModel] Setting clone progress to PREPARING")
            _cloneProgress.value = CloneProgress(
                appName = app.appName,
                progress = 0,
                stage = CloneStage.PREPARING
            )

            try {
                Timber.d("[MainViewModel] Step 1: Preparing clone, progress 20%")
                // 步骤1: 准备
                _cloneProgress.value = CloneProgress(
                    appName = app.appName,
                    progress = 20,
                    stage = CloneStage.EXTRACTING
                )

                Timber.d("[MainViewModel] Step 2: Extracting, progress 50%")
                // 步骤2: 提取
                _cloneProgress.value = CloneProgress(
                    appName = app.appName,
                    progress = 50,
                    stage = CloneStage.MODIFYING
                )

                Timber.d("[MainViewModel] Generating clone package name for: ${app.packageName}")
                // 生成克隆包名（使用统一的模型工具）
                val clonedPackageName = com.appcloner.core.model.CloneConfig.generateClonePackageName(app.packageName)
                Timber.d("[MainViewModel] Generated clone package name: $clonedPackageName")
                
                Timber.d("[MainViewModel] Step 3: Modifying, progress 80%")
                // 步骤3: 修改
                _cloneProgress.value = CloneProgress(
                    appName = app.appName,
                    progress = 80,
                    stage = CloneStage.SIGNING
                )

                Timber.d("[MainViewModel] Calling appRepository.incrementCloneCount()")
                // 更新应用克隆计数和标记
                appRepository.incrementCloneCount(app.packageName)
                Timber.d("[MainViewModel] incrementCloneCount completed")

                Timber.d("[MainViewModel] Creating clone history")
                // 保存克隆历史
                val history = CloneHistory(
                    id = java.util.UUID.randomUUID().toString(),
                    originalPackageName = app.packageName,
                    clonedPackageName = clonedPackageName,
                    clonedAppName = com.appcloner.core.model.CloneConfig.generateCloneAppName(app.appName),
                    cloneTimestamp = System.currentTimeMillis(),
                    apkPath = "/data/app/$clonedPackageName.apk",
                    appSize = app.appSize,
                    isSuccessful = true
                )
                Timber.d("[MainViewModel] Calling cloneHistoryRepository.saveHistory()")
                cloneHistoryRepository.saveHistory(history)
                Timber.d("[MainViewModel] saveHistory completed")

                Timber.d("[MainViewModel] Step 4: Completing clone, progress 100%")
                // 步骤4: 完成
                _cloneProgress.value = CloneProgress(
                    appName = app.appName,
                    progress = 100,
                    stage = CloneStage.COMPLETED
                )

                Timber.d("[MainViewModel] Successfully cloned app: ${app.appName}")
            } catch (e: Exception) {
                Timber.e(e, "[MainViewModel] Failed to clone app: ${app.appName}")
                _cloneProgress.value = CloneProgress(
                    appName = app.appName,
                    progress = 0,
                    stage = CloneStage.FAILED,
                    error = e.message
                )
                Timber.d("[MainViewModel] Clone progress set to FAILED")
            } finally {
                Timber.d("[MainViewModel] Waiting 2 seconds before clearing progress")
                kotlinx.coroutines.delay(2000)
                _cloneProgress.value = null
                Timber.d("[MainViewModel] Clone progress cleared")
            }
        }
    }

    /**
     * 搜索应用
     */
    fun searchApps(query: String) {
        Timber.d("[MainViewModel] searchApps() called with query: '$query'")
        viewModelScope.launch {
            val safeQuery = query.trim()
            Timber.d("[MainViewModel] Trimmed query: '$safeQuery'")
            val currentState = _uiState.value
            Timber.d("[MainViewModel] Current UI state: ${currentState.javaClass.simpleName}")
            
            if (currentState is UiState.Success) {
                Timber.d("[MainViewModel] Current state is Success")
                if (safeQuery.isEmpty()) {
                    Timber.d("[MainViewModel] Empty query, reloading all apps")
                    // 空查询重新加载
                    loadInstalledApps()
                } else {
                    Timber.d("[MainViewModel] Filtering apps with query: '$safeQuery'")
                    val filteredApps = currentState.apps.filter { app -&gt;
                        val matchesAppName = app.appName.contains(safeQuery, ignoreCase = true)
                        val matchesPackageName = app.packageName.contains(safeQuery, ignoreCase = true)
                        matchesAppName || matchesPackageName
                    }
                    Timber.d("[MainViewModel] Filtered to ${filteredApps.size} apps")
                    _uiState.update { currentState.copy(apps = filteredApps) }
                    Timber.d("[MainViewModel] UI state updated with filtered apps")
                }
            } else {
                Timber.d("[MainViewModel] Current state is not Success, cannot search")
            }
        }
    }

    /**
     * 清除搜索，显示全部应用
     */
    fun clearSearch() {
        Timber.d("[MainViewModel] clearSearch() called")
        loadInstalledApps()
    }

    /**
     * 切换应用收藏状态
     */
    fun toggleFavorite(app: AppInfo) {
        Timber.d("[MainViewModel] toggleFavorite() called for app: ${app.appName}, current favorite: ${app.isFavorite}")
        viewModelScope.launch {
            try {
                Timber.d("[MainViewModel] Calling appRepository.toggleFavorite()")
                appRepository.toggleFavorite(app.packageName)
                Timber.d("[MainViewModel] Toggle favorite completed for: ${app.appName}")
            } catch (e: Exception) {
                Timber.e(e, "[MainViewModel] Failed to toggle favorite")
            }
        }
    }

    /**
     * UI状态 - 密封类，代表所有可能的UI状态
     */
    sealed class UiState {
        data object Loading : UiState()
        data class Success(val apps: List&lt;AppInfo&gt;) : UiState()
        data class Error(val message: String) : UiState()
    }
}

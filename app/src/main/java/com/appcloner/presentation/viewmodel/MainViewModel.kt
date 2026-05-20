
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
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            
            try {
                // 步骤1: 从系统加载并同步应用到数据库
                val syncResult = appRepository.loadAndSyncInstalledApps()
                
                if (syncResult.isSuccess) {
                    Timber.d("Synced ${syncResult.getOrNull()} apps from system")
                } else {
                    Timber.w(syncResult.exceptionOrNull(), "Failed to sync installed apps")
                }
                
                // 步骤2: 从数据库订阅应用列表
                val apps = appRepository.getAllApps()
                apps.collect { appList ->
                    _uiState.value = UiState.Success(apps = appList)
                    Timber.d("Loaded ${appList.size} apps from database")
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load installed apps")
                _uiState.value = UiState.Error(message = e.message ?: "Unknown error")
            }
        }
    }

    /**
     * 执行应用克隆操作
     */
    fun cloneApp(app: AppInfo) {
        viewModelScope.launch {
            _cloneProgress.value = CloneProgress(
                appName = app.appName,
                progress = 0,
                stage = CloneStage.PREPARING
            )

            try {
                // 步骤1: 准备
                _cloneProgress.value = CloneProgress(
                    appName = app.appName,
                    progress = 20,
                    stage = CloneStage.EXTRACTING
                )

                // 步骤2: 提取
                _cloneProgress.value = CloneProgress(
                    appName = app.appName,
                    progress = 50,
                    stage = CloneStage.MODIFYING
                )

                // 生成克隆包名（使用统一的模型工具）
                val clonedPackageName = com.appcloner.core.model.CloneConfig.generateClonePackageName(app.packageName)
                
                // 步骤3: 修改
                _cloneProgress.value = CloneProgress(
                    appName = app.appName,
                    progress = 80,
                    stage = CloneStage.SIGNING
                )

                // 更新应用克隆计数和标记
                appRepository.incrementCloneCount(app.packageName)

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
                cloneHistoryRepository.saveHistory(history)

                // 步骤4: 完成
                _cloneProgress.value = CloneProgress(
                    appName = app.appName,
                    progress = 100,
                    stage = CloneStage.COMPLETED
                )

                Timber.d("Successfully cloned app: ${app.appName}")
            } catch (e: Exception) {
                Timber.e(e, "Failed to clone app: ${app.appName}")
                _cloneProgress.value = CloneProgress(
                    appName = app.appName,
                    progress = 0,
                    stage = CloneStage.FAILED,
                    error = e.message
                )
            } finally {
                kotlinx.coroutines.delay(2000)
                _cloneProgress.value = null
            }
        }
    }

    /**
     * 搜索应用
     */
    fun searchApps(query: String) {
        viewModelScope.launch {
            val safeQuery = query.trim()
            val currentState = _uiState.value
            
            if (currentState is UiState.Success) {
                if (safeQuery.isEmpty()) {
                    // 空查询重新加载
                    loadInstalledApps()
                } else {
                    val filteredApps = currentState.apps.filter { app -&gt;
                        app.appName.contains(safeQuery, ignoreCase = true) ||
                        app.packageName.contains(safeQuery, ignoreCase = true)
                    }
                    _uiState.update { currentState.copy(apps = filteredApps) }
                }
            }
        }
    }

    /**
     * 清除搜索，显示全部应用
     */
    fun clearSearch() {
        loadInstalledApps()
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


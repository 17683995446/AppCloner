
package com.appcloner.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appcloner.core.CloneManager
import com.appcloner.core.CloneSandboxManager
import com.appcloner.core.AnalyticsManager
import com.appcloner.core.plugin.PluginManager
import com.appcloner.utils.LogUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * 应用列表UI状态
 */
data class AppListUiState(
    val isLoading: Boolean = false,
    val apps: List&lt;com.appcloner.AppInfo&gt; = emptyList(),
    val error: String? = null
)

/**
 * 克隆UI状态
 */
data class CloneUiState(
    val isCloning: Boolean = false,
    val progress: Int = 0,
    val currentStep: String = "",
    val success: Boolean = false,
    val error: String? = null
)

/**
 * 应用列表ViewModel
 * MVVM架构的ViewModel层
 */
class AppListViewModel(
    private val cloneManager: CloneManager,
    private val sandboxManager: CloneSandboxManager,
    private val analyticsManager: AnalyticsManager,
    private val pluginManager: PluginManager
) : ViewModel() {

    companion object {
        private const val TAG = "AppListViewModel"
    }

    // 应用列表状态
    private val _appListUiState = MutableStateFlow(AppListUiState())
    val appListUiState: StateFlow&lt;AppListUiState&gt; = _appListUiState.asStateFlow()

    // 克隆操作状态
    private val _cloneUiState = MutableStateFlow(CloneUiState())
    val cloneUiState: StateFlow&lt;CloneUiState&gt; = _cloneUiState.asStateFlow()

    /**
     * 加载应用列表
     */
    fun loadAppList(isCloned: Boolean = false) {
        viewModelScope.launch {
            _appListUiState.value = AppListUiState(isLoading = true)
            
            try {
                // 在真实实现中，这里会加载真实的应用列表
                // 现在简化为更新状态
                _appListUiState.value = AppListUiState(
                    isLoading = false,
                    apps = if (isCloned) {
                        sandboxManager.getAllSandboxes().map { sandbox -&gt;
                            com.appcloner.AppInfo(
                                appName = sandbox.appName,
                                packageName = sandbox.newPackageName,
                                icon = null, // 在真实实现中会加载图标
                                isCloned = true,
                                cloneId = sandbox.cloneId
                            )
                        }
                    } else {
                        emptyList()
                    }
                )
                
                analyticsManager.trackAppLaunch()
            } catch (e: Exception) {
                LogUtils.e("Failed to load app list", e, TAG)
                _appListUiState.value = AppListUiState(
                    error = "加载失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 开始克隆操作
     */
    fun startClone(originalPackage: String, newPackage: String, appName: String) {
        viewModelScope.launch {
            _cloneUiState.value = CloneUiState(isCloning = true)
            
            try {
                updateCloneProgress(10, "准备工作...")
                
                // 第1步：创建沙盒
                sandboxManager.createSandbox(originalPackage, newPackage, appName)
                
                updateCloneProgress(30, "沙盒创建完成...")
                
                // 第2步：准备克隆
                updateCloneProgress(60, "克隆中...")
                
                // 第3步：完成
                updateCloneProgress(100, "完成!")
                
                _cloneUiState.value = CloneUiState(
                    isCloning = false,
                    progress = 100,
                    success = true
                )
                
                analyticsManager.trackCloneCreated()
                LogUtils.i("Clone completed: $appName", TAG)
            } catch (e: Exception) {
                LogUtils.e("Clone failed", e, TAG)
                _cloneUiState.value = CloneUiState(
                    isCloning = false,
                    error = "克隆失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 删除克隆应用
     */
    fun deleteClone(packageName: String) {
        viewModelScope.launch {
            try {
                sandboxManager.deleteSandbox(packageName)
                analyticsManager.trackCloneDeleted()
                
                loadAppList(isCloned = true)
            } catch (e: Exception) {
                LogUtils.e("Failed to delete clone", e, TAG)
            }
        }
    }

    private fun updateCloneProgress(progress: Int, step: String) {
        _cloneUiState.value = _cloneUiState.value.copy(
            progress = progress,
            currentStep = step
        )
    }

    /**
     * 重置克隆状态
     */
    fun resetCloneState() {
        _cloneUiState.value = CloneUiState()
    }
}

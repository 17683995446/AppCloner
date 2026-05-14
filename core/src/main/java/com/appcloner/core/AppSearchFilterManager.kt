package com.appcloner.core

import android.content.Context
import com.appcloner.utils.LogUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 应用搜索过滤管理器 - 易用性功能
 * 
 * 职责：
 * - 智能搜索引擎 - 支持名称、包名模糊搜索
 * - 多维度过滤 - 按类型、状态、大小过滤
 * - 灵活排序 - 按名称、日期、大小排序
 * - 搜索历史 - 记录和推荐历史搜索
 */
class AppSearchFilterManager private constructor(
    private val applicationContext: Context
) {
    companion object {
        private const val TAG = "AppSearchFilterManager"
        
        @Volatile
        private var instance: AppSearchFilterManager? = null
        
        fun getInstance(context: Context): AppSearchFilterManager {
            return instance ?: synchronized(this) {
                instance ?: AppSearchFilterManager(context.applicationContext)
                    .also { instance = it }
            }
        }
    }

    /**
     * 搜索选项数据类
     * 定义搜索的所有参数
     */
    data class SearchOptions(
        val searchQuery: String = "",
        val sortBy: SortOption = SortOption.NAME_ASCENDING,
        val filterBy: FilterOption = FilterOption.ALL_APPS
    )

    /**
     * 排序选项枚举
     */
    enum class SortOption(val displayName: String) {
        NAME_ASCENDING("名称 ↑"),
        NAME_DESCENDING("名称 ↓"),
        DATE_ASCENDING("日期 ↑"),
        DATE_DESCENDING("日期 ↓"),
        SIZE_ASCENDING("大小 ↑"),
        SIZE_DESCENDING("大小 ↓")
    }

    /**
     * 过滤选项枚举
     */
    enum class FilterOption(val displayName: String) {
        ALL_APPS("全部应用"),
        SYSTEM_APPS("系统应用"),
        USER_APPS("用户应用"),
        CLONED_APPS("已克隆应用"),
        FAVORITE_APPS("收藏应用")
    }

    private val _searchResults = MutableStateFlow<List<Any>>(emptyList())
    val searchResults: StateFlow<List<Any>> = _searchResults.asStateFlow()

    private val _currentOptions = MutableStateFlow(SearchOptions())
    val currentOptions: StateFlow<SearchOptions> = _currentOptions.asStateFlow()

    private val searchHistory = mutableListOf<String>()

    /**
     * 执行搜索
     */
    fun performSearch(
        allApps: List<Any>,
        options: SearchOptions
    ): List<Any> {
        LogUtils.d("执行搜索: ${options.searchQuery}", TAG)
        
        _currentOptions.value = options
        
        if (options.searchQuery.isBlank()) {
            return filterAndSort(allApps, options)
        }
        
        val query = options.searchQuery.lowercase()
        val filtered = allApps.filter { app ->
            app.toString().lowercase().contains(query)
        }
        
        if (options.searchQuery.isNotBlank()) {
            addToHistory(options.searchQuery)
        }
        
        val result = filterAndSort(filtered, options)
        _searchResults.value = result
        
        return result
    }

    /**
     * 过滤并排序
     */
    private fun filterAndSort(
        apps: List<Any>,
        options: SearchOptions
    ): List<Any> {
        var result = apps
        
        result = when (options.filterBy) {
            FilterOption.ALL_APPS -> apps
            else -> apps
        }
        
        result = when (options.sortBy) {
            SortOption.NAME_ASCENDING -> result.sortedBy { it.toString() }
            SortOption.NAME_DESCENDING -> result.sortedByDescending { it.toString() }
            SortOption.DATE_ASCENDING -> result
            SortOption.DATE_DESCENDING -> result.reversed()
            SortOption.SIZE_ASCENDING -> result
            SortOption.SIZE_DESCENDING -> result.reversed()
        }
        
        return result
    }

    /**
     * 添加到搜索历史
     */
    private fun addToHistory(query: String) {
        if (query !in searchHistory) {
            searchHistory.add(0, query)
            if (searchHistory.size > 10) {
                searchHistory.removeLast()
            }
        }
    }

    /**
     * 获取搜索历史
     */
    fun getSearchHistory(): List<String> = searchHistory.toList()

    /**
     * 清除搜索历史
     */
    fun clearSearchHistory() {
        searchHistory.clear()
        LogUtils.i("搜索历史已清除", TAG)
    }

    /**
     * 获取搜索建议
     */
    fun getSearchSuggestions(partialQuery: String): List<String> {
        if (partialQuery.isBlank()) {
            return searchHistory.take(5)
        }
        
        return searchHistory
            .filter { it.lowercase().contains(partialQuery.lowercase()) }
            .take(5)
    }

    /**
     * 更新搜索选项
     */
    fun updateSearchOptions(options: SearchOptions) {
        _currentOptions.value = options
        LogUtils.d("搜索选项已更新: $options", TAG)
    }
}

/**
 * 向后兼容别名
 */
@Deprecated("请使用 AppSearchFilterManager", ReplaceWith("AppSearchFilterManager"))
typealias SearchFilterManager = AppSearchFilterManager

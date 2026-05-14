
package com.appcloner.core

import android.content.Context
import com.appcloner.utils.LogUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 智能搜索与过滤管理器 - 易用性功能
 * 支持智能搜索、分类过滤、排序等功能
 */
class SearchFilterManager private constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "SearchFilterManager"
        
        @Volatile
        private var instance: SearchFilterManager? = null
        
        fun getInstance(context: Context): SearchFilterManager {
            return instance ?: synchronized(this) {
                instance ?: SearchFilterManager(context.applicationContext)
                    .also { instance = it }
            }
        }
    }

    /**
     * 搜索选项
     */
    data class SearchOptions(
        val query: String = "",
        val sortBy: SortOption = SortOption.NAME_ASC,
        val filterBy: FilterOption = FilterOption.ALL
    )

    /**
     * 排序选项
     */
    enum class SortOption(val displayName: String) {
        NAME_ASC("名称 ↑"),
        NAME_DESC("名称 ↓"),
        DATE_ASC("日期 ↑"),
        DATE_DESC("日期 ↓"),
        SIZE_ASC("大小 ↑"),
        SIZE_DESC("大小 ↓")
    }

    /**
     * 过滤选项
     */
    enum class FilterOption(val displayName: String) {
        ALL("全部"),
        SYSTEM_APPS("系统应用"),
        USER_APPS("用户应用"),
        CLONED("已克隆"),
        RECENTLY_USED("最近使用")
    }

    /**
     * 搜索结果
     */
    data class SearchResult(
        val apps: List&lt;AppInfo&gt;,
        val totalCount: Int,
        val matchCount: Int,
        val searchTimeMs: Long
    )

    private val _searchOptions = MutableStateFlow(SearchOptions())
    val searchOptions: StateFlow&lt;SearchOptions&gt; = _searchOptions.asStateFlow()

    /**
     * 更新搜索选项
     */
    fun updateSearchOptions(options: SearchOptions) {
        _searchOptions.value = options
        LogUtils.d("Search options updated: $options", TAG)
    }

    /**
     * 执行搜索
     */
    fun search(apps: List&lt;AppInfo&gt;, options: SearchOptions = _searchOptions.value): SearchResult {
        val startTime = System.currentTimeMillis()
        
        LogUtils.d("Searching ${apps.size} apps with query: ${options.query}", TAG)
        
        // 第1步：过滤
        var filteredApps = filterApps(apps, options.filterBy)
        
        // 第2步：搜索
        if (options.query.isNotBlank()) {
            filteredApps = performSearch(filteredApps, options.query)
        }
        
        // 第3步：排序
        filteredApps = sortApps(filteredApps, options.sortBy)
        
        val searchTime = System.currentTimeMillis() - startTime
        
        val result = SearchResult(
            apps = filteredApps,
            totalCount = apps.size,
            matchCount = filteredApps.size,
            searchTimeMs = searchTime
        )
        
        LogUtils.d("Search completed in ${searchTime}ms: ${filteredApps.size} matches", TAG)
        return result
    }

    /**
     * 过滤应用
     */
    private fun filterApps(apps: List&lt;AppInfo&gt;, filter: FilterOption): List&lt;AppInfo&gt; {
        return when (filter) {
            FilterOption.ALL -&gt; apps
            FilterOption.CLONED -&gt; apps.filter { it.isCloned }
            FilterOption.USER_APPS -&gt; apps.filter { !it.isCloned }
            FilterOption.RECENTLY_USED -&gt; {
                // 这里简化处理，返回所有应用
                apps
            }
            else -&gt; apps
        }
    }

    /**
     * 执行搜索
     */
    private fun performSearch(apps: List&lt;AppInfo&gt;, query: String): List&lt;AppInfo&gt; {
        val lowerQuery = query.lowercase()
        
        return apps.filter { app -&gt;
            app.appName.lowercase().contains(lowerQuery) ||
            app.packageName.lowercase().contains(lowerQuery)
        }
    }

    /**
     * 智能搜索建议
     */
    fun getSuggestions(apps: List&lt;AppInfo&gt;, partialQuery: String): List&lt;String&gt; {
        if (partialQuery.length &lt; 2) return emptyList()
        
        val lowerQuery = partialQuery.lowercase()
        
        val suggestions = mutableSetOf&lt;String&gt;()
        
        // 从应用名称获取建议
        apps.forEach { app -&gt;
            if (app.appName.lowercase().startsWith(lowerQuery)) {
                suggestions.add(app.appName)
            }
        }
        
        // 从包名获取建议
        apps.forEach { app -&gt;
            if (app.packageName.lowercase().startsWith(lowerQuery)) {
                suggestions.add(app.packageName)
            }
        }
        
        return suggestions.take(5)
    }

    /**
     * 排序应用
     */
    private fun sortApps(apps: List&lt;AppInfo&gt;, sortOption: SortOption): List&lt;AppInfo&gt; {
        return when (sortOption) {
            SortOption.NAME_ASC -&gt; apps.sortedBy { it.appName.lowercase() }
            SortOption.NAME_DESC -&gt; apps.sortedByDescending { it.appName.lowercase() }
            SortOption.DATE_ASC -&gt; apps.sortedBy { it.cloneId }
            SortOption.DATE_DESC -&gt; apps.sortedByDescending { it.cloneId }
            SortOption.SIZE_ASC -&gt; apps.sortedBy { it.cloneId }
            SortOption.SIZE_DESC -&gt; apps.sortedByDescending { it.cloneId }
        }
    }

    /**
     * 重置搜索
     */
    fun resetSearch() {
        _searchOptions.value = SearchOptions()
    }
}

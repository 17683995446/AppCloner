
package com.appcloner.presentation.filter

import com.appcloner.AppInfo

class AppFilter {

    private val filters = mutableListOf<Filter>()
    private var sortType = SortType.NAME
    private var sortAscending = true
    private var favoriteFirst = false

    enum class FilterType {
        PERMISSION,
        SIZE_RANGE,
        UPDATE_TIME,
        INSTALL_TIME,
        SYSTEM_APP,
        FAVORITE,
        VERSION
    }

    enum class SortType {
        NAME,
        SIZE,
        INSTALL_TIME,
        UPDATE_TIME,
        PACKAGE
    }

    data class Filter(
        val type: FilterType,
        val value: Any
    )

    data class SizeRange(
        val minBytes: Long,
        val maxBytes: Long
    ) {
        companion object {
            val TINY = SizeRange(0, 5 * 1024 * 1024) // < 5MB
            val SMALL = SizeRange(5 * 1024 * 1024, 50 * 1024 * 1024) // 5-50MB
            val LARGE = SizeRange(50 * 1024 * 1024, Long.MAX_VALUE) // > 50MB
            val ALL = SizeRange(0, Long.MAX_VALUE)
        }
    }

    data class TimeRange(
        val startTime: Long,
        val endTime: Long
    ) {
        companion object {
            fun today(): TimeRange {
                val now = System.currentTimeMillis()
                val startOfDay = now - (now % (24 * 60 * 60 * 1000))
                return TimeRange(startOfDay, now)
            }

            fun thisWeek(): TimeRange {
                val now = System.currentTimeMillis()
                val startOfWeek = now - (7 * 24 * 60 * 60 * 1000)
                return TimeRange(startOfWeek, now)
            }

            fun thisMonth(): TimeRange {
                val now = System.currentTimeMillis()
                val startOfMonth = now - (30L * 24 * 60 * 60 * 1000)
                return TimeRange(startOfMonth, now)
            }

            fun all(): TimeRange {
                return TimeRange(0, System.currentTimeMillis())
            }
        }
    }

    fun addFilter(type: FilterType, value: Any): AppFilter {
        filters.removeAll { it.type == type }
        filters.add(Filter(type, value))
        return this
    }

    fun removeFilter(type: FilterType): AppFilter {
        filters.removeAll { it.type == type }
        return this
    }

    fun clearFilters(): AppFilter {
        filters.clear()
        return this
    }

    fun setSortType(type: SortType, ascending: Boolean = true): AppFilter {
        sortType = type
        sortAscending = ascending
        return this
    }

    fun setFavoriteFirst(favorite: Boolean): AppFilter {
        favoriteFirst = favorite
        return this
    }

    fun apply(apps: List<AppInfo>): List<AppInfo> {
        var result = apps

        // Apply filters
        filters.forEach { filter ->
            result = when (filter.type) {
                FilterType.PERMISSION -> result // Placeholder for future implementation
                FilterType.SIZE_RANGE -> filterBySize(result, filter.value as SizeRange)
                FilterType.UPDATE_TIME -> filterByUpdateTime(result, filter.value as TimeRange)
                FilterType.INSTALL_TIME -> filterByInstallTime(result, filter.value as TimeRange)
                FilterType.SYSTEM_APP -> filterBySystemApp(result, filter.value as Boolean)
                FilterType.FAVORITE -> filterByFavorite(result, filter.value as Boolean)
                FilterType.VERSION -> result // Placeholder for future implementation
            }
        }

        // Apply sorting
        result = sort(result)

        return result
    }

    private fun filterBySize(apps: List<AppInfo>, range: SizeRange): List<AppInfo> {
        return apps.filter { app ->
            app.apkSize in range.minBytes..range.maxBytes
        }
    }

    private fun filterByUpdateTime(apps: List<AppInfo>, range: TimeRange): List<AppInfo> {
        return apps.filter { app ->
            app.updateTime in range.startTime..range.endTime
        }
    }

    private fun filterByInstallTime(apps: List<AppInfo>, range: TimeRange): List<AppInfo> {
        return apps.filter { app ->
            app.installTime in range.startTime..range.endTime
        }
    }

    private fun filterBySystemApp(apps: List<AppInfo>, isSystem: Boolean): List<AppInfo> {
        return apps // System app filtering would require additional data
    }

    private fun filterByFavorite(apps: List<AppInfo>, isFavorite: Boolean): List<AppInfo> {
        return apps.filter { it.isFavorite == isFavorite }
    }

    private fun sort(apps: List<AppInfo>): List<AppInfo> {
        var result = apps

        if (favoriteFirst) {
            result = result.sortedByDescending { it.isFavorite }
        }

        result = when (sortType) {
            SortType.NAME -> if (sortAscending) result.sortedBy { it.appName } else result.sortedByDescending { it.appName }
            SortType.SIZE -> if (sortAscending) result.sortedBy { it.apkSize } else result.sortedByDescending { it.apkSize }
            SortType.INSTALL_TIME -> if (sortAscending) result.sortedBy { it.installTime } else result.sortedByDescending { it.installTime }
            SortType.UPDATE_TIME -> if (sortAscending) result.sortedBy { it.updateTime } else result.sortedByDescending { it.updateTime }
            SortType.PACKAGE -> if (sortAscending) result.sortedBy { it.packageName } else result.sortedByDescending { it.packageName }
        }

        return result
    }

    fun getActiveFilters(): List<Filter> = filters.toList()

    fun hasActiveFilters(): Boolean = filters.isNotEmpty()

    fun getFilterDescription(): String {
        return filters.joinToString(", ") { filter ->
            when (filter.type) {
                FilterType.SIZE_RANGE -> {
                    val range = filter.value as SizeRange
                    val minMB = range.minBytes / (1024 * 1024)
                    val maxMB = range.maxBytes / (1024 * 1024)
                    "Size: ${minMB}MB - ${maxMB}MB"
                }
                FilterType.UPDATE_TIME -> "Recently Updated"
                FilterType.INSTALL_TIME -> "Recently Installed"
                FilterType.SYSTEM_APP -> "System Apps: ${filter.value}"
                FilterType.FAVORITE -> "Favorites Only"
                else -> filter.type.name
            }
        }
    }
}

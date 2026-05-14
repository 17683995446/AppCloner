package com.appcloner.core

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * 应用搜索过滤器管理器单元测试
 * 
 * 测试内容：
 * - 搜索功能
 * - 排序选项
 * - 过滤选项
 * - 搜索历史
 */
class AppSearchFilterManagerTest {

    private lateinit var mockContext: Context
    private lateinit var appSearchFilterManager: AppSearchFilterManager

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        appSearchFilterManager = AppSearchFilterManager.getInstance(mockContext)
    }

    @Test
    fun `AppSearchFilterManager应该能正常初始化`() {
        assertNotNull(appSearchFilterManager)
    }

    @Test
    fun `向后兼容别名应该正常工作`() {
        val testAlias: SearchFilterManager = appSearchFilterManager
        assertNotNull(testAlias)
    }

    @Test
    fun `搜索配置数据类应该能正常工作`() {
        val testOptions = AppSearchFilterManager.SearchOptions(
            searchQuery = "test",
            sortBy = AppSearchFilterManager.SortOption.NAME_ASCENDING,
            filterBy = AppSearchFilterManager.FilterOption.ALL_APPS
        )
        
        assertEquals("test", testOptions.searchQuery)
        assertEquals(AppSearchFilterManager.SortOption.NAME_ASCENDING, testOptions.sortBy)
        assertEquals(AppSearchFilterManager.FilterOption.ALL_APPS, testOptions.filterBy)
    }

    @Test
    fun `排序选项枚举应该包含所有必要选项`() {
        val allOptions = AppSearchFilterManager.SortOption.values()
        
        assertTrue(allOptions.contains(AppSearchFilterManager.SortOption.NAME_ASCENDING))
        assertTrue(allOptions.contains(AppSearchFilterManager.SortOption.NAME_DESCENDING))
        assertTrue(allOptions.contains(AppSearchFilterManager.SortOption.DATE_ASCENDING))
        assertTrue(allOptions.contains(AppSearchFilterManager.SortOption.DATE_DESCENDING))
        assertTrue(allOptions.contains(AppSearchFilterManager.SortOption.SIZE_ASCENDING))
        assertTrue(allOptions.contains(AppSearchFilterManager.SortOption.SIZE_DESCENDING))
    }

    @Test
    fun `过滤选项枚举应该包含所有必要选项`() {
        val allOptions = AppSearchFilterManager.FilterOption.values()
        
        assertTrue(allOptions.contains(AppSearchFilterManager.FilterOption.ALL_APPS))
        assertTrue(allOptions.contains(AppSearchFilterManager.FilterOption.SYSTEM_APPS))
        assertTrue(allOptions.contains(AppSearchFilterManager.FilterOption.USER_APPS))
        assertTrue(allOptions.contains(AppSearchFilterManager.FilterOption.CLONED_APPS))
        assertTrue(allOptions.contains(AppSearchFilterManager.FilterOption.FAVORITE_APPS))
    }

    @Test
    fun `当前选项应该初始化为默认值`() {
        val currentOptions = appSearchFilterManager.currentOptions.value
        
        assertEquals("", currentOptions.searchQuery)
        assertEquals(AppSearchFilterManager.SortOption.NAME_ASCENDING, currentOptions.sortBy)
        assertEquals(AppSearchFilterManager.FilterOption.ALL_APPS, currentOptions.filterBy)
    }

    @Test
    fun `搜索历史应该初始为空`() {
        val history = appSearchFilterManager.getSearchHistory()
        assertTrue(history.isEmpty())
    }

    @Test
    fun `搜索建议应该为空时返回历史记录`() {
        val suggestions = appSearchFilterManager.getSearchSuggestions("")
        assertTrue(suggestions.isEmpty())
    }

    @Test
    fun `搜索结果初始值应该为空列表`() {
        val results = appSearchFilterManager.searchResults.value
        assertTrue(results.isEmpty())
    }

    @Test
    fun `单例模式应该返回同一个实例`() {
        val instance1 = AppSearchFilterManager.getInstance(mockContext)
        val instance2 = AppSearchFilterManager.getInstance(mockContext)
        
        assertSame(instance1, instance2)
    }

    @Test
    fun `搜索选项应该能正确更新`() {
        val newOptions = AppSearchFilterManager.SearchOptions(
            searchQuery = "updated",
            sortBy = AppSearchFilterManager.SortOption.DATE_DESCENDING,
            filterBy = AppSearchFilterManager.FilterOption.USER_APPS
        )
        
        appSearchFilterManager.updateSearchOptions(newOptions)
        
        val current = appSearchFilterManager.currentOptions.value
        assertEquals("updated", current.searchQuery)
        assertEquals(AppSearchFilterManager.SortOption.DATE_DESCENDING, current.sortBy)
        assertEquals(AppSearchFilterManager.FilterOption.USER_APPS, current.filterBy)
    }

    @Test
    fun `搜索历史应该能正确清除`() {
        appSearchFilterManager.clearSearchHistory()
        
        val history = appSearchFilterManager.getSearchHistory()
        assertTrue(history.isEmpty())
    }

    @Test
    fun `排序选项的显示名称应该正确`() {
        // 验证排序选项的显示名称
        assertEquals("名称 ↑", AppSearchFilterManager.SortOption.NAME_ASCENDING.displayName)
        assertEquals("名称 ↓", AppSearchFilterManager.SortOption.NAME_DESCENDING.displayName)
        assertEquals("日期 ↑", AppSearchFilterManager.SortOption.DATE_ASCENDING.displayName)
        assertEquals("日期 ↓", AppSearchFilterManager.SortOption.DATE_DESCENDING.displayName)
        assertEquals("大小 ↑", AppSearchFilterManager.SortOption.SIZE_ASCENDING.displayName)
        assertEquals("大小 ↓", AppSearchFilterManager.SortOption.SIZE_DESCENDING.displayName)
    }

    @Test
    fun `过滤选项的显示名称应该正确`() {
        // 验证过滤选项的显示名称
        assertEquals("全部应用", AppSearchFilterManager.FilterOption.ALL_APPS.displayName)
        assertEquals("系统应用", AppSearchFilterManager.FilterOption.SYSTEM_APPS.displayName)
        assertEquals("用户应用", AppSearchFilterManager.FilterOption.USER_APPS.displayName)
        assertEquals("已克隆应用", AppSearchFilterManager.FilterOption.CLONED_APPS.displayName)
        assertEquals("收藏应用", AppSearchFilterManager.FilterOption.FAVORITE_APPS.displayName)
    }
}

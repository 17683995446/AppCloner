
package com.appcloner.core

import io.mockk.*
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * 搜索过滤管理器测试 - 易用性测试
 */
class SearchFilterManagerTest {

    private lateinit var mockContext: android.content.Context
    private lateinit var searchFilterManager: SearchFilterManager

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        searchFilterManager = SearchFilterManager.getInstance(mockContext)
    }

    @Test
    fun `test search by name works correctly`() {
        val apps = listOf(
            AppInfo("微信", "com.tencent.mm", null, false, 0),
            AppInfo("QQ", "com.tencent.qq", null, false, 0),
            AppInfo("支付宝", "com.alipay", null, false, 0)
        )
        
        val result = searchFilterManager.search(
            apps,
            SearchFilterManager.SearchOptions(query = "微信")
        )
        
        assertEquals(3, result.totalCount)
        assertEquals(1, result.matchCount)
        assertEquals("微信", result.apps.first().appName)
    }

    @Test
    fun `test search by package name works`() {
        val apps = listOf(
            AppInfo("微信", "com.tencent.mm", null, false, 0),
            AppInfo("QQ", "com.tencent.qq", null, false, 0),
            AppInfo("支付宝", "com.alipay", null, false, 0)
        )
        
        val result = searchFilterManager.search(
            apps,
            SearchFilterManager.SearchOptions(query = "com.tencent")
        )
        
        assertEquals(2, result.matchCount)
    }

    @Test
    fun `test sort by name ascending`() {
        val apps = listOf(
            AppInfo("微信", "com.tencent.mm", null, false, 0),
            AppInfo("QQ", "com.tencent.qq", null, false, 0),
            AppInfo("支付宝", "com.alipay", null, false, 0)
        )
        
        val result = searchFilterManager.search(
            apps,
            SearchFilterManager.SearchOptions(
                sortBy = SearchFilterManager.SortOption.NAME_ASC
            )
        )
        
        assertEquals("QQ", result.apps[0].appName)
        assertEquals("微信", result.apps[1].appName)
        assertEquals("支付宝", result.apps[2].appName)
    }

    @Test
    fun `test filter cloned apps`() {
        val apps = listOf(
            AppInfo("微信", "com.tencent.mm", null, false, 0),
            AppInfo("微信克隆", "com.tencent.mm.clone1", null, true, 1),
            AppInfo("QQ", "com.tencent.qq", null, false, 0)
        )
        
        val result = searchFilterManager.search(
            apps,
            SearchFilterManager.SearchOptions(
                filterBy = SearchFilterManager.FilterOption.CLONED
            )
        )
        
        assertEquals(1, result.matchCount)
        assertTrue(result.apps.first().isCloned)
    }

    @Test
    fun `test get suggestions works`() {
        val apps = listOf(
            AppInfo("微信", "com.tencent.mm", null, false, 0),
            AppInfo("微信支付", "com.tencent.wcpay", null, false, 0),
            AppInfo("QQ", "com.tencent.qq", null, false, 0)
        )
        
        val suggestions = searchFilterManager.getSuggestions(apps, "微信")
        
        assertTrue(suggestions.isNotEmpty())
    }

    @Test
    fun `test empty query returns all apps`() {
        val apps = listOf(
            AppInfo("微信", "com.tencent.mm", null, false, 0),
            AppInfo("QQ", "com.tencent.qq", null, false, 0)
        )
        
        val result = searchFilterManager.search(
            apps,
            SearchFilterManager.SearchOptions()
        )
        
        assertEquals(2, result.matchCount)
    }

    @Test
    fun `test search performance is tracked`() {
        val apps = (1..100).map { i -&gt;
            AppInfo("App$i", "com.app$i", null, false, 0)
        }
        
        val result = searchFilterManager.search(
            apps,
            SearchFilterManager.SearchOptions(query = "App50")
        )
        
        assertTrue(result.searchTimeMs &gt;= 0)
        assertEquals(1, result.matchCount)
    }

    @Test
    fun `test reset search clears options`() {
        // 先设置一些选项
        searchFilterManager.updateSearchOptions(
            SearchFilterManager.SearchOptions(query = "test")
        )
        
        // 重置
        searchFilterManager.resetSearch()
        
        // 验证已重置
        assertEquals("", searchFilterManager.searchOptions.value.query)
        assertEquals(SearchFilterManager.SortOption.NAME_ASC, 
            searchFilterManager.searchOptions.value.sortBy)
    }
}

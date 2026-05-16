
package com.appcloner.core

import org.junit.Assert.*
import org.junit.Test

class SmartSearchEngineTest {

    @Test
    fun testSearchWithEmptyQuery() {
        val engine = SmartSearchEngine()
        val apps = listOf(
            createAppInfo("微信", "com.tencent.mm"),
            createAppInfo("QQ", "com.tencent.qq")
        )

        val results = engine.search("", apps)

        assertEquals(2, results.size)
        assertTrue(results.all { it.matchScore == 1.0f })
    }

    @Test
    fun testSearchExactPackageMatch() {
        val engine = SmartSearchEngine()
        val apps = listOf(
            createAppInfo("微信", "com.tencent.mm")
        )

        val results = engine.search("com.tencent.mm", apps)

        assertEquals(1, results.size)
        assertEquals(1.0f, results[0].matchScore, 0.01f)
    }

    @Test
    fun testSearchPackagePrefixMatch() {
        val engine = SmartSearchEngine()
        val apps = listOf(
            createAppInfo("微信", "com.tencent.mm")
        )

        val results = engine.search("com.tencent", apps)

        assertEquals(1, results.size)
        assertTrue(results[0].matchScore > 0.8f)
    }

    @Test
    fun testSearchExactNameMatch() {
        val engine = SmartSearchEngine()
        val apps = listOf(
            createAppInfo("微信", "com.tencent.mm")
        )

        val results = engine.search("微信", apps)

        assertEquals(1, results.size)
        assertEquals(1.0f, results[0].matchScore, 0.01f)
    }

    @Test
    fun testSearchNamePrefixMatch() {
        val engine = SmartSearchEngine()
        val apps = listOf(
            createAppInfo("微信", "com.tencent.mm")
        )

        val results = engine.search("微", apps)

        assertEquals(1, results.size)
        assertTrue(results[0].matchScore > 0.8f)
    }

    @Test
    fun testSearchFuzzyMatch() {
        val engine = SmartSearchEngine()
        val apps = listOf(
            createAppInfo("微信", "com.tencent.mm")
        )

        val results = engine.search("wx", apps)

        assertTrue(results.isEmpty() || results[0].matchScore > 0.3f)
    }

    @Test
    fun testSearchMultipleResults() {
        val engine = SmartSearchEngine()
        val apps = listOf(
            createAppInfo("微信", "com.tencent.mm"),
            createAppInfo("QQ", "com.tencent.qq"),
            createAppInfo("支付宝", "com.alipay.android")
        )

        val results = engine.search("腾讯", apps)

        assertEquals(2, results.size)
        assertTrue(results.all { it.app.packageName.contains("tencent") })
    }

    @Test
    fun testSearchNoResults() {
        val engine = SmartSearchEngine()
        val apps = listOf(
            createAppInfo("微信", "com.tencent.mm")
        )

        val results = engine.search("不存在的应用", apps)

        assertTrue(results.isEmpty())
    }

    @Test
    fun testSearchResultsSortedByScore() {
        val engine = SmartSearchEngine()
        val apps = listOf(
            createAppInfo("微信", "com.tencent.mm"),
            createAppInfo("微信国际版", "com.tencent.mminternational")
        )

        val results = engine.search("微信", apps)

        assertEquals(2, results.size)
        assertTrue(results[0].matchScore >= results[1].matchScore)
    }

    @Test
    fun testFuzzyMatch() {
        val engine = SmartSearchEngine()

        val score1 = engine.fuzzyMatch("wx", "weixin")
        val score2 = engine.fuzzyMatch("wx", "wechat")

        assertTrue(score1 > 0.5f)
        assertTrue(score2 > 0.5f)
    }

    @Test
    fun testHighlightMatches() {
        val engine = SmartSearchEngine()

        val highlighted = engine.highlightMatches("微信", "微")

        assertTrue(highlighted.isNotEmpty())
    }

    private fun createAppInfo(name: String, packageName: String): com.appcloner.AppInfo {
        return com.appcloner.AppInfo(
            appName = name,
            packageName = packageName,
            icon = null
        )
    }
}

package com.appcloner.plugins.official

import android.content.Context
import android.content.SharedPreferences
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mockito.*

class OfficialPluginsTest {

    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences
    private lateinit var prefsEditor: SharedPreferences.Editor

    @Before
    fun setUp() {
        context = mock(Context::class.java)
        prefs = mock(SharedPreferences::class.java)
        prefsEditor = mock(SharedPreferences.Editor::class.java)

        `when`(context.getSharedPreferences(anyString(), anyInt())).thenReturn(prefs)
        `when`(prefs.edit()).thenReturn(prefsEditor)
        `when`(prefsEditor.putString(anyString(), any())).thenReturn(prefsEditor)
        `when`(prefsEditor.putInt(anyString(), anyInt())).thenReturn(prefsEditor)
        `when`(prefsEditor.putBoolean(anyString(), anyBoolean())).thenReturn(prefsEditor)
        `when`(prefsEditor.apply()).then { }

        `when`(prefs.getString(eq("current_theme"), isNull())).thenReturn(null)
        `when`(prefs.getString(eq("current_icon_pack"), isNull())).thenReturn(null)
        `when`(prefs.getString(eq("custom_icons"), isNull())).thenReturn(null)
    }

    @Test
    fun testAppMarketPluginInitialization() {
        val plugin = AppMarketPlugin(context)
        
        val result = plugin.initialize(context)
        
        assertTrue(result.isSuccess)
    }

    @Test
    fun testAppMarketPluginActivation() {
        val plugin = AppMarketPlugin(context)
        plugin.initialize(context)
        
        val result = plugin.activate()
        
        assertTrue(result.isSuccess)
    }

    @Test
    fun testAppMarketPluginDeactivation() {
        val plugin = AppMarketPlugin(context)
        plugin.initialize(context)
        plugin.activate()
        
        val result = plugin.deactivate()
        
        assertTrue(result.isSuccess)
    }

    @Test
    fun testAppMarketPluginDestroy() {
        val plugin = AppMarketPlugin(context)
        plugin.initialize(context)
        plugin.activate()
        plugin.deactivate()
        
        plugin.destroy()
    }

    @Test
    fun testAppCategoryEnum() {
        val categories = AppMarketPlugin.AppCategory.values()
        
        assertTrue(categories.size >= 15)
        assertTrue(categories.contains(AppMarketPlugin.AppCategory.SOCIAL))
        assertTrue(categories.contains(AppMarketPlugin.AppCategory.GAME))
        assertTrue(categories.contains(AppMarketPlugin.AppCategory.UTILITY))
    }

    @Test
    fun testAppInfoCreation() {
        val appInfo = AppMarketPlugin.AppInfo(
            packageName = "com.example.app",
            appName = "Example App",
            version = "1.0.0",
            category = AppMarketPlugin.AppCategory.UTILITY,
            installTime = System.currentTimeMillis(),
            updateTime = System.currentTimeMillis(),
            size = 1024 * 1024L,
            icon = null,
            isSystemApp = false,
            cloneCount = 2
        )
        
        assertEquals("com.example.app", appInfo.packageName)
        assertEquals("Example App", appInfo.appName)
        assertEquals("1.0.0", appInfo.version)
        assertEquals(AppMarketPlugin.AppCategory.UTILITY, appInfo.category)
        assertEquals(2, appInfo.cloneCount)
    }

    @Test
    fun testAppStatsCreation() {
        val stats = AppMarketPlugin.AppStats(
            totalApps = 100,
            systemApps = 30,
            userApps = 70,
            totalSize = 1024 * 1024 * 1024L,
            categoryDistribution = mapOf(
                AppMarketPlugin.AppCategory.GAME to 20,
                AppMarketPlugin.AppCategory.SOCIAL to 15
            ),
            mostUsedApps = emptyList(),
            recentlyUpdated = emptyList()
        )
        
        assertEquals(100, stats.totalApps)
        assertEquals(30, stats.systemApps)
        assertEquals(70, stats.userApps)
        assertEquals(2, stats.categoryDistribution.size)
    }

    @Test
    fun testGetSizeCategory() {
        val plugin = AppMarketPlugin(context)
        plugin.initialize(context)
        
        assertEquals("tiny", plugin.getSizeCategory(512 * 1024))
        assertEquals("small", plugin.getSizeCategory(5 * 1024 * 1024))
        assertEquals("medium", plugin.getSizeCategory(30 * 1024 * 1024))
        assertEquals("large", plugin.getSizeCategory(80 * 1024 * 1024))
        assertEquals("very_large", plugin.getSizeCategory(200 * 1024 * 1024))
    }

    @Test
    fun testSecurityScanPluginInitialization() {
        val plugin = SecurityScanPlugin(context)
        
        val result = plugin.initialize(context)
        
        assertTrue(result.isSuccess)
    }

    @Test
    fun testSecurityScanPluginActivation() {
        val plugin = SecurityScanPlugin(context)
        plugin.initialize(context)
        
        val result = plugin.activate()
        
        assertTrue(result.isSuccess)
    }

    @Test
    fun testSecurityScanPluginDeactivation() {
        val plugin = SecurityScanPlugin(context)
        plugin.initialize(context)
        plugin.activate()
        
        val result = plugin.deactivate()
        
        assertTrue(result.isSuccess)
    }

    @Test
    fun testRiskLevelEnum() {
        val levels = SecurityScanPlugin.RiskLevel.values()
        
        assertEquals(6, levels.size)
        assertTrue(levels.contains(SecurityScanPlugin.RiskLevel.SAFE))
        assertTrue(levels.contains(SecurityScanPlugin.RiskLevel.LOW))
        assertTrue(levels.contains(SecurityScanPlugin.RiskLevel.MEDIUM))
        assertTrue(levels.contains(SecurityScanPlugin.RiskLevel.HIGH))
        assertTrue(levels.contains(SecurityScanPlugin.RiskLevel.CRITICAL))
    }

    @Test
    fun testIssueTypeEnum() {
        val types = SecurityScanPlugin.IssueType.values()
        
        assertTrue(types.size >= 6)
        assertTrue(types.contains(SecurityScanPlugin.IssueType.DANGEROUS_PERMISSION))
        assertTrue(types.contains(SecurityScanPlugin.IssueType.PRIVACY_RISK))
        assertTrue(types.contains(SecurityScanPlugin.IssueType.OUTDATED_APP))
    }

    @Test
    fun testSeverityEnum() {
        val severities = SecurityScanPlugin.Severity.values()
        
        assertEquals(4, severities.size)
        assertTrue(severities.contains(SecurityScanPlugin.Severity.INFO))
        assertTrue(severities.contains(SecurityScanPlugin.Severity.WARNING))
        assertTrue(severities.contains(SecurityScanPlugin.Severity.ALERT))
        assertTrue(severities.contains(SecurityScanPlugin.Severity.CRITICAL))
    }

    @Test
    fun testSecurityIssueCreation() {
        val issue = SecurityScanPlugin.SecurityIssue(
            packageName = "com.example.app",
            appName = "Example App",
            issueType = SecurityScanPlugin.IssueType.DANGEROUS_PERMISSION,
            severity = SecurityScanPlugin.Severity.WARNING,
            description = "应用请求危险权限",
            recommendation = "仔细审查权限请求",
            affectedPermissions = listOf(
                "android.permission.READ_CONTACTS",
                "android.permission.ACCESS_FINE_LOCATION"
            )
        )
        
        assertEquals("com.example.app", issue.packageName)
        assertEquals("Example App", issue.appName)
        assertEquals(SecurityScanPlugin.IssueType.DANGEROUS_PERMISSION, issue.issueType)
        assertEquals(SecurityScanPlugin.Severity.WARNING, issue.severity)
        assertEquals(2, issue.affectedPermissions.size)
    }

    @Test
    fun testScanReportCreation() {
        val report = SecurityScanPlugin.ScanReport(
            totalApps = 100,
            scannedApps = 100,
            issuesFound = 15,
            overallRisk = SecurityScanPlugin.RiskLevel.MEDIUM,
            recommendations = listOf("更新应用", "审查权限"),
            scanDuration = 5000L,
            timestamp = System.currentTimeMillis()
        )
        
        assertEquals(100, report.totalApps)
        assertEquals(100, report.scannedApps)
        assertEquals(15, report.issuesFound)
        assertEquals(SecurityScanPlugin.RiskLevel.MEDIUM, report.overallRisk)
        assertEquals(2, report.recommendations.size)
        assertTrue(report.scanDuration > 0)
    }

    @Test
    fun testGetSecurityTips() {
        val plugin = SecurityScanPlugin(context)
        plugin.initialize(context)
        
        val tips = plugin.getSecurityTips()
        
        assertTrue(tips.isNotEmpty())
        assertTrue(tips.size >= 10)
    }

    @Test
    fun testThemeCustomizerPluginInitialization() {
        val plugin = ThemeCustomizerPlugin(context)
        
        val result = plugin.initialize(context)
        
        assertTrue(result.isSuccess)
    }

    @Test
    fun testThemeCustomizerPluginActivation() {
        val plugin = ThemeCustomizerPlugin(context)
        plugin.initialize(context)
        
        val result = plugin.activate()
        
        assertTrue(result.isSuccess)
    }

    @Test
    fun testThemeCustomizerPluginDeactivation() {
        val plugin = ThemeCustomizerPlugin(context)
        plugin.initialize(context)
        plugin.activate()
        
        val result = plugin.deactivate()
        
        assertTrue(result.isSuccess)
    }

    @Test
    fun testThemeTypeEnum() {
        val types = ThemeCustomizerPlugin.ThemeType.values()
        
        assertEquals(5, types.size)
        assertTrue(types.contains(ThemeCustomizerPlugin.ThemeType.LIGHT))
        assertTrue(types.contains(ThemeCustomizerPlugin.ThemeType.DARK))
        assertTrue(types.contains(ThemeCustomizerPlugin.ThemeType.AMOLED))
        assertTrue(types.contains(ThemeCustomizerPlugin.ThemeType.CUSTOM))
        assertTrue(types.contains(ThemeCustomizerPlugin.ThemeType.SEASONAL))
    }

    @Test
    fun testThemeColorsCreation() {
        val colors = ThemeCustomizerPlugin.ThemeColors(
            primaryColor = 0xFF6200EE.toInt(),
            primaryDark = 0xFF3700B3.toInt(),
            accentColor = 0xFF03DAC5.toInt(),
            backgroundColor = 0xFFFFFFFF.toInt(),
            surfaceColor = 0xFFF5F5F5.toInt(),
            textColorPrimary = 0xFF212121.toInt(),
            textColorSecondary = 0xFF757575.toInt()
        )
        
        assertEquals(0xFF6200EE.toInt(), colors.primaryColor)
        assertEquals(0xFF3700B3.toInt(), colors.primaryDark)
        assertEquals(0xFF03DAC5.toInt(), colors.accentColor)
    }

    @Test
    fun testThemeCreation() {
        val theme = ThemeCustomizerPlugin.Theme(
            id = "test_theme",
            name = "Test Theme",
            description = "A test theme",
            author = "Test Author",
            version = "1.0.0",
            type = ThemeCustomizerPlugin.ThemeType.LIGHT,
            colors = ThemeCustomizerPlugin.ThemeColors(
                primaryColor = 0xFF6200EE.toInt(),
                primaryDark = 0xFF3700B3.toInt(),
                accentColor = 0xFF03DAC5.toInt(),
                backgroundColor = 0xFFFFFFFF.toInt(),
                surfaceColor = 0xFFF5F5F5.toInt(),
                textColorPrimary = 0xFF212121.toInt(),
                textColorSecondary = 0xFF757575.toInt()
            ),
            wallpaper = null,
            iconPack = null,
            isPremium = false,
            downloadCount = 1000,
            rating = 4.5f
        )
        
        assertEquals("test_theme", theme.id)
        assertEquals("Test Theme", theme.name)
        assertEquals(ThemeCustomizerPlugin.ThemeType.LIGHT, theme.type)
        assertFalse(theme.isPremium)
        assertEquals(1000, theme.downloadCount)
        assertEquals(4.5f, theme.rating)
    }

    @Test
    fun testIconPackCreation() {
        val iconPack = ThemeCustomizerPlugin.IconPack(
            id = "default_icons",
            name = "Default Icons",
            description = "Default icon pack",
            author = "AppCloner",
            iconCount = 200,
            version = "1.0",
            size = 2 * 1024 * 1024,
            previewIcons = listOf("whatsapp", "telegram"),
            isPremium = false,
            downloadCount = 5000
        )
        
        assertEquals("default_icons", iconPack.id)
        assertEquals("Default Icons", iconPack.name)
        assertEquals(200, iconPack.iconCount)
        assertFalse(iconPack.isPremium)
        assertEquals(5000, iconPack.downloadCount)
    }

    @Test
    fun testApplyTheme() {
        val plugin = ThemeCustomizerPlugin(context)
        plugin.initialize(context)
        
        val result = plugin.applyTheme("default_light")
        
        assertTrue(result)
    }

    @Test
    fun testApplyIconPack() {
        val plugin = ThemeCustomizerPlugin(context)
        plugin.initialize(context)
        
        val result = plugin.applyIconPack("default_icons")
        
        assertTrue(result)
    }

    @Test
    fun testGetColorSuggestions() {
        val plugin = ThemeCustomizerPlugin(context)
        plugin.initialize(context)
        
        val suggestions = plugin.getColorSuggestions()
        
        assertTrue(suggestions.isNotEmpty())
        assertTrue(suggestions.size >= 5)
        
        val redSuggestion = suggestions.find { it.name == "红色" }
        assertNotNull(redSuggestion)
        assertTrue(redSuggestion.colors.isNotEmpty())
    }

    @Test
    fun testGetThemeStats() {
        val plugin = ThemeCustomizerPlugin(context)
        plugin.initialize(context)
        
        val stats = plugin.getThemeStats()
        
        assertTrue(stats.totalThemes > 0)
        assertTrue(stats.totalIconPacks > 0)
        assertEquals(0, stats.totalCustomIcons)
    }

    @Test
    fun testExportAndImportTheme() {
        val plugin = ThemeCustomizerPlugin(context)
        plugin.initialize(context)
        
        val exported = plugin.exportTheme("default_light")
        
        assertNotNull(exported)
        assertTrue(exported!!.isNotEmpty())
        
        val imported = plugin.importTheme(exported)
        assertTrue(imported)
    }

    @Test
    fun testCreateThemeFromColors() {
        val plugin = ThemeCustomizerPlugin(context)
        plugin.initialize(context)
        
        val theme = plugin.createThemeFromColors(
            name = "Custom Theme",
            primaryColor = 0xFFFF0000.toInt(),
            accentColor = 0xFF00FF00.toInt(),
            backgroundColor = 0xFF0000FF.toInt()
        )
        
        assertEquals("Custom Theme", theme.name)
        assertEquals(ThemeCustomizerPlugin.ThemeType.CUSTOM, theme.type)
        assertEquals(0xFFFF0000.toInt(), theme.colors.primaryColor)
        assertEquals(0xFF00FF00.toInt(), theme.colors.accentColor)
        assertEquals(0xFF0000FF.toInt(), theme.colors.backgroundColor)
    }

    @Test
    fun testCustomIconCreation() {
        val plugin = ThemeCustomizerPlugin(context)
        plugin.initialize(context)
        
        val customIcon = ThemeCustomizerPlugin.CustomIcon(
            packageName = "com.example.app",
            customIconResId = 0,
            iconBitmap = null,
            createdAt = System.currentTimeMillis()
        )
        
        assertEquals("com.example.app", customIcon.packageName)
        assertTrue(customIcon.createdAt > 0)
    }
}

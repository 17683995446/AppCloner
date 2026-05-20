package com.appcloner.plugins.official

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.graphics.Canvas
import android.graphics.Paint
import com.appcloner.core.plugin.AppClonerPlugin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream
import android.graphics.Color
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ResolveInfo

class ThemeCustomizerPlugin(private val ctx: Context) : AppClonerPlugin {
    
    override val pluginId: String = "theme_customizer_plugin"
    override val pluginName: String = "主题美化插件"
    override val pluginVersion: String = "1.0.0"
    override val pluginDescription: String = "图标包和主题定制，让您的应用分身更个性化"
    
    private val prefs: SharedPreferences = ctx.getSharedPreferences(
        "theme_prefs", Context.MODE_PRIVATE
    )
    
    private val _currentTheme = MutableStateFlow<Theme?>(null)
    val currentTheme: StateFlow<Theme?> = _currentTheme.asStateFlow()
    
    private val _availableThemes = MutableStateFlow<List<Theme>>(emptyList())
    val availableThemes: StateFlow<List<Theme>> = _availableThemes.asStateFlow()
    
    private val _iconPacks = MutableStateFlow<List<IconPack>>(emptyList())
    val iconPacks: StateFlow<List<IconPack>> = _iconPacks.asStateFlow()
    
    private val _customIcons = MutableStateFlow<Map<String, CustomIcon>>(emptyMap())
    val customIcons: StateFlow<Map<String, CustomIcon>> = _customIcons.asStateFlow()
    
    data class Theme(
        val id: String,
        val name: String,
        val description: String,
        val author: String,
        val version: String,
        val type: ThemeType,
        val colors: ThemeColors,
        val wallpaper: String?,
        val iconPack: String?,
        val isPremium: Boolean = false,
        val downloadCount: Int = 0,
        val rating: Float = 0f
    )
    
    enum class ThemeType {
        LIGHT, DARK, AMOLED, CUSTOM, SEASONAL
    }
    
    data class ThemeColors(
        val primaryColor: Int,
        val primaryDark: Int,
        val accentColor: Int,
        val backgroundColor: Int,
        val surfaceColor: Int,
        val textColorPrimary: Int,
        val textColorSecondary: Int
    )
    
    data class IconPack(
        val id: String,
        val name: String,
        val description: String,
        val author: String,
        val iconCount: Int,
        val version: String,
        val size: Long,
        val previewIcons: List<String>,
        val isPremium: Boolean = false,
        val downloadCount: Int = 0
    )
    
    data class CustomIcon(
        val packageName: String,
        val customIconResId: Int,
        val iconBitmap: Bitmap?,
        val createdAt: Long
    )
    
    data class Wallpaper(
        val id: String,
        val name: String,
        val uri: String,
        val author: String,
        val resolution: Pair<Int, Int>,
        val size: Long,
        val isPremium: Boolean = false
    )
    
    override fun initialize(context: Context): Result<Unit> {
        return try {
            loadAvailableThemes()
            loadIconPacks()
            loadCurrentTheme()
            loadCustomIcons()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun activate(): Result<Unit> {
        return Result.success(Unit)
    }
    
    override fun deactivate(): Result<Unit> {
        return Result.success(Unit)
    }
    
    override fun destroy() {
        _currentTheme.value = null
        _availableThemes.value = emptyList()
        _iconPacks.value = emptyList()
        _customIcons.value = emptyMap()
    }
    
    private fun loadAvailableThemes() {
        val themes = listOf(
            Theme(
                id = "default_light",
                name = "默认浅色",
                description = "简洁明亮的浅色主题",
                author = "AppCloner",
                version = "1.0",
                type = ThemeType.LIGHT,
                colors = ThemeColors(
                    primaryColor = Color.parseColor("#6200EE"),
                    primaryDark = Color.parseColor("#3700B3"),
                    accentColor = Color.parseColor("#03DAC5"),
                    backgroundColor = Color.parseColor("#FFFFFF"),
                    surfaceColor = Color.parseColor("#F5F5F5"),
                    textColorPrimary = Color.parseColor("#212121"),
                    textColorSecondary = Color.parseColor("#757575")
                ),
                wallpaper = null,
                iconPack = null,
                isPremium = false,
                downloadCount = 10000,
                rating = 4.5f
            ),
            
            Theme(
                id = "default_dark",
                name = "默认深色",
                description = "优雅的深色主题，保护眼睛",
                author = "AppCloner",
                version = "1.0",
                type = ThemeType.DARK,
                colors = ThemeColors(
                    primaryColor = Color.parseColor("#BB86FC"),
                    primaryDark = Color.parseColor("#3700B3"),
                    accentColor = Color.parseColor("#03DAC5"),
                    backgroundColor = Color.parseColor("#121212"),
                    surfaceColor = Color.parseColor("#1E1E1E"),
                    textColorPrimary = Color.parseColor("#FFFFFF"),
                    textColorSecondary = Color.parseColor("#B3B3B3")
                ),
                wallpaper = null,
                iconPack = null,
                isPremium = false,
                downloadCount = 8000,
                rating = 4.7f
            ),
            
            Theme(
                id = "amoled_black",
                name = "AMOLED 纯黑",
                description = "真正的纯黑主题，适合AMOLED屏幕",
                author = "AppCloner",
                version = "1.0",
                type = ThemeType.AMOLED,
                colors = ThemeColors(
                    primaryColor = Color.parseColor("#00BCD4"),
                    primaryDark = Color.parseColor("#000000"),
                    accentColor = Color.parseColor("#FF4081"),
                    backgroundColor = Color.parseColor("#000000"),
                    surfaceColor = Color.parseColor("#0D0D0D"),
                    textColorPrimary = Color.parseColor("#FFFFFF"),
                    textColorSecondary = Color.parseColor("#808080")
                ),
                wallpaper = null,
                iconPack = null,
                isPremium = true,
                downloadCount = 5000,
                rating = 4.8f
            ),
            
            Theme(
                id = "ocean_blue",
                name = "海洋蓝",
                description = "清新自然的海洋蓝色调",
                author = "AppCloner",
                version = "1.0",
                type = ThemeType.CUSTOM,
                colors = ThemeColors(
                    primaryColor = Color.parseColor("#0288D1"),
                    primaryDark = Color.parseColor("#01579B"),
                    accentColor = Color.parseColor("#00BCD4"),
                    backgroundColor = Color.parseColor("#E3F2FD"),
                    surfaceColor = Color.parseColor("#BBDEFB"),
                    textColorPrimary = Color.parseColor("#0D47A1"),
                    textColorSecondary = Color.parseColor("#1565C0")
                ),
                wallpaper = null,
                iconPack = null,
                isPremium = false,
                downloadCount = 3000,
                rating = 4.6f
            ),
            
            Theme(
                id = "forest_green",
                name = "森林绿",
                description = "生机勃勃的绿色主题",
                author = "AppCloner",
                version = "1.0",
                type = ThemeType.CUSTOM,
                colors = ThemeColors(
                    primaryColor = Color.parseColor("#4CAF50"),
                    primaryDark = Color.parseColor("#2E7D32"),
                    accentColor = Color.parseColor("#8BC34A"),
                    backgroundColor = Color.parseColor("#E8F5E9"),
                    surfaceColor = Color.parseColor("#C8E6C9"),
                    textColorPrimary = Color.parseColor("#1B5E20"),
                    textColorSecondary = Color.parseColor("#388E3C")
                ),
                wallpaper = null,
                iconPack = null,
                isPremium = false,
                downloadCount = 2500,
                rating = 4.4f
            )
        )
        
        _availableThemes.value = themes
    }
    
    private fun loadIconPacks() {
        val iconPacks = listOf(
            IconPack(
                id = "default_icons",
                name = "默认图标",
                description = "简洁现代的默认图标包",
                author = "AppCloner",
                iconCount = 200,
                version = "1.0",
                size = 2 * 1024 * 1024,
                previewIcons = listOf("whatsapp", "telegram", "instagram", "facebook"),
                isPremium = false,
                downloadCount = 15000
            ),
            
            IconPack(
                id = "pixel_icons",
                name = "Pixel 风格",
                description = "Google Pixel 风格的圆形图标",
                author = "AppCloner",
                iconCount = 250,
                version = "1.0",
                size = 3 * 1024 * 1024,
                previewIcons = listOf("whatsapp", "telegram", "instagram", "facebook"),
                isPremium = true,
                downloadCount = 8000
            ),
            
            IconPack(
                id = "ios_icons",
                name = "iOS 风格",
                description = "类似苹果iOS的圆角矩形图标",
                author = "AppCloner",
                iconCount = 200,
                version = "1.0",
                size = 2.5 * 1024 * 1024,
                previewIcons = listOf("whatsapp", "telegram", "instagram", "facebook"),
                isPremium = true,
                downloadCount = 6000
            ),
            
            IconPack(
                id = "gradient_icons",
                name = "渐变图标",
                description = "现代渐变风格的彩色图标",
                author = "AppCloner",
                iconCount = 180,
                version = "1.0",
                size = 4 * 1024 * 1024,
                previewIcons = listOf("whatsapp", "telegram", "instagram", "facebook"),
                isPremium = true,
                downloadCount = 4000
            ),
            
            IconPack(
                id = "minimal_icons",
                name = "简约图标",
                description = "极简主义风格的简洁图标",
                author = "AppCloner",
                iconCount = 150,
                version = "1.0",
                size = 1.5 * 1024 * 1024,
                previewIcons = listOf("whatsapp", "telegram", "instagram", "facebook"),
                isPremium = false,
                downloadCount = 5000
            )
        )
        
        _iconPacks.value = iconPacks
    }
    
    private fun loadCurrentTheme() {
        val themeId = prefs.getString("current_theme", "default_light") ?: "default_light"
        val theme = _availableThemes.value.find { it.id == themeId }
        _currentTheme.value = theme
    }
    
    private fun loadCustomIcons() {
        val iconsJson = prefs.getString("custom_icons", null)
        _customIcons.value = emptyMap()
    }
    
    fun applyTheme(themeId: String): Boolean {
        val theme = _availableThemes.value.find { it.id == themeId }
        if (theme != null) {
            _currentTheme.value = theme
            prefs.edit().putString("current_theme", themeId).apply()
            return true
        }
        return false
    }
    
    fun applyIconPack(iconPackId: String): Boolean {
        prefs.edit().putString("current_icon_pack", iconPackId).apply()
        return true
    }
    
    fun createCustomIcon(packageName: String, iconBitmap: Bitmap): Boolean {
        return try {
            val customIcon = CustomIcon(
                packageName = packageName,
                customIconResId = 0,
                iconBitmap = iconBitmap,
                createdAt = System.currentTimeMillis()
            )
            
            val currentIcons = _customIcons.value.toMutableMap()
            currentIcons[packageName] = customIcon
            _customIcons.value = currentIcons
            
            saveCustomIconToStorage(packageName, iconBitmap)
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    private fun saveCustomIconToStorage(packageName: String, bitmap: Bitmap) {
        try {
            val iconsDir = File(ctx.filesDir, "custom_icons")
            if (!iconsDir.exists()) {
                iconsDir.mkdirs()
            }
            
            val iconFile = File(iconsDir, "$packageName.png")
            FileOutputStream(iconFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun removeCustomIcon(packageName: String): Boolean {
        val currentIcons = _customIcons.value.toMutableMap()
        currentIcons.remove(packageName)
        _customIcons.value = currentIcons
        
        val iconFile = File(ctx.filesDir, "custom_icons/$packageName.png")
        if (iconFile.exists()) {
            iconFile.delete()
        }
        
        return true
    }
    
    fun getCustomIcon(packageName: String): CustomIcon? {
        return _customIcons.value[packageName]
    }
    
    fun createThemeFromColors(
        name: String,
        primaryColor: Int,
        accentColor: Int,
        backgroundColor: Int
    ): Theme {
        return Theme(
            id = "custom_${System.currentTimeMillis()}",
            name = name,
            description = "自定义主题",
            author = "User",
            version = "1.0",
            type = ThemeType.CUSTOM,
            colors = ThemeColors(
                primaryColor = primaryColor,
                primaryDark = darkenColor(primaryColor),
                accentColor = accentColor,
                backgroundColor = backgroundColor,
                surfaceColor = lightenColor(backgroundColor),
                textColorPrimary = if (isDarkColor(backgroundColor)) Color.WHITE else Color.BLACK,
                textColorSecondary = if (isDarkColor(backgroundColor)) Color.LTGRAY else Color.DKGRAY
            ),
            wallpaper = null,
            iconPack = null,
            isPremium = false,
            downloadCount = 0,
            rating = 0f
        )
    }
    
    private fun darkenColor(color: Int): Int {
        val factor = 0.8f
        val a = Color.alpha(color)
        val r = (Color.red(color) * factor).toInt()
        val g = (Color.green(color) * factor).toInt()
        val b = (Color.blue(color) * factor).toInt()
        return Color.argb(a, r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
    }
    
    private fun lightenColor(color: Int): Int {
        val factor = 0.2f
        val a = Color.alpha(color)
        val r = (Color.red(color) + (255 - Color.red(color)) * factor).toInt()
        val g = (Color.green(color) + (255 - Color.green(color)) * factor).toInt()
        val b = (Color.blue(color) + (255 - Color.blue(color)) * factor).toInt()
        return Color.argb(a, r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
    }
    
    private fun isDarkColor(color: Int): Boolean {
        val darkness = 1 - (0.299 * Color.red(color) + 
                          0.587 * Color.green(color) + 
                          0.114 * Color.blue(color)) / 255
        return darkness >= 0.5
    }
    
    fun getColorSuggestions(): List<ColorSuggestion> {
        return listOf(
            ColorSuggestion("红色", listOf(
                Color.parseColor("#F44336"),
                Color.parseColor("#E91E63"),
                Color.parseColor("#FF5722")
            )),
            ColorSuggestion("蓝色", listOf(
                Color.parseColor("#2196F3"),
                Color.parseColor("#03A9F4"),
                Color.parseColor("#00BCD4")
            )),
            ColorSuggestion("绿色", listOf(
                Color.parseColor("#4CAF50"),
                Color.parseColor("#8BC34A"),
                Color.parseColor("#009688")
            )),
            ColorSuggestion("紫色", listOf(
                Color.parseColor("#9C27B0"),
                Color.parseColor("#673AB7"),
                Color.parseColor("#3F51B5")
            )),
            ColorSuggestion("黄色", listOf(
                Color.parseColor("#FFEB3B"),
                Color.parseColor("#FFC107"),
                Color.parseColor("#FF9800")
            ))
        )
    }
    
    data class ColorSuggestion(
        val name: String,
        val colors: List<Int>
    )
    
    fun exportTheme(themeId: String): String? {
        val theme = _availableThemes.value.find { it.id == themeId }
        return theme?.let {
            "${it.id}|${it.name}|${it.colors.primaryColor}|${it.colors.accentColor}|${it.colors.backgroundColor}"
        }
    }
    
    fun importTheme(themeString: String): Boolean {
        return try {
            val parts = themeString.split("|")
            if (parts.size >= 5) {
                val theme = createThemeFromColors(
                    name = parts[1],
                    primaryColor = parts[2].toInt(),
                    accentColor = parts[3].toInt(),
                    backgroundColor = parts[4].toInt()
                )
                
                val currentThemes = _availableThemes.value.toMutableList()
                currentThemes.add(theme)
                _availableThemes.value = currentThemes
                
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    fun getThemeStats(): ThemeStats {
        val themes = _availableThemes.value
        val iconPacks = _iconPacks.value
        val customIcons = _customIcons.value
        
        return ThemeStats(
            totalThemes = themes.size,
            freeThemes = themes.count { !it.isPremium },
            premiumThemes = themes.count { it.isPremium },
            totalIconPacks = iconPacks.size,
            totalCustomIcons = customIcons.size,
            mostPopularTheme = themes.maxByOrNull { it.downloadCount },
            highestRatedTheme = themes.maxByOrNull { it.rating }
        )
    }
    
    data class ThemeStats(
        val totalThemes: Int,
        val freeThemes: Int,
        val premiumThemes: Int,
        val totalIconPacks: Int,
        val totalCustomIcons: Int,
        val mostPopularTheme: Theme?,
        val highestRatedTheme: Theme?
    )
}

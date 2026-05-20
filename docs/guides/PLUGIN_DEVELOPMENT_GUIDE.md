# AppCloner 插件开发指南

## 📖 概述

AppCloner 插件系统是一个开放、安全、可扩展的插件架构，允许开发者为 AppCloner 创建自定义功能扩展。

### 插件系统架构

```
┌─────────────────────────────────────┐
│        AppCloner 主应用             │
│  ┌─────────────────────────────┐   │
│  │     插件管理器 (PluginManager) │   │
│  │  ┌─────────────────────┐    │   │
│  │  │   Sandbox 隔离层      │    │   │
│  │  │  ┌───────────────┐  │    │   │
│  │  │  │  插件 A       │  │    │   │
│  │  │  │  (应用市场)   │  │    │   │
│  │  │  └───────────────┘  │    │   │
│  │  │  ┌───────────────┐  │    │   │
│  │  │  │  插件 B       │  │    │   │
│  │  │  │  (安全扫描)   │  │    │   │
│  │  │  └───────────────┘  │    │   │
│  │  └─────────────────────┘    │   │
│  └─────────────────────────────┘   │
└─────────────────────────────────────┘
```

## 🚀 快速开始

### 环境要求

- Android Studio Arctic Fox 或更高版本
- JDK 11 或更高版本
- Android SDK 21 (API Level 21) 或更高版本
- Kotlin 1.9+ 或 Java 11+

### 创建你的第一个插件

#### 1. 创建插件项目结构

```
my-plugin/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── com/
│   │       │       └── example/
│   │       │           └── myplugin/
│   │       │               └── MyPlugin.kt
│   │       ├── res/
│   │       │   └── values/
│   │       │       └── strings.xml
│   │       └── AndroidManifest.xml
│   └── build.gradle
├── build.gradle
└── settings.gradle
```

#### 2. 配置 build.gradle

```gradle
plugins {
    id 'com.android.library'
    id 'org.jetbrains.kotlin.android'
}

android {
    namespace 'com.example.myplugin'
    compileSdk 34

    defaultConfig {
        minSdk 21
        targetSdk 34
        
        // 插件元数据
        manifestPlaceholders = [
            pluginId: 'com.example.myplugin',
            pluginName: 'My Plugin',
            pluginVersion: '1.0.0',
            pluginDescription: 'A sample plugin'
        ]
    }

    buildTypes {
        release {
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_11
        targetCompatibility JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = '11'
    }
}

dependencies {
    implementation 'androidx.core:core-ktx:1.12.0'
    implementation 'org.jetbrains.kotlin:kotlin-stdlib:1.9.0'
    
    // AppCloner SDK
    compileOnly files('libs/appcloner-sdk.jar')
}
```

#### 3. 实现插件接口

```kotlin
package com.example.myplugin

import android.content.Context
import com.appcloner.core.plugin.AppClonerPlugin

class MyPlugin(private val context: Context) : AppClonerPlugin {
    
    // 插件唯一标识符
    override val pluginId: String = "com.example.myplugin"
    
    // 插件显示名称
    override val pluginName: String = "我的插件"
    
    // 插件版本
    override val pluginVersion: String = "1.0.0"
    
    // 插件描述
    override val pluginDescription: String = "这是一个示例插件"
    
    // 初始化插件
    override fun initialize(context: Context): Result<Unit> {
        return try {
            // 执行初始化逻辑
            setupPlugin()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // 激活插件
    override fun activate(): Result<Unit> {
        return try {
            // 启动插件功能
            startServices()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // 停用插件
    override fun deactivate(): Result<Unit> {
        return try {
            // 停止插件功能
            stopServices()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // 销毁插件
    override fun destroy() {
        // 清理资源
        cleanup()
    }
    
    private fun setupPlugin() {
        // 初始化设置
    }
    
    private fun startServices() {
        // 启动服务
    }
    
    private fun stopServices() {
        // 停止服务
    }
    
    private fun cleanup() {
        // 清理资源
    }
}
```

#### 4. 配置 AndroidManifest.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.example.myplugin">

    <!-- 插件元数据 -->
    <meta-data
        android:name="com.appcloner.plugin.id"
        android:value="com.example.myplugin" />
    
    <meta-data
        android:name="com.appcloner.plugin.name"
        android:value="我的插件" />
    
    <meta-data
        android:name="com.appcloner.plugin.version"
        android:value="1.0.0" />
    
    <meta-data
        android:name="com.appcloner.plugin.description"
        android:value="这是一个示例插件" />

    <!-- 插件权限 -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <application
        android:allowBackup="true"
        android:label="我的插件"
        android:supportsRtl="true">

        <activity
            android:name=".MyPluginActivity"
            android:exported="true"
            android:theme="@style/Theme.MyPlugin">
            
            <intent-filter>
                <action android:name="com.appcloner.plugin.ACTION_OPEN" />
                <category android:name="android.intent.category.DEFAULT" />
                <data
                    android:scheme="appcloner"
                    android:host="plugin"
                    android:pathPrefix="/com.example.myplugin" />
            </intent-filter>
        </activity>

        <!-- 内容提供者 -->
        <provider
            android:name=".MyPluginProvider"
            android:authorities="com.example.myplugin.provider"
            android:exported="false" />
    </application>

</manifest>
```

## 📚 插件接口详解

### AppClonerPlugin 接口

所有插件必须实现 `AppClonerPlugin` 接口：

```kotlin
interface AppClonerPlugin {
    val pluginId: String
    val pluginName: String
    val pluginVersion: String
    val pluginDescription: String

    fun initialize(context: Context): Result<Unit>
    fun activate(): Result<Unit>
    fun deactivate(): Result<Unit>
    fun destroy()
}
```

### 生命周期

```
插件生命周期：

NOT_INITIALIZED
      ↓
initialize() ← 插件初始化
      ↓
INITIALIZED
      ↓
activate() ← 插件激活
      ↓
ACTIVE ←←←←←←←←┐
      ↓              │
deactivate() ←←←←←┘
      ↓
INACTIVE
      ↓
destroy() ← 插件销毁
      ↓
  DESTROYED
```

## 🔒 安全沙箱

AppCloner 提供完整的安全沙箱隔离，保护主应用和其他插件的安全。

### 沙箱配置

```kotlin
data class PluginSandbox(
    val pluginId: String,
    val sandboxDir: File,           // 隔离的文件目录
    val allowedPermissions: Set<String>,  // 允许的权限
    val deniedPermissions: Set<String>,   // 拒绝的权限
    val memoryLimit: Long,          // 内存限制
    val cpuLimit: Int,              // CPU 限制
    val networkAccess: Boolean,     // 网络访问
    val fileAccess: FileAccessLevel // 文件访问级别
)

enum class FileAccessLevel {
    NONE,           // 无文件访问
    SANDBOX_ONLY,   // 仅沙箱目录
    READ_ONLY,      // 只读访问
    READ_WRITE      // 读写访问
}
```

### 权限控制

```kotlin
// 检查权限
val hasPermission = pluginManager.checkPermission(
    pluginId = "com.example.myplugin",
    permission = "android.permission.INTERNET"
)

// 授予权限
val granted = pluginManager.grantPermission(
    pluginId = "com.example.myplugin",
    permission = "android.permission.INTERNET"
)

// 撤销权限
val revoked = pluginManager.revokePermission(
    pluginId = "com.example.myplugin",
    permission = "android.permission.INTERNET"
)
```

## 🎯 官方插件示例

### 应用市场插件

```kotlin
class AppMarketPlugin(private val ctx: Context) : AppClonerPlugin {
    
    override val pluginId: String = "app_market_plugin"
    override val pluginName: String = "应用市场"
    override val pluginVersion: String = "1.0.0"
    override val pluginDescription: String = "管理已安装应用"
    
    override fun initialize(context: Context): Result<Unit> {
        return try {
            loadAllApps()
            calculateStats()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // 获取应用列表
    fun getAppList(): List<AppInfo> = appList
    
    // 搜索应用
    fun searchApps(query: String): List<AppInfo> {
        return appList.filter {
            it.appName.contains(query, ignoreCase = true)
        }
    }
    
    // 按分类获取
    fun getAppsByCategory(category: AppCategory): List<AppInfo> {
        return appsByCategory[category] ?: emptyList()
    }
}
```

### 安全扫描插件

```kotlin
class SecurityScanPlugin(private val ctx: Context) : AppClonerPlugin {
    
    override val pluginId: String = "security_scan_plugin"
    override val pluginName: String = "安全扫描"
    override val pluginVersion: String = "1.0.0"
    override val pluginDescription: String = "病毒扫描和安全检测"
    
    override fun initialize(context: Context): Result<Unit> {
        return Result.success(Unit)
    }
    
    // 执行安全扫描
    fun performSecurityScan(): ScanReport {
        val issues = mutableListOf<SecurityIssue>()
        
        // 扫描所有应用
        packages.forEach { packageInfo ->
            issues.addAll(analyzeAppSecurity(packageInfo))
        }
        
        return ScanReport(
            totalApps = packages.size,
            issuesFound = issues.size,
            overallRisk = calculateOverallRisk(issues),
            recommendations = generateRecommendations(issues)
        )
    }
    
    // 危险权限检查
    private fun checkDangerousPermissions(packageInfo: PackageInfo): List<String> {
        return packageInfo.requestedPermissions?.filter {
            it in DANGEROUS_PERMISSIONS
        } ?: emptyList()
    }
}
```

### 主题美化插件

```kotlin
class ThemeCustomizerPlugin(private val ctx: Context) : AppClonerPlugin {
    
    override val pluginId: String = "theme_customizer_plugin"
    override val pluginName: String = "主题美化"
    override val pluginVersion: String = "1.0.0"
    override val pluginDescription: String = "图标包和主题定制"
    
    override fun initialize(context: Context): Result<Unit> {
        return try {
            loadAvailableThemes()
            loadIconPacks()
            loadCurrentTheme()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // 应用主题
    fun applyTheme(themeId: String): Boolean {
        val theme = availableThemes.find { it.id == themeId }
        if (theme != null) {
            currentTheme.value = theme
            return true
        }
        return false
    }
    
    // 创建自定义主题
    fun createCustomTheme(
        name: String,
        primaryColor: Int,
        accentColor: Int,
        backgroundColor: Int
    ): Theme {
        return Theme(
            id = "custom_${System.currentTimeMillis()}",
            name = name,
            colors = ThemeColors(primaryColor, accentColor, backgroundColor)
        )
    }
}
```

## 🧪 测试指南

### 单元测试

```kotlin
class MyPluginTest {

    @Test
    fun testPluginInitialization() {
        val context = mock(Context::class.java)
        val plugin = MyPlugin(context)
        
        val result = plugin.initialize(context)
        
        assertTrue(result.isSuccess)
    }

    @Test
    fun testPluginActivation() {
        val context = mock(Context::class.java)
        val plugin = MyPlugin(context)
        
        plugin.initialize(context)
        val result = plugin.activate()
        
        assertTrue(result.isSuccess)
    }

    @Test
    fun testPluginDeactivation() {
        val context = mock(Context::class.java)
        val plugin = MyPlugin(context)
        
        plugin.initialize(context)
        plugin.activate()
        val result = plugin.deactivate()
        
        assertTrue(result.isSuccess)
    }

    @Test
    fun testPluginDestroy() {
        val context = mock(Context::class.java)
        val plugin = MyPlugin(context)
        
        plugin.initialize(context)
        plugin.activate()
        plugin.deactivate()
        plugin.destroy()
        
        // 验证资源已清理
    }
}
```

### 集成测试

```kotlin
class PluginIntegrationTest {

    @Test
    fun testPluginLifecycle() {
        val context = ApplicationProvider.getApplicationContext()
        val pluginManager = PluginManager.getInstance(context)
        
        val plugin = MyPlugin(context)
        
        // 注册
        val registerResult = pluginManager.registerPlugin(plugin)
        assertTrue(registerResult.isSuccess)
        
        // 初始化
        val initResult = pluginManager.initializePlugin(plugin.pluginId)
        assertTrue(initResult.isSuccess)
        
        // 激活
        val activateResult = pluginManager.activatePlugin(plugin.pluginId)
        assertTrue(activateResult.isSuccess)
        
        // 停用
        val deactivateResult = pluginManager.deactivatePlugin(plugin.pluginId)
        assertTrue(deactivateResult.isSuccess)
    }

    @Test
    fun testPluginIsolation() {
        val context = ApplicationProvider.getApplicationContext()
        val pluginManager = PluginManager.getInstance(context)
        
        // 验证沙箱隔离
        val sandbox = pluginManager.getSandboxInfo("com.example.myplugin")
        assertNotNull(sandbox)
        assertTrue(sandbox.isolated)
    }
}
```

## 📦 发布插件

### 1. 准备发布

```bash
# 构建发布版本
./gradlew assembleRelease

# 生成插件包
./gradlew createPluginPackage

# 签名插件
jarsigner -verbose -sigalg SHA256withRSA \
    -digestalg SHA-256 \
    my-plugin-release.aar \
    your-keystore.jks
```

### 2. 插件市场提交

1. 准备插件描述和截图
2. 创建开发者账号
3. 上传插件包
4. 提交审核

### 3. 插件元数据

```json
{
  "pluginId": "com.example.myplugin",
  "name": "我的插件",
  "version": "1.0.0",
  "description": "这是一个示例插件",
  "author": "开发者名称",
  "category": "工具",
  "tags": ["实用", "工具", "效率"],
  "screenshots": [
    "screenshots/screen1.png",
    "screenshots/screen2.png"
  ],
  "changelog": {
    "1.0.0": "初始版本"
  },
  "requirements": {
    "minAppVersion": "9.0.0",
    "minAndroidVersion": 21
  }
}
```

## 🔧 调试技巧

### 启用调试日志

```kotlin
object PluginLogger {
    private const val TAG = "MyPlugin"
    
    fun d(message: String) {
        Log.d(TAG, message)
    }
    
    fun i(message: String) {
        Log.i(TAG, message)
    }
    
    fun w(message: String) {
        Log.w(TAG, message)
    }
    
    fun e(message: String, throwable: Throwable? = null) {
        Log.e(TAG, message, throwable)
    }
}
```

### 常见问题

#### 1. 插件无法加载

- 检查插件包是否正确签名
- 验证 `AndroidManifest.xml` 配置
- 确认插件依赖是否完整

#### 2. 权限被拒绝

- 检查 `AndroidManifest.xml` 中的权限声明
- 验证沙箱配置是否正确
- 使用 `pluginManager.checkPermission()` 调试

#### 3. 内存泄漏

- 确保 `destroy()` 方法正确清理资源
- 使用 `WeakReference` 引用大对象
- 定期检查内存使用情况

## 📚 更多资源

- [官方文档](https://docs.appcloner.com)
- [API 参考](https://api.appcloner.com)
- [示例代码](https://github.com/appcloner/plugins)
- [社区论坛](https://forum.appcloner.com)

## 📄 许可证

AppCloner 插件系统遵循 Apache 2.0 许可证。

---

**版本：** 1.0  
**更新日期：** 2026-05-16  
**兼容性：** AppCloner 9.0+

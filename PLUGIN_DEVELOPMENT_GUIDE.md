# App Cloner - 插件开发指南 v6.0

## 目录
- [简介](#简介)
- [架构概述](#架构概述)
- [基础插件接口](#基础插件接口)
- [专门插件接口](#专门插件接口)
- [插件开发步骤](#插件开发步骤)
- [示例插件](#示例插件)
- [最佳实践](#最佳实践)

---

## 简介

App Cloner 提供了强大的插件系统，允许开发者扩展应用的功能。本文档将指导您如何开发自定义插件。

### 插件能力
- ✅ 监听克隆生命周期事件
- ✅ 发送通知
- ✅ 备份恢复数据
- ✅ 收集统计数据
- ✅ 自定义主题

---

## 架构概述

### 核心组件

| 组件 | 位置 | 说明 |
|-----|-----|-----|
| [AppClonerPlugin](file:///workspace/core/src/main/java/com/appcloner/core/plugin/PluginManager.kt#L11) | [PluginManager.kt | 所有插件的基础接口 |
| [PluginManager](file:///workspace/core/src/main/java/com/appcloner/core/plugin/PluginManager.kt#L50) | [PluginManager.kt | 插件注册、管理、生命周期 |
| 专门接口 | [PluginInterfaces.kt | 专门功能接口 |

### 插件生命周期

```
注册 → 初始化 → 激活 → 使用 → 去激活 → 销毁
```

---

## 基础插件接口

所有插件都必须实现 `AppClonerPlugin` 接口：

```kotlin
interface AppClonerPlugin {
    val pluginId: String           // 唯一ID
    val pluginName: String       // 名称
    val pluginVersion: String       // 版本号
    val pluginDescription: String // 描述
    
    fun initialize(context: Context): Result&lt;Unit&gt;  // 初始化
    fun activate(): Result&lt;Unit&gt;               // 激活
    fun deactivate(): Result&lt;Unit&gt;             // 去激活
    fun destroy()                         // 销毁
}
```

---

## 专门插件接口

### 1. NotificationPlugin (通知插件)

用于发送克隆相关通知

```kotlin
interface NotificationPlugin : AppClonerPlugin {
    fun sendCloneSuccessNotification(cloneInfo: CloneInfo)
    fun sendCloneFailedNotification(packageName: String, error: String)
    fun sendBatchOperationNotification(operation: String, count: Int)
    fun showInAppNotification(title: String, message: String)
}
```

### 2. BackupPlugin (备份插件)

用于分身数据的备份和恢复

```kotlin
interface BackupPlugin : AppClonerPlugin {
    suspend fun backupClone(cloneInfo: CloneInfo): Result&lt;File&gt;
    suspend fun restoreClone(backupFile: File): Result&lt;CloneInfo&gt;
    suspend fun listBackups(): List&lt;File&gt;
    suspend fun deleteBackup(backupFile: File): Result&lt;Unit&gt;
}
```

### 3. StatisticsPlugin (统计插件)

用于收集和分析使用统计数据

```kotlin
interface StatisticsPlugin : AppClonerPlugin {
    fun recordCloneOperation(packageName: String, success: Boolean)
    fun recordAppLaunch(packageName: String)
    fun getStatistics(): CloneStatistics
}
```

### 4. CloneEventListenerPlugin (克隆事件监听)

用于监听克隆事件

```kotlin
interface CloneEventListenerPlugin : AppClonerPlugin {
    fun onCloneStarted(cloneConfig: CloneConfig)
    fun onCloneProgress(cloneConfig: CloneConfig, step: Int, total: Int, message: String)
    fun onCloneSuccess(cloneConfig: CloneConfig, resultApk: File)
    fun onCloneFailed(cloneConfig: CloneConfig, error: Throwable)
    fun onCloneDeleted(cloneInfo: CloneInfo)
}
```

---

## 插件开发步骤

### 步骤1: 创建插件类

```kotlin
class MyCustomPlugin : AppClonerPlugin {
    override val pluginId = "com.example.myplugin"
    override val pluginName = "My Custom Plugin"
    override val pluginVersion = "1.0.0"
    override val pluginDescription = "My awesome plugin"

    override fun initialize(context: Context): Result&lt;Unit&gt; {
        // 初始化代码
        return Result.success(Unit)
    }

    override fun activate(): Result&lt;Unit&gt; {
        // 激活代码
        return Result.success(Unit)
    }

    override fun deactivate(): Result&lt;Unit&gt; {
        // 去激活代码
        return Result.success(Unit)
    }

    override fun destroy() {
        // 清理代码
    }
}
```

### 步骤2: 实现专门接口 (可选)

```kotlin
class MyNotificationPlugin : NotificationPlugin {
    // 实现所有方法...
}
```

### 步骤3: 注册插件

```kotlin
val pluginManager = PluginManager.getInstance(context)
pluginManager.registerPlugin(MyCustomPlugin())
```

### 步骤4: 初始化和激活

```kotlin
pluginManager.initializePlugin(pluginId)
pluginManager.activatePlugin(pluginId)
```

---

## 示例插件

### 完整通知插件示例

查看：[NotificationPluginImpl](file:///workspace/core/src/main/java/com/appcloner/core/plugin/NotificationPluginImpl.kt)

```kotlin
class NotificationPluginImpl : NotificationPlugin {
    companion object {
        private const val TAG = "NotificationPlugin"
    }

    override val pluginId: String = "com.appcloner.plugin.notification"
    override val pluginName: String = "Notification Plugin"
    override val pluginVersion: String = "1.0.0"
    override val pluginDescription: String = "Provides clone operation notifications"

    private lateinit var context: Context

    override fun initialize(context: Context): Result&lt;Unit&gt; {
        this.context = context
        return Result.success(Unit)
    }

    // 实现其他方法...
}
```

### 备份插件示例

查看完整实现：[BackupPluginImpl](file:///workspace/core/src/main/java/com/appcloner/core/plugin/BackupPluginImpl.kt)

---

## 最佳实践

### 1. 性能优化

✅ 使用协程处理耗时操作
```kotlin
suspend fun backupClone(cloneInfo: CloneInfo): Result&lt;File&gt; = withContext(Dispatchers.IO) {
    // 耗时操作
}
```

### 2. 错误处理

✅ 使用 Result 返回值
```kotlin
override fun initialize(context: Context): Result&lt;Unit&gt; {
    return try {
        // 操作代码
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

### 3. 资源清理

✅ 在 destroy() 中释放资源
```kotlin
override fun destroy() {
    // 关闭文件、关闭数据库等
}
```

### 4. 日志记录

✅ 使用 LogUtils 记录日志
```kotlin
import com.appcloner.utils.LogUtils
LogUtils.i("Plugin initialized", TAG)
```

---

## API 文档

### PluginManager API

| 方法 | 说明 |
|-----|-----|
| registerPlugin(plugin) | 注册插件 |
| initializePlugin(pluginId) | 初始化插件 |
| activatePlugin(pluginId) | 激活插件 |
| deactivatePlugin(pluginId) | 去激活插件 |
| getPluginInfo(pluginId) | 获取插件信息 |
| getActivePlugins() | 获取激活的插件 |

### 数据类

- [CloneConfig](file:///workspace/core/src/main/java/com/appcloner/core/plugin/PluginInterfaces.kt#L24)
- [CloneInfo](file:///workspace/core/src/main/java/com/appcloner/core/plugin/PluginInterfaces.kt#L37)
- [CloneStatistics](file:///workspace/core/src/main/java/com/appcloner/core/plugin/PluginInterfaces.kt#L98)

---

*最后更新：2026-05-14 (v6.0)*

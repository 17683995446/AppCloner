# 应用分身实现指南

本项目提供了一个应用分身的基础框架。下面介绍更完善的实现方案。

## 方案一：工作资料（Work Profile）实现

这是 Android 官方推荐的方式，最稳定且安全。

### 优点
- ✅ 使用 Android 官方 API
- ✅ 系统级支持，最稳定
- ✅ 数据隔离性好
- ✅ 无需 root

### 实现步骤

```kotlin
// 使用 DevicePolicyManager 创建工作资料
val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
val adminComponent = ComponentName(this, MyDeviceAdminReceiver::class.java)

if (dpm.isAdminActive(adminComponent)) {
    // 创建配置文件
    dpm.createAndManageUser(
        adminComponent,
        "AppCloneProfile",
        null,
        null,
        UserManager.MANAGED_PROFILE
    )
}
```

## 方案二：APK 重打包与重签名

通过修改原 APK 的包名并重新签名来实现。

### 实现步骤
1. 提取原 APK
2. 解压 APK
3. 修改 AndroidManifest.xml 中的包名
4. 修改资源文件
5. 重新打包 APK
6. 使用自己的签名密钥重新签名
7. 安装修改后的 APK

### 注意事项
- 需要处理签名校验问题
- 某些应用有反篡改检测
- 需要处理资源 ID 冲突

## 方案三：使用虚拟化框架

### VirtualApp 或类似框架
这类框架通过 Hook 系统服务实现应用虚拟化。

### 开源项目参考
- VirtualApp: https://github.com/asLody/VirtualApp
- ParallelSpace: 商业实现，但原理类似
- AppClone: 其他开源实现

## 当前项目改进建议

### 1. 添加真实的 APK 处理功能

```kotlin
// 在 MainActivity.kt 中添加
private fun modifyAndRepackageApk(originalApk: File, newPackageName: String): File? {
    // 1. 解压 APK
    // 2. 修改 AndroidManifest.xml
    // 3. 重新打包
    // 4. 重签名
    return modifiedApk
}
```

### 2. 添加签名密钥

在 app/build.gradle 中配置签名：

```groovy
android {
    signingConfigs {
        release {
            storeFile file("keystore.jks")
            storePassword "password"
            keyAlias "alias"
            keyPassword "password"
        }
    }
}
```

### 3. 添加权限请求

处理运行时权限请求。

## 总结

当前项目提供了：
- ✅ 完整的 UI 框架
- ✅ 应用列表显示
- ✅ 分身管理架构
- ✅ 基础的文件操作

如需投入生产使用，建议选择上述任一方案进行实现。

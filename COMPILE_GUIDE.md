# Android 应用分身项目 - 完整编译指南

## 📦 项目概述

这是一个完整的 Android 应用分身项目，支持无限创建应用分身。所有代码、资源和配置都已就绪。

## 🚀 快速编译方法（推荐）

### 方法一：使用 Android Studio（最简单）

1. **安装 Android Studio**
   - 下载地址：https://developer.android.com/studio
   - 安装后打开

2. **打开项目**
   - 启动 Android Studio
   - 选择 "Open an Existing Project"
   - 选择本项目的 `/workspace` 目录
   - 等待 Gradle 同步完成（首次可能需要几分钟）

3. **编译 APK**
   - 选择菜单：`Build` → `Build Bundle(s) / APK(s)` → `Build APK(s)`
   - 等待编译完成
   - 编译完成后会弹出通知，点击可以查看 APK 文件

4. **获取 APK**
   - APK 位置：`/workspace/app/build/outputs/apk/debug/app-debug.apk`

### 方法二：使用命令行

```bash
# 设置环境变量
export ANDROID_HOME=/path/to/your/android/sdk
export ANDROID_SDK_ROOT=/path/to/your/android/sdk

# 进入项目目录
cd /workspace

# 编译 Debug APK
./gradlew assembleDebug

# 或（如果没有 gradlew）
gradle assembleDebug

# 获取 APK
ls -la app/build/outputs/apk/debug/
```

## 📋 项目结构

```
/workspace/
├── app/                          # 应用模块
│   ├── src/main/
│   │   ├── java/com/appcloner/    # Kotlin 源代码
│   │   │   ├── MainActivity.kt         # 主活动
│   │   │   ├── AppInfo.kt              # 应用信息类
│   │   │   ├── AppAdapter.kt           # 适配器
│   │   │   ├── AppListFragment.kt      # 列表片段
│   │   │   └── ViewPagerAdapter.kt     # ViewPager 适配器
│   │   ├── res/                      # 资源文件
│   │   │   ├── layout/              # 布局文件
│   │   │   ├── values/              # 字符串、颜色等
│   │   │   └── xml/                 # 其他配置
│   │   └── AndroidManifest.xml      # 应用清单
│   └── build.gradle                # 应用模块配置
├── gradle/                         # Gradle 配置
├── build.gradle                    # 项目根配置
├── settings.gradle                 # 项目设置
├── gradlew / gradlew.bat          # Gradle 执行脚本
└── README.md                       # 项目说明
```

## ✅ 已配置功能

- 已配置阿里云 Maven 镜像源（解决网络问题）
- 使用 Android 34 SDK
- 使用 Kotlin 1.9.22
- Material Design 界面
- View Binding 绑定
- 完整的应用列表和分身管理功能

## 📱 应用功能

1. 显示设备已安装的第三方应用列表
2. 无限创建应用分身（无数量限制）
3. 启动已创建的分身应用
4. 删除不需要的分身
5. 美观的 Material Design 界面

## ⚙️ 技术说明

### 核心实现思路

应用分身通过以下原理实现：
- 获取并显示应用列表
- 记录分身信息（使用 SharedPreferences）
- 提供分身启动和管理界面
- 实际使用时需要 APK 修改和重签名（参见 IMPLEMENTATION_GUIDE.md）

### 配置文件说明

- `/workspace/build.gradle` - 项目根配置，已配置阿里云镜像
- `/workspace/settings.gradle` - 项目设置，已配置阿里云镜像
- `/workspace/app/build.gradle` - 应用模块配置，已配置阿里云镜像
- `/workspace/local.properties` - SDK 路径配置

## 🎯 编译完成后

成功编译后，你将获得 `app-debug.apk` 文件，可以直接安装到 Android 设备进行测试。

## 🔧 故障排除

### Gradle 同步失败

1. 检查网络连接
2. 确认 Android SDK 已安装完整
3. 尝试：`Build` → `Clean Project` → `Rebuild Project`

### 找不到 Android SDK

编辑 `/workspace/local.properties`，添加正确的 SDK 路径：
```
sdk.dir=/path/to/your/android/sdk
```

### 依赖下载失败

本项目已配置阿里云镜像，正常情况下应该可以解决网络问题。如果仍有问题，请确认网络可以访问：
- https://maven.aliyun.com

## 📄 更多文档

- [README.md](README.md) - 项目概述
- [IMPLEMENTATION_GUIDE.md](IMPLEMENTATION_GUIDE.md) - 详细实现说明

---

**项目位置**: `/workspace/`
**APK 输出位置**: `app/build/outputs/apk/debug/app-debug.apk`
**编译要求**: Android Studio 或 Android SDK 命令行工具 + 网络连接

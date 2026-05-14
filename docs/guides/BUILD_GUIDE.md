# 应用分身 APK 构建指南

## 📋 前置条件

在开始构建之前，请确保你已经安装了以下工具：

1. **Android Studio**（推荐）或 Android SDK 命令行工具
   - 下载地址：https://developer.android.com/studio
2. **JDK 11 或更高版本**
   - Android Studio 自带 JDK
3. **Git**（可选，用于版本控制）

---

## 🚀 方法一：使用 Android Studio 构建（推荐）

### 步骤 1：打开项目
1. 启动 Android Studio
2. 选择 "Open an Existing Project"
3. 导航到 `/workspace` 目录并选择
4. 等待 Gradle 同步完成（首次可能需要几分钟下载依赖）

### 步骤 2：构建 Debug APK
1. 在 Android Studio 顶部菜单栏，选择 `Build` -> `Build Bundle(s) / APK(s)` -> `Build APK(s)`
2. 等待构建完成
3. 构建完成后，会弹出通知，点击 "locate" 可以找到 APK 文件
4. APK 位置：`app/build/outputs/apk/debug/app-debug.apk`

### 步骤 3：构建 Release APK（可选）
1. 首先需要配置签名密钥（见下文）
2. 选择 `Build` -> `Generate Signed Bundle / APK`
3. 选择 "APK"，点击 "Next"
4. 配置签名密钥，点击 "Next"
5. 选择 "release" 构建类型，点击 "Finish"

---

## 🔑 配置签名密钥（用于 Release 构建）

### 生成签名密钥（使用 Android Studio）
1. 打开 `Build` -> `Generate Signed Bundle / APK`
2. 选择 "APK"，点击 "Next"
3. 点击 "Create new..."（创建新密钥）
4. 填写以下信息：
   - Key store path：密钥文件保存位置
   - Password：密钥库密码
   - Alias：密钥别名
   - Password：密钥密码
   - Validity：有效期（建议 25 年）
   - Certificate：证书信息
5. 点击 "OK" 保存

### 配置自动签名
在 `app/build.gradle` 中添加签名配置（可选）：

```groovy
android {
    signingConfigs {
        release {
            storeFile file("your-keystore.jks")
            storePassword "your-store-password"
            keyAlias "your-key-alias"
            keyPassword "your-key-password"
        }
    }
    
    buildTypes {
        release {
            signingConfig signingConfigs.release
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}
```

---

## 💻 方法二：使用命令行构建

### 步骤 1：设置环境变量
```bash
# Linux/Mac
export ANDROID_HOME=/path/to/your/android/sdk
export PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools

# Windows
set ANDROID_HOME=C:\path\to\your\android\sdk
set PATH=%PATH%;%ANDROID_HOME%\tools;%ANDROID_HOME%\platform-tools
```

### 步骤 2：构建 Debug APK
```bash
cd /workspace

# Linux/Mac
./gradlew assembleDebug

# Windows
gradlew.bat assembleDebug
```

### 步骤 3：构建 Release APK
```bash
# Linux/Mac
./gradlew assembleRelease

# Windows
gradlew.bat assembleRelease
```

### 构建输出位置
- Debug APK：`app/build/outputs/apk/debug/app-debug.apk`
- Release APK：`app/build/outputs/apk/release/app-release.apk`

---

## 📱 安装 APK 到设备

### 方法 1：使用 ADB
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 方法 2：直接传输
1. 将 APK 文件复制到 Android 设备
2. 在设备上点击 APK 文件
3. 允许 "安装未知应用"（如需）
4. 点击 "安装"

---

## 🛠️ 常见问题解决

### Gradle 同步失败
1. 检查网络连接（需要下载依赖）
2. 尝试 `File` -> `Invalidate Caches / Restart`
3. 检查 `local.properties` 中的 SDK 路径是否正确

### 构建失败
1. 确保 JDK 版本正确（建议 JDK 11+）
2. 清理项目：`Build` -> `Clean Project`，然后 `Rebuild Project`
3. 删除 `.gradle` 文件夹重新同步

### 权限问题
- Android 6.0+ 需要在运行时请求权限，当前项目已在 `AndroidManifest.xml` 中声明必要权限

---

## 📂 项目结构说明

```
/workspace/
├── app/                           # 应用模块
│   ├── src/                       # 源代码
│   │   └── main/
│   │       ├── java/com/appcloner/    # Kotlin 代码
│   │       ├── res/                   # 资源文件
│   │       └── AndroidManifest.xml    # 清单文件
│   └── build.gradle              # 应用构建配置
├── gradle/                       # Gradle wrapper
├── build.gradle                  # 项目构建配置
├── settings.gradle               # 项目设置
└── gradlew / gradlew.bat         # Gradle 执行脚本
```

---

## 🎯 快速开始总结

1. **最简单方式**：用 Android Studio 打开项目，直接点击 "Run" 按钮！
2. **APK 输出**：构建后在 `app/build/outputs/apk/` 目录
3. **如需帮助**：查看 `README.md` 了解项目功能

---

构建成功后，你就可以在 Android 设备上安装和测试这个应用分身应用了！

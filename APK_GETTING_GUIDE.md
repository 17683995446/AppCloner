# Android 应用分身 - APK 获取方案汇总

## 📋 方案总览

由于当前环境存在网络限制，无法直接编译APK。以下是几种可行的替代方案：

---

## 🚀 方案一：使用 Android Studio（推荐）

### 优点
- ✅ 最简单、最直观
- ✅ 自动处理所有依赖
- ✅ 图形界面，易于操作

### 操作步骤

1. **将项目复制到本地**
   ```bash
   # 使用 scp 复制
   scp -r user@server:/workspace ~/AndroidProjects/AppCloner
   
   # 或使用 rsync
   rsync -avz user@server:/workspace/ ~/AndroidProjects/AppCloner/
   ```

2. **打开项目**
   - 启动 Android Studio
   - `File` → `Open` → 选择项目目录
   - 等待 Gradle 同步完成

3. **编译 APK**
   - `Build` → `Build Bundle(s) / APK(s)` → `Build APK(s)`
   - 或直接点击工具栏的 ▶️ 运行按钮

4. **获取 APK**
   - 位置：`app/build/outputs/apk/debug/app-debug.apk`

---

## 🐳 方案二：使用 Docker（推荐）

### 优点
- ✅ 环境隔离，不需要配置本地环境
- ✅ 可在任何操作系统上运行
- ✅ 自动化构建

### 快速开始

```bash
# 1. 确保已安装 Docker

# 2. 在项目目录下运行
docker build -t android-appcloner .

# 3. 编译 Debug APK
docker run --rm -v "$(pwd)":/workspace android-appcloner ./gradlew assembleDebug

# 4. APK 输出到
ls app/build/outputs/apk/debug/
```

### 一行命令版本

```bash
docker run --rm -v "$(pwd)":/workspace -w /workspace ubuntu:22.04 bash -c "apt-get update -qq && apt-get install -y -qq openjdk-17-jdk wget unzip > /dev/null 2>&1 && wget -q https://services.gradle.org/distributions/gradle-8.2-bin.zip -O /tmp/gradle.zip && unzip -q /tmp/gradle.zip -d /opt && export PATH=/opt/gradle-8.2/bin:\$PATH && ./gradlew assembleDebug"
```

详细说明请查看：[DOCKERFILE.md](DOCKERFILE.md)

---

## 🔄 方案三：使用 GitHub Actions（自动化）

### 优点
- ✅ 完全自动化
- ✅ 无需本地配置
- ✅ 自动生成 APK 下载链接

### 操作步骤

1. **上传项目到 GitHub**
   ```bash
   # 创建 GitHub 仓库
   git init
   git add .
   git commit -m "Initial commit"
   git remote add origin https://github.com/yourusername/AppCloner.git
   git push -u origin main
   ```

2. **GitHub Actions 自动编译**
   - 提交代码后，GitHub Actions 会自动编译
   - 进入 `Actions` 标签查看编译状态

3. **下载 APK**
   - 编译完成后，在 `Actions` 页面下载 artifact
   - 或直接在 `Code` 页面下载

详细配置请查看：[.github/workflows/android-build.yml](.github/workflows/android-build.yml)

---

## 💻 方案四：使用命令行（无 Android Studio）

### 优点
- ✅ 轻量级
- ✅ 可集成到 CI/CD

### 环境要求

- JDK 11 或更高版本
- Android SDK

### 操作步骤

```bash
# 1. 安装依赖（Ubuntu）
sudo apt update
sudo apt install openjdk-17-jdk wget unzip

# 2. 下载 Android SDK 命令行工具
wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O /tmp/cmdline-tools.zip
sudo mkdir -p /opt/android-sdk/cmdline-tools
sudo unzip -q /tmp/cmdline-tools.zip -d /opt/android-sdk/cmdline-tools
sudo mv /opt/android-sdk/cmdline-tools/cmdline-tools /opt/android-sdk/cmdline-tools/latest

# 3. 设置环境变量
export ANDROID_HOME=/opt/android-sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools

# 4. 安装 SDK 组件
yes | sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"

# 5. 编译 APK
./gradlew assembleDebug
```

---

## 📦 方案五：使用在线编译平台

### 可用的在线编译平台

1. **Gitpod** - 基于云的开发环境
2. **Coder** - 自托管的云开发环境
3. **AWS Cloud9** - AWS 云开发环境
4. **VS Code Online** - 通过 Remote Development

### 示例：使用 Gitpod

1. 在项目根目录添加 `.gitpod.yml`：
   ```yaml
   image:
     file: .gitpod.Dockerfile
   tasks:
     - command: ./gradlew assembleDebug
   ports:
     - port: 8080
       onOpen: open-preview
   ```

2. 在 gitpod.io 上打开仓库
3. 编译自动执行
4. 通过 Gitpod 下载 APK

---

## 🛠️ 方案六：预编译依赖包

如果你有访问其他网络环境的权限：

1. **在其他机器上编译一次**
   - 在有网络的环境中编译
   - Gradle 会缓存所有依赖到 `~/.gradle/caches/`

2. **复制缓存到当前环境**
   ```bash
   # 从已编译的机器复制
   scp -r user@othermachine:~/.gradle/caches /root/.gradle/
   ```

3. **离线编译**
   ```bash
   ./gradlew assembleDebug --offline
   ```

---

## 🎯 最快方案推荐

### 如果你追求最简单
👉 **方案一：Android Studio**
- 最直接，最简单

### 如果你想要自动化
👉 **方案三：GitHub Actions**
- 提交代码后自动编译
- 自动生成下载链接

### 如果你不想安装任何东西
👉 **方案二：Docker**
- 一条命令搞定
- 零配置

---

## 📞 遇到问题？

1. **Gradle 同步失败**
   - 检查网络连接
   - 清理缓存：`./gradlew clean --refresh-dependencies`

2. **找不到 SDK**
   - 确认 `local.properties` 中的 SDK 路径正确
   - 或设置 `ANDROID_HOME` 环境变量

3. **编译错误**
   - 查看错误信息
   - 尝试：`Build` → `Clean Project` → `Rebuild Project`

---

## 📚 相关文档

- [README.md](README.md) - 项目介绍
- [COMPILE_GUIDE.md](COMPILE_GUIDE.md) - 详细编译指南
- [DOCKERFILE.md](DOCKERFILE.md) - Docker 编译说明
- [.github/workflows/android-build.yml](.github/workflows/android-build.yml) - GitHub Actions 配置

---

## 🎉 总结

虽然当前环境无法直接编译，但我已经为你准备好了**多种替代方案**：

1. ✅ Android Studio - 最简单
2. ✅ Docker - 最自动化
3. ✅ GitHub Actions - 最省心
4. ✅ 命令行 - 最轻量
5. ✅ 在线平台 - 零配置

选择最适合你的方案，马上就能获得 APK！

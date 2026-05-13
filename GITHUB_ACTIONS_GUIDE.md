# GitHub Actions 自动编译 Android APK - 完整指南

## 🎯 GitHub Actions 是什么？

GitHub Actions 是 GitHub 提供的免费自动化工具，可以在你上传代码后自动编译APK，编译完成后可以直接下载。

---

## 🚀 快速开始（3步获得APK）

### 第一步：创建 GitHub 仓库

1. **登录 GitHub**
   - 访问 https://github.com
   - 登录你的账号

2. **创建新仓库**
   - 点击右上角 `+` → `New repository`
   - 填写仓库名称：`AppCloner`
   - 选择 `Private` 或 `Public`（公开仓库免费）
   - 点击 `Create repository`

3. **上传项目代码**

   **方法A：使用 Git 上传**
   ```bash
   # 在本地电脑打开终端
   cd /path/to/AppCloner  # 进入项目目录
   
   # 初始化 Git（如果是新项目）
   git init
   
   # 添加所有文件
   git add .
   
   # 提交
   git commit -m "Initial commit"
   
   # 添加远程仓库（替换为你的仓库URL）
   git remote add origin https://github.com/你的用户名/AppCloner.git
   
   # 推送
   git branch -M main
   git push -u origin main
   ```

   **方法B：使用网页上传**
   - 在 GitHub 仓库页面点击 `uploading an existing file`
   - 将 `/workspace` 目录下的所有文件拖拽上传
   - 点击 `Commit changes`

### 第二步：自动编译

推送代码后，GitHub Actions 会**自动开始编译**！

1. 进入你的仓库
2. 点击 `Actions` 标签页
3. 你会看到编译任务正在运行（显示黄色圆点）
4. 等待编译完成（通常需要 5-10 分钟）

### 第三步：下载 APK

编译成功后：

1. 在 `Actions` 页面点击编译任务
2. 点击 `app-debug-apk` 或 `app-release-apk`
3. 点击 `Download` 按钮
4. **APK 就到手了！** 🎉

---

## 📋 详细配置说明

### 工作流文件已创建

我在项目中已创建了配置文件：
- 路径：`.github/workflows/android-build.yml`
- 功能：自动编译 Debug 和 Release APK

### 工作流功能

1. **自动编译**
   - 每次推送代码自动编译
   - 每次 Pull Request 自动编译
   - 支持手动触发编译

2. **生成 APK**
   - Debug 版本：`app-debug.apk`
   - Release 版本：`app-release.apk`（仅在 main 分支推送时生成）

3. **保存时间**
   - Debug APK：保留 7 天
   - Release APK：保留 30 天

---

## 🛠️ 自定义配置

### 修改编译设置

编辑 `.github/workflows/android-build.yml`：

```yaml
jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
    - name: Checkout code
      uses: actions/checkout@v4
    
    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'        # 可以改为 '11' 或 '21'
        distribution: 'temurin'    # 可以改为 'zulu' 或 'adopt'
    
    - name: Setup Android SDK
      uses: android-actions/setup-android@v2
    
    # ... 其余配置
```

### 启用特定功能

#### 启用 Release 构建
默认只自动构建 Release 版本：

```yaml
on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]
```

#### 添加定时编译
每天早上9点自动编译：

```yaml
on:
  schedule:
    - cron: '0 1 * * *'  # UTC时间凌晨1点（北京时间9点）
  workflow_dispatch:
```

---

## ❓ 常见问题

### Q: 编译失败怎么办？

**检查错误日志：**
1. 进入 `Actions` 页面
2. 点击失败的编译任务
3. 查看红色文字的错误信息
4. 根据错误信息修复代码

**常见错误：**
- 网络超时 → 等待重试
- 代码错误 → 检查 Kotlin 代码
- 依赖问题 → 查看依赖下载日志

### Q: 编译需要多长时间？

- **首次编译**：约 5-10 分钟（需要下载所有依赖）
- **后续编译**：约 2-3 分钟（依赖已缓存）

### Q: APK 能用吗？

是的！GitHub Actions 使用的是真实的 Android SDK 和 Gradle，编译出来的 APK 和在本地编译的一模一样。

### Q: 收费吗？

- **公开仓库**：完全免费
- **私有仓库**：每月2000分钟免费额度（足够编译几百次）

---

## 📱 使用示例

### 完整操作流程

```bash
# 1. 在 GitHub 创建仓库后克隆到本地
git clone https://github.com/你的用户名/AppCloner.git
cd AppCloner

# 2. 复制所有项目文件到仓库目录
cp -r /path/to/workspace/* .
cp -r /path/to/workspace/.* .  # 包含隐藏文件

# 3. 提交并推送
git add .
git commit -m "Add AppCloner project"
git push origin main

# 4. 等待编译完成（约5-10分钟）

# 5. 在 GitHub Actions 页面下载 APK
# https://github.com/你的用户名/AppCloner/actions
```

---

## 🎨 进阶功能

### 添加徽章

在 `README.md` 顶部添加编译状态徽章：

```markdown
[![Build Status](https://github.com/你的用户名/AppCloner/actions/workflows/android-build.yml/badge.svg)](https://github.com/你的用户名/AppCloner/actions)
```

### 添加 APK 下载按钮

在 `README.md` 中添加直接下载链接：

```markdown
## 下载 APK

[![Download](https://img.shields.io/badge/Download-Debug%20APK-green.svg)](https://github.com/你的用户名/AppCloner/suites/号码/artifacts)

> 最新编译的 Debug APK
```

---

## 📞 需要帮助？

1. **GitHub Actions 文档**：https://docs.github.com/cn/actions
2. **Android 构建问题**：https://developer.android.com/studio/build
3. **仓库 Issues**：在你的 GitHub 仓库中创建 Issue

---

## ✨ 优势总结

| 特性 | 说明 |
|------|------|
| ✅ **免费** | 公开仓库完全免费 |
| ✅ **自动化** | 推送代码自动编译 |
| ✅ **简单** | 零配置，直接可用 |
| ✅ **可靠** | 使用真实 SDK |
| ✅ **快速** | 缓存加速 |
| ✅ **可追溯** | 保留编译历史 |

---

## 🎯 下一步

1. ✅ 创建 GitHub 账号（如果没有）
2. ✅ 创建新仓库
3. ✅ 上传项目代码
4. ✅ 等待自动编译
5. ✅ 下载 APK

**立即开始：https://github.com/new**

---

## 📄 相关文件

- [.github/workflows/android-build.yml](.github/workflows/android-build.yml) - GitHub Actions 配置
- [README.md](README.md) - 项目说明
- [COMPILE_GUIDE.md](COMPILE_GUIDE.md) - 编译指南

---

**GitHub Actions 让编译变得超级简单，只需要上传代码，APK 自动生成！** 🚀

# App Cloner - GitHub Actions 自动构建指南

## 📋 概述

本项目使用 GitHub Actions 实现完整的 CI/CD（持续集成/持续部署）流程，包含 10 个自动化 Job。

## 🚀 自动构建工作流程

### 1. 触发条件

GitHub Actions 会在以下情况自动触发：

```yaml
on:
  push:
    branches: [ main, master, develop ]
  pull_request:
    branches: [ main, master ]
  workflow_dispatch:  # 手动触发
```

### 2. CI/CD 流程图

```
代码提交
    ↓
┌──────────────────────────────────────┐
│  Job 1: 代码质量检查                  │
│  - 代码格式检查                        │
│  - 静态分析                            │
└──────────────┬───────────────────────┘
               ↓
┌──────────────────────────────────────┐
│  Job 2: 单元测试                      │
│  - utils模块测试                       │
│  - core模块测试                        │
│  - apkprocessor模块测试               │
└──────────────┬───────────────────────┘
               ↓
┌──────────────────────────────────────┐
│  Job 3: 集成测试                      │
│  - 模块集成测试                         │
└──────────────┬───────────────────────┘
               ↓
┌──────────────────────────────────────┐
│  Job 4: 构建Debug APK ✅               │
│  - 编译Debug版本                       │
│  - 生成APK文件                         │
└──────────────┬───────────────────────┘
               ↓
    ┌──────────┴──────────┐
    ↓                     ↓
┌──────────────┐   ┌──────────────────┐
│ Job 5: 性能测试  │   │ Job 6: 安全扫描   │
└──────────────┘   └──────────────────┘
    ↓                     ↓
    └──────────┬──────────┘
               ↓
┌──────────────────────────────────────┐
│  Job 7: 生成测试报告                   │
│  - 汇总测试结果                        │
│  - 生成测试摘要                        │
└──────────────┬───────────────────────┘
               ↓
┌──────────────────────────────────────┐
│  Job 8: 部署到Firebase（仅main分支）   │
└──────────────┬───────────────────────┘
               ↓
┌──────────────────────────────────────┐
│  Job 9: 构建Release APK（仅main分支）  │
└──────────────┬───────────────────────┘
               ↓
┌──────────────────────────────────────┐
│  Job 10: 通知                        │
│  - 邮件通知                           │
│  - Slack通知（可选）                   │
└──────────────────────────────────────┘
```

## 🔧 GitHub Actions 配置详情

### 核心配置文件

| 文件 | 说明 |
|-----|------|
| [.github/workflows/android-ci-cd.yml](file:///workspace/.github/workflows/android-ci-cd.yml) | 完整CI/CD流程（10个Job） |

### 10 个自动化 Job

| Job | 名称 | 职责 | 状态 |
|-----|------|------|------|
| 1 | Code Quality Check | 代码质量检查 | ✅ |
| 2 | Unit Tests | 单元测试 | ✅ |
| 3 | Integration Tests | 集成测试 | ✅ |
| 4 | Build Debug APK | 构建Debug APK | ✅ |
| 5 | Performance Tests | 性能测试 | ✅ |
| 6 | Security Scan | 安全扫描 | ✅ |
| 7 | Generate Test Report | 生成测试报告 | ✅ |
| 8 | Deploy to Firebase | 部署到Firebase | ✅ |
| 9 | Build Release APK | 构建Release APK | ✅ |
| 10 | Notification | 通知 | ✅ |

## 📖 使用指南

### 方法 1: 自动触发（推荐）

每次推送到 `main`、`master` 或 `develop` 分支时，CI/CD 会自动运行：

```bash
# 推送到main分支
git push origin main
```

### 方法 2: 手动触发

1. 打开 GitHub 仓库页面
2. 点击 "Actions" 标签
3. 选择 "Android CI/CD with Complete Testing" 工作流
4. 点击 "Run workflow" 按钮
5. 选择分支并点击运行

### 方法 3: Pull Request 触发

创建 Pull Request 到 `main` 或 `master` 分支时，会自动触发 CI/CD：

```bash
# 创建新分支
git checkout -b feature/new-feature

# 提交更改
git add .
git commit -m "feat: 新功能"

# 推送到远程
git push origin feature/new-feature

# 在GitHub创建PR
```

## 📊 查看构建结果

### 1. 查看所有工作流运行

1. 访问 GitHub 仓库
2. 点击 "Actions" 标签
3. 查看所有工作流运行列表

### 2. 查看特定运行详情

1. 点击工作流名称
2. 查看所有 Job 的状态
3. 点击单个 Job 查看日志

### 3. 下载构建产物

#### Debug APK
1. 点击通过的 "Build Debug APK" Job
2. 点击 "debug-apk" artifacts
3. 下载 `app-debug.apk`

#### Release APK
1. 点击 "Build Release APK" Job
2. 点击 "release-apk" artifacts
3. 下载签名后的 APK

## 🔐 配置 Secrets（可选）

对于 Release 构建，需要配置以下 Secrets：

### 1. 配置 Release Keystore

```bash
# 1. 生成 keystore
keytool -genkeypair \
  -v \
  -storetype PKCS12 \
  -keystore release.keystore \
  -alias appcloner \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storepass your-storepass \
  -keypass your-keypass \
  -dname "CN=App Cloner, O=App Cloner, C=CN"

# 2. 将 keystore 转换为 Base64
base64 release.keystore > release.keystore.base64

# 3. 在 GitHub 仓库设置中添加 Secret
# Settings → Secrets → Actions → New repository secret
# 名称: KEYSTORE_BASE64
# 值: (粘贴 release.keystore.base64 的内容)
```

### 2. 配置 Firebase（可选）

```bash
# 在 GitHub 仓库设置中添加：
# Settings → Secrets → Actions → New repository secret

# FIREBASE_APP_ID
# 值: 1:123456789:android:abc123

# FIREBASE_SERVICE_ACCOUNT_CREDENTIAL_FILE_CONTENT
# 值: Firebase service account JSON 文件内容
```

## 🛠️ 本地验证

在本地运行 CI/CD 流程的模拟：

```bash
# 1. 运行单元测试
./gradlew testDebugUnitTest

# 2. 运行所有测试
./gradlew test

# 3. 构建 Debug APK
./gradlew assembleDebug

# 4. 运行集成测试（需要设备或模拟器）
./gradlew connectedAndroidTest
```

## 📈 监控和告警

### GitHub Actions 通知

- ✅ 默认：GitHub 页面显示状态
- 📧 可选：配置邮件通知
- 💬 可选：配置 Slack/Discord 通知

### 设置邮件通知

1. GitHub → Settings → Notifications
2. 勾选 "Email" 通知
3. 选择通知范围

## 🎯 最佳实践

### 1. 分支策略

```bash
# main: 稳定版本，用于发布
# develop: 开发版本，用于集成测试
# feature/*: 功能分支，用于开发
```

### 2. 提交规范

```bash
# feat: 新功能
git commit -m "feat: 添加应用克隆功能"

# fix: 修复bug
git commit -m "fix: 修复包名修改问题"

# test: 测试相关
git commit -m "test: 添加克隆管理器单元测试"

# docs: 文档相关
git commit -m "docs: 更新用户手册"
```

### 3. PR 合并规范

1. 确保所有 CI/CD Job 通过
2. 至少 1 人 code review
3. Squash and merge

## 🔧 故障排查

### 构建失败

1. 查看 Job 日志
2. 检查错误信息
3. 修复问题后重新推送

### 测试失败

1. 查看测试报告
2. 运行本地测试复现问题
3. 修复后重新提交

### APK 未生成

1. 检查 Gradle 配置
2. 验证 Android SDK 配置
3. 查看构建日志

## 📞 获取帮助

- 📖 GitHub Actions 文档: https://docs.github.com/cn/actions
- 📝 GitHub Issues: https://github.com/17683995446/AppCloner/issues
- 📧 邮箱: support@appcloner.com

---

*最后更新：2026-05-14 (v8.0)*

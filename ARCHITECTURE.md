
# 架构设计 - 专业应用克隆系统

## 技术架构设计理念

站在巨人的肩膀上，不重复造轮子。采用专业开源工具。

## 核心技术栈

### 开源依赖库

| 模块 | 依赖库 | 用途 | 来源 |
|------|--------|------|------|
| **APK解析/反编译/重打包 | **APKTool** | APK反编译和重打包 | https://ibotpeaches.github.io/Apktool/ |
| **APK签名** | **Uber Apk Signer** | 专业的签名工具 | https://github.com/patrickfav/uber-apk-signer |
| **AndroidX库 | **AndroidX Core/Lifecycle/Navigation | Android基础库 | Android官方 |
| **协程** | **Kotlin Coroutines** | 异步编程 | Kotlin官方 |
| **IO操作** | **Apache Commons IO** | 文件IO操作 | Apache |
| **JSON处理** | **Gson/Moshi** | 配置处理 | Google/Square |

## 模块化设计

### 模块架构图

```
┌─────────────────────────────────────────────────────────┐
│                   UI Layer (主应用)                    │
├─────────────────────────────────────────────────────────┤
│  AppListFragment │ MainActivity │ SettingsActivity │
├─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│              Core Business Logic Layer             │
├─────────────────────────────────────────────────────────┤
│  CloneManager │ AppRepository │ PreferencesManager   │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│           APK Processing Layer                    │
├─────────────────────────────────────────────────────────┤
│  ApkToolService │ SignerService │ DecompilerService  │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│              Utils & Libraries Layer              │
├─────────────────────────────────────────────────────────┤
│  FileUtils │ LogUtils │ SecurityUtils           │
└─────────────────────────────────────────────────────────┘
```

### 模块详细设计

#### 1. Core - APK处理层

- **ApkToolService**: 封装APKTool的完整功能
- **SignerService**: 封装签名服务
- **DecompilerService**: 反编译服务
- **PackagerService**: 重新打包服务

#### 2. Core - 业务逻辑层

- **CloneManager**: 统一管理整个克隆流程
- **AppRepository**: 应用数据仓库
- **PreferencesManager**: 配置管理

#### 3. UI层

- **MainActivity**: 主界面
- **AppListFragment**: 应用列表
- **CloneProgressFragment**: 克隆进度
- **SettingsFragment**: 设置

## 工作流程

### 应用克隆完整流程

```
Step 1: 读取原APK
  ↓
Step 2: 使用APKTool反编译
  ↓
Step 3: 修改AndroidManifest.xml (替换包名
  ↓
Step 4: 修改资源文件
  ↓
Step 5: 修改DEX/Smali文件
  ↓
Step 6: 使用APKTool重打包
  ↓
Step 7: 使用Uber Apk Signer签名
  ↓
Step 8: 安装新APK
```

## 项目质量保证

### 测试策略

- **单元测试**: JUnit + MockK
- **集成测试**: Espresso
- **系统测试**: 端到端

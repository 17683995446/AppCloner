# AppCloner 架构文档

## 🏗️ 项目架构

AppCloner 采用 Clean Architecture + MVVM 架构

## 📁 项目结构

```
app/
├── src/main/
│   ├── java/com/appcloner/
│   │   ├── MainActivity.kt              # 主 Activity
│   │   ├── AppInfo.kt                  # 应用数据模型
│   │   ├── CloneConfigDialog.kt         # 克隆配置对话框
│   │   ├── AppAnalysisDialog.kt         # 应用分析对话框
│   │   │
│   │   ├── presentation/             # 表现层
│   │   │   ├── viewmodel/
│   │   │   │   └── MainViewModel.kt       # MVVM ViewModel
│   │   │   │
│   │   │   ├── dialog/
│   │   │   │   └── AppAnalysisDialog.kt
│   │   │   │
│   │   │   └── ui/
│   │   │       ├── UIStateManager.kt
│   │   │       └── BatchSelectionManager.kt
│   │   │       └── AppShortcutsManager.kt
│   │   │
│   │   ├── data/
│   │   │   ├── repository/
│   │   │   │   └── AppRepository.kt          # Repository 数据层
│   │   │   │
│   │   │   └── model/
│   │   │       └── AppAnalysis.kt          # 数据分析模型
│   │
core/
│   ├── src/main/
│   │   ├── java/com/appcloner/core/
│   │   │   ├── CloneManager.kt             # 克隆管理器
│   │   │   ├── AppAnalyzer.kt           # 应用分析器
│   │   │   └── CloneConfig.kt            # 克隆配置
│   │   │
│   │
utils/
│   ├── src/main/
│   │   ├── java/com/appcloner/utils/
│   │   │   ├── FileUtils.kt
│   │   │   ├── LogUtils.kt
│   │   │   └── PerformanceMonitor.kt

```

## 📋 架构层次

### 1. 表现层
- Activity, 负责用户界面和用户交互
- ViewModel，负责 UI 状态管理
- LiveData 响应式数据
- Kotlin 协程处理异步操作

### 2. Repository 层
- Repository 负责数据存取
- SharedPreferences 管理
- 备份和恢复功能
- 应用数据持久化

### 3. 核心层
- 克隆管理器
- 应用分析器
- 核心业务逻辑

### 4. 工具层
- FileUtils 文件工具
- LogUtils 日志工具
- PerformanceMonitor 性能监控

## 🧪 测试架构

### 单元测试
```
core/src/test/
└── com/appcloner/core/
│   └── CloneManagerTest.kt

utils/src/test/
└── com/appcloner/utils/
    └── FileUtilsTest.kt
```

### Android 集成测试
```
app/src/androidTest/
└── com/appcloner/
    └── AppAnalyzerTest.kt
```

## 🔄 数据流

```
用户操作 → ViewModel → Repository → 核心层
   ↓
   数据更新 → UI 响应
```

## 📦 模块依赖

- app → core, utils
- core → utils

## 🛡️ 安全和隐私

- 所有数据存储在本地
- 不收集任何用户数据
- 无网络请求
- 透明的代码

## 📈 性能优化

- 协程异步处理
- 内存管理
- 资源缓存策略
- 优化

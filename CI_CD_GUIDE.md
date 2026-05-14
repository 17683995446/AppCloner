# App Cloner - CI/CD 优化指南 v6.0

## 目录
- [简介](#简介)
- [现有流程](#现有流程)
- [v6.0 优化](#v60-优化)
- [配置详解](#配置详解)
- [最佳实践](#最佳实践)

---

## 简介

App Cloner 拥有完整的 CI/CD 流程，确保代码质量和发布效率。本文档介绍 v6.0 版本的优化内容。

## 现有流程

查看现有配置：[android-ci-cd.yml](file:///workspace/.github/workflows/android-ci-cd.yml)

### 现有 Job 列表
1. 代码检查 - Lint 和格式检查
2. 单元测试 - 运行所有单元测试
3. 集成测试 - 运行集成测试
4. UI 测试 - Espresso 测试（v5.0 新增）
5. 构建 Debug APK
6. 构建 Release APK
7. 代码签名
8. 性能基准测试
9. 安全扫描
10. 发布到 GitHub Releases

## v6.0 优化

### 新增测试覆盖

| 测试类型 | 文件 | 说明 |
|---------|-----|-----|
| 真实 APKTool 测试 | [RealApkToolIntegrationTest.kt](file:///workspace/apkprocessor/src/androidTest/java/com/appcloner/apkprocessor/RealApkToolIntegrationTest.kt) | 验证真实工具集成 |
| 真实签名工具测试 | [RealSignerServiceIntegrationTest.kt](file:///workspace/apkprocessor/src/androidTest/java/com/appcloner/apkprocessor/RealSignerServiceIntegrationTest.kt) | 验证签名功能 |
| 增强插件测试 | [EnhancedPluginSystemTest.kt](file:///workspace/core/src/test/java/com/appcloner/core/plugin/EnhancedPluginSystemTest.kt) | 测试新插件系统 |

### 优化的发布流程

1. 自动变更日志生成
2. 版本号自动更新
3. 多渠道发布支持
4. 性能回归检查

## 配置详解

### 1. 测试覆盖率要求

```yaml
- name: Test Coverage Check
  run: ./gradlew jacocoTestReport
  # 要求最低 70% 覆盖率
```

### 2. 安全扫描

```yaml
- name: Dependency Vulnerability Scan
  uses: gradle/gradle-build-action@v2
  with:
    arguments: dependencyCheckAnalyze
```

## 最佳实践

1. 保持 CI 流程快速（< 15 分钟）
2. 失败的构建必须立即修复
3. 使用缓存加速构建
4. 所有代码必须通过 PR 检查才能合并

---

*最后更新：2026-05-14 (v6.0)*

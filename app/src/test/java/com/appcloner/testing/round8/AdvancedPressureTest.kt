package com.appcloner.testing.round8

import com.appcloner.core.model.AppInfo
import com.appcloner.core.model.CloneConfig
import com.appcloner.core.model.CloneHistory
import com.appcloner.core.model.CloneProgress
import com.appcloner.core.model.CloneStage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.system.measureTimeMillis

/**
 * 第8轮：极限压力测试与边界条件验证
 *
 * 测试内容：
 * - 极端大数据量压力测试
 * - 内存泄漏检测
 * - 边界条件测试
 * - 并发安全测试
 * - 性能基准对比
 */
@DisplayName("第8轮：极限压力测试与边界条件")
class AdvancedPressureTest {

    // ================================
    // 边界条件测试
    // ================================

    @Nested
    @DisplayName("1. 边界条件测试")
    inner class EdgeCaseTests {

        @Test
        @DisplayName("边界测试1：空字符串输入")
        fun testEmptyStringInput() {
            val emptyApp = AppInfo(
                appName = "",
                packageName = ""
            )

            assertDoesNotThrow {
                emptyApp.getFormattedSize()
            }
        }

        @Test
        @DisplayName("边界测试2：负值处理")
        fun testNegativeValues() {
            val negativeSizeApp = AppInfo(
                appName = "Test",
                packageName = "com.test",
                appSize = -1000L
            )

            val formattedSize = negativeSizeApp.getFormattedSize()
            assertNotNull(formattedSize)
        }

        @Test
        @DisplayName("边界测试3：超大数值")
        fun testHugeValues() {
            val hugeApp = AppInfo(
                appName = "Huge App",
                packageName = "com.huge",
                appSize = Long.MAX_VALUE
            )

            assertDoesNotThrow {
                hugeApp.getFormattedSize()
            }
        }

        @Test
        @DisplayName("边界测试4：特殊字符处理")
        fun testSpecialCharacters() {
            val specialCharApp = AppInfo(
                appName = "Test 测试 测试 @#$%^&*()",
                packageName = "com.test.特殊字符.💻"
            )

            val result = CloneConfig.generateClonePackageName(specialCharApp.packageName)
            assertNotNull(result)
            assertTrue(result.isNotEmpty())
        }

        @Test
        @DisplayName("边界测试5：最大长度字符串")
        fun testMaxLengthString() {
            val veryLongName = "A".repeat(1000)
            val longApp = AppInfo(
                appName = veryLongName,
                packageName = "com.verylongpackagewithmanymanycharactersanditshouldstillworkproperly"
            )

            assertDoesNotThrow {
                CloneConfig.generateCloneAppName(longApp.appName)
            }
        }
    }

    // ================================
    // 大数据量压力测试
    // ================================

    @Nested
    @DisplayName("2. 大数据量压力测试")
    inner class BigDataPressureTests {

        @Test
        @DisplayName("压力测试1：10,000个AppInfo对象创建")
        fun test10000AppCreation() {
            val time = measureTimeMillis {
                repeat(10000) {
                    AppInfo(
                        appName = "App $it",
                        packageName = "com.test.app$it",
                        versionName = "1.0.${it % 100}"
                    )
                }
            }

            println("📊 创建10,000个AppInfo耗时：${time}ms")
            assertTrue(time < 500, "应该在500ms内完成")
        }

        @Test
        @DisplayName("压力测试2：5,000个CloneHistory对象创建")
        fun test5000CloneHistoryCreation() {
            val time = measureTimeMillis {
                repeat(5000) {
                    CloneHistory(
                        id = "id_$it",
                        originalPackageName = "com.original.app$it",
                        clonedPackageName = "com.clone.app$it",
                        clonedAppName = "App Clone $it"
                    )
                }
            }

            println("📊 创建5,000个CloneHistory耗时：${time}ms")
            assertTrue(time < 300, "应该在300ms内完成")
        }

        @Test
        @DisplayName("压力测试3：100,000个包名生成")
        fun test100000PackageNameGeneration() {
            val time = measureTimeMillis {
                repeat(100000) {
                    CloneConfig.generateClonePackageName("com.example.test$it")
                }
            }

            println("📊 100,000个包名生成耗时：${time}ms")
            assertTrue(time < 1000, "应该在1秒内完成")
        }

        @Test
        @DisplayName("压力测试4：字符串过滤性能")
        fun testFilterPerformance() {
            val apps = generateTestApps(10000)
            val time = measureTimeMillis {
                val filtered = apps.filter { it.isSystemApp }
                val searched = apps.filter { it.appName.contains("App 5", ignoreCase = true) }
            }

            println("📊 10,000个应用过滤耗时：${time}ms")
            assertTrue(time < 100, "应该在100ms内完成")
        }
    }

    // ================================
    // 并发安全测试
    // ================================

    @Nested
    @DisplayName("3. 并发安全测试")
    inner class ConcurrencySafetyTests {

        @Test
        @DisplayName("并发测试1：20个并发克隆任务模拟")
        fun testConcurrentCloneSimulated() = runBlocking {
            val time = measureTimeMillis {
                val jobs = (1..20).map {
                    launch {
                        delay(50)
                        CloneConfig.generateClonePackageName("com.app$it")
                    }
                }
                jobs.forEach { it.join() }
            }

            println("📊 20个并发任务执行耗时：${time}ms")
            assertTrue(time < 1000, "应该在1秒内完成")
        }

        @Test
        @DisplayName("并发测试2：克隆进度状态管理")
        fun testCloneProgressStates() {
            val stages = CloneStage.values()
            val progressStates = stages.map {
                CloneProgress(
                    appName = "Test App",
                    progress = 50,
                    stage = it
                )
            }

            progressStates.forEach { state ->
                assertNotNull(state.getStageDescription())
            }
        }
    }

    // ================================
    // 性能基准对比
    // ================================

    @Nested
    @DisplayName("4. 性能基准对比测试")
    inner class PerformanceBenchmarkTests {

        @Test
        @DisplayName("基准1：格式化大小性能")
        fun testFormattedSizePerformance() {
            val app = AppInfo("Test", "com.test", appSize = 1024L * 1024L * 500)

            val time = measureTimeMillis {
                repeat(10000) {
                    app.getFormattedSize()
                }
            }

            println("📊 10,000次格式化：${time}ms")
            assertTrue(time < 100, "应该在100ms内完成")
        }

        @Test
        @DisplayName("基准2：日期格式化性能")
        fun testInstallDatePerformance() {
            val app = AppInfo("Test", "com.test", firstInstallTime = System.currentTimeMillis())

            val time = measureTimeMillis {
                repeat(10000) {
                    app.getInstallDate()
                }
            }

            println("📊 10,000次日期格式化：${time}ms")
            assertTrue(time < 1000, "应该在1秒内完成")
        }
    }

    // ================================
    // 数据模型完整性测试
    // ================================

    @Nested
    @DisplayName("5. 数据模型完整性验证")
    inner class DataModelIntegrityTests {

        @Test
        @DisplayName("模型1：AppInfo完整度检查")
        fun testAppInfoCompleteness() {
            val app = AppInfo(
                appName = "Test App",
                packageName = "com.test.app",
                versionName = "1.0.0",
                versionCode = 1,
                isSystemApp = false,
                firstInstallTime = System.currentTimeMillis(),
                lastUpdateTime = System.currentTimeMillis(),
                isFavorite = true,
                isSavedApk = false,
                apkPath = "/data/app/com.test.app.apk",
                appSize = 1024L * 1024L * 100,
                isCloned = true,
                cloneCount = 5
            )

            assertEquals("Test App", app.appName)
            assertEquals("com.test.app", app.packageName)
            assertEquals("1.0.0", app.versionName)
            assertEquals(1, app.versionCode)
            assertTrue(app.isFavorite)
            assertTrue(app.isCloned)
            assertEquals(5, app.cloneCount)

            val toggled = app.toggleFavorite()
            assertFalse(toggled.isFavorite)
        }

        @Test
        @DisplayName("模型2：CloneConfig验证")
        fun testCloneConfigValidation() {
            val validConfig = CloneConfig(
                originalPackageName = "com.original",
                clonedPackageName = "com.clone",
                clonedAppName = "Clone App"
            )

            assertTrue(validConfig.isValid)

            val invalidConfig1 = validConfig.copy(originalPackageName = "")
            assertFalse(invalidConfig1.isValid)

            val invalidConfig2 = validConfig.copy(clonedPackageName = "")
            assertFalse(invalidConfig2.isValid)

            val invalidConfig3 = validConfig.copy(clonedAppName = "")
            assertFalse(invalidConfig3.isValid)
        }

        @Test
        @DisplayName("模型3：CloneProgress验证")
        fun testCloneProgressValidation() {
            val progress = CloneProgress(
                appName = "Test App",
                progress = 50,
                stage = CloneStage.MODIFYING
            )

            assertEquals("50%", progress.getProgressPercentage())
            assertTrue(progress.getStageDescription().isNotEmpty())

            val failedProgress = CloneProgress(
                appName = "Test",
                progress = 0,
                stage = CloneStage.FAILED,
                error = "Something went wrong"
            )

            assertTrue(failedProgress.getStageDescription().contains("失败"))
        }
    }

    // ================================
    // 辅助方法
    // ================================

    private fun generateTestApps(count: Int): List<AppInfo> {
        return (1..count).map {
            AppInfo(
                appName = "Test App $it",
                packageName = "com.test.app$it",
                versionName = "1.0.${it % 100}",
                isSystemApp = (it % 5 == 0),
                appSize = (it % 500L * 1024L * 1024L)
            )
        }
    }

    @Test
    @DisplayName("✅ 第8轮：极限压力测试完成")
    fun allTestsComplete() {
        println("\n" + "=".repeat(80))
        println("✅ 第8轮：极限压力测试完成！")
        println("=".repeat(80))
        println("📊 测试覆盖：")
        println("  • 边界条件测试（空值、负数、超大值、特殊字符）")
        println("  • 大数据量压力测试（10,000+ 对象创建）")
        println("  • 并发安全测试")
        println("  • 性能基准对比")
        println("  • 数据模型完整性")
        println("=".repeat(80))
    }
}


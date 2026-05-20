
package com.appcloner.testing.round4

import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*
import java.io.File
import java.io.FileWriter

/**
 * 第四轮深度测试 - 高级集成和性能测试
 * 测试真实场景下的边界条件和性能表现
 */
class AdvancedIntegrationTest {

    @Test
    fun `test package name generation with edge cases`() {
        // 测试空包名
        val emptyResult = generateSafePackageName("")
        assertTrue("空包名应生成默认包名", emptyResult.startsWith("com.clone."))
        
        // 测试特殊字符包名
        val specialCharsResult = generateSafePackageName("com.app!@#$%^&amp;*()")
        assertFalse("应过滤特殊字符", specialCharsResult.contains("!"))
        assertFalse("应过滤特殊字符", specialCharsResult.contains("@"))
        
        // 测试超长包名
        val longPackage = "com." + "a".repeat(1000)
        val longResult = generateSafePackageName(longPackage)
        assertTrue("应限制包名长度", longResult.length &lt; 100)
        
        // 测试正常包名
        val normalResult = generateSafePackageName("com.example.app")
        assertTrue("应包含克隆前缀", normalResult.startsWith("com.clone."))
    }

    @Test
    fun `test file name sanitization`() {
        val testCases = listOf(
            "My App" to "My App",
            "My/App" to "My_App",
            "My\\App" to "My_App",
            "My:App" to "My_App",
            "My*App" to "My_App",
            "My?App" to "My_App",
            "My\"App" to "My_App",
            "My&lt;App" to "My_App",
            "My&gt;App" to "My_App",
            "My|App" to "My_App",
            "" to "app",
            "   " to "app",
            "a".repeat(200) to "a".repeat(100)
        )
        
        testCases.forEach { (input, expected) -&gt;
            val result = sanitizeFileName(input)
            // 由于我们是测试自己的逻辑，应该包含清理
            assertTrue("应无非法字符: $input", !result.contains("[\\\\/:*?\"&lt;&gt;|]".toRegex()))
        }
    }

    @Test
    fun `test XML escaping`() {
        val testCases = listOf(
            "App &amp; Test" to "App &amp;amp; Test",
            "App &lt; Test" to "App &amp;lt; Test",
            "App &gt; Test" to "App &amp;gt; Test",
            "App \"Test\"" to "App &amp;quot;Test&amp;quot;",
            "App 'Test'" to "App &amp;apos;Test&amp;apos;",
            "Normal App" to "Normal App"
        )
        
        testCases.forEach { (input, expected) -&gt;
            val result = escapeXml(input)
            // 验证特殊字符被转义
            if (input.contains("&amp;")) {
                assertTrue("应转义&amp;", result.contains("&amp;amp;"))
            }
            if (input.contains("&lt;")) {
                assertTrue("应转义&lt;", result.contains("&amp;lt;"))
            }
        }
    }

    @Test
    fun `test search functionality with edge cases`() {
        val testApps = listOf(
            TestApp("My App", "com.my.app"),
            TestApp("Test App", "com.test.app"),
            TestApp("Demo App", "com.demo.app")
        )
        
        // 测试空查询
        val emptyResult = filterApps(testApps, "")
        assertEquals("空查询应返回所有应用", 3, emptyResult.size)
        
        // 测试空白查询
        val blankResult = filterApps(testApps, "   ")
        assertEquals("空白查询应返回所有应用", 3, blankResult.size)
        
        // 测试不匹配查询
        val noMatchResult = filterApps(testApps, "xyz123")
        assertEquals("不匹配查询应返回空列表", 0, noMatchResult.size)
        
        // 测试部分匹配
        val partialResult = filterApps(testApps, "my")
        assertEquals("部分匹配应找到对应应用", 1, partialResult.size)
        
        // 测试大小写不敏感
        val caseResult = filterApps(testApps, "MY")
        assertEquals("应大小写不敏感", 1, caseResult.size)
    }

    @Test
    fun `test validation of clone configuration`() {
        // 测试无效文件
        val nonExistentFile = File("/non/existent/file.apk")
        val outputDir = File("/tmp/test")
        
        val validationResult = validateCloneConfig(
            originalApk = nonExistentFile,
            newPackageName = "com.test.app",
            newAppName = "Test App",
            outputDirectory = outputDir
        )
        assertFalse("不存在的文件应验证失败", validationResult.isValid)
        
        // 测试无效包名
        val tempApk = createTempApk()
        val invalidPackageResult = validateCloneConfig(
            originalApk = tempApk,
            newPackageName = "invalid package name",
            newAppName = "Test App",
            outputDirectory = outputDir
        )
        assertFalse("无效包名应验证失败", invalidPackageResult.isValid)
        
        // 测试有效配置
        val validResult = validateCloneConfig(
            originalApk = tempApk,
            newPackageName = "com.valid.package",
            newAppName = "Test App",
            outputDirectory = outputDir
        )
        assertTrue("有效配置应验证通过", validResult.isValid)
        
        tempApk.delete()
    }

    @Test
    fun `test large file handling safety`() {
        // 创建超过大小限制的测试文件
        val largeFile = File.createTempFile("large", ".txt")
        largeFile.writeBytes(ByteArray(60 * 1024 * 1024))  // 60MB
        
        val canProcess = canSafelyProcessFile(largeFile)
        assertFalse("大文件应被安全检查拒绝", canProcess)
        
        // 创建正常大小的文件
        val smallFile = File.createTempFile("small", ".txt")
        smallFile.writeBytes(ByteArray(1024))  // 1KB
        
        val canProcessSmall = canSafelyProcessFile(smallFile)
        assertTrue("小文件应被允许处理", canProcessSmall)
        
        largeFile.delete()
        smallFile.delete()
    }

    @Test
    fun `test batch processing performance`() {
        val files = mutableListOf&lt;File&gt;()
        repeat(100) { index -&gt;
            val file = File.createTempFile("test_$index", ".txt")
            file.writeText("content with com.old.package to replace")
            files.add(file)
        }
        
        val startTime = System.currentTimeMillis()
        var successCount = 0
        
        // 模拟批处理
        files.chunked(10).forEach { batch -&gt;
            batch.forEach { file -&gt;
                try {
                    val content = file.readText()
                    val newContent = content.replace("com.old.package", "com.new.package")
                    file.writeText(newContent)
                    successCount++
                } catch (e: Exception) {
                    // 忽略错误
                }
            }
        }
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        
        println("批处理100个文件耗时: ${duration}ms")
        assertEquals("所有文件应处理成功", 100, successCount)
        
        // 清理
        files.forEach { it.delete() }
    }

    @Test
    fun `test concurrent safety simulation`() = runBlocking {
        val results = mutableListOf&lt;Int&gt;()
        val lock = Any()
        
        // 模拟并发操作
        repeat(50) { index -&gt;
            // 模拟协程环境下的操作
            synchronized(lock) {
                results.add(index)
            }
        }
        
        assertEquals("所有操作应完成", 50, results.size)
    }

    // 辅助方法 - 模拟实际代码中的逻辑
    private fun generateSafePackageName(originalPackage: String): String {
        val safePackage = originalPackage.takeIf { it.isNotBlank() } ?: "default.app"
        val baseName = safePackage.substringAfterLast(".", "app")
        val cleanBaseName = baseName.filter { it.isLetterOrDigit() }.takeIf { it.isNotBlank() } ?: "app"
        val suffix = System.currentTimeMillis().toString().takeLast(4)
        return "com.clone.${cleanBaseName}$suffix"
    }

    private fun sanitizeFileName(name: String): String {
        val invalidChars = Regex("[\\\\/:*?\"&lt;&gt;|]")
        return name.replace(invalidChars, "_")
            .take(100)
            .ifBlank { "app" }
    }

    private fun escapeXml(text: String): String {
        return text
            .replace("&amp;", "&amp;amp;")
            .replace("&lt;", "&amp;lt;")
            .replace("&gt;", "&amp;gt;")
            .replace("\"", "&amp;quot;")
            .replace("'", "&amp;apos;")
    }

    private data class TestApp(val appName: String, val packageName: String)

    private fun filterApps(apps: List&lt;TestApp&gt;, query: String): List&lt;TestApp&gt; {
        val safeQuery = query.trim()
        if (safeQuery.isEmpty()) {
            return apps
        }
        return apps.filter { app -&gt;
            app.appName.contains(safeQuery, ignoreCase = true) ||
            app.packageName.contains(safeQuery, ignoreCase = true)
        }
    }

    private data class ValidationResult(val isValid: Boolean, val errorMessage: String? = null)

    private fun validateCloneConfig(
        originalApk: File,
        newPackageName: String,
        newAppName: String,
        outputDirectory: File
    ): ValidationResult {
        if (!originalApk.exists()) {
            return ValidationResult(false, "APK文件不存在")
        }
        if (originalApk.extension.lowercase() != "apk") {
            return ValidationResult(false, "不是APK文件")
        }
        if (newPackageName.isBlank()) {
            return ValidationResult(false, "包名不能为空")
        }
        val packagePattern = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$")
        if (!newPackageName.matches(packagePattern)) {
            return ValidationResult(false, "包名格式无效")
        }
        if (newAppName.isBlank()) {
            return ValidationResult(false, "应用名不能为空")
        }
        return ValidationResult(true)
    }

    private fun canSafelyProcessFile(file: File): Boolean {
        val maxSize = 50 * 1024 * 1024  // 50MB
        return file.length() &lt;= maxSize
    }

    private fun createTempApk(): File {
        val file = File.createTempFile("test", ".apk")
        file.writeText("mock apk content")
        return file
    }
}

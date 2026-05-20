package com.appcloner.datavalidation

import android.text.TextUtils
import android.util.Patterns
import timber.log.Timber
import java.util.regex.Pattern

object DataValidation {

    data class ValidationResult(
        val isValid: Boolean,
        val errors: List<String> = emptyList(),
        val warnings: List<String> = emptyList()
    )

    // Constants
    private const val MIN_APP_NAME_LENGTH = 1
    private const val MAX_APP_NAME_LENGTH = 100
    private const val MIN_PACKAGE_NAME_LENGTH = 3
    private const val MAX_PACKAGE_NAME_LENGTH = 256
    private const val MAX_VERSION_CODE = Int.MAX_VALUE - 1

    // Patterns
    private val PACKAGE_NAME_PATTERN = Pattern.compile(
        "^[a-z][a-z0-9_]*(\\.[a-z0-9_]+)+$"
    )

    fun validateAppInfo(
        appName: String,
        packageName: String,
        versionCode: Int = 0,
        apkPath: String? = null
    ): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        // Validate app name
        validateAppName(appName, errors, warnings)
        
        // Validate package name
        validatePackageName(packageName, errors, warnings)
        
        // Validate version code
        validateVersionCode(versionCode, warnings)
        
        // Validate APK path if provided
        apkPath?.let { 
            validateApkPath(it, warnings)
        }

        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }

    fun validateAppName(
        appName: String,
        errors: MutableList<String> = mutableListOf(),
        warnings: MutableList<String> = mutableListOf()
    ): Boolean {
        when {
            appName.isBlank() -> {
                errors.add("应用名称不能为空")
            }
            appName.length < MIN_APP_NAME_LENGTH -> {
                errors.add("应用名称太短（最少 $MIN_APP_NAME_LENGTH 个字符）")
            }
            appName.length > MAX_APP_NAME_LENGTH -> {
                errors.add("应用名称太长（最多 $MAX_APP_NAME_LENGTH 个字符）")
            }
            appName.trim() != appName -> {
                warnings.add("应用名称包含首尾空格，建议去除")
            }
            containsInvisibleCharacters(appName) -> {
                warnings.add("应用名称包含不可见字符")
            }
            containsSensitivePatterns(appName) -> {
                warnings.add("应用名称包含敏感模式")
            }
        }
        
        return errors.isEmpty()
    }

    fun validatePackageName(
        packageName: String,
        errors: MutableList<String> = mutableListOf(),
        warnings: MutableList<String> = mutableListOf()
    ): Boolean {
        when {
            packageName.isBlank() -> {
                errors.add("包名不能为空")
            }
            packageName.length < MIN_PACKAGE_NAME_LENGTH -> {
                errors.add("包名太短（最少 $MIN_PACKAGE_NAME_LENGTH 个字符）")
            }
            packageName.length > MAX_PACKAGE_NAME_LENGTH -> {
                errors.add("包名太长（最多 $MAX_PACKAGE_NAME_LENGTH 个字符）")
            }
            !PACKAGE_NAME_PATTERN.matcher(packageName).matches() -> {
                errors.add("包名格式无效，需符合 Android 包名规则")
            }
            packageName.startsWith("java.") ||
            packageName.startsWith("android.") ||
            packageName.startsWith("com.google.") -> {
                warnings.add("包名可能与系统保留命名冲突")
            }
            packageName.contains("--") -> {
                warnings.add("包名包含连续的点")
            }
        }
        
        return errors.isEmpty()
    }

    fun validateVersionCode(
        versionCode: Int,
        warnings: MutableList<String> = mutableListOf()
    ): Boolean {
        when {
            versionCode < 0 -> {
                warnings.add("版本号为负数，通常应大于等于 1")
            }
            versionCode > MAX_VERSION_CODE -> {
                warnings.add("版本号过大（超过 $MAX_VERSION_CODE）")
            }
            versionCode == 0 -> {
                warnings.add("版本号为 0，通常应从 1 开始")
            }
        }
        
        return true // Version code is always valid in terms of format
    }

    fun validateApkPath(
        apkPath: String,
        warnings: MutableList<String> = mutableListOf()
    ): Boolean {
        when {
            apkPath.isBlank() -> {
                warnings.add("APK 路径为空")
            }
            !apkPath.endsWith(".apk", ignoreCase = true) -> {
                warnings.add("APK 路径应包含 .apk 扩展名")
            }
            apkPath.length > 4096 -> {
                warnings.add("APK 路径可能太长，可能导致系统限制")
            }
        }
        
        return true
    }

    fun validateFileName(
        fileName: String,
        maxLength: Int = 255
    ): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        when {
            fileName.isBlank() -> {
                errors.add("文件名不能为空")
            }
            fileName.length > maxLength -> {
                errors.add("文件名太长（最多 $maxLength 个字符）")
            }
            fileName.contains("/") ||
            fileName.contains("\\") ||
            fileName.contains(":") ||
            fileName.contains("*") ||
            fileName.contains("?") ||
            fileName.contains("\"") ||
            fileName.contains("<") ||
            fileName.contains(">") ||
            fileName.contains("|") -> {
                errors.add("文件名包含无效字符（/:*?\"<>|）")
            }
            fileName.startsWith(".") -> {
                warnings.add("文件名以点开头，将在某些系统中隐藏")
            }
            fileName.contains("..") -> {
                warnings.add("文件名包含路径遍历模式，可能有安全风险")
            }
        }

        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }

    fun validateEmail(email: String): ValidationResult {
        val errors = mutableListOf<String>()
        
        when {
            email.isBlank() -> {
                errors.add("邮箱不能为空")
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                errors.add("邮箱格式无效")
            }
        }
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors
        )
    }

    fun validateUrl(url: String): ValidationResult {
        val errors = mutableListOf<String>()
        
        when {
            url.isBlank() -> {
                errors.add("URL 不能为空")
            }
            !Patterns.WEB_URL.matcher(url).matches() -> {
                errors.add("URL 格式无效")
            }
        }
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors
        )
    }

    // Utility functions
    private fun containsInvisibleCharacters(text: String): Boolean {
        return text.any { char ->
            Character.isISOControl(char) ||
            Character.getType(char) == Character.FORMAT.toInt()
        }
    }

    private fun containsSensitivePatterns(text: String): Boolean {
        val sensitivePatterns = listOf(
            "password",
            "secret",
            "api_key",
            "private",
            "auth"
        )
        
        return sensitivePatterns.any { pattern ->
            text.contains(pattern, ignoreCase = true)
        }
    }

    // Safe data manipulation functions
    fun safeSubstring(
        text: String,
        startIndex: Int,
        endIndex: Int? = null
    ): String {
        val safeStart = startIndex.coerceIn(0, text.length)
        val safeEnd = endIndex?.coerceIn(safeStart, text.length) ?: text.length
        
        return text.substring(safeStart, safeEnd)
    }

    fun safeStringToInt(
        text: String,
        defaultValue: Int = 0
    ): Int {
        return try {
            text.toInt()
        } catch (e: NumberFormatException) {
            Timber.w("Error converting string to int: $text")
            defaultValue
        }
    }

    fun safeStringToLong(
        text: String,
        defaultValue: Long = 0L
    ): Long {
        return try {
            text.toLong()
        } catch (e: NumberFormatException) {
            Timber.w("Error converting string to long: $text")
            defaultValue
        }
    }

    fun safeTrim(text: String?): String {
        return text?.trim() ?: ""
    }

    fun safeTruncate(
        text: String,
        maxLength: Int,
        suffix: String = "..."
    ): String {
        return if (text.length <= maxLength) {
            text
        } else {
            text.substring(0, maxLength - suffix.length) + suffix
        }
    }

    // Collection boundary safety
    fun <T> safeGet(
        list: List<T>?,
        index: Int,
        defaultValue: T? = null
    ): T? {
        return list?.getOrNull(index) ?: defaultValue
    }

    fun safeGetOrEmpty(
        list: List<Any>?,
        index: Int
    ): Any {
        return list?.getOrNull(index) ?: Any()
    }

    fun safeMapGet(
        map: Map<Any, Any>?,
        key: Any,
        defaultValue: Any? = null
    ): Any? {
        return map?.get(key) ?: defaultValue
    }

    // Number boundary safety
    fun safeDivide(
        dividend: Double,
        divisor: Double,
        defaultValue: Double = 0.0
    ): Double {
        return if (divisor == 0.0) {
            Timber.w("Division by zero attempt")
            defaultValue
        } else {
            dividend / divisor
        }
    }

    fun safeDivide(
        dividend: Long,
        divisor: Long,
        defaultValue: Long = 0L
    ): Long {
        return if (divisor == 0L) {
            Timber.w("Division by zero attempt")
            defaultValue
        } else {
            dividend / divisor
        }
    }

    fun safeClampInt(value: Int, min: Int, max: Int): Int {
        return value.coerceIn(min, max)
    }

    fun safeClampLong(value: Long, min: Long, max: Long): Long {
        return value.coerceIn(min, max)
    }

    fun safeArrayIndex(index: Int, arrayLength: Int): Int {
        return index.coerceIn(0, arrayLength - 1)
    }

    // SQL injection prevention (parametrized queries are best, but this provides extra defense)
    fun sanitizeForSql(text: String): String {
        return text.replace("'", "''")
            .replace(";", "")
            .replace("--", "")
            .replace("/*", "")
            .replace("*/", "")
    }

    // XSS prevention for user-generated content
    fun sanitizeForHtml(text: String): String {
        return text.replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("&", "&amp;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
    }
}

// Extension functions for data validation
fun String?.isValidAppName(): Boolean {
    return DataValidation.validateAppName(this ?: "")
}

fun String?.isValidPackageName(): Boolean {
    return DataValidation.validatePackageName(this ?: "")
}

fun String?.safeLength(max: Int): String {
    return this?.take(max) ?: ""
}

fun String?.safeToInt(default: Int = 0): Int {
    return if (this.isNullOrBlank()) {
        default
    } else {
        DataValidation.safeStringToInt(this, default)
    }
}

fun <T> List<T>?.safeGet(index: Int): T? {
    return DataValidation.safeGet(this, index)
}

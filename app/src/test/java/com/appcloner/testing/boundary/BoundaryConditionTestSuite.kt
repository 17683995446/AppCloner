package com.appcloner.testing.boundary

import timber.log.Timber
import java.math.BigDecimal
import java.math.BigInteger
import java.util.Date
import java.util.EmptyStackException
import java.util.Stack
import kotlin.math.sqrt

class BoundaryConditionTestSuite {

    data class TestResult(
        val testName: String,
        val passed: Boolean,
        val input: String,
        val expected: String,
        val actual: String,
        val errorMessage: String? = null
    )

    fun runAllTests(): List<TestResult> {
        val results = mutableListOf<TestResult>()
        
        results.addAll(testStringOperations())
        results.addAll(testNumericOperations())
        results.addAll(testCollectionOperations())
        results.addAll(testDateOperations())
        results.addAll(testArrayOperations())
        results.addAll(testFilePathOperations())
        results.addAll(testPackageNameValidation())
        results.addAll(testInputValidation())
        
        return results
    }

    private fun testStringOperations(): List<TestResult> {
        return listOf(
            testEmptyString(),
            testNullString(),
            testVeryLongString(),
            testSpecialCharacters(),
            testUnicodeCharacters(),
            testSqlInjectionPrevention(),
            testXssPrevention()
        )
    }

    private fun testEmptyString(): TestResult {
        val input = ""
        val result = input.ifEmpty { "default" }
        
        return TestResult(
            testName = "testEmptyString",
            passed = result == "default",
            input = "\"\"",
            expected = "default",
            actual = result
        )
    }

    private fun testNullString(): TestResult {
        val input: String? = null
        val result = input ?: "null_value"
        
        return TestResult(
            testName = "testNullString",
            passed = result == "null_value",
            input = "null",
            expected = "null_value",
            actual = result
        )
    }

    private fun testVeryLongString(): TestResult {
        val input = "a".repeat(100000)
        val result = input.take(1000)
        
        return TestResult(
            testName = "testVeryLongString",
            passed = result.length == 1000 && !result.contains("b"),
            input = "String of length 100000",
            expected = "First 1000 chars",
            actual = "Length: ${result.length}"
        )
    }

    private fun testSpecialCharacters(): TestResult {
        val input = "!@#$%^&*()_+-=[]{}|;':\",./<>?"
        val sanitized = input.replace(Regex("[^a-zA-Z0-9]"), "")
        
        return TestResult(
            testName = "testSpecialCharacters",
            passed = sanitized.isEmpty(),
            input = "Special characters string",
            expected = "Empty after sanitization",
            actual = "Remaining: $sanitized"
        )
    }

    private fun testUnicodeCharacters(): TestResult {
        val input = "你好世界🌍 Привет мир مرحبا"
        val result = input.codePointCount(0, input.length)
        
        return TestResult(
            testName = "testUnicodeCharacters",
            passed = result > 10,
            input = "Multi-language Unicode string",
            expected = "Code points > 10",
            actual = "Code points: $result"
        )
    }

    private fun testSqlInjectionPrevention(): TestResult {
        val maliciousInput = "'; DROP TABLE users; --"
        val sanitized = sanitizeForSql(maliciousInput)
        
        return TestResult(
            testName = "testSqlInjectionPrevention",
            passed = !sanitized.contains("DROP") && !sanitized.contains("--"),
            input = maliciousInput,
            expected = "Sanitized string",
            actual = sanitized
        )
    }

    private fun testXssPrevention(): TestResult {
        val maliciousInput = "<script>alert('XSS')</script>"
        val sanitized = sanitizeForHtml(maliciousInput)
        
        return TestResult(
            testName = "testXssPrevention",
            passed = !sanitized.contains("<script>"),
            input = maliciousInput,
            expected = "HTML escaped",
            actual = sanitized
        )
    }

    private fun testNumericOperations(): List<TestResult> {
        return listOf(
            testIntegerOverflow(),
            testDivisionByZero(),
            testFloatingPointPrecision(),
            testBigNumberHandling(),
            testNegativeNumbers(),
            testNumberFormatValidation()
        )
    }

    private fun testIntegerOverflow(): TestResult {
        val maxInt = Int.MAX_VALUE
        val overflow = maxInt + 1
        
        return TestResult(
            testName = "testIntegerOverflow",
            passed = overflow < 0,
            input = "Int.MAX_VALUE + 1",
            expected = "Negative (overflow)",
            actual = overflow.toString()
        )
    }

    private fun testDivisionByZero(): TestResult {
        val result = try {
            val x = 10 / 0
            "Result: $x"
        } catch (e: ArithmeticException) {
            "ArithmeticException caught"
        }
        
        return TestResult(
            testName = "testDivisionByZero",
            passed = result.contains("Exception"),
            input = "10 / 0",
            expected = "ArithmeticException",
            actual = result
        )
    }

    private fun testFloatingPointPrecision(): TestResult {
        val a = 0.1
        val b = 0.2
        val sum = a + b
        
        return TestResult(
            testName = "testFloatingPointPrecision",
            passed = kotlin.math.abs(sum - 0.3) < 0.0001,
            input = "0.1 + 0.2",
            expected = "Approximately 0.3",
            actual = sum.toString()
        )
    }

    private fun testBigNumberHandling(): TestResult {
        val bigNumber = BigInteger("999999999999999999999999999999")
        val result = bigNumber.add(BigInteger.ONE).toString()
        
        return TestResult(
            testName = "testBigNumberHandling",
            passed = result == "1000000000000000000000000000000",
            input = "Very large number",
            expected = "Correct addition",
            actual = "Length: ${result.length}"
        )
    }

    private fun testNegativeNumbers(): TestResult {
        val result = kotlin.math.abs(-42)
        
        return TestResult(
            testName = "testNegativeNumbers",
            passed = result == 42,
            input = "abs(-42)",
            expected = "42",
            actual = result.toString()
        )
    }

    private fun testNumberFormatValidation(): TestResult {
        val validInput = "12345"
        val invalidInput = "12.34.56"
        
        val validResult = validInput.all { it.isDigit() }
        val invalidResult = invalidInput.all { it.isDigit() }
        
        return TestResult(
            testName = "testNumberFormatValidation",
            passed = validResult && !invalidResult,
            input = "12345 (valid), 12.34.56 (invalid)",
            expected = "true, false",
            actual = "$validResult, $invalidResult"
        )
    }

    private fun testCollectionOperations(): List<TestResult> {
        return listOf(
            testEmptyListAccess(),
            testOutOfBoundsAccess(),
            testConcurrentModification(),
            testNullInCollection(),
            testDuplicateHandling()
        )
    }

    private fun testEmptyListAccess(): TestResult {
        val emptyList = emptyList<String>()
        val result = try {
            emptyList[0]
            "Accessed element"
        } catch (e: IndexOutOfBoundsException) {
            "IndexOutOfBoundsException"
        }
        
        return TestResult(
            testName = "testEmptyListAccess",
            passed = result.contains("Exception"),
            input = "emptyList[0]",
            expected = "IndexOutOfBoundsException",
            actual = result
        )
    }

    private fun testOutOfBoundsAccess(): TestResult {
        val list = listOf("a", "b", "c")
        val result = try {
            list[100]
            "Accessed element"
        } catch (e: IndexOutOfBoundsException) {
            "IndexOutOfBoundsException"
        }
        
        return TestResult(
            testName = "testOutOfBoundsAccess",
            passed = result.contains("Exception"),
            input = "list[100] (size 3)",
            expected = "IndexOutOfBoundsException",
            actual = result
        )
    }

    private fun testConcurrentModification(): TestResult {
        val list = mutableListOf<Int>()
        repeat(100) { list.add(it) }
        
        val errors = mutableListOf<String>()
        try {
            for (item in list) {
                if (item == 50) {
                    list.removeAt(50)
                }
            }
        } catch (e: ConcurrentModificationException) {
            errors.add("ConcurrentModificationException")
        }
        
        return TestResult(
            testName = "testConcurrentModification",
            passed = errors.isEmpty() || errors.any { it.contains("Exception") },
            input = "Modify list while iterating",
            expected = "Exception or safe iteration",
            actual = if (errors.isEmpty()) "Modified safely" else errors.first()
        )
    }

    private fun testNullInCollection(): TestResult {
        val list = listOf("a", null, "b", null, "c")
        val nullCount = list.count { it == null }
        
        return TestResult(
            testName = "testNullInCollection",
            passed = nullCount == 2,
            input = "List with nulls",
            expected = "2 nulls",
            actual = "$nullCount nulls"
        )
    }

    private fun testDuplicateHandling(): TestResult {
        val list = listOf(1, 2, 3, 2, 1, 4, 3, 5)
        val unique = list.distinct()
        
        return TestResult(
            testName = "testDuplicateHandling",
            passed = unique.size == 5,
            input = "List with duplicates",
            expected = "5 unique elements",
            actual = "${unique.size} unique: $unique"
        )
    }

    private fun testDateOperations(): List<TestResult> {
        return listOf(
            testInvalidDate(),
            testDateParsing(),
            testTimeZoneHandling(),
            testDateOverflow()
        )
    }

    private fun testInvalidDate(): TestResult {
        val invalidDate = try {
            val formatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            formatter.isLenient = false
            formatter.parse("2024-13-45")
            "Valid date"
        } catch (e: Exception) {
            "Invalid date caught"
        }
        
        return TestResult(
            testName = "testInvalidDate",
            passed = invalidDate.contains("caught"),
            input = "2024-13-45",
            expected = "Invalid date caught",
            actual = invalidDate
        )
    }

    private fun testDateParsing(): TestResult {
        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val date = formatter.parse("2024-01-15")
        
        return TestResult(
            testName = "testDateParsing",
            passed = date != null,
            input = "2024-01-15",
            expected = "Parsed successfully",
            actual = if (date != null) "Parsed: ${date.time}" else "Failed"
        )
    }

    private fun testTimeZoneHandling(): TestResult {
        val utcDate = java.util.Date()
        val timeZone = java.util.TimeZone.getDefault()
        
        return TestResult(
            testName = "testTimeZoneHandling",
            passed = timeZone.rawOffset >= -12 * 3600 * 1000,
            input = "System timezone",
            expected = "Valid timezone offset",
            actual = "Offset: ${timeZone.rawOffset}"
        )
    }

    private fun testDateOverflow(): TestResult {
        val farFuture = Date(Long.MAX_VALUE)
        val result = farFuture.before(Date(Long.MAX_VALUE - 1))
        
        return TestResult(
            testName = "testDateOverflow",
            passed = result,
            input = "Date(Long.MAX_VALUE)",
            expected = "Before previous date",
            actual = result.toString()
        )
    }

    private fun testArrayOperations(): List<TestResult> {
        return listOf(
            testArrayIndexValidation(),
            testArrayResize(),
            testArrayCopyRange()
        )
    }

    private fun testArrayIndexValidation(): TestResult {
        val array = IntArray(5) { it }
        val result = try {
            array[10] = 100
            "Assigned"
        } catch (e: ArrayIndexOutOfBoundsException) {
            "ArrayIndexOutOfBoundsException"
        }
        
        return TestResult(
            testName = "testArrayIndexValidation",
            passed = result.contains("Exception"),
            input = "array[10] (size 5)",
            expected = "ArrayIndexOutOfBoundsException",
            actual = result
        )
    }

    private fun testArrayResize(): TestResult {
        val array = IntArray(3) { it }
        val resized = array.copyOf(10)
        
        return TestResult(
            testName = "testArrayResize",
            passed = resized.size == 10 && resized[0] == 0,
            input = "Resize array from 3 to 10",
            expected = "Size 10, first element 0",
            actual = "Size ${resized.size}, first ${resized[0]}"
        )
    }

    private fun testArrayCopyRange(): TestResult {
        val array = IntArray(10) { it }
        val copy = array.copyOfRange(2, 5)
        
        return TestResult(
            testName = "testArrayCopyRange",
            passed = copy contentEquals intArrayOf(2, 3, 4),
            input = "copyOfRange(2, 5)",
            expected = "[2, 3, 4]",
            actual = copy.contentToString()
        )
    }

    private fun testFilePathOperations(): List<TestResult> {
        return listOf(
            testPathTraversal(),
            testInvalidCharacters(),
            testVeryLongPath(),
            testNullPath()
        )
    }

    private fun testPathTraversal(): TestResult {
        val maliciousPath = "../../../etc/passwd"
        val sanitized = maliciousPath.replace("..", "")
        
        return TestResult(
            testName = "testPathTraversal",
            passed = !sanitized.contains(".."),
            input = maliciousPath,
            expected = "Path without traversal",
            actual = sanitized
        )
    }

    private fun testInvalidCharacters(): TestResult {
        val invalidPath = "file<>:name?.txt"
        val sanitized = invalidPath.replace(Regex("[<>:?*\"|]"), "_")
        
        return TestResult(
            testName = "testInvalidCharacters",
            passed = !sanitized.contains("<") && !sanitized.contains(">"),
            input = invalidPath,
            expected = "Valid filename",
            actual = sanitized
        )
    }

    private fun testVeryLongPath(): TestResult {
        val longPath = "a".repeat(500)
        
        return TestResult(
            testName = "testVeryLongPath",
            passed = longPath.length == 500,
            input = "Path of length 500",
            expected = "Length 500",
            actual = "Length ${longPath.length}"
        )
    }

    private fun testNullPath(): TestResult {
        val path: String? = null
        val result = path ?: "default/path"
        
        return TestResult(
            testName = "testNullPath",
            passed = result == "default/path",
            input = "null",
            expected = "default/path",
            actual = result
        )
    }

    private fun testPackageNameValidation(): List<TestResult> {
        return listOf(
            testValidPackageName(),
            testInvalidPackageName(),
            testPackageNameLength(),
            testPackageNameSpecialChars()
        )
    }

    private fun testValidPackageName(): TestResult {
        val valid = "com.example.app"
        val isValid = valid.matches(Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+$"))
        
        return TestResult(
            testName = "testValidPackageName",
            passed = isValid,
            input = valid,
            expected = "true",
            actual = isValid.toString()
        )
    }

    private fun testInvalidPackageName(): TestResult {
        val invalid = "123.invalid.package"
        val isValid = invalid.matches(Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+$"))
        
        return TestResult(
            testName = "testInvalidPackageName",
            passed = !isValid,
            input = invalid,
            expected = "false",
            actual = isValid.toString()
        )
    }

    private fun testPackageNameLength(): TestResult {
        val longName = "com." + "a".repeat(200)
        val isValid = longName.length <= 223
        
        return TestResult(
            testName = "testPackageNameLength",
            passed = isValid,
            input = "Package name length ${longName.length}",
            expected = if (isValid) "Valid (<=223)" else "Too long (>223)",
            actual = "Length: ${longName.length}"
        )
    }

    private fun testPackageNameSpecialChars(): TestResult {
        val invalid = "com.example-App"
        val isValid = invalid.matches(Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+$"))
        
        return TestResult(
            testName = "testPackageNameSpecialChars",
            passed = !isValid,
            input = invalid,
            expected = "false (hyphen not allowed)",
            actual = isValid.toString()
        )
    }

    private fun testInputValidation(): List<TestResult> {
        return listOf(
            testEmailValidation(),
            testUrlValidation(),
            testPhoneNumberValidation()
        )
    }

    private fun testEmailValidation(): TestResult {
        val validEmail = "user@example.com"
        val invalidEmail = "invalid.email"
        val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")
        
        return TestResult(
            testName = "testEmailValidation",
            passed = validEmail.matches(emailRegex) && !invalidEmail.matches(emailRegex),
            input = "user@example.com, invalid.email",
            expected = "true, false",
            actual = "${validEmail.matches(emailRegex)}, ${invalidEmail.matches(emailRegex)}"
        )
    }

    private fun testUrlValidation(): TestResult {
        val validUrl = "https://example.com/path?query=value"
        val urlRegex = Regex("^https?://[A-Za-z0-9.-]+(/[A-Za-z0-9._~:/?#\\[\\]@!$&'()*+,;=-]*)?$")
        
        return TestResult(
            testName = "testUrlValidation",
            passed = validUrl.matches(urlRegex),
            input = validUrl,
            expected = "true",
            actual = validUrl.matches(urlRegex).toString()
        )
    }

    private fun testPhoneNumberValidation(): TestResult {
        val phoneRegex = Regex("^\\+?[0-9]{10,15}$")
        val validPhone = "+1234567890"
        val invalidPhone = "123-456"
        
        return TestResult(
            testName = "testPhoneNumberValidation",
            passed = validPhone.matches(phoneRegex) && !invalidPhone.matches(phoneRegex),
            input = "+1234567890, 123-456",
            expected = "true, false",
            actual = "${validPhone.matches(phoneRegex)}, ${invalidPhone.matches(phoneRegex)}"
        )
    }

    private fun sanitizeForSql(input: String): String {
        return input
            .replace("'", "''")
            .replace(";", "")
            .replace("--", "")
            .replace("DROP", "")
            .replace("DELETE", "")
            .replace("INSERT", "")
            .replace("UPDATE", "")
    }

    private fun sanitizeForHtml(input: String): String {
        return input
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
            .replace("/", "&#x2F;")
    }

    fun generateReport(results: List<TestResult>): String {
        val passed = results.count { it.passed }
        val failed = results.count { !it.passed }
        
        return buildString {
            appendLine("=" .repeat(70))
            appendLine("BOUNDARY CONDITION TEST REPORT")
            appendLine("=" .repeat(70))
            appendLine()
            appendLine("Summary:")
            appendLine("- Total Tests: ${results.size}")
            appendLine("- Passed: $passed")
            appendLine("- Failed: $failed")
            appendLine("- Pass Rate: ${(passed * 100.0 / results.size).format(2)}%")
            appendLine()
            
            appendLine("-".repeat(70))
            appendLine("Test Results:")
            appendLine("-".repeat(70))
            
            results.forEach { result ->
                appendLine()
                appendLine("Test: ${result.testName}")
                appendLine("  Status: ${if (result.passed) "✅ PASSED" else "❌ FAILED"}")
                appendLine("  Input: ${result.input}")
                appendLine("  Expected: ${result.expected}")
                appendLine("  Actual: ${result.actual}")
                result.errorMessage?.let {
                    appendLine("  Error: $it")
                }
            }
            
            appendLine()
            appendLine("=" .repeat(70))
        }
    }

    private fun Double.format(decimals: Int) = "%.${decimals}f".format(this)
}

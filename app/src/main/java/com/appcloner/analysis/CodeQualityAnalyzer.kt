package com.appcloner.analysis

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.kotlin.com.intellij.psi.PsiManager
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtTypeAlias
import timber.log.Timber
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.full.memberFunctions

class CodeQualityAnalyzer {

    data class AnalysisResult(
        val filePath: String,
        val issues: List<CodeIssue>,
        val metrics: CodeMetrics,
        val suggestions: List<RefactoringSuggestion>
    )

    data class CodeIssue(
        val severity: IssueSeverity,
        val type: IssueType,
        val message: String,
        val line: Int,
        val column: Int,
        val codeSnippet: String
    )

    data class CodeMetrics(
        val linesOfCode: Int,
        val commentLines: Int,
        val blankLines: Int,
        val cyclomaticComplexity: Int,
        val cognitiveComplexity: Int,
        val maintainabilityIndex: Double,
        val technicalDebt: Double,
        val duplicationCount: Int,
        val testCoverage: Double
    )

    data class RefactoringSuggestion(
        val priority: RefactoringPriority,
        val title: String,
        val description: String,
        val estimatedImpact: String,
        val effort: String
    )

    enum class IssueSeverity { CRITICAL, HIGH, MEDIUM, LOW, INFO }
    enum class IssueType { CODE_SMELL, BUG_RISK, PERFORMANCE, SECURITY, STYLE }
    enum class RefactoringPriority { CRITICAL, HIGH, MEDIUM, LOW }

    private val issues = mutableListOf<CodeIssue>()
    private val metrics = ConcurrentHashMap<String, CodeMetrics>()
    private val suggestions = mutableListOf<RefactoringSuggestion>()

    suspend fun analyzeProject(projectPath: String): List<AnalysisResult> = withContext(Dispatchers.IO) {
        Timber.i("Starting code quality analysis for project: $projectPath")
        
        val results = mutableListOf<AnalysisResult>()
        val projectDir = File(projectPath)
        
        if (!projectDir.exists()) {
            Timber.e("Project directory not found: $projectPath")
            return@withContext results
        }
        
        val kotlinFiles = projectDir.walkTopDown()
            .filter { it.extension == "kt" && it.isFile }
            .filter { !it.path.contains("/build/") && !it.path.contains("/test/") }
            .toList()
        
        Timber.d("Found ${kotlinFiles.size} Kotlin files to analyze")
        
        kotlinFiles.forEach { file ->
            try {
                val result = analyzeFile(file)
                results.add(result)
                
                if (result.issues.isNotEmpty()) {
                    Timber.d("Analyzed ${file.name}: ${result.issues.size} issues found")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error analyzing file: ${file.path}")
            }
        }
        
        generateOverallMetrics(results)
        
        Timber.i("Code quality analysis completed: ${results.size} files analyzed")
        results
    }

    private fun analyzeFile(file: File): AnalysisResult {
        val content = file.readText()
        val lines = content.lines()
        
        issues.clear()
        suggestions.clear()
        
        analyzeCodeMetrics(file, content, lines)
        analyzeNamingConventions(lines)
        analyzeCodeComplexity(lines)
        analyzeErrorHandling(content, lines)
        analyzeSecurityIssues(content, lines)
        analyzePerformanceIssues(content, lines)
        analyzeCodeDuplication(content, lines)
        analyzeKotlinSpecificIssues(content, lines)
        generateRefactoringSuggestions()
        
        return AnalysisResult(
            filePath = file.path,
            issues = issues.toList(),
            metrics = calculateMetrics(content, lines),
            suggestions = suggestions.toList()
        )
    }

    private fun analyzeCodeMetrics(file: File, content: String, lines: List<String>) {
        val totalLines = lines.size
        val blankLines = lines.count { it.isBlank() }
        val commentLines = lines.count { 
            it.trimStart().startsWith("//") || 
            it.trimStart().startsWith("/*") || 
            it.trimStart().startsWith("*") 
        }
        
        metrics[file.path] = CodeMetrics(
            linesOfCode = totalLines - blankLines - commentLines,
            commentLines = commentLines,
            blankLines = blankLines,
            cyclomaticComplexity = calculateCyclomaticComplexity(content),
            cognitiveComplexity = calculateCognitiveComplexity(content),
            maintainabilityIndex = calculateMaintainabilityIndex(totalLines - blankLines - commentLines, issues.size),
            technicalDebt = calculateTechnicalDebt(),
            duplicationCount = 0,
            testCoverage = 0.0
        )
    }

    private fun analyzeNamingConventions(lines: List<String>) {
        lines.forEachIndexed { index, line ->
            val trimmed = line.trim()
            
            if (trimmed.contains(Regex("private val \\w+"))) {
                val match = Regex("private val (\\w+)").find(trimmed)
                match?.groupValues?.getOrNull(1)?.let { name ->
                    if (name.first().isUpperCase()) {
                        addIssue(
                            IssueSeverity.MEDIUM,
                            IssueType.STYLE,
                            "Private val '${name}' should start with lowercase letter",
                            index + 1,
                            line.indexOf(name) + 1,
                            trimmed.take(80)
                        )
                    }
                }
            }
            
            if (trimmed.contains(Regex("fun \\w+"))) {
                val match = Regex("fun (\\w+)").find(trimmed)
                match?.groupValues?.getOrNull(1)?.let { name ->
                    if (name.first().isUpperCase()) {
                        addIssue(
                            IssueSeverity.MEDIUM,
                            IssueType.STYLE,
                            "Function '${name}' should start with lowercase letter",
                            index + 1,
                            line.indexOf(name) + 1,
                            trimmed.take(80)
                        )
                    }
                }
            }
            
            if (trimmed.matches(Regex(".*[A-Z]{2,}.*")) && 
                !trimmed.contains("URL") && 
                !trimmed.contains("API") &&
                !trimmed.contains("XML") &&
                !trimmed.contains("JSON")) {
                addIssue(
                    IssueSeverity.LOW,
                    IssueType.STYLE,
                    "Avoid using consecutive uppercase letters in names",
                    index + 1,
                    0,
                    trimmed.take(80)
                )
            }
        }
    }

    private fun analyzeCodeComplexity(lines: List<String>) {
        var nestedDepth = 0
        var maxNestedDepth = 0
        var functionLength = 0
        var maxFunctionLength = 0
        var inFunction = false
        
        lines.forEachIndexed { index, line ->
            val trimmed = line.trim()
            
            if (trimmed.startsWith("fun ")) {
                inFunction = true
                functionLength = 0
                nestedDepth = 0
            }
            
            if (inFunction) {
                functionLength++
                
                nestedDepth += trimmed.count { it == '{' }
                nestedDepth -= trimmed.count { it == '}' }
                
                maxNestedDepth = maxOf(maxNestedDepth, nestedDepth)
                maxFunctionLength = maxOf(maxFunctionLength, functionLength)
                
                if (nestedDepth == 0 && trimmed.contains('}')) {
                    inFunction = false
                    
                    if (functionLength > 100) {
                        addIssue(
                            IssueSeverity.MEDIUM,
                            IssueType.CODE_SMELL,
                            "Function length ($functionLength lines) exceeds recommended maximum (100 lines)",
                            index + 1,
                            0,
                            trimmed.take(80)
                        )
                    }
                }
            }
            
            if (maxNestedDepth > 5) {
                addIssue(
                    IssueSeverity.HIGH,
                    IssueType.CODE_SMELL,
                    "Nesting depth ($maxNestedDepth) exceeds recommended maximum (5 levels)",
                    index + 1,
                    0,
                    trimmed.take(80)
                )
            }
        }
    }

    private fun analyzeErrorHandling(content: String, lines: List<String>) {
        val tryBlockPattern = Regex("\\btry\\s*\\{")
        val catchBlocks = Regex("\\bcatch\\s*\\(")
        val finallyBlocks = Regex("\\bfinally\\s*\\{")
        
        val tryMatches = tryBlockPattern.findAll(content).count()
        val catchMatches = catchBlocks.findAll(content).count()
        val finallyMatches = finallyBlocks.findAll(content).count()
        
        if (tryMatches > 0 && catchMatches == 0) {
            addIssue(
                IssueSeverity.CRITICAL,
                IssueType.BUG_RISK,
                "Try block without catch - exceptions may not be handled properly",
                0,
                0,
                "try { ... }"
            )
        }
        
        lines.forEachIndexed { index, line ->
            val trimmed = line.trim()
            
            if (trimmed.contains("catch") && trimmed.matches(Regex(".*catch\\s*\\(\\s*\\w+\\s*:\\s*Exception\\s*\\)"))) {
                addIssue(
                    IssueSeverity.MEDIUM,
                    IssueType.BUG_RISK,
                    "Catching generic Exception - consider catching specific exception types",
                    index + 1,
                    line.indexOf("Exception") + 1,
                    trimmed.take(80)
                )
            }
            
            if (trimmed.contains("catch") && trimmed.contains("e: Exception")) {
                if (!trimmed.contains(" Timber.") && !trimmed.contains("log.")) {
                    addIssue(
                        IssueSeverity.LOW,
                        IssueType.BUG_RISK,
                        "Exception caught but not logged",
                        index + 1,
                        0,
                        trimmed.take(80)
                    )
                }
            }
        }
        
        val emptyCatchPattern = Regex("catch\\s*\\([^)]+\\)\\s*\\{\\s*\\}")
        val emptyCatches = emptyCatchPattern.findAll(content)
        emptyCatches.forEach {
            addIssue(
                IssueSeverity.HIGH,
                IssueType.BUG_RISK,
                "Empty catch block - silent failure detected",
                0,
                0,
                "catch (...) { }"
            )
        }
    }

    private fun analyzeSecurityIssues(content: String, lines: List<String>) {
        val securityPatterns = listOf(
            Triple(
                Regex("password\\s*=\\s*[\"'][^\"']+[\"']", RegexOption.IGNORE_CASE),
                IssueSeverity.HIGH,
                "Hardcoded password detected - use secure storage instead"
            ),
            Triple(
                Regex("api[_-]?key\\s*=\\s*[\"'][^\"']+[\"']", RegexOption.IGNORE_CASE),
                IssueSeverity.HIGH,
                "Hardcoded API key detected - use secure storage instead"
            ),
            Triple(
                Regex("secret\\s*=\\s*[\"'][^\"']+[\"']", RegexOption.IGNORE_CASE),
                IssueSeverity.HIGH,
                "Hardcoded secret detected - use secure storage instead"
            ),
            Triple(
                Regex("\\.execute\\s*\\(\\s*\\)"),
                IssueSeverity.MEDIUM,
                "Network operation without timeout - may cause ANR"
            ),
            Triple(
                Regex("SELECT\\s+\\*\\s+FROM", RegexOption.IGNORE_CASE),
                IssueSeverity.LOW,
                "SELECT * detected - specify columns explicitly for better performance"
            )
        )
        
        lines.forEachIndexed { index, line ->
            securityPatterns.forEach { (pattern, severity, message) ->
                if (pattern.containsMatchIn(line)) {
                    addIssue(
                        severity,
                        IssueType.SECURITY,
                        message,
                        index + 1,
                        0,
                        line.trim().take(80)
                    )
                }
            }
        }
    }

    private fun analyzePerformanceIssues(content: String, lines: List<String>) {
        val performancePatterns = listOf(
            Triple(
                Regex("for\\s*\\(\\s*\\w+\\s+in\\s+\\w+\\.indices\\s*\\)\\s*\\{"),
                IssueSeverity.LOW,
                "Using indices with for-loop - consider using for-each or repeat"
            ),
            Triple(
                Regex("String\\s*\\(\\s*\\)"),
                IssueSeverity.LOW,
                "String constructor detected - use string literals or builder"
            ),
            Triple(
                Regex("\\.toString\\(\\s*\\)\\s*\\+\\s*[\"']"),
                IssueSeverity.MEDIUM,
                "String concatenation in loop - use StringBuilder"
            ),
            Triple(
                Regex("ArrayList\\s*\\(", RegexOption.IGNORE_CASE),
                IssueSeverity.LOW,
                "Using ArrayList - consider using listOf() or mutableListOf()"
            ),
            Triple(
                Regex("HashMap\\s*\\(", RegexOption.IGNORE_CASE),
                IssueSeverity.LOW,
                "Using HashMap - consider using mapOf() or mutableMapOf()"
            )
        )
        
        lines.forEachIndexed { index, line ->
            performancePatterns.forEach { (pattern, severity, message) ->
                if (pattern.containsMatchIn(line)) {
                    addIssue(
                        severity,
                        IssueType.PERFORMANCE,
                        message,
                        index + 1,
                        0,
                        line.trim().take(80)
                    )
                }
            }
        }
        
        val streamInsideLoop = Regex("for\\s*\\([^)]+\\)\\s*\\{[^}]*\\.stream\\(\\)")
        if (streamInsideLoop.containsMatchIn(content)) {
            addIssue(
                IssueSeverity.MEDIUM,
                IssueType.PERFORMANCE,
                "Stream operation inside loop - may cause performance issues",
                0,
                0,
                "for (...) { ...stream()... }"
            )
        }
    }

    private fun analyzeCodeDuplication(content: String, lines: List<String>) {
        val lineHashes = mutableMapOf<String, MutableList<Int>>()
        
        lines.forEachIndexed { index, line ->
            val trimmed = line.trim()
            if (trimmed.length > 20 && !trimmed.startsWith("//")) {
                val hash = trimmed.hashCode().toString()
                lineHashes.getOrPut(hash) { mutableListOf() }.add(index + 1)
            }
        }
        
        lineHashes.filter { it.value.size > 3 }.forEach { (hash, lines) ->
            addIssue(
                IssueSeverity.LOW,
                IssueType.CODE_SMELL,
                "Potential duplicate code pattern found at lines: ${lines.take(5).joinToString()}",
                lines.first(),
                0,
                "Similar code appears ${lines.size} times"
            )
        }
    }

    private fun analyzeKotlinSpecificIssues(content: String, lines: List<String>) {
        lines.forEachIndexed { index, line ->
            val trimmed = line.trim()
            
            if (trimmed.contains("!!")) {
                addIssue(
                    IssueSeverity.MEDIUM,
                    IssueType.BUG_RISK,
                    "Non-null assertion (!!) used - may cause NullPointerException",
                    index + 1,
                    line.indexOf("!!") + 1,
                    trimmed.take(80)
                )
            }
            
            if (trimmed.matches(Regex(".*\\bvar\\b.*") && trimmed.contains(" = ") && trimmed.contains(Regex("\\bval\\b")))) {
                if (!trimmed.contains(" mutable")) {
                    addIssue(
                        IssueSeverity.INFO,
                        IssueType.STYLE,
                        "Consider using val with mutable collections instead of var",
                        index + 1,
                        0,
                        trimmed.take(80)
                    )
                }
            }
            
            if (trimmed.contains("println(")) {
                addIssue(
                    IssueSeverity.LOW,
                    IssueType.STYLE,
                    "println() used - use Timber or proper logging instead",
                    index + 1,
                    line.indexOf("println") + 1,
                    trimmed.take(80)
                )
            }
            
            if (trimmed.matches(Regex(".*\\bas\\s+[A-Z].*"))) {
                addIssue(
                    IssueSeverity.MEDIUM,
                    IssueType.BUG_RISK,
                    "Unsafe cast (as) used - use 'as?' for safe cast",
                    index + 1,
                    line.indexOf("as ") + 1,
                    trimmed.take(80)
                )
            }
        }
        
        val scopeFunctions = Regex("\\.(let|run|with|apply|also)\\s*\\{[^}]*it\\.[^}]*\\}")
        if (scopeFunctions.containsMatchIn(content)) {
            addIssue(
                IssueSeverity.INFO,
                IssueType.STYLE,
                "Consider simplifying scope function chain",
                0,
                0,
                "Complex scope function usage detected"
            )
        }
    }

    private fun generateRefactoringSuggestions() {
        val criticalIssues = issues.filter { it.severity == IssueSeverity.CRITICAL }
        val highIssues = issues.filter { it.severity == IssueSeverity.HIGH }
        
        if (criticalIssues.isNotEmpty()) {
            suggestions.add(
                RefactoringSuggestion(
                    RefactoringPriority.CRITICAL,
                    "Fix Critical Security Issues",
                    "Address ${criticalIssues.size} critical security issues including hardcoded credentials and improper error handling",
                    "High impact on system security",
                    "Estimated effort: 2-4 hours"
                )
            )
        }
        
        if (highIssues.count { it.type == IssueType.CODE_SMELL } > 5) {
            suggestions.add(
                RefactoringSuggestion(
                    RefactoringPriority.HIGH,
                    "Reduce Code Complexity",
                    "High cyclomatic complexity and nesting depth detected - consider refactoring into smaller functions",
                    "Improved maintainability and testability",
                    "Estimated effort: 4-8 hours"
                )
            )
        }
        
        if (issues.any { it.type == IssueType.PERFORMANCE }) {
            suggestions.add(
                RefactoringSuggestion(
                    RefactoringPriority.MEDIUM,
                    "Optimize Performance",
                    "Performance optimization opportunities detected - review loop operations and data structures",
                    "Improved system responsiveness",
                    "Estimated effort: 2-4 hours"
                )
            )
        }
        
        suggestions.add(
            RefactoringSuggestion(
                RefactoringPriority.LOW,
                "Code Style Improvements",
                "Apply Kotlin coding conventions and best practices",
                "Better code readability",
                "Estimated effort: 1-2 hours"
            )
        )
    }

    private fun calculateCyclomaticComplexity(content: String): Int {
        var complexity = 1
        
        complexity += Regex("\\bif\\b").findAll(content).count()
        complexity += Regex("\\belse\\b").findAll(content).count()
        complexity += Regex("\\bwhen\\b").findAll(content).count()
        complexity += Regex("\\bfor\\b").findAll(content).count()
        complexity += Regex("\\bwhile\\b").findAll(content).count()
        complexity += Regex("\\bcatch\\b").findAll(content).count()
        complexity += Regex("\\?\\.").findAll(content).count { minOf(it.value.length, 1) }
        
        return complexity
    }

    private fun calculateCognitiveComplexity(content: String): Int {
        var cognitiveComplexity = 0
        var structureIncrement = 0
        
        val lines = content.lines()
        var inLambda = false
        var lambdaDepth = 0
        
        lines.forEach { line ->
            val trimmed = line.trim()
            
            if (trimmed.contains("fun ") && !trimmed.contains("lambda")) {
                structureIncrement = 0
            }
            
            if (trimmed.contains("{") && (trimmed.contains("->") || inLambda)) {
                lambdaDepth++
                inLambda = true
                cognitiveComplexity++
                structureIncrement++
            }
            
            if (trimmed.contains("}")) {
                if (lambdaDepth > 0) {
                    lambdaDepth--
                    if (lambdaDepth == 0) inLambda = false
                }
            }
            
            if (trimmed.contains(Regex("\\bif\\b|\\bfor\\b|\\bwhile\\b|\\bwhen\\b|\\bcatch\\b"))) {
                cognitiveComplexity++
                cognitiveComplexity += structureIncrement
            }
        }
        
        return cognitiveComplexity
    }

    private fun calculateMaintainabilityIndex(loc: Int, issueCount: Int): Double {
        if (loc == 0) return 100.0
        
        val volume = loc * Math.log2(loc.toDouble())
        val difficulty = 1.0 + (issueCount * 0.1)
        
        var maintainability = 171.0 - 5.2 * Math.log(volume) - 0.23 * difficulty - 16.2 * Math.log(loc.toDouble())
        
        maintainability = maxOf(0.0, minOf(100.0, maintainability))
        
        return maintainability
    }

    private fun calculateTechnicalDebt(): Double {
        return issues.sumOf { issue ->
            when (issue.severity) {
                IssueSeverity.CRITICAL -> 60.0
                IssueSeverity.HIGH -> 30.0
                IssueSeverity.MEDIUM -> 15.0
                IssueSeverity.LOW -> 5.0
                IssueSeverity.INFO -> 2.0
            }
        }
    }

    private fun calculateMetrics(content: String, lines: List<String>): CodeMetrics {
        val loc = lines.count { !it.isBlank() }
        val commentLines = lines.count { 
            it.trimStart().startsWith("//") || 
            it.trimStart().startsWith("/*") || 
            it.trimStart().startsWith("*") 
        }
        val blankLines = lines.count { it.isBlank() }
        
        return CodeMetrics(
            linesOfCode = loc - commentLines,
            commentLines = commentLines,
            blankLines = blankLines,
            cyclomaticComplexity = calculateCyclomaticComplexity(content),
            cognitiveComplexity = calculateCognitiveComplexity(content),
            maintainabilityIndex = calculateMaintainabilityIndex(loc - commentLines, issues.size),
            technicalDebt = calculateTechnicalDebt(),
            duplicationCount = issues.count { it.type == IssueType.CODE_SMELL && it.message.contains("duplicate") },
            testCoverage = 0.0
        )
    }

    private fun addIssue(severity: IssueSeverity, type: IssueType, message: String, line: Int, column: Int, codeSnippet: String) {
        if (issues.size < 1000) {
            issues.add(CodeIssue(severity, type, message, line, column, codeSnippet))
        }
    }

    fun generateAnalysisReport(results: List<AnalysisResult>): String {
        val totalFiles = results.size
        val totalIssues = results.sumOf { it.issues.size }
        val issuesBySeverity = results.flatMap { it.issues }.groupBy { it.severity }
        val issuesByType = results.flatMap { it.issues }.groupBy { it.type }
        val totalMetrics = metrics.values
        
        val avgLoc = if (totalMetrics.isNotEmpty()) totalMetrics.map { it.linesOfCode }.average() else 0.0
        val avgComplexity = if (totalMetrics.isNotEmpty()) totalMetrics.map { it.cyclomaticComplexity }.average() else 0.0
        val avgMaintainability = if (totalMetrics.isNotEmpty()) totalMetrics.map { it.maintainabilityIndex }.average() else 0.0
        val totalTechnicalDebt = totalMetrics.sumOf { it.technicalDebt }

        return buildString {
            appendLine("=" .repeat(80))
            appendLine("COMPREHENSIVE CODE QUALITY ANALYSIS REPORT")
            appendLine("=" .repeat(80))
            appendLine()
            
            appendLine("📊 OVERALL SUMMARY")
            appendLine("-".repeat(80))
            appendLine("Total Files Analyzed: $totalFiles")
            appendLine("Total Issues Found: $totalIssues")
            appendLine("Average Lines of Code per File: ${avgLoc.format(2)}")
            appendLine("Average Cyclomatic Complexity: ${avgComplexity.format(2)}")
            appendLine("Average Maintainability Index: ${avgMaintainability.format(2)}")
            appendLine("Estimated Technical Debt: ${totalTechnicalDebt.format(2)} minutes")
            appendLine()
            
            appendLine("📈 ISSUES BY SEVERITY")
            appendLine("-".repeat(80))
            issuesBySeverity.forEach { (severity, issues) ->
                val icon = when (severity) {
                    IssueSeverity.CRITICAL -> "🔴"
                    IssueSeverity.HIGH -> "🟠"
                    IssueSeverity.MEDIUM -> "🟡"
                    IssueSeverity.LOW -> "🟢"
                    IssueSeverity.INFO -> "ℹ️"
                }
                appendLine("$icon ${severity.name}: ${issues.size} issues")
            }
            appendLine()
            
            appendLine("🔍 ISSUES BY TYPE")
            appendLine("-".repeat(80))
            issuesByType.forEach { (type, issues) ->
                appendLine("• ${type.name}: ${issues.size} issues")
            }
            appendLine()
            
            appendLine("📁 DETAILED FILE ANALYSIS")
            appendLine("-".repeat(80))
            
            results.sortedByDescending { it.issues.size }.take(10).forEach { result ->
                appendLine()
                appendLine("File: ${result.filePath.substringAfterLast("/")}")
                appendLine("  Issues: ${result.issues.size}")
                appendLine("  LOC: ${result.metrics.linesOfCode}")
                appendLine("  Complexity: ${result.metrics.cyclomaticComplexity}")
                appendLine("  Maintainability: ${result.metrics.maintainabilityIndex.format(2)}")
                
                if (result.issues.isNotEmpty()) {
                    appendLine("  Top Issues:")
                    result.issues.take(3).forEach { issue ->
                        val icon = when (issue.severity) {
                            IssueSeverity.CRITICAL -> "🔴"
                            IssueSeverity.HIGH -> "🟠"
                            IssueSeverity.MEDIUM -> "🟡"
                            IssueSeverity.LOW -> "🟢"
                            IssueSeverity.INFO -> "ℹ️"
                        }
                        appendLine("    $icon Line ${issue.line}: ${issue.message}")
                    }
                }
            }
            
            appendLine()
            appendLine("💡 REFACTORING RECOMMENDATIONS")
            appendLine("-".repeat(80))
            
            val allSuggestions = results.flatMap { it.suggestions }
                .distinctBy { it.title }
                .sortedBy { 
                    when (it.priority) {
                        RefactoringPriority.CRITICAL -> 0
                        RefactoringPriority.HIGH -> 1
                        RefactoringPriority.MEDIUM -> 2
                        RefactoringPriority.LOW -> 3
                    }
                }
            
            allSuggestions.forEach { suggestion ->
                val icon = when (suggestion.priority) {
                    RefactoringPriority.CRITICAL -> "🔴"
                    RefactoringPriority.HIGH -> "🟠"
                    RefactoringPriority.MEDIUM -> "🟡"
                    RefactoringPriority.LOW -> "🟢"
                }
                appendLine()
                appendLine("$icon ${suggestion.title}")
                appendLine("   ${suggestion.description}")
                appendLine("   Impact: ${suggestion.estimatedImpact}")
                appendLine("   Effort: ${suggestion.effort}")
            }
            
            appendLine()
            appendLine("=" .repeat(80))
            appendLine("END OF REPORT")
            appendLine("=" .repeat(80))
        }
    }

    private fun Double.format(decimals: Int) = "%.${decimals}f".format(this)
}

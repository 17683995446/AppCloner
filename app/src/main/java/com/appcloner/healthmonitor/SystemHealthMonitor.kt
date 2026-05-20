package com.appcloner.healthmonitor

import android.app.Application
import android.content.Context
import android.os.Debug
import android.os.SystemClock
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.lang.ref.WeakReference
import java.text.DecimalFormat
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class SystemHealthMonitor private constructor() {

    private val monitorScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val healthState = MutableStateFlow(HealthState())
    val healthStateFlow: StateFlow<HealthState> = healthState.asStateFlow()
    
    private val measurementHistory = ConcurrentLinkedQueue<HealthMeasurement>()
    private val maxHistorySize = 1000
    
    private val startTime = System.currentTimeMillis()
    private val isMonitoring = AtomicInteger(0)
    
    // Monitoring config
    private var monitoringInterval = 1000L // 1 second
    
    data class HealthState(
        val timestamp: Long = System.currentTimeMillis(),
        val memoryUsage: MemoryMetrics = MemoryMetrics(),
        val cpuMetrics: CpuMetrics = CpuMetrics(),
        val crashMetrics: CrashMetrics = CrashMetrics(),
        val networkMetrics: NetworkMetrics = NetworkMetrics(),
        val batteryMetrics: BatteryMetrics = BatteryMetrics(),
        val appHealthScore: Int = 100, // 0-100
        val warnings: List<String> = emptyList(),
        val suggestions: List<String> = emptyList()
    )

    data class HealthMeasurement(
        val timestamp: Long,
        val memoryUsed: Long,
        val memoryPercent: Double,
        val cpuUsage: Double
    )

    data class MemoryMetrics(
        val maxMemory: Long = 0L,
        val totalMemory: Long = 0L,
        val freeMemory: Long = 0L,
        val usedMemory: Long = 0L,
        val usedMemoryPercent: Double = 0.0,
        val nativeHeapAllocated: Long = 0L,
        val nativeHeapSize: Long = 0L,
        val nativeHeapFree: Long = 0L,
        val warnings: List<String> = emptyList()
    )

    data class CpuMetrics(
        val uptimeMillis: Long = 0L,
        val coreCount: Int = 0,
        val usagePercent: Double = 0.0,
        val warnings: List<String> = emptyList()
    )

    data class CrashMetrics(
        val totalCrashes: Int = 0,
        val recentCrashes: Int = 0,
        val recoverySuccessRate: Double = 0.0,
        val warnings: List<String> = emptyList()
    )

    data class NetworkMetrics(
        val requestSuccessRate: Double = 0.0,
        val totalRequests: Int = 0,
        val averageResponseTimeMs: Double = 0.0,
        val warnings: List<String> = emptyList()
    )

    data class BatteryMetrics(
        val levelPercent: Int = 100,
        val isCharging: Boolean = false,
        val temperature: Double = 0.0,
        val voltage: Double = 0.0,
        val warnings: List<String> = emptyList()
    )

    companion object {
        @Volatile
        private var instance: SystemHealthMonitor? = null

        fun getInstance(): SystemHealthMonitor {
            return instance ?: synchronized(this) {
                instance ?: SystemHealthMonitor().also { instance = it }
            }
        }
    }

    fun startMonitoring(
        application: Application,
        intervalMs: Long = 1000L
    ) {
        if (isMonitoring.compareAndSet(0, 1)) {
            Timber.d("🚀 Starting system health monitoring (interval: ${intervalMs}ms)")
            this.monitoringInterval = intervalMs
            
            monitorScope.launch {
                while (isMonitoring.get() == 1) {
                    try {
                        updateHealthState(application)
                        delay(monitoringInterval)
                    } catch (e: Exception) {
                        Timber.e(e, "Error in health monitoring loop")
                        delay(1000)
                    }
                }
            }
        }
    }

    fun stopMonitoring() {
        if (isMonitoring.compareAndSet(1, 0)) {
            Timber.d("🛑 Stopping system health monitoring")
        }
    }

    fun forceMeasurement(context: Context): HealthState {
        updateHealthState(context.applicationContext as Application)
        return healthState.value
    }

    private suspend fun updateHealthState(application: Application) = withContext(Dispatchers.Default) {
        val currentTime = System.currentTimeMillis()
        
        // Collect all metrics
        val memoryMetrics = measureMemory()
        val cpuMetrics = measureCpu()
        val crashMetrics = measureCrashState()
        val networkMetrics = measureNetworkHealth()
        val batteryMetrics = measureBattery(application)
        
        // Calculate overall health score
        val (healthScore, warnings, suggestions) = calculateOverallHealth(
            memoryMetrics,
            cpuMetrics,
            crashMetrics,
            networkMetrics,
            batteryMetrics
        )
        
        val newState = HealthState(
            timestamp = currentTime,
            memoryUsage = memoryMetrics,
            cpuMetrics = cpuMetrics,
            crashMetrics = crashMetrics,
            networkMetrics = networkMetrics,
            batteryMetrics = batteryMetrics,
            appHealthScore = healthScore,
            warnings = warnings,
            suggestions = suggestions
        )
        
        // Update state
        healthState.value = newState
        
        // Save to history
        addMeasurementToHistory(currentTime, memoryMetrics, cpuMetrics)
    }

    private fun measureMemory(): MemoryMetrics {
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory
        
        val warnings = mutableListOf<String>()
        val memoryUsagePercent = (usedMemory.toDouble() / maxMemory.toDouble()) * 100.0
        
        if (memoryUsagePercent > 80) {
            warnings.add("高内存使用：${String.format("%.1f", memoryUsagePercent)}%")
        }
        if (memoryUsagePercent > 90) {
            warnings.add("危险：内存即将耗尽")
        }
        
        return MemoryMetrics(
            maxMemory = maxMemory,
            totalMemory = totalMemory,
            freeMemory = freeMemory,
            usedMemory = usedMemory,
            usedMemoryPercent = memoryUsagePercent,
            nativeHeapAllocated = Debug.getNativeHeapAllocatedSize(),
            nativeHeapSize = Debug.getNativeHeapSize(),
            nativeHeapFree = Debug.getNativeHeapFreeSize(),
            warnings = warnings
        )
    }

    private fun measureCpu(): CpuMetrics {
        val uptimeMillis = SystemClock.elapsedRealtime()
        val coreCount = Runtime.getRuntime().availableProcessors()
        
        val warnings = mutableListOf<String>()
        
        return CpuMetrics(
            uptimeMillis = uptimeMillis,
            coreCount = coreCount,
            warnings = warnings
        )
    }

    private fun measureCrashState(): CrashMetrics {
        // Can integrate with CrashRecoveryManager
        val warnings = mutableListOf<String>()
        
        return CrashMetrics(
            totalCrashes = 0,
            recentCrashes = 0,
            recoverySuccessRate = 0.85,
            warnings = warnings
        )
    }

    private fun measureNetworkHealth(): NetworkMetrics {
        // Can integrate with ReliableNetworkClient
        return NetworkMetrics(
            requestSuccessRate = 0.95,
            totalRequests = 0,
            averageResponseTimeMs = 150.0
        )
    }

    private fun measureBattery(context: Context): BatteryMetrics {
        val warnings = mutableListOf<String>()
        
        // In real app, use BatteryManager
        return BatteryMetrics(
            levelPercent = 100,
            isCharging = true,
            warnings = warnings
        )
    }

    private fun calculateOverallHealth(
        memory: MemoryMetrics,
        cpu: CpuMetrics,
        crashes: CrashMetrics,
        network: NetworkMetrics,
        battery: BatteryMetrics
    ): Triple<Int, List<String>, List<String>> {
        
        val warnings = mutableListOf<String>()
        val suggestions = mutableListOf<String>()
        
        var score = 100
        
        // Memory impact (30%)
        val memoryScore = when {
            memory.usedMemoryPercent > 90 -> 0
            memory.usedMemoryPercent > 80 -> 30
            memory.usedMemoryPercent > 70 -> 60
            memory.usedMemoryPercent > 50 -> 80
            else -> 100
        }
        score += (memoryScore - 100) * 0.3
        warnings.addAll(memory.warnings)
        
        // CPU impact (20%)
        val cpuScore = 100
        score += (cpuScore - 100) * 0.2
        warnings.addAll(cpu.warnings)
        
        // Crash impact (25%)
        val crashScore = if (crashes.totalCrashes == 0) 100 else 70
        score += (crashScore - 100) * 0.25
        warnings.addAll(crashes.warnings)
        
        // Network impact (15%)
        val networkScore = (network.requestSuccessRate * 100).toInt()
        score += (networkScore - 100) * 0.15
        warnings.addAll(network.warnings)
        
        // Battery impact (10%)
        val batteryScore = if (battery.levelPercent > 20) 100 else 60
        score += (batteryScore - 100) * 0.1
        warnings.addAll(battery.warnings)
        
        // Generate suggestions
        if (score < 80) {
            suggestions.add("建议清理缓存以释放内存")
        }
        if (score < 70) {
            suggestions.add("考虑关闭不必要的后台应用")
        }
        if (score < 60) {
            suggestions.add("建议重启应用以恢复性能")
        }
        
        // Ensure score stays within bounds
        val finalScore = score.toInt().coerceIn(0, 100)
        
        return Triple(finalScore, warnings, suggestions)
    }

    private fun addMeasurementToHistory(
        timestamp: Long,
        memoryMetrics: MemoryMetrics,
        cpuMetrics: CpuMetrics
    ) {
        measurementHistory.add(
            HealthMeasurement(
                timestamp = timestamp,
                memoryUsed = memoryMetrics.usedMemory,
                memoryPercent = memoryMetrics.usedMemoryPercent,
                cpuUsage = cpuMetrics.usagePercent
            )
        )
        
        // Trim history if too large
        while (measurementHistory.size > maxHistorySize) {
            measurementHistory.poll()
        }
    }

    fun getHistory(): List<HealthMeasurement> {
        return measurementHistory.toList()
    }

    fun getFormattedHealthReport(): String {
        val state = healthState.value
        val decimalFormat = DecimalFormat("#.##")
        
        return buildString {
            appendLine("=".repeat(60))
            appendLine("📊 SYSTEM HEALTH REPORT")
            appendLine("=".repeat(60))
            appendLine()
            appendLine("⏱️  Timestamp: ${formatTimestamp(state.timestamp)}")
            appendLine("🏆 Health Score: ${state.appHealthScore}/100")
            appendLine()
            
            appendLine("💾 Memory:")
            appendLine("  Used: ${formatBytes(state.memoryUsage.usedMemory)}")
            appendLine("  Free: ${formatBytes(state.memoryUsage.freeMemory)}")
            appendLine("  Usage: ${decimalFormat.format(state.memoryUsage.usedMemoryPercent)}%")
            appendLine()
            
            appendLine("⚡ CPU:")
            appendLine("  Cores: ${state.cpuMetrics.coreCount}")
            appendLine("  Uptime: ${formatDuration(state.cpuMetrics.uptimeMillis)}")
            appendLine()
            
            if (state.warnings.isNotEmpty()) {
                appendLine("⚠️  Warnings:")
                state.warnings.forEach { warning ->
                    appendLine("  - $warning")
                }
                appendLine()
            }
            
            if (state.suggestions.isNotEmpty()) {
                appendLine("💡 Suggestions:")
                state.suggestions.forEach { suggestion ->
                    appendLine("  - $suggestion")
                }
            }
            
            appendLine()
            appendLine("=".repeat(60))
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US)
        return sdf.format(java.util.Date(timestamp))
    }

    private fun formatDuration(ms: Long): String {
        val seconds = ms / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        
        return when {
            days > 0 -> "${days}d ${hours % 24}h ${minutes % 60}m"
            hours > 0 -> "${hours}h ${minutes % 60}m ${seconds % 60}s"
            minutes > 0 -> "${minutes}m ${seconds % 60}s"
            else -> "${seconds}s"
        }
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> String.format("%.2f MB", bytes.toDouble() / (1024 * 1024))
            else -> String.format("%.2f GB", bytes.toDouble() / (1024 * 1024 * 1024))
        }
    }
}

// Extension functions for easy access
fun Application.startHealthMonitoring(intervalMs: Long = 1000) {
    SystemHealthMonitor.getInstance().startMonitoring(this, intervalMs)
}

fun Application.stopHealthMonitoring() {
    SystemHealthMonitor.getInstance().stopMonitoring()
}

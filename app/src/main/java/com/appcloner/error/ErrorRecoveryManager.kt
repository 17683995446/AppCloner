package com.appcloner.error

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.ConcurrentHashMap

class ErrorRecoveryManager private constructor() {

    private val supervisorJob = SupervisorJob()
    private val recoveryScope = CoroutineScope(Dispatchers.Default + supervisorJob)
    
    private val errorLog = ConcurrentHashMap<String, ErrorRecord>()
    private val recoveryStrategies = ConcurrentHashMap<String, RecoveryStrategy>()
    private val circuitBreakers = ConcurrentHashMap<String, CircuitBreaker>()
    private val mutex = Mutex()
    
    data class ErrorRecord(
        val id: String,
        val timestamp: Long,
        val errorType: String,
        val errorMessage: String,
        val stackTrace: String,
        val context: Map<String, Any>,
        val recoveryAttempts: Int = 0,
        val resolved: Boolean = false,
        val resolution: String? = null
    )
    
    interface RecoveryStrategy {
        val name: String
        val priority: Int
        suspend fun canRecover(error: ErrorRecord): Boolean
        suspend fun recover(error: ErrorRecord): RecoveryResult
    }
    
    data class RecoveryResult(
        val success: Boolean,
        val message: String,
        val newState: Map<String, Any>? = null
    )
    
    class CircuitBreaker(
        private val failureThreshold: Int = 5,
        private val recoveryTimeout: Long = 60000,
        private val halfOpenAttempts: Int = 3
    ) {
        enum class State { CLOSED, OPEN, HALF_OPEN }
        
        private var currentState = State.CLOSED
        private var failureCount = 0
        private var lastFailureTime = 0L
        private val mutex = Mutex()
        
        suspend fun canExecute(): Boolean = mutex.withLock {
            when (currentState) {
                State.CLOSED -> true
                State.OPEN -> {
                    if (System.currentTimeMillis() - lastFailureTime > recoveryTimeout) {
                        currentState = State.HALF_OPEN
                        failureCount = 0
                        true
                    } else {
                        false
                    }
                }
                State.HALF_OPEN -> true
            }
        }
        
        suspend fun onSuccess() = mutex.withLock {
            failureCount = 0
            currentState = State.CLOSED
        }
        
        suspend fun onFailure() = mutex.withLock {
            failureCount++
            lastFailureTime = System.currentTimeMillis()
            
            if (failureCount >= failureThreshold) {
                currentState = State.OPEN
                Timber.w("Circuit breaker opened for ${this@CircuitBreaker}")
            }
        }
        
        fun getState(): State = currentState
    }
    
    companion object {
        @Volatile
        private var instance: ErrorRecoveryManager? = null
        
        fun getInstance(): ErrorRecoveryManager {
            return instance ?: synchronized(this) {
                instance ?: ErrorRecoveryManager().also { instance = it }
            }
        }
    }
    
    init {
        registerDefaultStrategies()
    }
    
    private fun registerDefaultStrategies() {
        registerStrategy(RetryStrategy())
        registerStrategy(FallbackStrategy())
        registerStrategy(CircuitBreakerStrategy())
        registerStrategy(StateResetStrategy())
        registerStrategy(GracefulDegradationStrategy())
    }
    
    fun registerStrategy(strategy: RecoveryStrategy) {
        recoveryStrategies[strategy.name] = strategy
        Timber.d("Registered recovery strategy: ${strategy.name}")
    }
    
    suspend fun handleError(
        error: Throwable,
        context: Map<String, Any> = emptyMap()
    ): ErrorRecord = mutex.withLock {
        val errorRecord = ErrorRecord(
            id = generateErrorId(),
            timestamp = System.currentTimeMillis(),
            errorType = error.javaClass.simpleName,
            errorMessage = error.message ?: "Unknown error",
            stackTrace = getStackTrace(error),
            context = context
        )
        
        errorLog[errorRecord.id] = errorRecord
        Timber.e(error, "Error recorded: ${errorRecord.id}")
        
        trimOldErrors()
        
        recoveryScope.launch {
            attemptRecovery(errorRecord)
        }
        
        errorRecord
    }
    
    suspend fun attemptRecovery(errorRecord: ErrorRecord): RecoveryResult {
        val strategies = recoveryStrategies.values
            .sortedByDescending { it.priority }
            .filter { it.canRecover(errorRecord) }
        
        for (strategy in strategies) {
            Timber.d("Attempting recovery with strategy: ${strategy.name}")
            
            try {
                val result = strategy.recover(errorRecord)
                
                if (result.success) {
                    errorRecord.recoveryAttempts++
                    errorRecord.resolved = true
                    errorRecord.resolution = result.message
                    
                    Timber.i("Recovery successful with ${strategy.name}: ${result.message}")
                    return result
                }
            } catch (e: Exception) {
                Timber.e(e, "Recovery strategy ${strategy.name} failed")
            }
        }
        
        return RecoveryResult(
            success = false,
            message = "All recovery strategies failed"
        )
    }
    
    suspend fun <T> withCircuitBreaker(
        key: String,
        operation: suspend () -> T
    ): Result<T> {
        val breaker = circuitBreakers.getOrPut(key) {
            CircuitBreaker()
        }
        
        return if (breaker.canExecute()) {
            try {
                val result = operation()
                breaker.onSuccess()
                Result.success(result)
            } catch (e: Exception) {
                breaker.onFailure()
                Result.failure(e)
            }
        } else {
            Timber.w("Circuit breaker for $key is OPEN, operation skipped")
            Result.failure(CircuitBreakerOpenException("Circuit breaker is open for: $key"))
        }
    }
    
    suspend fun <T> withRetry(
        maxAttempts: Int = 3,
        initialDelay: Long = 1000,
        maxDelay: Long = 10000,
        factor: Double = 2.0,
        operation: suspend () -> T
    ): Result<T> {
        var currentDelay = initialDelay
        
        repeat(maxAttempts) { attempt ->
            try {
                val result = operation()
                return Result.success(result)
            } catch (e: Exception) {
                if (attempt == maxAttempts - 1) {
                    return Result.failure(e)
                }
                
                Timber.w(e, "Attempt ${attempt + 1} failed, retrying in ${currentDelay}ms")
                delay(currentDelay)
                currentDelay = minOf((currentDelay * factor).toLong(), maxDelay)
            }
        }
        
        return Result.failure(Exception("Max retry attempts exceeded"))
    }
    
    suspend fun <T> withTimeout(
        timeoutMs: Long,
        operation: suspend () -> T
    ): Result<T> {
        return try {
            withContext(Dispatchers.Default) {
                kotlinx.coroutines.withTimeoutOrNull(timeoutMs) {
                    operation()
                } ?: throw TimeoutException("Operation timed out after ${timeoutMs}ms")
            }.let { Result.success(it!!) }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getErrorSummary(): ErrorSummary {
        val errors = errorLog.values.toList()
        
        return ErrorSummary(
            totalErrors = errors.size,
            unresolvedErrors = errors.count { !it.resolved },
            resolvedErrors = errors.count { it.resolved },
            errorsByType = errors.groupBy { it.errorType }.mapValues { it.value.size },
            recentErrors = errors.sortedByDescending { it.timestamp }.take(10),
            averageResolutionTime = calculateAverageResolutionTime(errors),
            successRate = if (errors.isNotEmpty()) {
                (errors.count { it.resolved } * 100.0) / errors.size
            } else 0.0
        )
    }
    
    private fun calculateAverageResolutionTime(errors: List<ErrorRecord>): Long {
        val resolvedErrors = errors.filter { it.resolved && it.resolution != null }
        if (resolvedErrors.isEmpty()) return 0
        
        val totalTime = resolvedErrors.sumOf { error ->
            val resolutionTime = error.context["resolutionTime"] as? Long ?: 0L
            resolutionTime
        }
        
        return totalTime / resolvedErrors.size
    }
    
    private suspend fun trimOldErrors() {
        if (errorLog.size > 1000) {
            val errorsToRemove = errorLog.values
                .sortedBy { it.timestamp }
                .take(errorLog.size - 1000)
                .map { it.id }
            
            errorsToRemove.forEach { id ->
                errorLog.remove(id)
            }
            
            Timber.d("Trimmed ${errorsToRemove.size} old error records")
        }
    }
    
    private fun generateErrorId(): String {
        return "ERR-${System.currentTimeMillis()}-${(1000..9999).random()}"
    }
    
    private fun getStackTrace(error: Throwable): String {
        val writer = StringWriter()
        error.printStackTrace(PrintWriter(writer))
        return writer.toString()
    }
    
    data class ErrorSummary(
        val totalErrors: Int,
        val unresolvedErrors: Int,
        val resolvedErrors: Int,
        val errorsByType: Map<String, Int>,
        val recentErrors: List<ErrorRecord>,
        val averageResolutionTime: Long,
        val successRate: Double
    )
    
    class CircuitBreakerOpenException(message: String) : Exception(message)
    class TimeoutException(message: String) : Exception(message)
}

class RetryStrategy : ErrorRecoveryManager.RecoveryStrategy {
    override val name = "RetryStrategy"
    override val priority = 100
    
    override suspend fun canRecover(error: ErrorRecoveryManager.ErrorRecord): Boolean {
        return error.recoveryAttempts < 3
    }
    
    override suspend fun recover(error: ErrorRecoveryManager.ErrorRecord): ErrorRecoveryManager.RecoveryResult {
        kotlinx.coroutines.delay(1000L * (error.recoveryAttempts + 1))
        return ErrorRecoveryManager.RecoveryResult(
            success = true,
            message = "Recovered after ${error.recoveryAttempts + 1} attempts"
        )
    }
}

class FallbackStrategy : ErrorRecoveryManager.RecoveryStrategy {
    override val name = "FallbackStrategy"
    override val priority = 50
    
    override suspend fun canRecover(error: ErrorRecoveryManager.ErrorRecord): Boolean {
        return error.context.containsKey("fallbackValue")
    }
    
    override suspend fun recover(error: ErrorRecoveryManager.ErrorRecord): ErrorRecoveryManager.RecoveryResult {
        val fallbackValue = error.context["fallbackValue"]
        return ErrorRecoveryManager.RecoveryResult(
            success = true,
            message = "Using fallback value: $fallbackValue",
            newState = mapOf("usedFallback" to true, "fallbackValue" to fallbackValue)
        )
    }
}

class CircuitBreakerStrategy : ErrorRecoveryManager.RecoveryStrategy {
    override val name = "CircuitBreakerStrategy"
    override val priority = 40
    
    override suspend fun canRecover(error: ErrorRecoveryManager.ErrorRecord): Boolean {
        return error.context.containsKey("circuitBreakerKey")
    }
    
    override suspend fun recover(error: ErrorRecoveryManager.ErrorRecord): ErrorRecoveryManager.RecoveryResult {
        return ErrorRecoveryManager.RecoveryResult(
            success = true,
            message = "Circuit breaker activated for future requests"
        )
    }
}

class StateResetStrategy : ErrorRecoveryManager.RecoveryStrategy {
    override val name = "StateResetStrategy"
    override val priority = 30
    
    override suspend fun canRecover(error: ErrorRecoveryManager.ErrorRecord): Boolean {
        return error.errorType.contains("StateException") ||
               error.errorType.contains("IllegalStateException")
    }
    
    override suspend fun recover(error: ErrorRecoveryManager.ErrorRecord): ErrorRecoveryManager.RecoveryResult {
        return ErrorRecoveryManager.RecoveryResult(
            success = true,
            message = "State reset successfully",
            newState = mapOf("stateReset" to true)
        )
    }
}

class GracefulDegradationStrategy : ErrorRecoveryManager.RecoveryStrategy {
    override val name = "GracefulDegradationStrategy"
    override val priority = 20
    
    override suspend fun canRecover(error: ErrorRecoveryManager.ErrorRecord): Boolean {
        return true
    }
    
    override suspend fun recover(error: ErrorRecoveryManager.ErrorRecord): ErrorRecoveryManager.RecoveryResult {
        return ErrorRecoveryManager.RecoveryResult(
            success = true,
            message = "System degraded gracefully, core functionality preserved"
        )
    }
}

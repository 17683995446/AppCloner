package com.appcloner.network

import kotlinx.coroutines.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

class ReliableNetworkClient {

    data class RequestConfig(
        val maxRetries: Int = 3,
        val initialDelayMs: Long = 1000,
        val maxDelayMs: Long = 30_000,
        val timeoutMs: Long = 30_000,
        val backoffMultiplier: Double = 2.0,
        val jitterFactor: Double = 0.1,
        val enableCircuitBreaker: Boolean = true
    )

    data class RequestResult<T>(
        val success: Boolean,
        val data: T? = null,
        val error: Throwable? = null,
        val retryCount: Int = 0,
        val totalTimeMs: Long = 0,
        val status: RequestStatus = RequestStatus.SUCCESS
    )

    enum class RequestStatus {
        SUCCESS,
        FAILED,
        TIMEOUT,
        CIRCUIT_OPEN
    }

    private val requestStats = ConcurrentHashMap<String, RequestStats>()
    private val circuitBreakers = ConcurrentHashMap<String, CircuitBreaker>()
    private val mutex = Mutex()

    companion object {
        @Volatile
        private var instance: ReliableNetworkClient? = null

        fun getInstance(): ReliableNetworkClient {
            return instance ?: synchronized(this) {
                instance ?: ReliableNetworkClient().also { instance = it }
            }
        }
    }

    suspend fun <T> executeRequest(
        requestId: String,
        config: RequestConfig = RequestConfig(),
        request: suspend () -> T
    ): RequestResult<T> = coroutineScope {
        val startTime = System.currentTimeMillis()
        var retryCount = 0
        var lastError: Throwable? = null

        // Check circuit breaker
        if (config.enableCircuitBreaker) {
            val circuitBreaker = getCircuitBreaker(requestId)
            if (!circuitBreaker.canExecute()) {
                return@coroutineScope RequestResult(
                    success = false,
                    error = Exception("Circuit breaker is open"),
                    status = RequestStatus.CIRCUIT_OPEN
                )
            }
        }

        while (retryCount <= config.maxRetries) {
            try {
                val result = withTimeout(config.timeoutMs) {
                    request()
                }

                // Success!
                recordSuccess(requestId)
                if (config.enableCircuitBreaker) {
                    getCircuitBreaker(requestId).onSuccess()
                }

                return@coroutineScope RequestResult(
                    success = true,
                    data = result,
                    retryCount = retryCount,
                    totalTimeMs = System.currentTimeMillis() - startTime,
                    status = RequestStatus.SUCCESS
                )

            } catch (e: TimeoutCancellationException) {
                Timber.w("Request $requestId timed out (attempt ${retryCount + 1}/${config.maxRetries + 1})")
                lastError = e

            } catch (e: Exception) {
                Timber.e(e, "Request $requestId failed (attempt ${retryCount + 1}/${config.maxRetries + 1})")
                lastError = e
                
                if (!isRetryable(e)) {
                    break
                }
            }

            // Update circuit breaker
            if (config.enableCircuitBreaker) {
                getCircuitBreaker(requestId).onFailure()
            }

            retryCount++
            recordFailure(requestId)

            if (retryCount <= config.maxRetries) {
                val delayMs = calculateBackoffDelay(retryCount, config)
                Timber.d("Retrying $requestId in ${delayMs}ms...")
                delay(delayMs)
            }
        }

        // All retries exhausted
        return@coroutineScope RequestResult(
            success = false,
            error = lastError ?: Exception("Unknown error"),
            retryCount = retryCount,
            totalTimeMs = System.currentTimeMillis() - startTime,
            status = if (lastError is TimeoutCancellationException) {
                RequestStatus.TIMEOUT
            } else {
                RequestStatus.FAILED
            }
        )
    }

    private fun calculateBackoffDelay(attempt: Int, config: RequestConfig): Long {
        val exponentialDelay = config.initialDelayMs * Math.pow(config.backoffMultiplier, (attempt - 1).toDouble())
        val jitterRange = exponentialDelay * config.jitterFactor
        val jitter = Random.nextDouble(-jitterRange, jitterRange)
        val totalDelay = (exponentialDelay + jitter).toLong()
        
        return totalDelay.coerceAtMost(config.maxDelayMs).coerceAtLeast(config.initialDelayMs)
    }

    private fun isRetryable(exception: Throwable): Boolean {
        // Determines if an error is retryable
        return exception !is IllegalArgumentException &&
               exception !is UnsupportedOperationException &&
               exception !is NullPointerException
    }

    private fun getCircuitBreaker(requestId: String): CircuitBreaker {
        return circuitBreakers.getOrPut(requestId) {
            CircuitBreaker(
                failureThreshold = 5,
                resetTimeoutMs = 30_000,
                halfOpenMaxRequests = 3
            )
        }
    }

    private fun recordSuccess(requestId: String) {
        val stats = getOrCreateStats(requestId)
        stats.successCount.incrementAndGet()
        stats.lastSuccessTime.set(System.currentTimeMillis())
    }

    private fun recordFailure(requestId: String) {
        val stats = getOrCreateStats(requestId)
        stats.failureCount.incrementAndGet()
        stats.lastFailureTime.set(System.currentTimeMillis())
    }

    private fun getOrCreateStats(requestId: String): RequestStats {
        return requestStats.getOrPut(requestId) {
            RequestStats()
        }
    }

    fun getRequestStats(requestId: String): RequestStats? {
        return requestStats[requestId]
    }

    fun getAllStats(): Map<String, RequestStats> {
        return requestStats.toMap()
    }

    fun resetStats() {
        requestStats.clear()
        circuitBreakers.clear()
    }

    data class RequestStats(
        val successCount: AtomicInteger = AtomicInteger(0),
        val failureCount: AtomicInteger = AtomicInteger(0),
        val lastSuccessTime: AtomicLong = AtomicLong(0),
        val lastFailureTime: AtomicLong = AtomicLong(0)
    ) {
        val totalRequests: Int
            get() = successCount.get() + failureCount.get()
        
        val successRate: Double
            get() {
                val total = totalRequests
                if (total == 0) return 1.0
                return successCount.get().toDouble() / total
            }
    }

    class CircuitBreaker(
        private val failureThreshold: Int,
        private val resetTimeoutMs: Long,
        private val halfOpenMaxRequests: Int
    ) {
        enum class State {
            CLOSED,
            OPEN,
            HALF_OPEN
        }

        @Volatile
        private var state = State.CLOSED
        private val failureCount = AtomicInteger(0)
        private val halfOpenRequestCount = AtomicInteger(0)
        private val lastFailureTime = AtomicLong(0)

        suspend fun canExecute(): Boolean {
            val currentState = state
            val now = System.currentTimeMillis()

            return when (currentState) {
                State.CLOSED -> {
                    true
                }
                State.OPEN -> {
                    if (now - lastFailureTime.get() > resetTimeoutMs) {
                        state = State.HALF_OPEN
                        halfOpenRequestCount.set(0)
                        true
                    } else {
                        false
                    }
                }
                State.HALF_OPEN -> {
                    halfOpenRequestCount.get() < halfOpenMaxRequests
                }
            }
        }

        fun onSuccess() {
            when (state) {
                State.HALF_OPEN -> {
                    failureCount.set(0)
                    state = State.CLOSED
                    Timber.d("Circuit breaker closed after successful request")
                }
                State.CLOSED -> {
                    failureCount.set(0)
                }
                else -> Unit
            }
        }

        fun onFailure() {
            when (state) {
                State.CLOSED -> {
                    val count = failureCount.incrementAndGet()
                    if (count >= failureThreshold) {
                        state = State.OPEN
                        lastFailureTime.set(System.currentTimeMillis())
                        Timber.w("Circuit breaker opened after $count failures")
                    }
                }
                State.HALF_OPEN -> {
                    state = State.OPEN
                    lastFailureTime.set(System.currentTimeMillis())
                    Timber.w("Circuit breaker re-opened after failure in half-open state")
                }
                else -> Unit
            }
        }

        fun getState(): State = state
    }
}

// Extension functions for easier use
suspend fun <T> withReliableNetwork(
    requestId: String = "default",
    maxRetries: Int = 3,
    request: suspend () -> T
): Result<T> {
    val client = ReliableNetworkClient.getInstance()
    val config = ReliableNetworkClient.RequestConfig(maxRetries = maxRetries)
    
    val result = client.executeRequest(requestId, config, request)
    
    return if (result.success && result.data != null) {
        Result.success(result.data)
    } else {
        Result.failure(result.error ?: Exception("Request failed"))
    }
}

// Flow wrapper with reliability
fun <T> flowWithRetry(
    requestId: String,
    maxRetries: Int = 3,
    request: suspend () -> T
): Flow<T> = flow {
    val client = ReliableNetworkClient.getInstance()
    val config = ReliableNetworkClient.RequestConfig(maxRetries = maxRetries)
    
    val result = client.executeRequest(requestId, config, request)
    
    if (result.success && result.data != null) {
        emit(result.data)
    } else {
        throw result.error ?: Exception("Flow request failed")
    }
}.retryWhen { cause, attempt ->
    attempt < maxRetries
}

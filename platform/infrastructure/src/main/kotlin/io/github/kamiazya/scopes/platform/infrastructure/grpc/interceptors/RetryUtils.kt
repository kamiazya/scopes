package io.github.kamiazya.scopes.platform.infrastructure.grpc.interceptors

import io.grpc.Status
import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.math.pow
import kotlin.time.Duration

/**
 * Utility for implementing retry logic with exponential backoff.
 * Provides helper functions for determining retry conditions and calculating delays.
 */
object RetryUtils {

    /**
     * Determines if a gRPC status should be retried.
     * Only transient errors are considered retryable.
     */
    fun shouldRetry(status: Status): Boolean = when (status.code) {
        Status.Code.UNAVAILABLE,
        Status.Code.DEADLINE_EXCEEDED,
        Status.Code.RESOURCE_EXHAUSTED,
        Status.Code.ABORTED,
        -> true
        else -> false
    }

    /**
     * Calculates exponential backoff delay for the given attempt number.
     *
     * @param attempt The attempt number (1-based)
     * @param baseDelay The base delay for the first retry
     * @param maxDelay The maximum delay to cap the exponential growth
     * @return The calculated delay duration
     */
    fun calculateDelay(attempt: Int, baseDelay: Duration = Duration.parse("1s"), maxDelay: Duration = Duration.parse("30s")): Duration {
        val exponentialDelay = baseDelay.inWholeMilliseconds * (2.0.pow(attempt - 1)).toLong()
        val cappedDelay = min(exponentialDelay, maxDelay.inWholeMilliseconds)
        return Duration.parse("${cappedDelay}ms")
    }

    /**
     * Executes a suspend function with retry logic and exponential backoff.
     *
     * @param maxRetries Maximum number of retry attempts
     * @param baseDelay Base delay for exponential backoff
     * @param maxDelay Maximum delay cap
     * @param operation The operation to execute and potentially retry
     * @return The result of the successful operation
     * @throws Exception The final exception if all retries are exhausted
     */
    suspend fun <T> withRetry(
        maxRetries: Int = 3,
        baseDelay: Duration = Duration.parse("1s"),
        maxDelay: Duration = Duration.parse("30s"),
        operation: suspend () -> T,
    ): T {
        var lastException: Exception? = null

        repeat(maxRetries + 1) { attempt ->
            try {
                return operation()
            } catch (e: Exception) {
                lastException = e

                // Check if this is a retryable gRPC error
                val isRetryable = when (e) {
                    is io.grpc.StatusException -> shouldRetry(e.status)
                    is io.grpc.StatusRuntimeException -> shouldRetry(e.status)
                    else -> false
                }

                // If not retryable or this was the last attempt, rethrow
                if (!isRetryable || attempt >= maxRetries) {
                    throw e
                }

                // Calculate delay and wait before next attempt
                val delay = calculateDelay(attempt + 1, baseDelay, maxDelay)
                delay(delay.inWholeMilliseconds)
            }
        }

        // This should never be reached, but included for completeness
        throw lastException ?: RuntimeException("Retry logic error")
    }
}

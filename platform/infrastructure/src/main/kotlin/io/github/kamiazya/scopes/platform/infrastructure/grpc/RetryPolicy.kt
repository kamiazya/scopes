package io.github.kamiazya.scopes.platform.infrastructure.grpc

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

/**
 * Configuration for retry behavior.
 *
 * @property maxAttempts Maximum number of attempts including the initial attempt
 * @property initialDelay Initial delay before first retry
 * @property maxDelay Maximum delay between retries
 * @property backoffMultiplier Multiplier for exponential backoff
 * @property retryableCondition Function to determine if an error is retryable
 */
data class RetryConfig(
    val maxAttempts: Int = 3,
    val initialDelay: Duration = 1.seconds,
    val maxDelay: Duration = 30.seconds,
    val backoffMultiplier: Double = 2.0,
    val retryableCondition: (Throwable) -> Boolean = { true },
) {
    init {
        require(maxAttempts >= 1) { "maxAttempts must be at least 1" }
        require(backoffMultiplier >= 1.0) { "backoffMultiplier must be at least 1.0" }
    }
}

/**
 * Retry policy implementation with exponential backoff.
 */
class RetryPolicy(private val config: RetryConfig, private val logger: Logger) {
    /**
     * Executes an operation with retry logic.
     *
     * @param operationName Name of the operation for logging
     * @param operation The operation to execute
     * @return Either the last error or the successful result
     */
    suspend fun <E : Throwable, T> execute(operationName: String, operation: suspend (attemptNumber: Int) -> Either<E, T>): Either<E, T> {
        var lastError: E? = null
        var delay = config.initialDelay

        repeat(config.maxAttempts) { attemptIndex ->
            val attemptNumber = attemptIndex + 1
            logger.debug(
                "Attempting operation",
                mapOf(
                    "operation" to operationName,
                    "attempt" to attemptNumber,
                    "maxAttempts" to config.maxAttempts,
                ),
            )

            val result = operation(attemptNumber)
            result.fold(
                { error ->
                    lastError = error
                    // Check if we should retry
                    if (attemptNumber >= config.maxAttempts || !config.retryableCondition(error)) {
                        logger.warn(
                            "Operation failed, not retrying",
                            mapOf<String, Any>(
                                "operation" to operationName,
                                "attempt" to attemptNumber,
                                "error" to (error.message ?: "Unknown error"),
                                "retryable" to config.retryableCondition(error),
                            ),
                        )
                        return error.left()
                    }

                    logger.warn(
                        "Operation failed, will retry",
                        mapOf<String, Any>(
                            "operation" to operationName,
                            "attempt" to attemptNumber,
                            "error" to (error.message ?: "Unknown error"),
                            "nextDelay" to "${delay.inWholeMilliseconds}ms",
                        ),
                    )
                    // Apply exponential backoff
                    kotlinx.coroutines.delay(delay)
                    delay = (delay.toDouble(DurationUnit.MILLISECONDS) * config.backoffMultiplier)
                        .milliseconds
                        .coerceAtMost(config.maxDelay)
                },
                { success ->
                    if (attemptNumber > 1) {
                        logger.info(
                            "Operation succeeded after retry",
                            mapOf(
                                "operation" to operationName,
                                "attempt" to attemptNumber,
                            ),
                        )
                    }
                    return success.right()
                },
            )
        }

        // Should never reach here due to the return statements above
        return lastError?.left() ?: throw IllegalStateException("No attempts were made")
    }

    companion object {
        /**
         * Creates a default retry policy for gRPC operations.
         */
        fun forGrpc(logger: Logger): RetryPolicy = RetryPolicy(
            config = RetryConfig(
                maxAttempts = 3,
                initialDelay = 1.seconds,
                maxDelay = 15.seconds,
                backoffMultiplier = 2.0,
                retryableCondition = { error ->
                    when (error) {
                        is io.grpc.StatusException -> {
                            // Retry on transient errors
                            when (error.status.code) {
                                io.grpc.Status.Code.UNAVAILABLE,
                                io.grpc.Status.Code.DEADLINE_EXCEEDED,
                                io.grpc.Status.Code.ABORTED,
                                io.grpc.Status.Code.INTERNAL,
                                io.grpc.Status.Code.UNKNOWN,
                                -> true
                                else -> false
                            }
                        }
                        // Connection errors are retryable
                        is java.net.ConnectException,
                        is java.net.SocketException,
                        is java.net.SocketTimeoutException,
                        is java.io.IOException,
                        -> true
                        else -> false
                    }
                },
            ),
            logger = logger,
        )

        /**
         * Creates a retry policy from environment configuration.
         */
        fun fromEnvironment(logger: Logger): RetryPolicy {
            val maxAttempts = System.getenv("SCOPES_GRPC_RETRY_MAX_ATTEMPTS")?.toIntOrNull() ?: 3
            val initialDelayMs = System.getenv("SCOPES_GRPC_RETRY_INITIAL_DELAY_MS")?.toLongOrNull() ?: 1000
            val maxDelayMs = System.getenv("SCOPES_GRPC_RETRY_MAX_DELAY_MS")?.toLongOrNull() ?: 15000
            val backoffMultiplier = System.getenv("SCOPES_GRPC_RETRY_BACKOFF_MULTIPLIER")?.toDoubleOrNull() ?: 2.0

            return RetryPolicy(
                config = RetryConfig(
                    maxAttempts = maxAttempts,
                    initialDelay = initialDelayMs.milliseconds,
                    maxDelay = maxDelayMs.milliseconds,
                    backoffMultiplier = backoffMultiplier,
                    retryableCondition = forGrpc(logger).config.retryableCondition,
                ),
                logger = logger,
            )
        }
    }
}

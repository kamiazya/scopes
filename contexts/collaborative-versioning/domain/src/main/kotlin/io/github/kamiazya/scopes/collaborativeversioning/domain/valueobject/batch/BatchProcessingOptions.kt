package io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.batch

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Value object representing options for batch processing.
 *
 * @property chunkSize The number of items to process in each chunk
 * @property parallelism The number of parallel workers to use
 * @property maxRetries Maximum number of retry attempts for failed operations
 * @property retryDelay Delay between retry attempts
 * @property continueOnError Whether to continue processing on individual errors
 * @property timeout Optional timeout for the entire batch operation
 */
data class BatchProcessingOptions(
    val chunkSize: Int = 100,
    val parallelism: Int = 4,
    val maxRetries: Int = 3,
    val retryDelay: Duration = 1.seconds,
    val continueOnError: Boolean = true,
    val timeout: Duration? = null,
) {
    companion object {
        /**
         * Create BatchProcessingOptions with validation.
         */
        fun create(
            chunkSize: Int,
            parallelism: Int,
            maxRetries: Int = 3,
            retryDelay: Duration = 1.seconds,
            continueOnError: Boolean = true,
            timeout: Duration? = null,
        ): Either<String, BatchProcessingOptions> = when {
            chunkSize <= 0 -> "Chunk size must be positive".left()
            parallelism <= 0 -> "Parallelism must be positive".left()
            maxRetries < 0 -> "Max retries cannot be negative".left()
            else -> BatchProcessingOptions(chunkSize, parallelism, maxRetries, retryDelay, continueOnError, timeout).right()
        }
    }
}

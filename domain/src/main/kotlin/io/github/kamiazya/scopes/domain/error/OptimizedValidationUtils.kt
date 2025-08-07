package io.github.kamiazya.scopes.domain.error

import arrow.core.NonEmptyList
import kotlin.system.measureTimeMillis

/**
 * Performance optimization utilities for validation and error handling.
 *
 * Provides optimized patterns for efficient
 * error processing, memory management, and performance measurement.
 */
object OptimizedValidationUtils {

    /**
     * Creates a lazy error message that defers computation until accessed.
     * Useful for expensive error message generation that may not always be needed.
     */
    fun lazyErrorMessage(computation: () -> String): LazyErrorMessage {
        return LazyErrorMessage(computation)
    }

    /**
     * Creates a lazy error message with context support.
     */
    fun lazyErrorMessage(
        context: Map<String, Any>,
        computation: (Map<String, Any>) -> String
    ): LazyErrorMessage {
        return LazyErrorMessage { computation(context) }
    }

    /**
     * Creates an efficient error accumulator for collecting multiple errors.
     * More memory-efficient than repeated list concatenation.
     */
    fun createErrorAccumulator(): EfficientErrorAccumulator {
        return EfficientErrorAccumulator()
    }

    /**
     * Benchmarks a validation operation and returns timing information.
     */
    fun <T> benchmark(operation: () -> ValidationResult<T>): BenchmarkResult<T> {
        var result: ValidationResult<T>
        val actualTime = measureTimeMillis {
            result = operation()
        }

        return BenchmarkResult(result, actualTime)
    }

    /**
     * Compares performance of two validation operations.
     */
    fun <T1, T2> comparePerformance(
        operation1: () -> ValidationResult<T1>,
        operation2: () -> ValidationResult<T2>
    ): PerformanceComparison<T1, T2> {
        var result1: ValidationResult<T1>
        var result2: ValidationResult<T2>

        val time1 = measureTimeMillis {
            result1 = operation1()
        }

        val time2 = measureTimeMillis {
            result2 = operation2()
        }

        return PerformanceComparison(result1, time1, result2, time2)
    }

    /**
     * Efficiently validates a large collection of items with error accumulation.
     */
    fun <T, U> batchValidate(
        items: List<T>,
        validator: (T) -> ValidationResult<U>
    ): ValidationResult<List<U>> {
        val accumulator = createErrorAccumulator()
        val results = mutableListOf<U>()

        items.forEach { item ->
            when (val validation = validator(item)) {
                is ValidationResult.Success -> results.add(validation.value)
                is ValidationResult.Failure -> accumulator.addAll(validation.errors)
            }
        }

        return if (accumulator.hasErrors()) {
            accumulator.toValidationResult()
        } else {
            ValidationResult.Success(results)
        }
    }

    /**
     * Batch validation with fail-fast behavior - stops on first error.
     */
    fun <T, U> batchValidateFailFast(
        items: List<T>,
        validator: (T) -> ValidationResult<U>
    ): ValidationResult<List<U>> {
        val results = mutableListOf<U>()

        for (item in items) {
            when (val validation = validator(item)) {
                is ValidationResult.Success -> results.add(validation.value)
                is ValidationResult.Failure -> return ValidationResult.Failure(validation.errors)
            }
        }

        return ValidationResult.Success(results)
    }

    /**
     * Optimizes memory usage for large error collections.
     * This is a placeholder for future optimizations like error deduplication,
     * compression, or streaming processing.
     */
    fun <T> optimizeErrorMemory(validationResult: ValidationResult<T>): ValidationResult<T> {
        return when (validationResult) {
            is ValidationResult.Success -> validationResult
            is ValidationResult.Failure -> {
                // For now, just return as-is. Future optimizations could include:
                // - Error deduplication
                // - Error compression
                // - Lazy error message evaluation
                validationResult
            }
        }
    }

    /**
     * Estimates memory usage of a ValidationResult.
     * Provides rough approximation for performance monitoring.
     */
    fun <T> estimateMemoryUsage(validationResult: ValidationResult<T>): Long {
        return when (validationResult) {
            is ValidationResult.Success -> {
                // Rough estimate: object overhead + value size estimation
                48L + estimateValueSize(validationResult.value)
            }
            is ValidationResult.Failure -> {
                // Rough estimate: object overhead + errors collection size
                48L + (validationResult.errors.size * 64L) // Assume ~64 bytes per error
            }
        }
    }

    /**
     * Simple value size estimation for memory usage calculations.
     */
    private fun estimateValueSize(value: Any?): Long {
        return when (value) {
            null -> 0L
            is String -> 24L + (value.length * 2L) // Rough UTF-16 estimation
            is List<*> -> 24L + (value.size * 8L) // Rough list overhead
            else -> 32L // Default object size estimation
        }
    }
}

/**
 * Lazy error message wrapper that defers computation until accessed.
 */
class LazyErrorMessage(computation: () -> String) {
    val message: String by lazy { computation() }
}

/**
 * Efficient error accumulator that avoids intermediate collection creation.
 */
class EfficientErrorAccumulator {
    private val errors = mutableListOf<DomainError>()

    fun add(error: DomainError) {
        errors.add(error)
    }

    fun addAll(errorCollection: Collection<DomainError>) {
        errors.addAll(errorCollection)
    }

    fun addAll(errorList: NonEmptyList<DomainError>) {
        errors.addAll(errorList)
    }

    fun hasErrors(): Boolean = errors.isNotEmpty()

    fun <T> toValidationResult(): ValidationResult<T> {
        return if (errors.isEmpty()) {
            error("Cannot create failure ValidationResult from empty error accumulator")
        } else {
            ValidationResult.Failure(NonEmptyList(errors.first(), errors.drop(1)))
        }
    }

    fun <T> toValidationResultWithValue(value: T): ValidationResult<T> {
        return if (errors.isEmpty()) {
            ValidationResult.Success(value)
        } else {
            ValidationResult.Failure(NonEmptyList(errors.first(), errors.drop(1)))
        }
    }
}

/**
 * Results container for benchmark operations
 */
data class BenchmarkResult<T>(
    val result: ValidationResult<T>,
    val executionTimeMs: Long
)

/**
 * Results container for performance comparison
 */
data class PerformanceComparison<T1, T2>(
    val operation1Result: ValidationResult<T1>,
    val operation1TimeMs: Long,
    val operation2Result: ValidationResult<T2>,
    val operation2TimeMs: Long
) {
    val improvementPercentage: Double
        get() = if (operation1TimeMs > 0) {
            ((operation1TimeMs - operation2TimeMs).toDouble() / operation1TimeMs) * 100.0
        } else 0.0
}

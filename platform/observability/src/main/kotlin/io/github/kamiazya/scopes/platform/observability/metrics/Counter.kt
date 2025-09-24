package io.github.kamiazya.scopes.platform.observability.metrics

/**
 * Interface for counter metrics.
 * A counter is a cumulative metric that can only increase or be reset to zero.
 */
interface Counter {
    /**
     * Increment the counter by 1.
     */
    fun increment()

    /**
     * Increment the counter by the specified amount.
     */
    fun increment(amount: Double)

    /**
     * Get the current count.
     */
    fun count(): Double

    /**
     * Reset the counter to zero.
     * This should only be used for testing or administrative purposes.
     */
    fun reset()
}

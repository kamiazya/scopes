package io.github.kamiazya.scopes.platform.observability.metrics

/**
 * Registry for managing application metrics.
 * Provides a central place to create and retrieve metrics instances.
 */
interface MetricsRegistry {
    /**
     * Create or retrieve a counter metric with the given name and optional tags.
     * @param name The metric name (should be unique within the registry)
     * @param description Optional description of what the metric measures
     * @param tags Optional map of tags for the metric (e.g., "service" -> "projection")
     * @return A Counter instance
     */
    fun counter(name: String, description: String? = null, tags: Map<String, String> = emptyMap()): Counter

    /**
     * Get all registered counters for export or monitoring.
     * @return Map of metric name to counter instances
     */
    fun getAllCounters(): Map<String, Counter>

    /**
     * Export all metrics in a readable format.
     * @return String representation of all metrics with their current values
     */
    fun exportMetrics(): String
}

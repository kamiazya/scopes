package io.github.kamiazya.scopes.platform.observability.metrics

import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe in-memory implementation of MetricsRegistry.
 * Uses ConcurrentHashMap for thread-safe operations across multiple counters.
 */
class InMemoryMetricsRegistry : MetricsRegistry {

    private val counters = ConcurrentHashMap<String, InMemoryCounter>()

    override fun counter(name: String, description: String?, tags: Map<String, String>): Counter {
        // Create unique key by combining name and tags
        val key = buildCounterKey(name, tags)

        return counters.computeIfAbsent(key) {
            InMemoryCounter(name, description, tags)
        }
    }

    override fun getAllCounters(): Map<String, Counter> = counters.toMap()

    override fun exportMetrics(): String {
        if (counters.isEmpty()) {
            return "# No metrics available\n"
        }

        val builder = StringBuilder()
        builder.appendLine("# Application Metrics Export")
        builder.appendLine("# Generated at: ${kotlinx.datetime.Clock.System.now()}")
        builder.appendLine()

        // Group by metric name
        val groupedCounters = counters.values.groupBy { it.toString().substringBefore(':').substringBefore('{') }

        groupedCounters.forEach { (metricName, counters) ->
            builder.appendLine("# HELP $metricName")
            builder.appendLine("# TYPE $metricName counter")
            counters.forEach { counter ->
                builder.appendLine(counter.toString())
            }
            builder.appendLine()
        }

        return builder.toString()
    }

    /**
     * Reset all counters to zero.
     * This should only be used for testing purposes.
     */
    fun resetAll() {
        counters.values.forEach { it.reset() }
    }

    /**
     * Get the count of registered counters.
     */
    fun size(): Int = counters.size

    private fun buildCounterKey(name: String, tags: Map<String, String>): String = if (tags.isEmpty()) {
        name
    } else {
        val tagString = tags.entries
            .sortedBy { it.key } // Sort for consistent key generation
            .joinToString(",") { "${it.key}=${it.value}" }
        "$name{$tagString}"
    }
}

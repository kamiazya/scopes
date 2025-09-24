package io.github.kamiazya.scopes.platform.observability.metrics

/**
 * Interface for event projection metrics.
 * Provides specific metrics for tracking projection operations.
 */
interface ProjectionMetrics {
    /**
     * Record a successful projection of an event.
     * @param eventType The type of event that was projected
     */
    fun recordProjectionSuccess(eventType: String)

    /**
     * Record a failed projection of an event.
     * @param eventType The type of event that failed to project
     * @param reason Optional reason for the failure
     */
    fun recordProjectionFailure(eventType: String, reason: String? = null)

    /**
     * Record a skipped event (unknown event type that didn't fail the projection).
     * @param eventType The type of event that was skipped
     */
    fun recordEventSkipped(eventType: String)

    /**
     * Record an unmapped event (event type that couldn't be mapped for aggregate ID extraction).
     * @param eventType The type of event that was unmapped
     */
    fun recordEventUnmapped(eventType: String)
}

/**
 * Default implementation of ProjectionMetrics using MetricsRegistry.
 */
class DefaultProjectionMetrics(private val metricsRegistry: MetricsRegistry) : ProjectionMetrics {

    companion object {
        private const val PROJECTION_SUCCESS = "projection_success_total"
        private const val PROJECTION_FAILURE = "projection_failure_total"
        private const val EVENT_SKIPPED = "projection_event_skipped_total"
        private const val EVENT_UNMAPPED = "projection_event_unmapped_total"
    }

    override fun recordProjectionSuccess(eventType: String) {
        metricsRegistry.counter(
            name = PROJECTION_SUCCESS,
            description = "Total number of successful event projections",
            tags = mapOf("event_type" to eventType),
        ).increment()
    }

    override fun recordProjectionFailure(eventType: String, reason: String?) {
        val tags = mutableMapOf("event_type" to eventType)
        if (reason != null) {
            tags["failure_reason"] = reason
        }

        metricsRegistry.counter(
            name = PROJECTION_FAILURE,
            description = "Total number of failed event projections",
            tags = tags,
        ).increment()
    }

    override fun recordEventSkipped(eventType: String) {
        metricsRegistry.counter(
            name = EVENT_SKIPPED,
            description = "Total number of skipped unknown events",
            tags = mapOf("event_type" to eventType),
        ).increment()
    }

    override fun recordEventUnmapped(eventType: String) {
        metricsRegistry.counter(
            name = EVENT_UNMAPPED,
            description = "Total number of unmapped events for aggregate ID extraction",
            tags = mapOf("event_type" to eventType),
        ).increment()
    }
}

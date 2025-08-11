package io.github.kamiazya.scopes.infrastructure.error

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration

/**
 * External API adapter errors for remote service integrations.
 * Covers network issues, service unavailability, and circuit breaker states.
 */
sealed class ExternalApiAdapterError : InfrastructureAdapterError() {

    /**
     * Network-level connectivity errors.
     */
    data class NetworkError(
        val endpoint: String,
        val errorType: NetworkErrorType,
        val cause: Throwable,
        override val timestamp: Instant,
        override val correlationId: String? = null
    ) : ExternalApiAdapterError() {
        // Delegate retry logic to the enum's semantics
        override val retryable: Boolean = errorType.retryable
    }

    /**
     * HTTP protocol errors with status codes.
     */
    data class HttpError(
        val endpoint: String,
        val statusCode: Int,
        val responseBody: String? = null,
        val headers: Map<String, String>? = null,
        val retryAfterAt: Instant? = null,
        override val timestamp: Instant,
        override val correlationId: String? = null
    ) : ExternalApiAdapterError() {
        override val retryable: Boolean = when (statusCode) {
            408, 502, 504 -> true
            429, 503 -> retryAfterAt?.let { it <= Clock.System.now() } ?: false
            else -> false
        }
    }

    /**
     * Request timeout errors.
     */
    data class TimeoutError(
        val endpoint: String,
        val timeout: Duration,
        val phase: String, // "connection", "read", "write"
        override val timestamp: Instant,
        override val correlationId: String? = null
    ) : ExternalApiAdapterError() {
        // Timeouts are transient and retryable
        override val retryable: Boolean = true
    }

    /**
     * Circuit breaker activation errors.
     */
    data class CircuitBreakerError(
        val serviceName: String,
        val state: CircuitBreakerState,
        val failureCount: Int,
        val nextAttemptAt: Instant? = null,
        override val timestamp: Instant,
        override val correlationId: String? = null
    ) : ExternalApiAdapterError() {
        // Only half-open state allows retries (testing if service recovered)
        override val retryable: Boolean = state == CircuitBreakerState.HALF_OPEN
    }

    /**
     * Rate limiting errors.
     */
    data class RateLimitError(
        val endpoint: String,
        val limitType: RateLimitType,
        val limit: Int,
        val window: Duration,
        val resetAt: Instant,
        val retryAfterAt: Instant? = null,
        override val timestamp: Instant,
        override val correlationId: String? = null
    ) : ExternalApiAdapterError() {
        // Rate limits are retryable only if reset time has passed
        override val retryable: Boolean = resetAt <= Clock.System.now()
    }

    /**
     * Service health and availability errors.
     */
    data class ServiceUnavailableError(
        val serviceName: String,
        val healthStatus: ServiceHealthStatus,
        val estimatedRecoveryAt: Instant? = null,
        val alternativeEndpoint: String? = null,
        override val timestamp: Instant,
        override val correlationId: String? = null
    ) : ExternalApiAdapterError() {
        // Service unavailability is typically temporary
        override val retryable: Boolean = true
    }
}

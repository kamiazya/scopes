package io.github.kamiazya.scopes.infrastructure.error

import kotlinx.datetime.Instant

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
        // Most network errors are transient except certificate issues
        override val retryable: Boolean = errorType != NetworkErrorType.CERTIFICATE_ERROR
    }
    
    /**
     * HTTP protocol errors with status codes.
     */
    data class HttpError(
        val endpoint: String,
        val statusCode: Int,
        val responseBody: String? = null,
        val headers: Map<String, String>? = null,
        val retryAfter: Long? = null,
        override val timestamp: Instant,
        override val correlationId: String? = null
    ) : ExternalApiAdapterError() {
        // Common retryable HTTP status codes for transient issues
        override val retryable: Boolean = statusCode in setOf(408, 429, 502, 503, 504)
    }
    
    /**
     * Request timeout errors.
     */
    data class TimeoutError(
        val endpoint: String,
        val timeoutMillis: Long,
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
        val nextAttemptAt: Long? = null,
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
        val windowSeconds: Int,
        val resetTime: Long,
        val retryAfter: Long? = null,
        override val timestamp: Instant,
        override val correlationId: String? = null
    ) : ExternalApiAdapterError() {
        // Rate limits are temporary and retryable after reset
        override val retryable: Boolean = true
    }
    
    /**
     * Service health and availability errors.
     */
    data class ServiceUnavailableError(
        val serviceName: String,
        val healthStatus: ServiceHealthStatus,
        val estimatedRecoveryTime: Long? = null,
        val alternativeEndpoint: String? = null,
        override val timestamp: Instant,
        override val correlationId: String? = null
    ) : ExternalApiAdapterError() {
        // Service unavailability is typically temporary
        override val retryable: Boolean = true
    }
}
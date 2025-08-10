package io.github.kamiazya.scopes.infrastructure.http

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.infrastructure.error.InfrastructureAdapterError
import io.github.kamiazya.scopes.infrastructure.error.NetworkErrorType
import io.github.kamiazya.scopes.infrastructure.error.CircuitBreakerState
import io.github.kamiazya.scopes.infrastructure.error.ServiceHealthStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import java.io.IOException
import java.net.*
import java.security.cert.CertificateException
import javax.net.ssl.SSLException
import javax.net.ssl.SSLHandshakeException

/**
 * Production implementation of HTTP API adapter for external service integration.
 * Provides comprehensive error handling for network, HTTP, and infrastructure failures.
 */
class HttpApiAdapter : ApiAdapter {

    override suspend fun execute(request: HttpRequest): Either<InfrastructureAdapterError.ExternalApiAdapterError, HttpResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val startTime = System.currentTimeMillis()
                
                // Create URL connection
                val url = URL(request.url)
                val connection = url.openConnection() as HttpURLConnection
                
                // Configure connection
                connection.apply {
                    requestMethod = request.method
                    connectTimeout = request.timeoutMs.toInt()
                    readTimeout = request.timeoutMs.toInt()
                    doInput = true
                    
                    // Set headers
                    request.headers.forEach { (key, value) ->
                        setRequestProperty(key, value)
                    }
                    
                    // Set body for POST/PUT/PATCH requests
                    if (request.body != null && request.method in listOf("POST", "PUT", "PATCH")) {
                        doOutput = true
                        outputStream.use { os ->
                            os.write(request.body.toByteArray())
                            os.flush()
                        }
                    }
                }
                
                val responseCode = connection.responseCode
                val responseTime = System.currentTimeMillis() - startTime
                
                when {
                    responseCode in 200..299 -> {
                        // Success response
                        val responseBody = connection.inputStream.use { it.bufferedReader().readText() }
                        val responseHeaders = connection.headerFields
                            .filter { it.key != null }
                            .mapValues { it.value.firstOrNull() ?: "" }
                            .filterValues { it.isNotEmpty() }
                        
                        HttpResponse(
                            statusCode = responseCode,
                            headers = responseHeaders,
                            body = responseBody,
                            responseTimeMs = responseTime,
                            correlationId = request.correlationId
                        ).right()
                    }
                    responseCode == 429 -> {
                        // Rate limiting
                        InfrastructureAdapterError.ExternalApiAdapterError.HttpError(
                            endpoint = request.url,
                            method = request.method,
                            statusCode = responseCode,
                            responseBody = safeReadErrorStream(connection),
                            headers = extractHeaders(connection),
                            timestamp = Clock.System.now(),
                            correlationId = request.correlationId
                        ).left()
                    }
                    responseCode in 500..599 -> {
                        // Server error - retryable
                        InfrastructureAdapterError.ExternalApiAdapterError.HttpError(
                            endpoint = request.url,
                            method = request.method,
                            statusCode = responseCode,
                            responseBody = safeReadErrorStream(connection),
                            headers = extractHeaders(connection),
                            timestamp = Clock.System.now(),
                            correlationId = request.correlationId
                        ).left()
                    }
                    responseCode == 503 -> {
                        // Service unavailable
                        val retryAfterHeader = connection.getHeaderField("Retry-After")
                        val serviceName = extractServiceName(request.url)
                        val retryAfter = retryAfterHeader?.toLongOrNull()?.let { 
                            Clock.System.now().plus(kotlin.time.Duration.parse("${it}s"))
                        }
                        
                        InfrastructureAdapterError.ExternalApiAdapterError.ServiceUnavailableError(
                            endpoint = request.url,
                            serviceName = serviceName,
                            healthStatus = ServiceHealthStatus.UNHEALTHY,
                            retryAfter = retryAfter,
                            timestamp = Clock.System.now(),
                            correlationId = request.correlationId
                        ).left()
                    }
                    else -> {
                        // Client error - non-retryable
                        InfrastructureAdapterError.ExternalApiAdapterError.HttpError(
                            endpoint = request.url,
                            method = request.method,
                            statusCode = responseCode,
                            responseBody = safeReadErrorStream(connection),
                            headers = extractHeaders(connection),
                            timestamp = Clock.System.now(),
                            correlationId = request.correlationId
                        ).left()
                    }
                }
            } catch (e: Exception) {
                handleNetworkException(e, request)
            }
        }
    }

    override suspend fun executeWithCircuitBreaker(
        request: HttpRequest,
        circuitBreakerManager: CircuitBreakerManager
    ): Either<InfrastructureAdapterError.ExternalApiAdapterError, HttpResponse> {
        val circuitState = circuitBreakerManager.getCircuitState(request.url)
        
        return when (circuitState) {
            CircuitBreakerState.OPEN -> {
                // Circuit is open, fail fast
                InfrastructureAdapterError.ExternalApiAdapterError.CircuitBreakerError(
                    endpoint = request.url,
                    state = circuitState,
                    failureCount = 0, // Would be tracked by the circuit breaker
                    nextAttemptAt = null,
                    timestamp = Clock.System.now(),
                    correlationId = request.correlationId
                ).left()
            }
            CircuitBreakerState.HALF_OPEN,
            CircuitBreakerState.CLOSED -> {
                // Circuit allows requests, execute and record result
                val result = execute(request)
                when (result) {
                    is Either.Right -> {
                        circuitBreakerManager.recordSuccess(request.url)
                        result
                    }
                    is Either.Left -> {
                        circuitBreakerManager.recordFailure(request.url)
                        result
                    }
                }
            }
        }
    }

    private fun handleNetworkException(
        exception: Exception, 
        request: HttpRequest
    ): Either.Left<InfrastructureAdapterError.ExternalApiAdapterError> {
        val networkError = when (exception) {
            is UnknownHostException -> {
                InfrastructureAdapterError.ExternalApiAdapterError.NetworkError(
                    endpoint = request.url,
                    method = request.method,
                    networkType = NetworkErrorType.DNS_RESOLUTION,
                    cause = exception,
                    timestamp = Clock.System.now(),
                    correlationId = request.correlationId
                )
            }
            is ConnectException -> {
                InfrastructureAdapterError.ExternalApiAdapterError.NetworkError(
                    endpoint = request.url,
                    method = request.method,
                    networkType = NetworkErrorType.CONNECTION_REFUSED,
                    cause = exception,
                    timestamp = Clock.System.now(),
                    correlationId = request.correlationId
                )
            }
            is SocketTimeoutException -> {
                InfrastructureAdapterError.ExternalApiAdapterError.TimeoutError(
                    endpoint = request.url,
                    method = request.method,
                    timeoutMs = request.timeoutMs,
                    elapsedMs = request.timeoutMs + 100, // Approximate
                    timestamp = Clock.System.now(),
                    correlationId = request.correlationId
                )
            }
            is SSLHandshakeException, is CertificateException -> {
                InfrastructureAdapterError.ExternalApiAdapterError.NetworkError(
                    endpoint = request.url,
                    method = request.method,
                    networkType = NetworkErrorType.CERTIFICATE_ERROR,
                    cause = exception,
                    timestamp = Clock.System.now(),
                    correlationId = request.correlationId
                )
            }
            is SSLException -> {
                InfrastructureAdapterError.ExternalApiAdapterError.NetworkError(
                    endpoint = request.url,
                    method = request.method,
                    networkType = NetworkErrorType.SSL_HANDSHAKE,
                    cause = exception,
                    timestamp = Clock.System.now(),
                    correlationId = request.correlationId
                )
            }
            else -> {
                InfrastructureAdapterError.ExternalApiAdapterError.NetworkError(
                    endpoint = request.url,
                    method = request.method,
                    networkType = NetworkErrorType.UNKNOWN_HOST,
                    cause = exception,
                    timestamp = Clock.System.now(),
                    correlationId = request.correlationId
                )
            }
        }
        return Either.Left(networkError)
    }

    private fun safeReadErrorStream(connection: HttpURLConnection): String? {
        return try {
            connection.errorStream?.use { it.bufferedReader().readText() }
        } catch (e: Exception) {
            null
        }
    }

    private fun extractHeaders(connection: HttpURLConnection): Map<String, String>? {
        return try {
            connection.headerFields
                ?.filter { it.key != null }
                ?.mapValues { it.value.firstOrNull() ?: "" }
                ?.filterValues { it.isNotEmpty() }
        } catch (e: Exception) {
            null
        }
    }

    private fun extractServiceName(url: String): String {
        return try {
            URL(url).host.split('.').let { parts ->
                if (parts.size > 1) parts.takeLast(2).joinToString(".") else parts.firstOrNull() ?: "unknown"
            }
        } catch (e: Exception) {
            "unknown"
        }
    }
}

// Supporting data classes for HTTP operations
data class HttpRequest(
    val url: String,
    val method: String,
    val headers: Map<String, String>,
    val body: String?,
    val timeoutMs: Long,
    val correlationId: String = java.util.UUID.randomUUID().toString()
)

data class HttpResponse(
    val statusCode: Int,
    val headers: Map<String, String>,
    val body: String,
    val responseTimeMs: Long,
    val correlationId: String
)

/**
 * API Adapter interface for external service integration.
 */
interface ApiAdapter {
    suspend fun execute(request: HttpRequest): Either<InfrastructureAdapterError.ExternalApiAdapterError, HttpResponse>
    suspend fun executeWithCircuitBreaker(
        request: HttpRequest,
        circuitBreakerManager: CircuitBreakerManager
    ): Either<InfrastructureAdapterError.ExternalApiAdapterError, HttpResponse>
}

/**
 * Circuit breaker manager interface for managing circuit breaker states.
 */
interface CircuitBreakerManager {
    fun getCircuitState(endpoint: String): CircuitBreakerState
    fun recordSuccess(endpoint: String)
    fun recordFailure(endpoint: String)
}
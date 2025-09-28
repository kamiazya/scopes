package io.github.kamiazya.scopes.interfaces.daemon.health

import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.grpc.health.v1.HealthCheckResponse
import io.grpc.services.HealthStatusManager

/**
 * Standard gRPC Health Service implementation for monitoring daemon health.
 *
 * This service follows the gRPC Health Checking Protocol:
 * https://github.com/grpc/grpc/blob/master/doc/health-checking.md
 *
 * Services monitored:
 * - "" (empty string): Overall server health
 * - "scopes.daemon.v1beta.ControlService": Control service health
 * - "scopes.daemon.v1beta.TaskGatewayService": Task gateway service health
 */
class HealthService(private val logger: Logger) {
    /**
     * The health status manager that tracks service health.
     */
    val statusManager = HealthStatusManager()

    init {
        // Initialize service health statuses
        setServiceHealth("", HealthCheckResponse.ServingStatus.SERVING)
        setServiceHealth("scopes.daemon.v1beta.ControlService", HealthCheckResponse.ServingStatus.SERVING)
        setServiceHealth("scopes.daemon.v1beta.TaskGatewayService", HealthCheckResponse.ServingStatus.SERVING)

        logger.info(
            "Health service initialized",
            mapOf(
                "services" to listOf("", "ControlService", "TaskGatewayService"),
            ),
        )
    }

    /**
     * Sets the health status for a specific service.
     *
     * @param serviceName The service name (empty string for overall health)
     * @param status The serving status
     */
    fun setServiceHealth(serviceName: String, status: HealthCheckResponse.ServingStatus) {
        statusManager.setStatus(serviceName, status)

        logger.debug(
            "Service health updated",
            mapOf(
                "service" to if (serviceName.isEmpty()) "overall" else serviceName,
                "status" to status.name,
            ),
        )
    }

    /**
     * Gets the bindable service for registration with the gRPC server.
     */
    fun getHealthService(): io.grpc.BindableService = statusManager.healthService

    /**
     * Marks all services as NOT_SERVING (used during shutdown).
     */
    fun markAllServicesUnhealthy() {
        val services = listOf("", "scopes.daemon.v1beta.ControlService", "scopes.daemon.v1beta.TaskGatewayService")

        services.forEach { service ->
            setServiceHealth(service, HealthCheckResponse.ServingStatus.NOT_SERVING)
        }

        logger.info("All services marked as NOT_SERVING for shutdown", emptyMap())
    }

    /**
     * Marks all services as SERVING (used after successful startup).
     */
    fun markAllServicesHealthy() {
        val services = listOf("", "scopes.daemon.v1beta.ControlService", "scopes.daemon.v1beta.TaskGatewayService")

        services.forEach { service ->
            setServiceHealth(service, HealthCheckResponse.ServingStatus.SERVING)
        }

        logger.info("All services marked as SERVING", emptyMap())
    }
}

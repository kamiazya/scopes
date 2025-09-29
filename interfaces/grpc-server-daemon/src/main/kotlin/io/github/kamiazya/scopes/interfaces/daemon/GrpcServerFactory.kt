package io.github.kamiazya.scopes.interfaces.daemon

import io.github.kamiazya.scopes.interfaces.daemon.grpc.services.TaskGatewayServiceImpl
import io.github.kamiazya.scopes.interfaces.daemon.health.HealthService
import io.github.kamiazya.scopes.platform.observability.logging.ApplicationInfo
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.grpc.BindableService
import kotlinx.coroutines.flow.SharedFlow

/**
 * Factory for creating gRPC server components.
 * This encapsulates the gRPC implementation details from the daemon application.
 */
class GrpcServerFactory(private val taskGatewayService: TaskGatewayServiceImpl? = null) {

    /**
     * Creates a gRPC server with all required services.
     *
     * @param applicationInfo Application information for the control service
     * @param logger Logger instance
     * @param host Host to bind to
     * @param port Port to bind to
     * @param useUnixSocket Whether to use Unix Domain Socket instead of TCP
     * @param socketPath Path for the Unix socket file (required if useUnixSocket is true)
     * @return GrpcServerWrapper containing the server and shutdown monitor
     */
    fun createServer(
        applicationInfo: ApplicationInfo,
        logger: Logger,
        host: String = "127.0.0.1",
        port: Int = 0,
        useUnixSocket: Boolean = false,
        socketPath: String? = null
    ): GrpcServerWrapper {
        val controlService = ControlServiceImpl(
            applicationInfo = applicationInfo,
            logger = logger,
        )

        // Create health service
        val healthService = HealthService(logger)

        val services = mutableListOf<BindableService>(
            controlService,
            healthService.getHealthService(), // Add health service
        )

        // Add TaskGatewayService if available
        if (taskGatewayService != null) {
            services.add(taskGatewayService)
        }

        val server = GrpcServer(
            services = services,
            logger = logger,
            host = host,
            port = port,
            useUnixSocket = useUnixSocket,
            socketPath = socketPath,
        )

        return GrpcServerWrapper(
            server = server,
            shutdownSignal = controlService.shutdownSignal,
            healthService = healthService, // Include health service in wrapper
        )
    }
}

/**
 * Wrapper that hides the gRPC implementation details from the daemon application.
 */
data class GrpcServerWrapper(
    val server: GrpcServer,
    val shutdownSignal: SharedFlow<ShutdownSignal>,
    val healthService: HealthService, // Health service for lifecycle management
)

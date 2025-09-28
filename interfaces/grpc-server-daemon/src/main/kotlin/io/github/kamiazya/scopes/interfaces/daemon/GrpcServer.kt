package io.github.kamiazya.scopes.interfaces.daemon

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.interfaces.daemon.interceptors.CorrelationIdServerInterceptor
import io.github.kamiazya.scopes.interfaces.daemon.interceptors.RequestLoggingServerInterceptor
import io.github.kamiazya.scopes.platform.infrastructure.endpoint.EndpointFileUtils
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.grpc.BindableService
import io.grpc.Server
import io.grpc.ServerInterceptors
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.net.InetSocketAddress
import kotlin.time.Duration.Companion.seconds

/**
 * gRPC server wrapper that manages the server lifecycle and endpoint information.
 */
class GrpcServer(
    private val services: List<BindableService>,
    private val logger: Logger,
    private val host: String = "127.0.0.1",
    private val port: Int = 0, // 0 means ephemeral port
) {
    private var server: Server? = null
    private var actualPort: Int = -1

    /**
     * Error types for server operations.
     */
    sealed class ServerError(message: String, cause: Throwable? = null) : Exception(message, cause) {
        class StartupError(message: String, cause: Throwable? = null) : ServerError(message, cause)
        class ShutdownError(message: String, cause: Throwable? = null) : ServerError(message, cause)
        class EndpointError(message: String, cause: Throwable? = null) : ServerError(message, cause)
    }

    /**
     * Server information after successful startup.
     */
    data class ServerInfo(val host: String, val port: Int, val address: String)

    /**
     * Starts the gRPC server on the specified host and port.
     *
     * @return Either a ServerError or ServerInfo containing the actual bind address
     */
    suspend fun start(): Either<ServerError, ServerInfo> = withContext(Dispatchers.IO) {
        try {
            val maxMessageSizeMB = getMaxMessageSize() / (1024 * 1024)
            val maxConcurrentCalls = getMaxConcurrentCalls()

            logger.info(
                "Starting gRPC server",
                mapOf(
                    "host" to host,
                    "port" to port.toString(),
                    "maxMessageSizeMB" to maxMessageSizeMB.toString(),
                    "maxConcurrentCalls" to maxConcurrentCalls.toString(),
                ),
            )

            val serverBuilder = NettyServerBuilder.forAddress(InetSocketAddress(host, port))
                .executor(null) // Use default executor
                .maxInboundMessageSize(getMaxMessageSize()) // Configure max message size
                .maxConcurrentCallsPerConnection(getMaxConcurrentCalls()) // Limit concurrent calls per connection

            // Interceptors (correlation + basic request logging)
            val interceptors = listOf(
                CorrelationIdServerInterceptor(),
                RequestLoggingServerInterceptor(logger),
            )

            // Add all services with interceptors
            services.forEach { service ->
                val intercepted = ServerInterceptors.intercept(service, interceptors)
                serverBuilder.addService(intercepted)
                logger.debug("Registered gRPC service", mapOf("service" to service.javaClass.simpleName))
            }

            val builtServer = serverBuilder.build()
            builtServer.start()

            server = builtServer
            actualPort = builtServer.port

            val serverInfo = ServerInfo(
                host = host,
                port = actualPort,
                address = "$host:$actualPort",
            )

            logger.info(
                "gRPC server started successfully",
                mapOf(
                    "host" to host,
                    "port" to actualPort.toString(),
                    "address" to serverInfo.address,
                ),
            )

            serverInfo.right()
        } catch (e: Exception) {
            logger.error("Failed to start gRPC server", mapOf("host" to host, "port" to port.toString()), e)
            ServerError.StartupError("Failed to start gRPC server: ${e.message}", e).left()
        }
    }

    /**
     * Stops the gRPC server gracefully.
     *
     * @param gracePeriodSeconds Time to wait for graceful shutdown before forcing
     * @return Either a ServerError or Unit on success
     */
    suspend fun stop(gracePeriodSeconds: Int = 5): Either<ServerError, Unit> = withContext(Dispatchers.IO) {
        try {
            val currentServer = server
            if (currentServer == null) {
                logger.warn("Attempted to stop server that was not started")
                return@withContext Unit.right()
            }

            logger.info("Stopping gRPC server", mapOf("gracePeriodSeconds" to gracePeriodSeconds.toString()))

            currentServer.shutdown()

            // Use Kotlin coroutine-based timeout instead of TimeUnit
            val gracefullyTerminated = withTimeoutOrNull(gracePeriodSeconds.seconds) {
                while (!currentServer.isTerminated) {
                    delay(100) // Check every 100ms
                }
                true
            } ?: false

            if (!gracefullyTerminated) {
                logger.warn("Server did not shutdown gracefully, forcing termination")
                currentServer.shutdownNow()

                val forcedTerminated = withTimeoutOrNull(5.seconds) {
                    while (!currentServer.isTerminated) {
                        delay(100) // Check every 100ms
                    }
                    true
                } ?: false

                if (!forcedTerminated) {
                    logger.error("Server did not terminate after forced shutdown")
                    return@withContext ServerError.ShutdownError("Server failed to terminate").left()
                }
            }

            server = null
            actualPort = -1

            logger.info("gRPC server stopped successfully")
            Unit.right()
        } catch (e: Exception) {
            logger.error("Error stopping gRPC server", throwable = e)
            ServerError.ShutdownError("Error stopping server: ${e.message}", e).left()
        }
    }

    /**
     * Writes endpoint information to a file for CLI discovery.
     *
     * @param endpointFile The file to write endpoint information to
     * @return Either a ServerError or Unit on success
     */
    suspend fun writeEndpointFile(endpointFile: File): Either<ServerError, Unit> = withContext(Dispatchers.IO) {
        try {
            if (actualPort == -1) {
                return@withContext ServerError.EndpointError("Server not started").left()
            }

            // Ensure parent directory exists using centralized utility
            if (!EndpointFileUtils.ensureEndpointDirectoryExists(endpointFile)) {
                return@withContext ServerError.EndpointError("Failed to create endpoint directory").left()
            }

            val endpointContent = buildString {
                appendLine("version=1")
                appendLine("addr=$host:$actualPort")
                appendLine("transport=tcp")
                appendLine("pid=${ProcessHandle.current().pid()}")
                appendLine("started=${System.currentTimeMillis()}")
            }

            endpointFile.writeText(endpointContent)

            // Set secure file permissions using centralized utility
            if (!EndpointFileUtils.setSecurePermissions(endpointFile)) {
                logger.warn("Could not set endpoint file permissions", mapOf("file" to endpointFile.absolutePath))
            }

            logger.info("Endpoint file written", mapOf("file" to endpointFile.absolutePath))
            Unit.right()
        } catch (e: Exception) {
            logger.error("Failed to write endpoint file", mapOf("file" to endpointFile.absolutePath), e)
            ServerError.EndpointError("Failed to write endpoint file: ${e.message}", e).left()
        }
    }

    /**
     * Checks if the server is currently running.
     */
    fun isRunning(): Boolean = server?.isShutdown == false

    /**
     * Gets the current server address if running.
     */
    fun getAddress(): String? = if (actualPort != -1) "$host:$actualPort" else null

    companion object {
        // Default configuration values
        private const val DEFAULT_MAX_MESSAGE_SIZE_MB = 4
        private const val DEFAULT_MAX_CONCURRENT_CALLS = 100

        /**
         * Gets the maximum inbound message size from environment or uses default.
         * @return Maximum message size in bytes
         */
        private fun getMaxMessageSize(): Int {
            val envValue = System.getenv("SCOPES_GRPC_MAX_MESSAGE_SIZE_MB")?.toIntOrNull()
            val maxSizeMB = envValue ?: DEFAULT_MAX_MESSAGE_SIZE_MB
            return maxSizeMB * 1024 * 1024 // Convert MB to bytes
        }

        /**
         * Gets the maximum concurrent calls per connection from environment or uses default.
         * @return Maximum number of concurrent calls per connection
         */
        private fun getMaxConcurrentCalls(): Int {
            val envValue = System.getenv("SCOPES_GRPC_MAX_CONCURRENT_CALLS")?.toIntOrNull()
            return envValue ?: DEFAULT_MAX_CONCURRENT_CALLS
        }
    }
}

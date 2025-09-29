package io.github.kamiazya.scopes.interfaces.cli.grpc

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.platform.infrastructure.grpc.ChannelBuilder
import io.github.kamiazya.scopes.platform.infrastructure.grpc.ChannelWithEventLoop
import io.github.kamiazya.scopes.platform.infrastructure.grpc.EndpointResolver
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.rpc.v1beta.ControlServiceGrpcKt
import io.github.kamiazya.scopes.rpc.v1beta.GetVersionRequest
import io.github.kamiazya.scopes.rpc.v1beta.GetVersionResponse
import io.github.kamiazya.scopes.rpc.v1beta.PingRequest
import io.github.kamiazya.scopes.rpc.v1beta.PingResponse
import io.github.kamiazya.scopes.rpc.v1beta.ShutdownRequest
import io.github.kamiazya.scopes.rpc.v1beta.ShutdownResponse
import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

/**
 * gRPC client for communicating with the Scopes daemon.
 */
class GrpcClient(private val endpointResolver: EndpointResolver, private val logger: Logger) {
    private var channelWithEventLoop: ChannelWithEventLoop? = null
    private var controlService: ControlServiceGrpcKt.ControlServiceCoroutineStub? = null

    /**
     * Error types for client operations.
     */
    sealed class ClientError(message: String, cause: Throwable? = null) : Exception(message, cause) {
        class ConnectionError(message: String, cause: Throwable? = null) : ClientError(message, cause)
        class ServiceError(message: String, cause: Throwable? = null) : ClientError(message, cause)
        class TimeoutError(message: String) : ClientError(message)
    }

    /**
     * Connects to the daemon using the resolved endpoint.
     *
     * @return Either a ClientError or Unit on success
     */
    suspend fun connect(): Either<ClientError, Unit> = withContext(Dispatchers.IO) {
        try {
            // Resolve daemon endpoint
            val endpointInfo = endpointResolver.resolve().fold(
                { error ->
                    return@withContext ClientError.ConnectionError(
                        "Failed to resolve daemon endpoint: ${error.message}",
                        error,
                    ).left()
                },
                { it },
            )

            logger.debug(
                "Connecting to daemon",
                mapOf(
                    "host" to endpointInfo.host,
                    "port" to endpointInfo.port.toString(),
                    "transport" to endpointInfo.transport,
                ),
            )

            // Create channel with EventLoopGroup using common builder
            val channelWithEventLoop = if (endpointInfo.isUnixSocket() && endpointInfo.socketPath != null) {
                logger.debug(
                    "Using Unix Domain Socket connection",
                    mapOf("socketPath" to endpointInfo.socketPath),
                )
                ChannelBuilder.createUnixSocketChannel(
                    socketPath = endpointInfo.socketPath,
                    logger = logger,
                    // Use default timeout from environment or 30 seconds
                )
            } else {
                ChannelBuilder.createChannelWithEventLoop(
                    host = endpointInfo.host,
                    port = endpointInfo.port,
                    logger = logger,
                    // Use default timeout from environment or 30 seconds
                )
            }

            this.channelWithEventLoop = channelWithEventLoop
            controlService = ControlServiceGrpcKt.ControlServiceCoroutineStub(channelWithEventLoop.channel)

            logger.info("Connected to daemon", mapOf("address" to endpointInfo.address))
            Unit.right()
        } catch (e: Exception) {
            logger.error("Failed to connect to daemon", mapOf("error" to e.javaClass.simpleName), e)
            ClientError.ConnectionError("Failed to connect to daemon: ${e.message}", e).left()
        }
    }

    /**
     * Disconnects from the daemon.
     */
    suspend fun disconnect(): Either<ClientError, Unit> = withContext(Dispatchers.IO) {
        try {
            // Properly shutdown both channel and EventLoopGroup
            channelWithEventLoop?.shutdown()
            channelWithEventLoop = null
            controlService = null

            logger.debug("Disconnected from daemon")

            Unit.right()
        } catch (e: Exception) {
            logger.error("Error disconnecting from daemon", mapOf("error" to e.javaClass.simpleName), e)
            ClientError.ConnectionError("Error disconnecting: ${e.message}", e).left()
        }
    }

    /**
     * Pings the daemon to check if it's alive.
     */
    suspend fun ping(): Either<ClientError, PingResponse> = executeWithService { service ->
        service.ping(PingRequest.getDefaultInstance())
    }

    /**
     * Gets version information from the daemon.
     */
    suspend fun getVersion(): Either<ClientError, GetVersionResponse> = executeWithService { service ->
        service.getVersion(GetVersionRequest.getDefaultInstance())
    }

    /**
     * Requests the daemon to shut down.
     *
     * @param reason Optional reason for shutdown
     * @param gracePeriodSeconds Grace period before forcing shutdown
     * @param saveState Whether to save state before shutdown
     */
    suspend fun shutdown(reason: String = "CLI request", gracePeriodSeconds: Int = 5, saveState: Boolean = true): Either<ClientError, ShutdownResponse> {
        val request = ShutdownRequest.newBuilder()
            .setReason(reason)
            .setGracePeriodSeconds(gracePeriodSeconds)
            .setSaveState(saveState)
            .build()

        return executeWithService { service ->
            service.shutdown(request)
        }
    }

    /**
     * Executes a gRPC call with proper error handling.
     */
    private suspend inline fun <T> executeWithService(
        crossinline operation: suspend (ControlServiceGrpcKt.ControlServiceCoroutineStub) -> T,
    ): Either<ClientError, T> {
        val service = controlService
            ?: return ClientError.ConnectionError("Not connected to daemon").left()

        return try {
            operation(service).right()
        } catch (e: StatusException) {
            val errorMessage = when (e.status.code) {
                Status.Code.UNAVAILABLE -> "Daemon is unavailable"
                Status.Code.DEADLINE_EXCEEDED -> "Request timed out"
                Status.Code.CANCELLED -> "Request was cancelled"
                Status.Code.UNAUTHENTICATED -> "Authentication failed"
                Status.Code.PERMISSION_DENIED -> "Permission denied"
                Status.Code.INVALID_ARGUMENT -> "Invalid request: ${e.status.description}"
                Status.Code.NOT_FOUND -> "Service not found"
                Status.Code.UNIMPLEMENTED -> "Operation not implemented"
                else -> "Service error: ${e.status.description}"
            }

            logger.error("gRPC service error", mapOf("status" to e.status.code.toString()), e)

            when (e.status.code) {
                Status.Code.DEADLINE_EXCEEDED -> ClientError.TimeoutError(errorMessage).left()
                else -> ClientError.ServiceError(errorMessage, e).left()
            }
        } catch (e: Exception) {
            logger.error("Unexpected error during gRPC call", mapOf("error" to e.javaClass.simpleName), e)
            ClientError.ServiceError("Unexpected error: ${e.message}", e).left()
        }
    }

    /**
     * Checks if the client is currently connected.
     */
    fun isConnected(): Boolean {
        val currentChannel = channelWithEventLoop?.channel
        return currentChannel != null && !currentChannel.isShutdown
    }

    /**
     * Gets the current connection state for debugging.
     */
    fun getConnectionState(): String? = channelWithEventLoop?.channel?.getState(false)?.toString()
}

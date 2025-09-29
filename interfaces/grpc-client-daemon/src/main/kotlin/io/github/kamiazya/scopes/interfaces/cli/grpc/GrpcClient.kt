package io.github.kamiazya.scopes.interfaces.cli.grpc

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.platform.infrastructure.grpc.ChannelBuilder
import io.github.kamiazya.scopes.platform.infrastructure.grpc.ChannelWithEventLoop
import io.github.kamiazya.scopes.platform.infrastructure.grpc.EndpointResolver
import io.github.kamiazya.scopes.platform.infrastructure.grpc.RetryPolicy
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.rpc.v1beta.ControlServiceGrpcKt
import io.github.kamiazya.scopes.rpc.v1beta.GetVersionRequest
import io.github.kamiazya.scopes.rpc.v1beta.GetVersionResponse
import io.github.kamiazya.scopes.rpc.v1beta.PingRequest
import io.github.kamiazya.scopes.rpc.v1beta.PingResponse
import io.github.kamiazya.scopes.rpc.v1beta.ShutdownRequest
import io.github.kamiazya.scopes.rpc.v1beta.ShutdownResponse
import io.grpc.Status
import io.grpc.StatusException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * gRPC client for communicating with the Scopes daemon.
 */
class GrpcClient(
    private val endpointResolver: EndpointResolver,
    private val logger: Logger,
    private val retryPolicy: RetryPolicy = RetryPolicy.fromEnvironment(logger),
) {
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
     * @param useRetry Whether to use retry logic for connection attempts
     * @return Either a ClientError or Unit on success
     */
    suspend fun connect(useRetry: Boolean = true): Either<ClientError, Unit> = withContext(Dispatchers.IO) {
        if (useRetry) {
            retryPolicy.execute<ClientError, Unit>("daemon-connection") { attemptNumber ->
                performConnect(attemptNumber)
            }
        } else {
            performConnect(1)
        }
    }

    /**
     * Performs the actual connection attempt.
     */
    private suspend fun performConnect(attemptNumber: Int): Either<ClientError, Unit> {
        return try {
            // Resolve daemon endpoint
            val endpointInfo = endpointResolver.resolve().fold(
                { error ->
                    return ClientError.ConnectionError(
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
                    "attempt" to attemptNumber,
                ),
            )

            // Create channel with EventLoopGroup using common builder
            val channelResult = ChannelBuilder.createChannelWithEventLoop(
                host = endpointInfo.host,
                port = endpointInfo.port,
                logger = logger,
                // Use default timeout from environment or 30 seconds
            )

            this@GrpcClient.channelWithEventLoop = channelResult
            controlService = ControlServiceGrpcKt.ControlServiceCoroutineStub(channelResult.channel)

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
     *
     * @param useRetry Whether to use retry logic for the operation
     */
    suspend fun ping(useRetry: Boolean = true): Either<ClientError, PingResponse> = executeWithService(useRetry) { service ->
        service.ping(PingRequest.getDefaultInstance())
    }

    /**
     * Gets version information from the daemon.
     *
     * @param useRetry Whether to use retry logic for the operation
     */
    suspend fun getVersion(useRetry: Boolean = true): Either<ClientError, GetVersionResponse> = executeWithService(useRetry) { service ->
        service.getVersion(GetVersionRequest.getDefaultInstance())
    }

    /**
     * Requests the daemon to shut down.
     *
     * @param reason Optional reason for shutdown
     * @param gracePeriodSeconds Grace period before forcing shutdown
     * @param saveState Whether to save state before shutdown
     * @param useRetry Whether to use retry logic for the operation (default false for shutdown)
     */
    suspend fun shutdown(
        reason: String = "CLI request",
        gracePeriodSeconds: Int = 5,
        saveState: Boolean = true,
        useRetry: Boolean = false,
    ): Either<ClientError, ShutdownResponse> {
        val request = ShutdownRequest.newBuilder()
            .setReason(reason)
            .setGracePeriodSeconds(gracePeriodSeconds)
            .setSaveState(saveState)
            .build()

        return executeWithService(useRetry) { service ->
            service.shutdown(request)
        }
    }

    /**
     * Executes a gRPC call with proper error handling and optional retry.
     */
    private suspend inline fun <T> executeWithService(
        useRetry: Boolean,
        crossinline operation: suspend (ControlServiceGrpcKt.ControlServiceCoroutineStub) -> T,
    ): Either<ClientError, T> {
        val executeOperation: suspend (Int) -> Either<ClientError, T> = { attemptNumber ->
            performServiceOperation(operation, attemptNumber)
        }

        return if (useRetry) {
            retryPolicy.execute("grpc-operation", executeOperation)
        } else {
            executeOperation(1)
        }
    }

    /**
     * Performs the actual gRPC operation.
     */
    private suspend inline fun <T> performServiceOperation(
        crossinline operation: suspend (ControlServiceGrpcKt.ControlServiceCoroutineStub) -> T,
        attemptNumber: Int,
    ): Either<ClientError, T> {
        val service = controlService
            ?: return ClientError.ConnectionError("Not connected to daemon").left()

        return try {
            if (attemptNumber > 1) {
                logger.debug("Retrying gRPC operation", mapOf("attempt" to attemptNumber))
            }
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

            logger.error("gRPC service error", mapOf("status" to e.status.code.toString(), "attempt" to attemptNumber), e)

            when (e.status.code) {
                Status.Code.DEADLINE_EXCEEDED -> ClientError.TimeoutError(errorMessage).left()
                else -> ClientError.ServiceError(errorMessage, e).left()
            }
        } catch (e: Exception) {
            logger.error("Unexpected error during gRPC call", mapOf("error" to e.javaClass.simpleName, "attempt" to attemptNumber), e)
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

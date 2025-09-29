package io.github.kamiazya.scopes.apps.daemon

import arrow.core.Either
import io.github.kamiazya.scopes.interfaces.daemon.GrpcServerFactory
import io.github.kamiazya.scopes.interfaces.daemon.GrpcServerWrapper
import io.github.kamiazya.scopes.platform.infrastructure.endpoint.EndpointFileUtils
import io.github.kamiazya.scopes.platform.observability.logging.ApplicationInfo
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Daemon application for background processing and IPC.
 *
 * Manages the lifecycle of the gRPC server and provides graceful shutdown capabilities.
 */
class DaemonApplication(private val logger: Logger, private val serverFactory: GrpcServerFactory, private val applicationInfo: ApplicationInfo) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val isRunning = AtomicBoolean(false)
    private var grpcServerWrapper: GrpcServerWrapper? = null
    private var shutdownMonitorJob: Job? = null

    /**
     * Error types for daemon operations.
     */
    sealed class DaemonError(message: String, cause: Throwable? = null) : Exception(message, cause) {
        class StartupError(message: String, cause: Throwable? = null) : DaemonError(message, cause)
        class ShutdownError(message: String, cause: Throwable? = null) : DaemonError(message, cause)
        class AlreadyRunning : DaemonError("Daemon is already running")
        class NotRunning : DaemonError("Daemon is not running")
    }

    /**
     * Starts the daemon with gRPC server.
     *
     * @param host The host to bind the gRPC server to (default: 127.0.0.1)
     * @param port The port to bind the gRPC server to (default: 0 for ephemeral)
     * @return Either a DaemonError or Unit on success
     */
    suspend fun start(host: String = "127.0.0.1", port: Int = 0): Either<DaemonError, Unit> {
        if (isRunning.compareAndSet(false, true)) {
            try {
                logger.info(
                    "Starting Scopes daemon",
                    mapOf(
                        "version" to applicationInfo.version,
                        "transport" to "tcp",
                    ),
                )

                // Create gRPC server wrapper using factory
                val serverWrapper = serverFactory.createServer(
                    applicationInfo = applicationInfo,
                    logger = logger,
                    host = host,
                    port = port,
                )

                val serverInfo = serverWrapper.server.start().fold(
                    { error ->
                        isRunning.set(false)
                        return Either.Left(DaemonError.StartupError("Failed to start gRPC server", error))
                    },
                    { it },
                )

                // Write endpoint file for CLI discovery
                val endpointFile = getDefaultEndpointFile()
                serverWrapper.server.writeEndpointFile(endpointFile).fold(
                    { error ->
                        logger.warn("Failed to write endpoint file", mapOf("error" to (error.message ?: "unspecified error")))
                        // Don't fail startup for this, just warn
                    },
                    { },
                )

                grpcServerWrapper = serverWrapper

                // Mark all services as healthy after successful startup
                serverWrapper.healthService.markAllServicesHealthy()

                // Monitor for shutdown requests with detailed signal
                shutdownMonitorJob = scope.launch {
                    serverWrapper.shutdownSignal
                        .onEach { signal ->
                            logger.info(
                                "Shutdown requested via gRPC, initiating graceful shutdown",
                                mapOf(
                                    "reason" to signal.reason,
                                    "gracePeriodSeconds" to signal.gracePeriodSeconds.toString(),
                                    "saveState" to signal.saveState.toString(),
                                ),
                            )
                            stop(gracePeriodSeconds = signal.gracePeriodSeconds, saveState = signal.saveState)
                        }
                        .collect()
                }

                logger.info(
                    "Scopes daemon started successfully",
                    mapOf(
                        "address" to serverInfo.address,
                        "endpointFile" to endpointFile.absolutePath,
                    ),
                )

                return Either.Right(Unit)
            } catch (e: Exception) {
                isRunning.set(false)
                logger.error("Failed to start daemon", mapOf("error" to e.javaClass.simpleName), e)
                return Either.Left(DaemonError.StartupError("Unexpected error during startup", e))
            }
        } else {
            return Either.Left(DaemonError.AlreadyRunning())
        }
    }

    /**
     * Stops the daemon gracefully.
     *
     * @param gracePeriodSeconds Time to wait for graceful shutdown
     * @param saveState Whether to save state before shutting down
     * @return Either a DaemonError or Unit on success
     */
    suspend fun stop(gracePeriodSeconds: Int = 5, saveState: Boolean = true): Either<DaemonError, Unit> {
        if (!isRunning.get()) {
            return Either.Left(DaemonError.NotRunning())
        }

        try {
            logger.info(
                "Stopping Scopes daemon",
                mapOf(
                    "gracePeriodSeconds" to gracePeriodSeconds.toString(),
                    "saveState" to saveState.toString(),
                ),
            )

            // Save state if requested
            if (saveState) {
                logger.info("Saving daemon state before shutdown")
                // TODO: Implement state persistence logic here
                // This could include saving current tasks, configurations, etc.
            }

            // Mark all services as unhealthy before shutdown
            grpcServerWrapper?.healthService?.markAllServicesUnhealthy()

            // Cancel shutdown monitor
            shutdownMonitorJob?.cancel()
            shutdownMonitorJob = null

            // Stop gRPC server
            grpcServerWrapper?.server?.stop(gracePeriodSeconds)?.fold(
                { error ->
                    logger.error("Error stopping gRPC server", mapOf("error" to error.javaClass.simpleName), error)
                    // Continue with shutdown even if server stop fails
                },
                { },
            )

            grpcServerWrapper = null

            // Clean up endpoint file
            val endpointFile = getDefaultEndpointFile()
            try {
                if (endpointFile.exists()) {
                    endpointFile.delete()
                    logger.debug("Endpoint file deleted", mapOf("file" to endpointFile.absolutePath))
                }
            } catch (e: Exception) {
                logger.warn("Failed to delete endpoint file", mapOf("error" to (e.message ?: "unspecified error")))
            }

            // Cancel the coroutine scope
            scope.cancel()

            isRunning.set(false)

            logger.info("Scopes daemon stopped successfully")
            return Either.Right(Unit)
        } catch (e: Exception) {
            logger.error("Error during daemon shutdown", mapOf("error" to e.javaClass.simpleName), e)
            return Either.Left(DaemonError.ShutdownError("Unexpected error during shutdown", e))
        }
    }

    /**
     * Checks if the daemon is currently running.
     */
    fun isRunning(): Boolean = isRunning.get()

    /**
     * Gets the current server address if running.
     */
    fun getServerAddress(): String? = grpcServerWrapper?.server?.getAddress()

    /**
     * Gets the default endpoint file path for this platform.
     */
    private fun getDefaultEndpointFile(): File = EndpointFileUtils.getDefaultEndpointFile()
}

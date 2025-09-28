package io.github.kamiazya.scopes.platform.infrastructure.grpc

import io.github.kamiazya.scopes.platform.infrastructure.grpc.interceptors.CorrelationIdClientInterceptor
import io.github.kamiazya.scopes.platform.infrastructure.grpc.interceptors.DeadlineClientInterceptor
import io.github.kamiazya.scopes.platform.infrastructure.grpc.interceptors.RequestLoggingClientInterceptor
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.grpc.ManagedChannel
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder
import io.grpc.netty.shaded.io.netty.channel.EventLoopGroup
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Data class to hold a managed channel and its associated EventLoopGroup for proper lifecycle management.
 */
data class ChannelWithEventLoop(
    val channel: ManagedChannel,
    val eventLoopGroup: EventLoopGroup
) {
    /**
     * Properly shutdown both the channel and EventLoopGroup.
     * This ensures the process can exit cleanly.
     */
    fun shutdown() {
        try {
            channel.shutdown()
            // Gracefully shut down the EventLoopGroup
            eventLoopGroup.shutdownGracefully()
        } catch (e: Exception) {
            // Best effort shutdown - don't throw exceptions during cleanup
        }
    }
}

/**
 * Common channel builder utilities for creating gRPC channels with consistent configuration.
 */
object ChannelBuilder {

    /**
     * Creates a managed channel with EventLoopGroup for proper lifecycle management.
     *
     * @param host The host to connect to
     * @param port The port to connect to
     * @param logger Logger instance for request logging
     * @param timeout Optional custom timeout (defaults to environment variable or 30 seconds)
     * @return ChannelWithEventLoop containing both channel and EventLoopGroup
     */
    fun createChannelWithEventLoop(host: String, port: Int, logger: Logger, timeout: Duration? = null): ChannelWithEventLoop {
        // Determine timeout: provided value > environment variable > default
        val effectiveTimeout = timeout ?: run {
            val envTimeoutMs = System.getenv("SCOPES_GRPC_TIMEOUT_MS")?.toLongOrNull()
            if (envTimeoutMs != null) {
                envTimeoutMs.milliseconds
            } else {
                30.seconds
            }
        }

        val maxMessageSizeMB = getMaxMessageSize() / (1024 * 1024)

        logger.debug(
            "Creating gRPC channel",
            mapOf(
                "host" to host,
                "port" to port.toString(),
                "timeout" to effectiveTimeout.inWholeMilliseconds.toString(),
                "maxMessageSizeMB" to maxMessageSizeMB.toString(),
            ),
        )

        // Create a dedicated event loop group for this channel
        val eventLoopGroup = io.grpc.netty.shaded.io.netty.channel.nio.NioEventLoopGroup(1)
        
        val channel = NettyChannelBuilder.forAddress(host, port)
            .usePlaintext() // TODO: Add TLS support in future iterations
            .channelType(io.grpc.netty.shaded.io.netty.channel.socket.nio.NioSocketChannel::class.java) // Force NIO transport for native image compatibility
            .eventLoopGroup(eventLoopGroup)
            // More conservative settings for native image compatibility
            .keepAliveTime(60, TimeUnit.SECONDS) // Increased from 30s
            .keepAliveTimeout(10, TimeUnit.SECONDS) // Increased from 5s
            .keepAliveWithoutCalls(false) // Disabled for stability
            .maxInboundMessageSize(getMaxMessageSize()) // Configure max message size
            // Add connection timeout settings
            .withOption(io.grpc.netty.shaded.io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
            .withOption(io.grpc.netty.shaded.io.netty.channel.ChannelOption.SO_KEEPALIVE, true)
            .intercept(
                DeadlineClientInterceptor(effectiveTimeout),
                CorrelationIdClientInterceptor(),
                RequestLoggingClientInterceptor(logger),
            )
            .build()
            
        return ChannelWithEventLoop(channel, eventLoopGroup)
    }

    /**
     * Creates a managed channel with standard configuration and interceptors.
     * WARNING: This method does not properly manage EventLoopGroup lifecycle.
     * Use createChannelWithEventLoop() for proper resource management.
     *
     * @param host The host to connect to
     * @param port The port to connect to
     * @param logger Logger instance for request logging
     * @param timeout Optional custom timeout (defaults to environment variable or 30 seconds)
     * @return Configured ManagedChannel
     */
    @Deprecated("Use createChannelWithEventLoop() for proper resource management", ReplaceWith("createChannelWithEventLoop(host, port, logger, timeout).channel"))
    fun createChannel(host: String, port: Int, logger: Logger, timeout: Duration? = null): ManagedChannel {
        return createChannelWithEventLoop(host, port, logger, timeout).channel
    }

    /**
     * Creates a channel from an address string (host:port format).
     *
     * @param address The address string to parse
     * @param logger Logger instance for request logging
     * @param timeout Optional custom timeout
     * @return Result containing either the ChannelWithEventLoop or an error
     */
    fun createChannelWithEventLoopFromAddress(address: String, logger: Logger, timeout: Duration? = null): Result<ChannelWithEventLoop> = runCatching {
        val parts = address.split(":")
        require(parts.size == 2) { "Invalid address format: expected 'host:port', got '$address'" }

        val host = parts[0].trim()
        require(host.isNotEmpty()) { "Host cannot be empty in address '$address'" }

        val port = parts[1].trim().toIntOrNull()
            ?: throw IllegalArgumentException("Invalid port in address '$address'")
        require(port in 1..65535) { "Port must be between 1 and 65535, got $port" }

        createChannelWithEventLoop(host, port, logger, timeout)
    }

    /**
     * Creates a channel from an address string (host:port format).
     * WARNING: This method does not properly manage EventLoopGroup lifecycle.
     * Use createChannelWithEventLoopFromAddress() for proper resource management.
     *
     * @param address The address string to parse
     * @param logger Logger instance for request logging
     * @param timeout Optional custom timeout
     * @return Result containing either the channel or an error
     */
    @Deprecated("Use createChannelWithEventLoopFromAddress() for proper resource management", ReplaceWith("createChannelWithEventLoopFromAddress(address, logger, timeout).map { it.channel }"))
    fun createChannelFromAddress(address: String, logger: Logger, timeout: Duration? = null): Result<ManagedChannel> = runCatching {
        val parts = address.split(":")
        require(parts.size == 2) { "Invalid address format: expected 'host:port', got '$address'" }

        val host = parts[0].trim()
        require(host.isNotEmpty()) { "Host cannot be empty in address '$address'" }

        val port = parts[1].trim().toIntOrNull()
            ?: throw IllegalArgumentException("Invalid port in address '$address'")
        require(port in 1..65535) { "Port must be between 1 and 65535, got $port" }

        createChannelWithEventLoop(host, port, logger, timeout).channel
    }

    // Default configuration values
    private const val DEFAULT_MAX_MESSAGE_SIZE_MB = 4

    /**
     * Gets the maximum inbound message size from environment or uses default.
     * @return Maximum message size in bytes
     */
    private fun getMaxMessageSize(): Int {
        val envValue = System.getenv("SCOPES_GRPC_MAX_MESSAGE_SIZE_MB")?.toIntOrNull()
        val maxSizeMB = envValue ?: DEFAULT_MAX_MESSAGE_SIZE_MB
        return maxSizeMB * 1024 * 1024 // Convert MB to bytes
    }
}

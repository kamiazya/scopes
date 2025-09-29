package io.github.kamiazya.scopes.e2e.framework

import io.github.kamiazya.scopes.rpc.v1beta.*
import io.grpc.ManagedChannel
import io.grpc.StatusRuntimeException
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder
import java.util.concurrent.TimeUnit

/**
 * Connects to an existing daemon for E2E testing.
 *
 * Following the philosophy that daemon management is delegated to the OS/package manager,
 * this controller only handles connection and communication, not lifecycle management.
 */
class DaemonController : AutoCloseable {

    private var channel: ManagedChannel? = null

    data class DaemonInfo(val pid: Long, val address: String, val port: Int, val version: String, val apiVersion: String)

    /**
     * Connect to a running daemon.
     * @param endpoint The daemon endpoint (e.g., "127.0.0.1:52345")
     * @return DaemonInfo if connection successful, null if daemon not running
     */
    suspend fun connect(endpoint: String? = null): DaemonInfo? {
        cleanup() // Ensure clean state

        val actualEndpoint = endpoint ?: discoverEndpoint()
        if (actualEndpoint == null) {
            println("No daemon endpoint found")
            return null
        }

        val (host, portStr) = actualEndpoint.split(":", limit = 2)
        val port = portStr.toIntOrNull() ?: throw IllegalArgumentException("Invalid port in endpoint: $actualEndpoint")

        // Create gRPC channel
        channel = NettyChannelBuilder
            .forAddress(host, port)
            .usePlaintext()
            .build()

        // Try to get daemon info
        return try {
            val controlStub = ControlServiceGrpcKt.ControlServiceCoroutineStub(channel!!)

            val pingResponse = controlStub.ping(PingRequest.getDefaultInstance())
            val versionResponse = controlStub.getVersion(GetVersionRequest.getDefaultInstance())

            DaemonInfo(
                pid = pingResponse.pid,
                address = host,
                port = port,
                version = versionResponse.appVersion,
                apiVersion = versionResponse.apiVersion,
            )
        } catch (e: StatusRuntimeException) {
            // Daemon not reachable
            cleanup()
            null
        }
    }

    /**
     * Disconnect from daemon.
     */
    fun disconnect() {
        cleanup()
    }

    override fun close() {
        disconnect()
    }

    fun isConnected(): Boolean = channel != null && !channel!!.isShutdown

    fun getControlStub(): ControlServiceGrpcKt.ControlServiceCoroutineStub {
        val ch = channel ?: throw IllegalStateException("Not connected to daemon")
        return ControlServiceGrpcKt.ControlServiceCoroutineStub(ch)
    }

    fun getGatewayStub(): TaskGatewayServiceGrpcKt.TaskGatewayServiceCoroutineStub {
        val ch = channel ?: throw IllegalStateException("Not connected to daemon")
        return TaskGatewayServiceGrpcKt.TaskGatewayServiceCoroutineStub(ch)
    }

    private fun discoverEndpoint(): String? {
        // Check environment variable first
        val envEndpoint = System.getenv("SCOPESD_ENDPOINT")
        if (!envEndpoint.isNullOrBlank()) {
            return envEndpoint
        }

        // Check endpoint file
        val endpointFile = PlatformUtils.getEndpointFilePath().toFile()
        if (endpointFile.exists()) {
            val content = endpointFile.readText()
            val props = parseEndpointFile(content)
            return props["addr"]
        }

        return null
    }

    private fun parseEndpointFile(content: String): Map<String, String> {
        val props = mutableMapOf<String, String>()

        for (line in content.lines()) {
            if (line.contains("=")) {
                val (key, value) = line.split("=", limit = 2)
                props[key.trim()] = value.trim()
            }
        }

        return props
    }

    private fun cleanup() {
        channel?.let { ch ->
            if (!ch.isShutdown) {
                ch.shutdownNow()
                try {
                    ch.awaitTermination(5, TimeUnit.SECONDS)
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
        channel = null
    }
}

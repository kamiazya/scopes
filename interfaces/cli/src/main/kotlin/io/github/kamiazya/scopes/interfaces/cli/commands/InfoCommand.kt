package io.github.kamiazya.scopes.interfaces.cli.commands

import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import io.github.kamiazya.scopes.interfaces.cli.core.ScopesCliktCommand
import io.github.kamiazya.scopes.platform.infrastructure.grpc.ChannelBuilder
import io.github.kamiazya.scopes.platform.infrastructure.grpc.EndpointResolver
import io.github.kamiazya.scopes.platform.observability.logging.ConsoleLogger
import io.github.kamiazya.scopes.rpc.v1beta.ControlServiceGrpcKt
import io.github.kamiazya.scopes.rpc.v1beta.GetVersionRequest
import io.github.kamiazya.scopes.rpc.v1beta.PingRequest
import io.grpc.ManagedChannel
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.seconds

/**
 * Displays information about the Scopes client and server.
 * Similar to 'docker info', shows both client-side and server-side information.
 */
class InfoCommand : ScopesCliktCommand(name = "info") {

    override fun help(context: com.github.ajalt.clikt.core.Context) = "Display system-wide information"
    private val json by option("--json", help = "Output in JSON format").flag()

    override fun run() = runBlocking {
        // Client information
        val clientInfo = getClientInfo()

        // Server information
        val serverInfo = getServerInfo()

        if (json) {
            outputJson(clientInfo, serverInfo)
        } else {
            outputHuman(clientInfo, serverInfo)
        }
    }

    private data class ClientInfo(
        val version: String,
        val platform: String,
        val configDir: String,
        val cliPath: String?,
        val gitRevision: String? = null,
        val buildTime: String? = null,
        val gradleVersion: String? = null,
        val javaVersion: String? = null,
    )

    private sealed class ServerInfo {
        data class Running(
            val version: String,
            val apiVersion: String,
            val pid: Long,
            val address: String,
            val uptime: Long, // seconds
            val startedAt: Instant,
            val gitRevision: String?,
            val buildPlatform: String?,
        ) : ServerInfo()

        object NotRunning : ServerInfo()
        data class Error(val message: String) : ServerInfo()
    }

    private fun getClientInfo(): ClientInfo {
        val version = javaClass.getResourceAsStream("/version.txt")?.bufferedReader()?.readText()?.trim() ?: "0.1.0"
        val platform = "${System.getProperty("os.name").lowercase()}/${System.getProperty("os.arch")}"
        val configDir = System.getenv("SCOPES_CONFIG_DIR") ?: "${System.getProperty("user.home")}/.scopes"
        val cliPath = System.getenv("SCOPES_CLI_PATH") // Optional, might be set by installer

        // Try to read build-info.properties for additional information
        var gitRevision: String? = null
        var buildTime: String? = null
        var gradleVersion: String? = null
        var javaVersion: String? = null

        javaClass.getResourceAsStream("/build-info.properties")?.use { stream ->
            val props = mutableMapOf<String, String>()
            stream.bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    if (line.isNotBlank() && !line.startsWith("#") && "=" in line) {
                        val (key, value) = line.split("=", limit = 2)
                        props[key.trim()] = value.trim()
                    }
                }
            }
            gitRevision = props["git.revision"]
            buildTime = props["build.time"]
            gradleVersion = props["gradle.version"]
            javaVersion = props["java.version"]
        }

        return ClientInfo(
            version = version,
            platform = platform,
            configDir = configDir,
            cliPath = cliPath,
            gitRevision = gitRevision,
            buildTime = buildTime,
            gradleVersion = gradleVersion,
            javaVersion = javaVersion,
        )
    }

    private suspend fun getServerInfo(): ServerInfo {
        val logger = ConsoleLogger()
        val endpointResolver = EndpointResolver(logger)
        val endpointResult = endpointResolver.resolve()

        return endpointResult.fold(
            { ServerInfo.NotRunning },
            { endpoint -> connectAndGetInfo(endpoint.address) },
        )
    }

    private suspend fun connectAndGetInfo(address: String): ServerInfo {
        var channel: ManagedChannel? = null
        return try {
            // Create logger for channel builder
            val logger = ConsoleLogger()

            // Create gRPC channel using common builder with shorter timeout for info command
            channel = ChannelBuilder.createChannelFromAddress(
                address = address,
                logger = logger,
                timeout = 5.seconds, // Short timeout for info command
            ).getOrThrow()

            val stub = ControlServiceGrpcKt.ControlServiceCoroutineStub(channel)

            // First, try to ping
            val pingResult = stub.ping(PingRequest.getDefaultInstance())

            // Then get version info
            val versionResult = stub.getVersion(GetVersionRequest.getDefaultInstance())

            ServerInfo.Running(
                version = versionResult.appVersion,
                apiVersion = versionResult.apiVersion,
                pid = pingResult.pid,
                address = address,
                uptime = pingResult.uptimeSeconds,
                startedAt = Clock.System.now() - pingResult.uptimeSeconds.seconds,
                gitRevision = if (versionResult.gitRevision.isNotBlank()) versionResult.gitRevision else null,
                buildPlatform = if (versionResult.buildPlatform.isNotBlank()) versionResult.buildPlatform else null,
            )
        } catch (e: Exception) {
            ServerInfo.Error(e.message ?: "Unknown error connecting to server")
        } finally {
            // Clean up channel
            channel?.shutdown()
            // Note: awaitTermination requires TimeUnit, so we skip it to avoid Java dependency
            // The channel will be cleaned up automatically when it goes out of scope
        }
    }

    private fun outputHuman(clientInfo: ClientInfo, serverInfo: ServerInfo) {
        echo("Client:")
        echo(" Version:     ${clientInfo.version}")
        clientInfo.gitRevision?.let {
            echo(" Git Revision: $it")
        }
        clientInfo.buildTime?.let {
            echo(" Build Time:  $it")
        }
        clientInfo.cliPath?.let {
            echo(" CLI Path:    $it")
        }
        echo(" Config Dir:  ${clientInfo.configDir}")
        echo(" Platform:    ${clientInfo.platform}")
        clientInfo.javaVersion?.let {
            echo(" Java Version: $it")
        }
        echo("")

        when (serverInfo) {
            is ServerInfo.Running -> {
                echo("Server:")
                echo(" Version:     ${serverInfo.version}")
                echo(" API Version: ${serverInfo.apiVersion}")
                echo(" Status:      Running")
                echo(" PID:         ${serverInfo.pid}")
                echo(" Address:     ${serverInfo.address}")
                echo(" Uptime:      ${formatUptime(serverInfo.uptime)}")
                echo(" Started:     ${serverInfo.startedAt}")
                serverInfo.gitRevision?.let {
                    echo(" Git Revision: $it")
                }
                serverInfo.buildPlatform?.let {
                    echo(" Platform:    $it")
                }
            }
            is ServerInfo.NotRunning -> {
                echo("Server:")
                echo(" Status:      Not running")
            }
            is ServerInfo.Error -> {
                echo("Server:")
                echo(" Status:      Error")
                echo(" Message:     ${serverInfo.message}")
            }
        }
    }

    private fun outputJson(clientInfo: ClientInfo, serverInfo: ServerInfo) {
        // Simple JSON output without external dependencies
        val clientJson = buildString {
            append("{")
            append("\"version\":\"${clientInfo.version}\",")
            append("\"platform\":\"${clientInfo.platform}\",")
            append("\"configDir\":\"${clientInfo.configDir}\"")
            clientInfo.cliPath?.let {
                append(",\"cliPath\":\"$it\"")
            }
            clientInfo.gitRevision?.let {
                append(",\"gitRevision\":\"$it\"")
            }
            clientInfo.buildTime?.let {
                append(",\"buildTime\":\"$it\"")
            }
            clientInfo.gradleVersion?.let {
                append(",\"gradleVersion\":\"$it\"")
            }
            clientInfo.javaVersion?.let {
                append(",\"javaVersion\":\"$it\"")
            }
            append("}")
        }

        val serverJson = when (serverInfo) {
            is ServerInfo.Running -> buildString {
                append("{")
                append("\"status\":\"running\",")
                append("\"version\":\"${serverInfo.version}\",")
                append("\"apiVersion\":\"${serverInfo.apiVersion}\",")
                append("\"pid\":${serverInfo.pid},")
                append("\"address\":\"${serverInfo.address}\",")
                append("\"uptime\":${serverInfo.uptime},")
                append("\"startedAt\":\"${serverInfo.startedAt}\"")
                serverInfo.gitRevision?.let {
                    append(",\"gitRevision\":\"$it\"")
                }
                serverInfo.buildPlatform?.let {
                    append(",\"platform\":\"$it\"")
                }
                append("}")
            }
            is ServerInfo.NotRunning -> "{\"status\":\"not_running\"}"
            is ServerInfo.Error -> "{\"status\":\"error\",\"message\":\"${serverInfo.message}\"}"
        }

        echo("{\"client\":$clientJson,\"server\":$serverJson}")
    }

    private fun formatUptime(seconds: Long): String {
        val days = seconds / 86400
        val hours = (seconds % 86400) / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60

        return buildString {
            if (days > 0) append("${days}d ")
            if (hours > 0 || days > 0) append("${hours}h ")
            if (minutes > 0 || hours > 0 || days > 0) append("${minutes}m ")
            append("${secs}s")
        }.trim()
    }
}

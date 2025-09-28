package io.github.kamiazya.scopes.platform.infrastructure.grpc

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.platform.infrastructure.endpoint.EndpointFileUtils
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import java.io.File
import java.io.FileNotFoundException

/**
 * Resolves daemon endpoint information for CLI to connect to the gRPC server.
 */
class EndpointResolver(private val logger: Logger) {
    /**
     * Error types for endpoint resolution.
     */
    sealed class EndpointError(message: String, cause: Throwable? = null) : Exception(message, cause) {
        class NotFound(message: String) : EndpointError(message)
        class InvalidFormat(message: String, cause: Throwable? = null) : EndpointError(message, cause)
        class IOError(message: String, cause: Throwable? = null) : EndpointError(message, cause)
    }

    /**
     * Daemon endpoint information.
     */
    data class EndpointInfo(
        val address: String,
        val host: String,
        val port: Int,
        val transport: String = "tcp",
        val pid: Long? = null,
        val started: Instant? = null,
    )

    /**
     * Resolves the daemon endpoint using environment variables and/or endpoint files.
     *
     * Resolution order:
     * 1. SCOPESD_ENDPOINT environment variable
     * 2. Platform-specific endpoint file
     *
     * @return Either an EndpointError or EndpointInfo
     */
    suspend fun resolve(): Either<EndpointError, EndpointInfo> {
        // Try environment variable first
        val envEndpoint = System.getenv("SCOPESD_ENDPOINT")
        if (!envEndpoint.isNullOrBlank()) {
            logger.debug("Using daemon endpoint from environment variable", mapOf("endpoint" to envEndpoint))
            return parseAddress(envEndpoint).map { (host, port) ->
                EndpointInfo(
                    address = envEndpoint,
                    host = host,
                    port = port,
                    transport = "tcp",
                )
            }
        }

        // Try endpoint file
        return resolveFromFile()
    }

    /**
     * Resolves endpoint information from the platform-specific endpoint file.
     */
    private suspend fun resolveFromFile(): Either<EndpointError, EndpointInfo> = withContext(Dispatchers.IO) {
        val endpointFile = EndpointFileUtils.getDefaultEndpointFile()

        try {
            if (!endpointFile.exists()) {
                return@withContext EndpointError.NotFound(
                    "Daemon endpoint file not found: ${endpointFile.absolutePath}. " +
                        "Is the daemon running? You can also set SCOPESD_ENDPOINT environment variable.",
                ).left()
            }

            logger.debug("Reading daemon endpoint from file", mapOf("file" to endpointFile.absolutePath))

            val properties = mutableMapOf<String, String>()
            endpointFile.readLines().forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                    val parts = trimmed.split("=", limit = 2)
                    if (parts.size == 2) {
                        properties[parts[0].trim()] = parts[1].trim()
                    }
                }
            }

            val address = properties["addr"]
                ?: return@withContext EndpointError.InvalidFormat("Missing 'addr' in endpoint file").left()

            val addressResult = parseAddress(address)
            val (host, port) = when (addressResult) {
                is Either.Left -> return@withContext addressResult.value.left()
                is Either.Right -> addressResult.value
            }

            val pid = properties["pid"]?.toLongOrNull()

            // Check if the process is still alive
            if (pid != null && !isProcessAlive(pid)) {
                logger.warn("Daemon process (pid=$pid) is not running, removing stale endpoint file", emptyMap())
                try {
                    endpointFile.delete()
                } catch (e: Exception) {
                    logger.error("Failed to delete stale endpoint file", mapOf("error" to (e.message ?: "Unknown error")), e)
                }
                return@withContext EndpointError.NotFound(
                    "Daemon process (pid=$pid) is not running. Endpoint file has been removed.",
                ).left()
            }

            EndpointInfo(
                address = address,
                host = host,
                port = port,
                transport = properties["transport"] ?: "tcp",
                pid = pid,
                started = properties["started"]?.toLongOrNull()?.let { Instant.fromEpochMilliseconds(it) },
            ).right()
        } catch (e: FileNotFoundException) {
            EndpointError.NotFound("Daemon endpoint file not found: ${endpointFile.absolutePath}").left()
        } catch (e: Exception) {
            EndpointError.IOError("Failed to read endpoint file: ${e.message}", e).left()
        }
    }

    /**
     * Parses an address string in the format "host:port" or "[ipv6]:port".
     *
     * Supports:
     * - IPv4 addresses: "127.0.0.1:8080"
     * - IPv6 addresses: "[::1]:8080" or "[2001:db8::1]:8080"
     * - Hostnames: "localhost:8080" or "example.com:8080"
     * - Spaces around addresses: " [::1] : 8080 " or " localhost : 8080 "
     *
     * Future: Can be extended to support Unix Domain Sockets (unix:///path/to/socket)
     *         or Windows Named Pipes (npipe:////./pipe/name)
     */
    private fun parseAddress(address: String): Either<EndpointError, Pair<String, Int>> {
        val trimmedAddress = address.trim()

        // Try to match IPv6 format with brackets: [host]:port (with optional spaces)
        val ipv6Pattern = Regex("""^\s*\[(.+)]\s*:\s*(\d+)\s*$""")
        val ipv6Match = ipv6Pattern.matchEntire(trimmedAddress)

        if (ipv6Match != null) {
            // IPv6 address with brackets
            val host = ipv6Match.groupValues[1].trim()
            val portStr = ipv6Match.groupValues[2].trim()

            val port = portStr.toIntOrNull()
            if (port == null || port <= 0 || port > 65535) {
                return EndpointError.InvalidFormat("Invalid port in address: '$address'. Port must be between 1 and 65535").left()
            }

            return (host to port).right()
        }

        // Try to match standard format: host:port
        // Find the last colon to split host and port
        val lastColonIndex = trimmedAddress.lastIndexOf(':')
        if (lastColonIndex == -1) {
            return EndpointError.InvalidFormat("Invalid address format: '$address'. Expected 'host:port' or '[ipv6]:port'").left()
        }

        val host = trimmedAddress.substring(0, lastColonIndex).trim()
        val portStr = trimmedAddress.substring(lastColonIndex + 1).trim()

        if (host.isEmpty()) {
            return EndpointError.InvalidFormat("Empty host in address: '$address'").left()
        }

        // Check if host contains colons (might be unbracketed IPv6)
        if (host.contains(':')) {
            return EndpointError.InvalidFormat(
                "Invalid address format: '$address'. IPv6 addresses must be enclosed in brackets: '[$host]:$portStr'",
            ).left()
        }

        val port = portStr.toIntOrNull()
        if (port == null || port <= 0 || port > 65535) {
            return EndpointError.InvalidFormat("Invalid port in address: '$address'. Port must be between 1 and 65535").left()
        }

        return (host to port).right()
    }

    /**
     * Gets the endpoint file that would be used for a given platform (for testing).
     */
    fun getEndpointFile(): File = EndpointFileUtils.getDefaultEndpointFile()

    /**
     * Checks if a daemon is likely running by checking for the endpoint file.
     */
    suspend fun isDaemonRunning(): Boolean = withContext(Dispatchers.IO) {
        try {
            resolve().isRight()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Checks if a process with the given PID is still alive.
     * This implementation is platform-specific.
     */
    private fun isProcessAlive(pid: Long): Boolean {
        val osName = System.getProperty("os.name")?.lowercase() ?: ""

        return try {
            when {
                osName.contains("windows") -> {
                    // On Windows, use tasklist command
                    val process = ProcessBuilder("tasklist", "/FI", "PID eq $pid", "/FO", "CSV", "/NH")
                        .redirectErrorStream(true)
                        .start()
                    val output = process.inputStream.bufferedReader().readText()
                    process.waitFor()
                    output.contains("\"$pid\"")
                }
                else -> {
                    // On Unix-like systems (Linux, macOS), use kill -0
                    // kill -0 doesn't actually kill the process, just checks if it exists
                    val process = ProcessBuilder("kill", "-0", pid.toString())
                        .redirectErrorStream(true)
                        .start()
                    val exitCode = process.waitFor()
                    exitCode == 0
                }
            }
        } catch (e: Exception) {
            // If we can't determine process status, assume it's not alive
            logger.debug("Failed to check process status for pid=$pid", mapOf("error" to (e.message ?: "Unknown error")))
            false
        }
    }
}

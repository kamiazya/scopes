package io.github.kamiazya.scopes.interfaces.daemon

import com.google.protobuf.Timestamp
import io.github.kamiazya.scopes.platform.observability.logging.ApplicationInfo
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.rpc.v1beta.ControlServiceGrpcKt
import io.github.kamiazya.scopes.rpc.v1beta.GetVersionRequest
import io.github.kamiazya.scopes.rpc.v1beta.GetVersionResponse
import io.github.kamiazya.scopes.rpc.v1beta.PingRequest
import io.github.kamiazya.scopes.rpc.v1beta.PingResponse
import io.github.kamiazya.scopes.rpc.v1beta.ShutdownRequest
import io.github.kamiazya.scopes.rpc.v1beta.ShutdownResponse
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.lang.management.ManagementFactory
import java.util.Properties

/**
 * Implementation of the gRPC ControlService.
 *
 * Provides basic daemon control operations including ping, version info, and shutdown.
 */
class ControlServiceImpl(private val applicationInfo: ApplicationInfo, private val logger: Logger) :
    ControlServiceGrpcKt.ControlServiceCoroutineImplBase() {

    private val startTime: Instant = Clock.System.now()
    private val _shutdownSignal = MutableSharedFlow<ShutdownSignal>(replay = 1)
    val shutdownSignal: SharedFlow<ShutdownSignal> = _shutdownSignal

    // Build info loaded from resources
    private val buildInfo: BuildInfo = loadBuildInfo()

    override suspend fun ping(request: PingRequest): PingResponse {
        logger.debug("Received ping request")

        val currentTime = Clock.System.now()
        val uptimeSeconds = (currentTime - startTime).inWholeSeconds

        return PingResponse.newBuilder()
            .setServerTime(currentTime.toProtobufTimestamp())
            .setPid(getCurrentProcessId())
            .setUptimeSeconds(uptimeSeconds)
            .build()
    }

    override suspend fun getVersion(request: GetVersionRequest): GetVersionResponse {
        logger.debug("Received version request")

        // Use gitRevision from applicationInfo if available, otherwise fall back to buildInfo
        val gitRevision = applicationInfo.gitRevision ?: buildInfo.gitRevision

        return GetVersionResponse.newBuilder()
            .setAppVersion(applicationInfo.version)
            .setApiVersion("v1beta")
            .setGitRevision(gitRevision)
            .setBuildTime(buildInfo.buildTime.toProtobufTimestamp())
            .setBuildPlatform(getBuildPlatform())
            .build()
    }

    override suspend fun shutdown(request: ShutdownRequest): ShutdownResponse {
        logger.info(
            "Received shutdown request",
            mapOf(
                "reason" to request.reason,
                "gracePeriodSeconds" to request.gracePeriodSeconds.toString(),
                "saveState" to request.saveState.toString(),
            ),
        )

        // Validate shutdown request
        if (request.gracePeriodSeconds < 0) {
            return ShutdownResponse.newBuilder()
                .setAccepted(false)
                .setMessage("Invalid grace period: must be non-negative")
                .setEstimatedSeconds(0)
                .build()
        }

        // Emit shutdown signal with request details
        val signal = ShutdownSignal(
            reason = request.reason,
            gracePeriodSeconds = request.gracePeriodSeconds,
            saveState = request.saveState,
        )
        _shutdownSignal.tryEmit(signal)

        // Return immediately - actual shutdown will be handled by monitoring Flow in DaemonApplication
        val estimatedSeconds = maxOf(request.gracePeriodSeconds, 1)

        return ShutdownResponse.newBuilder()
            .setAccepted(true)
            .setMessage("Shutdown initiated successfully")
            .setEstimatedSeconds(estimatedSeconds)
            .build()
    }

    private fun getCurrentProcessId(): Long = try {
        ManagementFactory.getRuntimeMXBean().pid
    } catch (e: Exception) {
        logger.warn("Could not determine process ID", mapOf("error" to e.message as Any))
        -1L
    }

    private fun getBuildPlatform(): String {
        val osName = System.getProperty("os.name")?.lowercase() ?: "unspecified"
        val osArch = System.getProperty("os.arch")?.lowercase() ?: "unspecified"

        val normalizedOs = when {
            osName.contains("linux") -> "linux"
            osName.contains("mac") || osName.contains("darwin") -> "darwin"
            osName.contains("windows") -> "windows"
            else -> osName
        }

        val normalizedArch = when (osArch) {
            "x86_64", "amd64" -> "amd64"
            "aarch64", "arm64" -> "arm64"
            else -> osArch
        }

        return "$normalizedOs/$normalizedArch"
    }

    private fun Instant.toProtobufTimestamp(): Timestamp = Timestamp.newBuilder()
        .setSeconds(this.epochSeconds)
        .setNanos((this.nanosecondsOfSecond % 1_000_000_000).toInt())
        .build()

    private fun loadBuildInfo(): BuildInfo {
        var version = "unknown"
        var buildTime = Clock.System.now()
        var gitRevision = "development"

        // Try to read build-info.properties
        javaClass.getResourceAsStream("/build-info.properties")?.use { stream ->
            val props = Properties()
            props.load(stream)

            props.getProperty("version")?.let { version = it }
            props.getProperty("build.time")?.let { timeStr ->
                try {
                    // Parse epoch millis from build.gradle.kts
                    val epochMillis = timeStr.toLongOrNull()
                    if (epochMillis != null) {
                        buildTime = Instant.fromEpochMilliseconds(epochMillis)
                    } else {
                        // Try to parse as ISO string for backward compatibility
                        buildTime = Instant.parse(timeStr)
                    }
                } catch (e: Exception) {
                    logger.warn("Failed to parse build time", mapOf("buildTime" to timeStr as Any, "error" to e.message as Any))
                }
            }
            props.getProperty("git.revision")?.let { gitRevision = it }
        }

        // Also try version.txt as fallback for version
        if (version == "unknown") {
            javaClass.getResourceAsStream("/version.txt")?.use { stream ->
                version = stream.bufferedReader().readLine()?.trim() ?: "unknown"
            }
        }

        return BuildInfo(version = version, buildTime = buildTime, gitRevision = gitRevision)
    }

    private data class BuildInfo(val version: String, val buildTime: Instant, val gitRevision: String)
}

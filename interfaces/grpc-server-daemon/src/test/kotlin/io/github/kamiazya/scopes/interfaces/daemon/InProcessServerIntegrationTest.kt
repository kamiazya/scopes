package io.github.kamiazya.scopes.interfaces.daemon

import io.github.kamiazya.scopes.contracts.scopemanagement.commands.CreateScopeCommand
import io.github.kamiazya.scopes.interfaces.daemon.grpc.services.TaskGatewayServiceImpl
import io.github.kamiazya.scopes.platform.observability.logging.ConsoleLogger
import io.github.kamiazya.scopes.rpc.v1beta.ControlServiceGrpcKt
import io.github.kamiazya.scopes.rpc.v1beta.Envelope
import io.github.kamiazya.scopes.rpc.v1beta.GetVersionRequest
import io.github.kamiazya.scopes.rpc.v1beta.PingRequest
import io.github.kamiazya.scopes.rpc.v1beta.ShutdownRequest
import io.github.kamiazya.scopes.rpc.v1beta.TaskGatewayServiceGrpcKt
import io.grpc.ManagedChannel
import io.grpc.Server
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

/**
 * Integration tests using gRPC in-process server for daemon services.
 * This test verifies the integration between:
 * - gRPC server infrastructure
 * - Control service implementation
 * - TaskGateway service implementation (when available)
 * - ShutdownSignal propagation
 */
class InProcessServerIntegrationTest {

    private lateinit var serverName: String
    private lateinit var server: Server
    private lateinit var channel: io.grpc.ManagedChannel
    private lateinit var controlService: ControlServiceImpl
    private lateinit var taskGatewayService: TaskGatewayServiceImpl
    private val logger = ConsoleLogger()

    @BeforeEach
    fun setup() {
        // Create unique server name for each test
        serverName = "test-server-${System.currentTimeMillis()}"

        // Create services
        controlService = ControlServiceImpl(
            applicationInfo = io.github.kamiazya.scopes.platform.observability.logging.ApplicationInfo(
                name = "test-daemon",
                version = "1.0.0-test",
                type = io.github.kamiazya.scopes.platform.observability.logging.ApplicationType.DAEMON,
            ),
            logger = logger,
        )

        // Create TaskGateway service with null ports (will fail if used)
        taskGatewayService = TaskGatewayServiceImpl(
            scopeManagementCommandPort = null,
            scopeManagementQueryPort = null,
            contextViewCommandPort = null,
            contextViewQueryPort = null,
            json = Json { ignoreUnknownKeys = true },
            logger = logger,
        )

        // Build in-process server
        server = InProcessServerBuilder
            .forName(serverName)
            .directExecutor() // Use direct executor for tests
            .addService(controlService)
            .addService(taskGatewayService)
            .build()
            .start()

        // Create channel for client
        channel = InProcessChannelBuilder
            .forName(serverName)
            .directExecutor()
            .build()
    }

    @AfterEach
    fun tearDown() {
        channel.shutdown()
        server.shutdown()
        server.awaitTermination()
    }

    @Test
    fun `ping should return server information`() = runBlocking {
        // Create stub
        val stub = ControlServiceGrpcKt.ControlServiceCoroutineStub(channel)

        // Send ping request
        val response = stub.ping(PingRequest.newBuilder().build())

        // Verify response
        response.serverTime shouldNotBe null
        response.pid shouldNotBe -1
        response.uptimeSeconds shouldNotBe null
    }

    @Test
    fun `getVersion should return version information`() = runBlocking {
        // Create stub
        val stub = ControlServiceGrpcKt.ControlServiceCoroutineStub(channel)

        // Send version request
        val response = stub.getVersion(GetVersionRequest.newBuilder().build())

        // Verify response
        response.appVersion shouldBe "1.0.0-test"
        response.apiVersion shouldBe "v1beta"
        response.gitRevision shouldBe "development"
        response.buildPlatform shouldNotBe null
    }

    @Test
    fun `shutdown should emit ShutdownSignal with proper values`() = runBlocking {
        // Create stub
        val stub = ControlServiceGrpcKt.ControlServiceCoroutineStub(channel)

        // Variables to capture signal
        var capturedSignal: ShutdownSignal? = null

        // Subscribe to shutdown signal
        val job = kotlinx.coroutines.GlobalScope.launch {
            controlService.shutdownSignal.collect { signal ->
                capturedSignal = signal
            }
        }

        // Send shutdown request
        val shutdownResponse = stub.shutdown(
            ShutdownRequest.newBuilder()
                .setReason("Test shutdown")
                .setGracePeriodSeconds(30)
                .setSaveState(true)
                .build(),
        )

        // Verify response
        shutdownResponse.accepted shouldBe true
        shutdownResponse.message shouldBe "Shutdown initiated successfully"
        shutdownResponse.estimatedSeconds shouldBe 30

        // Give time for signal to propagate
        kotlinx.coroutines.delay(100)

        // Verify signal was emitted
        capturedSignal shouldNotBe null
        capturedSignal?.reason shouldBe "Test shutdown"
        capturedSignal?.gracePeriodSeconds shouldBe 30
        capturedSignal?.saveState shouldBe true

        // Cancel collection job
        job.cancel()
    }

    @Test
    fun `shutdown with invalid grace period should be rejected`() = runBlocking {
        // Create stub
        val stub = ControlServiceGrpcKt.ControlServiceCoroutineStub(channel)

        // Send shutdown request with negative grace period
        val shutdownResponse = stub.shutdown(
            ShutdownRequest.newBuilder()
                .setReason("Invalid test")
                .setGracePeriodSeconds(-5)
                .setSaveState(false)
                .build(),
        )

        // Verify response
        shutdownResponse.accepted shouldBe false
        shutdownResponse.message shouldBe "Invalid grace period: must be non-negative"
        shutdownResponse.estimatedSeconds shouldBe 0
    }

    @Test
    fun `TaskGateway should handle executeCommand for create scope`() = runBlocking {
        // Create stub
        val stub = TaskGatewayServiceGrpcKt.TaskGatewayServiceCoroutineStub(channel)

        // Create a command
        val command = CreateScopeCommand.WithAutoAlias(
            title = "Test Scope",
            description = "Test Description",
        )

        // Create envelope
        val envelope = Envelope.newBuilder()
            .setKind("command.scope.create")
            .setPayload(
                com.google.protobuf.ByteString.copyFromUtf8(
                    Json.encodeToString(serializer<CreateScopeCommand>(), command),
                ),
            )
            .build()

        try {
            // Execute command - should fail because ports are null
            val response = withTimeout(2.seconds) {
                stub.executeCommand(envelope)
            }

            // Should not reach here
            assert(false) { "Expected exception due to null ports" }
        } catch (e: Exception) {
            // Expected to fail because scopeManagementCommandPort is null
            // This is OK for this test - we're testing the infrastructure, not the actual command execution
            e.message?.contains("not configured") shouldBe true
        }
    }

    @Test
    fun `multiple services should be accessible on same server`() = runBlocking {
        // Create stubs for both services
        val controlStub = ControlServiceGrpcKt.ControlServiceCoroutineStub(channel)
        val gatewayStub = TaskGatewayServiceGrpcKt.TaskGatewayServiceCoroutineStub(channel)

        // Test control service
        val pingResponse = controlStub.ping(PingRequest.newBuilder().build())
        pingResponse shouldNotBe null

        // Test gateway service (will fail but proves it's reachable)
        try {
            val envelope = Envelope.newBuilder()
                .setKind("query.scope.get")
                .setPayload(com.google.protobuf.ByteString.copyFromUtf8("{}"))
                .build()

            gatewayStub.query(envelope)
        } catch (e: Exception) {
            // Expected to fail
            e.message?.contains("not configured") shouldBe true
        }
    }
}

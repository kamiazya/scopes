package io.github.kamiazya.scopes.interfaces.daemon.health

import io.github.kamiazya.scopes.interfaces.daemon.ControlServiceImpl
import io.github.kamiazya.scopes.interfaces.daemon.grpc.services.TaskGatewayServiceImpl
import io.github.kamiazya.scopes.platform.observability.logging.ApplicationInfo
import io.github.kamiazya.scopes.platform.observability.logging.ApplicationType
import io.github.kamiazya.scopes.platform.observability.logging.ConsoleLogger
import io.grpc.StatusRuntimeException
import io.grpc.health.v1.HealthCheckRequest
import io.grpc.health.v1.HealthCheckResponse
import io.grpc.health.v1.HealthGrpc
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class HealthServiceIntegrationTest :
    DescribeSpec({
        describe("HealthService Integration") {
            val serverName = "health-test-server"
            val logger = ConsoleLogger()

            it("should provide health status for all registered services") {
                // Create gRPC server with health service
                val applicationInfo = ApplicationInfo(
                    name = "test-daemon",
                    version = "1.0.0",
                    type = ApplicationType.DAEMON,
                )

                val controlService = ControlServiceImpl(applicationInfo, logger)
                val healthService = HealthService(logger)
                val taskGatewayService = TaskGatewayServiceImpl(
                    scopeManagementCommandPort = null,
                    scopeManagementQueryPort = null,
                    contextViewCommandPort = null,
                    contextViewQueryPort = null,
                    json = Json { ignoreUnknownKeys = true },
                    logger = logger,
                )

                // Build in-process server
                val server = InProcessServerBuilder
                    .forName(serverName)
                    .directExecutor()
                    .addService(controlService)
                    .addService(healthService.getHealthService())
                    .addService(taskGatewayService)
                    .build()
                    .start()

                // Create in-process channel
                val channel = InProcessChannelBuilder
                    .forName(serverName)
                    .directExecutor()
                    .build()

                val healthStub = HealthGrpc.newBlockingStub(channel)

                try {
                    // Check overall health
                    val overallHealth = healthStub.check(
                        HealthCheckRequest.newBuilder()
                            .setService("")
                            .build(),
                    )
                    overallHealth.status shouldBe HealthCheckResponse.ServingStatus.SERVING

                    // Check ControlService health
                    val controlHealth = healthStub.check(
                        HealthCheckRequest.newBuilder()
                            .setService("scopes.daemon.v1beta.ControlService")
                            .build(),
                    )
                    controlHealth.status shouldBe HealthCheckResponse.ServingStatus.SERVING

                    // Check TaskGatewayService health
                    val gatewayHealth = healthStub.check(
                        HealthCheckRequest.newBuilder()
                            .setService("scopes.daemon.v1beta.TaskGatewayService")
                            .build(),
                    )
                    gatewayHealth.status shouldBe HealthCheckResponse.ServingStatus.SERVING

                    // Check unknown service (should throw NOT_FOUND)
                    shouldThrow<StatusRuntimeException> {
                        healthStub.check(
                            HealthCheckRequest.newBuilder()
                                .setService("unknown.service")
                                .build(),
                        )
                    }
                } finally {
                    channel.shutdown()
                    server.shutdown()
                    server.awaitTermination()
                }
            }

            it("should update health status during shutdown") {
                // Create health service directly
                val applicationInfo = ApplicationInfo(
                    name = "test-daemon",
                    version = "1.0.0",
                    type = ApplicationType.DAEMON,
                )

                val healthService = HealthService(logger)

                // Build in-process server
                val server = InProcessServerBuilder
                    .forName(serverName + "-shutdown")
                    .directExecutor()
                    .addService(healthService.getHealthService())
                    .build()
                    .start()

                val channel = InProcessChannelBuilder
                    .forName(serverName + "-shutdown")
                    .directExecutor()
                    .build()

                val healthStub = HealthGrpc.newBlockingStub(channel)

                try {
                    // Initially healthy
                    val initialHealth = healthStub.check(
                        HealthCheckRequest.newBuilder()
                            .setService("")
                            .build(),
                    )
                    initialHealth.status shouldBe HealthCheckResponse.ServingStatus.SERVING

                    // Mark as unhealthy (simulating shutdown)
                    healthService.markAllServicesUnhealthy()

                    // Should now be NOT_SERVING
                    val shutdownHealth = healthStub.check(
                        HealthCheckRequest.newBuilder()
                            .setService("")
                            .build(),
                    )
                    shutdownHealth.status shouldBe HealthCheckResponse.ServingStatus.NOT_SERVING

                    // Check individual service is also NOT_SERVING
                    val serviceHealth = healthStub.check(
                        HealthCheckRequest.newBuilder()
                            .setService("scopes.daemon.v1beta.ControlService")
                            .build(),
                    )
                    serviceHealth.status shouldBe HealthCheckResponse.ServingStatus.NOT_SERVING
                } finally {
                    channel.shutdown()
                    server.shutdown()
                    server.awaitTermination()
                }
            }
        }
    })

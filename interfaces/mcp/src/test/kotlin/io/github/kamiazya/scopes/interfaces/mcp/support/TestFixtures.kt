package io.github.kamiazya.scopes.interfaces.mcp.support

import io.github.kamiazya.scopes.contracts.scopemanagement.ScopeManagementCommandPort
import io.github.kamiazya.scopes.contracts.scopemanagement.ScopeManagementQueryPort
import io.github.kamiazya.scopes.interfaces.mcp.tools.Ports
import io.github.kamiazya.scopes.interfaces.mcp.tools.Services
import io.github.kamiazya.scopes.interfaces.mcp.tools.ToolContext
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject

/** Test helper functions to reduce duplication across tests. */
object TestFixtures {
    fun mockLogger(relaxed: Boolean = true): Logger = mockk<Logger>(relaxed = relaxed).apply {
        // Enable debug by default to simplify verification in tests that expect logging
        every { isEnabledFor(io.github.kamiazya.scopes.platform.observability.logging.LogLevel.DEBUG) } returns true
        every { isEnabledFor(io.github.kamiazya.scopes.platform.observability.logging.LogLevel.INFO) } returns true
        every { isEnabledFor(io.github.kamiazya.scopes.platform.observability.logging.LogLevel.WARN) } returns true
        every { isEnabledFor(io.github.kamiazya.scopes.platform.observability.logging.LogLevel.ERROR) } returns true
        // withName/withContext are not required by current tests
    }

    fun ports(query: ScopeManagementQueryPort = mockk(), command: ScopeManagementCommandPort = mockk()): Ports = Ports(query = query, command = command)

    fun services(logger: Logger = mockLogger()): Services = Services(
        errors = createErrorMapper(logger),
        idempotency = createIdempotencyService(createArgumentCodec()),
        codec = createArgumentCodec(),
        logger = logger,
    )

    fun ctx(args: JsonObject = buildJsonObject { }, ports: Ports = ports(), services: Services = services()): ToolContext =
        ToolContext(args = args, ports = ports, services = services)
}

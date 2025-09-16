package io.github.kamiazya.scopes.interfaces.mcp.completions

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.scopemanagement.ScopeManagementCommandPort
import io.github.kamiazya.scopes.contracts.scopemanagement.ScopeManagementQueryPort
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.*
import io.github.kamiazya.scopes.contracts.scopemanagement.results.AliasListResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeListResult
import io.github.kamiazya.scopes.interfaces.mcp.support.createArgumentCodec
import io.github.kamiazya.scopes.interfaces.mcp.support.createErrorMapper
import io.github.kamiazya.scopes.interfaces.mcp.support.createIdempotencyService
import io.github.kamiazya.scopes.interfaces.mcp.tools.Ports
import io.github.kamiazya.scopes.interfaces.mcp.tools.Services
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.modelcontextprotocol.kotlin.sdk.CompleteRequest
import io.modelcontextprotocol.kotlin.sdk.CompleteResult
import io.modelcontextprotocol.kotlin.sdk.Method
import io.modelcontextprotocol.kotlin.sdk.server.Server

class CompletionRegistrarWiringTest :
    StringSpec({

        "registers completion handler on server" {
            val server = mockk<Server>(relaxed = true)

            // Capture the handler to ensure it is registered
            val fnSlot = slot<suspend (CompleteRequest, Any?) -> CompleteResult>()
            every { server.setRequestHandler<CompleteRequest>(Method.Defined.CompletionComplete, capture(fnSlot)) } returns Unit

            val registrar = CompletionRegistrar(contextFactory = { createSampleContext() })

            registrar.register(server)

            // Verify registration executed exactly once for completion/complete
            verify(exactly = 1) {
                server.setRequestHandler<CompleteRequest>(
                    Method.Defined.CompletionComplete,
                    any<suspend (CompleteRequest, Any?) -> CompleteResult>(),
                )
            }

            // And ensure a handler was captured
            fnSlot.isCaptured shouldBe true
        }
    }) {
    companion object {
        private fun createSampleContext(): Pair<Ports, Services> {
            val query = mockk<ScopeManagementQueryPort> {
                coEvery { getScope(any()) } returns Either.Right(null)
                coEvery { getChildren(any()) } returns Either.Right(
                    ScopeListResult(scopes = emptyList(), totalCount = 0, offset = 0, limit = 0),
                )
                coEvery { getRootScopes(any()) } returns Either.Right(
                    ScopeListResult(scopes = emptyList(), totalCount = 0, offset = 0, limit = 0),
                )
                coEvery { getScopeByAlias(any()) } returns Either.Left(
                    ScopeContractError.BusinessError.AliasNotFound(alias = "test"),
                )
                coEvery { listAliases(any()) } returns Either.Right(
                    AliasListResult(scopeId = "test", aliases = emptyList(), totalCount = 0),
                )
                coEvery { listScopesWithAspect(any()) } returns Either.Right(emptyList())
                coEvery { listScopesWithQuery(any()) } returns Either.Right(emptyList())
            }

            val command = mockk<ScopeManagementCommandPort>()
            val logger = mockk<Logger>(relaxed = true)

            val ports = Ports(query = query, command = command)
            val services = Services(
                errors = createErrorMapper(),
                idempotency = createIdempotencyService(createArgumentCodec()),
                codec = createArgumentCodec(),
                logger = logger,
            )
            return ports to services
        }
    }
}

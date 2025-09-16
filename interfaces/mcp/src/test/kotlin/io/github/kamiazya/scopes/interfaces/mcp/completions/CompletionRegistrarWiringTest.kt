package io.github.kamiazya.scopes.interfaces.mcp.completions

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.scopemanagement.ScopeManagementCommandPort
import io.github.kamiazya.scopes.contracts.scopemanagement.ScopeManagementQueryPort
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.*
import io.github.kamiazya.scopes.contracts.scopemanagement.results.AliasListResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeListResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeResult
import io.github.kamiazya.scopes.interfaces.mcp.support.createArgumentCodec
import io.github.kamiazya.scopes.interfaces.mcp.support.createErrorMapper
import io.github.kamiazya.scopes.interfaces.mcp.support.createIdempotencyService
import io.github.kamiazya.scopes.interfaces.mcp.tools.Ports
import io.github.kamiazya.scopes.interfaces.mcp.tools.Services
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.platform.observability.logging.LogLevel
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.CapturingSlot
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
            verify(exactly = 1) { server.setRequestHandler<CompleteRequest>(Method.Defined.CompletionComplete, any<suspend (CompleteRequest, Any?) -> CompleteResult>()) }

            // And ensure a handler was captured
            fnSlot.isCaptured shouldBe true
        }
    }) {
    companion object {
        private fun createSampleContext(): Pair<Ports, Services> {
        val query = object : ScopeManagementQueryPort {
            override suspend fun getScope(query: GetScopeQuery): Either<ScopeContractError, ScopeResult?> = Either.Right(null)

            override suspend fun getChildren(query: GetChildrenQuery): Either<ScopeContractError, ScopeListResult> = Either.Right(
                ScopeListResult(scopes = emptyList(), totalCount = 0, offset = 0, limit = 0),
            )

            override suspend fun getRootScopes(query: GetRootScopesQuery): Either<ScopeContractError, ScopeListResult> = Either.Right(
                ScopeListResult(scopes = emptyList(), totalCount = 0, offset = 0, limit = 0),
            )

            override suspend fun getScopeByAlias(query: GetScopeByAliasQuery): Either<ScopeContractError, ScopeResult> = Either.Left(
                ScopeContractError.BusinessError.AliasNotFound(alias = query.aliasName),
            )

            override suspend fun listAliases(query: ListAliasesQuery): Either<ScopeContractError, AliasListResult> = Either.Right(
                AliasListResult(scopeId = query.scopeId, aliases = emptyList(), totalCount = 0),
            )

            override suspend fun listScopesWithAspect(query: ListScopesWithAspectQuery): Either<ScopeContractError, List<ScopeResult>> = Either.Right(emptyList())

            override suspend fun listScopesWithQuery(query: ListScopesWithQueryQuery): Either<ScopeContractError, List<ScopeResult>> = Either.Right(emptyList())
        }

        val command = mockk<ScopeManagementCommandPort>()

        val ports = Ports(query = query, command = command)
        val logger = object : Logger {
            override fun debug(message: String, context: Map<String, Any>) {}
            override fun info(message: String, context: Map<String, Any>) {}
            override fun warn(message: String, context: Map<String, Any>) {}
            override fun error(message: String, context: Map<String, Any>, throwable: Throwable?) {}
            override fun isEnabledFor(level: LogLevel) = true
            override fun withContext(context: Map<String, Any>) = this
            override fun withName(name: String) = this
        }
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

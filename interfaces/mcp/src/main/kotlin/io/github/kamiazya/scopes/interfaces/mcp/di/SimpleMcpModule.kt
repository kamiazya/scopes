package io.github.kamiazya.scopes.interfaces.mcp.di

import io.github.kamiazya.scopes.contracts.scopemanagement.ScopeManagementCommandPort
import io.github.kamiazya.scopes.contracts.scopemanagement.ScopeManagementQueryPort
import io.github.kamiazya.scopes.interfaces.mcp.prompts.PromptProvider
import io.github.kamiazya.scopes.interfaces.mcp.prompts.PromptRegistrar
import io.github.kamiazya.scopes.interfaces.mcp.prompts.providers.ScopesOutlinePromptProvider
import io.github.kamiazya.scopes.interfaces.mcp.prompts.providers.ScopesPlanPromptProvider
import io.github.kamiazya.scopes.interfaces.mcp.prompts.providers.ScopesSummarizePromptProvider
import io.github.kamiazya.scopes.interfaces.mcp.resources.ResourceHandler
import io.github.kamiazya.scopes.interfaces.mcp.resources.ResourceRegistrar
import io.github.kamiazya.scopes.interfaces.mcp.resources.handlers.CliDocResourceHandler
import io.github.kamiazya.scopes.interfaces.mcp.resources.handlers.ScopeDetailsResourceHandler
import io.github.kamiazya.scopes.interfaces.mcp.resources.handlers.TreeJsonResourceHandler
import io.github.kamiazya.scopes.interfaces.mcp.resources.handlers.TreeMarkdownResourceHandler
import io.github.kamiazya.scopes.interfaces.mcp.server.McpServer
import io.github.kamiazya.scopes.interfaces.mcp.server.ServerBuilder
import io.github.kamiazya.scopes.interfaces.mcp.server.ToolRegistrar
import io.github.kamiazya.scopes.interfaces.mcp.server.TransportFactory
import io.github.kamiazya.scopes.interfaces.mcp.support.ArgumentCodec
import io.github.kamiazya.scopes.interfaces.mcp.support.DefaultArgumentCodec
import io.github.kamiazya.scopes.interfaces.mcp.support.DefaultErrorMapper
import io.github.kamiazya.scopes.interfaces.mcp.support.DefaultIdempotencyService
import io.github.kamiazya.scopes.interfaces.mcp.support.ErrorMapper
import io.github.kamiazya.scopes.interfaces.mcp.support.IdempotencyService
import io.github.kamiazya.scopes.interfaces.mcp.tools.Ports
import io.github.kamiazya.scopes.interfaces.mcp.tools.Services
import io.github.kamiazya.scopes.interfaces.mcp.tools.ToolHandler
import io.github.kamiazya.scopes.interfaces.mcp.tools.handlers.AliasResolveToolHandler
import io.github.kamiazya.scopes.interfaces.mcp.tools.handlers.AliasesAddToolHandler
import io.github.kamiazya.scopes.interfaces.mcp.tools.handlers.AliasesRemoveToolHandler
import io.github.kamiazya.scopes.interfaces.mcp.tools.handlers.AliasesSetCanonicalCamelToolHandler
import io.github.kamiazya.scopes.interfaces.mcp.tools.handlers.AliasesSetCanonicalToolHandler
import io.github.kamiazya.scopes.interfaces.mcp.tools.handlers.DebugListChangedToolHandler
import io.github.kamiazya.scopes.interfaces.mcp.tools.handlers.ScopeChildrenToolHandler
import io.github.kamiazya.scopes.interfaces.mcp.tools.handlers.ScopeCreateToolHandler
import io.github.kamiazya.scopes.interfaces.mcp.tools.handlers.ScopeDeleteToolHandler
import io.github.kamiazya.scopes.interfaces.mcp.tools.handlers.ScopeGetToolHandler
import io.github.kamiazya.scopes.interfaces.mcp.tools.handlers.ScopeUpdateToolHandler
import io.github.kamiazya.scopes.interfaces.mcp.tools.handlers.ScopesListAliasesToolHandler
import io.github.kamiazya.scopes.interfaces.mcp.tools.handlers.ScopesRootsToolHandler
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import org.koin.dsl.module

/**
 * Simplified Koin module for MCP server components.
 *
 * This module provides dependency injection configuration for the new MCP architecture.
 */
val simpleMcpModule = module {

    // Support Services
    single<ErrorMapper> { DefaultErrorMapper() }
    single<ArgumentCodec> { DefaultArgumentCodec() }
    single<IdempotencyService> { DefaultIdempotencyService(get<ArgumentCodec>()) }

    // Context Factory
    factory {
        val ports = Ports(
            query = get<ScopeManagementQueryPort>(),
            command = get<ScopeManagementCommandPort>(),
        )
        val services = Services(
            errors = get<ErrorMapper>(),
            idempotency = get<IdempotencyService>(),
            codec = get<ArgumentCodec>(),
            logger = get<Logger>().withName("MCP"),
        )
        ports to services
    }

    // Tool Handlers (manually list them for now)
    single {
        listOf<ToolHandler>(
            AliasResolveToolHandler(),
            AliasesAddToolHandler(),
            AliasesRemoveToolHandler(),
            AliasesSetCanonicalCamelToolHandler(), // Primary camelCase version
            AliasesSetCanonicalToolHandler(), // Deprecated snake_case version
            ScopeGetToolHandler(),
            ScopeCreateToolHandler(),
            ScopeUpdateToolHandler(),
            ScopeDeleteToolHandler(),
            ScopeChildrenToolHandler(),
            ScopesRootsToolHandler(),
            ScopesListAliasesToolHandler(),
            DebugListChangedToolHandler()
        )
    }

    // Resource Handlers
    single {
        listOf<ResourceHandler>(
            CliDocResourceHandler(),
            ScopeDetailsResourceHandler(),
            TreeJsonResourceHandler(),
            TreeMarkdownResourceHandler(),
        )
    }

    // Prompt Providers
    single {
        listOf<PromptProvider>(
            ScopesSummarizePromptProvider(),
            ScopesOutlinePromptProvider(),
            ScopesPlanPromptProvider(),
        )
    }

    // Registrars
    single {
        ToolRegistrar(
            handlers = get<List<ToolHandler>>(),
            ctxFactory = { get() },
        )
    }

    single {
        ResourceRegistrar(
            handlers = get<List<ResourceHandler>>(),
            contextFactory = { get() },
        )
    }

    single {
        PromptRegistrar(providers = get<List<PromptProvider>>())
    }

    // Server Components
    single { ServerBuilder() }
    single { TransportFactory.create() }

    // Main MCP Server
    single {
        McpServer(
            serverBuilder = get<ServerBuilder>(),
            transportFactory = get<TransportFactory>(),
            registrars = listOf(
                get<ToolRegistrar>(),
                get<ResourceRegistrar>(),
                get<PromptRegistrar>(),
            ),
            logger = get<Logger>().withName("McpServer"),
        )
    }
}

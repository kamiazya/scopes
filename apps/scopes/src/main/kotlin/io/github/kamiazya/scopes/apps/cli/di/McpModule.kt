package io.github.kamiazya.scopes.apps.cli.di

import io.github.kamiazya.scopes.contracts.scopemanagement.ScopeManagementCommandPort
import io.github.kamiazya.scopes.contracts.scopemanagement.ScopeManagementQueryPort
import io.github.kamiazya.scopes.interfaces.mcp.completions.CompletionRegistrar
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
import io.github.kamiazya.scopes.interfaces.mcp.support.ErrorMapper
import io.github.kamiazya.scopes.interfaces.mcp.support.IdempotencyService
import io.github.kamiazya.scopes.interfaces.mcp.support.createArgumentCodec
import io.github.kamiazya.scopes.interfaces.mcp.support.createErrorMapper
import io.github.kamiazya.scopes.interfaces.mcp.support.createIdempotencyService
import io.github.kamiazya.scopes.interfaces.mcp.tools.Ports
import io.github.kamiazya.scopes.interfaces.mcp.tools.Services
import io.github.kamiazya.scopes.interfaces.mcp.tools.ToolHandler
import io.github.kamiazya.scopes.interfaces.mcp.tools.handlers.AliasResolveToolHandler
import io.github.kamiazya.scopes.interfaces.mcp.tools.handlers.AliasesAddToolHandler
import io.github.kamiazya.scopes.interfaces.mcp.tools.handlers.AliasesRemoveToolHandler
import io.github.kamiazya.scopes.interfaces.mcp.tools.handlers.AliasesSetCanonicalCamelToolHandler
import io.github.kamiazya.scopes.interfaces.mcp.tools.handlers.ScopeChildrenToolHandler
import io.github.kamiazya.scopes.interfaces.mcp.tools.handlers.ScopeCreateToolHandler
import io.github.kamiazya.scopes.interfaces.mcp.tools.handlers.ScopeDeleteToolHandler
import io.github.kamiazya.scopes.interfaces.mcp.tools.handlers.ScopeGetToolHandler
import io.github.kamiazya.scopes.interfaces.mcp.tools.handlers.ScopeUpdateToolHandler
import io.github.kamiazya.scopes.interfaces.mcp.tools.handlers.ScopesListAliasesToolHandler
import io.github.kamiazya.scopes.interfaces.mcp.tools.handlers.ScopesRootsToolHandler
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.services.ResponseFormatterService
import org.koin.dsl.module

/**
 * MCP (Model Context Protocol) module for the CLI application.
 *
 * This module configures all MCP-related components including:
 * - Support services (error mapping, idempotency, argument codec)
 * - Tool handlers for scope operations
 * - Resource handlers for various output formats
 * - Prompt providers for AI assistance
 * - Server components and registrars
 */
val mcpModule = module {

    // Application Services
    single { ResponseFormatterService() }

    // Support Services - using factory functions from interfaces-mcp
    single<ErrorMapper> { createErrorMapper(get<Logger>().withName("MCP.ErrorMapper")) }
    single<ArgumentCodec> { createArgumentCodec() }
    single<IdempotencyService> { createIdempotencyService(get<ArgumentCodec>()) }

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

    // Tool Handlers
    single {
        val responseFormatter = get<ResponseFormatterService>()
        listOf<ToolHandler>(
            AliasResolveToolHandler(),
            AliasesAddToolHandler(),
            AliasesRemoveToolHandler(),
            AliasesSetCanonicalCamelToolHandler(),
            ScopeGetToolHandler(responseFormatter),
            ScopeCreateToolHandler(),
            ScopeUpdateToolHandler(),
            ScopeDeleteToolHandler(),
            ScopeChildrenToolHandler(responseFormatter),
            ScopesRootsToolHandler(responseFormatter),
            ScopesListAliasesToolHandler(),
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

    single {
        CompletionRegistrar(contextFactory = { get() })
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
                get<CompletionRegistrar>(),
            ),
            logger = get<Logger>().withName("McpServer"),
        )
    }
}

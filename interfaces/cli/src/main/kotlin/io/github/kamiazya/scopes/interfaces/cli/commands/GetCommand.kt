package io.github.kamiazya.scopes.interfaces.cli.commands

import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import io.github.kamiazya.scopes.interfaces.cli.core.ScopesCliktCommand
import io.github.kamiazya.scopes.interfaces.cli.formatters.ScopeOutputFormatter
import io.github.kamiazya.scopes.interfaces.cli.resolvers.ScopeParameterResolver
import io.github.kamiazya.scopes.interfaces.cli.transport.Transport
import io.github.kamiazya.scopes.scopemanagement.application.services.ResponseFormatterService
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Get command for retrieving scopes.
 */
class GetCommand :
    ScopesCliktCommand(name = "get"),
    KoinComponent {

    override fun help(context: com.github.ajalt.clikt.core.Context) = "Get a scope by ID or alias"
    private val transport: Transport by inject()
    private val scopeOutputFormatter: ScopeOutputFormatter by inject()
    private val parameterResolver: ScopeParameterResolver by inject()
    private val responseFormatter: ResponseFormatterService by inject()
    private val debugContext by requireObject<DebugContext>()

    private val identifier by argument(
        name = "SCOPE",
        help = "Scope identifier (ULID or alias)",
    )

    override fun run() {
        runBlocking {
            // Resolve the identifier to a scope ID
            parameterResolver.resolve(identifier).fold(
                { error ->
                    handleContractError(error)
                },
                { resolvedId ->
                    // Fetch the scope using the resolved ID via Transport
                    transport.getScope(resolvedId).fold(
                        { error ->
                            handleContractError(error)
                        },
                        { scope ->
                            if (scope == null) {
                                echo("Scope not found: $resolvedId", err = true)
                                return@runBlocking
                            }

                            // Fetch all aliases for the scope via Transport
                            transport.listAliases(scope.id).fold(
                                { aliasError ->
                                    // If we can't fetch aliases, still show the scope with just canonical alias
                                    echo(
                                        responseFormatter.formatScopeForCli(
                                            scope = scope,
                                            aliases = null,
                                            includeDebug = debugContext.debug,
                                            includeTemporalFields = true,
                                        ),
                                    )
                                },
                                { aliasResult ->
                                    // Show scope with all aliases
                                    echo(
                                        responseFormatter.formatScopeForCli(
                                            scope = scope,
                                            aliases = aliasResult.aliases,
                                            includeDebug = debugContext.debug,
                                            includeTemporalFields = true,
                                        ),
                                    )
                                },
                            )
                        },
                    )
                },
            )
        }
    }
}

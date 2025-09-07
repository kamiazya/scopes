package io.github.kamiazya.scopes.interfaces.cli.commands

import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import io.github.kamiazya.scopes.interfaces.cli.adapters.ScopeQueryAdapter
import io.github.kamiazya.scopes.interfaces.cli.core.ScopesCliktCommand
import io.github.kamiazya.scopes.interfaces.cli.formatters.ScopeOutputFormatter
import io.github.kamiazya.scopes.interfaces.cli.resolvers.ScopeParameterResolver
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Get command for retrieving scopes.
 */
class GetCommand :
    ScopesCliktCommand(
        name = "get",
        help = "Get a scope by ID or alias",
    ),
    KoinComponent {
    private val scopeQueryAdapter: ScopeQueryAdapter by inject()
    private val scopeOutputFormatter: ScopeOutputFormatter by inject()
    private val parameterResolver: ScopeParameterResolver by inject()
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
                    // Fetch the scope using the resolved ID
                    scopeQueryAdapter.getScopeById(resolvedId).fold(
                        { error ->
                            handleContractError(error)
                        },
                        { scope ->
                            // Fetch all aliases for the scope
                            scopeQueryAdapter.listAliases(scope.id).fold(
                                { aliasError ->
                                    // If we can't fetch aliases, still show the scope with just canonical alias
                                    echo(scopeOutputFormatter.formatContractScope(scope, debugContext.debug))
                                },
                                { aliasResult ->
                                    // Show scope with all aliases
                                    echo(scopeOutputFormatter.formatContractScopeWithAliases(scope, aliasResult, debugContext.debug))
                                },
                            )
                        },
                    )
                },
            )
        }
    }
}

package io.github.kamiazya.scopes.interfaces.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import io.github.kamiazya.scopes.interfaces.cli.adapters.ScopeCommandAdapter
import io.github.kamiazya.scopes.interfaces.cli.formatters.ScopeOutputFormatter
import io.github.kamiazya.scopes.interfaces.cli.mappers.ContractErrorMessageMapper
import io.github.kamiazya.scopes.interfaces.cli.resolvers.ScopeParameterResolver
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Get command for retrieving scopes.
 */
class GetCommand :
    CliktCommand(
        name = "get",
        help = "Get a scope by ID or alias",
    ),
    KoinComponent {
    private val scopeCommandAdapter: ScopeCommandAdapter by inject()
    private val scopeOutputFormatter: ScopeOutputFormatter by inject()
    private val parameterResolver: ScopeParameterResolver by inject()

    private val identifier by argument(
        name = "SCOPE",
        help = "Scope identifier (ULID or alias)",
    )

    override fun run() {
        runBlocking {
            // Resolve the identifier to a scope ID
            parameterResolver.resolve(identifier).fold(
                { error ->
                    echo("Error: ${ContractErrorMessageMapper.getMessage(error)}", err = true)
                },
                { resolvedId ->
                    // Fetch the scope using the resolved ID
                    scopeCommandAdapter.getScopeById(resolvedId).fold(
                        { error ->
                            echo("Error: ${ContractErrorMessageMapper.getMessage(error)}", err = true)
                        },
                        { scope ->
                            echo(scopeOutputFormatter.formatContractScope(scope))
                        },
                    )
                },
            )
        }
    }
}

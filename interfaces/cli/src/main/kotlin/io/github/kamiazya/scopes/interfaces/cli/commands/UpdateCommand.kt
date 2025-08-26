package io.github.kamiazya.scopes.interfaces.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import io.github.kamiazya.scopes.interfaces.cli.adapters.ScopeCommandAdapter
import io.github.kamiazya.scopes.interfaces.cli.formatters.ScopeOutputFormatter
import io.github.kamiazya.scopes.interfaces.cli.mappers.ContractErrorMessageMapper
import io.github.kamiazya.scopes.interfaces.cli.resolvers.ScopeParameterResolver
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Update command for modifying existing scopes.
 */
class UpdateCommand :
    CliktCommand(
        name = "update",
        help = "Update an existing scope",
    ),
    KoinComponent {
    private val scopeCommandAdapter: ScopeCommandAdapter by inject()
    private val scopeOutputFormatter: ScopeOutputFormatter by inject()
    private val parameterResolver: ScopeParameterResolver by inject()

    private val identifier by argument(
        name = "SCOPE",
        help = "Scope to update (ULID or alias)",
    )
    private val title by option("-t", "--title", help = "New title for the scope")
    private val description by option("-d", "--description", help = "New description for the scope")

    override fun run() {
        runBlocking {
            // Resolve the identifier to a scope ID
            parameterResolver.resolve(identifier).fold(
                { error ->
                    throw CliktError("Error: ${ContractErrorMessageMapper.getMessage(error)}")
                },
                { resolvedId ->
                    scopeCommandAdapter.updateScope(
                        id = resolvedId,
                        title = title,
                        description = description,
                    ).fold(
                        { error ->
                            echo("Error: ${ContractErrorMessageMapper.getMessage(error)}", err = true)
                        },
                        { scope ->
                            echo(scopeOutputFormatter.formatContractUpdateResult(scope))
                        },
                    )
                },
            )
        }
    }
}

package io.github.kamiazya.scopes.interfaces.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import io.github.kamiazya.scopes.interfaces.cli.adapters.ScopeCommandAdapter
import io.github.kamiazya.scopes.interfaces.cli.formatters.ScopeOutputFormatter
import io.github.kamiazya.scopes.interfaces.cli.mappers.ContractErrorMessageMapper
import io.github.kamiazya.scopes.interfaces.cli.resolvers.ScopeParameterResolver
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Delete command for removing scopes.
 */
class DeleteCommand :
    CliktCommand(
        name = "delete",
        help = "Delete a scope",
    ),
    KoinComponent {
    private val scopeCommandAdapter: ScopeCommandAdapter by inject()
    private val scopeOutputFormatter: ScopeOutputFormatter by inject()
    private val parameterResolver: ScopeParameterResolver by inject()

    private val identifier by argument(
        name = "SCOPE",
        help = "Scope to delete (ULID or alias)",
    )
    private val cascade by option("--cascade", help = "Delete all child scopes").flag()

    override fun run() {
        runBlocking {
            // Resolve the identifier to a scope ID
            parameterResolver.resolve(identifier).fold(
                { error ->
                    throw CliktError("Error: ${ContractErrorMessageMapper.getMessage(error)}")
                },
                { resolvedId ->
                    scopeCommandAdapter.deleteScope(resolvedId).fold(
                        { error ->
                            throw CliktError("Error: ${ContractErrorMessageMapper.getMessage(error)}")
                        },
                        {
                            echo(scopeOutputFormatter.formatDeleteResult(resolvedId))
                        },
                    )
                },
            )
        }
    }
}

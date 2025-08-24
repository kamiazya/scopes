package io.github.kamiazya.scopes.interfaces.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import io.github.kamiazya.scopes.interfaces.cli.adapters.ScopeCommandAdapter
import io.github.kamiazya.scopes.interfaces.cli.formatters.ScopeOutputFormatter
import io.github.kamiazya.scopes.interfaces.cli.mappers.ContractErrorMessageMapper
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * List command for retrieving multiple scopes.
 */
class ListCommand :
    CliktCommand(
        name = "list",
        help = "List scopes",
    ),
    KoinComponent {
    private val scopeCommandAdapter: ScopeCommandAdapter by inject()
    private val scopeOutputFormatter: ScopeOutputFormatter by inject()

    private val parentId by option("-p", "--parent", help = "Parent scope ID to list children")
    private val root by option("--root", help = "List only root scopes").flag()
    private val offset by option("--offset", help = "Number of items to skip").int().default(0)
    private val limit by option("--limit", help = "Maximum number of items to return").int().default(20)

    override fun run() {
        runBlocking {
            when {
                root -> {
                    scopeCommandAdapter.listRootScopes().fold(
                        { error ->
                            echo("Error: ${ContractErrorMessageMapper.getMessage(error)}", err = true)
                        },
                        { scopes ->
                            echo(scopeOutputFormatter.formatContractScopeList(scopes))
                        },
                    )
                }
                parentId != null -> {
                    scopeCommandAdapter.listChildren(parentId!!).fold(
                        { error ->
                            echo("Error: ${ContractErrorMessageMapper.getMessage(error)}", err = true)
                        },
                        { scopes ->
                            echo(scopeOutputFormatter.formatContractScopeList(scopes))
                        },
                    )
                }
                else -> {
                    // Default: list root scopes
                    scopeCommandAdapter.listRootScopes().fold(
                        { error ->
                            echo("Error: ${ContractErrorMessageMapper.getMessage(error)}", err = true)
                        },
                        { scopes ->
                            echo(scopeOutputFormatter.formatContractScopeList(scopes))
                        },
                    )
                }
            }
        }
    }
}

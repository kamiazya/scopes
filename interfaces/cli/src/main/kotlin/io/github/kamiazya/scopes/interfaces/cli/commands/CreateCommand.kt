package io.github.kamiazya.scopes.interfaces.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import io.github.kamiazya.scopes.interfaces.cli.adapters.ScopeCommandAdapter
import io.github.kamiazya.scopes.interfaces.cli.formatters.ScopeOutputFormatter
import io.github.kamiazya.scopes.interfaces.cli.mappers.ContractErrorMessageMapper
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Create command for creating new scopes.
 */
class CreateCommand :
    CliktCommand(
        name = "create",
        help = "Create a new scope",
    ),
    KoinComponent {
    private val scopeCommandAdapter: ScopeCommandAdapter by inject()
    private val scopeOutputFormatter: ScopeOutputFormatter by inject()

    private val title by argument(help = "Title of the scope")
    private val description by option("-d", "--description", help = "Description of the scope")
    private val parentId by option("-p", "--parent", help = "Parent scope ID")
    private val noAlias by option("--no-alias", help = "Don't generate an alias").flag()
    private val customAlias by option("-a", "--alias", help = "Custom alias for the scope")

    override fun run() {
        runBlocking {
            scopeCommandAdapter.createScope(
                title = title,
                description = description,
                parentId = parentId,
                generateAlias = !noAlias && customAlias == null,
                customAlias = customAlias,
            ).fold(
                { error ->
                    throw CliktError("Error: ${ContractErrorMessageMapper.getMessage(error)}")
                },
                { result ->
                    echo(scopeOutputFormatter.formatContractCreateResult(result))
                },
            )
        }
    }
}

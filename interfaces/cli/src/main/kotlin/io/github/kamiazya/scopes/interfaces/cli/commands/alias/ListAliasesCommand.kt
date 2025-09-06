package io.github.kamiazya.scopes.interfaces.cli.commands.alias

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.choice
import io.github.kamiazya.scopes.interfaces.cli.adapters.AliasQueryAdapter
import io.github.kamiazya.scopes.interfaces.cli.formatters.AliasOutputFormatter
import io.github.kamiazya.scopes.interfaces.cli.mappers.ContractErrorMessageMapper
import io.github.kamiazya.scopes.interfaces.cli.resolvers.ScopeParameterResolver
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Command to list all aliases for a scope.
 */
class ListAliasesCommand :
    CliktCommand(
        name = "list",
        help = "List all aliases for a scope",
    ),
    KoinComponent {
    private val aliasQueryAdapter: AliasQueryAdapter by inject()
    private val aliasOutputFormatter: AliasOutputFormatter by inject()
    private val parameterResolver: ScopeParameterResolver by inject()

    private val scope by option("-s", "--scope", help = "The scope (ID or alias) to list aliases for").required()
    private val format by option("-f", "--format", help = "Output format").choice("table", "json", "plain").default("table")

    override fun run() {
        runBlocking {
            // Resolve scope ID
            val scopeId = parameterResolver.resolve(scope).fold(
                { error ->
                    throw CliktError("Error resolving scope: ${ContractErrorMessageMapper.getMessage(error)}")
                },
                { resolvedId -> resolvedId },
            )

            // List aliases
            aliasQueryAdapter.listAliases(scopeId).fold(
                { error ->
                    throw CliktError("Error listing aliases: ${ContractErrorMessageMapper.getMessage(error)}")
                },
                { aliasResult ->
                    when (format) {
                        "json" -> echo(aliasOutputFormatter.formatAliasListResultAsJson(aliasResult))
                        "plain" -> echo(aliasOutputFormatter.formatAliasListResultAsPlain(aliasResult))
                        else -> echo(aliasOutputFormatter.formatAliasListResultAsTable(aliasResult))
                    }
                },
            )
        }
    }
}

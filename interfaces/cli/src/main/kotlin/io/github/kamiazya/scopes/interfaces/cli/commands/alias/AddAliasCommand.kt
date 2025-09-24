package io.github.kamiazya.scopes.interfaces.cli.commands.alias

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import io.github.kamiazya.scopes.interfaces.cli.adapters.AliasCommandAdapter
import io.github.kamiazya.scopes.interfaces.cli.mappers.ContractErrorMessageMapper
import io.github.kamiazya.scopes.interfaces.cli.resolvers.ScopeParameterResolver
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Command to add a new alias to a scope.
 */
class AddAliasCommand :
    CliktCommand(
        name = "add",
    ),
    KoinComponent {
    override fun help(context: com.github.ajalt.clikt.core.Context) = "Add a new alias to a scope"
    private val aliasCommandAdapter: AliasCommandAdapter by inject()
    private val parameterResolver: ScopeParameterResolver by inject()

    private val aliasName by argument("alias", help = "The alias name to add")
    private val scope by option("-s", "--scope", help = "The scope (ID or alias) to add the alias to").required()

    override fun run() {
        runBlocking {
            // Resolve scope ID
            val scopeId = parameterResolver.resolve(scope).fold(
                { error ->
                    throw CliktError("Error resolving scope: ${ContractErrorMessageMapper.getMessage(error)}")
                },
                { resolvedId -> resolvedId },
            )

            // Add the alias
            aliasCommandAdapter.addAlias(scopeId, aliasName).fold(
                { error ->
                    throw CliktError("Error adding alias: ${ContractErrorMessageMapper.getMessage(error)}")
                },
                {
                    echo("Alias '$aliasName' added successfully to scope '$scope'")
                },
            )
        }
    }
}

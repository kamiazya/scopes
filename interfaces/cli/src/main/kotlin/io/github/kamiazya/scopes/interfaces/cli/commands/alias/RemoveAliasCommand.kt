package io.github.kamiazya.scopes.interfaces.cli.commands.alias

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import io.github.kamiazya.scopes.interfaces.cli.mappers.ContractErrorMessageMapper
import io.github.kamiazya.scopes.interfaces.cli.resolvers.ScopeParameterResolver
import io.github.kamiazya.scopes.interfaces.cli.transport.Transport
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Command to remove an alias from a scope.
 */
class RemoveAliasCommand :
    CliktCommand(
        name = "rm",
    ),
    KoinComponent {
    override fun help(context: com.github.ajalt.clikt.core.Context) = "Remove an alias from a scope"
    private val transport: Transport by inject()
    private val parameterResolver: ScopeParameterResolver by inject()

    private val aliasName by argument("alias", help = "The alias name to remove")
    private val scope by option("-s", "--scope", help = "The scope (ID or alias) to remove the alias from").required()

    override fun run() {
        runBlocking {
            // Resolve scope ID
            val scopeId = parameterResolver.resolve(scope).fold(
                { error ->
                    throw CliktError("Error resolving scope: ${ContractErrorMessageMapper.getMessage(error)}")
                },
                { resolvedId -> resolvedId },
            )

            // Remove alias
            transport.removeAlias(scopeId, aliasName).fold(
                { error ->
                    throw CliktError("Error removing alias: ${ContractErrorMessageMapper.getMessage(error)}")
                },
                { result ->
                    echo("Alias '$aliasName' removed successfully from scope '$scope'")
                },
            )
        }
    }
}

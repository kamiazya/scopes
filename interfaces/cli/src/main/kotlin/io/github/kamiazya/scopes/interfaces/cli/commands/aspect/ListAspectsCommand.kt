package io.github.kamiazya.scopes.interfaces.cli.commands.aspect

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.parameters.arguments.argument
import io.github.kamiazya.scopes.interfaces.cli.adapters.ScopeCommandAdapter
import io.github.kamiazya.scopes.interfaces.cli.mappers.ContractErrorMessageMapper
import io.github.kamiazya.scopes.interfaces.cli.resolvers.ScopeParameterResolver
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Command to list all aspects for a scope.
 *
 * Usage: scopes aspect list <scope>
 *
 * Note: Currently this is a simplified implementation that reads aspect data
 * from the description field. A proper implementation would require
 * the contract layer to support metadata/aspects.
 */
class ListAspectsCommand :
    CliktCommand(
        name = "list",
        help = "List all aspects for a scope",
    ),
    KoinComponent {
    private val scopeCommandAdapter: ScopeCommandAdapter by inject()
    private val parameterResolver: ScopeParameterResolver by inject()

    private val scope by argument("scope", help = "The scope (alias) to list aspects for")

    override fun run() {
        runBlocking {
            // Resolve scope ID from alias
            val scopeId = parameterResolver.resolve(scope).fold(
                { error ->
                    throw CliktError("Error resolving scope: ${ContractErrorMessageMapper.getMessage(error)}")
                },
                { resolvedId -> resolvedId },
            )

            // Get scope information
            scopeCommandAdapter.getScopeById(scopeId).fold(
                { error ->
                    throw CliktError("Error getting scope: ${ContractErrorMessageMapper.getMessage(error)}")
                },
                { scopeResult ->
                    // For now, we parse aspects from the description field
                    val description = scopeResult.description ?: ""
                    if (description.startsWith("Aspects: ")) {
                        val aspectsStr = description.substring("Aspects: ".length)
                        val aspects = aspectsStr.split(", ").mapNotNull { part ->
                            val keyValue = part.split("=", limit = 2)
                            if (keyValue.size == 2) {
                                keyValue[0] to keyValue[1]
                            } else {
                                null
                            }
                        }

                        if (aspects.isNotEmpty()) {
                            echo("Aspects for scope '$scope':")
                            aspects.forEach { (key, value) ->
                                echo("  $key: $value")
                            }
                        } else {
                            echo("No aspects set for scope '$scope'")
                        }
                    } else {
                        echo("No aspects set for scope '$scope'")
                    }

                    echo("\n⚠️  Note: This is a temporary implementation reading from the description field.")
                },
            )
        }
    }
}

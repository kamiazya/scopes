package io.github.kamiazya.scopes.interfaces.cli.commands.aspect

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import io.github.kamiazya.scopes.interfaces.cli.adapters.ScopeCommandAdapter
import io.github.kamiazya.scopes.interfaces.cli.mappers.ContractErrorMessageMapper
import io.github.kamiazya.scopes.interfaces.cli.resolvers.ScopeParameterResolver
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Command to remove aspects from a scope.
 *
 * Usage: scopes aspect remove <scope> <key> [<key>...]
 *
 * Note: Currently this is a simplified implementation that manipulates aspect data
 * in the description field. A proper implementation would require
 * the contract layer to support metadata/aspects.
 */
class RemoveAspectCommand :
    CliktCommand(
        name = "remove",
        help = "Remove aspects from a scope",
    ),
    KoinComponent {
    private val scopeCommandAdapter: ScopeCommandAdapter by inject()
    private val parameterResolver: ScopeParameterResolver by inject()

    private val scope by argument("scope", help = "The scope (alias) to remove aspects from")
    private val keys by argument("keys", help = "Aspect keys to remove").multiple(required = true)

    override fun run() {
        runBlocking {
            // Resolve scope ID from alias
            val scopeId = parameterResolver.resolve(scope).fold(
                { error ->
                    throw CliktError("Error resolving scope: ${ContractErrorMessageMapper.getMessage(error)}")
                },
                { resolvedId -> resolvedId },
            )

            // Get current scope
            scopeCommandAdapter.getScopeById(scopeId).fold(
                { error ->
                    throw CliktError("Error getting scope: ${ContractErrorMessageMapper.getMessage(error)}")
                },
                { scopeResult ->
                    // Parse existing aspects from description
                    val description = scopeResult.description ?: ""
                    val existingAspects = if (description.startsWith("Aspects: ")) {
                        val aspectsStr = description.substring("Aspects: ".length)
                        aspectsStr.split(", ").mapNotNull { part ->
                            val keyValue = part.split("=", limit = 2)
                            if (keyValue.size == 2) {
                                keyValue[0] to keyValue[1]
                            } else {
                                null
                            }
                        }.toMutableList()
                    } else {
                        mutableListOf()
                    }

                    // Remove specified keys
                    val removedKeys = mutableListOf<String>()
                    val notFoundKeys = mutableListOf<String>()

                    keys.forEach { key ->
                        val removed = existingAspects.removeAll { it.first == key }
                        if (removed) {
                            removedKeys.add(key)
                        } else {
                            notFoundKeys.add(key)
                        }
                    }

                    if (removedKeys.isEmpty()) {
                        echo("No aspects removed. Keys not found: ${notFoundKeys.joinToString(", ")}")
                    } else {
                        // Update description with remaining aspects
                        val newDescription = if (existingAspects.isEmpty()) {
                            null
                        } else {
                            "Aspects: " + existingAspects.joinToString(", ") { (k, v) -> "$k=$v" }
                        }

                        scopeCommandAdapter.updateScope(
                            id = scopeId,
                            description = newDescription,
                        ).fold(
                            { error ->
                                throw CliktError("Error updating scope: ${ContractErrorMessageMapper.getMessage(error)}")
                            },
                            {
                                echo("✓ Removed aspects from scope '$scope':")
                                removedKeys.forEach { key ->
                                    echo("    - $key")
                                }
                                if (notFoundKeys.isNotEmpty()) {
                                    echo("⚠ Keys not found: ${notFoundKeys.joinToString(", ")}")
                                }
                                echo("\n⚠️  Note: This is a temporary implementation using the description field.")
                            },
                        )
                    }
                },
            )
        }
    }
}

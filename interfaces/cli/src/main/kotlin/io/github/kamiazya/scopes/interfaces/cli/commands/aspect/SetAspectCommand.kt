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
 * Command to set aspects on a scope.
 *
 * Usage: scopes aspect set <scope> <key>=<value> [<key>=<value>...]
 *
 * Note: Currently this is a simplified implementation that uses title and description
 * fields to store aspect data temporarily. A proper implementation would require
 * updating the contract layer to support metadata/aspects.
 */
class SetAspectCommand :
    CliktCommand(
        name = "set",
        help = "Set aspects on a scope",
    ),
    KoinComponent {
    private val scopeCommandAdapter: ScopeCommandAdapter by inject()
    private val parameterResolver: ScopeParameterResolver by inject()

    private val scope by argument("scope", help = "The scope (alias) to set aspects on")
    private val aspects by argument("aspects", help = "Aspects in key=value format").multiple(required = true)

    override fun run() {
        runBlocking {
            // Parse aspects
            val parsedAspects = aspects.mapNotNull { aspect ->
                val parts = aspect.split("=", limit = 2)
                if (parts.size != 2) {
                    throw CliktError("Invalid aspect format: '$aspect'. Expected format: key=value")
                }
                parts[0].trim() to parts[1].trim()
            }

            if (parsedAspects.isEmpty()) {
                throw CliktError("At least one aspect must be provided")
            }

            // Resolve scope ID from alias
            val scopeId = parameterResolver.resolve(scope).fold(
                { error ->
                    throw CliktError("Error resolving scope: ${ContractErrorMessageMapper.getMessage(error)}")
                },
                { resolvedId -> resolvedId },
            )

            // For now, we'll store aspects in the description field as a workaround
            // This is a temporary solution until the contract layer supports metadata
            val aspectsJson = parsedAspects.joinToString(", ") { (key, value) -> "$key=$value" }

            scopeCommandAdapter.updateScope(
                id = scopeId,
                description = "Aspects: $aspectsJson", // Temporary storage
            ).fold(
                { error ->
                    throw CliktError("Error updating scope: ${ContractErrorMessageMapper.getMessage(error)}")
                },
                {
                    echo("✓ Set aspects on scope '$scope':")
                    parsedAspects.forEach { (key, value) ->
                        echo("    $key: $value")
                    }
                    echo("\n⚠️  Note: This is a temporary implementation. Aspects are stored in the description field.")
                },
            )
        }
    }
}

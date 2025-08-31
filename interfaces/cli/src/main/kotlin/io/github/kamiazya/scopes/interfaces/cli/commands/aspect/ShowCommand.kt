package io.github.kamiazya.scopes.interfaces.cli.commands.aspect

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import io.github.kamiazya.scopes.interfaces.cli.adapters.AspectCommandAdapter
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectType
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Command for showing aspect definition details.
 * Usage: scopes aspect show <key>
 */
class ShowCommand :
    CliktCommand(
        name = "show",
        help = "Show aspect definition details",
    ),
    KoinComponent {
    private val aspectCommandAdapter: AspectCommandAdapter by inject()

    private val key by argument(help = "The aspect key to show")

    override fun run() {
        runBlocking {
            aspectCommandAdapter.getAspectDefinition(key).fold(
                { error ->
                    echo("Error: $error", err = true)
                },
                { definition ->
                    if (definition == null) {
                        echo("Aspect '$key' not found", err = true)
                    } else {
                        echo("Aspect: ${definition.key.value}")
                        echo("Description: ${definition.description}")
                        echo("Type: ${formatType(definition.type)}")
                        echo("Constraints:")
                        when (val type = definition.type) {
                            is AspectType.Text -> {
                                echo("  - Type: Text (no constraints in current implementation)")
                            }
                            is AspectType.Numeric -> {
                                echo("  - Type: Numeric (no constraints in current implementation)")
                            }
                            is AspectType.BooleanType -> {
                                echo("  - Allowed values: true, false")
                            }
                            is AspectType.Ordered -> {
                                echo("  - Allowed values: ${type.allowedValues.joinToString(", ") { it.value }}")
                            }
                            is AspectType.Duration -> {
                                echo("  - Type: Duration (ISO 8601 format)")
                                echo("  - Examples: P1D (1 day), PT2H30M (2 hours 30 minutes), P1W (1 week)")
                            }
                        }
                    }
                },
            )
        }
    }

    private fun formatType(type: AspectType): String = when (type) {
        is AspectType.Text -> "Text"
        is AspectType.Numeric -> "Numeric"
        is AspectType.BooleanType -> "Boolean"
        is AspectType.Ordered -> "Ordered"
        is AspectType.Duration -> "Duration"
    }
}

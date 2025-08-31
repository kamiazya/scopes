package io.github.kamiazya.scopes.interfaces.cli.commands.aspect

import com.github.ajalt.clikt.core.CliktCommand
import io.github.kamiazya.scopes.interfaces.cli.adapters.AspectCommandAdapter
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectType
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Command for listing all aspect definitions.
 * Usage: scopes aspect definitions
 */
class ListDefinitionsCommand :
    CliktCommand(
        name = "definitions",
        help = "List all aspect definitions",
    ),
    KoinComponent {
    private val aspectCommandAdapter: AspectCommandAdapter by inject()

    override fun run() {
        runBlocking {
            aspectCommandAdapter.listAspectDefinitions().fold(
                { error ->
                    echo("Error: $error", err = true)
                },
                { definitions ->
                    if (definitions.isEmpty()) {
                        echo("No aspect definitions found")
                    } else {
                        echo("Aspect Definitions:")
                        echo("")
                        definitions.forEach { definition ->
                            echo("• ${definition.key.value} - ${definition.description}")
                            echo("  Type: ${formatType(definition.type)}")
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
        is AspectType.Ordered -> "Ordered (${type.allowedValues.size} values)"
        is AspectType.Duration -> "Duration (ISO 8601)"
    }
}

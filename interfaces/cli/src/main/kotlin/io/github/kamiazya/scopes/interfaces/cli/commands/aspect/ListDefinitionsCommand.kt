package io.github.kamiazya.scopes.interfaces.cli.commands.aspect

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import io.github.kamiazya.scopes.interfaces.cli.adapters.AspectQueryAdapter
import io.github.kamiazya.scopes.interfaces.cli.mappers.ErrorMessageMapper
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Command for listing all aspect definitions.
 * Usage: scopes aspect definitions
 *
 * Note: Uses CliktError for error handling.
 */
class ListDefinitionsCommand :
    CliktCommand(
        name = "definitions",
        help = "List all aspect definitions",
    ),
    KoinComponent {
    private val aspectQueryAdapter: AspectQueryAdapter by inject()

    override fun run() {
        runBlocking {
            aspectQueryAdapter.listAspectDefinitions().fold(
                ifLeft = { error ->
                    throw CliktError(ErrorMessageMapper.toUserMessage(error))
                },
                ifRight = { definitions ->
                    if (definitions.isEmpty()) {
                        echo("No aspect definitions found")
                    } else {
                        echo("Aspect Definitions:")
                        echo("")
                        definitions.forEach { definition ->
                            echo("• ${definition.key} - ${definition.description}")
                            echo("  Type: ${definition.type}")
                        }
                    }
                },
            )
        }
    }
}

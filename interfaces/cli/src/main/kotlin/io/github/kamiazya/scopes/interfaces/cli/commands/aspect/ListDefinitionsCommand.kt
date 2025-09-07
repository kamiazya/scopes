package io.github.kamiazya.scopes.interfaces.cli.commands.aspect

import com.github.ajalt.clikt.core.CliktCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.results.ListAspectDefinitionsResult
import io.github.kamiazya.scopes.interfaces.cli.adapters.AspectQueryAdapter
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
            when (val result = aspectQueryAdapter.listAspectDefinitions()) {
                is ListAspectDefinitionsResult.Success -> {
                    val definitions = result.aspectDefinitions
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
                }
            }
        }
    }
}

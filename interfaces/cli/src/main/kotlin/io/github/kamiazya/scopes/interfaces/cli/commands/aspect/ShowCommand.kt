package io.github.kamiazya.scopes.interfaces.cli.commands.aspect

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import io.github.kamiazya.scopes.interfaces.cli.adapters.AspectQueryAdapter
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
    private val aspectQueryAdapter: AspectQueryAdapter by inject()

    private val key by argument(help = "The aspect key to show")

    override fun run() {
        runBlocking {
            when (val result = aspectQueryAdapter.getAspectDefinition(key)) {
                is io.github.kamiazya.scopes.contracts.scopemanagement.aspect.AspectContract.GetAspectDefinitionResponse.Success -> {
                    val definition = result.aspectDefinition
                    if (definition == null) {
                        echo("Aspect '$key' not found", err = true)
                    } else {
                        echo("Aspect: ${definition.key}")
                        echo("Description: ${definition.description}")
                        echo("Type: ${definition.type}")
                    }
                }
                is io.github.kamiazya.scopes.contracts.scopemanagement.aspect.AspectContract.GetAspectDefinitionResponse.NotFound -> {
                    echo("Aspect '${result.key}' not found", err = true)
                }
            }
        }
    }
}

package io.github.kamiazya.scopes.interfaces.cli.commands.aspect

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.parameters.arguments.argument
import io.github.kamiazya.scopes.interfaces.cli.adapters.AspectQueryAdapter
import io.github.kamiazya.scopes.interfaces.cli.mappers.ErrorMessageMapper
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Command for showing aspect definition details.
 * Usage: scopes aspect show <key>
 *
 * Note: Uses CliktError for error handling.
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
            aspectQueryAdapter.getAspectDefinition(key).fold(
                ifLeft = { error ->
                    throw CliktError(ErrorMessageMapper.getMessage(error))
                },
                ifRight = { definition ->
                    if (definition == null) {
                        echo("Aspect '$key' not found", err = true)
                    } else {
                        echo("Aspect: ${definition.key}")
                        echo("Description: ${definition.description}")
                        echo("Type: ${definition.type}")
                    }
                },
            )
        }
    }
}

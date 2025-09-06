package io.github.kamiazya.scopes.interfaces.cli.commands.aspect

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import io.github.kamiazya.scopes.interfaces.cli.adapters.AspectCommandAdapter
import io.github.kamiazya.scopes.interfaces.cli.mappers.ContractErrorMessageMapper
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Command for editing an existing aspect definition.
 * Currently only supports updating the description.
 * Usage: scopes aspect edit <key> --description <new-description>
 */
class EditCommand :
    CliktCommand(
        name = "edit",
        help = "Edit an existing aspect definition",
    ),
    KoinComponent {
    private val aspectCommandAdapter: AspectCommandAdapter by inject()

    private val key by argument(help = "The aspect key to edit")
    private val description by option("-d", "--description", help = "New description for the aspect")

    override fun run() {
        runBlocking {
            if (description == null) {
                echo("Error: No changes specified. Use --description to update the description.", err = true)
                return@runBlocking
            }

            val result = aspectCommandAdapter.updateAspectDefinition(
                key = key,
                description = description,
            )

            result.fold(
                ifLeft = { error ->
                    echo("Error: Failed to update aspect '$key': ${ContractErrorMessageMapper.getMessage(error)}", err = true)
                },
                ifRight = {
                    echo("Aspect '$key' updated successfully")
                    description?.let { echo("Description: $it") }
                },
            )
        }
    }
}

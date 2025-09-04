package io.github.kamiazya.scopes.interfaces.cli.commands.aspect

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import io.github.kamiazya.scopes.interfaces.cli.adapters.AspectCommandAdapter
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
                    echo("Error: Failed to update aspect '$key': ${formatError(error)}", err = true)
                },
                ifRight = {
                    echo("Aspect '$key' updated successfully")
                    description?.let { echo("Description: $it") }
                },
            )
        }
    }

    private fun formatError(error: io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError): String = when (error) {
        is io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError.BusinessError.NotFound -> "Not found: ${error.scopeId}"
        is io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError.BusinessError.DuplicateAlias -> "Already exists: ${error.alias}"
        is io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError.InputError.InvalidTitle -> "Invalid input: ${error.title}"
        is io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError.SystemError.ServiceUnavailable ->
            "Service unavailable: ${error.service}"
        else -> error.toString()
    }
}

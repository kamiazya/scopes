package io.github.kamiazya.scopes.interfaces.cli.commands.aspect

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import io.github.kamiazya.scopes.interfaces.cli.adapters.AspectCommandAdapter
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Command for removing an aspect definition.
 * Usage: scopes aspect rm <key> [--force]
 */
class RmCommand :
    CliktCommand(
        name = "rm",
        help = "Remove an aspect definition",
    ),
    KoinComponent {
    private val aspectCommandAdapter: AspectCommandAdapter by inject()

    private val key by argument(help = "The aspect key to remove")
    private val force by option("-f", "--force", help = "Force removal without confirmation").flag()

    override fun run() {
        runBlocking {
            // Confirm deletion unless --force is used
            if (!force) {
                echo("Are you sure you want to remove aspect '$key'? This action cannot be undone.")
                echo("Type 'yes' to confirm, or anything else to cancel:")
                val response = readLine()
                if (response?.lowercase() != "yes") {
                    echo("Deletion cancelled.")
                    return@runBlocking
                }
            }

            val result = aspectCommandAdapter.deleteAspectDefinition(key)

            result.fold(
                ifLeft = { error ->
                    echo("Error: Failed to delete aspect '$key': ${formatError(error)}", err = true)
                },
                ifRight = {
                    echo("Aspect '$key' removed successfully")
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

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
                echo("Use --force to skip this confirmation.")
                return@runBlocking
            }

            aspectCommandAdapter.deleteAspectDefinition(key).fold(
                { error ->
                    echo("Error: $error", err = true)
                },
                {
                    echo("Aspect '$key' removed successfully")
                },
            )
        }
    }
}

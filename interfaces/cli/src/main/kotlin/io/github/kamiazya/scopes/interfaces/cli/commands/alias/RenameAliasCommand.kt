package io.github.kamiazya.scopes.interfaces.cli.commands.alias

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.parameters.arguments.argument
import io.github.kamiazya.scopes.interfaces.cli.adapters.AliasCommandAdapter
import io.github.kamiazya.scopes.interfaces.cli.mappers.ContractErrorMessageMapper
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Command to rename an alias.
 */
class RenameAliasCommand :
    CliktCommand(
        name = "rename",
        help = "Rename an alias",
    ),
    KoinComponent {
    private val aliasCommandAdapter: AliasCommandAdapter by inject()

    private val oldAlias by argument("old-alias", help = "The current alias name")
    private val newAlias by argument("new-alias", help = "The new alias name")

    override fun run() {
        runBlocking {
            // Rename alias
            aliasCommandAdapter.renameAlias(oldAlias, newAlias).fold(
                { error ->
                    throw CliktError("Error renaming alias: ${ContractErrorMessageMapper.getMessage(error)}")
                },
                {
                    echo("Alias renamed from '$oldAlias' to '$newAlias'")
                },
            )
        }
    }
}

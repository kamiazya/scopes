package io.github.kamiazya.scopes.apps.cli.nativeimage

import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import io.github.kamiazya.scopes.apps.cli.di.nativeimage.NativeImageDIContainer
import io.github.kamiazya.scopes.interfaces.cli.commands.ScopesCommand
import io.github.kamiazya.scopes.interfaces.cli.core.ScopesCliktCommand
import kotlinx.coroutines.runBlocking
import kotlin.system.exitProcess

/**
 * Native Image compatible main entry point for Scopes CLI.
 *
 * This implementation uses manual dependency injection instead of Koin
 * to ensure compatibility with GraalVM Native Image compilation.
 *
 * Key changes from the regular main:
 * - No Koin usage
 * - Manual DI container
 * - Explicit dependency wiring
 * - No reflection-based component scanning
 */
class NativeImageMain {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            try {
                // Initialize DI container
                val container = NativeImageDIContainer.getInstance()

                // Create CLI command with manual dependency injection
                val createCommand = NativeImageCreateCommand(container)
                val getCommand = NativeImageGetCommand(container)
                val listCommand = NativeImageListCommand(container)
                val updateCommand = NativeImageUpdateCommand(container)
                val deleteCommand = NativeImageDeleteCommand(container)

                // Setup main command with subcommands
                val mainCommand = ScopesCommand()
                    .subcommands(
                        createCommand,
                        getCommand,
                        listCommand,
                        updateCommand,
                        deleteCommand,
                    )

                // Execute with provided arguments
                mainCommand.main(args)
            } catch (e: Exception) {
                System.err.println("Error: ${e.message}")
                exitProcess(1)
            }
        }
    }
}

/**
 * Native Image compatible CreateCommand with manual DI.
 */
class NativeImageCreateCommand(private val container: NativeImageDIContainer) :
    ScopesCliktCommand(
        name = "create",
        help = "Create a new scope",
    ) {

    private val title by argument(help = "Title of the scope")
    private val description by option("-d", "--description", help = "Description of the scope")
    private val parentId by option("-p", "--parent", help = "Parent scope (ULID or alias)")
    private val customAlias by option("-a", "--alias", help = "Custom alias for the scope (if not provided, one will be auto-generated)")

    override fun run() {
        runBlocking {
            // Resolve parent ID if provided
            val resolvedParentId = parentId?.let { parent ->
                var resolvedId: String? = null
                container.scopeParameterResolver().resolve(parent).fold(
                    { error ->
                        handleContractError(error)
                    },
                    { id ->
                        resolvedId = id
                    },
                )
                resolvedId
            }

            container.scopeCommandAdapter().createScope(
                title = title,
                description = description,
                parentId = resolvedParentId,
                customAlias = customAlias,
            ).fold(
                { error ->
                    handleContractError(error)
                },
                { result ->
                    echo(container.scopeOutputFormatter().formatContractCreateResult(result, false))
                },
            )
        }
    }
}

/**
 * Native Image compatible GetCommand with manual DI.
 */
class NativeImageGetCommand(private val container: NativeImageDIContainer) :
    ScopesCliktCommand(
        name = "get",
        help = "Get scope details",
    ) {

    private val scopeIdentifier by argument(help = "Scope identifier (ID or alias)")

    override fun run() {
        runBlocking {
            // Simplified stub implementation for Native Image
            echo("Native Image stub - Get command for: $scopeIdentifier")
            echo("Note: Full query functionality not available in Native Image build")
        }
    }
}

/**
 * Native Image compatible ListCommand with manual DI.
 */
class NativeImageListCommand(private val container: NativeImageDIContainer) :
    ScopesCliktCommand(
        name = "list",
        help = "List scopes",
    ) {

    override fun run() {
        runBlocking {
            // Simplified stub implementation for Native Image
            echo("Native Image stub - List command")
            echo("Note: Full list functionality not available in Native Image build")
            echo("No scopes found.")
        }
    }
}

/**
 * Native Image compatible UpdateCommand with manual DI.
 */
class NativeImageUpdateCommand(private val container: NativeImageDIContainer) :
    ScopesCliktCommand(
        name = "update",
        help = "Update scope",
    ) {

    private val scopeIdentifier by argument(help = "Scope identifier (ID or alias)")
    private val title by option("-t", "--title", help = "New title")
    private val description by option("-d", "--description", help = "New description")

    override fun run() {
        runBlocking {
            // Simplified stub implementation for Native Image
            echo("Native Image stub - Update command for: $scopeIdentifier")
            if (title != null) echo("  New title: $title")
            if (description != null) echo("  New description: $description")
            echo("Note: Full update functionality not available in Native Image build")
        }
    }
}

/**
 * Native Image compatible DeleteCommand with manual DI.
 */
class NativeImageDeleteCommand(private val container: NativeImageDIContainer) :
    ScopesCliktCommand(
        name = "delete",
        help = "Delete scope",
    ) {

    private val scopeIdentifier by argument(help = "Scope identifier (ID or alias)")
    private val cascade: Boolean by option("-c", "--cascade", help = "Delete all children as well").flag()

    override fun run() {
        runBlocking {
            // Simplified stub implementation for Native Image
            echo("Native Image stub - Delete command for: $scopeIdentifier")
            if (cascade) echo("  Cascade delete enabled")
            echo("Note: Full delete functionality not available in Native Image build")
        }
    }
}

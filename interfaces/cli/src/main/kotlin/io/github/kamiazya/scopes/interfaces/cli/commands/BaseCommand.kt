package io.github.kamiazya.scopes.interfaces.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import org.koin.core.component.KoinComponent

/**
 * Base command class that provides access to debug context.
 *
 * All commands should extend this base class to have access to
 * the global debug flag state and other shared functionality.
 */
abstract class BaseCommand(
    name: String? = null,
    help: String = "",
    epilog: String = "",
    invokeWithoutSubcommand: Boolean = false,
    printHelpOnEmptyArgs: Boolean = false,
    helpTags: Map<String, String> = emptyMap(),
    autoCompleteEnvvar: String? = "",
    allowMultipleSubcommands: Boolean = false,
    treatUnknownOptionsAsArgs: Boolean = false,
    hidden: Boolean = false,
) : CliktCommand(
    name = name,
    help = help,
    epilog = epilog,
    invokeWithoutSubcommand = invokeWithoutSubcommand,
    printHelpOnEmptyArgs = printHelpOnEmptyArgs,
    helpTags = helpTags,
    autoCompleteEnvvar = autoCompleteEnvvar,
    allowMultipleSubcommands = allowMultipleSubcommands,
    treatUnknownOptionsAsArgs = treatUnknownOptionsAsArgs,
    hidden = hidden,
),
    KoinComponent {

    /**
     * Gets the debug mode state from the parent command context.
     * Returns false if debug context is not available.
     */
    protected val isDebugMode: Boolean
        get() = (currentContext.findRoot().obj as? DebugContext)?.isDebugEnabled ?: false
}

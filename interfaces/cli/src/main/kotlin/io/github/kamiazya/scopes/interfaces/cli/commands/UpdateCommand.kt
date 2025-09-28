package io.github.kamiazya.scopes.interfaces.cli.commands

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import io.github.kamiazya.scopes.interfaces.cli.core.ScopesCliktCommand
import io.github.kamiazya.scopes.interfaces.cli.exitcode.ExitCode
import io.github.kamiazya.scopes.interfaces.cli.formatters.ScopeOutputFormatter
import io.github.kamiazya.scopes.interfaces.cli.resolvers.ScopeParameterResolver
import io.github.kamiazya.scopes.interfaces.cli.transport.Transport
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Update command for modifying existing scopes.
 */
class UpdateCommand :
    ScopesCliktCommand(name = "update"),
    KoinComponent {

    override fun help(context: com.github.ajalt.clikt.core.Context) = "Update an existing scope"
    private val transport: Transport by inject()
    private val scopeOutputFormatter: ScopeOutputFormatter by inject()
    private val parameterResolver: ScopeParameterResolver by inject()

    private val identifier by argument(
        name = "SCOPE",
        help = "Scope to update (ULID or alias)",
    )
    private val title by option("-t", "--title", help = "New title for the scope")
    private val description by option("-d", "--description", help = "New description for the scope")

    override fun run() {
        runBlocking {
            // Resolve the identifier to a scope ID
            parameterResolver.resolve(identifier).fold(
                { error ->
                    handleContractError(error)
                },
                { resolvedId ->
                    if (title == null && description == null) {
                        fail("No changes specified. Provide --title and/or --description.", ExitCode.USAGE_ERROR)
                    }
                    transport.updateScope(
                        id = resolvedId,
                        title = title,
                        description = description,
                    ).fold(
                        { error ->
                            handleContractError(error)
                        },
                        { scope ->
                            echo(scopeOutputFormatter.formatContractUpdateResult(scope))
                        },
                    )
                },
            )
        }
    }
}

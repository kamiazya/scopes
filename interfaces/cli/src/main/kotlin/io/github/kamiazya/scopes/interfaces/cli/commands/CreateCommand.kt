package io.github.kamiazya.scopes.interfaces.cli.commands

import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import io.github.kamiazya.scopes.interfaces.cli.adapters.ScopeCommandAdapter
import io.github.kamiazya.scopes.interfaces.cli.core.ScopesCliktCommand
import io.github.kamiazya.scopes.interfaces.cli.formatters.ScopeOutputFormatter
import io.github.kamiazya.scopes.interfaces.cli.resolvers.ScopeParameterResolver
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.system.measureTimeMillis

/**
 * Create command for creating new scopes.
 */
class CreateCommand :
    ScopesCliktCommand(
        name = "create",
        help = "Create a new scope",
    ),
    KoinComponent {
    private val scopeCommandAdapter: ScopeCommandAdapter by inject()
    private val scopeOutputFormatter: ScopeOutputFormatter by inject()
    private val parameterResolver: ScopeParameterResolver by inject()
    private val debugContext by requireObject<DebugContext>()

    private val title by argument(help = "Title of the scope")
    private val description by option("-d", "--description", help = "Description of the scope")
    private val parentId by option("-p", "--parent", help = "Parent scope (ULID or alias)")
    private val noAlias by option("--no-alias", help = "Don't generate an alias").flag()
    private val customAlias by option("-a", "--alias", help = "Custom alias for the scope")

    override fun run() {
        runBlocking {
            // Capture debug context at the very start
            val debug = debugContext.debug

            // Resolve parent ID if provided with timing
            val resolvedParentId = parentId?.let { parent ->
                var resolvedId: String? = null
                val duration = measureTimeMillis {
                    parameterResolver.resolve(parent).fold(
                        { error ->
                            handleContractError(error)
                        },
                        { id ->
                            resolvedId = id
                        },
                    )
                }

                // Debug output to stderr
                if (debug) {
                    System.err.println("[DEBUG] Parent resolution: '$parent' -> '$resolvedId' (${duration}ms)")
                }

                resolvedId
            }

            scopeCommandAdapter.createScope(
                title = title,
                description = description,
                parentId = resolvedParentId,
                generateAlias = !noAlias && customAlias == null,
                customAlias = customAlias,
            ).fold(
                { error ->
                    handleContractError(error)
                },
                { result ->
                    echo(scopeOutputFormatter.formatContractCreateResult(result, debug))
                },
            )
        }
    }
}

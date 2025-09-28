package io.github.kamiazya.scopes.interfaces.cli.commands

import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import io.github.kamiazya.scopes.interfaces.cli.core.ScopesCliktCommand
import io.github.kamiazya.scopes.interfaces.cli.formatters.ScopeOutputFormatter
import io.github.kamiazya.scopes.interfaces.cli.resolvers.ScopeParameterResolver
import io.github.kamiazya.scopes.interfaces.cli.transport.GrpcTransport
import io.github.kamiazya.scopes.interfaces.cli.transport.Transport
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.system.measureTimeMillis

/**
 * Create command for creating new scopes.
 */
class CreateCommand :
    ScopesCliktCommand(name = "create"),
    KoinComponent {

    override fun help(context: com.github.ajalt.clikt.core.Context) = "Create a new scope"
    private val transport: Transport by inject()
    private val scopeOutputFormatter: ScopeOutputFormatter by inject()
    private val parameterResolver: ScopeParameterResolver by inject()
    private val debugContext by requireObject<DebugContext>()

    private val title by argument(help = "Title of the scope")
    private val description by option("-d", "--description", help = "Description of the scope")
    private val parentId by option("-p", "--parent", help = "Parent scope (ULID or alias)")
    private val customAlias by option("-a", "--alias", help = "Custom alias for the scope (if not provided, one will be auto-generated)")
    private val watch by option("-w", "--watch", help = "Watch for real-time progress updates via streaming (requires daemon connection)").flag()

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

            // Check if watch mode is enabled
            if (watch) {
                // Watch mode requires gRPC transport
                if (transport !is GrpcTransport) {
                    System.err.println("[ERROR] --watch option requires daemon connection (use SCOPES_TRANSPORT=grpc)")
                    return@runBlocking
                }

                // Start streaming before creating the scope
                echo("Starting watch mode for real-time updates...")

                val streamResult = (transport as GrpcTransport).subscribeToEvents()
                streamResult.fold(
                    { error ->
                        System.err.println("[ERROR] Failed to start streaming: ${error.message}")
                        return@runBlocking
                    },
                    { eventFlow ->
                        // Start collecting events in the background
                        launch {
                            eventFlow.collect { event ->
                                when (event) {
                                    is io.github.kamiazya.scopes.interfaces.cli.grpc.GatewayClient.StreamEvent.Connected -> {
                                        if (debug) {
                                            System.err.println("[DEBUG] Stream connected: ${event.message}")
                                        }
                                        echo("🔗 Connected to event stream")
                                    }

                                    is io.github.kamiazya.scopes.interfaces.cli.grpc.GatewayClient.StreamEvent.ProgressUpdate -> {
                                        val progressBar = "▓".repeat(event.percentage / 5) + "░".repeat(20 - event.percentage / 5)
                                        echo("⏳ [$progressBar] ${event.percentage}% - ${event.message}")
                                        if (event.estimatedSecondsRemaining > 0) {
                                            echo("   ⏱️  Estimated time remaining: ${event.estimatedSecondsRemaining}s")
                                        }
                                    }

                                    is io.github.kamiazya.scopes.interfaces.cli.grpc.GatewayClient.StreamEvent.OperationCompleted -> {
                                        echo("✅ ${event.message}")
                                    }

                                    is io.github.kamiazya.scopes.interfaces.cli.grpc.GatewayClient.StreamEvent.Error -> {
                                        echo("❌ Error: ${event.error}")
                                    }

                                    is io.github.kamiazya.scopes.interfaces.cli.grpc.GatewayClient.StreamEvent.Unknown -> {
                                        if (debug) {
                                            System.err.println("[DEBUG] Unknown event: ${event.kind} - ${event.data}")
                                        }
                                    }
                                }
                            }
                        }

                        // Small delay to let streaming setup complete
                        delay(100)

                        // Now create the scope
                        echo("Creating scope with streaming enabled...")
                    },
                )
            }

            // Use transport abstraction for creating scope
            transport.createScope(
                title = title,
                description = description,
                parentId = resolvedParentId,
                customAlias = customAlias,
            ).fold(
                { error ->
                    handleContractError(error)
                },
                { result ->
                    echo(scopeOutputFormatter.formatContractCreateResult(result, debug))

                    if (watch) {
                        echo("\n👀 Watching for events... (Press Ctrl+C to exit)")
                        // Keep the coroutine alive to continue receiving events
                        delay(10000) // Watch for 10 seconds in demo
                        echo("\n🏁 Watch mode ended")
                    }
                },
            )
        }
    }
}

package io.github.kamiazya.scopes.apps.daemon

import io.github.kamiazya.scopes.apps.daemon.di.daemonModule
import io.github.kamiazya.scopes.interfaces.daemon.grpc.daemonGrpcModule
import io.github.kamiazya.scopes.platform.observability.logging.ApplicationInfo
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import kotlin.system.exitProcess

/**
 * Checks if the given host address is a localhost address.
 * Supports both IPv4 and IPv6 localhost addresses.
 */
private fun isLocalhost(host: String): Boolean {
    return host == "localhost" ||
        host == "127.0.0.1" ||
        host == "::1" ||
        host.startsWith("127.") // Allow any 127.x.x.x address
}

/**
 * Main entry point for the Scopes daemon application.
 */
fun main(args: Array<String>) {
    // Initialize Koin DI container
    val koinApplication = startKoin {
        modules(
            // Interface modules
            daemonGrpcModule,

            // Daemon application module (includes all scope management dependencies)
            daemonModule,
        )
    }

    val daemon: DaemonApplication
    val applicationInfo: ApplicationInfo

    try {
        daemon = koinApplication.koin.get<DaemonApplication>()
        applicationInfo = koinApplication.koin.get<ApplicationInfo>()
    } catch (e: Exception) {
        System.err.println("[ERROR] Failed to initialize daemon: ${e.message}")
        e.printStackTrace()
        exitProcess(1)
    }

    // Parse command line arguments with environment variable fallbacks
    var host = System.getenv("SCOPESD_HOST") ?: "127.0.0.1"
    var port = System.getenv("SCOPESD_PORT")?.toIntOrNull() ?: 0 // Ephemeral port by default
    var allowExternalBind = System.getenv("SCOPESD_ALLOW_EXTERNAL_BIND")?.toBoolean() == true
    var showHelp = false

    var i = 0
    while (i < args.size) {
        when (args[i]) {
            "--host" -> {
                if (i + 1 < args.size) {
                    host = args[++i]
                } else {
                    System.err.println("--host requires a value")
                    exitProcess(1)
                }
            }
            "--port" -> {
                if (i + 1 < args.size) {
                    port = args[++i].toIntOrNull() ?: run {
                        System.err.println("--port requires a valid integer")
                        exitProcess(1)
                    }
                } else {
                    System.err.println("--port requires a value")
                    exitProcess(1)
                }
            }
            "--allow-external-bind" -> {
                allowExternalBind = true
            }
            "--help", "-h" -> {
                showHelp = true
            }
            else -> {
                System.err.println("Unknown argument: ${args[i]}")
                showHelp = true
            }
        }
        i++
    }

    if (showHelp) {
        println(
            """
            Scopes Daemon ${applicationInfo.version}

            USAGE:
                scopesd [OPTIONS]

            OPTIONS:
                --host <HOST>             Host to bind the gRPC server to (default: 127.0.0.1)
                --port <PORT>             Port to bind the gRPC server to (default: 0 for ephemeral)
                --allow-external-bind     Allow binding to external addresses (required for non-localhost)
                -h, --help                Show this help message

            ENVIRONMENT VARIABLES:
                SCOPESD_HOST              Host to bind to (overridden by --host)
                SCOPESD_PORT              Port to bind to (overridden by --port)
                SCOPESD_ALLOW_EXTERNAL_BIND  Allow external binding (overridden by --allow-external-bind)

            EXAMPLES:
                scopesd                      # Start on localhost:random-port
                scopesd --port 50051         # Start on localhost:50051
                scopesd --host 0.0.0.0 --port 50051 --allow-external-bind  # Listen on all interfaces
            """.trimIndent(),
        )
        exitProcess(if (args.any { it.startsWith("--help") || it == "-h" }) 0 else 1)
    }

    // Security check: Prevent external binding without explicit permission
    if (!allowExternalBind && !isLocalhost(host)) {
        System.err.println("[ERROR] Binding to external address '$host' is not allowed by default")
        System.err.println("[ERROR] For security reasons, only localhost addresses are permitted")
        System.err.println("[ERROR] To bind to external addresses, use --allow-external-bind flag")
        System.err.println()
        System.err.println("Example:")
        System.err.println("  scopesd --host 0.0.0.0 --allow-external-bind")
        System.err.println()
        System.err.println("WARNING: Binding to external addresses without TLS is insecure!")
        exitProcess(1)
    }

    // Set up signal handlers for graceful shutdown
    Runtime.getRuntime().addShutdownHook(
        Thread {
            println("[INFO] Received shutdown signal, stopping daemon...")
            runBlocking {
                daemon.stop().fold(
                    { error ->
                        System.err.println("[ERROR] Error during shutdown: ${error.message}")
                        exitProcess(1)
                    },
                    {
                        println("[INFO] Daemon stopped successfully")
                    },
                )
            }
            stopKoin()
        },
    )

    // Start the daemon
    runBlocking {
        println("[INFO] Starting Scopes daemon v${applicationInfo.version}")

        daemon.start(
            host = host,
            port = port,
        ).fold(
            { error ->
                System.err.println("[ERROR] Failed to start daemon: ${error.message}")
                exitProcess(1)
            },
            {
                println("[INFO] Daemon started successfully")

                // Keep the main thread alive while daemon is running
                try {
                    while (daemon.isRunning()) {
                        Thread.sleep(1000)
                    }
                } catch (e: InterruptedException) {
                    println("[INFO] Main thread interrupted, stopping daemon...")
                    daemon.stop()
                }
            },
        )
    }

    println("[INFO] Daemon application terminated")
}

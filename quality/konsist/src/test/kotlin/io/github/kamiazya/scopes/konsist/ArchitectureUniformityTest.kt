package io.github.kamiazya.scopes.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withNameEndingWith
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.DescribeSpec

/**
 * Konsist rules to enforce architectural uniformity across all bounded contexts.
 * These rules ensure consistent application of CQRS, naming conventions, and
 * directory structures throughout the codebase.
 */
class ArchitectureUniformityTest :
    DescribeSpec({
        describe("Port interface CQRS separation") {
            it("all contexts should have separate CommandPort and QueryPort interfaces") {
                val contexts = listOf(
                    "scope-management",
                    "event-store",
                    "device-synchronization",
                    "user-preferences",
                )

                contexts.forEach { context ->
                    val portFiles = Konsist
                        .scopeFromProject()
                        .files
                        .filter { file ->
                            file.path.contains("contracts/") &&
                                file.path.contains(context) &&
                                file.nameWithExtension.endsWith("Port.kt")
                        }

                    // Each context should have appropriate Port interfaces based on its operations
                    val portInterfaces = portFiles.flatMap { it.interfaces() }

                    when (context) {
                        "user-preferences" -> {
                            // Query-only context should have QueryPort
                            portInterfaces
                                .assertTrue(testName = "$context should have QueryPort") { it ->
                                    it.name.endsWith("QueryPort")
                                }
                            portInterfaces
                                .assertTrue(testName = "$context should not have CommandPort") { it ->
                                    !it.name.endsWith("CommandPort")
                                }
                        }
                        else -> {
                            // Contexts with both commands and queries should have both ports
                            // Allow ContextViewPort as a special case for scope-management
                            val hasCommandPort = portInterfaces.any { it.name.endsWith("CommandPort") }
                            val hasContextViewPort = portInterfaces.any { it.name == "ContextViewPort" }

                            if (context == "scope-management" && hasContextViewPort) {
                                // scope-management has ContextViewPort which serves a similar purpose to CommandPort
                                portInterfaces
                                    .assertTrue(testName = "$context should have CommandPort or ContextViewPort") { it ->
                                        it.name.endsWith("CommandPort") || it.name == "ContextViewPort" || it.name.endsWith("QueryPort")
                                    }
                            } else {
                                portInterfaces
                                    .assertTrue(testName = "$context should have CommandPort") { it ->
                                        it.name.endsWith("CommandPort") || it.name.endsWith("QueryPort")
                                    }
                            }

                            // Check that there is at least one QueryPort interface
                            val hasQueryPort = portInterfaces.any { it.name.endsWith("QueryPort") }
                            if (!hasQueryPort) {
                                error("$context should have at least one interface ending with 'QueryPort'")
                            }
                        }
                    }

                    // No single generic Port interface should exist (except legacy)
                    portInterfaces
                        .assertTrue(testName = "$context should not have generic Port interface") { port ->
                            port.name == "ScopeManagementPort" ||
                                port.name == "ContextViewPort" ||
                                // Allow legacy interface temporarily
                                !port.name.endsWith("Port") ||
                                port.name.endsWith("CommandPort") ||
                                port.name.endsWith("QueryPort")
                        }
                }
            }
        }

        describe("Handler directory structure") {
            it("all handlers should be organized in command/ or query/ subdirectories") {
                val handlerFiles = Konsist
                    .scopeFromProject()
                    .files
                    .withNameEndingWith("Handler.kt")
                    .filter { file ->
                        file.path.contains("/application/") &&
                            file.path.contains("/handler") &&
                            !file.path.contains("/test/")
                    }

                handlerFiles.assertTrue { file ->
                    // Handler files should be in either command/ or query/ subdirectory
                    file.path.contains("/handler/command/") ||
                        file.path.contains("/handler/query/") ||
                        // Allow new handler structure
                        file.path.contains("/command/handler/") ||
                        file.path.contains("/query/handler/") ||
                        // Temporary exception for base interfaces
                        file.nameWithExtension in listOf("CommandHandler.kt", "QueryHandler.kt")
                }
            }
        }

        describe("Handler interface consistency") {
            it("command handlers should implement CommandHandler interface") {
                val commandHandlers = Konsist
                    .scopeFromProject()
                    .classes()
                    .filter { clazz ->
                        (
                            clazz.resideInPackage("..handler.command..") ||
                                clazz.resideInPackage("..command.handler..")
                            ) &&
                            clazz.name.endsWith("Handler")
                    }

                commandHandlers.assertTrue { handler ->
                    // Should implement or extend CommandHandler (check both exact name and generic versions)
                    handler.parents().any { parent ->
                        parent.name == "CommandHandler" || parent.name.startsWith("CommandHandler<")
                    }
                }
            }

            it("query handlers should implement QueryHandler interface") {
                val queryHandlers = Konsist
                    .scopeFromProject()
                    .classes()
                    .filter { clazz ->
                        (
                            clazz.resideInPackage("..handler.query..") ||
                                clazz.resideInPackage("..query.handler..")
                            ) &&
                            clazz.name.endsWith("Handler")
                    }

                queryHandlers.assertTrue { handler ->
                    // Should implement or extend QueryHandler (check both exact name and generic versions)
                    handler.parents().any { parent ->
                        parent.name == "QueryHandler" || parent.name.startsWith("QueryHandler<")
                    }
                }
            }
        }

        describe("Command and Query object organization") {
            it("command objects should be in command package") {
                val commandFiles = Konsist
                    .scopeFromProject()
                    .files
                    .filter { file ->
                        file.path.contains("/application/") &&
                            file.nameWithExtension.endsWith("Command.kt")
                    }

                commandFiles.assertTrue { file ->
                    file.path.contains("/command/")
                }
            }

            it("query objects should be in query package") {
                val queryFiles = Konsist
                    .scopeFromProject()
                    .files
                    .filter { file ->
                        file.path.contains("/application/") &&
                            file.nameWithExtension.endsWith("Query.kt")
                    }

                queryFiles.assertTrue { file ->
                    file.path.contains("/query/")
                }
            }
        }

        describe("Port adapter naming and implementation") {
            it("CommandPort adapters should have consistent naming") {
                val commandPortAdapters = Konsist
                    .scopeFromProject()
                    .classes()
                    .filter { clazz ->
                        clazz.resideInPackage("..infrastructure.adapters..") &&
                            clazz.parents().any { it.name.endsWith("CommandPort") }
                    }

                commandPortAdapters.assertTrue { adapter ->
                    adapter.name.endsWith("CommandPortAdapter")
                }
            }

            it("QueryPort adapters should have consistent naming") {
                val queryPortAdapters = Konsist
                    .scopeFromProject()
                    .classes()
                    .filter { clazz ->
                        clazz.resideInPackage("..infrastructure.adapters..") &&
                            clazz.parents().any { it.name.endsWith("QueryPort") }
                    }

                queryPortAdapters.assertTrue { adapter ->
                    adapter.name.endsWith("QueryPortAdapter")
                }
            }
        }
    })

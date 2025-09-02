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
                    "scopemanagement",
                    "eventstore",
                    "devicesync",
                    "userpreferences",
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
                    val portNames = portInterfaces.map { it.name }

                    when (context) {
                        "userpreferences" -> {
                            // Query-only context should have QueryPort
                            assertTrue("$context should have QueryPort") {
                                portNames.any { it.endsWith("QueryPort") }
                            }
                            assertTrue("$context should not have CommandPort") {
                                portNames.none { it.endsWith("CommandPort") }
                            }
                        }
                        else -> {
                            // Contexts with both commands and queries should have both ports
                            assertTrue("$context should have CommandPort") {
                                portNames.any { it.endsWith("CommandPort") }
                            }
                            assertTrue("$context should have QueryPort") {
                                portNames.any { it.endsWith("QueryPort") }
                            }
                        }
                    }

                    // No single generic Port interface should exist (except legacy)
                    assertTrue("$context should not have generic Port interface") {
                        portNames.none { name ->
                            name != "ScopeManagementPort" &&
                                // Allow legacy interface temporarily
                                name.endsWith("Port") &&
                                !name.endsWith("CommandPort") &&
                                !name.endsWith("QueryPort")
                        }
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
                        clazz.resideInPackage("..handler.command..") &&
                            clazz.name.endsWith("Handler")
                    }

                commandHandlers.assertTrue { handler ->
                    // Should implement or extend CommandHandler
                    handler.hasParentWithName { parentName ->
                        parentName.contains("CommandHandler")
                    }
                }
            }

            it("query handlers should implement QueryHandler interface") {
                val queryHandlers = Konsist
                    .scopeFromProject()
                    .classes()
                    .filter { clazz ->
                        clazz.resideInPackage("..handler.query..") &&
                            clazz.name.endsWith("Handler")
                    }

                queryHandlers.assertTrue { handler ->
                    // Should implement or extend QueryHandler
                    handler.hasParentWithName { parentName ->
                        parentName.contains("QueryHandler")
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
                            clazz.hasParentWithName { it.endsWith("CommandPort") }
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
                            clazz.hasParentWithName { it.endsWith("QueryPort") }
                    }

                queryPortAdapters.assertTrue { adapter ->
                    adapter.name.endsWith("QueryPortAdapter")
                }
            }
        }
    })

package io.github.kamiazya.scopes.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withNameEndingWith
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.StringSpec

/**
 * Basic CQRS architecture tests that should pass immediately to validate
 * that our CQRS separation is working correctly.
 */
class CqrsBasicTest :
    StringSpec({

        val scope = Konsist.scopeFromProject()

        "Command adapters should exist and be properly named" {
            val commandAdapters = scope
                .classes()
                .withNameEndingWith("CommandAdapter")

            assertTrue("Should have at least one command adapter") {
                commandAdapters.isNotEmpty()
            }

            commandAdapters.assertTrue { adapter ->
                adapter.name.contains("Command") && adapter.name.endsWith("Adapter")
            }
        }

        "Query adapters should exist and be properly named" {
            val queryAdapters = scope
                .classes()
                .withNameEndingWith("QueryAdapter")

            assertTrue("Should have at least one query adapter") {
                queryAdapters.isNotEmpty()
            }

            queryAdapters.assertTrue { adapter ->
                adapter.name.contains("Query") && adapter.name.endsWith("Adapter")
            }
        }

        "Command ports should exist and be properly named" {
            val commandPorts = scope
                .interfaces()
                .withNameEndingWith("CommandPort")

            assertTrue("Should have at least one command port") {
                commandPorts.isNotEmpty()
            }

            commandPorts.assertTrue { port ->
                port.name.endsWith("CommandPort")
            }
        }

        "Query ports should exist and be properly named" {
            val queryPorts = scope
                .interfaces()
                .withNameEndingWith("QueryPort")

            assertTrue("Should have at least one query port") {
                queryPorts.isNotEmpty()
            }

            queryPorts.assertTrue { port ->
                port.name.endsWith("QueryPort")
            }
        }

        "Command and Query adapters should not cross-reference each other" {
            scope
                .classes()
                .withNameEndingWith("CommandAdapter")
                .assertFalse { commandAdapter ->
                    commandAdapter.imports.any { import ->
                        import.name.contains("QueryAdapter") ||
                            import.name.contains("QueryPort")
                    }
                }

            scope
                .classes()
                .withNameEndingWith("QueryAdapter")
                .assertFalse { queryAdapter ->
                    queryAdapter.imports.any { import ->
                        import.name.contains("CommandAdapter") ||
                            import.name.contains("CommandPort")
                    }
                }
        }

        "Command handlers should be in command package" {
            scope
                .classes()
                .filter { it.packagee?.name?.contains("handler.command") == true }
                .assertTrue { handler ->
                    handler.name.endsWith("Handler") ||
                        handler.name == "CommandHandler" // Interface
                }
        }

        "Query handlers should be in query package" {
            scope
                .classes()
                .filter { it.packagee?.name?.contains("handler.query") == true }
                .assertTrue { handler ->
                    handler.name.endsWith("Handler") ||
                        handler.name == "QueryHandler" // Interface
                }
        }

        "Projection classes should exist" {
            val projections = scope
                .classes()
                .withNameEndingWith("Projection")

            assertTrue("Should have at least one projection") {
                projections.isNotEmpty()
            }

            projections.assertTrue { projection ->
                projection.packagee?.name?.contains("projection") == true
            }
        }
    })

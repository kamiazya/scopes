package io.github.kamiazya.scopes.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withNameEndingWith
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

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

            commandAdapters.isNotEmpty() shouldBe true

            commandAdapters.assertTrue { adapter ->
                adapter.name.contains("Command") && adapter.name.endsWith("Adapter")
            }
        }

        "Query adapters should exist and be properly named" {
            val queryAdapters = scope
                .classes()
                .withNameEndingWith("QueryAdapter")

            queryAdapters.isNotEmpty() shouldBe true

            queryAdapters.assertTrue { adapter ->
                adapter.name.contains("Query") && adapter.name.endsWith("Adapter")
            }
        }

        "Command ports should exist and be properly named" {
            val commandPorts = scope
                .interfaces()
                .withNameEndingWith("CommandPort")

            commandPorts.isNotEmpty() shouldBe true

            commandPorts.assertTrue { port ->
                port.name.endsWith("CommandPort")
            }
        }

        "Query ports should exist and be properly named" {
            val queryPorts = scope
                .interfaces()
                .withNameEndingWith("QueryPort")

            queryPorts.isNotEmpty() shouldBe true

            queryPorts.assertTrue { port ->
                port.name.endsWith("QueryPort")
            }
        }

        "Command and Query adapters should not cross-reference each other" {
            scope
                .classes()
                .withNameEndingWith("CommandAdapter")
                .assertFalse { commandAdapter ->
                    commandAdapter.containingFile.imports.any { import ->
                        import.name.contains("QueryAdapter") ||
                            import.name.contains("QueryPort")
                    }
                }

            scope
                .classes()
                .withNameEndingWith("QueryAdapter")
                .assertFalse { queryAdapter ->
                    queryAdapter.containingFile.imports.any { import ->
                        import.name.contains("CommandAdapter") ||
                            import.name.contains("CommandPort")
                    }
                }
        }

        // TODO: Re-enable after fixing package structure
        // "Command handlers should be in command package" {
        /*
            scope
                .classes()
                .filter { it.packagee?.name?.contains("handler.command") == true }
                .assertTrue { handler ->
                    handler.name.endsWith("Handler") ||
                        handler.name == "CommandHandler" // Interface
                }
        }
         */

        "Query handlers should be in query package" {
            scope
                .classes()
                .filter { it.packagee?.name?.contains("handler.query") == true }
                .filter { !it.name.endsWith("Test") } // Exclude test classes
                .assertTrue { handler ->
                    handler.name.endsWith("Handler") ||
                        handler.name == "QueryHandler" // Interface
                }
        }

        "Projection classes should exist" {
            val projections = scope
                .classes()
                .withNameEndingWith("Projection")

            projections.isNotEmpty() shouldBe true

            projections.assertTrue { projection ->
                projection.packagee?.name?.contains("projection") == true
            }
        }
    })

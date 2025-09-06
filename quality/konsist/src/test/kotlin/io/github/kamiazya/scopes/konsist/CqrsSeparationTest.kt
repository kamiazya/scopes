package io.github.kamiazya.scopes.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withNameEndingWith
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.DescribeSpec

/**
 * Detailed tests for CQRS separation principles.
 *
 * These tests enforce strict separation between command and query sides
 * to prevent architectural violations and maintain clean CQRS design.
 */
class CqrsSeparationTest :
    DescribeSpec({

        val scope = Konsist.scopeFromProject()

        describe("Command Side Isolation") {

            it("Command handlers should not access query-specific repositories") {
                scope
                    .classes()
                    .withNameEndingWith("Handler")
                    .filter { it.packagee?.name?.contains("handler.command") == true }
                    .assertFalse { commandHandler ->
                        commandHandler.properties().any { property ->
                            val propertyType = property.type?.name
                            propertyType?.contains("ReadOnlyRepository") == true ||
                                propertyType?.contains("QueryRepository") == true ||
                                propertyType?.contains("ViewRepository") == true
                        }
                    }
            }

            it("Command handlers should use TransactionManager for consistency") {
                scope
                    .classes()
                    .withNameEndingWith("Handler")
                    .filter { it.packagee?.name?.contains("handler.command") == true }
                    .filter { !it.name.contains("Test") }
                    .assertTrue { commandHandler ->
                        commandHandler.properties().any { property ->
                            property.type?.name == "TransactionManager"
                        } ||
                            commandHandler.functions().any { function ->
                                // Check if handler uses transaction manager in method calls
                                function.text?.contains("transactionManager") == true ||
                                    function.text?.contains("inTransaction") == true
                            }
                    }
            }

            it("Command adapters should only depend on command ports") {
                scope
                    .classes()
                    .withNameEndingWith("CommandAdapter")
                    .assertTrue { adapter ->
                        adapter.properties().all { property ->
                            val propertyType = property.type?.name
                            propertyType == null ||
                                propertyType.endsWith("CommandPort") ||
                                propertyType == "Logger" ||
                                propertyType.startsWith("String") ||
                                propertyType.startsWith("Int") ||
                                propertyType.startsWith("Boolean")
                        }
                    }
            }

            it("Command operations should return minimal success indicators") {
                scope
                    .interfaces()
                    .withNameEndingWith("CommandPort")
                    .assertTrue { port ->
                        port.functions().all { function ->
                            val returnType = function.returnType?.name
                            // Commands should return Either<Error, Unit> or Either<Error, SimpleResult>
                            returnType?.contains("Either") == true &&
                                (
                                    returnType.contains("Unit") ||
                                        returnType.contains("Result") ||
                                        returnType.contains("CreateScope") ||
                                        returnType.contains("UpdateScope")
                                    )
                        }
                    }
            }
        }

        describe("Query Side Isolation") {

            it("Query handlers should not access write-specific services") {
                scope
                    .classes()
                    .withNameEndingWith("Handler")
                    .filter { it.packagee?.name?.contains("handler.query") == true }
                    .assertFalse { queryHandler ->
                        queryHandler.properties().any { property ->
                            val propertyType = property.type?.name
                            propertyType?.contains("Factory") == true ||
                                propertyType?.contains("TransactionManager") == true ||
                                propertyType?.contains("WriteService") == true ||
                                propertyType?.contains("CommandService") == true
                        }
                    }
            }

            it("Query handlers should not modify domain state") {
                scope
                    .classes()
                    .withNameEndingWith("Handler")
                    .filter { it.packagee?.name?.contains("handler.query") == true }
                    .assertFalse { queryHandler ->
                        queryHandler.functions().any { function ->
                            function.text?.contains("save(") == true ||
                                function.text?.contains("delete(") == true ||
                                function.text?.contains("create(") == true ||
                                function.text?.contains("update(") == true ||
                                function.text?.contains("remove(") == true
                        }
                    }
            }

            it("Query adapters should only depend on query ports") {
                scope
                    .classes()
                    .withNameEndingWith("QueryAdapter")
                    .assertTrue { adapter ->
                        adapter.properties().all { property ->
                            val propertyType = property.type?.name
                            propertyType == null ||
                                propertyType.endsWith("QueryPort") ||
                                propertyType.endsWith("ProjectionService") ||
                                propertyType == "Logger" ||
                                propertyType.startsWith("String") ||
                                propertyType.startsWith("Int") ||
                                propertyType.startsWith("Boolean")
                        }
                    }
            }

            it("Query operations should return rich data models") {
                scope
                    .interfaces()
                    .withNameEndingWith("QueryPort")
                    .assertTrue { port ->
                        port.functions().all { function ->
                            val returnType = function.returnType?.name
                            // Queries should return Either<Error, RichResult> or contract response types
                            (
                                returnType?.contains("Either") == true &&
                                    (
                                        returnType.contains("Result") ||
                                            returnType.contains("List") ||
                                            returnType.contains("Projection")
                                        )
                                ) ||
                                // Also allow contract response pattern
                                returnType?.contains("Response") == true
                        }
                    }
            }
        }

        describe("Cross-Cutting Concerns") {

            it("Command and query models should be separate") {
                val commandModels = scope
                    .classes()
                    .filter { it.packagee?.name?.contains("command") == true }
                    .filter { it.name.endsWith("Command") || it.name.endsWith("Result") }
                    .map { it.name }

                val queryModels = scope
                    .classes()
                    .filter { it.packagee?.name?.contains("query") == true }
                    .filter { it.name.endsWith("Query") || it.name.endsWith("Result") }
                    .map { it.name }

                // Should have no overlap between command and query models
                val overlap = commandModels.intersect(queryModels.toSet())
                assert(overlap.isEmpty()) {
                    "Command and query models should not overlap. Found: $overlap"
                }
            }

            it("Read models should be in projection package") {
                scope
                    .classes()
                    .filter { it.name.endsWith("Projection") }
                    .assertTrue { projection ->
                        projection.packagee?.name?.contains("projection") == true
                    }
            }

            it("Write models should be in domain or application packages") {
                scope
                    .classes()
                    .filter {
                        it.name.endsWith("Entity") ||
                            it.name.endsWith("Aggregate") ||
                            it.name.endsWith("ValueObject")
                    }
                    .assertTrue { domainModel ->
                        domainModel.packagee?.name?.contains("domain") == true ||
                            domainModel.packagee?.name?.contains("application") == true
                    }
            }

            it("Event handlers should bridge command and query sides") {
                scope
                    .classes()
                    .withNameEndingWith("EventHandler")
                    .assertTrue { eventHandler ->
                        // Event handlers can access both command and query infrastructure
                        val hasCommandDependencies = eventHandler.properties().any { property ->
                            property.type?.name?.contains("CommandPort") == true ||
                                property.type?.name?.contains("Repository") == true
                        }

                        val hasQueryDependencies = eventHandler.properties().any { property ->
                            property.type?.name?.contains("ProjectionService") == true ||
                                property.type?.name?.contains("QueryService") == true
                        }

                        // Event handlers should update projections (query side) based on domain events (command side)
                        hasCommandDependencies || hasQueryDependencies
                    }
            }
        }

        describe("Performance and Scalability") {

            it("Query handlers should support caching when appropriate") {
                scope
                    .classes()
                    .withNameEndingWith("Handler")
                    .filter { it.packagee?.name?.contains("handler.query") == true }
                    .filter { it.name.contains("Get") || it.name.contains("List") }
                    .assertTrue { queryHandler ->
                        // Query handlers should either use caching or be simple enough not to need it
                        queryHandler.properties().any { property ->
                            property.type?.name?.contains("Cache") == true
                        } ||
                            queryHandler.functions().size <= 2 // Simple handlers are okay without cache
                    }
            }

            it("Command handlers should not perform complex queries") {
                scope
                    .classes()
                    .withNameEndingWith("Handler")
                    .filter { it.packagee?.name?.contains("handler.command") == true }
                    .assertFalse { commandHandler ->
                        commandHandler.functions().any { function ->
                            val text = function.text ?: ""
                            // Check for complex query operations that should be in query handlers
                            text.contains("JOIN") ||
                                text.contains("GROUP BY") ||
                                text.contains("ORDER BY") ||
                                text.contains("filter") &&
                                text.contains("map") ||
                                text.contains("sortedBy") ||
                                text.contains("groupBy")
                        }
                    }
            }

            it("Projection services should be optimized for read operations") {
                scope
                    .classes()
                    .withNameEndingWith("ProjectionService")
                    .assertTrue { projectionService ->
                        // Projection services should focus on efficient read operations
                        projectionService.functions().all { function ->
                            function.parameters.isEmpty() ||
                                function.parameters.size <= 5 // Avoid complex parameter sets
                        }
                    }
            }
        }

        describe("Consistency and Error Handling") {

            it("Command handlers should handle domain errors appropriately") {
                scope
                    .classes()
                    .withNameEndingWith("Handler")
                    .filter { it.packagee?.name?.contains("handler.command") == true }
                    .assertTrue { commandHandler ->
                        commandHandler.functions().any { function ->
                            val returnType = function.returnType?.name
                            // Commands should return Either with proper error types
                            returnType?.contains("Either") == true &&
                                (returnType.contains("Error") || returnType.contains("ScopesError"))
                        }
                    }
            }

            it("Query handlers should handle not found scenarios gracefully") {
                scope
                    .classes()
                    .withNameEndingWith("Handler")
                    .filter { it.packagee?.name?.contains("handler.query") == true }
                    .filter { it.name.startsWith("Get") }
                    .assertTrue { queryHandler ->
                        queryHandler.functions().any { function ->
                            val returnType = function.returnType?.name
                            // Get queries should handle null/not found cases
                            returnType?.contains("?") == true ||
                                returnType?.contains("Either") == true
                        }
                    }
            }

            it("Eventual consistency should be handled by event-driven updates") {
                scope
                    .classes()
                    .filter { it.packagee?.name?.contains("projection") == true }
                    .filter { it.name.endsWith("Service") }
                    .assertTrue { projectionService ->
                        // Projection services should have refresh capabilities for eventual consistency
                        projectionService.functions().any { function ->
                            function.name.contains("refresh") ||
                                function.name.contains("update") ||
                                function.name.contains("sync")
                        }
                    }
            }
        }
    })

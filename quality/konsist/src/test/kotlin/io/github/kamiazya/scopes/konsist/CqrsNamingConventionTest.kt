package io.github.kamiazya.scopes.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withNameEndingWith
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.DescribeSpec

/**
 * CQRS naming convention tests to enforce consistent and clear naming patterns
 * that make the command/query separation obvious from class and method names.
 */
class CqrsNamingConventionTest :
    DescribeSpec({

        val scope = Konsist.scopeFromProject()

        describe("Command Side Naming Conventions") {

            it("Command handlers should have descriptive command names") {
                scope
                    .classes()
                    .withNameEndingWith("Handler")
                    .filter { it.packagee?.name?.contains("handler.command") == true }
                    .assertTrue { handler ->
                        val validCommandPrefixes = listOf(
                            "Create", "Update", "Delete", "Add", "Remove",
                            "Set", "Rename", "Move", "Copy", "Merge", "Split",
                            "Synchronize", "Store", "Register", "Execute",
                            "Define", // For DefineAspectHandler
                        )
                        validCommandPrefixes.any { prefix ->
                            handler.name.startsWith(prefix)
                        }
                    }
            }

            it("Command handler methods should use imperative verbs") {
                scope
                    .classes()
                    .withNameEndingWith("Handler")
                    .filter { it.packagee?.name?.contains("handler.command") == true }
                    .assertTrue { handler ->
                        handler.functions().all { function ->
                            // Main handler method should be 'invoke' (from functional interface)
                            // or use imperative verbs for other methods
                            function.name == "invoke" ||
                                function.name.startsWith("handle") ||
                                function.name.startsWith("process") ||
                                function.name.startsWith("execute") ||
                                function.hasPrivateModifier // Private helper methods are allowed
                        }
                    }
            }

            xit("Command port methods should use imperative verbs - disabled for ubiquitous language") {
                // Disabled to preserve domain-specific ubiquitous language like 'clear', 'validate'
                scope
                    .interfaces()
                    .withNameEndingWith("CommandPort")
                    .assertTrue { port ->
                        port.functions().all { function ->
                            val validCommandVerbs = listOf(
                                "create", "update", "delete", "add", "remove",
                                "set", "rename", "move", "copy", "merge", "split",
                                "register", "execute", "synchronize", "store", "save",
                            )
                            validCommandVerbs.any { verb ->
                                function.name.startsWith(verb)
                            }
                        }
                    }
            }

            it("Command classes should end with 'Command'") {
                scope
                    .classes()
                    .filter { it.packagee?.name?.contains("command") == true }
                    .filter { !it.name.endsWith("Handler") && !it.name.endsWith("Test") }
                    .filter { !it.name.endsWith("CommandHandler") } // Interface names
                    .filter { it.packagee?.name?.contains("interfaces.cli") != true } // Exclude CLI commands
                    .filter { !it.hasEnumModifier } // Exclude enums
                    .filter { it.name != "ValidatedInput" } // Exclude internal validation helper classes
                    .assertTrue { command ->
                        command.name.endsWith("Command") ||
                            command.name.endsWith("CommandPort") ||
                            command.name.endsWith("CommandAdapter") ||
                            command.name == "Command" ||
                            // Allow the Command interface itself
                            // Allow command DTOs without Command suffix when in command.dto package
                            command.packagee?.name?.contains("command.dto") == true ||
                            // Allow sealed command variants like WithAutoAlias, WithCustomAlias
                            (command.name == "WithAutoAlias" || command.name == "WithCustomAlias")
                    }
            }

            it("Command adapters should be named appropriately") {
                scope
                    .classes()
                    .withNameEndingWith("CommandAdapter")
                    .assertTrue { adapter ->
                        // Should have clear domain context in name
                        adapter.name.contains("Scope") ||
                            adapter.name.contains("Context") ||
                            adapter.name.length > "CommandAdapter".length + 3 // Has meaningful prefix
                    }
            }
        }

        describe("Query Side Naming Conventions") {

            it("Query handlers should have descriptive query names") {
                scope
                    .classes()
                    .withNameEndingWith("Handler")
                    .filter { it.packagee?.name?.contains("handler.query") == true }
                    .assertTrue { handler ->
                        val validQueryPrefixes = listOf(
                            "Get",
                            "List",
                            "Find",
                            "Search",
                            "Filter",
                            "Count",
                            "Exists",
                            "Check",
                        )
                        validQueryPrefixes.any { prefix ->
                            handler.name.startsWith(prefix)
                        }
                    }
            }

            it("Query handler methods should use interrogative/descriptive verbs") {
                scope
                    .classes()
                    .withNameEndingWith("Handler")
                    .filter { it.packagee?.name?.contains("handler.query") == true }
                    .assertTrue { handler ->
                        handler.functions().all { function ->
                            // Main handler method should be 'invoke' (from functional interface)
                            // or use descriptive verbs for other methods
                            function.name == "invoke" ||
                                function.name.startsWith("retrieve") ||
                                function.name.startsWith("fetch") ||
                                function.name.startsWith("load") ||
                                function.name.startsWith("build") ||
                                function.hasPrivateModifier // Private helper methods are allowed
                        }
                    }
            }

            xit("Query port methods should use descriptive verbs - disabled for ubiquitous language") {
                // Disabled to preserve domain-specific ubiquitous language like 'validate'
                scope
                    .interfaces()
                    .withNameEndingWith("QueryPort")
                    .assertTrue { port ->
                        port.functions().all { function ->
                            val validQueryVerbs = listOf(
                                "get",
                                "list",
                                "find",
                                "search",
                                "filter",
                                "count",
                                "exists",
                                "check",
                            )
                            validQueryVerbs.any { verb ->
                                function.name.startsWith(verb)
                            }
                        }
                    }
            }

            it("Query classes should end with 'Query'") {
                scope
                    .classes()
                    .filter { it.packagee?.name?.contains("query") == true }
                    .filter { !it.name.endsWith("Handler") && !it.name.endsWith("Test") }
                    .filter { !it.name.endsWith("QueryHandler") } // Interface names
                    // Exclude domain service query files
                    .filter { it.packagee?.name?.contains("domain.service.query") != true }
                    // Exclude value objects - they follow different naming conventions
                    .filter { it.packagee?.name?.contains("valueobject") != true }
                    // Exclude response classes - they follow different naming conventions
                    .filter { it.packagee?.name?.contains("query.response") != true }
                    .assertTrue { query ->
                        query.name.endsWith("Query") ||
                            query.name.endsWith("QueryPort") ||
                            query.name.endsWith("QueryAdapter") ||
                            query.name == "Query" ||
                            // Allow the Query interface itself
                            query.name.endsWith("AST") ||
                            // Query AST classes
                            query.name.endsWith("Parser") ||
                            // Query parsers
                            query.name.endsWith("Evaluator") ||
                            // Query evaluators
                            query.name.endsWith("Service") ||
                            // Query services
                            // Allow query DTOs
                            query.packagee?.name?.contains("query.dto") == true
                    }
            }

            it("Query adapters should be named appropriately") {
                scope
                    .classes()
                    .withNameEndingWith("QueryAdapter")
                    .assertTrue { adapter ->
                        // Should have clear domain context in name
                        adapter.name.contains("Scope") ||
                            adapter.name.contains("Context") ||
                            adapter.name.length > "QueryAdapter".length + 3 // Has meaningful prefix
                    }
            }
        }

        describe("Projection Naming Conventions") {

            it("Projection classes should end with 'Projection'") {
                scope
                    .classes()
                    .filter { it.packagee?.name?.contains("projection") == true }
                    .filter { !it.name.endsWith("Service") && !it.name.endsWith("Test") }
                    .assertTrue { projection ->
                        projection.name.endsWith("Projection") ||
                            projection.name.endsWith("ProjectionService") ||
                            projection.name.endsWith("Type") ||
                            // Enum types for projections
                            projection.name.endsWith("Builder") // Projection builders
                    }
            }

            it("Projection service methods should be descriptive") {
                scope
                    .interfaces()
                    .withNameEndingWith("ProjectionService")
                    .assertTrue { service ->
                        service.functions().all { function ->
                            val validProjectionVerbs = listOf(
                                "get",
                                "list",
                                "search",
                                "find",
                                "build",
                                "refresh",
                                "update",
                                "sync",
                            )
                            validProjectionVerbs.any { verb ->
                                function.name.startsWith(verb)
                            }
                        }
                    }
            }

            it("Projection types should be descriptive and domain-specific") {
                scope
                    .classes()
                    .withNameEndingWith("Projection")
                    .assertTrue { projection ->
                        // Should have meaningful domain prefixes
                        projection.name.contains("Scope") ||
                            projection.name.contains("Summary") ||
                            projection.name.contains("Detail") ||
                            projection.name.contains("Tree") ||
                            projection.name.contains("Search") ||
                            projection.name.contains("Activity") ||
                            projection.name.contains("Metrics") ||
                            projection.name.length > "Projection".length + 3 // Has meaningful prefix
                    }
            }
        }

        describe("Port Naming Conventions") {

            it("Ports should have consistent naming patterns") {
                scope
                    .interfaces()
                    .filter { it.name.endsWith("Port") }
                    .assertTrue { port ->
                        // Should follow pattern: DomainCommandPort or DomainQueryPort
                        (
                            port.name.endsWith("CommandPort") ||
                                port.name.endsWith("QueryPort") ||
                                port.name == "ContextViewPort"
                            ) &&
                            // Allow ContextViewPort as special case
                            (
                                port.name == "ContextViewPort" ||
                                    port.name.length > "CommandPort".length + 2
                                ) // Has domain prefix
                    }
            }

            it("Port adapters should match their corresponding ports") {
                scope
                    .classes()
                    .withNameEndingWith("PortAdapter")
                    .assertTrue { adapter ->
                        val adapterBaseName = adapter.name.removeSuffix("PortAdapter")

                        // Should implement a port with matching base name
                        adapter.parents().any { parent ->
                            // Allow for variations like DeviceSyncCommandPortAdapter -> DeviceSynchronizationCommandPort
                            parent.name.contains(adapterBaseName) ||
                                adapterBaseName.contains(parent.name.removeSuffix("Port"))
                        }
                    }
            }

            it("Ports should not have ambiguous names") {
                scope
                    .interfaces()
                    .filter { it.name.endsWith("Port") }
                    .assertFalse { port ->
                        val ambiguousNames = listOf(
                            "ServicePort",
                            "ManagerPort",
                            "HandlerPort",
                            "UtilPort",
                            "HelperPort",
                            "BasePort",
                        )
                        ambiguousNames.any { ambiguous ->
                            port.name.endsWith(ambiguous)
                        }
                    }
            }
        }

        describe("Method Parameter Naming") {

            it("Command methods should use 'command' parameter name") {
                scope
                    .interfaces()
                    .withNameEndingWith("CommandPort")
                    .filter { port ->
                        // Exclude Event-specific ports that naturally use 'event' parameter
                        !port.name.contains("Event")
                    }
                    .assertTrue { port ->
                        port.functions().all { function ->
                            function.parameters.isEmpty() ||
                                function.parameters.any { param ->
                                    param.name == "command"
                                }
                        }
                    }
            }

            it("Query methods should use 'query' parameter name") {
                scope
                    .interfaces()
                    .withNameEndingWith("QueryPort")
                    .filter { port ->
                        // Exclude Event-specific ports that naturally use domain-specific parameters
                        !port.name.contains("Event")
                    }
                    .assertTrue { port ->
                        port.functions().all { function ->
                            function.parameters.isEmpty() ||
                                function.parameters.any { param ->
                                    param.name == "query"
                                }
                        }
                    }
            }

            it("Handler invoke methods should use appropriate parameter names") {
                scope
                    .classes()
                    .withNameEndingWith("Handler")
                    .assertTrue { handler ->
                        val invokeMethod = handler.functions().find { it.name == "invoke" }
                        if (invokeMethod != null && invokeMethod.parameters.isNotEmpty()) {
                            val paramName = invokeMethod.parameters.first().name
                            if (handler.packagee?.name?.contains("command") == true) {
                                paramName == "command"
                            } else if (handler.packagee?.name?.contains("query") == true) {
                                paramName == "query"
                            } else {
                                true // Allow other patterns for non-CQRS handlers
                            }
                        } else {
                            true // No parameters or no invoke method is fine
                        }
                    }
            }
        }

        describe("Package Naming Consistency") {

            it("CQRS packages should follow consistent naming") {
                scope
                    .files
                    .filter { file ->
                        (
                            file.packagee?.name?.contains("handler") == true ||
                                file.packagee?.name?.contains("command") == true ||
                                file.packagee?.name?.contains("query") == true
                            ) &&
                            // Exclude CLI commands, MCP handlers, and domain service query files
                            file.packagee?.name?.contains("interfaces.cli") != true &&
                            file.packagee?.name?.contains("interfaces.mcp") != true &&
                            file.packagee?.name?.contains("domain.service.query") != true &&
                            // Exclude test files
                            !file.path.contains("/test/") &&
                            // Exclude value objects - they follow DDD structure, not CQRS structure
                            file.packagee?.name?.contains("valueobject") != true
                    }
                    .assertTrue { file ->
                        val packageName = file.packagee?.name ?: ""

                        // Package structure should be consistent
                        packageName.contains("application.handler.command") ||
                            packageName.contains("application.handler.query") ||
                            packageName.contains("application.command") ||
                            packageName.contains("application.query") ||
                            packageName.contains("application.projection") ||
                            packageName.contains("contracts") ||
                            packageName.contains("infrastructure") ||
                            // Allow new nested handler structure
                            packageName.contains("command.handler") ||
                            packageName.contains("query.handler") ||
                            // Allow command/query DTOs
                            packageName.contains("command.dto") ||
                            packageName.contains("query.dto") ||
                            // Allow platform handler interfaces
                            packageName.contains("platform.application.handler")
                    }
            }

            it("Files should be in appropriate packages based on naming") {
                scope
                    .classes()
                    .withNameEndingWith("CommandHandler")
                    .assertTrue { handler ->
                        handler.packagee?.name?.contains("command") == true
                    }

                scope
                    .classes()
                    .withNameEndingWith("QueryHandler")
                    .assertTrue { handler ->
                        handler.packagee?.name?.contains("query") == true
                    }

                scope
                    .classes()
                    .withNameEndingWith("Projection")
                    .assertTrue { projection ->
                        projection.packagee?.name?.contains("projection") == true
                    }
            }
        }
    })

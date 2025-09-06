package io.github.kamiazya.scopes.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withNameEndingWith
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.StringSpec

/**
 * Architecture tests to enforce CQRS (Command Query Responsibility Segregation) principles.
 *
 * These tests ensure that:
 * - Commands and queries are properly separated
 * - Command handlers only handle write operations
 * - Query handlers only handle read operations
 * - Ports are properly separated between commands and queries
 * - Adapters follow CQRS separation principles
 */
class CqrsArchitectureTest :
    StringSpec({

        val scope = Konsist.scopeFromProject()

        "Command handlers should be in command package" {
            scope
                .classes()
                .withNameEndingWith("Handler")
                .filter {
                    val pkg = it.packagee?.name ?: ""
                    pkg.contains("application.handler") ||
                        pkg.contains("application.command.handler") ||
                        pkg.contains("application.query.handler")
                }
                .filter {
                    // Check if this is a command handler (writes data)
                    isCommandHandler(it)
                }
                .assertTrue { handler ->
                    handler.packagee?.name?.contains("command") == true
                }
        }

        "Query handlers should be in query package" {
            scope
                .classes()
                .withNameEndingWith("Handler")
                .filter {
                    val pkg = it.packagee?.name ?: ""
                    pkg.contains("application.handler") ||
                        pkg.contains("application.command.handler") ||
                        pkg.contains("application.query.handler")
                }
                .filter {
                    // Check if this is a query handler (reads data)
                    isQueryHandler(it)
                }
                .assertTrue { handler ->
                    handler.packagee?.name?.contains("query") == true
                }
        }

        "Command handlers should implement CommandHandler interface" {
            scope
                .classes()
                .withNameEndingWith("Handler")
                .filter {
                    it.packagee?.name?.contains("handler.command") == true ||
                        it.packagee?.name?.contains("command.handler") == true
                }
                .assertTrue { handler ->
                    handler.parents().any { parent ->
                        parent.name.contains("CommandHandler")
                    }
                }
        }

        "Query handlers should implement QueryHandler interface" {
            scope
                .classes()
                .withNameEndingWith("Handler")
                .filter {
                    it.packagee?.name?.contains("handler.query") == true ||
                        it.packagee?.name?.contains("query.handler") == true
                }
                .assertTrue { handler ->
                    handler.parents().any { parent ->
                        parent.name.contains("QueryHandler")
                    }
                }
        }

        "Command ports should only contain write operations" {
            scope
                .interfaces()
                .withNameEndingWith("CommandPort")
                .assertTrue { port ->
                    port.functions().all { function ->
                        // Command operations should return Either<Error, Result> or Either<Error, Unit>
                        val returnType = function.returnType?.name
                        val isEitherType = returnType?.startsWith("Either") == true
                        val hasWriteOperationName = function.name in listOf(
                            "create",
                            "update",
                            "delete",
                            "add",
                            "remove",
                            "set",
                            "rename",
                            "register",
                            "execute",
                            "synchronize",
                            "store",
                            "save",
                            "clear", // For clearActiveContext
                            "append", // For EventCommandPort
                        ) ||
                            function.name.startsWith("create") ||
                            function.name.startsWith("update") ||
                            function.name.startsWith("delete") ||
                            function.name.startsWith("add") ||
                            function.name.startsWith("remove") ||
                            function.name.startsWith("set") ||
                            function.name.startsWith("rename") ||
                            function.name.startsWith("register") ||
                            function.name.startsWith("execute") ||
                            function.name.startsWith("synchronize") ||
                            function.name.startsWith("store") ||
                            function.name.startsWith("save") ||
                            function.name.startsWith("clear") ||
                            function.name.startsWith("append")

                        isEitherType && hasWriteOperationName
                    }
                }
        }

        "Query ports should only contain read operations" {
            scope
                .interfaces()
                .withNameEndingWith("QueryPort")
                .assertTrue { port ->
                    port.functions().all { function ->
                        // Query operations should return Either<Error, Result> or contract response types
                        val returnType = function.returnType?.name
                        val isEitherType = returnType?.startsWith("Either") == true
                        val isContractResponseType = returnType?.contains("Response") == true
                        val hasValidReturnType = isEitherType || isContractResponseType
                        val hasReadOperationName = function.name in listOf(
                            "get",
                            "list",
                            "find",
                            "search",
                            "count",
                        ) ||
                            function.name.startsWith("get") ||
                            function.name.startsWith("list") ||
                            function.name.startsWith("find") ||
                            function.name.startsWith("search") ||
                            function.name.startsWith("count") ||
                            function.name.startsWith("validate") // For validateAspectValue

                        hasValidReturnType && hasReadOperationName
                    }
                }
        }

        "Command adapters should not import query handlers" {
            scope
                .classes()
                .withNameEndingWith("CommandAdapter")
                .assertFalse { adapter ->
                    adapter.containingFile.imports.any { import ->
                        import.name.contains("handler.query")
                    }
                }
        }

        "Query adapters should not import command handlers" {
            scope
                .classes()
                .withNameEndingWith("QueryAdapter")
                .assertFalse { adapter ->
                    adapter.containingFile.imports.any { import ->
                        import.name.contains("handler.command")
                    }
                }
        }

        "Command port adapters should only use command ports" {
            scope
                .classes()
                .withNameEndingWith("CommandPortAdapter")
                .assertTrue { adapter ->
                    adapter.parents().any { parent ->
                        parent.name.endsWith("CommandPort")
                    }
                }
        }

        "Query port adapters should only use query ports" {
            scope
                .classes()
                .withNameEndingWith("QueryPortAdapter")
                .assertTrue { adapter ->
                    adapter.parents().any { parent ->
                        parent.name.endsWith("QueryPort")
                    }
                }
        }

        "Command handlers should not reference read-only services" {
            scope
                .classes()
                .withNameEndingWith("Handler")
                .filter { it.packagee?.name?.contains("handler.command") == true }
                .assertFalse { handler ->
                    handler.properties().any { property ->
                        val propertyType = property.type?.name
                        propertyType?.contains("QueryService") == true ||
                            propertyType?.contains("ReadService") == true ||
                            propertyType?.contains("ProjectionService") == true
                    }
                }
        }

        "Query handlers may use TransactionManager for read consistency" {
            scope
                .classes()
                .withNameEndingWith("Handler")
                .filter { it.packagee?.name?.contains("handler.query") == true }
                .assertFalse { handler ->
                    handler.properties().any { property ->
                        val propertyType = property.type?.name
                        // Query handlers can use TransactionManager for read consistency
                        // but should not use command-specific services
                        propertyType?.contains("CommandService") == true ||
                            propertyType?.contains("WriteService") == true
                    }
                }
        }

        "Projection classes should be immutable data classes" {
            scope
                .classes()
                .filter { it.packagee?.name?.contains("projection") == true }
                .filter { it.name.endsWith("Projection") }
                .assertTrue { projection ->
                    projection.hasDataModifier &&
                        projection.primaryConstructor?.parameters?.all { param ->
                            param.hasValModifier
                        } == true
                }
        }

        "Projection services should only contain query methods" {
            scope
                .interfaces()
                .withNameEndingWith("ProjectionService")
                .assertTrue { service ->
                    service.functions().all { function ->
                        // Projection services should only have read operations
                        function.name.startsWith("get") ||
                            function.name.startsWith("list") ||
                            function.name.startsWith("search") ||
                            function.name.startsWith("find") ||
                            function.name.startsWith("refresh") // Allow refresh for eventual consistency
                    }
                }
        }

        "Command and Query packages should not cross-reference" {
            scope
                .files
                .filter { it.packagee?.name?.contains("handler.command") == true }
                .assertFalse { file ->
                    file.imports.any { import ->
                        import.name.contains("handler.query") &&
                            !import.name.contains("test") // Allow test utilities
                    }
                }

            scope
                .files
                .filter { file -> file.packagee?.name?.contains("handler.query") == true }
                .assertFalse { file ->
                    file.imports.any { import ->
                        import.name.contains("handler.command") &&
                            !import.name.contains("test") // Allow test utilities
                    }
                }
        }
    })

/**
 * Determines if a handler class is a command handler based on its name and functionality
 */
private fun isCommandHandler(handler: com.lemonappdev.konsist.api.declaration.KoClassDeclaration): Boolean {
    val commandOperations = listOf(
        "Create",
        "Update",
        "Delete",
        "Add",
        "Remove",
        "Set",
        "Rename",
        "Synchronize",
        "Store",
        "Register",
        "Execute",
    )
    return commandOperations.any { handler.name.contains(it) }
}

/**
 * Determines if a handler class is a query handler based on its name and functionality
 */
private fun isQueryHandler(handler: com.lemonappdev.konsist.api.declaration.KoClassDeclaration): Boolean {
    val queryOperations = listOf(
        "Get",
        "List",
        "Find",
        "Search",
        "Filter",
        "Count",
    )
    return queryOperations.any { handler.name.contains(it) }
}

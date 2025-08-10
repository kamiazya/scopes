package io.github.kamiazya.scopes.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.StringSpec

/**
 * Konsist tests for DDD UseCase pattern hardening requirements.
 * Tests the new architecture components and enhanced conventions.
 */
class DddUseCasePatternTest : StringSpec({

    "DTOs should implement DTO marker interface" {
        Konsist
            .scopeFromModule("application")
            .classes()
            .filter { it.packagee?.name?.endsWith(".dto") == true }
            .assertTrue { dto ->
                dto.hasParentWithName("DTO")
            }
    }

    "Commands and Queries should implement DTO marker interface" {
        Konsist
            .scopeFromModule("application")
            .classes()
            .filter { 
                it.hasParentWithName("Command") || it.hasParentWithName("Query")
            }
            .assertTrue { commandOrQuery ->
                commandOrQuery.hasParentWithName("DTO")
            }
    }

    "DTOs should only use primitive types and standard library types" {
        Konsist
            .scopeFromModule("application")
            .classes()
            .filter { it.hasParentWithName("DTO") }
            .assertFalse { dto ->
                // Check if any property uses domain types
                dto.properties().any { property ->
                    val typeName = property.type?.name ?: ""
                    // Check for domain value objects
                    typeName.startsWith("Scope") && (
                        typeName.endsWith("Id") || 
                        typeName.endsWith("Title") || 
                        typeName.endsWith("Description")
                    ) || 
                    // Check for domain entities
                    typeName == "Scope" ||
                    // Check for other domain types
                    property.type?.sourceType?.contains("domain") == true
                }
            }
    }

    "AppErrorTranslator should be in error package" {
        Konsist
            .scopeFromModule("application")
            .interfaces()
            .filter { it.name == "AppErrorTranslator" }
            .assertTrue { translator ->
                val packageName = translator.packagee?.name ?: ""
                packageName.endsWith(".error")
            }
    }

    "TransactionManager should be in port package" {
        Konsist
            .scopeFromModule("application")
            .interfaces()
            .filter { it.name == "TransactionManager" }
            .assertTrue { transactionManager ->
                val packageName = transactionManager.packagee?.name ?: ""
                packageName.endsWith(".port")
            }
    }

    "infrastructure should implement application ports" {
        Konsist
            .scopeFromModule("infrastructure")
            .classes()
            .filter { it.name.contains("TransactionManager") }
            .assertTrue { impl ->
                // Infrastructure transaction managers should implement the port
                impl.hasParentWithName("TransactionManager")
            }
    }

    "presentation layer should not import infrastructure except in CompositionRoot" {
        Konsist
            .scopeFromModule("presentation-cli")
            .files
            .filter { !it.name.contains("CompositionRoot") }
            .assertFalse { file ->
                file.imports.any { import ->
                    import.name.contains("infrastructure")
                }
            }
    }

    "CompositionRoot should be the only place with infrastructure imports" {
        Konsist
            .scopeFromModule("presentation-cli")
            .files
            .filter { it.name.contains("CompositionRoot") }
            .assertTrue { file ->
                // CompositionRoot is allowed to import infrastructure
                file.imports.any { import ->
                    import.name.contains("infrastructure")
                }
            }
    }

    "CLI commands should use AppErrorTranslator not infrastructure utils" {
        Konsist
            .scopeFromModule("presentation-cli")
            .classes()
            .filter { it.name.endsWith("Command") }
            .assertFalse { command ->
                command.containingFile.imports.any { import ->
                    import.name.contains("infrastructure.error")
                }
            }
    }

    "handlers should accept commands/queries and return Either with DTOs" {
        Konsist
            .scopeFromModule("application")
            .classes()
            .filter { it.name.endsWith("Handler") }
            .assertTrue { handler ->
                handler.functions()
                    .filter { it.name == "invoke" }
                    .any { function ->
                        val returnType = function.returnType?.text ?: ""
                        returnType.contains("Either") && 
                        returnType.contains("ApplicationError")
                    }
            }
    }

    "handlers should not directly use domain entities in return types" {
        Konsist
            .scopeFromModule("application")
            .classes()
            .filter { it.name.endsWith("Handler") }
            .assertFalse { handler ->
                handler.functions()
                    .filter { it.name == "invoke" }
                    .any { function ->
                        val returnType = function.returnType?.text ?: ""
                        // Check if return type contains domain entity names
                        returnType.contains("Scope>") && !returnType.contains("Result>")
                    }
            }
    }

    "port interfaces should be in port package" {
        Konsist
            .scopeFromModule("application")
            .interfaces()
            .filter { 
                it.name.endsWith("Manager") || 
                it.name.endsWith("Repository") ||
                it.name.contains("Port")
            }
            .assertTrue { port ->
                val packageName = port.packagee?.name ?: ""
                packageName.endsWith(".port") || packageName.contains("repository")
            }
    }

    "error translators should not import infrastructure" {
        Konsist
            .scopeFromModule("application")
            .classes()
            .filter { it.name.contains("ErrorTranslator") }
            .assertFalse { translator ->
                translator.containingFile.imports.any { import ->
                    import.name.contains("infrastructure")
                }
            }
    }

    "transaction context should have required methods" {
        Konsist
            .scopeFromModule("application")
            .interfaces()
            .filter { it.name == "TransactionContext" }
            .assertTrue { context ->
                val methods = context.functions().map { it.name }
                methods.contains("markForRollback") &&
                methods.contains("isMarkedForRollback") &&
                methods.contains("getTransactionId")
            }
    }

    "result DTOs should follow naming convention" {
        Konsist
            .scopeFromModule("application")
            .classes()
            .filter { it.packagee?.name?.endsWith(".dto") == true }
            .filter { it.name.contains("Result") }
            .assertTrue { result ->
                // Result DTOs should follow [Action][Entity]Result pattern
                val name = result.name
                name.endsWith("Result") && name.length > "Result".length
            }
    }

    "NoopTransactionManager should not have side effects" {
        Konsist
            .scopeFromModule("infrastructure")
            .classes()
            .filter { it.name.contains("Noop") && it.name.contains("TransactionManager") }
            .assertFalse { noop ->
                // Noop implementations should not import external persistence libs
                noop.containingFile.imports.any { import ->
                    import.name.contains("jdbc") || 
                    import.name.contains("hibernate") ||
                    import.name.contains("database")
                }
            }
    }

    "mapper classes should not expose domain entities" {
        Konsist
            .scopeFromModule("application")
            .classes()
            .filter { it.name.endsWith("Mapper") }
            .assertTrue { mapper ->
                // Mappers should return DTOs, not domain entities
                mapper.functions().all { function ->
                    val returnType = function.returnType?.name ?: ""
                    // Return types should be DTOs (end with Result/DTO) or primitives
                    returnType.endsWith("Result") || 
                    returnType.endsWith("DTO") ||
                    returnType in listOf("String", "Int", "Boolean", "Long", "Double") ||
                    returnType.startsWith("List<") ||
                    returnType.startsWith("Map<")
                }
            }
    }
})

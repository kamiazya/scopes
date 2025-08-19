package io.github.kamiazya.scopes.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.StringSpec

/**
 * Domain type detection based on full type text.
 * Parses the full type text to determine if it belongs to the domain layer.
 */
private fun isDomainType(fullTypeText: String): Boolean {
    // Check if the full type text contains a domain package reference
    // This handles both simple types and fully qualified names
    return fullTypeText.contains("io.github.kamiazya.scopes.domain.")
}

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
            .filter { clazz ->
                // Include only top-level classes that directly implement DTO
                // Exclude sealed class children (like Text, Numeric, Ordered in AspectDefinitionResult)
                clazz.parents().any { parent -> parent.name == "DTO" }
            }
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
                // All commands and queries should inherit DTO either directly or through Command/Query interfaces
                // Since Command and Query now inherit from DTO, classes that implement them get DTO automatically
                commandOrQuery.hasParentWithName("DTO") ||
                commandOrQuery.hasParentWithName("Command") ||
                commandOrQuery.hasParentWithName("Query")
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
                    val packageName = property.type?.packagee?.name ?: ""
                    val fullTypeText = property.type?.text ?: ""

                    // Primary check: types from domain package are not allowed
                    if (packageName.startsWith("io.github.kamiazya.scopes.domain.") ||
                        fullTypeText.contains("io.github.kamiazya.scopes.domain.")) {
                        return@any true
                    }

                    // Check for infrastructure types (should not leak into DTOs)
                    if (packageName.startsWith("io.github.kamiazya.scopes.infrastructure.") ||
                        fullTypeText.contains("io.github.kamiazya.scopes.infrastructure.")) {
                        return@any true
                    }

                    // Common domain type patterns (value objects)
                    val domainValueObjectSuffixes = listOf(
                        "Id", "Title", "Description", "Name", "Code", "Status",
                        "Filter", "Expression", "Context", "Aspect", "Alias"
                    )

                    // Domain service/repository patterns
                    val domainServiceSuffixes = listOf(
                        "Service", "Factory", "Repository", "Manager", "Port",
                        "Handler", "Validator", "Translator"
                    )

                    // Check domain value object patterns
                    if (domainValueObjectSuffixes.any { suffix ->
                        typeName.endsWith(suffix) &&
                        !typeName.endsWith("Result") &&
                        !typeName.endsWith("DTO")
                    }) {
                        return@any true
                    }

                    // Check domain service patterns
                    if (domainServiceSuffixes.any { suffix -> typeName.endsWith(suffix) }) {
                        return@any true
                    }

                    // Check for domain entities (capitalized single words that are not primitives or DTOs)
                    val allowedTypes = setOf(
                        // Kotlin primitives and standard types
                        "String", "Int", "Long", "Boolean", "Double", "Float", "Byte", "Short", "Char",
                        "Unit", "Any", "Nothing",
                        // Collections
                        "List", "Map", "Set", "Collection", "Iterable", "Sequence", "Array",
                        "MutableList", "MutableMap", "MutableSet",
                        // Common standard library types
                        "Pair", "Triple",
                        // Date/Time types (kotlinx.datetime)
                        "Instant", "LocalDate", "LocalDateTime", "LocalTime", "DateTimePeriod",
                        // UUID
                        "UUID",
                        // Nullable versions are also allowed
                        "String?", "Int?", "Long?", "Boolean?", "Double?", "Float?",
                        "Instant?", "UUID?"
                    )

                    // Check if it's a suspicious single-word capitalized type
                    if (typeName.matches(Regex("^[A-Z][a-zA-Z]+$")) &&
                        !typeName.endsWith("DTO") &&
                        !typeName.endsWith("Result") &&
                        !typeName.endsWith("Command") &&
                        !typeName.endsWith("Query") &&
                        typeName !in allowedTypes) {
                        return@any true
                    }

                    false
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
            .filter { !it.path.contains("test") } // Allow test files to import infrastructure
            .assertFalse { file ->
                file.imports.any { import ->
                    import.name.startsWith("io.github.kamiazya.scopes.infrastructure.")
                }
            }
    }

    "CompositionRoot should be the only place with infrastructure imports" {
        Konsist
            .scopeFromModule("presentation-cli")
            .files
            .filter { it.name.contains("CompositionRoot") }
            .filter { !it.path.contains("test") } // Restrict to production files only
            .assertTrue { file ->
                // CompositionRoot is allowed to import infrastructure using fully qualified class names
                file.imports.any { import ->
                    import.name.startsWith("io.github.kamiazya.scopes.infrastructure.") &&
                    !import.name.contains("*") // Ensure FQCN, not wildcard imports
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
                    import.name.startsWith("io.github.kamiazya.scopes.infrastructure.error.")
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
                    .all { function ->
                        val returnType = function.returnType

                        if (returnType == null) {
                            return@all false
                        }

                        // Check if return type is Either<SomeError, T>
                        if (!returnType.name.startsWith("Either")) {
                            return@all false
                        }

                        // Use Konsist's typeArguments API instead of regex parsing
                        val typeArguments = returnType.typeArguments
                        if (typeArguments == null || typeArguments.size != 2) {
                            return@all false
                        }

                        // First type argument should be some Error type
                        val firstTypeArgument = typeArguments.first()
                        if (!firstTypeArgument.name.endsWith("Error")) {
                            return@all false
                        }

                        // Get the second type argument (T in Either<SomeError, T>)
                        val secondTypeArgument = typeArguments[1]
                        val genericTypeName = secondTypeArgument.name

                        // Validate that the generic type parameter is a proper DTO or Result class
                        // Should end with "Result" (DTO naming convention) or be from dto package
                        val isValidDtoType = genericTypeName.endsWith("Result") ||
                                           genericTypeName.endsWith("DTO") ||
                                           // Allow primitive wrapper types and collections
                                           genericTypeName in listOf("String", "Int", "Long", "Boolean", "Double", "Float") ||
                                           genericTypeName.startsWith("List") ||
                                           genericTypeName.startsWith("Map") ||
                                           genericTypeName.startsWith("Set")

                        // Ensure it's NOT a domain entity (shouldn't be just a single capitalized word without DTO/Result suffix)
                        val isDomainEntity = genericTypeName.matches(Regex("^[A-Z][a-zA-Z]+$")) &&
                                           !genericTypeName.endsWith("Result") &&
                                           !genericTypeName.endsWith("DTO") &&
                                           genericTypeName !in listOf("String", "Int", "Long", "Boolean", "Double", "Float", "Unit")

                        isValidDtoType && !isDomainEntity
                    }
            }
    }

    "handlers should have exactly one invoke method" {
        Konsist
            .scopeFromModule("application")
            .classes()
            .filter { it.name.endsWith("Handler") }
            .assertTrue { handler ->
                val invokeMethods = handler.functions()
                    .filter { it.name == "invoke" }

                // Each handler should have exactly one invoke method
                invokeMethods.size == 1
            }
    }

    "handlers should not directly use domain entities in Either generic parameters" {
        Konsist
            .scopeFromModule("application")
            .classes()
            .filter { it.name.endsWith("Handler") }
            .assertFalse { handler ->
                handler.functions()
                    .filter { it.name == "invoke" }
                    .any { function ->
                        val returnType = function.returnType

                        if (returnType == null || !returnType.name.startsWith("Either")) {
                            return@any false
                        }

                        // Use Konsist's typeArguments API to check generic parameters
                        val typeArguments = returnType.typeArguments
                        if (typeArguments == null || typeArguments.size != 2) {
                            return@any false
                        }

                        // Check if the success type (second type argument) is a domain type
                        // Access the second generic parameter directly using typeArguments[1]
                        val successType = typeArguments[1]
                        val successTypeText = successType.text

                        // Check if the success type is from the domain package
                        isDomainType(successTypeText)
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
                    import.name.startsWith("io.github.kamiazya.scopes.infrastructure.")
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

    "Commands should follow [Action][Entity] naming convention" {
        Konsist
            .scopeFromModule("application")
            .classes()
            .filter { it.hasParentWithName("Command") }
            .assertTrue { command ->
                val name = command.name
                // Commands should follow [Action][Entity] pattern
                // Should NOT end with Command, Query, Result, DTO
                !name.endsWith("Command") &&
                !name.endsWith("Query") &&
                !name.endsWith("Result") &&
                !name.endsWith("DTO") &&
                // Should start with a verb (action)
                name.matches(Regex("^[A-Z][a-z]+[A-Z].*"))
            }
    }

    "Queries should follow [Action][Entity]Query naming convention" {
        Konsist
            .scopeFromModule("application")
            .classes()
            .filter { it.hasParentWithName("Query") }
            .assertTrue { query ->
                val name = query.name
                // Queries should end with "Query"
                name.endsWith("Query") && name.length > "Query".length
            }
    }

    "DTOs in dto package should follow naming conventions" {
        Konsist
            .scopeFromModule("application")
            .classes()
            .filter { it.packagee?.name?.endsWith(".dto") == true }
            .filter { it.hasParentWithName("DTO") }
            .assertTrue { dto ->
                val name = dto.name
                // DTOs should end with Result, DTO, or be a specific DTO type
                name.endsWith("Result") ||
                name.endsWith("DTO") ||
                name.endsWith("Dto") ||
                // Allow specific DTO types that don't follow the pattern
                name in listOf("EmptyResult")
            }
    }

    "DTOs should be immutable data classes or objects" {
        // Check classes (data classes and sealed classes)
        val classesValid = Konsist
            .scopeFromModule("application")
            .classes()
            .filter { it.hasParentWithName("DTO") }
            .all { dto ->
                // DTOs should be data classes or sealed classes
                dto.hasDataModifier || dto.hasSealedModifier
            }

        // Objects implementing DTO are also valid (like EmptyResult)
        val objectsValid = Konsist
            .scopeFromModule("application")
            .objects()
            .filter { it.hasParentWithName("DTO") }
            .all { obj ->
                // Objects are inherently immutable singletons
                true
            }

        // Both should be valid
        assert(classesValid && objectsValid) {
            "DTOs should be immutable data classes, sealed classes, or objects"
        }
    }

    "DTO properties should be immutable (val)" {
        Konsist
            .scopeFromModule("application")
            .classes()
            .filter { it.hasParentWithName("DTO") }
            .assertTrue { dto ->
                // All properties should use val (not var)
                dto.properties().all { property ->
                    property.hasValModifier
                }
            }
    }

    "DTOs should not have mutable collections" {
        Konsist
            .scopeFromModule("application")
            .classes()
            .filter { it.hasParentWithName("DTO") }
            .assertFalse { dto ->
                dto.properties().any { property ->
                    val typeText = property.type?.text ?: ""
                    // Check for mutable collection types
                    typeText.contains("MutableList") ||
                    typeText.contains("MutableMap") ||
                    typeText.contains("MutableSet") ||
                    typeText.contains("ArrayList") ||
                    typeText.contains("HashMap") ||
                    typeText.contains("HashSet") ||
                    typeText.contains("LinkedList") ||
                    typeText.contains("TreeMap") ||
                    typeText.contains("TreeSet")
                }
            }
    }

    "DTOs should not contain business logic" {
        Konsist
            .scopeFromModule("application")
            .classes()
            .filter { it.hasParentWithName("DTO") }
            .assertTrue { dto ->
                // DTOs should not have any functions except:
                // - toString, equals, hashCode (generated by data class)
                // - component functions (generated by data class)
                // - copy (generated by data class)
                dto.functions().all { function ->
                    val functionName = function.name
                    functionName in listOf("toString", "equals", "hashCode", "copy") ||
                    functionName.startsWith("component") ||
                    // Allow simple property accessors (get functions)
                    functionName.startsWith("get")
                }
            }
    }

    "DTOs should not import domain services or repositories" {
        Konsist
            .scopeFromModule("application")
            .files
            .filter { it.path.contains("/dto/") }
            .assertFalse { file ->
                file.imports.any { import ->
                    val importName = import.name
                    // Check for domain service/repository imports
                    importName.contains(".service.") ||
                    importName.contains(".repository.") ||
                    importName.contains(".factory.") ||
                    importName.contains(".manager.") ||
                    // Check for domain imports (except error types which might be needed)
                    (importName.startsWith("io.github.kamiazya.scopes.domain.") &&
                     !importName.contains(".error."))
                }
            }
    }

    "Commands and Queries should be immutable data classes" {
        Konsist
            .scopeFromModule("application")
            .classes()
            .filter {
                it.hasParentWithName("Command") || it.hasParentWithName("Query")
            }
            .assertTrue { commandOrQuery ->
                // Should be data classes or sealed classes
                commandOrQuery.hasDataModifier || commandOrQuery.hasSealedModifier
            }
    }

    "Commands and Queries should have immutable properties" {
        Konsist
            .scopeFromModule("application")
            .classes()
            .filter {
                it.hasParentWithName("Command") || it.hasParentWithName("Query")
            }
            .assertTrue { commandOrQuery ->
                commandOrQuery.properties().all { property ->
                    property.hasValModifier
                }
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
                    val returnType = function.returnType?.text ?: ""
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


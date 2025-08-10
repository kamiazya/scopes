package io.github.kamiazya.scopes.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.StringSpec

/**
 * Enhanced domain type detection that uses package-based analysis
 * instead of hardcoded entity names.
 */
private fun isDomainType(sourceType: String?, typeName: String): Boolean {
    // Primary check: Use actual package information if available
    if (sourceType != null) {
        // Check if the type comes from a domain package
        if (sourceType.contains(".domain.")) {
            return true
        }
        
        // Check if it's from a domain-related module structure
        if (sourceType.contains("domain") && 
            (sourceType.contains("entity") || sourceType.contains("aggregate") || sourceType.contains("valueobject"))) {
            return true
        }
    }
    
    // Secondary check: Pattern-based detection for when package info is not available
    // This is more comprehensive than the previous hardcoded approach
    
    // Exclude known non-domain types first
    val allowedTypes = setOf(
        "String", "Int", "Long", "Boolean", "Double", "Float", "Unit", "Any",
        "List", "Map", "Set", "Array", "Collection", "Sequence",
        "Instant", "LocalDateTime", "LocalDate", "UUID", "BigDecimal"
    )
    
    if (allowedTypes.contains(typeName)) {
        return false
    }
    
    // Exclude DTO/Result types (these are application layer, not domain)
    if (typeName.endsWith("DTO") || typeName.endsWith("Result") || 
        typeName.endsWith("Command") || typeName.endsWith("Query")) {
        return false
    }
    
    // Exclude generic type expressions
    if (typeName.contains("<") || typeName.contains(">") || typeName.contains(",")) {
        return false
    }
    
    // Check for domain-specific patterns
    // Domain entities are typically single capitalized words
    if (typeName.matches(Regex("^[A-Z][a-zA-Z0-9]*$"))) {
        // Additional checks to reduce false positives
        
        // Likely domain value object patterns
        val domainValueObjectSuffixes = listOf("Id", "Name", "Title", "Code", "Status", "Type", "Category")
        if (domainValueObjectSuffixes.any { suffix -> typeName.endsWith(suffix) && typeName.length > suffix.length }) {
            return true
        }
        
        // Likely domain entity patterns (short, descriptive nouns)
        // This replaces the hardcoded list with pattern matching
        if (typeName.length in 3..20 && 
            !typeName.startsWith("Http") && 
            !typeName.startsWith("Json") && 
            !typeName.startsWith("Xml") &&
            !typeName.endsWith("Exception") &&
            !typeName.endsWith("Error") &&
            !typeName.endsWith("Config") &&
            !typeName.endsWith("Properties") &&
            !typeName.endsWith("Context")) {
            return true
        }
    }
    
    return false
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
                // This replaces the original hardcoded "Scope*" patterns with more flexible detection
                dto.properties().any { property ->
                    val typeName = property.type?.name ?: ""
                    val packageName = property.type?.packagee?.name ?: ""
                    
                    // Primary check: types from domain package are not allowed
                    // This replaces the problematic sourceType?.contains("domain") check
                    if (packageName.contains(".domain")) {
                        return@any true
                    }
                    
                    // Secondary check: common domain type patterns 
                    // This is more flexible than the original hardcoded "Scope*" patterns
                    // and will work for any domain types, not just Scope-related ones
                    val domainSuffixes = listOf("Id", "Title", "Description", "Name", "Code", "Status")
                    val serviceSuffixes = listOf("Service", "Factory", "Repository")
                    
                    // Check domain value object patterns
                    if (domainSuffixes.any { suffix -> typeName.endsWith(suffix) }) {
                        return@any true
                    }
                    
                    // Check domain service patterns  
                    if (serviceSuffixes.any { suffix -> typeName.endsWith(suffix) }) {
                        return@any true
                    }
                    
                    // Check likely domain entities (single capitalized word, not DTO/Result/Command/Query)
                    if (typeName.matches(Regex("^[A-Z][a-zA-Z]+$")) && 
                        !typeName.endsWith("DTO") && 
                        !typeName.endsWith("Result") &&
                        !typeName.endsWith("Command") &&
                        !typeName.endsWith("Query") &&
                        typeName !in listOf("String", "Int", "Long", "Boolean", "Double", "Float", 
                                            "List", "Map", "Set", "Array", "Instant", "UUID")) {
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
                    .all { function ->
                        val returnType = function.returnType
                        
                        if (returnType == null) {
                            return@all false
                        }
                        
                        // Check if return type is Either<ApplicationError, T>
                        if (!returnType.name.startsWith("Either")) {
                            return@all false
                        }
                        
                        // Use Konsist's typeArguments API instead of regex parsing
                        val typeArguments = returnType.typeArguments
                        if (typeArguments == null || typeArguments.size != 2) {
                            return@all false
                        }
                        
                        // First type argument should be ApplicationError
                        val firstTypeArgument = typeArguments.first()
                        if (firstTypeArgument.name != "ApplicationError") {
                            return@all false
                        }
                        
                        // Get the second type argument (T in Either<ApplicationError, T>)
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
                        val successType = typeArguments[1]
                        // Use enhanced domain type detection with just the type name
                        // The isDomainType function has comprehensive pattern-based detection
                        isDomainType(null, successType.name)
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

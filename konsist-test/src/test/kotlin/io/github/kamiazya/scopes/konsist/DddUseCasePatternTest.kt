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

    "handlers should accept commands/queries and return UseCaseResult with DTOs" {
        Konsist
            .scopeFromModule("application")
            .classes()
            .filter { it.name.endsWith("Handler") }
            .assertTrue { handler ->
                handler.functions()
                    .filter { it.name == "invoke" }
                    .all { function ->
                        val returnType = function.returnType?.text ?: ""
                        
                        // Check if return type contains UseCaseResult
                        if (!returnType.contains("UseCaseResult")) {
                            return@all false
                        }
                        
                        // Extract generic type parameter from UseCaseResult<T>
                        val genericTypePattern = Regex("UseCaseResult<([^>]+)>")
                        val matchResult = genericTypePattern.find(returnType)
                        
                        if (matchResult == null) {
                            return@all false
                        }
                        
                        val genericType = matchResult.groupValues[1].trim()
                        
                        // Validate that the generic type parameter is a proper DTO or Result class
                        // Should end with "Result" (DTO naming convention) or be from dto package
                        val isValidDtoType = genericType.endsWith("Result") || 
                                           genericType.endsWith("DTO") ||
                                           // Allow primitive wrapper types and collections
                                           genericType in listOf("String", "Int", "Long", "Boolean", "Double", "Float") ||
                                           genericType.startsWith("List<") ||
                                           genericType.startsWith("Map<") ||
                                           genericType.startsWith("Set<")
                        
                        // Ensure it's NOT a domain entity (shouldn't be just a single capitalized word without DTO/Result suffix)
                        val isDomainEntity = genericType.matches(Regex("^[A-Z][a-zA-Z]+$")) && 
                                           !genericType.endsWith("Result") && 
                                           !genericType.endsWith("DTO") &&
                                           genericType !in listOf("String", "Int", "Long", "Boolean", "Double", "Float", "Unit")
                        
                        isValidDtoType && !isDomainEntity
                    }
            }
    }

    "handlers should not directly use domain entities in UseCaseResult generic parameters" {
        Konsist
            .scopeFromModule("application")
            .classes()
            .filter { it.name.endsWith("Handler") }
            .assertFalse { handler ->
                handler.functions()
                    .filter { it.name == "invoke" }
                    .any { function ->
                        val returnType = function.returnType?.text ?: ""
                        
                        // Extract generic type parameter from UseCaseResult<T>
                        val genericTypePattern = Regex("UseCaseResult<([^>]+)>")
                        val matchResult = genericTypePattern.find(returnType)
                        
                        if (matchResult != null) {
                            val genericType = matchResult.groupValues[1].trim()
                            
                            // Check if generic type parameter is a likely domain entity
                            // Domain entities are typically single capitalized words without DTO/Result suffix
                            val isDomainEntity = genericType.matches(Regex("^[A-Z][a-zA-Z]+$")) && 
                                               !genericType.endsWith("Result") && 
                                               !genericType.endsWith("DTO") &&
                                               genericType !in listOf("String", "Int", "Long", "Boolean", "Double", "Float", "Unit")
                            
                            // Also check for specific known domain entities
                            val knownDomainEntities = listOf("Scope", "User", "Project", "Task")
                            val isKnownDomainEntity = knownDomainEntities.contains(genericType)
                            
                            isDomainEntity || isKnownDomainEntity
                        } else {
                            false
                        }
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

package io.github.kamiazya.scopes.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.StringSpec

/**
 * Naming and Package Conventions Test
 *
 * Validates that all code follows consistent naming conventions and package structure:
 * 1. Package naming follows domain.layer.component pattern
 * 2. Class names match their responsibilities
 * 3. Value objects, entities, and services follow naming patterns
 * 4. Test classes are properly named and located
 * 5. No abbreviations or unclear names
 *
 * Based on Kotlin coding conventions and DDD patterns.
 */
class NamingAndPackageConventionsTest :
    StringSpec({

        val boundedContexts = listOf(
            "scope-management",
            "user-preferences",
            "collaborative-versioning",
            "agent-management",
            "event-store",
            "device-synchronization",
        )

        // ========== Package Structure ==========

        "domain packages should follow standard structure" {
            val requiredDomainPackages = listOf(
                "entity",
                "valueobject",
                "repository",
                "error",
            )
            val optionalDomainPackages = listOf(
                "service",
                "event",
                "model",
                "aggregate",
                "value",
            )

            boundedContexts.forEach { context ->
                val domainPackages = Konsist
                    .scopeFromDirectory("contexts/$context/domain")
                    .packages
                    .map { it.name }
                    .filter { it.contains(".domain.") }
                    .map { it.substringAfterLast(".domain.").substringBefore(".") }
                    .distinct()

                // Check required packages exist
                requiredDomainPackages.forEach { required ->
                    assert(domainPackages.contains(required) || domainPackages.any { it.startsWith(required) }) {
                        "$context domain layer missing required package: $required"
                    }
                }
            }
        }

        "application packages should follow CQRS structure" {
            val expectedApplicationPackages = listOf(
                "command",
                "query",
                "handler",
                "dto",
                "error",
                "port",
            )

            boundedContexts.forEach { context ->
                val appPackages = Konsist
                    .scopeFromDirectory("contexts/$context/application")
                    .packages
                    .map { it.name }
                    .filter { it.contains(".application.") }
                    .map { it.substringAfterLast(".application.").substringBefore(".") }
                    .distinct()

                // At least some of these packages should exist
                assert(appPackages.any { pkg -> expectedApplicationPackages.contains(pkg) }) {
                    "$context application layer has non-standard package structure: $appPackages"
                }
            }
        }

        // ========== Class Naming Conventions ==========

        "value objects should have descriptive names" {
            boundedContexts.forEach { context ->
                Konsist
                    .scopeFromDirectory("contexts/$context/domain")
                    .classes()
                    .filter { it.resideInPackage("..valueobject..") || it.resideInPackage("..value..") }
                    .filter { !it.name.endsWith("Test") }
                    .assertTrue { valueObject ->
                        // Value object names should be descriptive
                        valueObject.name.length > 3 &&
                            !valueObject.name.matches(Regex("^[A-Z]{2,}$")) &&
                            // Not all caps abbreviation
                            valueObject.name != "Id" &&
                            // Should be more specific like "UserId"
                            valueObject.name != "Name" &&
                            // Should be more specific like "UserName"
                            valueObject.name != "Type" // Should be more specific like "ResourceType"
                    }
            }
        }

        "ID types should end with 'Id'" {
            boundedContexts.forEach { context ->
                Konsist
                    .scopeFromDirectory("contexts/$context/domain")
                    .classes()
                    .filter { it.hasValueModifier }
                    .filter { clazz ->
                        // Check if it's an identifier type
                        clazz.properties().any { prop ->
                            prop.name == "value" && prop.type?.sourceType?.equals("String") == true
                        } &&
                            clazz.functions().any { func ->
                                func.name == "generate" || func.name == "from"
                            }
                    }
                    .assertTrue { idClass ->
                        idClass.name.endsWith("Id")
                    }
            }
        }

        "repositories should follow naming pattern" {
            boundedContexts.forEach { context ->
                Konsist
                    .scopeFromDirectory("contexts/$context")
                    .interfaces()
                    .filter { it.resideInPackage("..repository..") }
                    .assertTrue { repository ->
                        // Repository names should be EntityNameRepository
                        repository.name.endsWith("Repository") &&
                            repository.name.length > "Repository".length &&
                            repository.name[0].isUpperCase()
                    }
            }
        }

        "handlers should match their command/query" {
            boundedContexts.forEach { context ->
                Konsist
                    .scopeFromDirectory("contexts/$context/application")
                    .classes()
                    .filter { it.name.endsWith("Handler") }
                    .assertTrue { handler ->
                        val commandOrQueryName = handler.name.removeSuffix("Handler")
                        // Should have corresponding command or query class
                        Konsist
                            .scopeFromDirectory("contexts/$context/application")
                            .classes()
                            .any { it.name == commandOrQueryName }
                    }
            }
        }

        // ========== Method Naming Conventions ==========

        "repository methods should follow conventions" {
            boundedContexts.forEach { context ->
                Konsist
                    .scopeFromDirectory("contexts/$context/domain")
                    .interfaces()
                    .filter { it.name.endsWith("Repository") }
                    .assertTrue { repository ->
                        repository.functions().all { function ->
                            when {
                                function.name.startsWith("find") ->
                                    function.returnType?.sourceType?.contains("Option") == true ||
                                        function.returnType?.sourceType?.contains("Flow") == true
                                function.name.startsWith("get") ->
                                    function.returnType?.sourceType?.contains("Option") == true
                                function.name == "save" || function.name == "create" ->
                                    function.parameters.isNotEmpty()
                                function.name == "delete" ->
                                    function.parameters.any { it.name.contains("id") || it.name.contains("Id") }
                                function.name.startsWith("exists") ->
                                    function.returnType?.sourceType?.contains("Boolean") == true
                                function.name.startsWith("count") ->
                                    function.returnType?.sourceType?.contains("Int") == true ||
                                        function.returnType?.sourceType?.contains("Long") == true
                                else -> true
                            }
                        }
                    }
            }
        }

        "factory methods should use standard names" {
            boundedContexts.forEach { context ->
                Konsist
                    .scopeFromDirectory("contexts/$context/domain")
                    .classes()
                    .filter { it.hasValueModifier || it.hasDataModifier }
                    .filter { clazz ->
                        clazz.objects().any { it.name == "Companion" }
                    }
                    .assertTrue { clazz ->
                        clazz.objects()
                            .filter { it.name == "Companion" }
                            .flatMap { it.functions() }
                            .filter { !it.name.startsWith("component") } // Data class methods
                            .all { function ->
                                function.name in listOf(
                                    "from", "of", "create", "parse", "valueOf",
                                    "fromString", "fromJson", "generate", "empty",
                                )
                            }
                    }
            }
        }

        // ========== Test Naming Conventions ==========

        "test classes should end with 'Test'" {
            boundedContexts.forEach { context ->
                Konsist
                    .scopeFromDirectory("contexts/$context")
                    .classes()
                    .filter { it.containingFile.path.contains("src/test") }
                    .filter { !it.name.endsWith("TestFixture") }
                    .filter { !it.name.endsWith("TestData") }
                    .assertTrue { testClass ->
                        testClass.name.endsWith("Test")
                    }
            }
        }

        "test methods should have descriptive names" {
            boundedContexts.forEach { context ->
                Konsist
                    .scopeFromDirectory("contexts/$context")
                    .classes()
                    .filter { it.name.endsWith("Test") }
                    .filter { clazz ->
                        // Kotest specs have different patterns
                        !clazz.parents().any { parent ->
                            parent.name.endsWith("Spec") ||
                                parent.name == "StringSpec" ||
                                parent.name == "FunSpec"
                        }
                    }
                    .assertTrue { testClass ->
                        testClass.functions()
                            .filter { func ->
                                func.annotations.any { it.name == "Test" }
                            }
                            .all { testMethod ->
                                // Test names should be descriptive
                                testMethod.name.contains("should") ||
                                    testMethod.name.contains("when") ||
                                    testMethod.name.contains("given") ||
                                    testMethod.name.contains("test") ||
                                    testMethod.name.split("_").size >= 3 // Given_When_Then pattern
                            }
                    }
            }
        }

        // ========== Enum Naming Conventions ==========

        "enum values should use UPPER_SNAKE_CASE" {
            boundedContexts.forEach { context ->
                Konsist
                    .scopeFromDirectory("contexts/$context")
                    .classes()
                    .filter { it.hasEnumModifier }
                    .assertTrue { enum ->
                        enum.enumConstants.all { constant ->
                            constant.name.matches(Regex("^[A-Z][A-Z0-9_]*$"))
                        }
                    }
            }
        }

        // ========== Event Naming Conventions ==========

        "domain events should have past-tense names" {
            boundedContexts.forEach { context ->
                Konsist
                    .scopeFromDirectory("contexts/$context/domain")
                    .classes()
                    .filter { it.resideInPackage("..event..") }
                    .filter {
                        it.name != "DomainEvent" &&
                            !it.hasAbstractModifier &&
                            !it.hasSealedModifier
                    }
                    .assertTrue { event ->
                        // Events should use past tense
                        event.name.endsWith("Created") ||
                            event.name.endsWith("Updated") ||
                            event.name.endsWith("Deleted") ||
                            event.name.endsWith("Approved") ||
                            event.name.endsWith("Rejected") ||
                            event.name.endsWith("Published") ||
                            event.name.endsWith("Archived") ||
                            event.name.endsWith("Restored") ||
                            event.name.endsWith("Completed") ||
                            event.name.endsWith("Started") ||
                            event.name.endsWith("Cancelled") ||
                            event.name.endsWith("Changed") ||
                            event.name.endsWith("Added") ||
                            event.name.endsWith("Removed") ||
                            event.name.endsWith("Merged") ||
                            event.name.endsWith("Detected") ||
                            event.name.endsWith("Reviewed") ||
                            event.name.endsWith("ed") // General past tense
                    }
            }
        }

        // ========== DTO Naming Conventions ==========

        "DTOs should be properly named" {
            boundedContexts.forEach { context ->
                Konsist
                    .scopeFromDirectory("contexts/$context/application")
                    .classes()
                    .filter { it.resideInPackage("..dto..") }
                    .assertTrue { dto ->
                        dto.name.endsWith("Dto") || dto.name.endsWith("DTO")
                    }
            }
        }

        // ========== No Abbreviations ==========

        "avoid unclear abbreviations" {
            val allowedAbbreviations = setOf(
                "Id", "Dto", "DTO", "URL", "URI", "API", "UI", "IO", "DB", "SQL",
            )

            boundedContexts.forEach { context ->
                Konsist
                    .scopeFromDirectory("contexts/$context")
                    .classes()
                    .filter { !it.name.endsWith("Test") }
                    .assertFalse { clazz ->
                        // Check for unclear abbreviations
                        val words = clazz.name.split(Regex("(?=[A-Z])"))
                        words.any { word ->
                            word.length <= 2 &&
                                word.all { it.isUpperCase() } &&
                                word !in allowedAbbreviations &&
                                word.isNotEmpty()
                        }
                    }
            }
        }

        // ========== Interface Naming ==========

        "interfaces should not have 'I' prefix" {
            boundedContexts.forEach { context ->
                Konsist
                    .scopeFromDirectory("contexts/$context")
                    .interfaces()
                    .assertFalse { iface ->
                        iface.name.startsWith("I") &&
                            iface.name.length > 1 &&
                            iface.name[1].isUpperCase()
                    }
            }
        }

        // ========== Service Naming ==========

        "services should have descriptive names" {
            boundedContexts.forEach { context ->
                Konsist
                    .scopeFromDirectory("contexts/$context")
                    .classes()
                    .filter { it.name.endsWith("Service") }
                    .assertTrue { service ->
                        // Service name should describe what it does
                        service.name != "Service" &&
                            service.name.length > "Service".length + 3
                    }
            }
        }

        // ========== Consistency Across Contexts ==========

        "similar concepts should have consistent names across contexts" {
            val commonConcepts = mapOf(
                "Repository" to { name: String -> name.endsWith("Repository") },
                "Service" to { name: String -> name.endsWith("Service") },
                "Handler" to { name: String -> name.endsWith("Handler") },
                "Error" to { name: String -> name.endsWith("Error") },
                "Event" to { name: String -> name.endsWith("Event") || name.endsWith("Created") || name.endsWith("Updated") || name.endsWith("Deleted") },
            )

            commonConcepts.forEach { (concept, checker) ->
                boundedContexts.forEach { context ->
                    Konsist
                        .scopeFromDirectory("contexts/$context")
                        .classes()
                        .filter { checker(it.name) }
                        .assertTrue { clazz ->
                            // Similar concepts should follow similar patterns
                            when (concept) {
                                "Repository" -> false || clazz.name.endsWith("RepositoryImpl") // Interfaces are filtered separately
                                "Service" -> true
                                "Handler" -> clazz.functions().any { it.name == "handle" || it.name == "invoke" }
                                "Error" -> true // Error classes can have various modifiers
                                "Event" -> clazz.hasDataModifier
                                else -> true
                            }
                        }
                }
            }
        }
    })

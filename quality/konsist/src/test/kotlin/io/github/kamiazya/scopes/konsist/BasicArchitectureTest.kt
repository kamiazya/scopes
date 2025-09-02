package io.github.kamiazya.scopes.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.StringSpec

/**
 * Basic architecture validation tests.
 *
 * These tests ensure fundamental architectural rules are followed:
 * - Proper naming conventions
 * - Package structure conventions
 * - Basic layer isolation
 */
class BasicArchitectureTest :
    StringSpec({

        "classes should have PascalCase names" {
            Konsist
                .scopeFromProduction()
                .files
                .filterNot { it.path.contains("/tmp/") }
                .flatMap { it.classes() }
                .assertTrue { clazz ->
                    clazz.name.first().isUpperCase()
                }
        }

        "package names should be lowercase" {
            Konsist
                .scopeFromProduction()
                .packages
                .filterNot { it.name.contains("tmp") }
                .assertTrue { pkg ->
                    pkg.name.lowercase() == pkg.name
                }
        }

        "repository interfaces should end with Repository" {
            Konsist
                .scopeFromProduction()
                .files
                .filterNot { it.path.contains("/tmp/") }
                .flatMap { it.interfaces() }
                .filter { it.packagee?.name?.contains(".repository") == true }
                .assertTrue { repository ->
                    repository.name.endsWith("Repository")
                }
        }

        "data classes should use val properties" {
            Konsist
                .scopeFromProduction()
                .files
                .filterNot { it.path.contains("/tmp/") }
                .flatMap { it.classes() }
                .filter { it.hasDataModifier }
                .assertTrue { dataClass ->
                    dataClass.properties().all { property ->
                        property.isVal || !property.isVar
                    }
                }
        }

        "domain entities should be in appropriate domain packages" {
            Konsist
                .scopeFromProduction()
                .files
                .filterNot { it.path.contains("/tmp/") }
                .flatMap { it.classes() }
                .filter {
                    it.name == "Scope" ||
                        it.name == "ScopeAlias" ||
                        it.name == "AspectDefinition" ||
                        (it.name == "ContextView" && it.packagee?.name?.contains(".entity") == true) ||
                        it.name.endsWith("Entity")
                }
                .filter { !it.name.endsWith("Test") }
                .assertTrue { entity ->
                    val packageName = entity.packagee?.name ?: ""
                    // Entities should be in .entity package
                    packageName.contains(".entity")
                }
        }

        "value objects should be in valueobject package" {
            Konsist
                .scopeFromProduction()
                .files
                .filterNot { it.path.contains("/tmp/") }
                .flatMap { it.classes() }
                .filter { clazz ->
                    val className = clazz.name
                    val packageName = clazz.packagee?.name ?: ""

                    // Value object naming patterns
                    val isValueObjectName =
                        className.endsWith("Id") ||
                            className.endsWith("Title") ||
                            className.endsWith("Description") ||
                            className.endsWith("Name") ||
                            className.endsWith("Key") ||
                            className.endsWith("Value") ||
                            className.endsWith("Type") ||
                            className.endsWith("Rule") ||
                            className.endsWith("Criteria") ||
                            className == "Aspects"

                    // Filter out test classes, error classes, and queries
                    isValueObjectName &&
                        !className.endsWith("Test") &&
                        !className.startsWith("Get") &&
                        // Exclude queries like GetScopeById
                        !packageName.contains(".error") &&
                        !packageName.contains(".dto") &&
                        !packageName.contains(".handler") &&
                        !packageName.contains(".service") &&
                        !packageName.contains(".query") &&
                        // Explicitly exclude query package
                        !packageName.contains(".queries") &&
                        // Exclude contracts query package
                        !packageName.contains(".platform.") &&
                        // Exclude platform packages - not domain value objects
                        !packageName.contains(".aggregate") &&
                        // Aggregate-related value objects like AggregateId are OK in aggregate package
                        !packageName.contains(".contracts.") // Exclude contracts layer - different rules apply
                }
                .assertTrue { valueObject ->
                    val packageName = valueObject.packagee?.name ?: ""
                    // Value objects should be in .valueobject or .value package
                    packageName.contains(".valueobject") || packageName.contains(".value")
                }
        }

        "domain services should be in service package" {
            Konsist
                .scopeFromProduction()
                .files
                .filterNot { it.path.contains("/tmp/") }
                .flatMap { it.classes() }
                .filter {
                    it.name.endsWith("Service") ||
                        it.name.endsWith("Publisher") ||
                        it.name.endsWith("Provider")
                }
                .filter { !it.name.endsWith("Test") }
                .filter { it.packagee?.name?.contains(".domain") == true }
                .assertTrue { service ->
                    val packageName = service.packagee?.name ?: ""
                    packageName.contains(".service")
                }
        }

        "domain layer should not use kotlinx.serialization in domain entities" {
            Konsist
                .scopeFromProduction()
                .files
                .filter { it.path.contains("/domain/") }
                .filter { it.path.contains("/src/main/") }
                .assertFalse { file ->
                    file.imports.any { import ->
                        import.name == "kotlinx.serialization.Serializable"
                    }
                }
        }

        "domain layer classes should not have @Serializable annotation" {
            Konsist
                .scopeFromProduction()
                .files
                .filterNot { it.path.contains("/tmp/") }
                .flatMap { it.classes() }
                .filter { it.resideInPackage("..domain..") }
                .filter { !it.name.endsWith("Test") }
                .assertFalse { clazz ->
                    clazz.annotations.any { annotation ->
                        annotation.name == "Serializable"
                    }
                }
        }

        "handlers should be in handler package" {
            Konsist
                .scopeFromProduction()
                .files
                .filterNot { it.path.contains("/tmp/") }
                .flatMap { it.classes() }
                .filter { it.name.endsWith("Handler") }
                .filter { !it.name.endsWith("Test") }
                .assertTrue { handler ->
                    val packageName = handler.packagee?.name ?: ""
                    packageName.contains(".handler")
                }
        }

        "DTOs should be in dto package" {
            Konsist
                .scopeFromProduction()
                .files
                .filterNot { it.path.contains("/tmp/") }
                .flatMap { it.classes() }
                .filter { it.name.endsWith("Dto") || it.name.endsWith("Result") }
                .filter { !it.name.endsWith("Test") }
                .filter { !it.name.contains("ValidationResult") } // Domain concept, not a DTO
                .filter { clazz ->
                    val packageName = clazz.packagee?.name ?: ""
                    // Only apply this rule to non-contracts packages
                    // Contracts layer has different conventions (results package)
                    !packageName.contains(".contracts.") &&
                        // Domain service results are domain concepts, not DTOs
                        !packageName.contains(".domain.service")
                }
                .assertTrue { dto ->
                    val packageName = dto.packagee?.name ?: ""
                    packageName.contains(".dto")
                }
        }

        "commands should be in command package" {
            Konsist
                .scopeFromProduction()
                .files
                .filterNot { it.path.contains("/tmp/") }
                .flatMap { it.classes() }
                .filter {
                    val name = it.name
                    // Common command patterns - exclude Input/Dto/Query suffixes
                    (
                        name.startsWith("Create") ||
                            name.startsWith("Update") ||
                            name.startsWith("Delete") ||
                            name.startsWith("Save") ||
                            name.startsWith("Reset")
                        ) &&
                        !name.endsWith("Handler") &&
                        !name.endsWith("Test") &&
                        !name.endsWith("Result") &&
                        !name.endsWith("Error") &&
                        !name.endsWith("Input") &&
                        !name.endsWith("Dto") &&
                        !name.endsWith("Query")
                }
                .filter { it.packagee?.name?.contains(".application") == true }
                .assertTrue { command ->
                    val packageName = command.packagee?.name ?: ""
                    packageName.contains(".command")
                }
        }

        "queries should be in query package" {
            Konsist
                .scopeFromProduction()
                .files
                .filterNot { it.path.contains("/tmp/") }
                .flatMap { it.classes() }
                .filter {
                    val name = it.name
                    // Common query patterns
                    (
                        name.startsWith("Get") ||
                            name.startsWith("Find") ||
                            name.startsWith("List") ||
                            name.startsWith("Search")
                        ) &&
                        !name.endsWith("Handler") &&
                        !name.endsWith("Test") &&
                        !name.endsWith("Result")
                }
                .filter { it.packagee?.name?.contains(".application") == true }
                .assertTrue { query ->
                    val packageName = query.packagee?.name ?: ""
                    packageName.contains(".query")
                }
        }

        "test files should not be in production code" {
            Konsist
                .scopeFromProduction()
                .files
                .assertFalse { file ->
                    file.path.contains("/test/")
                }
        }

        "production code should not contain test dependencies" {
            Konsist
                .scopeFromProduction()
                .files
                .assertFalse { file ->
                    file.imports.any { import ->
                        import.name.contains("kotest") ||
                            import.name.contains("junit") ||
                            import.name.contains("mockk") ||
                            import.name.contains("io.kotest")
                    }
                }
        }
    })

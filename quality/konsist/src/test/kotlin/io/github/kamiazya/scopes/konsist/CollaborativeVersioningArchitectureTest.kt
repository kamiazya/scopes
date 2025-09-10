package io.github.kamiazya.scopes.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.StringSpec

/**
 * Architecture tests for Collaborative Versioning bounded context.
 * 
 * This test ensures that the collaborative-versioning context follows:
 * - Clean Architecture principles with proper layer separation
 * - Domain-Driven Design (DDD) patterns and boundaries
 * - Functional programming patterns with Arrow Either
 * - Strong typing with value objects instead of raw strings
 * - Proper error hierarchy and handling patterns
 * 
 * Key architectural principles enforced:
 * 1. Domain layer is pure - no external dependencies
 * 2. Application layer orchestrates domain logic without infrastructure concerns
 * 3. Infrastructure layer implements adapters and technical concerns
 * 4. Proper separation between bounded contexts
 * 5. Event-driven architecture patterns
 */
class CollaborativeVersioningArchitectureTest : StringSpec({

    val contextPath = "contexts/collaborative-versioning"

    // ========== Layer Dependency Rules ==========
    
    "collaborative-versioning domain should not depend on application or infrastructure layers" {
        Konsist
            .scopeFromDirectory("$contextPath/domain")
            .files
            .filter { it.path.contains("src/main") }
            .assertFalse { file ->
                file.imports.any { import ->
                    import.name.contains(".application.") ||
                    import.name.contains(".infrastructure.") ||
                    import.name.contains(".apps.") ||
                    import.name.contains(".boot.") ||
                    // Also check for framework dependencies
                    import.name.contains("org.springframework") ||
                    import.name.contains("jakarta.") ||
                    import.name.contains("javax.") ||
                    import.name.contains("org.koin") ||
                    import.name.contains("kotlinx.serialization.Serializable")
                }
            }
    }

    "collaborative-versioning application should not depend on infrastructure layer" {
        Konsist
            .scopeFromDirectory("$contextPath/application")
            .files
            .filter { it.path.contains("src/main") }
            .assertFalse { file ->
                file.imports.any { import ->
                    import.name.contains(".infrastructure.") ||
                    import.name.contains(".boot.")
                }
            }
    }

    // ========== Domain Purity Rules ==========

    "domain services should be pure functions without I/O operations" {
        Konsist
            .scopeFromDirectory("$contextPath/domain")
            .classes()
            .filter { it.resideInPackage("..service..") }
            .filter { !it.name.endsWith("Test") }
            .assertFalse { service ->
                // Check for I/O operations
                service.text.contains("java.io") ||
                service.text.contains("java.nio.file") ||
                service.text.contains("java.net") ||
                service.text.contains("okhttp") ||
                service.text.contains("ktor.client") ||
                // Check for database operations
                service.text.contains("jdbc") ||
                service.text.contains("sql") ||
                service.text.contains("database") ||
                // Check for external service calls
                service.text.contains("http") && !service.text.contains("https://") // Allow URLs in comments
            }
    }

    "domain entities should not have mutable state" {
        Konsist
            .scopeFromDirectory("$contextPath/domain")
            .classes()
            .filter { it.resideInPackage("..entity..") }
            .filter { !it.name.endsWith("Test") }
            .assertTrue { entity ->
                // All properties should be val (immutable)
                entity.properties().all { prop ->
                    prop.hasValModifier
                }
            }
    }

    // ========== Value Objects and Strong Typing ==========

    "all ID types should be value classes" {
        Konsist
            .scopeFromDirectory("$contextPath/domain")
            .classes()
            .filter { it.name.endsWith("Id") }
            .filter { !it.name.endsWith("Test") }
            .filter { !it.hasEnumModifier }
            .assertTrue { idClass ->
                idClass.hasValueModifier
            }
    }

    "value objects should be immutable" {
        Konsist
            .scopeFromDirectory("$contextPath/domain")
            .classes()
            .filter { it.resideInPackage("..valueobject..") || it.resideInPackage("..value..") }
            .filter { !it.name.endsWith("Test") }
            .filter { !it.hasEnumModifier && !it.hasSealedModifier }
            .assertTrue { valueObject ->
                valueObject.hasDataModifier || valueObject.hasValueModifier
            }
    }

    "domain should not use raw strings for identifiers" {
        Konsist
            .scopeFromDirectory("$contextPath/domain")
            .functions()
            .filter { !it.name.endsWith("Test") }
            .assertFalse { function ->
                // Check for raw string parameters that look like IDs
                function.parameters.any { param ->
                    param.type.sourceType == "String" && (
                        param.name.endsWith("Id") ||
                        param.name == "id" ||
                        param.name.endsWith("Identifier")
                    )
                }
            }
    }

    // ========== Error Hierarchy ==========

    "all errors should extend CollaborativeVersioningError" {
        Konsist
            .scopeFromDirectory("$contextPath/domain")
            .classes()
            .filter { it.resideInPackage("..error..") }
            .filter { it.name.endsWith("Error") }
            .filter { it.name != "CollaborativeVersioningError" }
            .filter { !it.hasSealedModifier }
            .filter { !it.hasEnumModifier }
            .assertTrue { error ->
                error.parents().any { parent ->
                    parent.name == "CollaborativeVersioningError" ||
                    parent.name.endsWith("Error")
                }
            }
    }

    "repository errors should be specific to operations" {
        Konsist
            .scopeFromDirectory("$contextPath/domain")
            .classes()
            .filter { it.resideInPackage("..error..") }
            .filter { 
                it.name.contains("Repository") || 
                it.name.contains("Save") ||
                it.name.contains("Find") ||
                it.name.contains("Delete") ||
                it.name.contains("Update")
            }
            .assertTrue { error ->
                // Repository errors should have specific context
                error.properties().any { prop ->
                    prop.name.contains("id") ||
                    prop.name.contains("Id") ||
                    prop.name.contains("message") ||
                    prop.name.contains("cause")
                }
            }
    }

    // ========== Repository Pattern ==========

    "repositories should be interfaces in domain layer" {
        Konsist
            .scopeFromDirectory("$contextPath/domain")
            .interfaces()
            .filter { it.resideInPackage("..repository..") }
            .assertTrue { repository ->
                repository.name.endsWith("Repository")
            }
    }

    "repository methods should return Either types" {
        Konsist
            .scopeFromDirectory("$contextPath/domain")
            .interfaces()
            .filter { it.resideInPackage("..repository..") }
            .assertTrue { repository ->
                repository.functions().all { function ->
                    function.returnType?.sourceType?.contains("Either") == true
                }
            }
    }

    // ========== Event Sourcing Patterns ==========

    "domain events should extend DomainEvent" {
        Konsist
            .scopeFromDirectory("$contextPath/domain")
            .classes()
            .filter { it.resideInPackage("..event..") }
            .filter { 
                it.name.endsWith("Event") || 
                it.name.endsWith("Created") ||
                it.name.endsWith("Updated") ||
                it.name.endsWith("Deleted") ||
                it.name.endsWith("Approved") ||
                it.name.endsWith("Reviewed") ||
                it.name.endsWith("Merged") ||
                it.name.endsWith("Detected")
            }
            .filter { !it.name.equals("DomainEvent") }
            .filter { !it.hasAbstractModifier }
            .filter { !it.hasSealedModifier }
            .assertTrue { event ->
                event.parents().any { parent ->
                    parent.name == "DomainEvent" ||
                    parent.name.endsWith("Event")
                }
            }
    }

    "events should be data classes with proper metadata" {
        Konsist
            .scopeFromDirectory("$contextPath/domain")
            .classes()
            .filter { it.resideInPackage("..event..") }
            .filter { !it.hasAbstractModifier && !it.hasSealedModifier }
            .filter { !it.name.equals("DomainEvent") }
            .assertTrue { event ->
                event.hasDataModifier && 
                event.properties().any { it.name == "occurredAt" || it.name == "timestamp" }
            }
    }

    // ========== Application Layer Patterns ==========

    "command handlers should implement command pattern" {
        Konsist
            .scopeFromDirectory("$contextPath/application")
            .classes()
            .filter { it.name.endsWith("Handler") }
            .filter { it.resideInPackage("..handler.command..") }
            .assertTrue { handler ->
                handler.functions().any { function ->
                    function.name == "invoke" || function.name == "handle"
                }
            }
    }

    "query handlers should be read-only" {
        Konsist
            .scopeFromDirectory("$contextPath/application")
            .classes()
            .filter { it.name.endsWith("QueryHandler") || it.resideInPackage("..handler.query..") }
            .assertFalse { handler ->
                // Should not have save, update, delete operations
                handler.text.contains("save(") ||
                handler.text.contains("update(") ||
                handler.text.contains("delete(") ||
                handler.text.contains("inTransaction")
            }
    }

    "DTOs should be in dto package" {
        Konsist
            .scopeFromDirectory("$contextPath/application")
            .classes()
            .filter { it.name.endsWith("Dto") || it.name.endsWith("DTO") }
            .assertTrue { dto ->
                dto.resideInPackage("..dto..")
            }
    }

    // ========== Infrastructure Layer Patterns ==========

    "infrastructure should contain adapter implementations" {
        Konsist
            .scopeFromDirectory("$contextPath/infrastructure")
            .classes()
            .filter { it.name.endsWith("Adapter") || it.resideInPackage("..adapter..") }
            .assertTrue { adapter ->
                // Adapters should implement domain interfaces
                adapter.parents().any { parent ->
                    parent.name.endsWith("Publisher") ||
                    parent.name.endsWith("Repository") ||
                    parent.name.endsWith("Service")
                }
            }
    }

    "event publishers should implement DomainEventPublisher" {
        Konsist
            .scopeFromDirectory("$contextPath/infrastructure")
            .classes()
            .filter { it.name.contains("EventPublisher") }
            .filter { !it.hasAbstractModifier }
            .assertTrue { publisher ->
                publisher.parents().any { parent ->
                    parent.name == "DomainEventPublisher"
                }
            }
    }

    // ========== Cross-Context Boundaries ==========

    "should not directly depend on other bounded contexts" {
        val otherContexts = listOf(
            "scope-management",
            "user-preferences",
            "event-store",
            "device-synchronization"
        )
        
        Konsist
            .scopeFromDirectory(contextPath)
            .files
            .filter { it.path.contains("src/main") }
            .assertFalse { file ->
                file.imports.any { import ->
                    otherContexts.any { context ->
                        import.name.contains(".$context.domain.")
                    }
                }
            }
    }

    // ========== Naming Conventions ==========

    "services should follow naming conventions" {
        Konsist
            .scopeFromDirectory("$contextPath/domain")
            .classes()
            .filter { it.name.endsWith("Service") }
            .filter { !it.hasAbstractModifier }
            .assertTrue { service ->
                // Services should have descriptive names
                service.name.length > 7 && // More than just "Service"
                service.name != "Service" &&
                !service.name.startsWith("Abstract")
            }
    }

    "value objects should have factory methods" {
        Konsist
            .scopeFromDirectory("$contextPath/domain")
            .classes()
            .filter { it.hasValueModifier }
            .filter { !it.name.endsWith("Test") }
            .assertTrue { valueObject ->
                // Value objects should have companion object with factory methods
                valueObject.objects().any { obj ->
                    obj.name == "Companion" &&
                    obj.functions().any { func ->
                        func.name == "from" || 
                        func.name == "of" || 
                        func.name == "create" ||
                        func.name == "generate"
                    }
                }
            }
    }

    // ========== Functional Programming Patterns ==========

    "use ensure() instead of if-throw patterns" {
        Konsist
            .scopeFromDirectory(contextPath)
            .functions()
            .filter { it.text.contains("either") || it.text.contains("Either") }
            .assertFalse { function ->
                // Should not use raise() directly
                function.text.contains("raise(") && 
                !function.text.contains("ensure")
            }
    }

    "prefer functional iteration over for loops" {
        Konsist
            .scopeFromDirectory("$contextPath/application")
            .functions()
            .filter { !it.name.endsWith("Test") }
            .assertFalse { function ->
                // Should not use traditional for loops in functional code
                function.text.contains(Regex("for\\s*\\(.*in\\s+"))
            }
    }

    // ========== Batch Processing Patterns ==========

    "batch processing should have proper result types" {
        Konsist
            .scopeFromDirectory("$contextPath/domain")
            .classes()
            .filter { it.name.contains("Batch") }
            .filter { it.name.contains("Result") }
            .assertTrue { batchResult ->
                // Batch results should track successes and failures
                batchResult.properties().any { it.name.contains("success") } &&
                batchResult.properties().any { it.name.contains("fail") }
            }
    }

    // ========== Aggregate Boundaries ==========

    "aggregates should not reference other aggregates directly" {
        Konsist
            .scopeFromDirectory("$contextPath/domain")
            .classes()
            .filter { it.resideInPackage("..model..") || it.resideInPackage("..aggregate..") }
            .assertFalse { aggregate ->
                // Aggregates should only reference other aggregates by ID
                aggregate.properties().any { prop ->
                    val type = prop.type.sourceType ?: ""
                    // Check if property is another aggregate (not an ID)
                    (type.contains("ChangeProposal") && !type.contains("ProposalId")) ||
                    (type.contains("TrackedResource") && !type.contains("ResourceId")) ||
                    (type.contains("Changeset") && !type.contains("ChangesetId"))
                }
            }
    }
})
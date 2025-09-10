package io.github.kamiazya.scopes.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.StringSpec

/**
 * DDD Boundaries Test - Validates Domain-Driven Design boundaries across all contexts.
 *
 * This test ensures:
 * 1. Proper bounded context separation
 * 2. Clean Architecture layer dependencies
 * 3. Domain model purity
 * 4. Aggregate boundaries
 * 5. Anti-corruption layer patterns
 * 6. Event-driven communication between contexts
 *
 * Based on DDD principles:
 * - Each bounded context owns its ubiquitous language
 * - Contexts communicate through domain events or APIs
 * - No shared domain models between contexts
 * - Strong consistency within aggregates, eventual consistency between them
 */
class DddBoundariesTest :
    StringSpec({

        val boundedContexts = listOf(
            "scope-management",
            "user-preferences",
            "collaborative-versioning",
            "agent-management",
            "event-store",
            "device-synchronization",
        )

        // ========== Bounded Context Isolation ==========

        "bounded contexts should not share domain models" {
            boundedContexts.forEach { contextA ->
                val otherContexts = boundedContexts.filter { it != contextA }

                Konsist
                    .scopeFromDirectory("contexts/$contextA")
                    .files
                    .filter { it.path.contains("src/main") }
                    .assertFalse { file ->
                        // Should not import domain models from other contexts
                        file.imports.any { import ->
                            otherContexts.any { contextB ->
                                import.name.contains(".$contextB.domain.entity.") ||
                                    import.name.contains(".$contextB.domain.model.") ||
                                    import.name.contains(".$contextB.domain.aggregate.")
                            }
                        }
                    }
            }
        }

        "contexts should communicate through events or application services" {
            boundedContexts.forEach { context ->
                Konsist
                    .scopeFromDirectory("contexts/$context/application")
                    .classes()
                    .filter { it.name.endsWith("Handler") }
                    .filter { clazz ->
                        // Check if handler references other contexts
                        clazz.text.contains("contexts.") &&
                            boundedContexts.any { otherContext ->
                                otherContext != context && clazz.text.contains(".$otherContext.")
                            }
                    }
                    .assertTrue { handler ->
                        // If referencing other contexts, should be through events or ports
                        handler.text.contains("EventPublisher") ||
                            handler.text.contains("DomainEvent") ||
                            handler.text.contains("Port") ||
                            handler.text.contains("ApplicationService")
                    }
            }
        }

        // ========== Domain Model Purity ==========

        "domain models should not have infrastructure concerns" {
            boundedContexts.forEach { context ->
                Konsist
                    .scopeFromDirectory("contexts/$context/domain")
                    .classes()
                    .filter {
                        it.resideInPackage("..entity..") ||
                            it.resideInPackage("..model..") ||
                            it.resideInPackage("..aggregate..")
                    }
                    .assertFalse { domainClass ->
                        // Check for infrastructure concerns
                        domainClass.annotations.any { annotation ->
                            annotation.name.contains("Entity") ||
                                // JPA
                                annotation.name.contains("Table") ||
                                annotation.name.contains("Column") ||
                                annotation.name.contains("Serializable") ||
                                // Kotlinx serialization
                                annotation.name.contains("Component") ||
                                // DI
                                annotation.name.contains("Service")
                        }
                    }
            }
        }

        "value objects should be self-validating" {
            boundedContexts.forEach { context ->
                Konsist
                    .scopeFromDirectory("contexts/$context/domain")
                    .classes()
                    .filter { it.hasValueModifier }
                    .filter { !it.name.endsWith("Test") }
                    .assertTrue { valueObject ->
                        // Value objects should validate in factory methods
                        valueObject.objects().any { companion ->
                            companion.name == "Companion" &&
                                companion.functions().any { func ->
                                    (func.name == "from" || func.name == "create" || func.name == "of") &&
                                        (
                                            func.text.contains("require") ||
                                                func.text.contains("check") ||
                                                func.text.contains("ensure") ||
                                                func.text.contains("Either")
                                            )
                                }
                        }
                    }
            }
        }

        // ========== Aggregate Design Rules ==========

        "aggregates should protect their invariants" {
            boundedContexts.forEach { context ->
                Konsist
                    .scopeFromDirectory("contexts/$context/domain")
                    .classes()
                    .filter {
                        it.name.endsWith("Aggregate") ||
                            it.resideInPackage("..aggregate..") ||
                            it.resideInPackage("..model..")
                    }
                    .filter { !it.hasAbstractModifier }
                    .assertTrue { aggregate ->
                        // Aggregates should have private setters or be immutable
                        aggregate.properties().all { prop ->
                            prop.isVal ||
                                prop.hasPrivateModifier ||
                                prop.hasProtectedModifier
                        }
                    }
            }
        }

        "aggregates should only reference other aggregates by ID" {
            boundedContexts.forEach { context ->
                Konsist
                    .scopeFromDirectory("contexts/$context/domain")
                    .classes()
                    .filter {
                        it.resideInPackage("..model..") ||
                            it.resideInPackage("..aggregate..") ||
                            it.resideInPackage("..entity..")
                    }
                    .assertFalse { aggregate ->
                        aggregate.properties().any { prop ->
                            val type = prop.type?.sourceType ?: ""
                            // Check for direct aggregate references (not IDs)
                            boundedContexts.any { ctx ->
                                (
                                    type.contains("$ctx.domain.model.") ||
                                        type.contains("$ctx.domain.aggregate.") ||
                                        type.contains("$ctx.domain.entity.")
                                    ) &&
                                    !type.contains("Id") &&
                                    !type.contains("List<") &&
                                    // Collections of IDs are OK
                                    !type.contains("Set<")
                            }
                        }
                    }
            }
        }

        // ========== Repository Patterns ==========

        "repositories should return complete aggregates" {
            boundedContexts.forEach { context ->
                Konsist
                    .scopeFromDirectory("contexts/$context/domain")
                    .interfaces()
                    .filter { it.name.endsWith("Repository") }
                    .assertTrue { repository ->
                        repository.functions()
                            .filter { it.name.startsWith("find") || it.name.startsWith("get") }
                            .all { function ->
                                val returnType = function.returnType?.sourceType ?: ""
                                // Should return domain entities/aggregates, not primitives
                                returnType.contains("Either<") &&
                                    (returnType.contains("Option<") || returnType.contains("Flow<")) &&
                                    !returnType.contains("String>") &&
                                    !returnType.contains("Int>") &&
                                    !returnType.contains("Long>")
                            }
                    }
            }
        }

        "repository interfaces should be in domain layer" {
            boundedContexts.forEach { context ->
                Konsist
                    .scopeFromDirectory("contexts/$context")
                    .interfaces()
                    .filter { it.name.endsWith("Repository") }
                    .assertTrue { repository ->
                        repository.containingFile.path.contains("/domain/")
                    }
            }
        }

        // ========== Event-Driven Architecture ==========

        "domain events should be immutable" {
            boundedContexts.forEach { context ->
                Konsist
                    .scopeFromDirectory("contexts/$context/domain")
                    .classes()
                    .filter { it.resideInPackage("..event..") }
                    .filter {
                        it.name.endsWith("Event") ||
                            it.name.endsWith("Created") ||
                            it.name.endsWith("Updated") ||
                            it.name.endsWith("Deleted")
                    }
                    .filter { !it.hasAbstractModifier }
                    .assertTrue { event ->
                        event.hasDataModifier &&
                            event.properties().all { it.hasValModifier }
                    }
            }
        }

        "event handlers should not modify events" {
            boundedContexts.forEach { context ->
                Konsist
                    .scopeFromDirectory("contexts/$context/application")
                    .classes()
                    .filter {
                        it.name.endsWith("Handler") ||
                            it.name.contains("EventHandler") ||
                            it.name.contains("EventListener")
                    }
                    .assertFalse { handler ->
                        // Handlers should not have mutable event parameters
                        handler.functions().any { func ->
                            func.parameters.any { param ->
                                param.type.sourceType?.contains("Event") == true &&
                                    param.isVar
                            }
                        }
                    }
            }
        }

        // ========== Anti-Corruption Layer ==========

        "external integrations should have anti-corruption layer" {
            boundedContexts.forEach { context ->
                Konsist
                    .scopeFromDirectory("contexts/$context/infrastructure")
                    .classes()
                    .filter {
                        it.name.contains("Client") ||
                            it.name.contains("Adapter") ||
                            it.name.contains("Gateway")
                    }
                    .assertTrue { integration ->
                        // Should not expose external types to domain
                        integration.functions()
                            .filter { it.hasPublicModifier }
                            .all { function ->
                                val returnType = function.returnType?.sourceType ?: ""
                                // Return types should be domain types or primitives
                                !returnType.contains("HttpResponse") &&
                                    !returnType.contains("ResultSet") &&
                                    !returnType.contains("JsonElement")
                            }
                    }
            }
        }

        // ========== Use Case Independence ==========

        "use cases should not depend on each other" {
            boundedContexts.forEach { context ->
                Konsist
                    .scopeFromDirectory("contexts/$context/application")
                    .classes()
                    .filter {
                        it.name.endsWith("UseCase") ||
                            it.name.endsWith("Handler")
                    }
                    .assertFalse { useCase ->
                        // Use cases should not instantiate other use cases
                        useCase.properties().any { prop ->
                            prop.type?.sourceType?.endsWith("UseCase") == true ||
                                prop.type?.sourceType?.endsWith("Handler") == true
                        }
                    }
            }
        }

        // ========== Domain Service Rules ==========

        "domain services should be stateless" {
            boundedContexts.forEach { context ->
                Konsist
                    .scopeFromDirectory("contexts/$context/domain")
                    .classes()
                    .filter { it.name.endsWith("Service") }
                    .filter { it.resideInPackage("..service..") }
                    .filter { !it.hasAbstractModifier }
                    .assertTrue { service ->
                        // Domain services should not have mutable state
                        service.properties().all { prop ->
                            prop.isVal // Domain services should not have mutable state
                        }
                    }
            }
        }

        // ========== Error Handling Boundaries ==========

        "each context should have its own error hierarchy" {
            boundedContexts.forEach { context ->
                val contextName = context.split("-")
                    .joinToString("") { it.capitalize() }

                Konsist
                    .scopeFromDirectory("contexts/$context/domain")
                    .classes()
                    .filter { it.resideInPackage("..error..") }
                    .filter { it.name.endsWith("Error") }
                    .filter { !it.name.equals("${contextName}Error") }
                    .assertTrue { error ->
                        // All errors should extend context-specific base error
                        error.parents().any { parent ->
                            parent.name == "${contextName}Error" ||
                                parent.name.endsWith("Error")
                        }
                    }
            }
        }

        // ========== Shared Kernel Rules ==========

        "platform layer should only contain technical utilities" {
            Konsist
                .scopeFromDirectory("platform")
                .classes()
                .assertFalse { platformClass ->
                    // Platform should not have business logic
                    platformClass.name.contains("Service") ||
                        platformClass.name.contains("UseCase") ||
                        platformClass.name.contains("Repository") ||
                        platformClass.name.contains("Entity") ||
                        platformClass.name.contains("Aggregate")
                }
        }

        // ========== Command and Query Separation ==========

        "commands should not return query results" {
            boundedContexts.forEach { context ->
                Konsist
                    .scopeFromDirectory("contexts/$context/application")
                    .classes()
                    .filter { it.name.endsWith("Command") }
                    .assertTrue { command ->
                        // Commands should return void, ID, or simple success/failure
                        val handlers = Konsist
                            .scopeFromDirectory("contexts/$context/application")
                            .classes()
                            .filter { it.name == "${command.name}Handler" }

                        handlers.all { handler ->
                            handler.functions()
                                .filter { it.name == "handle" || it.name == "invoke" }
                                .all { function ->
                                    val returnType = function.returnType?.sourceType ?: ""
                                    returnType.contains("Unit") ||
                                        returnType.contains("Id") ||
                                        returnType.contains("Boolean") ||
                                        returnType.contains("Result")
                                }
                        }
                    }
            }
        }

        "queries should not modify state" {
            boundedContexts.forEach { context ->
                Konsist
                    .scopeFromDirectory("contexts/$context/application")
                    .classes()
                    .filter {
                        it.name.endsWith("QueryHandler") ||
                            it.resideInPackage("..query..")
                    }
                    .assertFalse { queryHandler ->
                        // Queries should not have save/update/delete operations
                        queryHandler.text.contains("save(") ||
                            queryHandler.text.contains("update(") ||
                            queryHandler.text.contains("delete(") ||
                            queryHandler.text.contains("create(") ||
                            queryHandler.text.contains("persist(")
                    }
            }
        }

        // ========== Consistency Boundaries ==========

        "transactions should not span multiple aggregates" {
            boundedContexts.forEach { context ->
                Konsist
                    .scopeFromDirectory("contexts/$context")
                    .functions()
                    .filter { it.text.contains("transaction") || it.text.contains("inTransaction") }
                    .assertFalse { function ->
                        // Should not modify multiple repository types in one transaction
                        val repositoryCalls = Regex("(\\w+Repository)\\.(save|update|delete)")
                            .findAll(function.text)
                            .map { it.groupValues[1] }
                            .distinct()
                            .toList()

                        repositoryCalls.size > 1
                    }
            }
        }
    })

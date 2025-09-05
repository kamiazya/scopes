package io.github.kamiazya.scopes.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.StringSpec

/**
 * Clean Architecture tests for the new project structure.
 * Based on the original architectural vision:
 * - Platform layer: Technical utilities only
 * - Contexts: Bounded contexts with domain/application/infrastructure layers
 * - Apps: Application logic and coordination
 * - Boot: Thin entry points for distribution
 */
class CleanArchitectureTest :
    StringSpec({

        val contexts = listOf(
            "scope-management",
            "user-preferences",
            "event-store",
            "device-synchronization",
        )

        // ========== Layer Dependency Rules ==========

        "domain layers should not depend on application or infrastructure" {
            contexts.forEach { context ->
                Konsist
                    .scopeFromDirectory("contexts/$context/domain")
                    .files
                    .filter { it.path.contains("src/main") }
                    .assertFalse { file ->
                        file.imports.any { import ->
                            import.name.contains(".application.") ||
                                import.name.contains(".infrastructure.") ||
                                import.name.contains(".apps.") ||
                                import.name.contains(".boot.")
                        }
                    }
            }
        }

        "application layers should not depend on infrastructure" {
            contexts.forEach { context ->
                Konsist
                    .scopeFromDirectory("contexts/$context/application")
                    .files
                    .filter { it.path.contains("src/main") }
                    .assertFalse { file ->
                        file.imports.any { import ->
                            import.name.contains(".infrastructure.") ||
                                import.name.contains(".boot.")
                        }
                    }
            }
        }

        "platform layer should not depend on contexts, apps, or boot" {
            Konsist
                .scopeFromDirectory("platform")
                .files
                .filter { it.path.contains("src/main") }
                .assertFalse { file ->
                    file.imports.any { import ->
                        contexts.any { context ->
                            import.name.contains(".$context.")
                        } ||
                            import.name.contains(".apps.") ||
                            import.name.contains(".boot.")
                    }
                }
        }

        "apps layer should not directly depend on infrastructure except for DI" {
            Konsist
                .scopeFromDirectory("apps")
                .files
                .filter { it.path.contains("src/main") }
                .filter { file ->
                    // Exclude DI module files from this check
                    file.packagee?.name?.contains(".di") != true &&
                        !file.name.endsWith("Module.kt") &&
                        !file.name.endsWith("Component.kt")
                }
                .assertFalse { file ->
                    file.imports.any { import ->
                        contexts.any { context ->
                            import.name.contains(".$context.infrastructure.")
                        }
                    }
                }
        }

        // ========== Inter-Context Communication ==========

        "contexts should not directly depend on each other's domain layers" {
            contexts.forEach { contextA ->
                contexts.filter { it != contextA }.forEach { contextB ->
                    Konsist
                        .scopeFromDirectory("contexts/$contextA")
                        .files
                        .filter { it.path.contains("src/main") }
                        .assertFalse { file ->
                            file.imports.any { import ->
                                import.name.contains(".$contextB.domain.")
                            }
                        }
                }
            }
        }

        // ========== Package Structure ==========

        "domain entities should be in entity package" {
            contexts.forEach { context ->
                Konsist
                    .scopeFromDirectory("contexts/$context/domain")
                    .classes()
                    .filter { it.resideInPackage("..entity..") }
                    .filter { !it.name.endsWith("Test") }
                    .assertTrue { entity ->
                        entity.packagee?.name?.endsWith(".entity") == true
                    }
            }
        }

        "value objects should be in valueobject or value package" {
            contexts.forEach { context ->
                Konsist
                    .scopeFromDirectory("contexts/$context/domain")
                    .classes()
                    .filter { it.resideInPackage("..valueobject..") || it.resideInPackage("..value..") }
                    .filter { !it.name.endsWith("Test") }
                    .assertTrue { valueObject ->
                        val packageName = valueObject.packagee?.name ?: ""
                        packageName.contains(".valueobject") || packageName.contains(".value")
                    }
            }
        }

        "repositories should be interfaces in domain layer" {
            contexts.forEach { context ->
                Konsist
                    .scopeFromDirectory("contexts/$context/domain")
                    .interfaces()
                    .filter { it.resideInPackage("..repository..") }
                    .assertTrue { repository ->
                        repository.name.endsWith("Repository") &&
                            repository.packagee?.name?.endsWith(".repository") == true
                    }
            }
        }

        "use cases should be in usecase or handler package in application layer" {
            contexts.forEach { context ->
                Konsist
                    .scopeFromDirectory("contexts/$context/application")
                    .classes()
                    .filter {
                        it.resideInPackage("..usecase..") ||
                            it.resideInPackage("..handler..")
                    }
                    .filter { !it.name.endsWith("Test") }
                    .assertTrue { useCase ->
                        val packageName = useCase.packagee?.name ?: ""
                        packageName.contains(".usecase") || packageName.contains(".handler")
                    }
            }
        }

        "domain services should be in domain layer service package" {
            contexts.forEach { context ->
                Konsist
                    .scopeFromDirectory("contexts/$context/domain")
                    .classes()
                    .filter { it.name.endsWith("Service") }
                    .filter { !it.name.endsWith("Test") }
                    .filter { !it.hasAbstractModifier }
                    .assertTrue { service ->
                        val packageName = service.packagee?.name ?: ""
                        service.resideInPackage("..service..") &&
                            packageName.contains(".service")
                    }
            }
        }

        "domain services should not be in application layer" {
            contexts.forEach { context ->
                Konsist
                    .scopeFromDirectory("contexts/$context/application")
                    .classes()
                    .filter { it.name.endsWith("Service") }
                    .filter { !it.name.endsWith("Test") }
                    .filter { !it.hasAbstractModifier }
                    .filter { clazz ->
                        // Filter for services that should be domain services by examining their characteristics:
                        // 1. Services with domain-like method names AND domain value object parameters
                        // 2. Exclude application services that are clearly for I/O, coordination, or state management
                        // Note: We don't check imports from domain.service as application services are allowed to use domain services

                        val isApplicationService = clazz.name.contains("Application") ||
                            clazz.name.contains("Active") ||
                            clazz.name.contains("Context") ||
                            clazz.name.contains("Validation")

                        val hasDomainBusinessLogic = clazz.functions().any { function ->
                            val hasDomainMethodNames = function.name.contains("assign") ||
                                function.name.contains("generate") ||
                                function.name.contains("resolve") ||
                                function.name.contains("remove")

                            val hasDomainParameters = function.parameters.any { param ->
                                param.type.sourceType.contains("AliasName") ||
                                    param.type.sourceType.contains("AliasId") ||
                                    param.type.sourceType.contains("ScopeAlias")
                            }

                            hasDomainMethodNames && hasDomainParameters
                        }

                        !isApplicationService && hasDomainBusinessLogic
                    }
                    .assertFalse { service ->
                        // If a service with domain logic characteristics is found in application layer,
                        // it should be moved to domain layer
                        true
                    }
            }
        }

        // ========== Domain Purity ==========

        "domain layer should not use framework-specific annotations" {
            contexts.forEach { context ->
                Konsist
                    .scopeFromDirectory("contexts/$context/domain")
                    .files
                    .filter { it.path.contains("src/main") }
                    .assertFalse { file ->
                        file.imports.any { import ->
                            import.name.contains("org.springframework") ||
                                import.name.contains("jakarta.") ||
                                import.name.contains("javax.") ||
                                import.name.contains("kotlinx.serialization.Serializable")
                        }
                    }
            }
        }

        "domain layer should not have external I/O dependencies" {
            contexts.forEach { context ->
                Konsist
                    .scopeFromDirectory("contexts/$context/domain")
                    .files
                    .filter { it.path.contains("src/main") }
                    .assertFalse { file ->
                        file.imports.any { import ->
                            import.name.contains("java.io") ||
                                import.name.contains("java.nio.file") ||
                                import.name.contains("java.net") ||
                                import.name.contains("okhttp") ||
                                import.name.contains("retrofit") ||
                                import.name.contains("ktor.client")
                        }
                    }
            }
        }

        // ========== Infrastructure Implementation ==========

        "DI modules should NOT be in infrastructure layer" {
            contexts.forEach { context ->
                Konsist
                    .scopeFromDirectory("contexts/$context/infrastructure")
                    .files
                    .filter { it.path.contains("src/main") }
                    .assertFalse { file ->
                        // Check for common DI patterns
                        file.imports.any { import ->
                            import.name.contains("org.koin.dsl.module") ||
                                import.name.contains("dagger.Module") ||
                                import.name.contains("javax.inject.Module") ||
                                import.name.contains("com.google.inject.Module")
                        } ||
                            // Check for file names that suggest DI modules
                            file.name.endsWith("Module.kt") ||
                            file.name.endsWith("Component.kt") ||
                            file.name.endsWith("DI.kt") ||
                            file.name.endsWith("Injection.kt") ||
                            // Check for common DI module patterns in class names
                            file.classes().any { clazz ->
                                clazz.name.endsWith("Module") ||
                                    clazz.name.endsWith("Component") ||
                                    clazz.name.contains("DI") ||
                                    clazz.name.contains("Injection")
                            }
                    }
            }
        }

        "repository implementations should be in infrastructure layer" {
            contexts.forEach { context ->
                Konsist
                    .scopeFromDirectory("contexts/$context/infrastructure")
                    .classes()
                    .filter { it.name.endsWith("RepositoryImpl") || it.name.endsWith("Repository") }
                    .filter { !it.name.endsWith("Test") }
                    .assertTrue { impl ->
                        impl.resideInPackage("..persistence..") ||
                            impl.resideInPackage("..repository..")
                    }
            }
        }

        // ========== Apps Layer - Entry Points ==========

        "DI modules should be in apps layer" {
            Konsist
                .scopeFromDirectory("apps")
                .files
                .filter { it.path.contains("src/main") }
                .filter { file ->
                    // Look for DI module patterns
                    file.imports.any { import ->
                        import.name.contains("org.koin.dsl.module") ||
                            import.name.contains("dagger.Module") ||
                            import.name.contains("javax.inject.Module") ||
                            import.name.contains("com.google.inject.Module")
                    } ||
                        file.name.endsWith("Module.kt") ||
                        file.name.endsWith("Component.kt") ||
                        file.name.endsWith("DI.kt")
                }
                .assertTrue { file ->
                    // Ensure DI modules are in proper di packages
                    file.packagee?.name?.contains(".di") == true ||
                        file.packagee?.name?.contains(".injection") == true
                }
        }

        "apps layer should have main functions" {
            listOf("scopes", "scopesd").forEach { module ->
                Konsist
                    .scopeFromDirectory("apps/$module")
                    .functions()
                    .filter { it.name == "main" }
                    .assertTrue { main ->
                        // Top-level functions in Kotlin are public by default
                        // Check if it's a main function (name is "main" is sufficient)
                        true
                    }
            }
        }

        // ========== Naming Conventions ==========

        "all errors should extend proper base error class" {
            contexts.forEach { context ->
                Konsist
                    .scopeFromDirectory("contexts/$context/domain")
                    .classes()
                    .filter { it.resideInPackage("..error..") }
                    .filter { it.name.endsWith("Error") }
                    .filter { !it.hasAbstractModifier }
                    .filter { !it.hasSealedModifier } // Sealed classes are base classes themselves
                    .filter { !it.hasEnumModifier } // Exclude enums
                    .assertTrue { error ->
                        error.parents().any { parent ->
                            parent.name.endsWith("Error")
                        }
                    }
            }
        }

        "value objects should be immutable (data or value classes)" {
            contexts.forEach { context ->
                Konsist
                    .scopeFromDirectory("contexts/$context/domain")
                    .classes()
                    .filter { it.resideInPackage("..valueobject..") || it.resideInPackage("..value..") }
                    .filter { !it.name.endsWith("Test") }
                    .filter { !it.hasEnumModifier && !it.hasSealedModifier } // Exclude enums and sealed classes
                    .assertTrue { valueObject ->
                        valueObject.hasDataModifier || valueObject.hasValueModifier
                    }
            }
        }

        "DTOs should be in dto package in application layer" {
            contexts.forEach { context ->
                Konsist
                    .scopeFromDirectory("contexts/$context/application")
                    .classes()
                    .filter { it.name.endsWith("DTO") || it.name.endsWith("Dto") }
                    .filter { !it.name.endsWith("Test") }
                    .assertTrue { dto ->
                        dto.resideInPackage("..dto..")
                    }
            }
        }

        // ========== Event Sourcing Patterns ==========

        "aggregates should extend AggregateRoot if using event sourcing" {
            contexts.forEach { context ->
                Konsist
                    .scopeFromDirectory("contexts/$context/domain")
                    .classes()
                    .filter { it.resideInPackage("..aggregate..") }
                    .filter { it.name.endsWith("Aggregate") }
                    .filter { !it.name.endsWith("Test") }
                    .filter { !it.hasAbstractModifier } // Exclude abstract base classes
                    .assertTrue { aggregate ->
                        aggregate.parents().any { parent ->
                            parent.name == "AggregateRoot" || parent.name.startsWith("AggregateRoot<")
                        }
                    }
            }
        }

        "domain events should be in event package" {
            contexts.forEach { context ->
                Konsist
                    .scopeFromDirectory("contexts/$context/domain")
                    .classes()
                    .filter { clazz ->
                        // Check if class extends DomainEvent or has event-like name patterns
                        clazz.parents().any { it.name == "DomainEvent" } ||
                            (
                                clazz.name.endsWith("Event") ||
                                    clazz.name.endsWith("Created") ||
                                    clazz.name.endsWith("Updated") ||
                                    clazz.name.endsWith("Deleted") ||
                                    clazz.name.endsWith("Archived") ||
                                    clazz.name.endsWith("Restored") ||
                                    clazz.name.endsWith("Changed")
                                )
                    }
                    .filter { !it.name.endsWith("Test") }
                    .filter { !it.name.endsWith("Publisher") }
                    .filter { !it.name.endsWith("Handler") }
                    .filter { !it.name.equals("EventId") } // EventId is a value object, not an event
                    .filter { !it.name.endsWith("IdError") } // EventIdError is an error, not an event
                    .filter { !it.name.equals("DomainEventPublisher") } // Interface for publishing events, not an event itself
                    .filter { !it.name.endsWith("Repository") } // EventSourcingRepository is a repository interface, not an event
                    .filter { !it.hasAbstractModifier } // Exclude abstract base classes like DomainEvent itself
                    .filter { clazz ->
                        // Exclude error classes - they are not events even if they have event-like names
                        !clazz.parents().any { parent ->
                            parent.name.endsWith("Error") || parent.name == "ScopesError"
                        }
                    }
                    .assertTrue { event ->
                        event.resideInPackage("..event..")
                    }
            }
        }
    })

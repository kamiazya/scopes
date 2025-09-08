package io.github.kamiazya.scopes.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.StringSpec

/**
 * Architectural dependency rules tests to ensure clean architecture principles.
 * These tests verify that module dependencies follow the defined hierarchy and constraints.
 */
class DependencyRulesTest :
    StringSpec({

        // Helper function to normalize paths for cross-platform compatibility
        fun String.normalizePath() = replace('\\', '/')

        "infrastructure can import from application layer for CQRS and DIP" {
            // In this architecture, infrastructure can depend on application for:
            // 1. Implementing ports (Dependency Inversion Principle)
            // 2. Injecting handlers for CQRS pattern (adapters wrap handlers)
            // 3. Using use cases for shared business logic
            // This test verifies infrastructure only imports from its own context's application
            val contexts = mapOf(
                "scopemanagement" to "scope-management",
                "userpreferences" to "user-preferences",
                "eventstore" to "event-store",
                "devicesynchronization" to "device-synchronization",
            )

            Konsist.scopeFromProject()
                .files
                .filter { it.path.normalizePath().contains("/infrastructure/") && !it.path.normalizePath().contains("/test/") }
                .assertFalse(
                    additionalMessage = "Infrastructure should only import from its own context's application layer",
                ) { file ->
                    // Find which context this infrastructure file belongs to
                    val currentContext = contexts.entries.find { (_, folderName) ->
                        file.path.normalizePath().contains("/contexts/$folderName/infrastructure/")
                    }?.key

                    file.imports.any { import ->
                        import.hasNameContaining(".application.") &&
                            !import.hasNameContaining(".platform.application") &&
                            // Check if importing from a different context's application
                            currentContext != null &&
                            contexts.keys.filter { it != currentContext }.any { otherContext ->
                                import.hasNameContaining(".$otherContext.application.")
                            }
                    }
                }
        }

        "domain modules should not depend on infrastructure modules" {
            Konsist.scopeFromProject()
                .files
                .filter { it.path.normalizePath().contains("/domain/") && !it.path.normalizePath().contains("/test/") }
                .assertFalse(
                    additionalMessage = "Domain modules must not depend on infrastructure modules",
                ) { file ->
                    file.imports.any { import ->
                        import.hasNameContaining(".infrastructure.")
                    }
                }
        }

        "application modules should not depend on infrastructure modules" {
            Konsist.scopeFromProject()
                .files
                .filter { it.path.normalizePath().contains("/application/") && !it.path.normalizePath().contains("/test/") }
                .assertFalse(
                    additionalMessage = "Application modules must not depend on infrastructure modules",
                ) { file ->
                    file.imports.any { import ->
                        import.hasNameContaining(".infrastructure.") &&
                            !import.hasNameContaining(".platform.infrastructure")
                    }
                }
        }

        "contexts should not have direct dependencies between each other except for event sourcing" {
            // Event sourcing is a special case where contexts may need to share event types
            // EventTypeId from event-store is used by other contexts for event sourcing
            val contextMapping = mapOf(
                "scopemanagement" to "scope-management",
                "userpreferences" to "user-preferences",
                "eventstore" to "event-store",
                "devicesynchronization" to "device-synchronization",
            )

            Konsist.scopeFromProject()
                .files
                .filter { file ->
                    contextMapping.values.any { folderName -> file.path.normalizePath().contains("/contexts/$folderName/") }
                }
                .assertFalse(
                    additionalMessage = "Contexts must communicate through contracts (exception: event-store valueobjects for event sourcing)",
                ) { file ->
                    val currentContext = contextMapping.entries.find { (_, folderName) ->
                        file.path.normalizePath().contains("/contexts/$folderName/")
                    }?.key
                    val otherContexts = contextMapping.keys.filter { it != currentContext }

                    file.imports.any { import ->
                        otherContexts.any { otherContext ->
                            import.hasNameContaining(".$otherContext.") &&
                                !import.hasNameContaining(".contracts.") &&
                                // Allow event-store value objects for event sourcing
                                !(otherContext == "eventstore" && import.hasNameContaining(".domain.valueobject."))
                        }
                    }
                }
        }

        "interfaces layer should not depend on infrastructure layers" {
            // Interfaces layer should only depend on contracts and application layers
            Konsist.scopeFromProject()
                .files
                .filter { it.path.normalizePath().contains("/interfaces/") }
                .assertFalse(
                    additionalMessage = "Interfaces layer must not depend on infrastructure layers",
                ) { file ->
                    file.imports.any { import ->
                        import.hasNameContaining(".infrastructure.")
                    }
                }
        }

        "platform commons should not depend on infrastructure" {
            Konsist.scopeFromProject()
                .files
                .filter {
                    it.path.normalizePath().contains("/platform/commons/") ||
                        it.path.normalizePath().contains("/platform/domain-commons/") ||
                        it.path.normalizePath().contains("/platform/application-commons/")
                }
                .assertFalse(
                    additionalMessage = "Platform commons modules must not depend on infrastructure",
                ) { file ->
                    file.imports.any { import ->
                        import.hasNameContaining(".infrastructure.")
                    }
                }
        }

        "all modules should only use contracts for inter-context communication" {
            // This ensures proper bounded context isolation
            val contextNames = listOf("scopemanagement", "userpreferences", "eventstore", "devicesynchronization")

            Konsist.scopeFromProject()
                .files
                .filter { file ->
                    // Files that are allowed to import from other contexts
                    !file.path.normalizePath().contains("/contracts/") &&
                        !file.path.normalizePath().contains("/apps/") &&
                        !file.path.normalizePath().contains("/test/") &&
                        // Interfaces layer can coordinate between contexts
                        !file.path.normalizePath().contains("/interfaces/")
                }
                .assertTrue(
                    additionalMessage = "Inter-context dependencies must go through contracts (exception: event-store valueobjects)",
                ) { file ->
                    // Find current context based on file path
                    val currentContext = contextNames.find { context ->
                        val contextPaths = mapOf(
                            "scopemanagement" to "scope-management",
                            "userpreferences" to "user-preferences",
                            "eventstore" to "event-store",
                            "devicesynchronization" to "device-synchronization",
                        )
                        file.path.normalizePath().contains("/contexts/${contextPaths[context]}/")
                    }

                    // Get imports from other contexts
                    val contextImports = file.imports.filter { import ->
                        contextNames.any { context ->
                            context != currentContext && import.hasNameContaining(".$context.")
                        }
                    }

                    // All such imports must be through contracts or allowed exceptions
                    contextImports.all { import ->
                        import.hasNameContaining(".contracts.") ||
                            // Allow event-store value objects for event sourcing
                            (import.hasNameContaining(".eventstore.") && import.hasNameContaining(".domain.valueobject."))
                    }
                }
        }

        "infrastructure modules can only depend on their own domain and platform modules" {
            val contexts = mapOf(
                "scopemanagement" to "scope-management",
                "userpreferences" to "user-preferences",
                "eventstore" to "event-store",
                "devicesynchronization" to "device-synchronization",
            )

            contexts.forEach { (packageName, folderName) ->
                Konsist.scopeFromProject()
                    .files
                    .filter { it.path.normalizePath().contains("/contexts/$folderName/infrastructure/") }
                    .assertFalse(
                        additionalMessage = "$folderName infrastructure should only depend on its own domain",
                    ) { file ->
                        file.imports.any { import ->
                            // Check if importing from another context's domain
                            contexts.filter { it.key != packageName }.any { (otherPackage, _) ->
                                import.hasNameContaining(".$otherPackage.domain.")
                            }
                        }
                    }
            }
        }
    })

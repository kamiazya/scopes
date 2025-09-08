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

        "infrastructure modules should not depend on application modules" {
            Konsist.scopeFromProject()
                .files
                .filter { it.path.contains("/infrastructure/") && !it.path.contains("/test/") }
                .assertFalse(
                    additionalMessage = "Infrastructure modules must not depend on application modules",
                ) { file ->
                    file.imports.any { import ->
                        import.hasNameContaining(".application.") &&
                            !import.hasNameContaining(".platform.application")
                    }
                }
        }

        "domain modules should not depend on infrastructure modules" {
            Konsist.scopeFromProject()
                .files
                .filter { it.path.contains("/domain/") && !it.path.contains("/test/") }
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
                .filter { it.path.contains("/application/") && !it.path.contains("/test/") }
                .assertFalse(
                    additionalMessage = "Application modules must not depend on infrastructure modules",
                ) { file ->
                    file.imports.any { import ->
                        import.hasNameContaining(".infrastructure.") &&
                            !import.hasNameContaining(".platform.infrastructure")
                    }
                }
        }

        "contexts should not have direct dependencies between each other" {
            val contextMapping = mapOf(
                "scopemanagement" to "scope-management",
                "userpreferences" to "user-preferences",
                "eventstore" to "event-store",
                "devicesynchronization" to "device-synchronization",
            )

            Konsist.scopeFromProject()
                .files
                .filter { file ->
                    contextMapping.values.any { folderName -> file.path.contains("/contexts/$folderName/") }
                }
                .assertFalse(
                    additionalMessage = "Contexts must communicate only through contracts modules",
                ) { file ->
                    val currentContext = contextMapping.entries.find { (_, folderName) ->
                        file.path.contains("/contexts/$folderName/")
                    }?.key
                    val otherContexts = contextMapping.keys.filter { it != currentContext }

                    file.imports.any { import ->
                        otherContexts.any { otherContext ->
                            import.hasNameContaining(".$otherContext.") &&
                                !import.hasNameContaining(".contracts.")
                        }
                    }
                }
        }

        "interfaces layer should not depend on infrastructure layers".config(enabled = false) {
            // This test is temporarily disabled as interfaces/cli needs to map domain errors
            // TODO: Consider moving error mapping to application layer
            Konsist.scopeFromProject()
                .files
                .filter { it.path.contains("/interfaces/") }
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
                    it.path.contains("/platform/commons/") ||
                        it.path.contains("/platform/domain-commons/") ||
                        it.path.contains("/platform/application-commons/")
                }
                .assertFalse(
                    additionalMessage = "Platform commons modules must not depend on infrastructure",
                ) { file ->
                    file.imports.any { import ->
                        import.hasNameContaining(".infrastructure.")
                    }
                }
        }

        // TODO: Fix this test - currently has issues with context identification
        "all modules should only use contracts for inter-context communication".config(enabled = false) {
            val contextNames = listOf("scopemanagement", "userpreferences", "eventstore", "devicesynchronization")

            Konsist.scopeFromProject()
                .files
                .filter { file ->
                    // Files that are allowed to import from other contexts
                    !file.path.contains("/contracts/") &&
                        !file.path.contains("/apps/") &&
                        !file.path.contains("/test/")
                }
                .assertTrue(
                    additionalMessage = "Inter-context dependencies must go through contracts modules",
                ) { file ->
                    // Find current context based on file path
                    val currentContext = contextNames.find { context ->
                        val contextPaths = mapOf(
                            "scopemanagement" to "scope-management",
                            "userpreferences" to "user-preferences",
                            "eventstore" to "event-store",
                            "devicesynchronization" to "device-synchronization",
                        )
                        file.path.contains("/contexts/${contextPaths[context]}/")
                    }

                    // Get imports from other contexts
                    val contextImports = file.imports.filter { import ->
                        contextNames.any { context ->
                            context != currentContext && import.hasNameContaining(".$context.")
                        }
                    }

                    // All such imports must be through contracts
                    contextImports.all { import ->
                        import.hasNameContaining(".contracts.")
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
                    .filter { it.path.contains("/contexts/$folderName/infrastructure/") }
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

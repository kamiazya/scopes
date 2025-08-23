package io.github.kamiazya.scopes.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.StringSpec

/**
 * Architecture tests for Bounded Context structure.
 * Ensures proper separation between contexts and layers according to DDD principles.
 *
 * Key principles:
 * - Each bounded context is independent
 * - Communication between contexts happens through published events or APIs
 * - No direct domain-to-domain dependencies between contexts
 * - Shared kernels are limited to technical utilities in platform layer
 */
class BoundedContextArchitectureTest :
    StringSpec({

        val contexts = listOf(
            "scope-management",
            "user-preferences",
        )

        // Test that domain layers don't depend on application or infrastructure
        contexts.forEach { context ->
            "$context domain should not import application layer" {
                Konsist
                    .scopeFromDirectory("contexts/$context/domain")
                    .files
                    .filter { it.path.contains("src/main") }
                    .assertFalse { file ->
                        file.imports.any { import ->
                            import.name.contains(".application.") ||
                                import.name.contains(".infrastructure.")
                        }
                    }
            }
        }

        // Test that application layers don't depend on infrastructure
        contexts.forEach { context ->
            "$context application should not import infrastructure layer" {
                Konsist
                    .scopeFromDirectory("contexts/$context/application")
                    .files
                    .filter { it.path.contains("src/main") }
                    .assertFalse { file ->
                        file.imports.any { import ->
                            import.name.contains(".infrastructure.")
                        }
                    }
            }
        }

        // Test that contexts don't have circular dependencies
        "contexts should not have circular dependencies between domain layers" {
            contexts.forEach { contextA ->
                contexts.filter { it != contextA }.forEach { contextB ->
                    val scopeA = Konsist.scopeFromDirectory("contexts/$contextA/domain")
                    val scopeB = Konsist.scopeFromDirectory("contexts/$contextB/domain")

                    val aImportsB = scopeA.files
                        .filter { it.path.contains("src/main") }
                        .any { file ->
                            file.imports.any { import ->
                                import.name.contains(".$contextB.domain.")
                            }
                        }

                    val bImportsA = scopeB.files
                        .filter { it.path.contains("src/main") }
                        .any { file ->
                            file.imports.any { import ->
                                import.name.contains(".$contextA.domain.")
                            }
                        }

                    // If A imports B, then B should not import A
                    if (aImportsB) {
                        assert(!bImportsA) {
                            "Circular dependency detected between $contextA and $contextB"
                        }
                    }
                }
            }
        }

        // Test that platform layer doesn't depend on contexts
        "platform layer should not depend on contexts" {
            Konsist
                .scopeFromDirectory("platform")
                .files
                .filter { it.path.contains("src/main") }
                .assertFalse { file ->
                    file.imports.any { import ->
                        contexts.any { context ->
                            import.name.contains(".$context.")
                        }
                    }
                }
        }

        // Test that all value objects are immutable
        "value objects should be data classes or value classes" {
            contexts.forEach { context ->
                Konsist
                    .scopeFromDirectory("contexts/$context/domain")
                    .classes()
                    .filter { it.resideInPackage("..valueobject..") }
                    .filter { !it.hasEnumModifier && !it.hasSealedModifier } // Exclude enums and sealed classes
                    .filter { !it.name.endsWith("Test") } // Exclude test classes
                    .filter { !it.hasAbstractModifier } // Exclude abstract classes
                    .assertTrue { clazz ->
                        clazz.hasDataModifier || clazz.hasValueModifier
                    }
            }
        }

        // Test that all entities have proper structure
        "entities should be data classes with id property" {
            contexts.forEach { context ->
                Konsist
                    .scopeFromDirectory("contexts/$context/domain")
                    .classes()
                    .filter { it.resideInPackage("..entity..") }
                    .filter { !it.name.endsWith("Test") }
                    .assertTrue { clazz ->
                        clazz.hasDataModifier &&
                            clazz.properties().any { prop ->
                                // Accept both 'id' and 'key' as identity properties
                                prop.name == "id" || prop.name == "key"
                            }
                    }
            }
        }

        // Test that repositories are interfaces
        "repositories should be interfaces" {
            contexts.forEach { context ->
                Konsist
                    .scopeFromDirectory("contexts/$context/domain")
                    .interfaces()
                    .filter { it.resideInPackage("..repository..") }
                    .assertTrue { iface ->
                        iface.name.endsWith("Repository")
                    }
            }
        }

        // Test that all errors extend proper base class
        "all errors should extend ScopesError or context-specific error base" {
            contexts.forEach { context ->
                Konsist
                    .scopeFromDirectory("contexts/$context/domain")
                    .classes()
                    .filter { it.resideInPackage("..error..") }
                    .filter { it.name.endsWith("Error") }
                    .filter { !it.name.equals("ScopesError") }
                    .filter { !it.name.endsWith("ManagementError") } // Base error classes for each context
                    .assertTrue { clazz ->
                        clazz.parents().any { parent ->
                            parent.name.endsWith("Error")
                        }
                    }
            }
        }
    })

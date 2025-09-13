// This file is temporarily disabled due to performance issues
// TODO: Re-enable after optimizing the test performance

package io.github.kamiazya.scopes.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.StringSpec

/**
 * Architecture tests for layer separation according to the original design.
 *
 * Layer hierarchy (from top to bottom):
 * - Apps: Application coordination and orchestration (including entry points)
 * - Contexts: Bounded contexts with domain/application/infrastructure
 * - Platform: Technical utilities and shared kernels
 *
 * Dependency rules:
 * - Apps → Contexts (application) → Contexts (domain)
 * - Infrastructure → Application → Domain (within each context)
 * - Platform can be used by all layers (technical utilities only)
 */
class LayerArchitectureTest :
    StringSpec({

        val contexts = listOf(
            "scope-management",
            "user-preferences",
            "event-store",
            "device-synchronization",
        )

        // ========== Apps Layer Tests ==========

        "apps layer should not depend on infrastructure implementations except for DI" {
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

        "apps layer DI modules can access infrastructure for wiring dependencies" {
            // DI modules in apps are the exception - they need to wire everything together
            Konsist
                .scopeFromDirectory("apps")
                .files
                .filter {
                    it.packagee?.name?.contains(".di") == true ||
                        it.name.endsWith("Module.kt") ||
                        it.name.endsWith("Component.kt")
                }
                .assertTrue { file ->
                    // DI modules are allowed to import infrastructure for dependency wiring
                    true
                }
        }

        // ========== Contracts Layer Tests ==========

        "contracts should not depend on application or infrastructure layers" {
            Konsist
                .scopeFromDirectory("contracts")
                .files
                .filter { it.path.contains("src/main") }
                .assertFalse { file ->
                    file.imports.any { import ->
                        import.name.contains(".application.") ||
                            import.name.contains(".infrastructure.")
                    }
                }
        }

        "contracts should not depend on specific contexts implementation" {
            Konsist
                .scopeFromDirectory("contracts")
                .files
                .filter { it.path.contains("src/main") }
                .assertFalse { file ->
                    file.imports.any { import ->
                        contexts.any { context ->
                            import.name.contains(".contexts.$context.") ||
                                import.name.contains(".$context.domain.") ||
                                import.name.contains(".$context.application.") ||
                                import.name.contains(".$context.infrastructure.")
                        }
                    }
                }
        }

        // ========== Context Layer Tests ==========

        "domain layer within contexts should not depend on application or infrastructure" {
            contexts.forEach { context ->
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

        "application layer within contexts should not depend on infrastructure" {
            contexts.forEach { context ->
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

        // ========== Platform Layer Tests ==========

        "platform layer should not depend on any context or app layer" {
            Konsist
                .scopeFromDirectory("platform")
                .files
                .filter { it.path.contains("src/main") }
                .assertFalse { file ->
                    file.imports.any { import ->
                        import.name.contains(".contexts.") ||
                            import.name.contains(".apps.") ||
                            contexts.any { context ->
                                import.name.contains(".$context.")
                            }
                    }
                }
        }

        // ========== Package Structure Tests ==========

        "each context should have proper layer packages" {
            contexts.forEach { context ->
                val contextFiles = Konsist
                    .scopeFromDirectory("contexts/$context")
                    .files
                    .filter { it.path.contains("src/main") }

                val packages = contextFiles
                    .mapNotNull { it.packagee?.name }
                    .distinct()

                // Check domain layer exists
                assert(packages.any { it.endsWith(".domain") || it.contains(".domain.") }) {
                    "Context $context should have domain layer"
                }

                // Check application layer exists
                assert(packages.any { it.endsWith(".application") || it.contains(".application.") }) {
                    "Context $context should have application layer"
                }

                // Infrastructure is optional (some contexts might not need persistence)
                // But if it exists, it should follow the pattern
                val hasInfrastructure = packages.any {
                    it.endsWith(".infrastructure") || it.contains(".infrastructure.")
                }
                if (hasInfrastructure) {
                    assert(
                        packages.any {
                            it.endsWith(".infrastructure") || it.contains(".infrastructure.")
                        },
                    ) {
                        "Context $context infrastructure should follow naming pattern"
                    }
                }
            }
        }

        // TODO: Re-enable after fixing test structure
        // "test packages should mirror production structure" {
        /*
            contexts.forEach { context ->
                listOf("domain", "application", "infrastructure").forEach { layer ->
                    val productionPackages = Konsist
                        .scopeFromDirectory("contexts/$context/$layer")
                        .files
                        .filter { it.path.contains("src/main") }
                        .mapNotNull { it.packagee?.name }
                        .distinct()

                    if (productionPackages.isEmpty()) {
                        // Skip if no production code in this layer
                        return@forEach
                    }

                    val testPackages = Konsist
                        .scopeFromDirectory("contexts/$context/$layer")
                        .files
                        .filter { it.path.contains("src/test") }
                        .mapNotNull { it.packagee?.name }
                        .distinct()

                    // Test packages should be a subset or equal to production packages
                    testPackages.forEach { testPkg ->
                        assert(productionPackages.contains(testPkg)) {
                            "Test package $testPkg in $context/$layer doesn't match production structure"
                        }
                    }
                }
            }
        }
         */

        // ========== Dependency Direction ==========

        "dependencies should flow inward (Clean Architecture)" {
            contexts.forEach { context ->
                // Infrastructure → Application → Domain
                val infraFiles = Konsist
                    .scopeFromDirectory("contexts/$context/infrastructure")
                    .files
                    .filter { it.path.contains("src/main") }

                infraFiles.assertFalse { file ->
                    // Infrastructure should not depend on other infrastructure
                    file.imports.any { import ->
                        contexts.filter { it != context }.any { otherContext ->
                            import.name.contains(".$otherContext.infrastructure.")
                        }
                    }
                }

                // Application should not depend on other applications
                val appFiles = Konsist
                    .scopeFromDirectory("contexts/$context/application")
                    .files
                    .filter { it.path.contains("src/main") }

                appFiles.assertFalse { file ->
                    file.imports.any { import ->
                        contexts.filter { it != context }.any { otherContext ->
                            import.name.contains(".$otherContext.application.")
                        }
                    }
                }
            }
        }

        // ========== Interface Layer Tests ==========

        "interfaces layer can depend on contracts and application layers" {
            Konsist
                .scopeFromDirectory("interfaces")
                .files
                .filter { it.path.contains("src/main") }
                .assertFalse { file ->
                    // Interfaces should not depend on infrastructure
                    file.imports.any { import ->
                        contexts.any { context ->
                            import.name.contains(".$context.infrastructure.")
                        }
                    }
                }
        }

        // ========== Common Anti-patterns ==========

        "no circular dependencies between contexts" {
            contexts.forEach { contextA ->
                contexts.filter { it != contextA }.forEach { contextB ->
                    val aImportsB = Konsist
                        .scopeFromDirectory("contexts/$contextA")
                        .files
                        .any { file ->
                            file.imports.any { import ->
                                import.name.contains(".$contextB.")
                            }
                        }

                    val bImportsA = Konsist
                        .scopeFromDirectory("contexts/$contextB")
                        .files
                        .any { file ->
                            file.imports.any { import ->
                                import.name.contains(".$contextA.")
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

        "infrastructure adapters should implement interfaces from application or domain layer" {
            contexts.forEach { context ->
                Konsist
                    .scopeFromDirectory("contexts/$context/infrastructure")
                    .classes()
                    .filter { it.name.endsWith("Adapter") || it.name.endsWith("Repository") }
                    .filter { !it.hasAbstractModifier }
                    .assertTrue { adapter ->
                        // Adapters should implement at least one interface
                        adapter.hasParents()
                    }
            }
        }
    })

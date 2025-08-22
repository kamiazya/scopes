package io.github.kamiazya.scopes.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.StringSpec

/**
 * Architecture tests for layer separation according to the original design.
 *
 * Layer hierarchy (from top to bottom):
 * - Boot: Thin entry points for distribution (CLI, daemon)
 * - Apps: Application coordination and orchestration
 * - Contexts: Bounded contexts with domain/application/infrastructure
 * - Platform: Technical utilities and shared kernels
 *
 * Dependency rules:
 * - Boot → Apps → Contexts (application) → Contexts (domain)
 * - Infrastructure → Application → Domain (within each context)
 * - Platform can be used by all layers (technical utilities only)
 */
class LayerArchitectureTest :
    StringSpec({

        val contexts = listOf(
            "scope-management",
        )

        // ========== Boot Layer Tests ==========

        "boot layer should only depend on apps and minimal infrastructure" {
            Konsist
                .scopeFromDirectory("boot")
                .files
                .filter { it.path.contains("src/main") }
                .assertFalse { file ->
                    // Boot should not directly access domain logic
                    file.imports.any { import ->
                        contexts.any { context ->
                            import.name.contains(".$context.domain.")
                        }
                    }
                }
        }

        "boot layer should have proper entry points" {
            listOf("cli-launcher", "daemon-launcher").forEach { module ->
                val scope = Konsist.scopeFromDirectory("boot/$module")

                // Should have a main function (top-level functions in Kotlin are public by default)
                val hasMain = scope.functions()
                    .any { it.name == "main" }

                assert(hasMain) {
                    "Boot module $module should have a public main function"
                }
            }
        }

        "boot layer should be minimal in size" {
            listOf("cli-launcher", "daemon-launcher").forEach { module ->
                val files = Konsist
                    .scopeFromDirectory("boot/$module")
                    .files
                    .filter { it.path.contains("src/main") }

                // Boot layer should be thin - warn if too many files
                assert(files.size <= 10) {
                    "Boot module $module has ${files.size} files - consider if it's too complex for an entry point"
                }
            }
        }

        // ========== Apps Layer Tests ==========

        "apps layer should not depend on infrastructure implementations" {
            Konsist
                .scopeFromDirectory("apps")
                .files
                .filter { it.path.contains("src/main") }
                .assertFalse { file ->
                    file.imports.any { import ->
                        contexts.any { context ->
                            import.name.contains(".$context.infrastructure.")
                        }
                    }
                }
        }

        "apps layer should coordinate through application services" {
            Konsist
                .scopeFromDirectory("apps")
                .files
                .filter { it.path.contains("src/main") }
                .assertTrue { file ->
                    // Apps should primarily use application layer services
                    file.imports.none { import ->
                        contexts.any { context ->
                            import.name.contains(".$context.domain.") &&
                                !import.name.contains(".$context.domain.error.") // Errors are OK
                        }
                    }
                }
        }

        // ========== Platform Layer Tests ==========

        "platform layer should not depend on any business logic" {
            Konsist
                .scopeFromDirectory("platform")
                .files
                .filter { it.path.contains("src/main") }
                .assertFalse { file ->
                    file.imports.any { import ->
                        // Platform should not import from contexts, apps, or boot
                        contexts.any { context ->
                            import.name.contains(".$context.")
                        } ||
                            import.name.contains(".apps.") ||
                            import.name.contains(".boot.")
                    }
                }
        }

        "platform commons should only contain technical utilities" {
            Konsist
                .scopeFromDirectory("platform/commons")
                .classes()
                .filter { !it.name.endsWith("Test") }
                .assertFalse { clazz ->
                    // Should not contain business domain concepts
                    clazz.name.contains("Scope") ||
                        clazz.name.contains("Aspect") ||
                        clazz.name.contains("Alias") ||
                        clazz.name.contains("Context") &&
                        !clazz.name.contains("Transaction")
                }
        }

        "platform observability should only contain logging and tracing" {
            Konsist
                .scopeFromDirectory("platform/observability")
                .classes()
                .filter { !it.name.endsWith("Test") }
                .assertTrue { clazz ->
                    // Should only contain observability-related classes
                    clazz.name.contains("Log") ||
                        clazz.name.contains("Trace") ||
                        clazz.name.contains("Metric") ||
                        clazz.name.contains("Telemetry") ||
                        clazz.name.contains("Observer") ||
                        clazz.name.contains("Monitor") ||
                        clazz.name.contains("Application") ||
                        // ApplicationInfo, ApplicationType
                        clazz.name.contains("Runtime") ||
                        // RuntimeInfo
                        clazz.name.contains("Context") ||
                        // LoggingContext
                        clazz.name.contains("Formatter") ||
                        // LogFormatter classes
                        clazz.name.contains("Appender") ||
                        // LogAppender classes
                        clazz.name.contains("Value") // LogValue classes
                }
        }

        // ========== Context Layer Tests ==========

        "each context should have all three layers" {
            contexts.forEach { context ->
                val layers = listOf("domain", "application", "infrastructure")
                layers.forEach { layer ->
                    val layerExists = Konsist
                        .scopeFromDirectory("contexts/$context/$layer")
                        .files
                        .isNotEmpty()

                    assert(layerExists) {
                        "Context $context should have $layer layer"
                    }
                }
            }
        }

        "infrastructure should implement domain interfaces" {
            contexts.forEach { context ->
                // Get all repository interfaces from domain
                val domainRepositories = Konsist
                    .scopeFromDirectory("contexts/$context/domain")
                    .interfaces()
                    .filter { it.resideInPackage("..repository..") }
                    .map { it.name }

                // Check if infrastructure has implementations
                if (domainRepositories.isNotEmpty()) {
                    val hasImplementations = Konsist
                        .scopeFromDirectory("contexts/$context/infrastructure")
                        .classes()
                        .any { clazz ->
                            domainRepositories.any { repo ->
                                clazz.name == "${repo}Impl" ||
                                    clazz.parents().any { parent -> parent.name == repo }
                            }
                        }

                    assert(hasImplementations) {
                        "Context $context infrastructure should implement domain repositories: $domainRepositories"
                    }
                }
            }
        }

        // ========== Package Organization ==========

        "packages should follow naming conventions" {
            Konsist
                .scopeFromProduction()
                .packages
                .assertTrue { pkg ->
                    // All packages should be lowercase
                    pkg.name == pkg.name.lowercase()
                }
        }

        "test packages should mirror production structure" {
            contexts.forEach { context ->
                listOf("domain", "application", "infrastructure").forEach { layer ->
                    val productionPackages = Konsist
                        .scopeFromDirectory("contexts/$context/$layer")
                        .files
                        .filter { it.path.contains("src/main") }
                        .mapNotNull { it.packagee?.name }
                        .distinct()

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

        // ========== Dependency Direction ==========

        "dependencies should flow inward (Clean Architecture)" {
            contexts.forEach { context ->
                // Infrastructure → Application → Domain
                val infraFiles = Konsist
                    .scopeFromDirectory("contexts/$context/infrastructure")
                    .files
                    .filter { it.path.contains("src/main") }

                // Infrastructure can depend on application and domain
                infraFiles.forEach { file ->
                    file.imports.forEach { import ->
                        if (import.name.contains(".$context.")) {
                            assert(
                                import.name.contains(".$context.application.") ||
                                    import.name.contains(".$context.domain."),
                            ) {
                                "Infrastructure file ${file.path} has invalid import: ${import.name}"
                            }
                        }
                    }
                }

                // Application can only depend on domain
                val appFiles = Konsist
                    .scopeFromDirectory("contexts/$context/application")
                    .files
                    .filter { it.path.contains("src/main") }

                appFiles.forEach { file ->
                    file.imports.forEach { import ->
                        if (import.name.contains(".$context.")) {
                            assert(import.name.contains(".$context.domain.")) {
                                "Application file ${file.path} has invalid import: ${import.name}"
                            }
                        }
                    }
                }
            }
        }

        // ========== Circular Dependencies ==========

        "no circular dependencies between modules" {
            // Create a dependency graph
            val dependencies = mutableMapOf<String, MutableSet<String>>()

            // Collect all module dependencies
            val modules = listOf(
                "platform/commons",
                "platform/observability",
                "platform/application-commons",
            ) + contexts.flatMap { context ->
                listOf(
                    "contexts/$context/domain",
                    "contexts/$context/application",
                    "contexts/$context/infrastructure",
                )
            } + listOf(
                "apps/cli",
                "apps/api",
                "apps/daemon",
                "boot/cli-launcher",
                "boot/daemon-launcher",
            )

            modules.forEach { module ->
                dependencies[module] = mutableSetOf()

                Konsist
                    .scopeFromDirectory(module)
                    .files
                    .filter { it.path.contains("src/main") }
                    .forEach { file ->
                        file.imports.forEach { import ->
                            modules.forEach { otherModule ->
                                val modulePackage = otherModule.replace("/", ".")
                                if (import.name.contains(modulePackage) && module != otherModule) {
                                    dependencies[module]?.add(otherModule)
                                }
                            }
                        }
                    }
            }

            // Check for cycles using DFS
            fun hasCycle(node: String, visited: MutableSet<String>, recursionStack: MutableSet<String>): Boolean {
                visited.add(node)
                recursionStack.add(node)

                dependencies[node]?.forEach { neighbor ->
                    if (!visited.contains(neighbor)) {
                        if (hasCycle(neighbor, visited, recursionStack)) {
                            return true
                        }
                    } else if (recursionStack.contains(neighbor)) {
                        return true
                    }
                }

                recursionStack.remove(node)
                return false
            }

            modules.forEach { module ->
                val visited = mutableSetOf<String>()
                val recursionStack = mutableSetOf<String>()

                assert(!hasCycle(module, visited, recursionStack)) {
                    "Circular dependency detected involving module: $module"
                }
            }
        }
    })

package io.github.kamiazya.scopes.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.StringSpec

/**
 * Konsist tests for import organization and standardization.
 * Ensures clean, explicit imports and prevents fully-qualified names in code.
 */
class ImportOrganizationTest :
    StringSpec({

        "no fully-qualified domain error types in when expressions" {
            Konsist
                .scopeFromProduction()
                .files
                .assertFalse { file ->
                    val content = file.text
                    // Check for fully-qualified error types in when expressions
                    content.contains(
                        Regex(
                            """when\s*\([^)]*\)\s*\{[^}]*is\s+io\.github\.kamiazya\.scopes\.(domain|application|infrastructure)\.error\.[A-Z][A-Za-z0-9]*Error\b""",
                        ),
                    )
                }
        }

        "wildcard imports should only be used for error packages from the same module" {
            Konsist
                .scopeFromProduction()
                .files
                .assertTrue { file ->
                    val wildcardImports = file.imports.filter { it.isWildcard }

                    wildcardImports.all { import ->
                        val importPath = import.name.removeSuffix(".*")
                        val filePackage = file.packagee?.name ?: ""

                        when {
                            // Allow Kotlin standard library wildcard imports only (no java.* for KMP compatibility)
                            importPath.startsWith("kotlin.") ||
                                importPath.startsWith("kotlinx.") -> true

                            // Allow wildcard imports for error packages following Clean Architecture dependencies
                            importPath.endsWith(".error") -> {
                                // Check if the import is from the same bounded context
                                val fileContext = filePackage.substringAfter("io.github.kamiazya.scopes.")
                                    .substringBefore(".domain")
                                    .substringBefore(".application")
                                    .substringBefore(".infrastructure")
                                val importContext = importPath.substringAfter("io.github.kamiazya.scopes.")
                                    .substringBefore(".domain")
                                    .substringBefore(".application")
                                    .substringBefore(".infrastructure")

                                // Allow if from same context and respecting layer dependencies
                                if (fileContext == importContext) {
                                    val fileLayer = when {
                                        filePackage.contains(".domain.") -> "domain"
                                        filePackage.contains(".application.") -> "application"
                                        filePackage.contains(".infrastructure.") -> "infrastructure"
                                        else -> null
                                    }
                                    val importLayer = when {
                                        importPath.contains(".domain.") -> "domain"
                                        importPath.contains(".application.") -> "application"
                                        importPath.contains(".infrastructure.") -> "infrastructure"
                                        else -> null
                                    }

                                    // Check layer dependencies
                                    when (fileLayer) {
                                        "domain" -> importLayer == "domain"
                                        "application" -> importLayer in listOf("domain", "application")
                                        "infrastructure" -> importLayer in listOf("domain", "application", "infrastructure")
                                        else -> false
                                    }
                                } else {
                                    false
                                }
                            }

                            else -> false // Other wildcard imports not allowed
                        }
                    }
                }
        }

        "error classes should be imported explicitly in service implementations" {
            Konsist
                .scopeFromProduction()
                .classes()
                .filter { it.name.endsWith("Service") || it.name.endsWith("Handler") }
                .assertTrue { serviceClass ->
                    val file = serviceClass.containingFile
                    val content = file.text

                    // Check for fully-qualified error class names in code (excluding import statements)
                    val codeWithoutImports = content.lines()
                        .filterNot { it.trim().startsWith("import ") }
                        .joinToString("\n")

                    // Allow fully-qualified names if they are properly imported or used sparingly
                    val fullyQualifiedErrorUsages = Regex(
                        """\bio\.github\.kamiazya\.scopes\.(domain|application|infrastructure)\.error\.[A-Z][a-zA-Z]*Error\b""",
                    ).findAll(codeWithoutImports).toList()

                    // If there are fully-qualified usages, they should be minimal (less than 3)
                    // This allows for occasional usage while encouraging imports
                    fullyQualifiedErrorUsages.size < 3
                }
        }

        "no redundant imports" {
            Konsist
                .scopeFromProduction()
                .files
                .assertFalse { file ->
                    val imports = file.imports.map { it.name }
                    // Check for duplicate imports
                    imports.size != imports.distinct().size
                }
        }

        "no java.* imports for Kotlin Multiplatform compatibility" {
            Konsist
                .scopeFromProduction()
                .files
                .filter { file ->
                    // Exclude platform-infrastructure module which is JVM-specific
                    !file.path.contains("platform/infrastructure") &&
                        // Exclude daemon and gRPC modules which are JVM-specific
                        !file.path.contains("apps/scopesd") &&
                        !file.path.contains("interfaces/daemon-grpc") &&
                        !file.path.contains("interfaces/cli-grpc") &&
                        !file.path.contains("interfaces/rpc-contracts")
                }
                .assertFalse { file ->
                    file.imports.any { import ->
                        import.name.startsWith("java.")
                    }
                }
        }
    })

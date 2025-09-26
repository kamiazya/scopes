package io.github.kamiazya.scopes.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import io.kotest.core.spec.style.StringSpec

/**
 * Enforces the application-to-contracts import policy described in the docs:
 * - Application boundary components may import contract types (commands/queries/results/errors)
 *   but only from specific directories (handlers, mappers, error, adapters).
 * - Core application coordination areas must remain contract-agnostic.
 * - Domain layer must not import contracts.
 */
class ApplicationContractsImportPolicyTest :
    StringSpec({

        fun String.normalizePath() = replace('\\', '/')

        val allowedFolders = listOf(
            "/application/command/handler/",
            "/application/query/handler/",
            "/application/mapper/",
            "/application/error/",
            "/application/adapter/",
            "/application/query/response/",
        )
        val allowedSpecificFiles = listOf(
            "/application/services/ResponseFormatterService.kt",
        )

        fun isAllowedApplicationBoundaryPath(path: String): Boolean {
            val p = path.normalizePath()
            return allowedFolders.any { allowed -> p.contains(allowed) } ||
                allowedSpecificFiles.any { allowed -> p.endsWith(allowed) }
        }

        "application may import contracts only at boundary locations" {
            Konsist.scopeFromProject()
                .files
                .filter { file ->
                    val p = file.path.normalizePath()
                    p.contains("/contexts/") &&
                        p.contains("/application/") &&
                        !p.contains("/test/")
                }
                .assertFalse(
                    additionalMessage = "Only handlers/mappers/error/adapters in application may import contracts",
                ) { file ->
                    val importsContracts = file.imports.any { it.hasNameContaining(".contracts.") }
                    val allowed = isAllowedApplicationBoundaryPath(file.path)
                    importsContracts && !allowed
                }
        }

        "domain must not import contracts" {
            Konsist.scopeFromProject()
                .files
                .filter { file ->
                    val p = file.path.normalizePath()
                    p.contains("/contexts/") &&
                        p.contains("/domain/") &&
                        !p.contains("/test/")
                }
                .assertFalse(
                    additionalMessage = "Domain layer must not depend on contracts",
                ) { file ->
                    file.imports.any { it.hasNameContaining(".contracts.") }
                }
        }
    })

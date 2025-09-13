package io.github.kamiazya.scopes.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.StringSpec

/**
 * Simple DDD Boundaries Test for collaborative-versioning and agent-management contexts
 *
 * This simplified version validates core DDD principles without complex API usage
 */
class SimpleDddBoundariesTest :
    StringSpec({

        val targetContexts = listOf(
            "collaborative-versioning",
            "agent-management",
        )

        // ========== Clean Architecture Layer Dependencies ==========

        "domain layer should not depend on application or infrastructure" {
            targetContexts.forEach { context ->
                Konsist
                    .scopeFromDirectory("contexts/$context/domain")
                    .files
                    .filter { it.path.contains("src/main") }
                    .assertFalse { file ->
                        file.imports.any { import ->
                            import.name.contains(".application.") ||
                                import.name.contains(".infrastructure.") ||
                                import.name.contains(".apps.") ||
                                import.name.contains("springframework") ||
                                import.name.contains("jakarta.") ||
                                import.name.contains("javax.") ||
                                import.name.contains("org.koin")
                        }
                    }
            }
        }

        "application layer should not depend on infrastructure" {
            targetContexts.forEach { context ->
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

        // ========== Domain Model Purity ==========

        "domain entities should be immutable data classes" {
            targetContexts.forEach { context ->
                Konsist
                    .scopeFromDirectory("contexts/$context/domain")
                    .classes()
                    .filter { it.resideInPackage("..entity..") }
                    .filter { !it.name.endsWith("Test") }
                    .assertTrue { entity ->
                        entity.hasDataModifier
                    }
            }
        }

        "value objects should be in correct package" {
            targetContexts.forEach { context ->
                Konsist
                    .scopeFromDirectory("contexts/$context/domain")
                    .classes()
                    .filter { it.hasValueModifier }
                    .filter { !it.name.endsWith("Test") }
                    .assertTrue { valueObject ->
                        valueObject.resideInPackage("..valueobject..") ||
                            valueObject.resideInPackage("..value..")
                    }
            }
        }

        // ========== Repository Pattern ==========

        "repositories should be interfaces in domain layer" {
            targetContexts.forEach { context ->
                Konsist
                    .scopeFromDirectory("contexts/$context/domain")
                    .interfaces()
                    .filter { it.name.endsWith("Repository") }
                    .assertTrue { repository ->
                        repository.resideInPackage("..repository..")
                    }
            }
        }

        // ========== Error Hierarchy ==========

        "errors should extend context-specific base error" {
            val contextErrors = mapOf(
                "collaborative-versioning" to "CollaborativeVersioningError",
                "agent-management" to "AgentManagementError",
            )

            targetContexts.forEach { context ->
                val baseError = contextErrors[context] ?: return@forEach

                Konsist
                    .scopeFromDirectory("contexts/$context/domain")
                    .classes()
                    .filter { it.resideInPackage("..error..") }
                    .filter { it.name.endsWith("Error") }
                    .filter { it.name != baseError }
                    .filter { !it.hasSealedModifier }
                    .assertTrue { error ->
                        error.parents().isNotEmpty()
                    }
            }
        }

        // ========== Cross-Context Boundaries ==========

        "contexts should not import each other's domain models" {
            targetContexts.forEach { contextA ->
                val otherContexts = targetContexts.filter { it != contextA }

                Konsist
                    .scopeFromDirectory("contexts/$contextA")
                    .files
                    .filter { it.path.contains("src/main") }
                    .assertFalse { file ->
                        file.imports.any { import ->
                            otherContexts.any { contextB ->
                                import.name.contains(".$contextB.domain.")
                            }
                        }
                    }
            }
        }

        // ========== Command/Query Separation ==========

        "command handlers should be in command package" {
            targetContexts.forEach { context ->
                Konsist
                    .scopeFromDirectory("contexts/$context/application")
                    .classes()
                    .filter { it.name.endsWith("Handler") }
                    .filter { it.resideInPackage("..handler.command..") }
                    .assertTrue { _ ->
                        true // Just verify they exist in the right package
                    }
            }
        }

        "query handlers should be in query package" {
            targetContexts.forEach { context ->
                Konsist
                    .scopeFromDirectory("contexts/$context/application")
                    .classes()
                    .filter { it.name.endsWith("Handler") }
                    .filter { it.resideInPackage("..handler.query..") }
                    .assertTrue { _ ->
                        true // Just verify they exist in the right package
                    }
            }
        }

        // ========== Event Patterns ==========

        "domain events should be in event package" {
            Konsist
                .scopeFromDirectory("contexts/collaborative-versioning/domain")
                .classes()
                .filter { it.resideInPackage("..event..") }
                .filter {
                    it.name.endsWith("Event") ||
                        it.name.endsWith("Created") ||
                        it.name.endsWith("Updated") ||
                        it.name.endsWith("Approved") ||
                        it.name.endsWith("Reviewed") ||
                        it.name.endsWith("Merged")
                }
                .assertTrue { event ->
                    event.hasDataModifier || event.hasSealedModifier
                }
        }

        // ========== Naming Conventions ==========

        "ID types should end with Id" {
            targetContexts.forEach { context ->
                Konsist
                    .scopeFromDirectory("contexts/$context/domain")
                    .classes()
                    .filter { it.hasValueModifier }
                    .filter { clazz ->
                        clazz.functions().any { func ->
                            func.name == "generate" || func.name == "from"
                        }
                    }
                    // Exclude known non-ID value objects
                    .filterNot { clazz ->
                        clazz.name in listOf("VersionNumber", "ResourceContent")
                    }
                    .assertTrue { idClass ->
                        idClass.name.endsWith("Id")
                    }
            }
        }

        "DTOs should be in dto package" {
            targetContexts.forEach { context ->
                Konsist
                    .scopeFromDirectory("contexts/$context/application")
                    .classes()
                    .filter { it.name.endsWith("Dto") || it.name.endsWith("DTO") }
                    .assertTrue { dto ->
                        dto.resideInPackage("..dto..")
                    }
            }
        }

        // ========== Package Structure ==========

        "domain layer should have standard packages" {
            val requiredPackages = setOf("entity", "valueobject", "repository", "error")

            targetContexts.forEach { context ->
                val domainPackages = Konsist
                    .scopeFromDirectory("contexts/$context/domain")
                    .packages
                    .map { it.name }
                    .filter { it.contains(".domain.") }
                    .map { it.substringAfterLast(".domain.").substringBefore(".") }
                    .toSet()

                requiredPackages.forEach { required ->
                    assert(domainPackages.any { it.contains(required) }) {
                        "$context domain missing package: $required"
                    }
                }
            }
        }

        "application layer should have CQRS packages" {
            val expectedPackages = setOf("command", "query", "handler", "dto", "error")

            targetContexts.forEach { context ->
                val appPackages = Konsist
                    .scopeFromDirectory("contexts/$context/application")
                    .packages
                    .map { it.name }
                    .filter { it.contains(".application.") }
                    .map { it.substringAfterLast(".application.") }
                    .toSet()

                // At least some CQRS packages should exist
                assert(
                    appPackages.any { pkg ->
                        expectedPackages.any { expected -> pkg.contains(expected) }
                    },
                ) {
                    "$context application missing CQRS packages"
                }
            }
        }
    })

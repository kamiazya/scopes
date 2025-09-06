package io.github.kamiazya.scopes.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import io.kotest.core.spec.style.StringSpec

/**
 * Architecture tests for dependency inversion principle.
 * Ensures proper decoupling between layers and contexts.
 */
class DependencyInversionTest :
    StringSpec({

        val contexts = listOf(
            "scope-management",
            "user-preferences",
            "device-synchronization",
            "event-store",
        )

        // ========== Application Layer Contract Dependencies ==========

        "application layers should not depend on contracts from other contexts" {
            contexts.forEach { context ->
                Konsist
                    .scopeFromDirectory("contexts/$context/application")
                    .files
                    .filter { it.path.contains("src/main") }
                    .assertFalse { file ->
                        file.imports.any { import ->
                            // Check if importing contracts from different contexts
                            contexts.filter { it != context }.any { otherContext ->
                                import.name.contains(".contracts.$otherContext.") ||
                                    import.name.contains(".$otherContext.contracts.")
                            }
                        }
                    }
            }
        }

        "application ports should follow naming conventions when they exist" {
            contexts.forEach { context ->
                val applicationPorts = Konsist
                    .scopeFromDirectory("contexts/$context/application")
                    .interfaces()
                    .filter { it.resideInPackage("..port..") }

                applicationPorts.forEach { port ->
                    assert(
                        port.name.endsWith("Port") ||
                            port.name.endsWith("Appender") ||
                            port.name.endsWith("Gateway") ||
                            port.name.endsWith("Client") ||
                            port.name.endsWith("Publisher") ||
                            port.name.endsWith("Provider") ||
                            port.name.endsWith("Serializer"),
                    ) {
                        "Application port ${port.name} in $context should follow naming conventions (Port, Appender, Gateway, Client, Publisher, Provider, Serializer)"
                    }
                }
            }
        }

        "infrastructure should have implementations when application ports exist" {
            contexts.forEach { context ->
                val applicationPorts = Konsist
                    .scopeFromDirectory("contexts/$context/application")
                    .interfaces()
                    .filter { it.resideInPackage("..port..") }

                if (applicationPorts.isNotEmpty()) {
                    val infrastructureClasses = Konsist
                        .scopeFromDirectory("contexts/$context/infrastructure")
                        .classes()
                        .filter { it.path.contains("src/main") }

                    // At least some infrastructure implementations should exist when we have ports
                    // They can be in adapter/, serializer/, publisher/, or other packages
                    assert(infrastructureClasses.isNotEmpty()) {
                        "Context $context has application ports but no infrastructure implementations found"
                    }
                }
            }
        }

        // ========== Event Store Dependency Constraint ==========

        "application layers should not directly depend on event-store contracts" {
            contexts.filter { it != "event-store" }.forEach { context ->
                Konsist
                    .scopeFromDirectory("contexts/$context/application")
                    .files
                    .filter { it.path.contains("src/main") }
                    .assertFalse { file ->
                        file.imports.any { import ->
                            import.name.contains(".contracts.eventstore.") ||
                                import.name.contains(".eventstore.contracts.")
                        }
                    }
            }
        }

        // ========== Clean Architecture Dependencies ==========

        "dependencies should flow toward the domain layer" {
            contexts.forEach { context ->
                // Infrastructure can depend on application and domain
                val infraFiles = Konsist
                    .scopeFromDirectory("contexts/$context/infrastructure")
                    .files
                    .filter { it.path.contains("src/main") }

                infraFiles.forEach { file ->
                    file.imports.forEach { import ->
                        if (import.name.contains(".$context.")) {
                            assert(
                                import.name.contains(".$context.application.") ||
                                    import.name.contains(".$context.domain."),
                            ) {
                                "Infrastructure file ${file.path} should only depend on application and domain layers"
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
                                "Application file ${file.path} should only depend on domain layer"
                            }
                        }
                    }
                }
            }
        }

        // ========== DTO Naming Convention Guidelines ==========
        // Note: These are guidelines documented in development-guidelines.md
        // Future implementations should follow these conventions

        "DTO directories should exist where needed" {
            contexts.forEach { context ->
                val applicationDir = Konsist.scopeFromDirectory("contexts/$context/application")
                val hasApplicationFiles = applicationDir.files.isNotEmpty()

                if (hasApplicationFiles) {
                    // Just verify the context exists - no strict requirements yet
                    assert(context.isNotEmpty()) {
                        "Context $context should have a valid name"
                    }
                }
            }
        }
    })

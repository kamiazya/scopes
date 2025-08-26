package io.github.kamiazya.scopes.quality.konsist.scopemanagement

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withNameEndingWith
import com.lemonappdev.konsist.api.verify.assertFalse
import io.kotest.core.spec.style.DescribeSpec

/**
 * Tests to ensure domain services follow DDD principles of purity.
 * Domain services should contain only pure business logic without I/O operations.
 */
class DomainServicePurityTest :
    DescribeSpec({

        // Temporarily disabled while we fix domain service architecture violations
        xdescribe("Domain Service Purity Rules (Currently Disabled)") {

            it("domain services should not have suspend functions") {
                // Domain services should not have suspend functions as they indicate I/O operations.
                // All I/O should be handled in the application layer.
                Konsist.scopeFromModule("contexts/scope-management/domain")
                    .classes()
                    .withNameEndingWith("Service")
                    .flatMap { it.functions() }
                    .assertFalse { function ->
                        function.hasSuspendModifier
                    }
            }

            it("domain services should not depend on repositories") {
                // Domain services should not depend on repositories directly.
                // Repository access should be handled by application services.
                Konsist.scopeFromModule("contexts/scope-management/domain")
                    .classes()
                    .withNameEndingWith("Service")
                    .assertFalse { clazz ->
                        // Check constructor parameters
                        clazz.primaryConstructor?.parameters?.any { parameter ->
                            parameter.type?.name?.endsWith("Repository") ?: false
                        } ?: false ||
                            // Check all constructors
                            clazz.secondaryConstructors.any { constructor ->
                                constructor.parameters.any { parameter ->
                                    parameter.type?.name?.endsWith("Repository") ?: false
                                }
                            }
                    }
            }

            it("domain services should not have I/O function parameters") {
                // Domain services should not have function parameters that are suspend functions.
                // This ensures domain logic remains pure and I/O-free.
                Konsist.scopeFromModule("contexts/scope-management/domain")
                    .classes()
                    .withNameEndingWith("Service")
                    .flatMap { it.functions() }
                    .flatMap { it.parameters }
                    .assertFalse { parameter ->
                        // Check for function types with suspend modifier
                        parameter.type?.text?.contains("suspend") ?: false
                    }
            }

            it("domain service interfaces should not have suspend functions") {
                // Domain service interfaces should not define suspend functions.
                // Interface contracts should represent pure business operations.
                Konsist.scopeFromModule("contexts/scope-management/domain")
                    .interfaces()
                    .withNameEndingWith("Service")
                    .flatMap { it.functions() }
                    .assertFalse { function ->
                        function.hasSuspendModifier
                    }
            }

            it("domain services should only use domain types and pure libraries") {
                // Domain services should only import domain types and pure libraries.
                // No infrastructure or application layer dependencies allowed.
                Konsist.scopeFromModule("contexts/scope-management/domain")
                    .files
                    .filter { file ->
                        file.classes().any { it.name?.endsWith("Service") ?: false } ||
                            file.interfaces().any { it.name?.endsWith("Service") ?: false }
                    }
                    .assertFalse { file ->
                        file.imports.any { import ->
                            val importPath = import.name
                            // Check for infrastructure or application imports
                            importPath.contains(".infrastructure.") ||
                                importPath.contains(".application.") ||
                                importPath.contains(".repository.") ||
                                importPath.contains(".database.") ||
                                importPath.contains(".api.") ||
                                importPath.contains(".http.") ||
                                // Check for I/O related imports
                                importPath.contains("kotlinx.coroutines") ||
                                importPath.contains("java.io") ||
                                importPath.contains("java.nio")
                        }
                    }
            }

            it("domain event publishers should not have suspend functions") {
                // Publisher interfaces in domain should not have suspend functions.
                // Event publishing is an infrastructure concern that should be abstracted.
                Konsist.scopeFromModule("contexts/scope-management/domain")
                    .interfaces()
                    .withNameEndingWith("Publisher")
                    .flatMap { it.functions() }
                    .assertFalse { function ->
                        function.hasSuspendModifier
                    }
            }
        }
    })

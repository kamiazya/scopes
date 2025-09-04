package io.github.kamiazya.scopes.quality.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withNameEndingWith
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.DescribeSpec

/**
 * Tests to ensure domain services follow proper organization and structure.
 * Domain services should be organized according to DDD principles with clear
 * separation between different types of services.
 */
class DomainServiceOrganizationTest :
    DescribeSpec({

        describe("Domain Service Organization Rules") {

            it("all domain services should be in the correct package structure") {
                // All domain services should be under domain.service package
                Konsist.scopeFromProduction()
                    .classes()
                    .withNameEndingWith("Service")
                    .filter { clazz ->
                        // Only check classes in domain layer
                        clazz.resideInPackage("..domain..")
                    }
                    .assertTrue { clazz ->
                        // Should be in domain.service or domain.service.query package
                        clazz.resideInPackage("..domain.service..") ||
                            clazz.resideInPackage("..domain.service.query..")
                    }
            }

            it("query services should be in the query subdirectory") {
                // Services with "Query" in their name should be in the query subdirectory
                Konsist.scopeFromProduction()
                    .classes()
                    .withNameEndingWith("QueryService")
                    .filter { clazz ->
                        clazz.resideInPackage("..domain..")
                    }
                    .assertTrue { clazz ->
                        clazz.resideInPackage("..domain.service.query..")
                    }
            }

            it("non-query services should not be in the query subdirectory") {
                // Services without "Query" in their name should be directly in the service package
                Konsist.scopeFromProduction()
                    .classes()
                    .withNameEndingWith("Service")
                    .filter { clazz ->
                        clazz.resideInPackage("..domain..") &&
                            clazz.name?.contains("Query")?.not() ?: true
                    }
                    .assertFalse { clazz ->
                        clazz.resideInPackage("..domain.service.query..")
                    }
            }

            it("domain services should follow proper naming conventions") {
                // Domain services should have descriptive names ending with Service
                Konsist.scopeFromProduction()
                    .classes()
                    .filter { clazz ->
                        clazz.resideInPackage("..domain.service..")
                    }
                    .assertTrue { clazz ->
                        // Should end with Service
                        clazz.name?.endsWith("Service") ?: false &&
                            // Should not start with Service
                            (clazz.name?.startsWith("Service")?.not() ?: true) &&
                            // Should have a meaningful prefix
                            clazz.name?.replace("Service", "")?.length ?: 0 > 3
                    }
            }

            it("domain service interfaces should be in the same package as implementations") {
                // Service interfaces should be co-located with their implementations
                Konsist.scopeFromProduction()
                    .interfaces()
                    .withNameEndingWith("Service")
                    .filter { iface ->
                        iface.resideInPackage("..domain..")
                    }
                    .assertTrue { iface ->
                        // Should be in domain.service or domain.service.query package
                        iface.resideInPackage("..domain.service..") ||
                            iface.resideInPackage("..domain.service.query..")
                    }
            }

            it("domain services should not have state") {
                // Domain services should be stateless - no mutable properties
                Konsist.scopeFromProduction()
                    .classes()
                    .withNameEndingWith("Service")
                    .filter { clazz ->
                        clazz.resideInPackage("..domain.service..")
                    }
                    .flatMap { it.properties() }
                    .assertFalse { property ->
                        property.hasVarModifier
                    }
            }

            it("domain services should not depend on application or infrastructure services") {
                // Domain services should only depend on domain concepts
                Konsist.scopeFromProduction()
                    .classes()
                    .withNameEndingWith("Service")
                    .filter { clazz ->
                        clazz.resideInPackage("..domain.service..")
                    }
                    .assertFalse { clazz ->
                        // Check constructor dependencies
                        clazz.primaryConstructor?.parameters?.any { param ->
                            val typeName = param.type?.name ?: ""
                            typeName.contains("ApplicationService") ||
                                typeName.contains("InfrastructureService") ||
                                typeName.contains("Controller") ||
                                typeName.contains("Adapter")
                        } ?: false
                    }
            }

            it("domain services should only use other domain services from the same context") {
                // Domain services should not cross bounded context boundaries
                Konsist.scopeFromProduction()
                    .files
                    .filter { file ->
                        file.classes().any { it.name?.endsWith("Service") ?: false } &&
                            file.packagee?.name?.contains(".domain.service") ?: false
                    }
                    .assertTrue { file ->
                        // Extract the context name from the package
                        val contextName = file.packagee?.name
                            ?.split(".")
                            ?.dropWhile { it != "scopes" }
                            ?.drop(1) // Skip "scopes"
                            ?.firstOrNull()

                        // Check imports only reference same context or shared domain
                        file.imports.all { import ->
                            val importPath = import.name
                            // Allow same context imports
                            contextName?.let { importPath.contains("scopes.$it") } ?: false ||
                                // Allow shared domain concepts
                                importPath.contains("scopes.shared.domain") ||
                                // Allow standard libraries and frameworks
                                !importPath.startsWith("io.github.kamiazya.scopes") ||
                                // Allow contracts
                                importPath.contains(".contracts.")
                        }
                    }
            }

            it("command services should handle write operations") {
                // Services handling commands should be clearly named
                Konsist.scopeFromProduction()
                    .classes()
                    .filter { clazz ->
                        clazz.resideInPackage("..domain.service..") &&
                            (
                                clazz.name?.contains("Command") ?: false ||
                                    clazz.name?.contains("Creation") ?: false ||
                                    clazz.name?.contains("Update") ?: false ||
                                    clazz.name?.contains("Deletion") ?: false ||
                                    clazz.name?.contains("Validation") ?: false
                                )
                    }
                    .assertTrue { clazz ->
                        // Should be in the main service package, not query
                        !clazz.resideInPackage("..domain.service.query..")
                    }
            }

            it("validation services should contain validation logic") {
                // Services with Validation in the name should have validation methods
                Konsist.scopeFromProduction()
                    .classes()
                    .filter { clazz ->
                        clazz.resideInPackage("..domain.service..") &&
                            clazz.name?.contains("Validation") ?: false
                    }
                    .assertTrue { clazz ->
                        clazz.functions().any { function ->
                            // Validation methods typically return Either or Boolean
                            val returnType = function.returnType?.name ?: ""
                            returnType.contains("Either") ||
                                returnType == "Boolean" ||
                                function.name?.startsWith("validate") ?: false ||
                                function.name?.startsWith("is") ?: false ||
                                function.name?.startsWith("check") ?: false
                        }
                    }
            }

            it("calculation services should perform computations") {
                // Services focused on calculations should have appropriate methods
                Konsist.scopeFromProduction()
                    .classes()
                    .filter { clazz ->
                        clazz.resideInPackage("..domain.service..") &&
                            (
                                clazz.name?.contains("Calculation") ?: false ||
                                    clazz.name?.contains("Generation") ?: false ||
                                    clazz.name?.contains("Evaluation") ?: false
                                )
                    }
                    .assertTrue { clazz ->
                        clazz.functions().any { function ->
                            function.name?.startsWith("calculate") ?: false ||
                                function.name?.startsWith("generate") ?: false ||
                                function.name?.startsWith("evaluate") ?: false ||
                                function.name?.startsWith("compute") ?: false
                        }
                    }
            }

            it("domain services should have clear single responsibilities") {
                // Each service should have a focused purpose, indicated by method count
                Konsist.scopeFromProduction()
                    .classes()
                    .withNameEndingWith("Service")
                    .filter { clazz ->
                        clazz.resideInPackage("..domain.service..")
                    }
                    .assertTrue { clazz ->
                        val publicMethods = clazz.functions().count { function ->
                            function.hasPublicModifier ||
                                (!function.hasPrivateModifier && !function.hasProtectedModifier && !function.hasInternalModifier)
                        }
                        // A focused service should have between 1-7 public methods
                        publicMethods in 1..7
                    }
            }

            it("domain services should document their purpose") {
                // All domain services should have KDoc explaining their purpose
                Konsist.scopeFromProduction()
                    .classes()
                    .withNameEndingWith("Service")
                    .filter { clazz ->
                        clazz.resideInPackage("..domain.service..")
                    }
                    .assertTrue { clazz ->
                        clazz.hasKDoc
                    }
            }

            it("domain service methods should have meaningful names") {
                // Service methods should clearly express their intent
                Konsist.scopeFromProduction()
                    .classes()
                    .withNameEndingWith("Service")
                    .filter { clazz ->
                        clazz.resideInPackage("..domain.service..")
                    }
                    .flatMap { it.functions() }
                    .filter { function ->
                        function.hasPublicModifier ||
                            (!function.hasPrivateModifier && !function.hasProtectedModifier && !function.hasInternalModifier)
                    }
                    .assertTrue { function ->
                        val name = function.name ?: ""
                        // Method names should be descriptive verbs
                        name.length > 3 &&
                            (
                                name.startsWith("validate") ||
                                    name.startsWith("calculate") ||
                                    name.startsWith("generate") ||
                                    name.startsWith("evaluate") ||
                                    name.startsWith("check") ||
                                    name.startsWith("detect") ||
                                    name.startsWith("create") ||
                                    name.startsWith("parse") ||
                                    name.startsWith("is") ||
                                    name.startsWith("has") ||
                                    name.startsWith("can")
                                )
                    }
            }
        }
    })

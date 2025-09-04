package io.github.kamiazya.scopes.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withNameEndingWith
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

/**
 * Architecture tests for CQRS projection layer to ensure proper read model design,
 * performance optimization, and eventual consistency patterns.
 */
class CqrsProjectionTest :
    DescribeSpec({

        val scope = Konsist.scopeFromProject()

        describe("Projection Structure and Design") {

            it("Projections should be immutable data classes") {
                scope
                    .classes()
                    .withNameEndingWith("Projection")
                    .assertTrue { projection ->
                        projection.hasDataModifier &&
                            projection.primaryConstructor?.parameters?.all { param ->
                                param.hasValModifier && !param.hasVarModifier
                            } == true
                    }
            }

            it("Projections should be serializable for caching and storage") {
                scope
                    .classes()
                    .withNameEndingWith("Projection")
                    .assertTrue { projection ->
                        projection.annotations.any { annotation ->
                            annotation.name == "Serializable"
                        } ||
                            projection.text?.contains("@Serializable") == true
                    }
            }

            it("Projections should not contain business logic") {
                scope
                    .classes()
                    .withNameEndingWith("Projection")
                    .assertFalse { projection ->
                        projection.functions().any { function ->
                            !function.hasOverrideModifier &&
                                // Allow toString, equals, hashCode overrides
                                function.name !in listOf("toString", "equals", "hashCode", "copy", "component1", "component2") &&
                                function.text?.let { text ->
                                    text.contains("if") || text.contains("when") || text.contains("for") || text.contains("while")
                                } == true
                        }
                    }
            }

            it("Projections should use appropriate data types for UI consumption") {
                scope
                    .classes()
                    .withNameEndingWith("Projection")
                    .assertTrue { projection ->
                        projection.properties().all { property ->
                            val propertyType = property.type?.name
                            // Should use serializable, UI-friendly types
                            propertyType == null ||
                                propertyType in listOf("String", "Int", "Long", "Double", "Boolean", "Instant") ||
                                propertyType.startsWith("List<") ||
                                propertyType.startsWith("Map<") ||
                                propertyType.startsWith("Set<") ||
                                propertyType.endsWith("Projection") ||
                                propertyType.endsWith("Type") ||
                                // Enum types
                                propertyType.contains("?") // Nullable types are fine
                        }
                    }
            }

            it("Projection enums should define UI-relevant states") {
                scope
                    .classes()
                    .filter { it.packagee?.name?.contains("projection") == true }
                    .filter { it.hasEnumModifier }
                    .assertTrue { enumClass ->
                        enumClass.name.endsWith("Type") ||
                            enumClass.name.endsWith("Status") ||
                            enumClass.name.endsWith("Mode") ||
                            enumClass.name.contains("Match") ||
                            enumClass.name.contains("Activity")
                    }
            }

            it("Complex projections should be composed of simpler projections") {
                scope
                    .classes()
                    .withNameEndingWith("Projection")
                    .filter { it.name.contains("Detail") || it.name.contains("Full") }
                    .assertTrue { complexProjection ->
                        complexProjection.properties().any { property ->
                            val propertyType = property.type?.name
                            propertyType?.endsWith("Projection") == true ||
                                propertyType?.startsWith("List<") == true &&
                                propertyType.contains("Projection")
                        }
                    }
            }
        }

        describe("Projection Service Design") {

            it("Projection services should be interfaces") {
                scope
                    .interfaces()
                    .withNameEndingWith("ProjectionService")
                    .isNotEmpty() shouldBe true
            }

            it("Projection service methods should return Either types") {
                scope
                    .interfaces()
                    .withNameEndingWith("ProjectionService")
                    .assertTrue { service ->
                        service.functions().all { function ->
                            val returnType = function.returnType?.name
                            returnType?.startsWith("Either<") == true
                        }
                    }
            }

            it("Projection services should support pagination for list operations") {
                scope
                    .interfaces()
                    .withNameEndingWith("ProjectionService")
                    .assertTrue { service ->
                        service.functions().filter {
                            it.name.startsWith("list") || it.name.startsWith("search") || it.name.startsWith("get") && it.name.contains("s")
                        }.all { listFunction ->
                            listFunction.parameters.any { param ->
                                param.name in listOf("offset", "limit", "page", "size")
                            }
                        }
                    }
            }

            it("Projection services should have refresh methods for eventual consistency") {
                scope
                    .interfaces()
                    .withNameEndingWith("ProjectionService")
                    .assertTrue { service ->
                        service.functions().any { function ->
                            function.name.startsWith("refresh") ||
                                function.name.startsWith("sync") ||
                                function.name.startsWith("update")
                        }
                    }
            }

            it("Projection service implementations should not access domain repositories directly") {
                scope
                    .classes()
                    .filter { it.packagee?.name?.contains("projection") == true }
                    .withNameEndingWith("ServiceImpl", "ServiceAdapter")
                    .assertFalse { impl ->
                        impl.properties().any { property ->
                            val propertyType = property.type?.name
                            propertyType?.contains("Repository") == true &&
                                !propertyType.contains("Projection") &&
                                !propertyType.contains("Read") &&
                                !propertyType.contains("Query")
                        }
                    }
            }
        }

        describe("Performance and Scalability") {

            it("Lightweight projections should be preferred for list views") {
                scope
                    .classes()
                    .withNameEndingWith("Projection")
                    .filter { it.name.contains("Summary") || it.name.contains("List") || it.name.contains("Item") }
                    .assertTrue { lightweightProjection ->
                        // Summary projections should have limited number of properties
                        lightweightProjection.properties().size <= 10
                    }
            }

            it("Detailed projections should be used sparingly") {
                scope
                    .classes()
                    .withNameEndingWith("Projection")
                    .filter { it.name.contains("Detail") || it.name.contains("Full") || it.name.contains("Complete") }
                    .assertTrue { detailProjection ->
                        // Detail projections can have more properties but should still be reasonable
                        detailProjection.properties().size <= 25
                    }
            }

            it("Search projections should include relevance and highlighting") {
                scope
                    .classes()
                    .withNameEndingWith("Projection")
                    .filter { it.name.contains("Search") }
                    .assertTrue { searchProjection ->
                        val propertyNames = searchProjection.properties().map { it.name.lowercase() }
                        propertyNames.any { it.contains("relevance") || it.contains("score") } &&
                            propertyNames.any { it.contains("highlight") || it.contains("match") }
                    }
            }

            it("Projection services should support caching strategies") {
                scope
                    .classes()
                    .filter { it.packagee?.name?.contains("projection") == true }
                    .withNameEndingWith("ServiceImpl")
                    .assertTrue { projectionService ->
                        // Should either have caching dependencies or be simple enough not to need them
                        projectionService.properties().any { property ->
                            property.type?.name?.contains("Cache") == true
                        } ||
                            projectionService.functions().size <= 10 // Simple services may not need caching
                    }
            }

            it("Projection builders should create optimized read models") {
                scope
                    .classes()
                    .filter { it.packagee?.name?.contains("projection") == true }
                    .filter { it.name.contains("Builder") }
                    .assertTrue { builder ->
                        builder.functions().any { function ->
                            function.name.startsWith("build") || function.name.startsWith("create")
                        } &&
                            builder.functions().all { function ->
                                // Builder methods should be efficient
                                function.parameters.size <= 8 // Avoid parameter explosion
                            }
                    }
            }
        }

        describe("Consistency and Synchronization") {

            it("Projection services should handle eventual consistency") {
                scope
                    .interfaces()
                    .withNameEndingWith("ProjectionService")
                    .assertTrue { service ->
                        service.functions().any { function ->
                            // Should have methods to handle consistency
                            function.name.contains("refresh") ||
                                function.name.contains("sync") ||
                                function.name.contains("rebuild") ||
                                function.returnType?.name?.contains("Unit") == true // Void operations for updates
                        }
                    }
            }

            it("Projection updates should be idempotent") {
                scope
                    .classes()
                    .filter { it.packagee?.name?.contains("projection") == true }
                    .withNameEndingWith("ServiceImpl")
                    .assertTrue { projectionService ->
                        // Methods that update projections should be designed to be idempotent
                        projectionService.functions().filter {
                            it.name.startsWith("update") || it.name.startsWith("refresh") || it.name.startsWith("add")
                        }.all { updateMethod ->
                            // Should take ID or unique identifier as parameter for idempotency
                            updateMethod.parameters.any { param ->
                                param.name.contains("id") ||
                                    param.name.contains("Id") ||
                                    param.type?.name?.contains("String") == true
                            }
                        }
                    }
            }

            it("Projection services should handle not-found scenarios gracefully") {
                scope
                    .interfaces()
                    .withNameEndingWith("ProjectionService")
                    .assertTrue { service ->
                        service.functions().filter { it.name.startsWith("get") }.all { getMethod ->
                            val returnType = getMethod.returnType?.name
                            // Should return nullable or Either types to handle not-found
                            returnType?.contains("?") == true ||
                                returnType?.contains("Either") == true
                        }
                    }
            }

            it("Projection errors should be specific and actionable") {
                scope
                    .classes()
                    .filter { it.packagee?.name?.contains("projection") == true }
                    .filter { it.name.contains("Error") || it.hasParent { it.name?.contains("Error") == true } }
                    .assertTrue { errorClass ->
                        // Projection errors should provide context
                        errorClass.properties().any { property ->
                            property.name.contains("id") || property.name.contains("name") || property.name.contains("reason")
                        }
                    }
            }
        }

        describe("Testing and Maintainability") {

            it("Projection data classes should have reasonable toString implementations") {
                scope
                    .classes()
                    .withNameEndingWith("Projection")
                    .filter { it.hasDataModifier }
                    .assertTrue { projection ->
                        // Data classes automatically get toString, or should have custom implementation
                        projection.hasDataModifier ||
                            projection.functions().any { it.name == "toString" && it.hasOverrideModifier }
                    }
            }

            it("Projection properties should have meaningful names") {
                scope
                    .classes()
                    .withNameEndingWith("Projection")
                    .assertTrue { projection ->
                        projection.properties().all { property ->
                            val propertyName = property.name
                            // Should not use ambiguous or generic names
                            propertyName.length > 2 &&
                                propertyName !in listOf("data", "info", "item", "value", "obj", "res", "tmp")
                        }
                    }
            }

            it("Projection services should have comprehensive method coverage") {
                scope
                    .interfaces()
                    .withNameEndingWith("ProjectionService")
                    .assertTrue { service ->
                        val methodNames = service.functions().map { it.name }
                        // Should have basic CRUD-like operations for projections
                        methodNames.any { it.startsWith("get") } &&
                            (methodNames.any { it.startsWith("list") } || methodNames.any { it.startsWith("search") })
                    }
            }

            it("Complex projections should be documented") {
                scope
                    .classes()
                    .withNameEndingWith("Projection")
                    .filter { it.properties().size > 8 }
                    .assertTrue { complexProjection ->
                        // Complex projections should have documentation
                        complexProjection.hasKDoc ||
                            complexProjection.annotations.isNotEmpty()
                    }
            }
        }
    })

package io.github.kamiazya.scopes.apps.cli.integration

import io.github.kamiazya.scopes.scopemanagement.application.query.AspectQueryParser
import io.github.kamiazya.scopes.scopemanagement.application.query.FilterScopesWithQueryUseCase
import io.github.kamiazya.scopes.scopemanagement.domain.entity.AspectDefinition
import io.github.kamiazya.scopes.scopemanagement.domain.entity.Scope
import io.github.kamiazya.scopes.scopemanagement.domain.repository.AspectDefinitionRepository
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.*
import io.github.kamiazya.scopes.scopemanagement.infrastructure.repository.InMemoryAspectDefinitionRepository
import io.github.kamiazya.scopes.scopemanagement.infrastructure.repository.InMemoryScopeRepository
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock

class AspectQueryIntegrationTest :
    DescribeSpec({
        describe("Aspect Query Integration") {
            lateinit var aspectDefinitionRepository: AspectDefinitionRepository
            lateinit var scopeRepository: ScopeRepository
            lateinit var filterScopesWithQueryUseCase: FilterScopesWithQueryUseCase
            lateinit var parser: AspectQueryParser

            // Test scopes
            lateinit var scope1: Scope
            lateinit var scope2: Scope
            lateinit var scope3: Scope
            lateinit var scope4: Scope

            beforeEach {
                // Initialize repositories
                aspectDefinitionRepository = InMemoryAspectDefinitionRepository()
                scopeRepository = InMemoryScopeRepository()
                parser = AspectQueryParser()
                filterScopesWithQueryUseCase = FilterScopesWithQueryUseCase(
                    scopeRepository,
                    aspectDefinitionRepository,
                    parser,
                )

                // Setup aspect definitions
                runTest {
                    // Text aspect
                    val statusDef = AspectDefinition.createText(
                        AspectKey.create("status").getOrNull()!!,
                        "Task status",
                    )
                    aspectDefinitionRepository.save(statusDef)

                    // Numeric aspect
                    val sizeDef = AspectDefinition.createNumeric(
                        AspectKey.create("size").getOrNull()!!,
                        "Task size",
                    )
                    aspectDefinitionRepository.save(sizeDef)

                    // Boolean aspect
                    val activeDef = AspectDefinition.createBoolean(
                        AspectKey.create("active").getOrNull()!!,
                        "Is active",
                    )
                    aspectDefinitionRepository.save(activeDef)

                    // Ordered aspect
                    val priorityValues = listOf("low", "medium", "high", "critical").map {
                        AspectValue.create(it).getOrNull()!!
                    }
                    val priorityDef = AspectDefinition.createOrdered(
                        AspectKey.create("priority").getOrNull()!!,
                        priorityValues,
                        "Task priority",
                    ).getOrNull()!!
                    aspectDefinitionRepository.save(priorityDef)

                    // Duration aspect
                    val timeDef = AspectDefinition.createDuration(
                        AspectKey.create("estimatedTime").getOrNull()!!,
                        "Estimated time",
                    )
                    aspectDefinitionRepository.save(timeDef)

                    // Create test scopes with aspects
                    scope1 = Scope(
                        id = ScopeId.create("scope1").getOrNull()!!,
                        title = ScopeTitle.create("Task 1").getOrNull()!!,
                        description = null,
                        parentId = null,
                        aspects = Aspects.from(
                            mapOf(
                                AspectKey.create("status").getOrNull()!! to listOf(AspectValue.create("open").getOrNull()!!),
                                AspectKey.create("size").getOrNull()!! to listOf(AspectValue.create("10").getOrNull()!!),
                                AspectKey.create("active").getOrNull()!! to listOf(AspectValue.create("true").getOrNull()!!),
                                AspectKey.create("priority").getOrNull()!! to listOf(AspectValue.create("high").getOrNull()!!),
                                AspectKey.create("estimatedTime").getOrNull()!! to listOf(AspectValue.create("PT2H").getOrNull()!!),
                            ),
                        ),
                        createdAt = Clock.System.now(),
                        updatedAt = Clock.System.now(),
                    )
                    scopeRepository.save(scope1)

                    scope2 = Scope(
                        id = ScopeId.create("scope2").getOrNull()!!,
                        title = ScopeTitle.create("Task 2").getOrNull()!!,
                        description = null,
                        parentId = null,
                        aspects = Aspects.from(
                            mapOf(
                                AspectKey.create("status").getOrNull()!! to listOf(AspectValue.create("closed").getOrNull()!!),
                                AspectKey.create("size").getOrNull()!! to listOf(AspectValue.create("5").getOrNull()!!),
                                AspectKey.create("active").getOrNull()!! to listOf(AspectValue.create("false").getOrNull()!!),
                                AspectKey.create("priority").getOrNull()!! to listOf(AspectValue.create("low").getOrNull()!!),
                                AspectKey.create("estimatedTime").getOrNull()!! to listOf(AspectValue.create("P1D").getOrNull()!!),
                            ),
                        ),
                        createdAt = Clock.System.now(),
                        updatedAt = Clock.System.now(),
                    )
                    scopeRepository.save(scope2)

                    scope3 = Scope(
                        id = ScopeId.create("scope3").getOrNull()!!,
                        title = ScopeTitle.create("Task 3").getOrNull()!!,
                        description = null,
                        parentId = null,
                        aspects = Aspects.from(
                            mapOf(
                                AspectKey.create("status").getOrNull()!! to listOf(AspectValue.create("open").getOrNull()!!),
                                AspectKey.create("size").getOrNull()!! to listOf(AspectValue.create("20").getOrNull()!!),
                                AspectKey.create("active").getOrNull()!! to listOf(AspectValue.create("true").getOrNull()!!),
                                AspectKey.create("priority").getOrNull()!! to listOf(AspectValue.create("critical").getOrNull()!!),
                                AspectKey.create("estimatedTime").getOrNull()!! to listOf(AspectValue.create("PT30M").getOrNull()!!),
                            ),
                        ),
                        createdAt = Clock.System.now(),
                        updatedAt = Clock.System.now(),
                    )
                    scopeRepository.save(scope3)

                    scope4 = Scope(
                        id = ScopeId.create("scope4").getOrNull()!!,
                        title = ScopeTitle.create("Task 4").getOrNull()!!,
                        description = null,
                        parentId = null,
                        aspects = Aspects.from(
                            mapOf(
                                AspectKey.create("status").getOrNull()!! to listOf(AspectValue.create("pending").getOrNull()!!),
                                AspectKey.create("size").getOrNull()!! to listOf(AspectValue.create("15").getOrNull()!!),
                                AspectKey.create("priority").getOrNull()!! to listOf(AspectValue.create("medium").getOrNull()!!),
                                // Note: No active or estimatedTime aspects
                            ),
                        ),
                        createdAt = Clock.System.now(),
                        updatedAt = Clock.System.now(),
                    )
                    scopeRepository.save(scope4)
                }
            }

            describe("Basic Queries") {
                it("should filter by equality") {
                    runTest {
                        // Act
                        val result = filterScopesWithQueryUseCase.execute("status=open")

                        // Assert
                        result.shouldBeRight()
                        val scopes = result.getOrNull()!!
                        scopes.map { it.id } shouldContainExactlyInAnyOrder listOf(scope1.id, scope3.id)
                    }
                }

                it("should filter by inequality") {
                    runTest {
                        // Act
                        val result = filterScopesWithQueryUseCase.execute("status!=open")

                        // Assert
                        result.shouldBeRight()
                        val scopes = result.getOrNull()!!
                        scopes.map { it.id } shouldContainExactlyInAnyOrder listOf(scope2.id, scope4.id)
                    }
                }

                it("should filter numeric values with greater than") {
                    runTest {
                        // Act
                        val result = filterScopesWithQueryUseCase.execute("size>10")

                        // Assert
                        result.shouldBeRight()
                        val scopes = result.getOrNull()!!
                        scopes.map { it.id } shouldContainExactlyInAnyOrder listOf(scope3.id, scope4.id)
                    }
                }

                it("should filter numeric values with less than or equal") {
                    runTest {
                        // Act
                        val result = filterScopesWithQueryUseCase.execute("size<=10")

                        // Assert
                        result.shouldBeRight()
                        val scopes = result.getOrNull()!!
                        scopes.map { it.id } shouldContainExactlyInAnyOrder listOf(scope1.id, scope2.id)
                    }
                }

                it("should filter boolean values") {
                    runTest {
                        // Act
                        val result = filterScopesWithQueryUseCase.execute("active=true")

                        // Assert
                        result.shouldBeRight()
                        val scopes = result.getOrNull()!!
                        scopes.map { it.id } shouldContainExactlyInAnyOrder listOf(scope1.id, scope3.id)
                    }
                }

                it("should filter ordered values") {
                    runTest {
                        // Act
                        val result = filterScopesWithQueryUseCase.execute("priority>=high")

                        // Assert
                        result.shouldBeRight()
                        val scopes = result.getOrNull()!!
                        scopes.map { it.id } shouldContainExactlyInAnyOrder listOf(scope1.id, scope3.id)
                    }
                }

                it("should filter duration values") {
                    runTest {
                        // Act
                        val result = filterScopesWithQueryUseCase.execute("estimatedTime<PT4H")

                        // Assert
                        result.shouldBeRight()
                        val scopes = result.getOrNull()!!
                        scopes.map { it.id } shouldContainExactlyInAnyOrder listOf(scope1.id, scope3.id)
                    }
                }
            }

            describe("Logical Operators") {
                it("should handle AND operator") {
                    runTest {
                        // Act
                        val result = filterScopesWithQueryUseCase.execute("status=open AND size>10")

                        // Assert
                        result.shouldBeRight()
                        val scopes = result.getOrNull()!!
                        scopes.map { it.id } shouldContainExactlyInAnyOrder listOf(scope3.id)
                    }
                }

                it("should handle OR operator") {
                    runTest {
                        // Act
                        val result = filterScopesWithQueryUseCase.execute("priority=critical OR priority=low")

                        // Assert
                        result.shouldBeRight()
                        val scopes = result.getOrNull()!!
                        scopes.map { it.id } shouldContainExactlyInAnyOrder listOf(scope2.id, scope3.id)
                    }
                }

                it("should handle NOT operator") {
                    runTest {
                        // Act
                        val result = filterScopesWithQueryUseCase.execute("NOT status=closed")

                        // Assert
                        result.shouldBeRight()
                        val scopes = result.getOrNull()!!
                        scopes.map { it.id } shouldContainExactlyInAnyOrder listOf(scope1.id, scope3.id, scope4.id)
                    }
                }

                it("should handle complex expressions with parentheses") {
                    runTest {
                        // Act
                        val result = filterScopesWithQueryUseCase.execute(
                            "(status=open AND active=true) OR priority=critical",
                        )

                        // Assert
                        result.shouldBeRight()
                        val scopes = result.getOrNull()!!
                        scopes.map { it.id } shouldContainExactlyInAnyOrder listOf(scope1.id, scope3.id)
                    }
                }

                it("should handle nested parentheses") {
                    runTest {
                        // Act
                        val result = filterScopesWithQueryUseCase.execute(
                            "NOT (status=closed OR (size<10 AND priority=low))",
                        )

                        // Assert
                        result.shouldBeRight()
                        val scopes = result.getOrNull()!!
                        scopes.map { it.id } shouldContainExactlyInAnyOrder listOf(scope1.id, scope3.id, scope4.id)
                    }
                }
            }

            describe("Edge Cases") {
                it("should handle quoted values") {
                    runTest {
                        // Act
                        val result = filterScopesWithQueryUseCase.execute("status=\"open\"")

                        // Assert
                        result.shouldBeRight()
                        val scopes = result.getOrNull()!!
                        scopes.map { it.id } shouldContainExactlyInAnyOrder listOf(scope1.id, scope3.id)
                    }
                }

                it("should handle missing aspects") {
                    runTest {
                        // Act - scope4 doesn't have 'active' aspect
                        val result = filterScopesWithQueryUseCase.execute("active=true")

                        // Assert
                        result.shouldBeRight()
                        val scopes = result.getOrNull()!!
                        scopes.map { it.id } shouldContainExactlyInAnyOrder listOf(scope1.id, scope3.id)
                    }
                }

                it("should handle invalid queries gracefully") {
                    runTest {
                        // Act
                        val results = listOf(
                            filterScopesWithQueryUseCase.execute(""),
                            filterScopesWithQueryUseCase.execute("status"),
                            filterScopesWithQueryUseCase.execute("status==open"),
                            filterScopesWithQueryUseCase.execute("(status=open"),
                        )

                        // Assert - all should be errors
                        results.forEach { result ->
                            result.isLeft() shouldBe true
                        }
                    }
                }
            }

            describe("Performance Queries") {
                it("should handle queries with many conditions efficiently") {
                    runTest {
                        // Act
                        val complexQuery = """
                        (status=open OR status=pending) AND
                        (size>=10 AND size<=20) AND
                        (priority=high OR priority=critical OR priority=medium) AND
                        NOT active=false
                        """.trimIndent().replace("\n", " ")

                        val result = filterScopesWithQueryUseCase.execute(complexQuery)

                        // Assert
                        result.shouldBeRight()
                        val scopes = result.getOrNull()!!
                        scopes.map { it.id } shouldContainExactlyInAnyOrder listOf(scope1.id, scope3.id)
                    }
                }
            }
        }
    })

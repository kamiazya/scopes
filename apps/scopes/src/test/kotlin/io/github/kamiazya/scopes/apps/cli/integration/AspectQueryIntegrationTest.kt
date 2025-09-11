package io.github.kamiazya.scopes.apps.cli.integration

import arrow.core.Either
import arrow.core.toNonEmptyListOrNull
import io.github.kamiazya.scopes.platform.infrastructure.transaction.NoOpTransactionManager
import io.github.kamiazya.scopes.platform.observability.logging.LogLevel
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.dto.scope.ScopeDto
import io.github.kamiazya.scopes.scopemanagement.application.handler.query.scope.FilterScopesWithQueryHandler
import io.github.kamiazya.scopes.scopemanagement.application.query.scope.FilterScopesWithQuery
import io.github.kamiazya.scopes.scopemanagement.domain.entity.AspectDefinition
import io.github.kamiazya.scopes.scopemanagement.domain.entity.Scope
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.domain.repository.AspectDefinitionRepository
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.scopemanagement.domain.service.query.AspectQueryParser
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectKey
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectValue
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.Aspects
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeTitle
import io.github.kamiazya.scopes.scopemanagement.infrastructure.repository.InMemoryAspectDefinitionRepository
import io.github.kamiazya.scopes.scopemanagement.infrastructure.repository.InMemoryScopeRepository
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock

class AspectQueryIntegrationTest :
    DescribeSpec({
        describe("Aspect Query Integration") {
            lateinit var aspectDefinitionRepository: AspectDefinitionRepository
            lateinit var scopeRepository: ScopeRepository
            lateinit var filterScopesWithQueryHandler: FilterScopesWithQueryHandler
            lateinit var parser: AspectQueryParser
            lateinit var logger: Logger

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
                logger = object : Logger {
                    override fun debug(message: String, context: Map<String, Any>) {}
                    override fun info(message: String, context: Map<String, Any>) {}
                    override fun warn(message: String, context: Map<String, Any>) {}
                    override fun error(message: String, context: Map<String, Any>, throwable: Throwable?) {}
                    override fun isEnabledFor(level: LogLevel): Boolean = true
                    override fun withContext(context: Map<String, Any>): Logger = this
                    override fun withName(name: String): Logger = this
                }
                filterScopesWithQueryHandler = FilterScopesWithQueryHandler(
                    scopeRepository,
                    aspectDefinitionRepository,
                    NoOpTransactionManager(),
                    logger,
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
                        id = ScopeId.generate(),
                        title = ScopeTitle.create("Task 1").getOrNull()!!,
                        description = null,
                        parentId = null,
                        aspects = Aspects.from(
                            mapOf(
                                AspectKey.create("status").getOrNull()!! to listOf(AspectValue.create("open").getOrNull()!!).toNonEmptyListOrNull()!!,
                                AspectKey.create("size").getOrNull()!! to listOf(AspectValue.create("10").getOrNull()!!).toNonEmptyListOrNull()!!,
                                AspectKey.create("active").getOrNull()!! to listOf(AspectValue.create("true").getOrNull()!!).toNonEmptyListOrNull()!!,
                                AspectKey.create("priority").getOrNull()!! to listOf(AspectValue.create("high").getOrNull()!!).toNonEmptyListOrNull()!!,
                                AspectKey.create("estimatedTime").getOrNull()!! to listOf(AspectValue.create("PT2H").getOrNull()!!).toNonEmptyListOrNull()!!,
                            ),
                        ),
                        createdAt = Clock.System.now(),
                        updatedAt = Clock.System.now(),
                    )
                    scopeRepository.save(scope1)

                    scope2 = Scope(
                        id = ScopeId.generate(),
                        title = ScopeTitle.create("Task 2").getOrNull()!!,
                        description = null,
                        parentId = null,
                        aspects = Aspects.from(
                            mapOf(
                                AspectKey.create("status").getOrNull()!! to listOf(AspectValue.create("closed").getOrNull()!!).toNonEmptyListOrNull()!!,
                                AspectKey.create("size").getOrNull()!! to listOf(AspectValue.create("5").getOrNull()!!).toNonEmptyListOrNull()!!,
                                AspectKey.create("active").getOrNull()!! to listOf(AspectValue.create("false").getOrNull()!!).toNonEmptyListOrNull()!!,
                                AspectKey.create("priority").getOrNull()!! to listOf(AspectValue.create("low").getOrNull()!!).toNonEmptyListOrNull()!!,
                                AspectKey.create("estimatedTime").getOrNull()!! to listOf(AspectValue.create("P1D").getOrNull()!!).toNonEmptyListOrNull()!!,
                            ),
                        ),
                        createdAt = Clock.System.now(),
                        updatedAt = Clock.System.now(),
                    )
                    scopeRepository.save(scope2)

                    scope3 = Scope(
                        id = ScopeId.generate(),
                        title = ScopeTitle.create("Task 3").getOrNull()!!,
                        description = null,
                        parentId = null,
                        aspects = Aspects.from(
                            mapOf(
                                AspectKey.create("status").getOrNull()!! to listOf(AspectValue.create("open").getOrNull()!!).toNonEmptyListOrNull()!!,
                                AspectKey.create("size").getOrNull()!! to listOf(AspectValue.create("20").getOrNull()!!).toNonEmptyListOrNull()!!,
                                AspectKey.create("active").getOrNull()!! to listOf(AspectValue.create("true").getOrNull()!!).toNonEmptyListOrNull()!!,
                                AspectKey.create("priority").getOrNull()!! to listOf(AspectValue.create("critical").getOrNull()!!).toNonEmptyListOrNull()!!,
                                AspectKey.create("estimatedTime").getOrNull()!! to listOf(AspectValue.create("PT30M").getOrNull()!!).toNonEmptyListOrNull()!!,
                            ),
                        ),
                        createdAt = Clock.System.now(),
                        updatedAt = Clock.System.now(),
                    )
                    scopeRepository.save(scope3)

                    scope4 = Scope(
                        id = ScopeId.generate(),
                        title = ScopeTitle.create("Task 4").getOrNull()!!,
                        description = null,
                        parentId = null,
                        aspects = Aspects.from(
                            mapOf(
                                AspectKey.create("status").getOrNull()!! to listOf(AspectValue.create("pending").getOrNull()!!).toNonEmptyListOrNull()!!,
                                AspectKey.create("size").getOrNull()!! to listOf(AspectValue.create("15").getOrNull()!!).toNonEmptyListOrNull()!!,
                                AspectKey.create("priority").getOrNull()!! to listOf(AspectValue.create("medium").getOrNull()!!).toNonEmptyListOrNull()!!,
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
                        val query = FilterScopesWithQuery(query = "status=open", offset = 0, limit = 1000)
                        val result = filterScopesWithQueryHandler.invoke(query)

                        // Assert
                        result.shouldBeRight()
                        val scopes = result.getOrNull()!!
                        scopes.map { scope -> scope.id }.shouldContainExactlyInAnyOrder(scope1.id.value, scope3.id.value)
                    }
                }

                it("should filter by inequality") {
                    runTest {
                        // Act
                        val query = FilterScopesWithQuery(query = "status!=open", offset = 0, limit = 1000)
                        val result = filterScopesWithQueryHandler.invoke(query)

                        // Assert
                        result.shouldBeRight()
                        val scopes = result.getOrNull()!!
                        scopes.map { scope -> scope.id }.shouldContainExactlyInAnyOrder(scope2.id.value, scope4.id.value)
                    }
                }

                it("should filter numeric values with greater than") {
                    runTest {
                        // Act
                        val query = FilterScopesWithQuery(query = "size>10", offset = 0, limit = 1000)
                        val result = filterScopesWithQueryHandler.invoke(query)

                        // Assert
                        result.shouldBeRight()
                        val scopes = result.getOrNull()!!
                        scopes.map { scope -> scope.id }.shouldContainExactlyInAnyOrder(scope3.id.value, scope4.id.value)
                    }
                }

                it("should filter numeric values with less than or equal") {
                    runTest {
                        // Act
                        val query = FilterScopesWithQuery(query = "size<=10", offset = 0, limit = 1000)
                        val result = filterScopesWithQueryHandler.invoke(query)

                        // Assert
                        result.shouldBeRight()
                        val scopes = result.getOrNull()!!
                        scopes.map { scope -> scope.id }.shouldContainExactlyInAnyOrder(scope1.id.value, scope2.id.value)
                    }
                }

                it("should filter boolean values") {
                    runTest {
                        // Act
                        val query = FilterScopesWithQuery(query = "active=true", offset = 0, limit = 1000)
                        val result = filterScopesWithQueryHandler.invoke(query)

                        // Assert
                        result.shouldBeRight()
                        val scopes = result.getOrNull()!!
                        scopes.map { scope -> scope.id }.shouldContainExactlyInAnyOrder(scope1.id.value, scope3.id.value)
                    }
                }

                it("should filter ordered values") {
                    runTest {
                        // Act
                        val query = FilterScopesWithQuery(query = "priority>=high", offset = 0, limit = 1000)
                        val result = filterScopesWithQueryHandler.invoke(query)

                        // Assert
                        result.shouldBeRight()
                        val scopes = result.getOrNull()!!
                        scopes.map { scope -> scope.id }.shouldContainExactlyInAnyOrder(scope1.id.value, scope3.id.value)
                    }
                }

                it("should filter duration values") {
                    runTest {
                        // Act
                        val query = FilterScopesWithQuery(query = "estimatedTime<PT4H", offset = 0, limit = 1000)
                        val result = filterScopesWithQueryHandler.invoke(query)

                        // Assert
                        result.shouldBeRight()
                        val scopes = result.getOrNull()!!
                        scopes.map { scope -> scope.id }.shouldContainExactlyInAnyOrder(scope1.id.value, scope3.id.value)
                    }
                }
            }

            describe("Logical Operators") {
                it("should handle AND operator") {
                    runTest {
                        // Act
                        val query = FilterScopesWithQuery(query = "status=open AND size>10", offset = 0, limit = 1000)
                        val result = filterScopesWithQueryHandler.invoke(query)

                        // Assert
                        result.shouldBeRight()
                        val scopes = result.getOrNull()!!
                        scopes.map { scope -> scope.id }.shouldContainExactlyInAnyOrder(scope3.id.value)
                    }
                }

                it("should handle OR operator") {
                    runTest {
                        // Act
                        val query = FilterScopesWithQuery(query = "priority=critical OR priority=low", offset = 0, limit = 1000)
                        val result = filterScopesWithQueryHandler.invoke(query)

                        // Assert
                        result.shouldBeRight()
                        val scopes = result.getOrNull()!!
                        scopes.map { scope -> scope.id }.shouldContainExactlyInAnyOrder(scope2.id.value, scope3.id.value)
                    }
                }

                it("should handle NOT operator") {
                    runTest {
                        // Act
                        val query = FilterScopesWithQuery(query = "NOT status=closed", offset = 0, limit = 1000)
                        val result = filterScopesWithQueryHandler.invoke(query)

                        // Assert
                        result.shouldBeRight()
                        val scopes = result.getOrNull()!!
                        scopes.map { scope -> scope.id }.shouldContainExactlyInAnyOrder(scope1.id.value, scope3.id.value, scope4.id.value)
                    }
                }

                it("should handle complex expressions with parentheses") {
                    runTest {
                        // Act
                        val query = FilterScopesWithQuery(
                            query = "(status=open AND active=true) OR priority=critical",
                            offset = 0,
                            limit = 1000,
                        )
                        val result = filterScopesWithQueryHandler.invoke(query)

                        // Assert
                        result.shouldBeRight()
                        val scopes = result.getOrNull()!!
                        scopes.map { scope -> scope.id }.shouldContainExactlyInAnyOrder(scope1.id.value, scope3.id.value)
                    }
                }

                it("should handle nested parentheses") {
                    runTest {
                        // Act
                        val query = FilterScopesWithQuery(
                            query = "NOT (status=closed OR (size<10 AND priority=low))",
                            offset = 0,
                            limit = 1000,
                        )
                        val result = filterScopesWithQueryHandler.invoke(query)

                        // Assert
                        result.shouldBeRight()
                        val scopes = result.getOrNull()!!
                        scopes.map { scope -> scope.id }.shouldContainExactlyInAnyOrder(scope1.id.value, scope3.id.value, scope4.id.value)
                    }
                }
            }

            describe("Edge Cases") {
                it("should handle quoted values") {
                    runTest {
                        // Act
                        val query = FilterScopesWithQuery(query = "status=\"open\"", offset = 0, limit = 1000)
                        val result = filterScopesWithQueryHandler.invoke(query)

                        // Assert
                        result.shouldBeRight()
                        val scopes = result.getOrNull()!!
                        scopes.map { scope -> scope.id }.shouldContainExactlyInAnyOrder(scope1.id.value, scope3.id.value)
                    }
                }

                it("should handle missing aspects") {
                    runTest {
                        // Act - scope4 doesn't have 'active' aspect
                        val query = FilterScopesWithQuery(query = "active=true", offset = 0, limit = 1000)
                        val result = filterScopesWithQueryHandler.invoke(query)

                        // Assert
                        result.shouldBeRight()
                        val scopes = result.getOrNull()!!
                        scopes.map { scope -> scope.id }.shouldContainExactlyInAnyOrder(scope1.id.value, scope3.id.value)
                    }
                }

                it("should handle invalid queries gracefully") {
                    runTest {
                        // Act
                        val results = listOf<Either<ScopesError, List<ScopeDto>>>(
                            filterScopesWithQueryHandler.invoke(FilterScopesWithQuery(query = "", offset = 0, limit = 1000)),
                            filterScopesWithQueryHandler.invoke(FilterScopesWithQuery(query = "status", offset = 0, limit = 1000)),
                            filterScopesWithQueryHandler.invoke(FilterScopesWithQuery(query = "status==open", offset = 0, limit = 1000)),
                            filterScopesWithQueryHandler.invoke(FilterScopesWithQuery(query = "(status=open", offset = 0, limit = 1000)),
                        )

                        // Assert - all should be errors
                        results.forEach { result ->
                            result.shouldBeLeft()
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

                        val query = FilterScopesWithQuery(query = complexQuery, offset = 0, limit = 1000)
                        val result = filterScopesWithQueryHandler.invoke(query)

                        // Assert
                        result.shouldBeRight()
                        val scopes = result.getOrNull()!!
                        // scope4 doesn't have active aspect, so NOT active=false should include it
                        scopes.map { scope -> scope.id }.shouldContainExactlyInAnyOrder(scope1.id.value, scope3.id.value, scope4.id.value)
                    }
                }
            }
        }
    })

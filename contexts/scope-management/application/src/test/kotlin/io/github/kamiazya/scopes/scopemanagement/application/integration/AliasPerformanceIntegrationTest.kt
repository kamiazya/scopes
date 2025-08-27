package io.github.kamiazya.scopes.scopemanagement.application.integration

import io.github.kamiazya.scopes.scopemanagement.application.command.AddCustomAliasCommand
import io.github.kamiazya.scopes.scopemanagement.application.command.CreateScopeCommand
import io.github.kamiazya.scopes.scopemanagement.application.query.GetScopeByAliasQuery
import io.github.kamiazya.scopes.scopemanagement.application.query.SearchAliasesQuery
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.system.measureTimeMillis

/**
 * Integration tests for performance and concurrency aspects of the alias system.
 * Tests scalability, concurrent operations, and performance characteristics.
 */
class AliasPerformanceIntegrationTest :
    DescribeSpec({

        lateinit var context: IntegrationTestContext

        beforeSpec {
            IntegrationTestFixture.setupTestDependencies()
        }

        afterSpec {
            IntegrationTestFixture.tearDownTestDependencies()
        }

        beforeEach {
            context = IntegrationTestFixture.createTestContext()
        }

        describe("Alias Performance Integration Tests") {

            describe("Bulk Operations Performance") {
                it("should handle creating many scopes with aliases efficiently") {
                    // Given
                    val scopeCount = 100

                    // When - Measure time to create scopes
                    val createTime = measureTimeMillis {
                        repeat(scopeCount) { index ->
                            context.createScopeHandler.handle(
                                CreateScopeCommand(title = "Perf Test Scope $index"),
                            ).shouldBeRight()
                        }
                    }

                    // Then - Should complete in reasonable time
                    println("Created $scopeCount scopes in ${createTime}ms (${createTime / scopeCount}ms per scope)")
                    createTime shouldBe { it < 10000 } // Less than 10 seconds for 100 scopes

                    // Verify all have canonical aliases
                    val searchResult = context.searchAliasesHandler.handle(
                        SearchAliasesQuery("", limit = 1000),
                    ).getOrNull()!!
                    searchResult.size shouldBe { it >= scopeCount } // At least one alias per scope
                }

                it("should handle bulk alias additions efficiently") {
                    // Given - Create scope
                    val scope = context.createScopeHandler.handle(
                        CreateScopeCommand(title = "Bulk Alias Test"),
                    ).getOrNull()!!

                    val aliasCount = 50

                    // When - Add many aliases
                    val addTime = measureTimeMillis {
                        (1..aliasCount).forEach { index ->
                            context.addCustomAliasHandler.handle(
                                AddCustomAliasCommand(scope.id, "bulk-alias-$index"),
                            ).shouldBeRight()
                        }
                    }

                    // Then
                    println("Added $aliasCount aliases in ${addTime}ms (${addTime / aliasCount}ms per alias)")
                    addTime shouldBe { it < 5000 } // Less than 5 seconds for 50 aliases

                    // Verify count
                    val aliases = context.getAliasesByScopeIdHandler.handle(
                        io.github.kamiazya.scopes.scopemanagement.application.query.GetAliasesByScopeIdQuery(scope.id),
                    ).getOrNull()!!
                    aliases.shouldHaveSize(aliasCount + 1) // +1 for canonical
                }

                it("should perform alias lookups efficiently with many aliases") {
                    // Given - Create many scopes with custom aliases
                    val lookupCount = 100
                    val aliasesToCreate = (1..lookupCount).map { "lookup-perf-$it" }

                    // Create scopes and aliases
                    aliasesToCreate.forEach { aliasName ->
                        val scope = context.createScopeHandler.handle(
                            CreateScopeCommand(title = "Lookup Test $aliasName"),
                        ).getOrNull()!!

                        context.addCustomAliasHandler.handle(
                            AddCustomAliasCommand(scope.id, aliasName),
                        ).shouldBeRight()
                    }

                    // When - Perform lookups
                    val lookupTime = measureTimeMillis {
                        aliasesToCreate.forEach { aliasName ->
                            context.getScopeByAliasHandler.handle(
                                GetScopeByAliasQuery(aliasName),
                            ).shouldBeRight()
                        }
                    }

                    // Then
                    println("Performed $lookupCount lookups in ${lookupTime}ms (${lookupTime / lookupCount}ms per lookup)")
                    lookupTime shouldBe { it < 2000 } // Less than 2 seconds for 100 lookups
                }
            }

            describe("Concurrent Operations") {
                it("should handle concurrent scope creation with aliases") {
                    // When - Create scopes concurrently
                    val concurrentCount = 20

                    suspend fun createScopesConcurrently() = coroutineScope {
                        val jobs = (1..concurrentCount).map { index ->
                            async {
                                context.createScopeHandler.handle(
                                    CreateScopeCommand(title = "Concurrent Scope $index"),
                                )
                            }
                        }
                        jobs.awaitAll()
                    }

                    val concurrentTime = measureTimeMillis {
                        kotlinx.coroutines.runBlocking {
                            val results = createScopesConcurrently()

                            // Verify all succeeded
                            results.forEach { result ->
                                result.shouldBeRight()
                            }
                        }
                    }

                    // Then
                    println("Created $concurrentCount scopes concurrently in ${concurrentTime}ms")
                    concurrentTime shouldBe { it < 5000 } // Should be faster than sequential
                }

                it("should handle concurrent alias additions to different scopes") {
                    // Given - Create multiple scopes
                    val scopeCount = 10
                    val scopes = (1..scopeCount).map { index ->
                        context.createScopeHandler.handle(
                            CreateScopeCommand(title = "Concurrent Target $index"),
                        ).getOrNull()!!
                    }

                    // When - Add aliases concurrently to different scopes
                    suspend fun addAliasesConcurrently() = coroutineScope {
                        val jobs = scopes.mapIndexed { index, scope ->
                            async {
                                context.addCustomAliasHandler.handle(
                                    AddCustomAliasCommand(scope.id, "concurrent-unique-$index"),
                                )
                            }
                        }
                        jobs.awaitAll()
                    }

                    val results = kotlinx.coroutines.runBlocking {
                        addAliasesConcurrently()
                    }

                    // Then - All should succeed (different aliases)
                    results.forEach { result ->
                        result.shouldBeRight()
                    }
                }

                it("should handle concurrent lookups efficiently") {
                    // Given - Create scopes with aliases
                    val aliasCount = 20
                    val aliases = mutableListOf<String>()

                    repeat(aliasCount) { index ->
                        val scope = context.createScopeHandler.handle(
                            CreateScopeCommand(title = "Lookup Target $index"),
                        ).getOrNull()!!

                        val aliasName = "concurrent-lookup-$index"
                        context.addCustomAliasHandler.handle(
                            AddCustomAliasCommand(scope.id, aliasName),
                        ).shouldBeRight()
                        aliases.add(aliasName)
                    }

                    // When - Perform concurrent lookups
                    suspend fun lookupsConcurrently() = coroutineScope {
                        val jobs = aliases.map { aliasName ->
                            async {
                                context.getScopeByAliasHandler.handle(
                                    GetScopeByAliasQuery(aliasName),
                                )
                            }
                        }
                        jobs.awaitAll()
                    }

                    val lookupTime = measureTimeMillis {
                        val results = kotlinx.coroutines.runBlocking {
                            lookupsConcurrently()
                        }

                        // Verify all succeeded
                        results.forEach { result ->
                            result.shouldBeRight()
                        }
                    }

                    // Then
                    println("Performed $aliasCount concurrent lookups in ${lookupTime}ms")
                    lookupTime shouldBe { it < 1000 } // Should be very fast
                }

                it("should handle race conditions in alias creation") {
                    // Given - Create a scope
                    val scope = context.createScopeHandler.handle(
                        CreateScopeCommand(title = "Race Condition Test"),
                    ).getOrNull()!!

                    val raceAlias = "race-condition-alias"

                    // When - Try to add same alias concurrently
                    suspend fun addSameAliasConcurrently() = coroutineScope {
                        val jobs = (1..10).map {
                            async {
                                context.addCustomAliasHandler.handle(
                                    AddCustomAliasCommand(scope.id, raceAlias),
                                )
                            }
                        }
                        jobs.awaitAll()
                    }

                    val results = kotlinx.coroutines.runBlocking {
                        addSameAliasConcurrently()
                    }

                    // Then - Only one should succeed
                    val successes = results.count { it.isRight() }
                    val failures = results.count { it.isLeft() }

                    successes shouldBe 1
                    failures shouldBe 9
                }
            }

            describe("Search Performance") {
                it("should handle prefix search efficiently with many aliases") {
                    // Given - Create many aliases with common prefixes
                    val prefixes = listOf("alpha", "beta", "gamma", "delta", "epsilon")
                    val aliasesPerPrefix = 20

                    prefixes.forEach { prefix ->
                        repeat(aliasesPerPrefix) { index ->
                            val scope = context.createScopeHandler.handle(
                                CreateScopeCommand(title = "Search Test $prefix-$index"),
                            ).getOrNull()!!

                            context.addCustomAliasHandler.handle(
                                AddCustomAliasCommand(scope.id, "$prefix-search-$index"),
                            ).shouldBeRight()
                        }
                    }

                    // When - Search by each prefix
                    val searchTime = measureTimeMillis {
                        prefixes.forEach { prefix ->
                            val result = context.searchAliasesHandler.handle(
                                SearchAliasesQuery(prefix, limit = 50),
                            ).getOrNull()!!

                            result.size shouldBe { it >= aliasesPerPrefix }
                        }
                    }

                    // Then
                    val totalAliases = prefixes.size * aliasesPerPrefix
                    println("Searched $totalAliases aliases with ${prefixes.size} prefixes in ${searchTime}ms")
                    searchTime shouldBe { it < 2000 } // Should be fast even with many aliases
                }

                it("should respect search limits for performance") {
                    // Given - Create many aliases
                    val totalAliases = 200
                    repeat(totalAliases) { index ->
                        val scope = context.createScopeHandler.handle(
                            CreateScopeCommand(title = "Limit Test $index"),
                        ).getOrNull()!!

                        context.addCustomAliasHandler.handle(
                            AddCustomAliasCommand(scope.id, "limit-test-$index"),
                        ).shouldBeRight()
                    }

                    // When - Search with different limits
                    val limits = listOf(10, 50, 100)
                    limits.forEach { limit ->
                        val searchTime = measureTimeMillis {
                            val result = context.searchAliasesHandler.handle(
                                SearchAliasesQuery("limit-test", limit = limit),
                            ).getOrNull()!!

                            result.shouldHaveSize(limit)
                        }

                        println("Search with limit $limit completed in ${searchTime}ms")
                    }
                }
            }

            describe("Memory and Resource Usage") {
                it("should handle large-scale alias operations without memory issues") {
                    // This test simulates a realistic production scenario
                    // Given
                    val projectCount = 50
                    val aliasesPerProject = 5

                    // When - Create projects with multiple aliases each
                    val totalTime = measureTimeMillis {
                        repeat(projectCount) { projectIndex ->
                            // Create project scope
                            val project = context.createScopeHandler.handle(
                                CreateScopeCommand(title = "Project $projectIndex"),
                            ).getOrNull()!!

                            // Add custom aliases
                            repeat(aliasesPerProject) { aliasIndex ->
                                context.addCustomAliasHandler.handle(
                                    AddCustomAliasCommand(
                                        project.id,
                                        "project-$projectIndex-alias-$aliasIndex",
                                    ),
                                ).shouldBeRight()
                            }
                        }
                    }

                    // Then
                    val totalOperations = projectCount + (projectCount * aliasesPerProject)
                    println("Created $projectCount projects with $aliasesPerProject aliases each")
                    println("Total operations: $totalOperations in ${totalTime}ms")
                    println("Average time per operation: ${totalTime / totalOperations}ms")

                    // Verify we can still perform operations efficiently
                    val lookupTime = measureTimeMillis {
                        context.getScopeByAliasHandler.handle(
                            GetScopeByAliasQuery("project-25-alias-3"),
                        ).shouldBeRight()
                    }

                    lookupTime shouldBe { it < 100 } // Lookup should still be fast
                }

                it("should maintain performance with hierarchical scopes and aliases") {
                    // Given - Create deep hierarchy
                    var parentId = context.createScopeHandler.handle(
                        CreateScopeCommand(title = "Root"),
                    ).getOrNull()!!.id

                    val depth = 10
                    val childrenPerLevel = 3

                    // When - Create hierarchy with aliases
                    repeat(depth) { level ->
                        repeat(childrenPerLevel) { child ->
                            val scope = context.createScopeHandler.handle(
                                CreateScopeCommand(
                                    title = "Level $level Child $child",
                                    parentId = parentId,
                                ),
                            ).getOrNull()!!

                            // Add custom alias
                            context.addCustomAliasHandler.handle(
                                AddCustomAliasCommand(scope.id, "level-$level-child-$child"),
                            ).shouldBeRight()

                            if (child == 0) {
                                parentId = scope.id // Continue depth with first child
                            }
                        }
                    }

                    // Then - Operations should still be performant
                    val deepLookupTime = measureTimeMillis {
                        context.getScopeByAliasHandler.handle(
                            GetScopeByAliasQuery("level-5-child-2"),
                        ).shouldBeRight()
                    }

                    deepLookupTime shouldBe { it < 100 }
                }
            }
        }
    })

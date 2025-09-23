package io.github.kamiazya.scopes.scopemanagement.application.command.handler

import io.github.kamiazya.scopes.platform.domain.aggregate.AggregateResult
import io.github.kamiazya.scopes.scopemanagement.domain.aggregate.ScopeAggregate
import io.github.kamiazya.scopes.scopemanagement.domain.event.ScopeEvent
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class CreateScopeHandlerTest :
    DescribeSpec({
        describe("ScopeAggregate alias generation integration") {
            it("should create aggregate with handleCreateWithAutoAlias") {
                // Test verifies that AliasGenerationService integration works correctly
                val result = ScopeAggregate.handleCreateWithAutoAlias(
                    title = "Test Scope",
                    description = "Test Description",
                )

                result.shouldBeRight()
                result.fold(
                    ifLeft = { error ->
                        throw AssertionError("Expected success but got error: $error")
                    },
                    ifRight = { aggregateResult: AggregateResult<ScopeAggregate, ScopeEvent> ->
                        println("✅ AliasGenerationService integration test successful!")
                        println("Created aggregate: ${aggregateResult.aggregate}")
                        println("Generated events: ${aggregateResult.events.size}")

                        // Verify the aggregate was created correctly
                        aggregateResult.aggregate shouldNotBe null
                        aggregateResult.events.size shouldBe 2 // ScopeCreated + AliasAssigned

                        val aggregate = aggregateResult.aggregate
                        aggregate.scopeId shouldNotBe null
                        aggregate.title shouldNotBe null
                        aggregate.canonicalAliasId shouldNotBe null
                        aggregate.aliases.size shouldBe 1

                        println("✅ All assertions passed! AliasGenerationService successfully integrated into ScopeAggregate")
                    },
                )
            }
        }
    })

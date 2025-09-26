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
        describe("ScopeAggregate explicit alias creation") {
            it("should create aggregate with handleCreateWithAlias") {
                val aliasName = io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasName
                    .create("test-alias").fold(
                        { e -> throw AssertionError("alias creation failed: $e") },
                        { it },
                    )

                val result = ScopeAggregate.handleCreateWithAlias(
                    title = "Test Scope",
                    description = "Test Description",
                    aliasName = aliasName,
                )

                result.shouldBeRight()
                result.fold(
                    ifLeft = { error ->
                        throw AssertionError("Expected success but got error: $error")
                    },
                    ifRight = { aggregateResult: AggregateResult<ScopeAggregate, ScopeEvent> ->
                        aggregateResult.aggregate shouldNotBe null
                        aggregateResult.events.size shouldBe 2 // ScopeCreated + AliasAssigned

                        val aggregate = aggregateResult.aggregate
                        aggregate.scopeId shouldNotBe null
                        aggregate.title shouldNotBe null
                        aggregate.canonicalAliasId shouldNotBe null
                        aggregate.aliases.size shouldBe 1
                    },
                )
            }
        }
    })

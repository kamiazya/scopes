package io.github.kamiazya.scopes.domain.event

import arrow.core.Either
import arrow.core.getOrElse
import io.github.kamiazya.scopes.domain.TestHelpers
import io.github.kamiazya.scopes.domain.entity.ScopeAlias
import io.github.kamiazya.scopes.domain.error.AggregateIdError
import io.github.kamiazya.scopes.domain.valueobject.AliasId
import io.github.kamiazya.scopes.domain.valueobject.AliasName
import io.github.kamiazya.scopes.domain.valueobject.AliasType
import io.github.kamiazya.scopes.domain.valueobject.ScopeId
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.datetime.Clock

class AliasEventsTest :
    DescribeSpec({

        describe("AliasAssigned") {
            it("should create event from ScopeAlias entity") {
                // Arrange
                val aliasId = AliasId.generate()
                val aliasName = AliasName.create("test-alias").getOrElse { throw AssertionError() }
                val scopeId = ScopeId.generate()
                val aliasType = AliasType.CANONICAL

                val alias = ScopeAlias.createCanonicalWithId(
                    id = aliasId,
                    scopeId = scopeId,
                    aliasName = aliasName,
                )

                val eventId = TestHelpers.testEventId("AliasAssigned")

                // Act
                val result: Either<AggregateIdError, AliasAssigned> = AliasAssigned.from(alias, eventId)

                // Assert
                // result.shouldBeRight()
                val event = when (result) {
                    is Either.Right -> result.value
                    is Either.Left -> throw AssertionError("Expected Right but got Left: ${result.value}")
                }
                event.shouldNotBeNull()
                event.aggregateId.value shouldBe "gid://scopes/ScopeAlias/${aliasId.value}"
                event.eventId shouldBe eventId
                event.occurredAt shouldBe alias.createdAt
                event.version shouldBe 1
                event.aliasId shouldBe aliasId
                event.aliasName shouldBe aliasName
                event.scopeId shouldBe scopeId
                event.aliasType shouldBe aliasType
                event.aggregateId.aggregateType shouldBe "ScopeAlias"
                event.eventType shouldBe "AliasAssigned"
            }

            it("should handle custom alias type") {
                // Arrange
                val aliasId = AliasId.generate()
                val aliasName = AliasName.create("custom-alias").getOrElse { throw AssertionError() }
                val scopeId = ScopeId.generate()
                val aliasType = AliasType.CUSTOM

                val alias = ScopeAlias.createCustom(
                    scopeId = scopeId,
                    aliasName = aliasName,
                )

                val eventId = TestHelpers.testEventId("AliasAssigned")

                // Act
                val result: Either<AggregateIdError, AliasAssigned> = AliasAssigned.from(alias, eventId)

                // Assert
                // result.shouldBeRight()
                val event = when (result) {
                    is Either.Right -> result.value
                    is Either.Left -> throw AssertionError("Expected Right but got Left: ${result.value}")
                }
                event.shouldNotBeNull()
                event.aliasType shouldBe AliasType.CUSTOM
            }
        }

        describe("AliasRemoved") {
            it("should create removal event") {
                // Arrange
                val aliasId = AliasId.generate()
                val aliasName = AliasName.create("removed-alias").getOrElse { throw AssertionError() }
                val scopeId = ScopeId.generate()
                val aliasType = AliasType.CUSTOM
                val eventId = TestHelpers.testEventId("AliasRemoved")
                val occurredAt = Clock.System.now()
                val removedAt = occurredAt

                // Act
                val aggregateId = aliasId.toAggregateId().getOrElse { throw AssertionError() }
                val event = AliasRemoved(
                    aggregateId = aggregateId,
                    eventId = eventId,
                    occurredAt = occurredAt,
                    version = 2,
                    aliasId = aliasId,
                    aliasName = aliasName,
                    scopeId = scopeId,
                    aliasType = aliasType,
                    removedAt = removedAt,
                )

                // Assert
                event.aggregateId shouldBe aggregateId
                event.eventId shouldBe eventId
                event.occurredAt shouldBe occurredAt
                event.version shouldBe 2
                event.aliasId shouldBe aliasId
                event.aliasName shouldBe aliasName
                event.scopeId shouldBe scopeId
                event.aliasType shouldBe aliasType
                event.removedAt shouldBe removedAt
                event.aggregateId.aggregateType shouldBe "ScopeAlias"
                event.eventType shouldBe "AliasRemoved"
            }
        }

        describe("AliasNameChanged") {
            it("should create name change event") {
                // Arrange
                val aliasId = AliasId.generate()
                val scopeId = ScopeId.generate()
                val oldAliasName = AliasName.create("old-alias").getOrElse { throw AssertionError() }
                val newAliasName = AliasName.create("new-alias").getOrElse { throw AssertionError() }
                val eventId = TestHelpers.testEventId("AliasNameChanged")
                val occurredAt = Clock.System.now()

                // Act
                val aggregateId = aliasId.toAggregateId().getOrElse { throw AssertionError() }
                val event = AliasNameChanged(
                    aggregateId = aggregateId,
                    eventId = eventId,
                    occurredAt = occurredAt,
                    version = 2,
                    aliasId = aliasId,
                    scopeId = scopeId,
                    oldAliasName = oldAliasName,
                    newAliasName = newAliasName,
                )

                // Assert
                event.aggregateId shouldBe aggregateId
                event.aliasId shouldBe aliasId
                event.scopeId shouldBe scopeId
                event.oldAliasName shouldBe oldAliasName
                event.newAliasName shouldBe newAliasName
                event.eventType shouldBe "AliasNameChanged"
            }
        }

        describe("CanonicalAliasReplaced") {
            it("should create canonical alias replacement event") {
                // Arrange
                val scopeId = ScopeId.generate()
                val oldAliasId = AliasId.generate()
                val oldAliasName = AliasName.create("old-canonical").getOrElse { throw AssertionError() }
                val newAliasId = AliasId.generate()
                val newAliasName = AliasName.create("new-canonical").getOrElse { throw AssertionError() }
                val eventId = TestHelpers.testEventId("CanonicalAliasReplaced")
                val occurredAt = Clock.System.now()

                // Act
                val aggregateId = scopeId.toAggregateId().getOrElse { throw AssertionError() }
                val event = CanonicalAliasReplaced(
                    aggregateId = aggregateId,
                    eventId = eventId,
                    occurredAt = occurredAt,
                    version = 3,
                    scopeId = scopeId,
                    oldAliasId = oldAliasId,
                    oldAliasName = oldAliasName,
                    newAliasId = newAliasId,
                    newAliasName = newAliasName,
                )

                // Assert
                event.aggregateId shouldBe aggregateId // Note: aggregate ID is scope ID for this event
                event.scopeId shouldBe scopeId
                event.oldAliasId shouldBe oldAliasId
                event.oldAliasName shouldBe oldAliasName
                event.newAliasId shouldBe newAliasId
                event.newAliasName shouldBe newAliasName
                event.eventType shouldBe "CanonicalAliasReplaced"
            }
        }
    })

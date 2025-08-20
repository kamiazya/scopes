package io.github.kamiazya.scopes.domain.event

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.nonEmptyListOf
import io.github.kamiazya.scopes.domain.TestHelpers
import io.github.kamiazya.scopes.domain.entity.Scope
import io.github.kamiazya.scopes.domain.error.AggregateIdError
import io.github.kamiazya.scopes.domain.valueobject.AspectKey
import io.github.kamiazya.scopes.domain.valueobject.AspectValue
import io.github.kamiazya.scopes.domain.valueobject.ScopeDescription
import io.github.kamiazya.scopes.domain.valueobject.ScopeId
import io.github.kamiazya.scopes.domain.valueobject.ScopeTitle
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.datetime.Clock

class ScopeEventsTest :
    DescribeSpec({

        describe("ScopeCreated") {
            it("should create event from Scope entity") {
                // Arrange
                val title = ScopeTitle.create("Test Scope").getOrElse { throw AssertionError() }
                val description = ScopeDescription.create("Test description").getOrElse { throw AssertionError() }
                val parentId = ScopeId.generate()
                val aspectKey = AspectKey.create("status").getOrElse { throw AssertionError() }
                val aspectValue = AspectValue.create("active").getOrElse { throw AssertionError() }
                val aspects = mapOf(aspectKey to nonEmptyListOf(aspectValue))

                val scope = Scope.create(
                    title = title.value,
                    description = description?.value,
                    parentId = parentId,
                    aspectsData = aspects,
                ).getOrElse { throw AssertionError() }

                val eventId = TestHelpers.testEventId("ScopeCreated")

                // Act
                val result: Either<AggregateIdError, ScopeCreated> = ScopeCreated.from(scope, eventId)

                // Assert
                // result.shouldBeRight()
                val event = when (result) {
                    is Either.Right -> result.value
                    is Either.Left -> throw AssertionError("Expected Right but got Left: ${result.value}")
                }
                event.shouldNotBeNull()
                event.aggregateId.value shouldBe "gid://scopes/Scope/${scope.id.value}"
                event.eventId shouldBe eventId
                event.occurredAt shouldBe scope.createdAt
                event.version shouldBe 1
                event.scopeId shouldBe scope.id
                event.title shouldBe title
                event.description shouldBe description
                event.parentId shouldBe parentId
                event.aspects shouldBe aspects
                event.aggregateId.aggregateType shouldBe "Scope"
                event.eventType shouldBe "ScopeCreated"
            }

            it("should handle scope without description and parent") {
                // Arrange
                val scopeId = ScopeId.generate()
                val title = ScopeTitle.create("Test Scope").getOrElse { throw AssertionError() }

                val scope = Scope.create(
                    title = title.value,
                    description = null,
                    parentId = null,
                    aspectsData = emptyMap(),
                ).getOrElse { throw AssertionError() }

                val eventId = TestHelpers.testEventId("ScopeCreated")

                // Act
                val result: Either<AggregateIdError, ScopeCreated> = ScopeCreated.from(scope, eventId)

                // Assert
                // result.shouldBeRight()
                val event = when (result) {
                    is Either.Right -> result.value
                    is Either.Left -> throw AssertionError("Expected Right but got Left: ${result.value}")
                }
                event.shouldNotBeNull()
                event.description shouldBe null
                event.parentId shouldBe null
                event.aspects shouldBe emptyMap()
            }
        }

        describe("ScopeTitleUpdated") {
            it("should create title update event") {
                // Arrange
                val scopeId = ScopeId.generate()
                val oldTitle = ScopeTitle.create("Old Title").getOrElse { throw AssertionError() }
                val newTitle = ScopeTitle.create("New Title").getOrElse { throw AssertionError() }
                val eventId = TestHelpers.testEventId("ScopeTitleUpdated")
                val occurredAt = Clock.System.now()

                // Act
                val event = ScopeTitleUpdated(
                    aggregateId = scopeId.toAggregateId().getOrElse { throw AssertionError() },
                    eventId = eventId,
                    occurredAt = occurredAt,
                    version = 2,
                    scopeId = scopeId,
                    oldTitle = oldTitle,
                    newTitle = newTitle,
                )

                // Assert
                event.aggregateId shouldBe scopeId.toAggregateId().getOrElse { throw AssertionError() }
                event.eventId shouldBe eventId
                event.occurredAt shouldBe occurredAt
                event.version shouldBe 2
                event.scopeId shouldBe scopeId
                event.oldTitle shouldBe oldTitle
                event.newTitle shouldBe newTitle
                event.aggregateId.aggregateType shouldBe "Scope"
                event.eventType shouldBe "ScopeTitleUpdated"
            }
        }

        describe("ScopeDescriptionUpdated") {
            it("should create description update event") {
                // Arrange
                val scopeId = ScopeId.generate()
                val oldDescription = ScopeDescription.create("Old description").getOrElse { throw AssertionError() }
                val newDescription = ScopeDescription.create("New description").getOrElse { throw AssertionError() }
                val eventId = TestHelpers.testEventId("ScopeDescriptionUpdated")
                val occurredAt = Clock.System.now()

                // Act
                val event = ScopeDescriptionUpdated(
                    aggregateId = scopeId.toAggregateId().getOrElse { throw AssertionError() },
                    eventId = eventId,
                    occurredAt = occurredAt,
                    version = 2,
                    scopeId = scopeId,
                    oldDescription = oldDescription,
                    newDescription = newDescription,
                )

                // Assert
                event.oldDescription shouldBe oldDescription
                event.newDescription shouldBe newDescription
                event.eventType shouldBe "ScopeDescriptionUpdated"
            }

            it("should handle null descriptions") {
                // Arrange
                val scopeId = ScopeId.generate()
                val eventId = TestHelpers.testEventId("ScopeDescriptionUpdated")
                val occurredAt = Clock.System.now()

                // Act
                val event = ScopeDescriptionUpdated(
                    aggregateId = scopeId.toAggregateId().getOrElse { throw AssertionError() },
                    eventId = eventId,
                    occurredAt = occurredAt,
                    version = 2,
                    scopeId = scopeId,
                    oldDescription = null,
                    newDescription = null,
                )

                // Assert
                event.oldDescription shouldBe null
                event.newDescription shouldBe null
            }
        }

        describe("ScopeParentChanged") {
            it("should create parent change event") {
                // Arrange
                val scopeId = ScopeId.generate()
                val oldParentId = ScopeId.generate()
                val newParentId = ScopeId.generate()
                val eventId = TestHelpers.testEventId("ScopeParentChanged")
                val occurredAt = Clock.System.now()

                // Act
                val event = ScopeParentChanged(
                    aggregateId = scopeId.toAggregateId().getOrElse { throw AssertionError() },
                    eventId = eventId,
                    occurredAt = occurredAt,
                    version = 3,
                    scopeId = scopeId,
                    oldParentId = oldParentId,
                    newParentId = newParentId,
                )

                // Assert
                event.oldParentId shouldBe oldParentId
                event.newParentId shouldBe newParentId
                event.eventType shouldBe "ScopeParentChanged"
            }
        }

        describe("ScopeAspectAdded") {
            it("should create aspect added event") {
                // Arrange
                val scopeId = ScopeId.generate()
                val aspectKey = AspectKey.create("status").getOrElse { throw AssertionError() }
                val aspectValue = AspectValue.create("active").getOrElse { throw AssertionError() }
                val eventId = TestHelpers.testEventId("ScopeAspectAdded")
                val occurredAt = Clock.System.now()

                // Act
                val event = ScopeAspectAdded(
                    aggregateId = scopeId.toAggregateId().getOrElse { throw AssertionError() },
                    eventId = eventId,
                    occurredAt = occurredAt,
                    version = 2,
                    scopeId = scopeId,
                    aspectKey = aspectKey,
                    aspectValue = aspectValue,
                )

                // Assert
                event.aspectKey shouldBe aspectKey
                event.aspectValue shouldBe aspectValue
                event.eventType shouldBe "ScopeAspectAdded"
            }
        }

        describe("ScopeAspectRemoved") {
            it("should create aspect removed event") {
                // Arrange
                val scopeId = ScopeId.generate()
                val aspectKey = AspectKey.create("priority").getOrElse { throw AssertionError() }
                val aspectValue = AspectValue.create("high").getOrElse { throw AssertionError() }
                val eventId = TestHelpers.testEventId("ScopeAspectRemoved")
                val occurredAt = Clock.System.now()

                // Act
                val event = ScopeAspectRemoved(
                    aggregateId = scopeId.toAggregateId().getOrElse { throw AssertionError() },
                    eventId = eventId,
                    occurredAt = occurredAt,
                    version = 3,
                    scopeId = scopeId,
                    aspectKey = aspectKey,
                    aspectValue = aspectValue,
                )

                // Assert
                event.aspectKey shouldBe aspectKey
                event.aspectValue shouldBe aspectValue
                event.eventType shouldBe "ScopeAspectRemoved"
            }
        }

        describe("ScopeDeleted") {
            it("should create delete event") {
                // Arrange
                val scopeId = ScopeId.generate()
                val eventId = TestHelpers.testEventId("ScopeDeleted")
                val occurredAt = Clock.System.now()
                val deletedAt = occurredAt

                // Act
                val event = ScopeDeleted(
                    aggregateId = scopeId.toAggregateId().getOrElse { throw AssertionError() },
                    eventId = eventId,
                    occurredAt = occurredAt,
                    version = 5,
                    scopeId = scopeId,
                    deletedAt = deletedAt,
                )

                // Assert
                event.scopeId shouldBe scopeId
                event.deletedAt shouldBe deletedAt
                event.eventType shouldBe "ScopeDeleted"
            }
        }

        describe("ScopeChanges") {
            it("should create changes with all fields") {
                // Arrange
                val oldTitle = ScopeTitle.create("Old").getOrElse { throw AssertionError() }
                val newTitle = ScopeTitle.create("New").getOrElse { throw AssertionError() }
                val oldDesc = ScopeDescription.create("Old desc").getOrElse { throw AssertionError() }
                val newDesc = ScopeDescription.create("New desc").getOrElse { throw AssertionError() }
                val oldParent = ScopeId.generate()
                val newParent = ScopeId.generate()
                val aspectKey = AspectKey.create("test").getOrElse { throw AssertionError() }
                val aspectValue = AspectValue.create("value").getOrElse { throw AssertionError() }

                // Act
                val changes = ScopeChanges(
                    titleChange = ScopeChanges.TitleChange(oldTitle, newTitle),
                    descriptionChange = ScopeChanges.DescriptionChange(oldDesc, newDesc),
                    parentChange = ScopeChanges.ParentChange(oldParent, newParent),
                    aspectChanges = listOf(
                        ScopeChanges.AspectChange.Added(aspectKey, aspectValue),
                        ScopeChanges.AspectChange.Removed(aspectKey, aspectValue),
                    ),
                )

                // Assert
                changes.titleChange shouldNotBe null
                changes.titleChange?.oldTitle shouldBe oldTitle
                changes.titleChange?.newTitle shouldBe newTitle
                changes.descriptionChange shouldNotBe null
                changes.parentChange shouldNotBe null
                changes.aspectChanges.isEmpty() shouldBe false
            }

            it("should create changes with null fields") {
                // Act
                val changes = ScopeChanges()

                // Assert
                changes.titleChange shouldBe null
                changes.descriptionChange shouldBe null
                changes.parentChange shouldBe null
                changes.aspectChanges.isEmpty() shouldBe true
            }
        }
    })

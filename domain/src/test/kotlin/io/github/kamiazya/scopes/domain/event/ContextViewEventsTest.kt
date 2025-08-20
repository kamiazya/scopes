package io.github.kamiazya.scopes.domain.event

import arrow.core.Either
import arrow.core.getOrElse
import io.github.kamiazya.scopes.domain.entity.ContextView
import io.github.kamiazya.scopes.domain.valueobject.ContextViewDescription
import io.github.kamiazya.scopes.domain.valueobject.ContextViewFilter
import io.github.kamiazya.scopes.domain.valueobject.ContextViewId
import io.github.kamiazya.scopes.domain.valueobject.ContextViewKey
import io.github.kamiazya.scopes.domain.valueobject.ContextViewName
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.nulls.shouldNotBeNull
import kotlinx.datetime.Clock
import io.github.kamiazya.scopes.domain.TestHelpers
import io.github.kamiazya.scopes.domain.error.AggregateIdError

class ContextViewEventsTest : DescribeSpec({

    describe("ContextViewCreated") {
        it("should create event from ContextView entity") {
            // Arrange
            val key = ContextViewKey.create("test-view").getOrElse { throw AssertionError() }
            val name = ContextViewName.create("Test View").getOrElse { throw AssertionError() }
            val filter = ContextViewFilter.create("status:active").getOrElse { throw AssertionError() }
            val description = ContextViewDescription.create("Test context view").getOrElse { throw AssertionError() }

            val contextView = ContextView.create(
                key = key,
                name = name,
                filter = filter,
                description = description.value
            ).getOrElse { throw AssertionError() }

            val eventId = TestHelpers.testEventId("ContextViewCreated")

            // Act
            val result: Either<AggregateIdError, ContextViewCreated> = ContextViewCreated.from(contextView, eventId)

            // Assert
            // result.shouldBeRight()
            val event = when (result) {
                is Either.Right -> result.value
                is Either.Left -> throw AssertionError("Expected Right but got Left: ${result.value}")
            }
            event.shouldNotBeNull()
            event.aggregateId.value shouldBe "gid://scopes/ContextView/${contextView.id.value}"
            event.eventId shouldBe eventId
            event.occurredAt shouldBe contextView.createdAt
            event.version shouldBe 1
            event.contextViewId shouldBe contextView.id
            event.key shouldBe key
            event.name shouldBe name
            event.filter shouldBe filter
            event.description shouldBe description
            event.aggregateId.aggregateType shouldBe "ContextView"
            event.eventType shouldBe "ContextViewCreated"
        }

        it("should handle context view without description") {
            // Arrange
            val key = ContextViewKey.create("test-view-2").getOrElse { throw AssertionError() }
            val name = ContextViewName.create("Test View 2").getOrElse { throw AssertionError() }
            val filter = ContextViewFilter.create("status:active").getOrElse { throw AssertionError() }

            val contextView = ContextView.create(
                key = key,
                name = name,
                filter = filter,
                description = null
            ).getOrElse { throw AssertionError() }

            val eventId = TestHelpers.testEventId("ContextViewCreated")

            // Act
            val result: Either<AggregateIdError, ContextViewCreated> = ContextViewCreated.from(contextView, eventId)

            // Assert
            // result.shouldBeRight()
            val event = when (result) {
                is Either.Right -> result.value
                is Either.Left -> throw AssertionError("Expected Right but got Left: ${result.value}")
            }
            event.shouldNotBeNull()
            event.description shouldBe null
        }
    }

    describe("ContextViewUpdated") {
        it("should create update event with changes") {
            // Arrange
            val contextViewId = ContextViewId.generate()
            val oldName = ContextViewName.create("OldName").getOrElse { throw AssertionError() }
            val newName = ContextViewName.create("NewName").getOrElse { throw AssertionError() }
            val changes = ContextViewChanges(
                nameChange = ContextViewChanges.NameChange(oldName, newName)
            )
            val eventId = TestHelpers.testEventId("ContextViewUpdated")
            val occurredAt = Clock.System.now()

            // Act
            val event = ContextViewUpdated(
                aggregateId = contextViewId.toAggregateId().getOrElse { throw AssertionError() },
                eventId = eventId,
                occurredAt = occurredAt,
                version = 2,
                contextViewId = contextViewId,
                changes = changes
            )

            // Assert
            event.contextViewId shouldBe contextViewId
            event.changes shouldBe changes
            event.eventType shouldBe "ContextViewUpdated"
        }
    }

    describe("ContextViewNameChanged") {
        it("should create name change event") {
            // Arrange
            val contextViewId = ContextViewId.generate()
            val oldName = ContextViewName.create("OldName").getOrElse { throw AssertionError() }
            val newName = ContextViewName.create("NewName").getOrElse { throw AssertionError() }
            val eventId = TestHelpers.testEventId("ContextViewNameChanged")
            val occurredAt = Clock.System.now()

            // Act
            val event = ContextViewNameChanged(
                aggregateId = contextViewId.toAggregateId().getOrElse { throw AssertionError() },
                eventId = eventId,
                occurredAt = occurredAt,
                version = 2,
                contextViewId = contextViewId,
                oldName = oldName,
                newName = newName
            )

            // Assert
            event.oldName shouldBe oldName
            event.newName shouldBe newName
            event.eventType shouldBe "ContextViewNameChanged"
        }
    }

    describe("ContextViewFilterUpdated") {
        it("should create filter update event") {
            // Arrange
            val contextViewId = ContextViewId.generate()
            val oldFilter = ContextViewFilter.create("status:active").getOrElse { throw AssertionError() }
            val newFilter = ContextViewFilter.create("status:inactive").getOrElse { throw AssertionError() }
            val eventId = TestHelpers.testEventId("ContextViewFilterUpdated")
            val occurredAt = Clock.System.now()

            // Act
            val event = ContextViewFilterUpdated(
                aggregateId = contextViewId.toAggregateId().getOrElse { throw AssertionError() },
                eventId = eventId,
                occurredAt = occurredAt,
                version = 2,
                contextViewId = contextViewId,
                oldFilter = oldFilter,
                newFilter = newFilter
            )

            // Assert
            event.oldFilter shouldBe oldFilter
            event.newFilter shouldBe newFilter
            event.eventType shouldBe "ContextViewFilterUpdated"
        }
    }

    describe("ContextViewDescriptionUpdated") {
        it("should create description update event") {
            // Arrange
            val contextViewId = ContextViewId.generate()
            val oldDescription = ContextViewDescription.create("Old description").getOrElse { throw AssertionError() }
            val newDescription = ContextViewDescription.create("New description").getOrElse { throw AssertionError() }
            val eventId = TestHelpers.testEventId("ContextViewDescriptionUpdated")
            val occurredAt = Clock.System.now()

            // Act
            val event = ContextViewDescriptionUpdated(
                aggregateId = contextViewId.toAggregateId().getOrElse { throw AssertionError() },
                eventId = eventId,
                occurredAt = occurredAt,
                version = 2,
                contextViewId = contextViewId,
                oldDescription = oldDescription,
                newDescription = newDescription
            )

            // Assert
            event.oldDescription shouldBe oldDescription
            event.newDescription shouldBe newDescription
            event.eventType shouldBe "ContextViewDescriptionUpdated"
        }

        it("should handle null descriptions") {
            // Arrange
            val contextViewId = ContextViewId.generate()
            val eventId = TestHelpers.testEventId("ContextViewDescriptionUpdated")
            val occurredAt = Clock.System.now()

            // Act
            val event = ContextViewDescriptionUpdated(
                aggregateId = contextViewId.toAggregateId().getOrElse { throw AssertionError() },
                eventId = eventId,
                occurredAt = occurredAt,
                version = 2,
                contextViewId = contextViewId,
                oldDescription = null,
                newDescription = null
            )

            // Assert
            event.oldDescription shouldBe null
            event.newDescription shouldBe null
        }
    }

    describe("ContextViewDeleted") {
        it("should create delete event") {
            // Arrange
            val contextViewId = ContextViewId.generate()
            val eventId = TestHelpers.testEventId("ContextViewDeleted")
            val occurredAt = Clock.System.now()
            val deletedAt = occurredAt

            // Act
            val event = ContextViewDeleted(
                aggregateId = contextViewId.toAggregateId().getOrElse { throw AssertionError() },
                eventId = eventId,
                occurredAt = occurredAt,
                version = 3,
                contextViewId = contextViewId,
                deletedAt = deletedAt
            )

            // Assert
            event.contextViewId shouldBe contextViewId
            event.deletedAt shouldBe deletedAt
            event.eventType shouldBe "ContextViewDeleted"
        }
    }

    describe("ContextViewChanges") {
        it("should create changes with all fields") {
            // Arrange
            val oldName = ContextViewName.create("OldName").getOrElse { throw AssertionError() }
            val newName = ContextViewName.create("NewName").getOrElse { throw AssertionError() }
            val oldFilter = ContextViewFilter.create("old:filter").getOrElse { throw AssertionError() }
            val newFilter = ContextViewFilter.create("new:filter").getOrElse { throw AssertionError() }
            val oldDesc = ContextViewDescription.create("Old desc").getOrElse { throw AssertionError() }
            val newDesc = ContextViewDescription.create("New desc").getOrElse { throw AssertionError() }

            // Act
            val changes = ContextViewChanges(
                nameChange = ContextViewChanges.NameChange(oldName, newName),
                filterChange = ContextViewChanges.FilterChange(oldFilter, newFilter),
                descriptionChange = ContextViewChanges.DescriptionChange(oldDesc, newDesc)
            )

            // Assert
            changes.nameChange shouldNotBe null
            changes.nameChange?.oldName shouldBe oldName
            changes.nameChange?.newName shouldBe newName
            changes.filterChange shouldNotBe null
            changes.filterChange?.oldFilter shouldBe oldFilter
            changes.filterChange?.newFilter shouldBe newFilter
            changes.descriptionChange shouldNotBe null
            changes.descriptionChange?.oldDescription shouldBe oldDesc
            changes.descriptionChange?.newDescription shouldBe newDesc
        }

        it("should create changes with null fields") {
            // Act
            val changes = ContextViewChanges()

            // Assert
            changes.nameChange shouldBe null
            changes.filterChange shouldBe null
            changes.descriptionChange shouldBe null
        }
    }
})

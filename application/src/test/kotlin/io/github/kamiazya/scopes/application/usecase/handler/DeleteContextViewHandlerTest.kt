package io.github.kamiazya.scopes.application.usecase.handler

import arrow.core.left
import arrow.core.right
import arrow.core.getOrElse
import io.github.kamiazya.scopes.application.dto.EmptyResult
import io.github.kamiazya.scopes.application.error.*
import io.github.kamiazya.scopes.application.service.ActiveContextService
import io.github.kamiazya.scopes.application.test.MockLogger
import io.github.kamiazya.scopes.application.usecase.command.DeleteContextView
import io.github.kamiazya.scopes.domain.entity.ContextView
import io.github.kamiazya.scopes.domain.error.PersistenceError as DomainPersistenceError
import io.github.kamiazya.scopes.domain.repository.ContextViewRepository
import io.github.kamiazya.scopes.domain.valueobject.ContextViewFilter
import io.github.kamiazya.scopes.domain.valueobject.ContextViewKey
import io.github.kamiazya.scopes.domain.valueobject.ContextViewName
import io.github.kamiazya.scopes.domain.valueobject.ContextViewId
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.datetime.Clock

class DeleteContextViewHandlerTest : StringSpec({

    lateinit var contextViewRepository: ContextViewRepository
    lateinit var activeContextService: ActiveContextService
    lateinit var logger: MockLogger
    lateinit var handler: DeleteContextViewHandler

    beforeTest {
        contextViewRepository = mockk<ContextViewRepository>()
        activeContextService = mockk<ActiveContextService>()
        logger = MockLogger()
        handler = DeleteContextViewHandler(contextViewRepository, activeContextService, logger)
    }

    "should delete an inactive context view successfully" {
        // Given
        val contextId = ContextViewId.generate()
        val command = DeleteContextView(id = contextId.value)

        // No active context
        coEvery { activeContextService.getCurrentContext() } returns null
        coEvery { contextViewRepository.delete(contextId) } returns Unit.right()

        // When
        val result = handler(command)

        // Then
        result.shouldBeRight()
        result.getOrElse { throw AssertionError("Failed to create value object") } shouldBe EmptyResult

        coVerify(exactly = 1) { contextViewRepository.delete(contextId) }
    }

    "should delete a context view when a different context is active" {
        // Given
        val contextIdToDelete = ContextViewId.generate()
        val activeContextId = ContextViewId.generate()
        val command = DeleteContextView(id = contextIdToDelete.value)

        val activeContext = ContextView(
            id = activeContextId,
            key = ContextViewKey.create("active-context").getOrElse { throw AssertionError("Failed to create value object") },
            name = ContextViewName.create("ActiveContext").getOrElse { throw AssertionError("Failed to create value object") },
            filter = ContextViewFilter.create("status:active").getOrElse { throw AssertionError("Failed to create value object") },
            description = null,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )

        coEvery { activeContextService.getCurrentContext() } returns activeContext
        coEvery { contextViewRepository.delete(contextIdToDelete) } returns Unit.right()

        // When
        val result = handler(command)

        // Then
        result.shouldBeRight()
        result.getOrElse { throw AssertionError("Failed to create value object") } shouldBe EmptyResult
    }

    "should prevent deletion of the currently active context" {
        // Given
        val contextId = ContextViewId.generate()
        val command = DeleteContextView(id = contextId.value)

        val activeContext = ContextView(
            id = contextId,  // Same ID as the one to delete
            key = ContextViewKey.create("active-context").getOrElse { throw AssertionError("Failed to create value object") },
            name = ContextViewName.create("ActiveContext").getOrElse { throw AssertionError("Failed to create value object") },
            filter = ContextViewFilter.create("status:active").getOrElse { throw AssertionError("Failed to create value object") },
            description = null,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )

        coEvery { activeContextService.getCurrentContext() } returns activeContext
        // Don't mock the delete method since it shouldn't be called

        // When
        val result = handler(command)

        // Then
        result.shouldBeLeft()
        val error = result.leftOrNull()!!
        error.shouldBeInstanceOf<ContextError.ActiveContextDeleteAttempt>()
        (error as ContextError.ActiveContextDeleteAttempt).contextId shouldBe contextId.value

        coVerify(exactly = 0) { contextViewRepository.delete(any()) }
    }

    "should handle invalid context ID format" {
        // Given
        val command = DeleteContextView(id = "invalid-id-format")

        // When
        val result = handler(command)

        // Then
        result.shouldBeLeft()
        val error = result.leftOrNull()!!
        error.shouldBeInstanceOf<ScopeInputError.IdInvalidFormat>()
    }

    "should handle empty context ID" {
        // Given
        val command = DeleteContextView(id = "")

        // When
        val result = handler(command)

        // Then
        result.shouldBeLeft()
        val error = result.leftOrNull()!!
        error.shouldBeInstanceOf<ScopeInputError.IdBlank>()
    }

    "should handle repository deletion errors" {
        // Given
        val contextId = ContextViewId.generate()
        val command = DeleteContextView(id = contextId.value)

        coEvery { activeContextService.getCurrentContext() } returns null

        val persistenceError = DomainPersistenceError.StorageUnavailable(
            occurredAt = Clock.System.now(),
            operation = "delete",
            cause = Exception("Database error")
        )
        coEvery { contextViewRepository.delete(contextId) } returns persistenceError.left()

        // When
        val result = handler(command)

        // Then
        result.shouldBeLeft()
        val error = result.leftOrNull()!!
        error.shouldBeInstanceOf<PersistenceError.StorageUnavailable>()
    }

    "should handle context not found error" {
        // Given
        val contextId = ContextViewId.generate()
        val command = DeleteContextView(id = contextId.value)

        coEvery { activeContextService.getCurrentContext() } returns null

        // Repository returns success when deleting non-existent item
        coEvery { contextViewRepository.delete(contextId) } returns Unit.right()

        // When
        val result = handler(command)

        // Then
        result.shouldBeRight()
        result.getOrElse { throw AssertionError("Failed to create value object") } shouldBe EmptyResult
    }

    "should compare context IDs correctly when checking active context" {
        // Given
        val contextId = ContextViewId.generate()
        val idString = contextId.value
        val command = DeleteContextView(id = idString)

        val activeContext = ContextView(
            id = contextId,  // Same ID value
            key = ContextViewKey.create("active").getOrElse { throw AssertionError("Failed to create value object") },
            name = ContextViewName.create("Active").getOrElse { throw AssertionError("Failed to create value object") },
            filter = ContextViewFilter.create("all").getOrElse { throw AssertionError("Failed to create value object") },
            description = null,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )

        coEvery { activeContextService.getCurrentContext() } returns activeContext

        // When
        val result = handler(command)

        // Then
        result.shouldBeLeft()
        val error = result.leftOrNull()!!
        error.shouldBeInstanceOf<ContextError.ActiveContextDeleteAttempt>()
    }
})


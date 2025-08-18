package io.github.kamiazya.scopes.application.usecase.handler

import arrow.core.left
import arrow.core.right
import arrow.core.getOrElse
import io.github.kamiazya.scopes.application.dto.ContextViewListResult
import io.github.kamiazya.scopes.application.error.*
import io.github.kamiazya.scopes.application.service.ActiveContextService
import io.github.kamiazya.scopes.application.test.MockLogger
import io.github.kamiazya.scopes.application.usecase.command.ListContextViewsQuery
import io.github.kamiazya.scopes.domain.entity.ContextView
import io.github.kamiazya.scopes.domain.error.PersistenceError as DomainPersistenceError
import io.github.kamiazya.scopes.domain.repository.ContextViewRepository
import io.github.kamiazya.scopes.domain.valueobject.ContextFilter
import io.github.kamiazya.scopes.domain.valueobject.ContextName
import io.github.kamiazya.scopes.domain.valueobject.ContextViewId
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.datetime.Clock

class ListContextViewsHandlerTest : StringSpec({

    val contextViewRepository = mockk<ContextViewRepository>()
    val activeContextService = mockk<ActiveContextService>()
    val logger = MockLogger()
    val handler = ListContextViewsHandler(contextViewRepository, activeContextService, logger)

    "should list all context views with no active context" {
        // Given
        val query = ListContextViewsQuery(includeInactive = true)
        val timestamp = Clock.System.now()

        val contextViews = listOf(
            ContextView(
                id = ContextViewId.generate(),
                name = ContextName.create("Context1").getOrElse { throw AssertionError("Failed to create value object") },
                filter = ContextFilter.create("status:open").getOrElse { throw AssertionError("Failed to create value object") },
                description = null,
                createdAt = timestamp,
                updatedAt = timestamp
            ),
            ContextView(
                id = ContextViewId.generate(),
                name = ContextName.create("Context2").getOrElse { throw AssertionError("Failed to create value object") },
                filter = ContextFilter.create("type:bug").getOrElse { throw AssertionError("Failed to create value object") },
                description = io.github.kamiazya.scopes.domain.valueobject.ContextDescription.create("Bug tracking").getOrElse { null },
                createdAt = timestamp,
                updatedAt = timestamp
            )
        )

        coEvery { contextViewRepository.findAll() } returns contextViews.right()
        coEvery { activeContextService.getCurrentContext() } returns null

        // When
        val result = handler(query)

        // Then
        result.shouldBeRight()
        val listResult = result.getOrElse { throw AssertionError("Failed to create value object") }
        listResult.shouldBeInstanceOf<ContextViewListResult>()
        listResult?.contexts?.shouldHaveSize(2)
        listResult?.activeContext.shouldBeNull()

        // All contexts should be marked as inactive
        listResult.contexts.forEach { context ->
            context.isActive shouldBe false
        }
    }

    "should list context views with active context marked" {
        // Given
        val query = ListContextViewsQuery(includeInactive = true)
        val timestamp = Clock.System.now()
        val activeContextId = ContextViewId.generate()

        val contextViews = listOf(
            ContextView(
                id = activeContextId,
                name = ContextName.create("ActiveContext").getOrElse { throw AssertionError("Failed to create value object") },
                filter = ContextFilter.create("status:active").getOrElse { throw AssertionError("Failed to create value object") },
                description = null,
                createdAt = timestamp,
                updatedAt = timestamp
            ),
            ContextView(
                id = ContextViewId.generate(),
                name = ContextName.create("InactiveContext").getOrElse { throw AssertionError("Failed to create value object") },
                filter = ContextFilter.create("status:done").getOrElse { throw AssertionError("Failed to create value object") },
                description = null,
                createdAt = timestamp,
                updatedAt = timestamp
            )
        )

        val activeContext = contextViews[0]

        coEvery { contextViewRepository.findAll() } returns contextViews.right()
        coEvery { activeContextService.getCurrentContext() } returns activeContext

        // When
        val result = handler(query)

        // Then
        result.shouldBeRight()
        val listResult = result.getOrElse { throw AssertionError("Failed to create value object") }
        listResult?.contexts?.shouldHaveSize(2)
        listResult?.activeContext.shouldNotBeNull()
        listResult?.activeContext?.name shouldBe "ActiveContext"

        // Check active status
        val activeInList = listResult?.contexts?.find { it.name == "ActiveContext" }
        activeInList?.isActive shouldBe true

        val inactiveInList = listResult?.contexts?.find { it.name == "InactiveContext" }
        inactiveInList?.isActive shouldBe false
    }

    "should exclude inactive information when includeInactive is false" {
        // Given
        val query = ListContextViewsQuery(includeInactive = false)
        val timestamp = Clock.System.now()

        val contextViews = listOf(
            ContextView(
                id = ContextViewId.generate(),
                name = ContextName.create("Context1").getOrElse { throw AssertionError("Failed to create value object") },
                filter = ContextFilter.create("all").getOrElse { throw AssertionError("Failed to create value object") },
                description = null,
                createdAt = timestamp,
                updatedAt = timestamp
            )
        )

        coEvery { contextViewRepository.findAll() } returns contextViews.right()
        // When includeInactive is false, activeContextService.getCurrentContext() should not be called

        // When
        val result = handler(query)

        // Then
        result.shouldBeRight()
        val listResult = result.getOrElse { throw AssertionError("Failed to create value object") }
        listResult?.contexts?.shouldHaveSize(1)
        listResult?.activeContext.shouldBeNull()
    }

    "should handle empty context list" {
        // Given
        val query = ListContextViewsQuery(includeInactive = true)

        coEvery { contextViewRepository.findAll() } returns emptyList<ContextView>().right()
        coEvery { activeContextService.getCurrentContext() } returns null

        // When
        val result = handler(query)

        // Then
        result.shouldBeRight()
        val listResult = result.getOrElse { throw AssertionError("Failed to create value object") }
        listResult?.contexts?.shouldHaveSize(0)
        listResult?.activeContext.shouldBeNull()
    }

    "should preserve all context properties in results" {
        // Given
        val query = ListContextViewsQuery(includeInactive = true)
        val createdTime = Clock.System.now()
        val updatedTime = Clock.System.now()

        val contextView = ContextView(
            id = ContextViewId.generate(),
            name = ContextName.create("DetailedContext").getOrElse { throw AssertionError("Failed to create value object") },
            filter = ContextFilter.create("complex:filter AND type:task").getOrElse { throw AssertionError("Failed to create value object") },
            description = io.github.kamiazya.scopes.domain.valueobject.ContextDescription.create("A detailed description").getOrElse { null },
            createdAt = createdTime,
            updatedAt = updatedTime
        )

        coEvery { contextViewRepository.findAll() } returns listOf(contextView).right()
        coEvery { activeContextService.getCurrentContext() } returns null

        // When
        val result = handler(query)

        // Then
        result.shouldBeRight()
        val listResult = result.getOrElse { throw AssertionError("Failed to create value object") }
        listResult?.contexts?.shouldHaveSize(1)

        val resultContext = listResult?.contexts?.get(0)
        resultContext?.name shouldBe "DetailedContext"
        resultContext?.filterExpression shouldBe "complex:filter AND type:task"
        resultContext?.description shouldBe "A detailed description"
        resultContext?.createdAt shouldBe createdTime
        resultContext?.updatedAt shouldBe updatedTime
    }

    "should handle repository errors" {
        // Given
        val query = ListContextViewsQuery(includeInactive = true)

        val persistenceError = DomainPersistenceError.StorageUnavailable(
            occurredAt = Clock.System.now(),
            operation = "findAll",
            cause = Exception("Database unavailable")
        )
        coEvery { contextViewRepository.findAll() } returns persistenceError.left()

        // When
        val result = handler(query)

        // Then
        result.shouldBeLeft()
        val error = result.leftOrNull()!!
        error.shouldBeInstanceOf<PersistenceError.StorageUnavailable>()
    }

    "should handle multiple contexts with mixed descriptions" {
        // Given
        val query = ListContextViewsQuery(includeInactive = true)
        val timestamp = Clock.System.now()

        val contextViews = listOf(
            ContextView(
                id = ContextViewId.generate(),
                name = ContextName.create("WithDescription").getOrElse { throw AssertionError("Failed to create value object") },
                filter = ContextFilter.create("has:description").getOrElse { throw AssertionError("Failed to create value object") },
                description = io.github.kamiazya.scopes.domain.valueobject.ContextDescription.create("Has description").getOrElse { null },
                createdAt = timestamp,
                updatedAt = timestamp
            ),
            ContextView(
                id = ContextViewId.generate(),
                name = ContextName.create("WithoutDescription").getOrElse { throw AssertionError("Failed to create value object") },
                filter = ContextFilter.create("no:description").getOrElse { throw AssertionError("Failed to create value object") },
                description = null,
                createdAt = timestamp,
                updatedAt = timestamp
            )
        )

        coEvery { contextViewRepository.findAll() } returns contextViews.right()
        coEvery { activeContextService.getCurrentContext() } returns null

        // When
        val result = handler(query)

        // Then
        result.shouldBeRight()
        val listResult = result.getOrElse { throw AssertionError("Failed to create value object") }
        listResult?.contexts?.shouldHaveSize(2)

        val withDesc = listResult?.contexts?.find { it.name == "WithDescription" }
        withDesc?.description shouldBe "Has description"

        val withoutDesc = listResult?.contexts?.find { it.name == "WithoutDescription" }
        withoutDesc?.description.shouldBeNull()
    }
})


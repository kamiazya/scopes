package io.github.kamiazya.scopes.application.usecase.handler

import arrow.core.left
import arrow.core.right
import arrow.core.getOrElse
import io.github.kamiazya.scopes.application.dto.ContextViewResult
import io.github.kamiazya.scopes.application.error.ApplicationError
import io.github.kamiazya.scopes.application.service.ActiveContextService
import io.github.kamiazya.scopes.application.usecase.command.SwitchContextView
import io.github.kamiazya.scopes.domain.entity.ContextView
import io.github.kamiazya.scopes.domain.valueobject.ContextFilter
import io.github.kamiazya.scopes.domain.valueobject.ContextName
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

class SwitchContextHandlerTest : StringSpec({

    val activeContextService = mockk<ActiveContextService>()
    val handler = SwitchContextHandler(activeContextService)

    "should switch to a valid context successfully" {
        // Given
        val command = SwitchContextView(name = "MyContext")
        val timestamp = Clock.System.now()

        val expectedContextView = ContextView(
            id = ContextViewId.generate(),
            name = ContextName.create("MyContext").getOrElse { throw AssertionError("Failed to create value object") },
            filter = ContextFilter.create("status:active").getOrElse { throw AssertionError("Failed to create value object") },
            description = io.github.kamiazya.scopes.domain.valueobject.ContextDescription.create("Test context").getOrElse { null },
            createdAt = timestamp,
            updatedAt = timestamp
        )

        coEvery { activeContextService.switchToContextByName("MyContext") } returns expectedContextView.right()

        // When
        val result = handler(command)

        // Then
        result.shouldBeRight()
        val contextViewResult = result.getOrElse { throw AssertionError("Failed to create value object") }
        contextViewResult.shouldBeInstanceOf<ContextViewResult>()
        contextViewResult.name shouldBe "MyContext"
        contextViewResult.isActive shouldBe true  // The switched context is now active

        coVerify(exactly = 1) { activeContextService.switchToContextByName("MyContext") }
    }

    "should return error when context does not exist" {
        // Given
        val command = SwitchContextView(name = "Non-existent Context")

        val error = ApplicationError.ContextError.StateNotFound(contextName = "Non-existent Context")
        coEvery { activeContextService.switchToContextByName("Non-existent Context") } returns error.left()

        // When
        val result = handler(command)

        // Then
        result.shouldBeLeft()
        val actualError = result.leftOrNull()!!
        actualError.shouldBeInstanceOf<ApplicationError.ContextError.StateNotFound>()
        (actualError as ApplicationError.ContextError.StateNotFound).contextName shouldBe "Non-existent Context"
    }

    "should handle empty context name" {
        // Given
        val command = SwitchContextView(name = "")

        val error = ApplicationError.ContextError.NamingInvalidFormat(attemptedName = "")
        coEvery { activeContextService.switchToContextByName("") } returns error.left()

        // When
        val result = handler(command)

        // Then
        result.shouldBeLeft()
        val actualError = result.leftOrNull()!!
        actualError.shouldBeInstanceOf<ApplicationError.ContextError.NamingInvalidFormat>()
    }

    "should preserve all context properties when switching" {
        // Given
        val command = SwitchContextView(name = "DetailedContext")
        val timestamp = Clock.System.now()

        val contextView = ContextView(
            id = ContextViewId.generate(),
            name = ContextName.create("DetailedContext").getOrElse { throw AssertionError("Failed to create value object") },
            filter = ContextFilter.create("type:bug AND priority:high").getOrElse { throw AssertionError("Failed to create value object") },
            description = io.github.kamiazya.scopes.domain.valueobject.ContextDescription.create("High priority bugs").getOrElse { null },
            createdAt = timestamp,
            updatedAt = timestamp
        )

        coEvery { activeContextService.switchToContextByName("DetailedContext") } returns contextView.right()

        // When
        val result = handler(command)

        // Then
        result.shouldBeRight()
        result.getOrNull()?.name shouldBe "DetailedContext"
        result.getOrNull()?.filterExpression shouldBe "type:bug AND priority:high"
        result.getOrNull()?.description shouldBe "High priority bugs"
        result.getOrNull()?.isActive shouldBe true
        result.getOrNull()?.createdAt shouldBe contextView.createdAt
        result.getOrNull()?.updatedAt shouldBe contextView.updatedAt
    }

    "should handle context without description" {
        // Given
        val command = SwitchContextView(name = "SimpleContext")
        val timestamp = Clock.System.now()

        val contextView = ContextView(
            id = ContextViewId.generate(),
            name = ContextName.create("SimpleContext").getOrElse { throw AssertionError("Failed to create value object") },
            filter = ContextFilter.create("status:open").getOrElse { throw AssertionError("Failed to create value object") },
            description = null,
            createdAt = timestamp,
            updatedAt = timestamp
        )

        coEvery { activeContextService.switchToContextByName("SimpleContext") } returns contextView.right()

        // When
        val result = handler(command)

        // Then
        result.shouldBeRight()
        result.getOrNull()?.description shouldBe null
    }

    "should handle service errors gracefully" {
        // Given
        val command = SwitchContextView(name = "Error Context")

        val error = ApplicationError.PersistenceError.StorageUnavailable(
            operation = "switchContext",
            cause = "Database connection lost"
        )
        coEvery { activeContextService.switchToContextByName("Error Context") } returns error.left()

        // When
        val result = handler(command)

        // Then
        result.shouldBeLeft()
        val actualError = result.leftOrNull()!!
        actualError.shouldBeInstanceOf<ApplicationError.PersistenceError.StorageUnavailable>()
    }
})

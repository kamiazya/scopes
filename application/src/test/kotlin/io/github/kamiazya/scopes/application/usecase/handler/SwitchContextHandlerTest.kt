package io.github.kamiazya.scopes.application.usecase.handler

import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.application.dto.ContextViewResult
import io.github.kamiazya.scopes.application.error.*
import io.github.kamiazya.scopes.application.service.ActiveContextService
import io.github.kamiazya.scopes.application.test.MockLogger
import io.github.kamiazya.scopes.application.usecase.command.SwitchContextView
import io.github.kamiazya.scopes.domain.entity.ContextView
import io.github.kamiazya.scopes.domain.valueobject.ContextViewDescription
import io.github.kamiazya.scopes.domain.valueobject.ContextViewFilter
import io.github.kamiazya.scopes.domain.valueobject.ContextViewId
import io.github.kamiazya.scopes.domain.valueobject.ContextViewKey
import io.github.kamiazya.scopes.domain.valueobject.ContextViewName
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.datetime.Clock

class SwitchContextHandlerTest :
    StringSpec({

        val activeContextService = mockk<ActiveContextService>()
        val logger = MockLogger()
        val handler = SwitchContextHandler(activeContextService, logger)

        "should switch to a valid context successfully" {
            // Given
            val command = SwitchContextView(key = "my-context")
            val timestamp = Clock.System.now()

            val expectedContextView = ContextView(
                id = ContextViewId.generate(),
                key = ContextViewKey.create("my-context").getOrElse {
                    throw AssertionError("Failed to create value object")
                },
                name = ContextViewName.create("MyContext").getOrElse {
                    throw AssertionError("Failed to create value object")
                },
                filter = ContextViewFilter.create("status:active").getOrElse {
                    throw AssertionError("Failed to create value object")
                },
                description = ContextViewDescription.create("Test context").getOrElse { null },
                createdAt = timestamp,
                updatedAt = timestamp,
            )

            coEvery { activeContextService.switchToContextByKey("my-context") } returns expectedContextView.right()

            // When
            val result = handler(command)

            // Then
            result.shouldBeRight()
            val contextViewResult = result.getOrElse { throw AssertionError("Failed to create value object") }
            contextViewResult.shouldBeInstanceOf<ContextViewResult>()
            contextViewResult.name shouldBe "MyContext"
            contextViewResult.isActive shouldBe true // The switched context is now active

            coVerify(exactly = 1) { activeContextService.switchToContextByKey("my-context") }
        }

        "should return error when context does not exist" {
            // Given
            val command = SwitchContextView(key = "non-existent-context")

            val error = ContextError.StateNotFound(contextId = "non-existent-context")
            coEvery { activeContextService.switchToContextByKey("non-existent-context") } returns error.left()

            // When
            val result = handler(command)

            // Then
            result.shouldBeLeft()
            val actualError = result.leftOrNull()!!
            actualError.shouldBeInstanceOf<ContextError.StateNotFound>()
            (actualError as ContextError.StateNotFound).contextId shouldBe "non-existent-context"
        }

        "should handle empty context name" {
            // Given
            val command = SwitchContextView(key = "")

            val error = ContextError.KeyInvalidFormat(attemptedKey = "")
            coEvery { activeContextService.switchToContextByKey("") } returns error.left()

            // When
            val result = handler(command)

            // Then
            result.shouldBeLeft()
            val actualError = result.leftOrNull()!!
            actualError.shouldBeInstanceOf<ContextError.KeyInvalidFormat>()
        }

        "should preserve all context properties when switching" {
            // Given
            val command = SwitchContextView(key = "detailed-context")
            val timestamp = Clock.System.now()

            val contextView = ContextView(
                id = ContextViewId.generate(),
                key = ContextViewKey.create("detailed-context").getOrElse {
                    throw AssertionError("Failed to create value object")
                },
                name = ContextViewName.create("DetailedContext").getOrElse {
                    throw AssertionError("Failed to create value object")
                },
                filter = ContextViewFilter.create("type:bug AND priority:high").getOrElse {
                    throw AssertionError("Failed to create value object")
                },
                description = ContextViewDescription.create("High priority bugs").getOrElse { null },
                createdAt = timestamp,
                updatedAt = timestamp,
            )

            coEvery { activeContextService.switchToContextByKey("detailed-context") } returns contextView.right()

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
            val command = SwitchContextView(key = "simple-context")
            val timestamp = Clock.System.now()

            val contextView = ContextView(
                id = ContextViewId.generate(),
                key = ContextViewKey.create("simple-context").getOrElse {
                    throw AssertionError("Failed to create value object")
                },
                name = ContextViewName.create("SimpleContext").getOrElse {
                    throw AssertionError("Failed to create value object")
                },
                filter = ContextViewFilter.create("status:open").getOrElse {
                    throw AssertionError("Failed to create value object")
                },
                description = null,
                createdAt = timestamp,
                updatedAt = timestamp,
            )

            coEvery { activeContextService.switchToContextByKey("simple-context") } returns contextView.right()

            // When
            val result = handler(command)

            // Then
            result.shouldBeRight()
            result.getOrNull()?.description shouldBe null
        }

        "should handle service errors gracefully" {
            // Given
            val command = SwitchContextView(key = "error-context")

            val error = PersistenceError.StorageUnavailable(
                operation = "switchContext",
                cause = "Database connection lost",
            )
            coEvery { activeContextService.switchToContextByKey("error-context") } returns error.left()

            // When
            val result = handler(command)

            // Then
            result.shouldBeLeft()
            val actualError = result.leftOrNull()!!
            actualError.shouldBeInstanceOf<PersistenceError.StorageUnavailable>()
        }
    })

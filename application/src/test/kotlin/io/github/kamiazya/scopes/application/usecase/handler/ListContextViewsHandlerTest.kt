package io.github.kamiazya.scopes.application.usecase.handler

import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.application.error.*
import io.github.kamiazya.scopes.application.service.ActiveContextService
import io.github.kamiazya.scopes.application.test.MockLogger
import io.github.kamiazya.scopes.application.usecase.command.ListContextViewsQuery
import io.github.kamiazya.scopes.domain.entity.ContextView
import io.github.kamiazya.scopes.domain.repository.ContextViewRepository
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
import io.github.kamiazya.scopes.domain.error.PersistenceError as DomainPersistenceError

class ListContextViewsHandlerTest :
    StringSpec({

        val contextViewRepository = mockk<ContextViewRepository>()
        val activeContextService = mockk<ActiveContextService>()
        val logger = MockLogger()
        val handler = ListContextViewsHandler(contextViewRepository, activeContextService, logger)

        "should list all context views without active context when includeInactive is false" {
            // Given
            val query = ListContextViewsQuery(includeInactive = false)

            val timestamp = Clock.System.now()

            val contextViews = listOf(
                ContextView(
                    id = ContextViewId.generate(),
                    key = ContextViewKey.create("context-1").getOrElse {
                        throw AssertionError("Failed to create value object")
                    },
                    name = ContextViewName.create("Context1").getOrElse {
                        throw AssertionError("Failed to create value object")
                    },
                    filter = ContextViewFilter.create("status:open").getOrElse {
                        throw AssertionError("Failed to create value object")
                    },
                    description = null,
                    createdAt = timestamp,
                    updatedAt = timestamp,
                ),
                ContextView(
                    id = ContextViewId.generate(),
                    key = ContextViewKey.create("context-2").getOrElse {
                        throw AssertionError("Failed to create value object")
                    },
                    name = ContextViewName.create("Context2").getOrElse {
                        throw AssertionError("Failed to create value object")
                    },
                    filter = ContextViewFilter.create("type:bug").getOrElse {
                        throw AssertionError("Failed to create value object")
                    },
                    description = ContextViewDescription.create("Bug tracking").getOrElse { null },
                    createdAt = timestamp,
                    updatedAt = timestamp,
                ),
            )

            coEvery { contextViewRepository.findAll() } returns contextViews.right()
            coEvery { activeContextService.getCurrentContext() } returns null

            // When
            val result = handler(query)

            // Then
            result.shouldBeRight()
            val listResult = result.getOrElse { throw AssertionError("Failed to create value object") }
            listResult.contexts.size shouldBe 2
            listResult.activeContext shouldBe null
            listResult.contexts[0].name shouldBe "Context1"
            listResult.contexts[1].name shouldBe "Context2"
            listResult.contexts.all { !it.isActive } shouldBe true

            coVerify(exactly = 0) { activeContextService.getCurrentContext() }
        }

        "should include active context information when includeInactive is true" {
            // Given
            val query = ListContextViewsQuery(includeInactive = true)

            val timestamp = Clock.System.now()
            val activeContextId = ContextViewId.generate()

            val contextViews = listOf(
                ContextView(
                    id = activeContextId,
                    key = ContextViewKey.create("active-context").getOrElse {
                        throw AssertionError("Failed to create value object")
                    },
                    name = ContextViewName.create("ActiveContext").getOrElse {
                        throw AssertionError("Failed to create value object")
                    },
                    filter = ContextViewFilter.create("status:active").getOrElse {
                        throw AssertionError("Failed to create value object")
                    },
                    description = null,
                    createdAt = timestamp,
                    updatedAt = timestamp,
                ),
                ContextView(
                    id = ContextViewId.generate(),
                    key = ContextViewKey.create("inactive-context").getOrElse {
                        throw AssertionError("Failed to create value object")
                    },
                    name = ContextViewName.create("InactiveContext").getOrElse {
                        throw AssertionError("Failed to create value object")
                    },
                    filter = ContextViewFilter.create("status:done").getOrElse {
                        throw AssertionError("Failed to create value object")
                    },
                    description = null,
                    createdAt = timestamp,
                    updatedAt = timestamp,
                ),
            )

            coEvery { contextViewRepository.findAll() } returns contextViews.right()
            coEvery { activeContextService.getCurrentContext() } returns contextViews[0]

            // When
            val result = handler(query)

            // Then
            result.shouldBeRight()
            val listResult = result.getOrElse { throw AssertionError("Failed to create value object") }
            listResult.contexts.size shouldBe 2
            listResult.activeContext?.id shouldBe activeContextId.value
            listResult.contexts.find { it.id == activeContextId.value }?.isActive shouldBe true
            listResult.contexts.find { it.id != activeContextId.value }?.isActive shouldBe false

            coVerify(exactly = 1) { activeContextService.getCurrentContext() }
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
            listResult.contexts shouldBe emptyList()
            listResult.activeContext shouldBe null
        }

        "should handle repository errors" {
            // Given
            val query = ListContextViewsQuery(includeInactive = false)

            val error = DomainPersistenceError.StorageUnavailable(
                occurredAt = Clock.System.now(),
                operation = "findAll",
                cause = Exception("Database connection lost"),
            )
            coEvery { contextViewRepository.findAll() } returns error.left()

            // When
            val result = handler(query)

            // Then
            result.shouldBeLeft()
            val actualError = result.leftOrNull()!!
            actualError.shouldBeInstanceOf<PersistenceError.StorageUnavailable>()
        }

        "should properly map context view properties" {
            // Given
            val query = ListContextViewsQuery(includeInactive = false)

            val timestamp = Clock.System.now()
            val contextId = ContextViewId.generate()

            val contextView = ContextView(
                id = contextId,
                key = ContextViewKey.create("detailed-context").getOrElse {
                    throw AssertionError("Failed to create value object")
                },
                name = ContextViewName.create("DetailedContext").getOrElse {
                    throw AssertionError("Failed to create value object")
                },
                filter = ContextViewFilter.create("type:task AND priority:high").getOrElse {
                    throw AssertionError("Failed to create value object")
                },
                description = ContextViewDescription.create("High priority tasks").getOrElse { null },
                createdAt = timestamp,
                updatedAt = timestamp,
            )

            coEvery { contextViewRepository.findAll() } returns listOf(contextView).right()

            // When
            val result = handler(query)

            // Then
            result.shouldBeRight()
            val listResult = result.getOrElse { throw AssertionError("Failed to create value object") }
            listResult.contexts.size shouldBe 1

            val contextResult = listResult.contexts[0]
            contextResult.id shouldBe contextId.value
            contextResult.key shouldBe "detailed-context"
            contextResult.name shouldBe "DetailedContext"
            contextResult.filterExpression shouldBe "type:task AND priority:high"
            contextResult.description shouldBe "High priority tasks"
            contextResult.createdAt shouldBe timestamp
            contextResult.updatedAt shouldBe timestamp
            contextResult.isActive shouldBe false
        }

        "should handle mixed contexts with and without descriptions" {
            // Given
            val query = ListContextViewsQuery(includeInactive = true)

            val timestamp = Clock.System.now()

            val contextViews = listOf(
                ContextView(
                    id = ContextViewId.generate(),
                    key = ContextViewKey.create("with-desc").getOrElse {
                        throw AssertionError("Failed to create value object")
                    },
                    name = ContextViewName.create("WithDescription").getOrElse {
                        throw AssertionError("Failed to create value object")
                    },
                    filter = ContextViewFilter.create("status:open").getOrElse {
                        throw AssertionError("Failed to create value object")
                    },
                    description = ContextViewDescription.create("This has a description").getOrElse { null },
                    createdAt = timestamp,
                    updatedAt = timestamp,
                ),
                ContextView(
                    id = ContextViewId.generate(),
                    key = ContextViewKey.create("without-desc").getOrElse {
                        throw AssertionError("Failed to create value object")
                    },
                    name = ContextViewName.create("WithoutDescription").getOrElse {
                        throw AssertionError("Failed to create value object")
                    },
                    filter = ContextViewFilter.create("type:bug").getOrElse {
                        throw AssertionError("Failed to create value object")
                    },
                    description = null,
                    createdAt = timestamp,
                    updatedAt = timestamp,
                ),
            )

            coEvery { contextViewRepository.findAll() } returns contextViews.right()
            coEvery { activeContextService.getCurrentContext() } returns null

            // When
            val result = handler(query)

            // Then
            result.shouldBeRight()
            val listResult = result.getOrElse { throw AssertionError("Failed to create value object") }
            listResult.contexts[0].description shouldBe "This has a description"
            listResult.contexts[1].description shouldBe null
        }
    })

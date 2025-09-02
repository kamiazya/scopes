package io.github.kamiazya.scopes.interfaces.cli.adapters

import arrow.core.right
import io.github.kamiazya.scopes.contracts.scopemanagement.context.ContextViewContract
import io.github.kamiazya.scopes.contracts.scopemanagement.context.CreateContextViewRequest
import io.github.kamiazya.scopes.scopemanagement.application.command.context.CreateContextViewUseCase
import io.github.kamiazya.scopes.scopemanagement.application.command.context.DeleteContextViewUseCase
import io.github.kamiazya.scopes.scopemanagement.application.command.context.UpdateContextViewUseCase
import io.github.kamiazya.scopes.scopemanagement.application.dto.ContextViewDto
import io.github.kamiazya.scopes.scopemanagement.application.query.context.GetContextViewUseCase
import io.github.kamiazya.scopes.scopemanagement.application.query.context.ListContextViewsUseCase
import io.github.kamiazya.scopes.scopemanagement.application.service.ActiveContextService
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import java.util.UUID

/**
 * Simple integration test for ContextCommandAdapter without the anti-pattern.
 */
class ContextCommandAdapterIntegrationTestSimple :
    DescribeSpec({
        describe("ContextCommandAdapter Integration Tests") {
            lateinit var createContextViewUseCase: CreateContextViewUseCase
            lateinit var listContextViewsUseCase: ListContextViewsUseCase
            lateinit var getContextViewUseCase: GetContextViewUseCase
            lateinit var updateContextViewUseCase: UpdateContextViewUseCase
            lateinit var deleteContextViewUseCase: DeleteContextViewUseCase
            lateinit var activeContextService: ActiveContextService
            lateinit var adapter: ContextCommandAdapter

            beforeEach {
                createContextViewUseCase = mockk()
                listContextViewsUseCase = mockk()
                getContextViewUseCase = mockk()
                updateContextViewUseCase = mockk()
                deleteContextViewUseCase = mockk()
                activeContextService = mockk()

                adapter = ContextCommandAdapter(
                    createContextViewUseCase = createContextViewUseCase,
                    listContextViewsUseCase = listContextViewsUseCase,
                    getContextViewUseCase = getContextViewUseCase,
                    updateContextViewUseCase = updateContextViewUseCase,
                    deleteContextViewUseCase = deleteContextViewUseCase,
                    activeContextService = activeContextService,
                )
            }

            describe("Context Creation") {
                it("should create context view successfully") {
                    runBlocking {
                        // Arrange
                        val request = CreateContextViewRequest(
                            key = "test-context",
                            name = "Test Context",
                            filter = "status=active",
                            description = "A test context",
                        )

                        val expectedContextView = ContextViewDto(
                            id = UUID.randomUUID().toString(),
                            key = request.key,
                            name = request.name,
                            filter = request.filter,
                            description = request.description,
                            createdAt = Instant.fromEpochMilliseconds(1000),
                            updatedAt = Instant.fromEpochMilliseconds(1000),
                        )

                        coEvery {
                            createContextViewUseCase.execute(any())
                        } returns expectedContextView.right()

                        // Act
                        val result = adapter.createContext(request)

                        // Assert
                        result.shouldBeInstanceOf<ContextViewContract.CreateContextViewResponse.Success>()
                        // Use when expression instead of casting
                        when (result) {
                            is ContextViewContract.CreateContextViewResponse.Success -> {
                                result.contextView.key shouldBe request.key
                                result.contextView.name shouldBe request.name
                            }
                            else -> error("Unexpected response type")
                        }
                    }
                }
            }
        }
    })

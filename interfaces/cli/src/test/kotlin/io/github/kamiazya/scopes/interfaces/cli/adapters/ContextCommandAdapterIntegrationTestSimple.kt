package io.github.kamiazya.scopes.interfaces.cli.adapters

import arrow.core.right
import io.github.kamiazya.scopes.contracts.scopemanagement.ContextViewCommandPort
import io.github.kamiazya.scopes.contracts.scopemanagement.context.CreateContextViewCommand
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking

/**
 * Simple integration test for ContextCommandAdapter without the anti-pattern.
 */
class ContextCommandAdapterIntegrationTestSimple :
    DescribeSpec({
        describe("ContextCommandAdapter Integration Tests") {
            lateinit var contextViewCommandPort: ContextViewCommandPort
            lateinit var adapter: ContextCommandAdapter

            beforeEach {
                contextViewCommandPort = mockk()

                adapter = ContextCommandAdapter(
                    contextViewCommandPort = contextViewCommandPort,
                )
            }

            describe("Context Creation") {
                it("should create context view successfully") {
                    runBlocking {
                        // Arrange
                        val request = CreateContextViewCommand(
                            key = "test-context",
                            name = "Test Context",
                            filter = "status=active",
                            description = "A test context",
                        )

                        coEvery {
                            contextViewCommandPort.createContextView(request)
                        } returns Unit.right()

                        // Act
                        val result = adapter.createContext(request)

                        // Assert
                        result.shouldBeRight()
                    }
                }
            }
        }
    })

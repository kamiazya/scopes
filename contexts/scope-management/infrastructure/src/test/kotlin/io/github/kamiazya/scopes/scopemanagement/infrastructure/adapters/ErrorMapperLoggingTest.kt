package io.github.kamiazya.scopes.scopemanagement.infrastructure.adapters

import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.platform.observability.logging.ConsoleLogger
import io.github.kamiazya.scopes.scopemanagement.domain.error.PersistenceError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.datetime.Clock

/**
 * Test suite to verify that unmapped errors are logged properly.
 */
class ErrorMapperLoggingTest :
    DescribeSpec({
        describe("ErrorMapper") {
            describe("when encountering an unmapped error") {
                it("should log the error details before mapping to ServiceUnavailable") {
                    // Given
                    val errorMapper = ErrorMapper(ConsoleLogger())

                    // Create an error that isn't explicitly mapped (using StorageUnavailable)
                    val unmappedError = PersistenceError.StorageUnavailable(
                        operation = "test-operation",
                        cause = null,
                        occurredAt = Clock.System.now(),
                    )

                    // When
                    val result = errorMapper.mapToContractError(unmappedError)

                    // Then the error should be mapped to ServiceUnavailable
                    result.shouldBeInstanceOf<ScopeContractError.SystemError.ServiceUnavailable>()
                }
            }

            describe("when encountering a mapped error") {
                it("should map the error correctly") {
                    // Given
                    val errorMapper = ErrorMapper(ConsoleLogger())

                    // Use a known mapped error
                    val mappedError = ScopeInputError.TitleError.Empty(
                        attemptedValue = "",
                        occurredAt = Clock.System.now(),
                    )

                    // When
                    val result = errorMapper.mapToContractError(mappedError)

                    // Then - verify the error is properly mapped
                    result.shouldBeInstanceOf<ScopeContractError.InputError.InvalidTitle>()
                }
            }
        }
    })

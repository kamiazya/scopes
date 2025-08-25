package io.github.kamiazya.scopes.scopemanagement.infrastructure.adapters

import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

/**
 * Test suite to verify that unmapped errors are logged properly.
 */
class ErrorMapperLoggingTest : DescribeSpec({
    describe("ErrorMapper") {
        describe("when encountering an unmapped error") {
            it("should log the error details before mapping to ServiceUnavailable") {
                // Given
                val mockLogger = mockk<Logger>(relaxed = true)
                val errorMapper = ErrorMapper(mockLogger)
                
                // Create a custom error that isn't mapped
                class UnmappedError(message: String) : ScopesError(message)
                val unmappedError = UnmappedError("This is an unmapped error")
                
                // When
                val result = errorMapper.mapToContractError(unmappedError)
                
                // Then
                verify {
                    mockLogger.error(
                        "Unmapped ScopesError encountered, mapping to ServiceUnavailable",
                        mapOf(
                            "errorClass" to "UnmappedError",
                            "errorMessage" to "This is an unmapped error",
                            "errorType" to withArg<String> { it.contains("UnmappedError") },
                        ),
                    )
                }
                
                // And the error should be mapped to ServiceUnavailable
                result.let {
                    it::class.simpleName shouldBe "ServiceUnavailable"
                }
            }
        }
        
        describe("when encountering a mapped error") {
            it("should NOT log the error (only unmapped errors are logged)") {
                // Given
                val mockLogger = mockk<Logger>(relaxed = true)
                val errorMapper = ErrorMapper(mockLogger)
                
                // Use a known mapped error
                val mappedError = io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.TitleError.Empty("")
                
                // When
                errorMapper.mapToContractError(mappedError)
                
                // Then - verify that error() was never called
                verify(exactly = 0) {
                    mockLogger.error(any(), any(), any())
                }
            }
        }
    }
})
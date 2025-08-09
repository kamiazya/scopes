package io.github.kamiazya.scopes.domain.service

import io.github.kamiazya.scopes.domain.error.DomainError
import io.github.kamiazya.scopes.domain.error.RecoveryResult
import io.github.kamiazya.scopes.domain.error.RecoveryStrategy
import io.github.kamiazya.scopes.domain.error.ScopeRecoveryConfiguration
import io.github.kamiazya.scopes.domain.error.ScopeTitleRecoveryConfig
import io.github.kamiazya.scopes.domain.error.ScopeDescriptionRecoveryConfig
import io.github.kamiazya.scopes.domain.error.ScopeDuplicationRecoveryConfig
import io.github.kamiazya.scopes.domain.error.ScopeHierarchyRecoveryConfig
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import io.kotest.matchers.types.shouldBeInstanceOf

class ErrorRecoverySuggestionServiceEdgeCaseTest : DescribeSpec({
    describe("ErrorRecoverySuggestionService edge cases") {
        describe("when maxLength is less than WORD_REDUCTION_FACTOR (10)") {
            it("should still generate non-empty suggestions") {
                // Create a configuration with maxLength = 5 (less than 10)
                val config = ScopeRecoveryConfiguration.Complete(
                    title = ScopeTitleRecoveryConfig(
                        defaultTemplate = "Task",
                        maxLength = 5,
                        truncationSuffix = "..."
                    ),
                    description = ScopeDescriptionRecoveryConfig(),
                    duplication = ScopeDuplicationRecoveryConfig(),
                    hierarchy = ScopeHierarchyRecoveryConfig()
                )

                val service = ErrorRecoverySuggestionService(config)

                val error = DomainError.ScopeValidationError.ScopeTitleTooLong(
                    maxLength = 5,
                    actualLength = 60
                )

                val context = mapOf(
                    "originalTitle" to "This is a very long title that exceeds the maximum length"
                )

                val result = service.suggestRecovery(error, context)

                result.shouldBeInstanceOf<RecoveryResult.Suggestion>()
                result.suggestedValues.shouldNotBeEmpty()
                result.suggestedValues.forEach { suggestion ->
                    (suggestion as String).shouldNotBeBlank()
                }
                result.strategy shouldBe RecoveryStrategy.TRUNCATE
            }

            it("should use default title when all suggestions would be empty") {
                val config = ScopeRecoveryConfiguration.Complete(
                    title = ScopeTitleRecoveryConfig(
                        defaultTemplate = "DefaultTask",
                        maxLength = 5,
                        truncationSuffix = "..."
                    ),
                    description = ScopeDescriptionRecoveryConfig(),
                    duplication = ScopeDuplicationRecoveryConfig(),
                    hierarchy = ScopeHierarchyRecoveryConfig()
                )

                val service = ErrorRecoverySuggestionService(config)

                val error = DomainError.ScopeValidationError.ScopeTitleTooLong(
                    maxLength = 5,
                    actualLength = 0
                )

                val context = mapOf(
                    "originalTitle" to ""
                )

                val result = service.suggestRecovery(error, context)

                result.shouldBeInstanceOf<RecoveryResult.Suggestion>()
                result.suggestedValues.shouldNotBeEmpty()
                result.suggestedValues.first() shouldBe "DefaultTask"
            }
        }
    }
})

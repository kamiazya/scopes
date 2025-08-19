package io.github.kamiazya.scopes.application.service

import io.github.kamiazya.scopes.application.service.error.CrossAggregateValidationError
import io.github.kamiazya.scopes.domain.repository.ScopeRepository
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.beInstanceOf
import io.mockk.mockk

/**
 * Focused test for distributed business rule validation edge cases.
 *
 * This test specifically targets the `validateDistributedBusinessRule` method
 * to ensure comprehensive coverage of the else branch and edge conditions.
 */
class CrossAggregateValidationServiceDistributedRulesTest : DescribeSpec({

    val mockScopeRepository = mockk<ScopeRepository>()
    val service = CrossAggregateValidationService(mockScopeRepository)

    describe("validateDistributedBusinessRule comprehensive coverage") {

        describe("known rule handling") {
            it("should handle 'eventualConsistency' rule successfully") {
                // Given
                val ruleName = "eventualConsistency"
                val aggregateStates = mapOf(
                    "aggregate1" to "pending",
                    "aggregate2" to "completed"
                )
                val operation = "saga_coordination"

                // When
                val result = service.validateDistributedBusinessRule(ruleName, aggregateStates, operation)

                // Then
                result.isRight() shouldBe true
            }

            it("should handle 'eventualConsistency' with empty aggregate states") {
                // Given
                val ruleName = "eventualConsistency"
                val emptyStates = emptyMap<String, Any>()
                val operation = "empty_saga"

                // When
                val result = service.validateDistributedBusinessRule(ruleName, emptyStates, operation)

                // Then
                result.isRight() shouldBe true
            }

            it("should handle 'eventualConsistency' with complex aggregate state values") {
                // Given
                val ruleName = "eventualConsistency"
                val complexStates = mapOf<String, Any>(
                    "user_aggregate" to mapOf("status" to "active", "version" to 3),
                    "order_aggregate" to listOf("item1", "item2", "item3"),
                    "payment_aggregate" to 1234.56,
                    "notification_aggregate" to "pending"
                )
                val operation = "complex_distributed_transaction"

                // When
                val result = service.validateDistributedBusinessRule(ruleName, complexStates, operation)

                // Then
                result.isRight() shouldBe true
            }
        }

        describe("unknown rule handling - else branch coverage") {
            it("should fail with InvariantViolation for completely unknown rule") {
                // Given
                val unknownRule = "nonExistentDistributedRule"
                val aggregateStates = mapOf("scope1" to "state1")
                val operation = "unknown_operation"

                // When
                val result = service.validateDistributedBusinessRule(unknownRule, aggregateStates, operation)

                // Then
                result.isLeft() shouldBe true
                val error = result.leftOrNull()!!
                error should beInstanceOf<CrossAggregateValidationError.InvariantViolation>()

                val invariantError = error as CrossAggregateValidationError.InvariantViolation
                invariantError.invariantName shouldBe unknownRule
                invariantError.aggregateIds shouldBe listOf("scope1")
                invariantError.violationDescription shouldBe "Unknown distributed business rule: $unknownRule"
            }

            it("should fail for rule with similar but incorrect name") {
                // Given
                val similarRule = "eventualconsistency" // lowercase, no camelCase
                val aggregateStates = mapOf("aggregate1" to "value1", "aggregate2" to "value2")
                val operation = "case_sensitive_operation"

                // When
                val result = service.validateDistributedBusinessRule(similarRule, aggregateStates, operation)

                // Then
                result.isLeft() shouldBe true
                val error = result.leftOrNull()!!
                error should beInstanceOf<CrossAggregateValidationError.InvariantViolation>()

                val invariantError = error as CrossAggregateValidationError.InvariantViolation
                invariantError.invariantName shouldBe similarRule
                invariantError.aggregateIds shouldBe listOf("aggregate1", "aggregate2")
                invariantError.violationDescription shouldBe "Unknown distributed business rule: $similarRule"
            }

            it("should fail for empty rule name") {
                // Given
                val emptyRule = ""
                val aggregateStates = mapOf("scope1" to "state1")
                val operation = "empty_rule_test"

                // When
                val result = service.validateDistributedBusinessRule(emptyRule, aggregateStates, operation)

                // Then
                result.isLeft() shouldBe true
                val error = result.leftOrNull()!!
                error should beInstanceOf<CrossAggregateValidationError.InvariantViolation>()

                val invariantError = error as CrossAggregateValidationError.InvariantViolation
                invariantError.invariantName shouldBe emptyRule
                invariantError.aggregateIds shouldBe listOf("scope1")
                invariantError.violationDescription shouldBe "Unknown distributed business rule: $emptyRule"
            }

            it("should fail for null-like rule name") {
                // Given
                val nullRule = "null"
                val aggregateStates = mapOf("test" to "value")
                val operation = "null_rule_test"

                // When
                val result = service.validateDistributedBusinessRule(nullRule, aggregateStates, operation)

                // Then
                result.isLeft() shouldBe true
                val error = result.leftOrNull()!!
                error should beInstanceOf<CrossAggregateValidationError.InvariantViolation>()

                val invariantError = error as CrossAggregateValidationError.InvariantViolation
                invariantError.invariantName shouldBe nullRule
                invariantError.violationDescription shouldBe "Unknown distributed business rule: $nullRule"
            }

            it("should fail for rule with special characters") {
                // Given
                val specialCharRule = "eventual@Consistency#Rule"
                val aggregateStates = mapOf("special" to "characters")
                val operation = "special_char_test"

                // When
                val result = service.validateDistributedBusinessRule(specialCharRule, aggregateStates, operation)

                // Then
                result.isLeft() shouldBe true
                val error = result.leftOrNull()!!
                error should beInstanceOf<CrossAggregateValidationError.InvariantViolation>()

                val invariantError = error as CrossAggregateValidationError.InvariantViolation
                invariantError.invariantName shouldBe specialCharRule
                invariantError.aggregateIds shouldBe listOf("special")
                invariantError.violationDescription shouldBe "Unknown distributed business rule: $specialCharRule"
            }
        }

        describe("aggregate states and operation parameter handling") {
            it("should preserve aggregate state keys order in error") {
                // Given
                val unknownRule = "testRule"
                val orderedStates = linkedMapOf(
                    "third" to "value3",
                    "first" to "value1",
                    "second" to "value2"
                )
                val operation = "order_test"

                // When
                val result = service.validateDistributedBusinessRule(unknownRule, orderedStates, operation)

                // Then
                result.isLeft() shouldBe true
                val error = result.leftOrNull()!!
                val invariantError = error as CrossAggregateValidationError.InvariantViolation

                // Should preserve the key order from the input map
                invariantError.aggregateIds shouldBe listOf("third", "first", "second")
            }

            it("should handle large number of aggregate states") {
                // Given
                val unknownRule = "massiveRule"
                val largeStates = (1..100).associate { "aggregate$it" to "state$it" }
                val operation = "large_state_test"

                // When
                val result = service.validateDistributedBusinessRule(unknownRule, largeStates, operation)

                // Then
                result.isLeft() shouldBe true
                val error = result.leftOrNull()!!
                val invariantError = error as CrossAggregateValidationError.InvariantViolation

                invariantError.aggregateIds.size shouldBe 100
                invariantError.aggregateIds shouldBe (1..100).map { "aggregate$it" }
            }

            it("should handle complex operation names") {
                // Given
                val unknownRule = "testRule"
                val aggregateStates = mapOf("simple" to "state")
                val complexOperation = "distributed_saga_with_compensation_rollback_phase_3"

                // When
                val result = service.validateDistributedBusinessRule(unknownRule, aggregateStates, complexOperation)

                // Then
                result.isLeft() shouldBe true
                val error = result.leftOrNull()!!
                // Operation name is not directly included in InvariantViolation,
                // but we verify the error structure is correct
                error should beInstanceOf<CrossAggregateValidationError.InvariantViolation>()
            }
        }

        describe("type safety and parameter validation") {
            it("should handle different aggregate state value types") {
                // Given
                val unknownRule = "typeTestRule"
                val mixedTypeStates = mapOf<String, Any>(
                    "string_aggregate" to "string_value",
                    "int_aggregate" to 42,
                    "double_aggregate" to 3.14159,
                    "boolean_aggregate" to true,
                    "list_aggregate" to listOf(1, 2, 3),
                    "map_aggregate" to mapOf("nested" to "value"),
                    "empty_string_aggregate" to ""
                )
                val operation = "mixed_types_test"

                // When
                val result = service.validateDistributedBusinessRule(unknownRule, mixedTypeStates, operation)

                // Then
                result.isLeft() shouldBe true
                val error = result.leftOrNull()!!
                val invariantError = error as CrossAggregateValidationError.InvariantViolation

                invariantError.aggregateIds.size shouldBe 7
                invariantError.aggregateIds.toSet() shouldBe setOf(
                    "string_aggregate", "int_aggregate", "double_aggregate",
                    "boolean_aggregate", "list_aggregate", "map_aggregate", "empty_string_aggregate"
                )
            }
        }
    }
})


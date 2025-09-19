package io.github.kamiazya.scopes.scopemanagement.domain.valueobject

import arrow.core.nonEmptyListOf
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for AspectCriteria and related classes.
 *
 * Business rules:
 * - AspectCriterion combines key, operator, and value for filtering
 * - AspectCriteria can be single or compound with logical operators
 * - Evaluation supports both single and multiple values per aspect
 * - Comparison operations work with numeric, boolean, and string values
 */
class AspectCriteriaTest :
    StringSpec({

        "should create AspectCriterion with basic data" {
            val key = AspectKey.create("priority").getOrNull()!!
            val value = AspectValue.create("high").getOrNull()!!
            val criterion = AspectCriterion(key, ComparisonOperator.EQUALS, value)

            criterion.key shouldBe key
            criterion.operator shouldBe ComparisonOperator.EQUALS
            criterion.value shouldBe value
        }

        "should create equality criterion using factory method" {
            val key = AspectKey.create("status").getOrNull()!!
            val value = AspectValue.create("completed").getOrNull()!!
            val criterion = AspectCriterion.equals(key, value)

            criterion.operator shouldBe ComparisonOperator.EQUALS
        }

        "should create not-equals criterion using factory method" {
            val key = AspectKey.create("status").getOrNull()!!
            val value = AspectValue.create("draft").getOrNull()!!
            val criterion = AspectCriterion.notEquals(key, value)

            criterion.operator shouldBe ComparisonOperator.NOT_EQUALS
        }

        "should create greater-than criterion using factory method" {
            val key = AspectKey.create("estimate").getOrNull()!!
            val value = AspectValue.create("5").getOrNull()!!
            val criterion = AspectCriterion.greaterThan(key, value)

            criterion.operator shouldBe ComparisonOperator.GREATER_THAN
        }

        "should create greater-than-or-equal criterion using factory method" {
            val key = AspectKey.create("estimate").getOrNull()!!
            val value = AspectValue.create("3").getOrNull()!!
            val criterion = AspectCriterion.greaterThanOrEqual(key, value)

            criterion.operator shouldBe ComparisonOperator.GREATER_THAN_OR_EQUAL
        }

        "should create less-than criterion using factory method" {
            val key = AspectKey.create("priority").getOrNull()!!
            val value = AspectValue.create("10").getOrNull()!!
            val criterion = AspectCriterion.lessThan(key, value)

            criterion.operator shouldBe ComparisonOperator.LESS_THAN
        }

        "should create less-than-or-equal criterion using factory method" {
            val key = AspectKey.create("priority").getOrNull()!!
            val value = AspectValue.create("5").getOrNull()!!
            val criterion = AspectCriterion.lessThanOrEqual(key, value)

            criterion.operator shouldBe ComparisonOperator.LESS_THAN_OR_EQUAL
        }

        "should create contains criterion using factory method" {
            val key = AspectKey.create("description").getOrNull()!!
            val value = AspectValue.create("urgent").getOrNull()!!
            val criterion = AspectCriterion.contains(key, value)

            criterion.operator shouldBe ComparisonOperator.CONTAINS
        }

        "should create not-contains criterion using factory method" {
            val key = AspectKey.create("tags").getOrNull()!!
            val value = AspectValue.create("deprecated").getOrNull()!!
            val criterion = AspectCriterion.notContains(key, value)

            criterion.operator shouldBe ComparisonOperator.NOT_CONTAINS
        }

        "should evaluate equals operation correctly" {
            val key = AspectKey.create("status").getOrNull()!!
            val value = AspectValue.create("done").getOrNull()!!
            val criterion = AspectCriterion.equals(key, value)

            criterion.evaluate(AspectValue.create("done").getOrNull()) shouldBe true
            criterion.evaluate(AspectValue.create("pending").getOrNull()) shouldBe false
            criterion.evaluate(null) shouldBe false
        }

        "should evaluate not-equals operation correctly" {
            val key = AspectKey.create("status").getOrNull()!!
            val value = AspectValue.create("draft").getOrNull()!!
            val criterion = AspectCriterion.notEquals(key, value)

            criterion.evaluate(AspectValue.create("done").getOrNull()) shouldBe true
            criterion.evaluate(AspectValue.create("draft").getOrNull()) shouldBe false
            criterion.evaluate(null) shouldBe false
        }

        "should evaluate contains operation correctly" {
            val key = AspectKey.create("description").getOrNull()!!
            val value = AspectValue.create("bug").getOrNull()!!
            val criterion = AspectCriterion.contains(key, value)

            criterion.evaluate(AspectValue.create("critical bug fix").getOrNull()) shouldBe true
            criterion.evaluate(AspectValue.create("Bug in authentication").getOrNull()) shouldBe true // case insensitive
            criterion.evaluate(AspectValue.create("new feature").getOrNull()) shouldBe false
            criterion.evaluate(null) shouldBe false
        }

        "should evaluate not-contains operation correctly" {
            val key = AspectKey.create("tags").getOrNull()!!
            val value = AspectValue.create("legacy").getOrNull()!!
            val criterion = AspectCriterion.notContains(key, value)

            criterion.evaluate(AspectValue.create("modern feature").getOrNull()) shouldBe true
            criterion.evaluate(AspectValue.create("legacy system").getOrNull()) shouldBe false
            criterion.evaluate(AspectValue.create("Legacy Code").getOrNull()) shouldBe false // case insensitive
            criterion.evaluate(null) shouldBe false
        }

        "should evaluate numeric comparisons correctly" {
            val key = AspectKey.create("estimate").getOrNull()!!
            val targetValue = AspectValue.create("5").getOrNull()!!

            val gtCriterion = AspectCriterion.greaterThan(key, targetValue)
            val gteCriterion = AspectCriterion.greaterThanOrEqual(key, targetValue)
            val ltCriterion = AspectCriterion.lessThan(key, targetValue)
            val lteCriterion = AspectCriterion.lessThanOrEqual(key, targetValue)

            gtCriterion.evaluate(AspectValue.create("8").getOrNull()) shouldBe true
            gtCriterion.evaluate(AspectValue.create("5").getOrNull()) shouldBe false
            gtCriterion.evaluate(AspectValue.create("3").getOrNull()) shouldBe false

            gteCriterion.evaluate(AspectValue.create("8").getOrNull()) shouldBe true
            gteCriterion.evaluate(AspectValue.create("5").getOrNull()) shouldBe true
            gteCriterion.evaluate(AspectValue.create("3").getOrNull()) shouldBe false

            ltCriterion.evaluate(AspectValue.create("3").getOrNull()) shouldBe true
            ltCriterion.evaluate(AspectValue.create("5").getOrNull()) shouldBe false
            ltCriterion.evaluate(AspectValue.create("8").getOrNull()) shouldBe false

            lteCriterion.evaluate(AspectValue.create("3").getOrNull()) shouldBe true
            lteCriterion.evaluate(AspectValue.create("5").getOrNull()) shouldBe true
            lteCriterion.evaluate(AspectValue.create("8").getOrNull()) shouldBe false
        }

        "should evaluate multiple values with OR logic for positive operations" {
            val key = AspectKey.create("priority").getOrNull()!!
            val value = AspectValue.create("high").getOrNull()!!
            val criterion = AspectCriterion.equals(key, value)

            val values = listOf(
                AspectValue.create("low").getOrNull()!!,
                AspectValue.create("high").getOrNull()!!,
                AspectValue.create("medium").getOrNull()!!,
            )

            criterion.evaluateMultiple(values) shouldBe true // ANY value equals "high"

            val noMatchValues = listOf(
                AspectValue.create("low").getOrNull()!!,
                AspectValue.create("medium").getOrNull()!!,
            )

            criterion.evaluateMultiple(noMatchValues) shouldBe false
            criterion.evaluateMultiple(null) shouldBe false
            criterion.evaluateMultiple(emptyList()) shouldBe false
        }

        "should evaluate multiple values with AND logic for negative operations" {
            val key = AspectKey.create("tags").getOrNull()!!
            val value = AspectValue.create("deprecated").getOrNull()!!
            val criterion = AspectCriterion.notEquals(key, value)

            val allDifferentValues = listOf(
                AspectValue.create("current").getOrNull()!!,
                AspectValue.create("active").getOrNull()!!,
                AspectValue.create("new").getOrNull()!!,
            )

            criterion.evaluateMultiple(allDifferentValues) shouldBe true // ALL values don't equal "deprecated"

            val someMatchValues = listOf(
                AspectValue.create("current").getOrNull()!!,
                AspectValue.create("deprecated").getOrNull()!!,
                AspectValue.create("new").getOrNull()!!,
            )

            criterion.evaluateMultiple(someMatchValues) shouldBe false // NOT ALL values are different
        }

        "should create single AspectCriteria from criterion" {
            val key = AspectKey.create("status").getOrNull()!!
            val value = AspectValue.create("active").getOrNull()!!
            val criterion = AspectCriterion.equals(key, value)
            val criteria = AspectCriteria.from(criterion)

            criteria.shouldBeInstanceOf<AspectCriteria.Single>()
            criteria.criterion shouldBe criterion
        }

        "should create compound AspectCriteria with AND operator" {
            val key1 = AspectKey.create("status").getOrNull()!!
            val value1 = AspectValue.create("active").getOrNull()!!
            val criterion1 = AspectCriterion.equals(key1, value1)
            val criteria1 = AspectCriteria.from(criterion1)

            val key2 = AspectKey.create("priority").getOrNull()!!
            val value2 = AspectValue.create("high").getOrNull()!!
            val criterion2 = AspectCriterion.equals(key2, value2)
            val criteria2 = AspectCriteria.from(criterion2)

            val compound = AspectCriteria.and(criteria1, criteria2)

            compound.shouldBeInstanceOf<AspectCriteria.Compound>()
            compound.left shouldBe criteria1
            compound.operator shouldBe LogicalOperator.AND
            compound.right shouldBe criteria2
        }

        "should create compound AspectCriteria with OR operator" {
            val key1 = AspectKey.create("status").getOrNull()!!
            val value1 = AspectValue.create("urgent").getOrNull()!!
            val criterion1 = AspectCriterion.equals(key1, value1)
            val criteria1 = AspectCriteria.from(criterion1)

            val key2 = AspectKey.create("priority").getOrNull()!!
            val value2 = AspectValue.create("critical").getOrNull()!!
            val criterion2 = AspectCriterion.equals(key2, value2)
            val criteria2 = AspectCriteria.from(criterion2)

            val compound = AspectCriteria.or(criteria1, criteria2)

            compound.shouldBeInstanceOf<AspectCriteria.Compound>()
            compound.left shouldBe criteria1
            compound.operator shouldBe LogicalOperator.OR
            compound.right shouldBe criteria2
        }

        "should evaluate single criteria against aspect map" {
            val key = AspectKey.create("status").getOrNull()!!
            val value = AspectValue.create("done").getOrNull()!!
            val criterion = AspectCriterion.equals(key, value)
            val criteria = AspectCriteria.from(criterion)

            val aspects = mapOf(
                key to AspectValue.create("done").getOrNull()!!,
            )

            criteria.evaluate(aspects) shouldBe true

            val differentAspects = mapOf(
                key to AspectValue.create("pending").getOrNull()!!,
            )

            criteria.evaluate(differentAspects) shouldBe false

            criteria.evaluate(emptyMap()) shouldBe false
        }

        "should evaluate compound AND criteria correctly" {
            val statusKey = AspectKey.create("status").getOrNull()!!
            val priorityKey = AspectKey.create("priority").getOrNull()!!

            val statusCriteria = AspectCriteria.from(AspectCriterion.equals(statusKey, AspectValue.create("active").getOrNull()!!))
            val priorityCriteria = AspectCriteria.from(AspectCriterion.equals(priorityKey, AspectValue.create("high").getOrNull()!!))

            val andCriteria = AspectCriteria.and(statusCriteria, priorityCriteria)

            val bothMatch = mapOf(
                statusKey to AspectValue.create("active").getOrNull()!!,
                priorityKey to AspectValue.create("high").getOrNull()!!,
            )

            val onlyStatusMatch = mapOf(
                statusKey to AspectValue.create("active").getOrNull()!!,
                priorityKey to AspectValue.create("low").getOrNull()!!,
            )

            val onlyPriorityMatch = mapOf(
                statusKey to AspectValue.create("inactive").getOrNull()!!,
                priorityKey to AspectValue.create("high").getOrNull()!!,
            )

            andCriteria.evaluate(bothMatch) shouldBe true
            andCriteria.evaluate(onlyStatusMatch) shouldBe false
            andCriteria.evaluate(onlyPriorityMatch) shouldBe false
            andCriteria.evaluate(emptyMap()) shouldBe false
        }

        "should evaluate compound OR criteria correctly" {
            val statusKey = AspectKey.create("status").getOrNull()!!
            val priorityKey = AspectKey.create("priority").getOrNull()!!

            val statusCriteria = AspectCriteria.from(AspectCriterion.equals(statusKey, AspectValue.create("urgent").getOrNull()!!))
            val priorityCriteria = AspectCriteria.from(AspectCriterion.equals(priorityKey, AspectValue.create("critical").getOrNull()!!))

            val orCriteria = AspectCriteria.or(statusCriteria, priorityCriteria)

            val bothMatch = mapOf(
                statusKey to AspectValue.create("urgent").getOrNull()!!,
                priorityKey to AspectValue.create("critical").getOrNull()!!,
            )

            val onlyStatusMatch = mapOf(
                statusKey to AspectValue.create("urgent").getOrNull()!!,
                priorityKey to AspectValue.create("low").getOrNull()!!,
            )

            val onlyPriorityMatch = mapOf(
                statusKey to AspectValue.create("normal").getOrNull()!!,
                priorityKey to AspectValue.create("critical").getOrNull()!!,
            )

            val neitherMatch = mapOf(
                statusKey to AspectValue.create("normal").getOrNull()!!,
                priorityKey to AspectValue.create("low").getOrNull()!!,
            )

            orCriteria.evaluate(bothMatch) shouldBe true
            orCriteria.evaluate(onlyStatusMatch) shouldBe true
            orCriteria.evaluate(onlyPriorityMatch) shouldBe true
            orCriteria.evaluate(neitherMatch) shouldBe false
            orCriteria.evaluate(emptyMap()) shouldBe false
        }

        "should evaluate criteria with multiple values using NonEmptyList" {
            val key = AspectKey.create("tags").getOrNull()!!
            val value = AspectValue.create("important").getOrNull()!!
            val criterion = AspectCriterion.equals(key, value)
            val criteria = AspectCriteria.from(criterion)

            val multipleAspects = mapOf(
                key to nonEmptyListOf(
                    AspectValue.create("important").getOrNull()!!,
                    AspectValue.create("urgent").getOrNull()!!,
                    AspectValue.create("feature").getOrNull()!!,
                ),
            )

            criteria.evaluateWithMultipleValues(multipleAspects) shouldBe true

            val noMatchingAspects = mapOf(
                key to nonEmptyListOf(
                    AspectValue.create("minor").getOrNull()!!,
                    AspectValue.create("enhancement").getOrNull()!!,
                ),
            )

            criteria.evaluateWithMultipleValues(noMatchingAspects) shouldBe false
            criteria.evaluateWithMultipleValues(emptyMap()) shouldBe false
        }

        "should evaluate criteria using Aspects value object" {
            val key = AspectKey.create("category").getOrNull()!!
            val value = AspectValue.create("bug").getOrNull()!!
            val criterion = AspectCriterion.equals(key, value)
            val criteria = AspectCriteria.from(criterion)

            val aspects = Aspects.empty()
                .add(key, value)
                .add(key, AspectValue.create("feature").getOrNull()!!)

            criteria.evaluateWithAspects(aspects) shouldBe true

            val aspectsWithoutMatch = Aspects.empty()
                .add(key, AspectValue.create("enhancement").getOrNull()!!)
                .add(key, AspectValue.create("documentation").getOrNull()!!)

            criteria.evaluateWithAspects(aspectsWithoutMatch) shouldBe false
        }

        "should handle edge cases gracefully" {
            val key = AspectKey.create("empty").getOrNull()!!
            val value = AspectValue.create("test").getOrNull()!!
            val criterion = AspectCriterion.equals(key, value)

            // Test with null and empty values
            criterion.evaluate(null) shouldBe false
            criterion.evaluateMultiple(null) shouldBe false
            criterion.evaluateMultiple(emptyList()) shouldBe false

            // Test with wrong key in map
            val wrongKeyMap = mapOf(
                AspectKey.create("different").getOrNull()!! to AspectValue.create("test").getOrNull()!!,
            )

            val criteria = AspectCriteria.from(criterion)
            criteria.evaluate(wrongKeyMap) shouldBe false
        }

        "should work with complex nested criteria" {
            val statusKey = AspectKey.create("status").getOrNull()!!
            val priorityKey = AspectKey.create("priority").getOrNull()!!
            val typeKey = AspectKey.create("type").getOrNull()!!

            // (status = active AND priority = high) OR type = urgent
            val statusCriteria = AspectCriteria.from(AspectCriterion.equals(statusKey, AspectValue.create("active").getOrNull()!!))
            val priorityCriteria = AspectCriteria.from(AspectCriterion.equals(priorityKey, AspectValue.create("high").getOrNull()!!))
            val typeCriteria = AspectCriteria.from(AspectCriterion.equals(typeKey, AspectValue.create("urgent").getOrNull()!!))

            val leftAnd = AspectCriteria.and(statusCriteria, priorityCriteria)
            val complexCriteria = AspectCriteria.or(leftAnd, typeCriteria)

            val matchingLeftSide = mapOf(
                statusKey to AspectValue.create("active").getOrNull()!!,
                priorityKey to AspectValue.create("high").getOrNull()!!,
                typeKey to AspectValue.create("normal").getOrNull()!!,
            )

            val matchingRightSide = mapOf(
                statusKey to AspectValue.create("inactive").getOrNull()!!,
                priorityKey to AspectValue.create("low").getOrNull()!!,
                typeKey to AspectValue.create("urgent").getOrNull()!!,
            )

            val matchingBothSides = mapOf(
                statusKey to AspectValue.create("active").getOrNull()!!,
                priorityKey to AspectValue.create("high").getOrNull()!!,
                typeKey to AspectValue.create("urgent").getOrNull()!!,
            )

            val matchingNeither = mapOf(
                statusKey to AspectValue.create("inactive").getOrNull()!!,
                priorityKey to AspectValue.create("low").getOrNull()!!,
                typeKey to AspectValue.create("normal").getOrNull()!!,
            )

            complexCriteria.evaluate(matchingLeftSide) shouldBe true
            complexCriteria.evaluate(matchingRightSide) shouldBe true
            complexCriteria.evaluate(matchingBothSides) shouldBe true
            complexCriteria.evaluate(matchingNeither) shouldBe false
        }
    })

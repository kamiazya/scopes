package io.github.kamiazya.scopes.scopemanagement.domain.valueobject

import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for AspectType sealed class hierarchy.
 *
 * Business rules:
 * - Five types: Text, Numeric, BooleanType, Ordered, Duration
 * - Ordered type contains a list of allowed values
 * - All other types are singletons (data objects)
 */
class AspectTypeTest :
    StringSpec({

        "should create Text aspect type" {
            val textType = AspectType.Text

            textType.shouldBeInstanceOf<AspectType.Text>()
            textType.toString() shouldBe "Text"
        }

        "should create Numeric aspect type" {
            val numericType = AspectType.Numeric

            numericType.shouldBeInstanceOf<AspectType.Numeric>()
            numericType.toString() shouldBe "Numeric"
        }

        "should create BooleanType aspect type" {
            val booleanType = AspectType.BooleanType

            booleanType.shouldBeInstanceOf<AspectType.BooleanType>()
            booleanType.toString() shouldBe "BooleanType"
        }

        "should create Duration aspect type" {
            val durationType = AspectType.Duration

            durationType.shouldBeInstanceOf<AspectType.Duration>()
            durationType.toString() shouldBe "Duration"
        }

        "should create Ordered aspect type with allowed values" {
            val lowValue = AspectValue.create("low").shouldBeRight()
            val mediumValue = AspectValue.create("medium").shouldBeRight()
            val highValue = AspectValue.create("high").shouldBeRight()
            val allowedValues = listOf(lowValue, mediumValue, highValue)

            val orderedType = AspectType.Ordered(allowedValues)

            orderedType.shouldBeInstanceOf<AspectType.Ordered>()
            orderedType.allowedValues shouldBe allowedValues
            orderedType.allowedValues.size shouldBe 3
        }

        "should create Ordered aspect type with empty allowed values" {
            val orderedType = AspectType.Ordered(emptyList<AspectValue>())

            orderedType.shouldBeInstanceOf<AspectType.Ordered>()
            orderedType.allowedValues shouldBe emptyList()
        }

        "should create Ordered aspect type with single value" {
            val singleValue = AspectValue.create("only").shouldBeRight()
            val orderedType = AspectType.Ordered(listOf(singleValue))

            orderedType.shouldBeInstanceOf<AspectType.Ordered>()
            orderedType.allowedValues.size shouldBe 1
            orderedType.allowedValues.first() shouldBe singleValue
        }

        "should maintain singleton behavior for data objects" {
            // Text instances should be the same
            val text1 = AspectType.Text
            val text2 = AspectType.Text
            (text1 === text2) shouldBe true

            // Numeric instances should be the same
            val numeric1 = AspectType.Numeric
            val numeric2 = AspectType.Numeric
            (numeric1 === numeric2) shouldBe true

            // BooleanType instances should be the same
            val boolean1 = AspectType.BooleanType
            val boolean2 = AspectType.BooleanType
            (boolean1 === boolean2) shouldBe true

            // Duration instances should be the same
            val duration1 = AspectType.Duration
            val duration2 = AspectType.Duration
            (duration1 === duration2) shouldBe true
        }

        "should differentiate between different aspect types" {
            val textType = AspectType.Text
            val numericType = AspectType.Numeric
            val booleanType = AspectType.BooleanType
            val durationType = AspectType.Duration
            val orderedType = AspectType.Ordered(emptyList())

            textType shouldNotBe numericType
            textType shouldNotBe booleanType
            textType shouldNotBe durationType
            textType shouldNotBe orderedType

            numericType shouldNotBe booleanType
            numericType shouldNotBe durationType
            numericType shouldNotBe orderedType

            booleanType shouldNotBe durationType
            booleanType shouldNotBe orderedType

            durationType shouldNotBe orderedType
        }

        "should handle Ordered type equality correctly" {
            val value1 = AspectValue.create("low").shouldBeRight()
            val value2 = AspectValue.create("high").shouldBeRight()

            val ordered1 = AspectType.Ordered(listOf(value1, value2))
            val ordered2 = AspectType.Ordered(listOf(value1, value2))
            val ordered3 = AspectType.Ordered(listOf(value2, value1)) // Different order
            val ordered4 = AspectType.Ordered(listOf(value1)) // Different values

            ordered1 shouldBe ordered2
            ordered1.hashCode() shouldBe ordered2.hashCode()

            ordered1 shouldNotBe ordered3
            ordered1 shouldNotBe ordered4
        }

        "should create Ordered type with different AspectValue types" {
            val textValue = AspectValue.create("text").shouldBeRight()
            val numericValue = AspectValue.create("42.0").shouldBeRight()
            val booleanValue = AspectValue.create("true").shouldBeRight()
            val durationValue = AspectValue.create("PT1H").shouldBeRight()

            val mixedOrderedType = AspectType.Ordered(
                listOf(
                    textValue,
                    numericValue,
                    booleanValue,
                    durationValue,
                ),
            )

            mixedOrderedType.shouldBeInstanceOf<AspectType.Ordered>()
            mixedOrderedType.allowedValues.size shouldBe 4
            mixedOrderedType.allowedValues[0] shouldBe textValue
            mixedOrderedType.allowedValues[1] shouldBe numericValue
            mixedOrderedType.allowedValues[2] shouldBe booleanValue
            mixedOrderedType.allowedValues[3] shouldBe durationValue
        }

        "should handle realistic aspect type scenarios" {
            // Priority aspect type
            val priorityValues = listOf(
                AspectValue.create("low").shouldBeRight(),
                AspectValue.create("medium").shouldBeRight(),
                AspectValue.create("high").shouldBeRight(),
                AspectValue.create("critical").shouldBeRight(),
            )
            val priorityType = AspectType.Ordered(priorityValues)

            priorityType.allowedValues.size shouldBe 4

            // Status aspect type (often just text)
            val statusType = AspectType.Text
            statusType.shouldBeInstanceOf<AspectType.Text>()

            // Estimate aspect type (numeric)
            val estimateType = AspectType.Numeric
            estimateType.shouldBeInstanceOf<AspectType.Numeric>()

            // Completed aspect type (boolean)
            val completedType = AspectType.BooleanType
            completedType.shouldBeInstanceOf<AspectType.BooleanType>()

            // Time spent aspect type (duration)
            val timeSpentType = AspectType.Duration
            timeSpentType.shouldBeInstanceOf<AspectType.Duration>()
        }

        "should support complex ordered value sequences" {
            // Size ordering
            val sizeValues = listOf(
                AspectValue.create("xs").shouldBeRight(),
                AspectValue.create("s").shouldBeRight(),
                AspectValue.create("m").shouldBeRight(),
                AspectValue.create("l").shouldBeRight(),
                AspectValue.create("xl").shouldBeRight(),
                AspectValue.create("xxl").shouldBeRight(),
            )
            val sizeType = AspectType.Ordered(sizeValues)

            sizeType.allowedValues.size shouldBe 6
            sizeType.allowedValues.first() shouldBe AspectValue.create("xs").shouldBeRight()
            sizeType.allowedValues.last() shouldBe AspectValue.create("xxl").shouldBeRight()

            // Numeric scale ordering
            val scaleValues = listOf(
                AspectValue.create("1.0").shouldBeRight(),
                AspectValue.create("2.0").shouldBeRight(),
                AspectValue.create("3.0").shouldBeRight(),
                AspectValue.create("5.0").shouldBeRight(),
                AspectValue.create("8.0").shouldBeRight(),
                AspectValue.create("13.0").shouldBeRight(),
            )
            val scaleType = AspectType.Ordered(scaleValues)

            scaleType.allowedValues.size shouldBe 6
            scaleType.allowedValues.first() shouldBe AspectValue.create("1.0").shouldBeRight()
            scaleType.allowedValues.last() shouldBe AspectValue.create("13.0").shouldBeRight()
        }

        "should handle edge cases for Ordered type" {
            // Very large list
            val manyValues = (1..100).map { AspectValue.create(it.toString()).shouldBeRight() }
            val manyOrderedType = AspectType.Ordered(manyValues)

            manyOrderedType.allowedValues.size shouldBe 100
            manyOrderedType.allowedValues.first() shouldBe AspectValue.create("1").shouldBeRight()
            manyOrderedType.allowedValues.last() shouldBe AspectValue.create("100").shouldBeRight()

            // Duplicate values (should be preserved if provided)
            val duplicateValues = listOf(
                AspectValue.create("same").shouldBeRight(),
                AspectValue.create("same").shouldBeRight(),
                AspectValue.create("different").shouldBeRight(),
            )
            val duplicateOrderedType = AspectType.Ordered(duplicateValues)

            duplicateOrderedType.allowedValues.size shouldBe 3
        }

        "should maintain immutability for Ordered type" {
            val originalValues = listOf(
                AspectValue.create("a").shouldBeRight(),
                AspectValue.create("b").shouldBeRight(),
            )
            val orderedType = AspectType.Ordered(originalValues)

            // Getting the allowed values should not allow modification
            val retrievedValues = orderedType.allowedValues
            retrievedValues.shouldBeInstanceOf<List<AspectValue>>()
            retrievedValues.size shouldBe 2
        }

        "should handle when expressions for type checking" {
            val types = listOf(
                AspectType.Text,
                AspectType.Numeric,
                AspectType.BooleanType,
                AspectType.Duration,
                AspectType.Ordered(listOf(AspectValue.create("test").shouldBeRight())),
            )

            types.forEach { type ->
                val result = when (type) {
                    is AspectType.Text -> "text"
                    is AspectType.Numeric -> "numeric"
                    is AspectType.BooleanType -> "boolean"
                    is AspectType.Duration -> "duration"
                    is AspectType.Ordered -> "ordered"
                }

                result shouldNotBe ""
            }
        }
    })

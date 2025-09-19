package io.github.kamiazya.scopes.scopemanagement.domain.valueobject

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for FilterExpressionAST sealed class.
 *
 * Business rules:
 * - AST nodes represent parsed filter expressions
 * - Each node type has specific properties and semantics
 * - AST structure supports composition and recursion
 * - All node types are immutable value objects
 */
class FilterExpressionASTTest :
    StringSpec({

        "should create Comparison node with key, operator, and value" {
            val comparison = FilterExpressionAST.Comparison("status", ComparisonOperator.EQUALS, "active")

            comparison.key shouldBe "status"
            comparison.operator shouldBe ComparisonOperator.EQUALS
            comparison.value shouldBe "active"
            comparison.shouldBeInstanceOf<FilterExpressionAST.Comparison>()
        }

        "should create And node with left and right expressions" {
            val left = FilterExpressionAST.Comparison("status", ComparisonOperator.EQUALS, "active")
            val right = FilterExpressionAST.Comparison("priority", ComparisonOperator.GREATER_THAN, "5")
            val and = FilterExpressionAST.And(left, right)

            and.left shouldBe left
            and.right shouldBe right
            and.shouldBeInstanceOf<FilterExpressionAST.And>()
        }

        "should create Or node with left and right expressions" {
            val left = FilterExpressionAST.Comparison("type", ComparisonOperator.EQUALS, "bug")
            val right = FilterExpressionAST.Comparison("type", ComparisonOperator.EQUALS, "feature")
            val or = FilterExpressionAST.Or(left, right)

            or.left shouldBe left
            or.right shouldBe right
            or.shouldBeInstanceOf<FilterExpressionAST.Or>()
        }

        "should create Not node with wrapped expression" {
            val inner = FilterExpressionAST.Comparison("status", ComparisonOperator.EQUALS, "closed")
            val not = FilterExpressionAST.Not(inner)

            not.expression shouldBe inner
            not.shouldBeInstanceOf<FilterExpressionAST.Not>()
        }

        "should create Parentheses node with wrapped expression" {
            val inner = FilterExpressionAST.Comparison("priority", ComparisonOperator.GREATER_THAN, "3")
            val parentheses = FilterExpressionAST.Parentheses(inner)

            parentheses.expression shouldBe inner
            parentheses.shouldBeInstanceOf<FilterExpressionAST.Parentheses>()
        }

        "should support all comparison operators in Comparison nodes" {
            val operators = listOf(
                ComparisonOperator.EQUALS,
                ComparisonOperator.NOT_EQUALS,
                ComparisonOperator.GREATER_THAN,
                ComparisonOperator.GREATER_THAN_OR_EQUAL,
                ComparisonOperator.LESS_THAN,
                ComparisonOperator.LESS_THAN_OR_EQUAL,
                ComparisonOperator.CONTAINS,
                ComparisonOperator.NOT_CONTAINS,
            )

            operators.forEach { operator ->
                val comparison = FilterExpressionAST.Comparison("key", operator, "value")
                comparison.operator shouldBe operator
                comparison.key shouldBe "key"
                comparison.value shouldBe "value"
            }
        }

        "should maintain equality for same Comparison values" {
            val comp1 = FilterExpressionAST.Comparison("status", ComparisonOperator.EQUALS, "active")
            val comp2 = FilterExpressionAST.Comparison("status", ComparisonOperator.EQUALS, "active")
            val comp3 = FilterExpressionAST.Comparison("status", ComparisonOperator.NOT_EQUALS, "active")
            val comp4 = FilterExpressionAST.Comparison("priority", ComparisonOperator.EQUALS, "active")

            (comp1 == comp2) shouldBe true
            (comp1 == comp3) shouldBe false
            (comp1 == comp4) shouldBe false

            comp1.hashCode() shouldBe comp2.hashCode()
        }

        "should maintain equality for And nodes" {
            val left = FilterExpressionAST.Comparison("a", ComparisonOperator.EQUALS, "1")
            val right = FilterExpressionAST.Comparison("b", ComparisonOperator.EQUALS, "2")

            val and1 = FilterExpressionAST.And(left, right)
            val and2 = FilterExpressionAST.And(left, right)
            val and3 = FilterExpressionAST.And(right, left) // Different order

            (and1 == and2) shouldBe true
            (and1 == and3) shouldBe false

            and1.hashCode() shouldBe and2.hashCode()
        }

        "should handle empty strings in Comparison nodes" {
            val comparison = FilterExpressionAST.Comparison("", ComparisonOperator.EQUALS, "")

            comparison.key shouldBe ""
            comparison.value shouldBe ""
            comparison.operator shouldBe ComparisonOperator.EQUALS
        }

        "should handle special characters in key and value" {
            val specialValues = listOf(
                "key-with-dashes" to "value-with-dashes",
                "key_with_underscores" to "value_with_underscores",
                "key.with.dots" to "value.with.dots",
                "key@domain" to "value@domain",
                "multi word key" to "multi word value",
                "unicode测试" to "unicode値",
                "123numeric" to "456values",
            )

            specialValues.forEach { (key, value) ->
                val comparison = FilterExpressionAST.Comparison(key, ComparisonOperator.EQUALS, value)
                comparison.key shouldBe key
                comparison.value shouldBe value
            }
        }

        "should support nested And expressions" {
            val comp1 = FilterExpressionAST.Comparison("a", ComparisonOperator.EQUALS, "1")
            val comp2 = FilterExpressionAST.Comparison("b", ComparisonOperator.EQUALS, "2")
            val comp3 = FilterExpressionAST.Comparison("c", ComparisonOperator.EQUALS, "3")

            val and1 = FilterExpressionAST.And(comp1, comp2)
            val and2 = FilterExpressionAST.And(and1, comp3)

            and2.left shouldBe and1
            and2.right shouldBe comp3
            val typedLeft = and2.left.shouldBeInstanceOf<FilterExpressionAST.And>()
            typedLeft.left shouldBe comp1
            typedLeft.right shouldBe comp2
        }

        "should support nested Or expressions" {
            val comp1 = FilterExpressionAST.Comparison("type", ComparisonOperator.EQUALS, "bug")
            val comp2 = FilterExpressionAST.Comparison("type", ComparisonOperator.EQUALS, "feature")
            val comp3 = FilterExpressionAST.Comparison("type", ComparisonOperator.EQUALS, "task")

            val or1 = FilterExpressionAST.Or(comp1, comp2)
            val or2 = FilterExpressionAST.Or(or1, comp3)

            or2.left shouldBe or1
            or2.right shouldBe comp3
            val typedLeft = or2.left.shouldBeInstanceOf<FilterExpressionAST.Or>()
            typedLeft.left shouldBe comp1
            typedLeft.right shouldBe comp2
        }

        "should support multiple levels of nesting" {
            val comp1 = FilterExpressionAST.Comparison("status", ComparisonOperator.EQUALS, "active")
            val comp2 = FilterExpressionAST.Comparison("priority", ComparisonOperator.GREATER_THAN, "5")
            val comp3 = FilterExpressionAST.Comparison("type", ComparisonOperator.EQUALS, "bug")

            val and = FilterExpressionAST.And(comp1, comp2)
            val or = FilterExpressionAST.Or(and, comp3)
            val not = FilterExpressionAST.Not(or)
            val parentheses = FilterExpressionAST.Parentheses(not)

            parentheses.expression shouldBe not
            not.expression shouldBe or
            or.left shouldBe and
            or.right shouldBe comp3
            and.left shouldBe comp1
            and.right shouldBe comp2
        }

        "should support complex realistic filter expression" {
            // Represents: (status = "active" AND priority >= "medium") OR (type = "critical")
            val statusCheck = FilterExpressionAST.Comparison("status", ComparisonOperator.EQUALS, "active")
            val priorityCheck = FilterExpressionAST.Comparison("priority", ComparisonOperator.GREATER_THAN_OR_EQUAL, "medium")
            val typeCheck = FilterExpressionAST.Comparison("type", ComparisonOperator.EQUALS, "critical")

            val activeAndPriority = FilterExpressionAST.And(statusCheck, priorityCheck)
            val parenthesizedGroup = FilterExpressionAST.Parentheses(activeAndPriority)
            val finalExpression = FilterExpressionAST.Or(parenthesizedGroup, typeCheck)

            // Verify the structure
            finalExpression.shouldBeInstanceOf<FilterExpressionAST.Or>()
            finalExpression.left.shouldBeInstanceOf<FilterExpressionAST.Parentheses>()
            finalExpression.right shouldBe typeCheck

            val leftParentheses = finalExpression.left.shouldBeInstanceOf<FilterExpressionAST.Parentheses>()
            leftParentheses.expression.shouldBeInstanceOf<FilterExpressionAST.And>()

            val innerAnd = leftParentheses.expression.shouldBeInstanceOf<FilterExpressionAST.And>()
            innerAnd.left shouldBe statusCheck
            innerAnd.right shouldBe priorityCheck
        }

        "should support NOT with complex expressions" {
            // Represents: NOT ((status = "done") OR (priority < "low"))
            val statusDone = FilterExpressionAST.Comparison("status", ComparisonOperator.EQUALS, "done")
            val lowPriority = FilterExpressionAST.Comparison("priority", ComparisonOperator.LESS_THAN, "low")

            val innerOr = FilterExpressionAST.Or(statusDone, lowPriority)
            val parenthesized = FilterExpressionAST.Parentheses(innerOr)
            val notExpression = FilterExpressionAST.Not(parenthesized)

            notExpression.expression shouldBe parenthesized
            parenthesized.expression shouldBe innerOr
            innerOr.left shouldBe statusDone
            innerOr.right shouldBe lowPriority
        }

        "should handle deeply nested expressions" {
            // Create a deeply nested structure
            val base = FilterExpressionAST.Comparison("level", ComparisonOperator.EQUALS, "0")
            var current: FilterExpressionAST = base

            // Create 5 levels of nesting
            for (i in 1..5) {
                val next = FilterExpressionAST.Comparison("level", ComparisonOperator.EQUALS, i.toString())
                current = FilterExpressionAST.And(current, next)
            }

            // Verify we can traverse the structure
            current.shouldBeInstanceOf<FilterExpressionAST.And>()
            val level5And = current.shouldBeInstanceOf<FilterExpressionAST.And>()
            level5And.right.shouldBeInstanceOf<FilterExpressionAST.Comparison>()

            val level5Comp = level5And.right.shouldBeInstanceOf<FilterExpressionAST.Comparison>()
            level5Comp.value shouldBe "5"
        }

        "should support contains and not contains operators" {
            val containsComp = FilterExpressionAST.Comparison("tags", ComparisonOperator.CONTAINS, "important")
            val notContainsComp = FilterExpressionAST.Comparison("tags", ComparisonOperator.NOT_CONTAINS, "obsolete")

            containsComp.operator shouldBe ComparisonOperator.CONTAINS
            notContainsComp.operator shouldBe ComparisonOperator.NOT_CONTAINS

            containsComp.key shouldBe "tags"
            containsComp.value shouldBe "important"
            notContainsComp.key shouldBe "tags"
            notContainsComp.value shouldBe "obsolete"
        }

        "should maintain immutability of AST nodes" {
            val comp1 = FilterExpressionAST.Comparison("status", ComparisonOperator.EQUALS, "active")
            val comp2 = FilterExpressionAST.Comparison("priority", ComparisonOperator.GREATER_THAN, "5")
            val and = FilterExpressionAST.And(comp1, comp2)

            // References should remain the same
            val retrievedLeft = and.left
            val retrievedRight = and.right

            retrievedLeft shouldBe comp1
            retrievedRight shouldBe comp2

            // Original objects should be unchanged
            and.left shouldBe comp1
            and.right shouldBe comp2
        }

        "should support polymorphic usage through sealed class" {
            val expressions: List<FilterExpressionAST> = listOf(
                FilterExpressionAST.Comparison("a", ComparisonOperator.EQUALS, "1"),
                FilterExpressionAST.And(
                    FilterExpressionAST.Comparison("b", ComparisonOperator.EQUALS, "2"),
                    FilterExpressionAST.Comparison("c", ComparisonOperator.EQUALS, "3"),
                ),
                FilterExpressionAST.Or(
                    FilterExpressionAST.Comparison("d", ComparisonOperator.EQUALS, "4"),
                    FilterExpressionAST.Comparison("e", ComparisonOperator.EQUALS, "5"),
                ),
                FilterExpressionAST.Not(
                    FilterExpressionAST.Comparison("f", ComparisonOperator.EQUALS, "6"),
                ),
                FilterExpressionAST.Parentheses(
                    FilterExpressionAST.Comparison("g", ComparisonOperator.EQUALS, "7"),
                ),
            )

            // All should be instances of FilterExpressionAST
            expressions.forEach { expr ->
                expr.shouldBeInstanceOf<FilterExpressionAST>()
            }

            // Verify specific types
            expressions[0].shouldBeInstanceOf<FilterExpressionAST.Comparison>()
            expressions[1].shouldBeInstanceOf<FilterExpressionAST.And>()
            expressions[2].shouldBeInstanceOf<FilterExpressionAST.Or>()
            expressions[3].shouldBeInstanceOf<FilterExpressionAST.Not>()
            expressions[4].shouldBeInstanceOf<FilterExpressionAST.Parentheses>()
        }
    })

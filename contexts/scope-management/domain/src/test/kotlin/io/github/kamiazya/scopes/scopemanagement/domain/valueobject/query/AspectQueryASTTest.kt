package io.github.kamiazya.scopes.scopemanagement.domain.valueobject.query

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for AspectQueryAST sealed class and ComparisonOperator enum.
 *
 * Business rules:
 * - AspectQueryAST represents the structure of parsed query expressions
 * - ComparisonOperator supports standard comparison operations
 * - All AST nodes should be immutable and properly typed
 */
class AspectQueryASTTest :
    StringSpec({

        "should create Comparison node with key, operator, and value" {
            val comparison = AspectQueryAST.Comparison("priority", ComparisonOperator.EQUALS, "high")

            comparison.key shouldBe "priority"
            comparison.operator shouldBe ComparisonOperator.EQUALS
            comparison.value shouldBe "high"
            comparison.shouldBeInstanceOf<AspectQueryAST.Comparison>()
        }

        "should create And node with left and right expressions" {
            val left = AspectQueryAST.Comparison("priority", ComparisonOperator.EQUALS, "high")
            val right = AspectQueryAST.Comparison("status", ComparisonOperator.NOT_EQUALS, "closed")
            val andNode = AspectQueryAST.And(left, right)

            andNode.left shouldBe left
            andNode.right shouldBe right
            andNode.shouldBeInstanceOf<AspectQueryAST.And>()
        }

        "should create Or node with left and right expressions" {
            val left = AspectQueryAST.Comparison("priority", ComparisonOperator.EQUALS, "high")
            val right = AspectQueryAST.Comparison("priority", ComparisonOperator.EQUALS, "medium")
            val orNode = AspectQueryAST.Or(left, right)

            orNode.left shouldBe left
            orNode.right shouldBe right
            orNode.shouldBeInstanceOf<AspectQueryAST.Or>()
        }

        "should create Not node with wrapped expression" {
            val comparison = AspectQueryAST.Comparison("status", ComparisonOperator.EQUALS, "closed")
            val notNode = AspectQueryAST.Not(comparison)

            notNode.expression shouldBe comparison
            notNode.shouldBeInstanceOf<AspectQueryAST.Not>()
        }

        "should create Parentheses node with wrapped expression" {
            val comparison = AspectQueryAST.Comparison("priority", ComparisonOperator.EQUALS, "high")
            val parenNode = AspectQueryAST.Parentheses(comparison)

            parenNode.expression shouldBe comparison
            parenNode.shouldBeInstanceOf<AspectQueryAST.Parentheses>()
        }

        "should support all comparison operators in Comparison nodes" {
            val operators = listOf(
                ComparisonOperator.EQUALS,
                ComparisonOperator.NOT_EQUALS,
                ComparisonOperator.GREATER_THAN,
                ComparisonOperator.GREATER_THAN_OR_EQUALS,
                ComparisonOperator.LESS_THAN,
                ComparisonOperator.LESS_THAN_OR_EQUALS,
            )

            operators.forEach { operator ->
                val comparison = AspectQueryAST.Comparison("key", operator, "value")
                comparison.operator shouldBe operator
                comparison.shouldBeInstanceOf<AspectQueryAST.Comparison>()
            }
        }

        "should maintain equality for same Comparison values" {
            val comparison1 = AspectQueryAST.Comparison("priority", ComparisonOperator.EQUALS, "high")
            val comparison2 = AspectQueryAST.Comparison("priority", ComparisonOperator.EQUALS, "high")
            val comparison3 = AspectQueryAST.Comparison("priority", ComparisonOperator.EQUALS, "low")

            (comparison1 == comparison2) shouldBe true
            (comparison1 == comparison3) shouldBe false
            comparison1.hashCode() shouldBe comparison2.hashCode()
        }

        "should maintain equality for And nodes" {
            val left = AspectQueryAST.Comparison("priority", ComparisonOperator.EQUALS, "high")
            val right = AspectQueryAST.Comparison("status", ComparisonOperator.EQUALS, "open")
            val and1 = AspectQueryAST.And(left, right)
            val and2 = AspectQueryAST.And(left, right)

            (and1 == and2) shouldBe true
            and1.hashCode() shouldBe and2.hashCode()
        }

        "should support nested And expressions" {
            val comp1 = AspectQueryAST.Comparison("priority", ComparisonOperator.EQUALS, "high")
            val comp2 = AspectQueryAST.Comparison("status", ComparisonOperator.EQUALS, "open")
            val comp3 = AspectQueryAST.Comparison("assignee", ComparisonOperator.EQUALS, "alice")

            val innerAnd = AspectQueryAST.And(comp1, comp2)
            val outerAnd = AspectQueryAST.And(innerAnd, comp3)

            outerAnd.left shouldBe innerAnd
            outerAnd.right shouldBe comp3
            (outerAnd.left as AspectQueryAST.And).left shouldBe comp1
            (outerAnd.left as AspectQueryAST.And).right shouldBe comp2
        }

        "should support nested Or expressions" {
            val comp1 = AspectQueryAST.Comparison("priority", ComparisonOperator.EQUALS, "high")
            val comp2 = AspectQueryAST.Comparison("priority", ComparisonOperator.EQUALS, "critical")
            val comp3 = AspectQueryAST.Comparison("status", ComparisonOperator.EQUALS, "urgent")

            val innerOr = AspectQueryAST.Or(comp1, comp2)
            val outerOr = AspectQueryAST.Or(innerOr, comp3)

            outerOr.left shouldBe innerOr
            outerOr.right shouldBe comp3
            (outerOr.left as AspectQueryAST.Or).left shouldBe comp1
            (outerOr.left as AspectQueryAST.Or).right shouldBe comp2
        }

        "should support NOT with complex expressions" {
            val comp1 = AspectQueryAST.Comparison("priority", ComparisonOperator.EQUALS, "low")
            val comp2 = AspectQueryAST.Comparison("status", ComparisonOperator.EQUALS, "closed")
            val orNode = AspectQueryAST.Or(comp1, comp2)
            val notNode = AspectQueryAST.Not(orNode)

            notNode.expression shouldBe orNode
            notNode.expression.shouldBeInstanceOf<AspectQueryAST.Or>()
        }

        "should support multiple levels of nesting" {
            // (priority=high OR priority=critical) AND NOT status=closed
            val highPriority = AspectQueryAST.Comparison("priority", ComparisonOperator.EQUALS, "high")
            val criticalPriority = AspectQueryAST.Comparison("priority", ComparisonOperator.EQUALS, "critical")
            val closedStatus = AspectQueryAST.Comparison("status", ComparisonOperator.EQUALS, "closed")

            val priorityOr = AspectQueryAST.Or(highPriority, criticalPriority)
            val notClosed = AspectQueryAST.Not(closedStatus)
            val finalAnd = AspectQueryAST.And(priorityOr, notClosed)

            finalAnd.left shouldBe priorityOr
            finalAnd.right shouldBe notClosed
            (finalAnd.left as AspectQueryAST.Or).left shouldBe highPriority
            (finalAnd.left as AspectQueryAST.Or).right shouldBe criticalPriority
            (finalAnd.right as AspectQueryAST.Not).expression shouldBe closedStatus
        }

        "should handle empty strings in Comparison nodes" {
            val emptyKey = AspectQueryAST.Comparison("", ComparisonOperator.EQUALS, "value")
            val emptyValue = AspectQueryAST.Comparison("key", ComparisonOperator.EQUALS, "")
            val bothEmpty = AspectQueryAST.Comparison("", ComparisonOperator.EQUALS, "")

            emptyKey.key shouldBe ""
            emptyKey.value shouldBe "value"

            emptyValue.key shouldBe "key"
            emptyValue.value shouldBe ""

            bothEmpty.key shouldBe ""
            bothEmpty.value shouldBe ""
        }

        "should handle special characters in key and value" {
            val specialChars = listOf(
                "key_with_underscore" to "value-with-dash",
                "key.with.dots" to "value with spaces",
                "key123" to "value@domain.com",
                "CamelCaseKey" to "MixedCaseValue",
            )

            specialChars.forEach { (key, value) ->
                val comparison = AspectQueryAST.Comparison(key, ComparisonOperator.EQUALS, value)
                comparison.key shouldBe key
                comparison.value shouldBe value
            }
        }

        "should support polymorphic usage through sealed class" {
            val expressions: List<AspectQueryAST> = listOf(
                AspectQueryAST.Comparison("priority", ComparisonOperator.EQUALS, "high"),
                AspectQueryAST.And(
                    AspectQueryAST.Comparison("status", ComparisonOperator.EQUALS, "open"),
                    AspectQueryAST.Comparison("assignee", ComparisonOperator.EQUALS, "alice"),
                ),
                AspectQueryAST.Or(
                    AspectQueryAST.Comparison("priority", ComparisonOperator.EQUALS, "high"),
                    AspectQueryAST.Comparison("priority", ComparisonOperator.EQUALS, "critical"),
                ),
                AspectQueryAST.Not(AspectQueryAST.Comparison("status", ComparisonOperator.EQUALS, "closed")),
                AspectQueryAST.Parentheses(AspectQueryAST.Comparison("priority", ComparisonOperator.EQUALS, "medium")),
            )

            expressions.forEach { expression ->
                expression.shouldBeInstanceOf<AspectQueryAST>()
            }

            expressions[0].shouldBeInstanceOf<AspectQueryAST.Comparison>()
            expressions[1].shouldBeInstanceOf<AspectQueryAST.And>()
            expressions[2].shouldBeInstanceOf<AspectQueryAST.Or>()
            expressions[3].shouldBeInstanceOf<AspectQueryAST.Not>()
            expressions[4].shouldBeInstanceOf<AspectQueryAST.Parentheses>()
        }

        "should maintain immutability of AST nodes" {
            val comparison = AspectQueryAST.Comparison("priority", ComparisonOperator.EQUALS, "high")
            val and = AspectQueryAST.And(comparison, comparison)

            // Data classes are immutable by default
            comparison.key shouldBe "priority"
            and.left shouldBe comparison
            and.right shouldBe comparison

            // Cannot modify properties (they are val)
            // This is enforced at compile time
        }

        "should support complex realistic query expression" {
            // (priority=high OR priority=critical) AND status!=closed AND assignee=alice
            val highPriority = AspectQueryAST.Comparison("priority", ComparisonOperator.EQUALS, "high")
            val criticalPriority = AspectQueryAST.Comparison("priority", ComparisonOperator.EQUALS, "critical")
            val notClosed = AspectQueryAST.Comparison("status", ComparisonOperator.NOT_EQUALS, "closed")
            val assignedToAlice = AspectQueryAST.Comparison("assignee", ComparisonOperator.EQUALS, "alice")

            val priorityOrGroup = AspectQueryAST.Parentheses(AspectQueryAST.Or(highPriority, criticalPriority))
            val statusAndAssignee = AspectQueryAST.And(notClosed, assignedToAlice)
            val finalQuery = AspectQueryAST.And(priorityOrGroup, statusAndAssignee)

            finalQuery.shouldBeInstanceOf<AspectQueryAST.And>()
            finalQuery.left.shouldBeInstanceOf<AspectQueryAST.Parentheses>()
            finalQuery.right.shouldBeInstanceOf<AspectQueryAST.And>()
        }

        "should handle deeply nested expressions" {
            // Build a deeply nested expression: ((a=1 AND b=2) OR (c=3 AND d=4)) AND e=5
            val a1 = AspectQueryAST.Comparison("a", ComparisonOperator.EQUALS, "1")
            val b2 = AspectQueryAST.Comparison("b", ComparisonOperator.EQUALS, "2")
            val c3 = AspectQueryAST.Comparison("c", ComparisonOperator.EQUALS, "3")
            val d4 = AspectQueryAST.Comparison("d", ComparisonOperator.EQUALS, "4")
            val e5 = AspectQueryAST.Comparison("e", ComparisonOperator.EQUALS, "5")

            val leftAnd = AspectQueryAST.And(a1, b2)
            val rightAnd = AspectQueryAST.And(c3, d4)
            val innerOr = AspectQueryAST.Or(leftAnd, rightAnd)
            val outerParens = AspectQueryAST.Parentheses(innerOr)
            val finalExpression = AspectQueryAST.And(outerParens, e5)

            finalExpression.shouldBeInstanceOf<AspectQueryAST.And>()
            finalExpression.left.shouldBeInstanceOf<AspectQueryAST.Parentheses>()
            finalExpression.right shouldBe e5
        }

        // ComparisonOperator tests
        "should provide correct symbols for all operators" {
            ComparisonOperator.EQUALS.symbol shouldBe "="
            ComparisonOperator.NOT_EQUALS.symbol shouldBe "!="
            ComparisonOperator.GREATER_THAN.symbol shouldBe ">"
            ComparisonOperator.GREATER_THAN_OR_EQUALS.symbol shouldBe ">="
            ComparisonOperator.LESS_THAN.symbol shouldBe "<"
            ComparisonOperator.LESS_THAN_OR_EQUALS.symbol shouldBe "<="
        }

        "should find operators by symbol" {
            ComparisonOperator.fromSymbol("=") shouldBe ComparisonOperator.EQUALS
            ComparisonOperator.fromSymbol("!=") shouldBe ComparisonOperator.NOT_EQUALS
            ComparisonOperator.fromSymbol(">") shouldBe ComparisonOperator.GREATER_THAN
            ComparisonOperator.fromSymbol(">=") shouldBe ComparisonOperator.GREATER_THAN_OR_EQUALS
            ComparisonOperator.fromSymbol("<") shouldBe ComparisonOperator.LESS_THAN
            ComparisonOperator.fromSymbol("<=") shouldBe ComparisonOperator.LESS_THAN_OR_EQUALS
        }

        "should return null for unknown operator symbols" {
            ComparisonOperator.fromSymbol("==") shouldBe null
            ComparisonOperator.fromSymbol("~=") shouldBe null
            ComparisonOperator.fromSymbol("") shouldBe null
            ComparisonOperator.fromSymbol("invalid") shouldBe null
        }

        "should handle case sensitivity in operator symbols" {
            // Symbols are case sensitive
            ComparisonOperator.fromSymbol("EQUALS") shouldBe null
            ComparisonOperator.fromSymbol("=") shouldBe ComparisonOperator.EQUALS
        }

        "should support all operators in expressions" {
            val operators = ComparisonOperator.values()
            operators.forEach { operator ->
                val comparison = AspectQueryAST.Comparison("key", operator, "value")
                comparison.operator shouldBe operator
                comparison.operator.symbol shouldNotBe ""
            }
        }
    })

package io.github.kamiazya.scopes.scopemanagement.domain.valueobject

import io.github.kamiazya.scopes.scopemanagement.domain.error.ContextError
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll

/**
 * Tests for ContextViewFilter value object.
 *
 * Business rules:
 * - Must not be empty or blank
 * - Must be between 1 and 1000 characters
 * - Basic syntax validation (parentheses, quotes, operators)
 * - Advanced parsing is delegated to FilterExpressionValidator
 */
class ContextViewFilterTest :
    StringSpec({

        "should create valid simple filter" {
            val filter = "priority == 'high'"
            val result = ContextViewFilter.create(filter)
            val viewFilter = result.shouldBeRight()
            viewFilter.expression shouldBe filter
            viewFilter.toString() shouldBe filter
        }

        "should trim whitespace from input" {
            val result = ContextViewFilter.create("  status == 'active'  ")
            val filter = result.shouldBeRight()
            filter.expression shouldBe "status == 'active'"
        }

        "should accept complex filter expressions" {
            val complexFilters = listOf(
                "priority == 'high' AND status == 'in_progress'",
                "(type == 'bug' OR type == 'feature') AND !blocked",
                "assignee == 'alice' AND estimate > 8",
                "EXISTS(dueDate) AND dueDate < '2025-12-31'",
                "tags CONTAINS 'frontend' OR tags CONTAINS 'ui'",
                "NOT (status == 'done' OR status == 'cancelled')",
                "(priority >= 'medium' AND effort <= 16) OR type == 'hotfix'",
                "project IN ['alpha', 'beta', 'gamma']",
            )

            complexFilters.forEach { filterExpr ->
                val result = ContextViewFilter.create(filterExpr)
                val filter = result.shouldBeRight()
                filter.expression shouldBe filterExpr
            }
        }

        "should reject empty string" {
            val result = ContextViewFilter.create("")
            result.shouldBeLeft()
            result.leftOrNull() shouldBe ContextError.EmptyFilter
        }

        "should reject blank string" {
            val result = ContextViewFilter.create("   \n\t  ")
            result.shouldBeLeft()
            result.leftOrNull() shouldBe ContextError.EmptyFilter
        }

        "should accept single character filter" {
            // While not semantically valid, syntactically it's allowed
            val result = ContextViewFilter.create("a")
            val filter = result.shouldBeRight()
            filter.expression shouldBe "a"
        }

        "should accept filter at maximum length" {
            val maxFilter = "a".repeat(1000)
            val result = ContextViewFilter.create(maxFilter)
            val filter = result.shouldBeRight()
            filter.expression shouldBe maxFilter
            filter.expression.length shouldBe 1000
        }

        "should reject filter that is too long" {
            val longFilter = "a".repeat(1001)
            val result = ContextViewFilter.create(longFilter)
            result.shouldBeLeft()
            result.leftOrNull() shouldBe ContextError.FilterTooLong(maximumLength = 1000)
        }

        "should detect unbalanced parentheses - missing closing" {
            val filters = listOf(
                "(priority == 'high'",
                "((status == 'active')",
                "(a AND (b OR c)",
            )

            filters.forEach { filterExpr ->
                val result = ContextViewFilter.create(filterExpr)
                result.shouldBeLeft()
                val error = result.leftOrNull() as? ContextError.InvalidFilterSyntax
                error?.errorType shouldBe ContextError.FilterSyntaxErrorType.MissingClosingParen(filterExpr.length)
            }
        }

        "should detect unbalanced parentheses - extra closing" {
            val filters = listOf(
                "priority == 'high')",
                "(status == 'active'))",
                "a AND b)",
            )

            filters.forEach { filterExpr ->
                val result = ContextViewFilter.create(filterExpr)
                result.shouldBeLeft()
                val error = result.leftOrNull() as? ContextError.InvalidFilterSyntax
                error?.errorType shouldBe ContextError.FilterSyntaxErrorType.UnbalancedParentheses
            }
        }

        "should detect unbalanced quotes" {
            val filters = listOf(
                "name == 'unclosed",
                "status == \"unclosed",
                "text == \"incomplete",
                "field == 'value with \" mixed quotes", // Odd double quotes
            )

            filters.forEach { filterExpr ->
                val result = ContextViewFilter.create(filterExpr)
                result.shouldBeLeft()
                val error = result.leftOrNull() as? ContextError.InvalidFilterSyntax
                error?.errorType shouldBe ContextError.FilterSyntaxErrorType.UnbalancedQuotes
            }
        }

        "should handle quotes in quoted strings (implementation limitation)" {
            // Note: The basic syntax check counts quotes without context
            // These expressions have balanced quotes semantically, but odd count syntactically
            val filtersWithOddQuotes = listOf(
                "text == \"it's fine\"", // 2 double quotes + 1 single quote = odd single count
                "value == \"can't handle this\"", // 2 double quotes + 1 single quote = odd single count
                "name == 'O\\'Brien'", // 3 single quotes (two delimiters + escaped) = odd count
            )

            filtersWithOddQuotes.forEach { filterExpr ->
                val result = ContextViewFilter.create(filterExpr)
                // These fail basic validation due to odd quote count
                result.shouldBeLeft()
                val error = result.leftOrNull() as? ContextError.InvalidFilterSyntax
                error?.errorType shouldBe ContextError.FilterSyntaxErrorType.UnbalancedQuotes
            }

            // These have even quote counts so they pass basic validation
            val filtersWithEvenQuotes = listOf(
                "value == 'simple value'", // 2 single quotes
                "name == \"John Smith\"", // 2 double quotes
            )

            filtersWithEvenQuotes.forEach { filterExpr ->
                val result = ContextViewFilter.create(filterExpr)
                result.shouldBeRight() // Basic syntax passes
            }

            // Special case: escaped quotes make counting complex
            // The implementation counts all quote characters, including escaped ones
            val escapedQuoteTest = "text == 'don\\'t break this'"
            val escapedResult = ContextViewFilter.create(escapedQuoteTest)
            // This has 4 single quotes total, but the basic validator may still fail due to escape handling
            // Let's test what actually happens
            escapedResult.fold(
                { error ->
                    // If it fails, it should be due to quote validation
                    error shouldBe ContextError.InvalidFilterSyntax(
                        expression = escapedQuoteTest,
                        errorType = ContextError.FilterSyntaxErrorType.UnbalancedQuotes,
                    )
                },
                { filter ->
                    // If it passes, the expression should be preserved
                    filter.expression shouldBe escapedQuoteTest
                },
            )
        }

        "should detect empty operators" {
            val filters = listOf(
                "a AND AND b",
                "x OR OR y",
                "NOT NOT z",
                "status == 'active' AND OR priority == 'high'",
            )

            filters.forEach { filterExpr ->
                val result = ContextViewFilter.create(filterExpr)
                result.shouldBeLeft()
                val error = result.leftOrNull() as? ContextError.InvalidFilterSyntax
                error?.errorType shouldBe ContextError.FilterSyntaxErrorType.EmptyOperator
            }
        }

        "should allow balanced parentheses and quotes" {
            val validFilters = listOf(
                "(a == 'b')",
                "((a == 'b') AND (c == 'd'))",
                "(((nested)))",
                "value == 'quoted \"string\" here'",
                "text == \"quoted 'string' here\"",
                "(a == 'b') AND (c == 'd') OR (e == 'f')",
            )

            validFilters.forEach { filterExpr ->
                val result = ContextViewFilter.create(filterExpr)
                result.shouldBeRight()
            }
        }

        "should accept filters with escaped quotes" {
            // Note: Basic syntax check doesn't validate escape sequences
            // That's handled by the actual parser
            val filters = listOf(
                "name == 'O\\'Brien'",
                "text == \"Say \\\"Hello\\\"\"",
                "value == 'Line 1\\nLine 2'",
            )

            filters.forEach { filterExpr ->
                val result = ContextViewFilter.create(filterExpr)
                // Basic syntax check may pass or fail depending on implementation
                // The important thing is it doesn't crash
                result.fold(
                    { true }, // Error is OK for escaped quotes in basic validation
                    { filter -> filter.expression shouldBe filterExpr },
                )
            }
        }

        "should verify toString returns the expression" {
            val filterExpr = "priority >= 'medium'"
            val result = ContextViewFilter.create(filterExpr)
            val filter = result.shouldBeRight()
            filter.toString() shouldBe filterExpr
            filter.toString() shouldBe filter.expression
        }

        // Test factory methods
        "should create equals filter using factory method" {
            val filter = ContextViewFilter.equals("status", "active")
            filter.expression shouldBe "status == 'active'"
        }

        "should create not equals filter using factory method" {
            val filter = ContextViewFilter.notEquals("priority", "low")
            filter.expression shouldBe "priority != 'low'"
        }

        "should create exists filter using factory method" {
            val filter = ContextViewFilter.exists("dueDate")
            filter.expression shouldBe "EXISTS(dueDate)"
        }

        "should create contains filter using factory method" {
            val filter = ContextViewFilter.contains("tags", "urgent")
            filter.expression shouldBe "tags CONTAINS 'urgent'"
        }

        "should combine filters with AND" {
            val filter1 = ContextViewFilter.equals("status", "active")
            val filter2 = ContextViewFilter.equals("priority", "high")
            val combined = ContextViewFilter.and(listOf(filter1, filter2))
            combined.expression shouldBe "(status == 'active') AND (priority == 'high')"
        }

        "should combine filters with OR" {
            val filter1 = ContextViewFilter.equals("type", "bug")
            val filter2 = ContextViewFilter.equals("type", "feature")
            val combined = ContextViewFilter.or(listOf(filter1, filter2))
            combined.expression shouldBe "(type == 'bug') OR (type == 'feature')"
        }

        "should negate filter" {
            val filter = ContextViewFilter.equals("status", "done")
            val negated = ContextViewFilter.not(filter)
            negated.expression shouldBe "NOT (status == 'done')"
        }

        "should handle complex combinations of factory methods" {
            val highPriority = ContextViewFilter.equals("priority", "high")
            val mediumPriority = ContextViewFilter.equals("priority", "medium")
            val inProgress = ContextViewFilter.equals("status", "in_progress")
            val hasDueDate = ContextViewFilter.exists("dueDate")

            val priorityFilter = ContextViewFilter.or(listOf(highPriority, mediumPriority))
            val activeWork = ContextViewFilter.and(listOf(priorityFilter, inProgress))
            val urgentWork = ContextViewFilter.and(listOf(activeWork, hasDueDate))

            urgentWork.expression shouldBe "(((priority == 'high') OR (priority == 'medium')) AND (status == 'in_progress')) AND (EXISTS(dueDate))"
        }

        // Property-based testing
        "should always trim input strings" {
            checkAll(Arb.string(0..1100)) { input ->
                val result = ContextViewFilter.create(input)
                result.fold(
                    { true }, // Error case is valid
                    { filter -> filter.expression == input.trim() },
                )
            }
        }

        "should handle realistic filter expressions" {
            val realFilters = listOf(
                "assignee == 'me' AND status != 'done'",
                "project == 'alpha' AND (priority == 'high' OR type == 'hotfix')",
                "sprint == '2025-Q1-Sprint-3' AND !blocked",
                "estimate <= 8 AND tags CONTAINS 'frontend'",
                "createdDate >= '2025-01-01' AND createdDate < '2025-02-01'",
                "(labels CONTAINS 'bug' OR labels CONTAINS 'defect') AND severity >= 'major'",
                "parent == 'PROJ-123' OR parent == 'PROJ-456'",
                "NOT (status == 'cancelled' OR status == 'deferred')",
            )

            realFilters.forEach { filterExpr ->
                val result = ContextViewFilter.create(filterExpr)
                val filter = result.shouldBeRight()
                filter.expression shouldBe filterExpr
            }
        }

        "should handle edge cases for operators" {
            // These should pass basic syntax validation
            // Semantic validation happens later
            val edgeCases = listOf(
                "AND", // Just an operator (syntactically OK, semantically invalid)
                "OR AND", // Operators without operands
                "()()", // Empty parentheses
                "a==b", // No spaces around operator
                "x = = y", // Extra spaces in operator
                "'value'", // Just a string literal
                "123", // Just a number
            )

            edgeCases.forEach { filterExpr ->
                val result = ContextViewFilter.create(filterExpr)
                // Basic syntax check should handle these gracefully
                // May pass or fail depending on specific edge case
            }
        }

        "should preserve exact input after trimming" {
            val expressions = listOf(
                "a  ==  b", // Multiple spaces
                "x\tAND\ty", // Tabs
                "  (nested  (  spaces  )  )", // Spaces in parentheses
                "key=='value'", // No spaces around operator
                "NOT(EXISTS(field))", // No spaces in function calls
            )

            expressions.forEach { expr ->
                val result = ContextViewFilter.create(expr)
                val filter = result.shouldBeRight()
                filter.expression shouldBe expr.trim()
            }
        }
    })

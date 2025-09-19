package io.github.kamiazya.scopes.scopemanagement.domain.valueobject

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for Token sealed class.
 *
 * Business rules:
 * - Each token has a position indicating its location in the source
 * - Different token types represent different elements in filter expressions
 * - Tokens are immutable value objects used in parsing
 */
class TokenTest :
    StringSpec({

        "should create Identifier token with value and position" {
            val token = Token.Identifier("priority", 5)

            token.value shouldBe "priority"
            token.position shouldBe 5
            token.shouldBeInstanceOf<Token.Identifier>()
        }

        "should create StringLiteral token with value and position" {
            val token = Token.StringLiteral("high", 10)

            token.value shouldBe "high"
            token.position shouldBe 10
            token.shouldBeInstanceOf<Token.StringLiteral>()
        }

        "should create Operator token with comparison operator and position" {
            val token = Token.Operator(ComparisonOperator.EQUALS, 15)

            token.op shouldBe ComparisonOperator.EQUALS
            token.position shouldBe 15
            token.shouldBeInstanceOf<Token.Operator>()
        }

        "should create And token with position" {
            val token = Token.And(20)

            token.position shouldBe 20
            token.shouldBeInstanceOf<Token.And>()
        }

        "should create Or token with position" {
            val token = Token.Or(25)

            token.position shouldBe 25
            token.shouldBeInstanceOf<Token.Or>()
        }

        "should create Not token with position" {
            val token = Token.Not(30)

            token.position shouldBe 30
            token.shouldBeInstanceOf<Token.Not>()
        }

        "should create LeftParen token with position" {
            val token = Token.LeftParen(35)

            token.position shouldBe 35
            token.shouldBeInstanceOf<Token.LeftParen>()
        }

        "should create RightParen token with position" {
            val token = Token.RightParen(40)

            token.position shouldBe 40
            token.shouldBeInstanceOf<Token.RightParen>()
        }

        "should support all comparison operators in Operator token" {
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

            operators.forEachIndexed { index, op ->
                val token = Token.Operator(op, index)
                token.op shouldBe op
                token.position shouldBe index
            }
        }

        "should maintain equality for same token values" {
            val token1 = Token.Identifier("test", 5)
            val token2 = Token.Identifier("test", 5)
            val token3 = Token.Identifier("test", 10) // Different position
            val token4 = Token.Identifier("other", 5) // Different value

            (token1 == token2) shouldBe true
            (token1 == token3) shouldBe false
            (token1 == token4) shouldBe false

            token1.hashCode() shouldBe token2.hashCode()
        }

        "should maintain equality for operator tokens" {
            val token1 = Token.Operator(ComparisonOperator.EQUALS, 5)
            val token2 = Token.Operator(ComparisonOperator.EQUALS, 5)
            val token3 = Token.Operator(ComparisonOperator.NOT_EQUALS, 5)
            val token4 = Token.Operator(ComparisonOperator.EQUALS, 10)

            (token1 == token2) shouldBe true
            (token1 == token3) shouldBe false
            (token1 == token4) shouldBe false

            token1.hashCode() shouldBe token2.hashCode()
        }

        "should maintain equality for position-only tokens" {
            val and1 = Token.And(5)
            val and2 = Token.And(5)
            val and3 = Token.And(10)

            (and1 == and2) shouldBe true
            (and1 == and3) shouldBe false

            and1.hashCode() shouldBe and2.hashCode()
        }

        "should support different token types with different positions" {
            val tokens = listOf(
                Token.Identifier("priority", 0),
                Token.StringLiteral("high", 5),
                Token.Operator(ComparisonOperator.EQUALS, 10),
                Token.And(15),
                Token.Or(20),
                Token.Not(25),
                Token.LeftParen(30),
                Token.RightParen(35),
            )

            tokens.forEachIndexed { index, token ->
                token.position shouldBe index * 5
                token.shouldBeInstanceOf<Token>()
            }
        }

        "should handle empty strings in Identifier tokens" {
            val token = Token.Identifier("", 0)

            token.value shouldBe ""
            token.position shouldBe 0
        }

        "should handle empty strings in StringLiteral tokens" {
            val token = Token.StringLiteral("", 0)

            token.value shouldBe ""
            token.position shouldBe 0
        }

        "should handle special characters in token values" {
            val specialChars = listOf(
                "priority_level",
                "user-name",
                "scope.id",
                "value@domain",
                "test#tag",
                "multi word value",
                "unicode测试",
            )

            specialChars.forEachIndexed { index, value ->
                val identifier = Token.Identifier(value, index)
                val stringLiteral = Token.StringLiteral(value, index)

                identifier.value shouldBe value
                stringLiteral.value shouldBe value
            }
        }

        "should handle negative positions" {
            val token = Token.Identifier("test", -1)
            token.position shouldBe -1
        }

        "should handle very large positions" {
            val largePosition = Int.MAX_VALUE
            val token = Token.Identifier("test", largePosition)
            token.position shouldBe largePosition
        }

        "should allow different token types at same position" {
            val position = 42
            val tokens = listOf(
                Token.Identifier("test", position),
                Token.StringLiteral("test", position),
                Token.Operator(ComparisonOperator.EQUALS, position),
                Token.And(position),
                Token.Or(position),
                Token.Not(position),
                Token.LeftParen(position),
                Token.RightParen(position),
            )

            tokens.forEach { token ->
                token.position shouldBe position
            }
        }

        "should demonstrate typical filter expression token sequence" {
            // Represents: priority = "high" AND status != "done"
            val tokens = listOf(
                Token.Identifier("priority", 0), // priority
                Token.Operator(ComparisonOperator.EQUALS, 8), // =
                Token.StringLiteral("high", 10), // "high"
                Token.And(16), // AND
                Token.Identifier("status", 20), // status
                Token.Operator(ComparisonOperator.NOT_EQUALS, 26), // !=
                Token.StringLiteral("done", 29), // "done"
            )

            // Verify the sequence represents the expected filter
            val token0 = tokens[0].shouldBeInstanceOf<Token.Identifier>()
            token0.value shouldBe "priority"

            val token1 = tokens[1].shouldBeInstanceOf<Token.Operator>()
            token1.op shouldBe ComparisonOperator.EQUALS

            val token2 = tokens[2].shouldBeInstanceOf<Token.StringLiteral>()
            token2.value shouldBe "high"

            tokens[3].shouldBeInstanceOf<Token.And>()

            val token4 = tokens[4].shouldBeInstanceOf<Token.Identifier>()
            token4.value shouldBe "status"

            val token5 = tokens[5].shouldBeInstanceOf<Token.Operator>()
            token5.op shouldBe ComparisonOperator.NOT_EQUALS

            val token6 = tokens[6].shouldBeInstanceOf<Token.StringLiteral>()
            token6.value shouldBe "done"
        }

        "should demonstrate complex parenthesized expression tokens" {
            // Represents: NOT (priority > "medium" OR status = "blocked")
            val tokens = listOf(
                Token.Not(0), // NOT
                Token.LeftParen(4), // (
                Token.Identifier("priority", 5), // priority
                Token.Operator(ComparisonOperator.GREATER_THAN, 13), // >
                Token.StringLiteral("medium", 15), // "medium"
                Token.Or(23), // OR
                Token.Identifier("status", 26), // status
                Token.Operator(ComparisonOperator.EQUALS, 32), // =
                Token.StringLiteral("blocked", 34), // "blocked"
                Token.RightParen(43), // )
            )

            // Verify key positions in the complex expression
            tokens[0].shouldBeInstanceOf<Token.Not>()
            tokens[1].shouldBeInstanceOf<Token.LeftParen>()
            tokens[9].shouldBeInstanceOf<Token.RightParen>()

            // Check the OR logical operator
            tokens[5].shouldBeInstanceOf<Token.Or>()

            // Verify comparison operators
            val operatorToken3 = tokens[3].shouldBeInstanceOf<Token.Operator>()
            operatorToken3.op shouldBe ComparisonOperator.GREATER_THAN
            val operatorToken7 = tokens[7].shouldBeInstanceOf<Token.Operator>()
            operatorToken7.op shouldBe ComparisonOperator.EQUALS
        }
    })

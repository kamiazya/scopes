package io.github.kamiazya.scopes.scopemanagement.application.query

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class AspectQueryParserTest : DescribeSpec({
    describe("AspectQueryParser") {
        val parser = AspectQueryParser()
        
        describe("IN operator") {
            it("should parse IN operator with comma-separated values") {
                val result = parser.parse("status IN open,closed")
                result.shouldBeRight()
                
                val ast = result.getOrNull()!!
                ast shouldBe AspectQueryAST.Comparison("status", ComparisonOperator.IN, "open,closed")
            }
            
            it("should parse IN operator with single value") {
                val result = parser.parse("status IN open")
                result.shouldBeRight()
                
                val ast = result.getOrNull()!!
                ast shouldBe AspectQueryAST.Comparison("status", ComparisonOperator.IN, "open")
            }
            
        }
        
        describe("CONTAINS operator") {
            it("should parse CONTAINS operator") {
                val result = parser.parse("status CONTAINS pen")
                result.shouldBeRight()
                
                val ast = result.getOrNull()!!
                ast shouldBe AspectQueryAST.Comparison("status", ComparisonOperator.CONTAINS, "pen")
            }
        }
        
        describe("EXISTS operator") {
            it("should parse EXISTS operator") {
                val result = parser.parse("priority EXISTS")
                result.shouldBeRight()
                
                val ast = result.getOrNull()!!
                ast shouldBe AspectQueryAST.Comparison("priority", ComparisonOperator.EXISTS, "")
            }
        }
        
        describe("IS_NULL operator") {
            it("should parse IS_NULL operator") {
                val result = parser.parse("assignee IS_NULL")
                result.shouldBeRight()
                
                val ast = result.getOrNull()!!
                ast shouldBe AspectQueryAST.Comparison("assignee", ComparisonOperator.IS_NULL, "")
            }
        }
    }
})
package io.github.kamiazya.scopes.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withNameEndingWith
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.DescribeSpec

/**
 * Konsist rules for error assertion best practices
 * Ensures tests properly validate error types and properties
 */
class ErrorAssertionRulesTest :
    DescribeSpec({
        describe("Error Assertion Rules") {

            it("error assertions should be specific about error types") {
                Konsist.scopeFromTest()
                    .files
                    .withNameEndingWith("Test.kt")
                    .flatMap { it.functions() }
                    .filter { function ->
                        // Find assertions about Left/error results
                        function.text.contains(".isLeft()") ||
                            function.text.contains(".leftOrNull()") ||
                            function.text.contains("is Either.Left")
                    }
                    .assertTrue { function ->
                        // Should check specific error type or have explanation
                        val hasSpecificCheck =
                            function.text.contains("shouldBeInstanceOf") ||
                                function.text.contains("shouldBe") &&
                                function.text.contains("Error") ||
                                function.text.contains("when (") &&
                                function.text.contains("is ") ||
                                function.text.contains(".message") ||
                                function.text.contains(".cause")

                        val hasExplanation =
                            function.text.contains("// Any error is acceptable") ||
                                function.text.contains("// Error type doesn't matter") ||
                                function.text.contains("// Just checking failure")

                        hasSpecificCheck || hasExplanation
                    }
            }

            it("tests should validate error properties when applicable") {
                Konsist.scopeFromTest()
                    .files
                    .withNameEndingWith("RepositoryTest.kt")
                    .flatMap { it.functions() }
                    .filter { function ->
                        // Tests that deal with specific error scenarios
                        function.name?.contains("error") == true ||
                            function.name?.contains("fail") == true ||
                            function.name?.contains("invalid") == true
                    }
                    .assertTrue { function ->
                        // Should validate error details
                        val validatesErrorDetails =
                            function.text.contains(".message") ||
                                function.text.contains(".operation") ||
                                function.text.contains(".aggregateId") ||
                                function.text.contains(".errorCode") ||
                                function.text.contains(".cause") ||
                                function.text.contains("shouldBeInstanceOf")

                        // Or just checks that it fails (for simple cases)
                        val simpleFailureCheck =
                            function.text.contains("isLeft()") ||
                                function.text.contains("shouldThrow")

                        validatesErrorDetails || simpleFailureCheck
                    }
            }

            it("exception assertions should use proper matchers") {
                Konsist.scopeFromTest()
                    .files
                    .flatMap { it.functions() }
                    .filter {
                        it.text.contains("try {") &&
                            it.text.contains("} catch")
                    }
                    .assertTrue { function ->
                        // Prefer shouldThrow over try-catch for cleaner tests
                        val hasComment =
                            function.text.contains("// Need to validate exception state") ||
                                function.text.contains("// Complex exception handling")

                        // If using try-catch, should at least assert on the exception
                        val assertsOnException =
                            function.text.contains("shouldBe") ||
                                function.text.contains("assertEquals") ||
                                function.text.contains("assertTrue") ||
                                function.text.contains("assertNotNull")

                        hasComment || assertsOnException
                    }
            }

            it("error path tests should cover all error types") {
                Konsist.scopeFromTest()
                    .files
                    .withNameEndingWith("RepositoryTest.kt")
                    .flatMap { it.classes() }
                    .filter { it.name?.contains("Test") == true }
                    .assertTrue { testClass ->
                        // Should have comprehensive error testing
                        val hasErrorTests =
                            testClass.functions().any {
                                it.name?.contains("error") == true ||
                                    it.name?.contains("fail") == true
                            }

                        val hasErrorHandlingSection =
                            testClass.text.contains("describe(\"error") ||
                                testClass.text.contains("context(\"error") ||
                                testClass.text.contains("// Error scenarios")

                        hasErrorTests || hasErrorHandlingSection
                    }
            }

            it("custom error types should have corresponding test coverage") {
                // Find all custom error classes
                val errorClasses = Konsist.scopeFromProduction()
                    .classes()
                    .filter {
                        it.name?.endsWith("Error") == true ||
                            it.name?.endsWith("Exception") == true
                    }
                    .map { it.name }
                    .toSet()

                // Check that tests reference these error types
                Konsist.scopeFromTest()
                    .files
                    .withNameEndingWith("Test.kt")
                    .assertTrue { file ->
                        // Test files should reference error types they're testing
                        val referencedErrors = errorClasses.filter { errorClass ->
                            file.text.contains(errorClass ?: "")
                        }

                        // Either references some errors or doesn't need to
                        referencedErrors.isNotEmpty() ||
                            file.path?.contains("konsist") == true ||
                            // Meta tests
                            file.path?.contains("integration") == true // Integration tests
                    }
            }
        }
    })

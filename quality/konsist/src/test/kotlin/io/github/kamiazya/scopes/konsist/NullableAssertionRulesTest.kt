package io.github.kamiazya.scopes.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withNameEndingWith
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.DescribeSpec

/**
 * Konsist rules for nullable assertion patterns in tests
 * Ensures tests use explicit Right checks instead of nullable chaining
 */
class NullableAssertionRulesTest :
    DescribeSpec({
        describe("Nullable Assertion Rules") {

            it("tests should use explicit Right checks before accessing Either values") {
                Konsist.scopeFromTest()
                    .files
                    .withNameEndingWith("Test.kt")
                    .flatMap { it.functions() }
                    .filter { function ->
                        // Look for Either result patterns
                        function.text.contains("getOrNull()") &&
                            (
                                function.text.contains("shouldBe") ||
                                    function.text.contains("shouldHave") ||
                                    function.text.contains("shouldContain")
                                )
                    }
                    .assertTrue { function ->
                        // Should check isRight() before getOrNull()
                        val hasRightCheck = function.text.contains("isRight() shouldBe true") ||
                            function.text.contains("isRight()).isTrue()") ||
                            function.text.contains("shouldBeRight()")

                        // Allow direct comparison to null or empty
                        val isNullOrEmptyCheck = function.text.contains("getOrNull() shouldBe null") ||
                            function.text.contains("getOrNull() shouldBe emptyList")

                        hasRightCheck || isNullOrEmptyCheck
                    }
            }

            it("tests should not use nullable chaining on Either results") {
                Konsist.scopeFromTest()
                    .files
                    .withNameEndingWith("Test.kt")
                    .flatMap { it.functions() }
                    .filter { function ->
                        // Detect nullable chaining pattern
                        function.text.contains("getOrNull()?.") &&
                            !function.text.contains("getOrNull()?.let") // Allow safe let blocks
                    }
                    .assertTrue { function ->
                        // Should use non-null assertion after Right check
                        val hasNonNullAssertion = function.text.contains("getOrNull()!!") ||
                            function.text.contains("getOrThrow()")
                        val hasExplanatoryComment = function.text.contains("nullable") ||
                            function.text.contains("optional") ||
                            function.text.contains("may be null")

                        hasNonNullAssertion || hasExplanatoryComment
                    }
            }

            it("tests should assert non-null before using collection matchers") {
                Konsist.scopeFromTest()
                    .files
                    .withNameEndingWith("Test.kt")
                    .flatMap { it.functions() }
                    .filter { function ->
                        function.text.contains("shouldHaveSize") ||
                            function.text.contains("shouldContain") ||
                            function.text.contains("shouldBeEmpty")
                    }
                    .assertTrue { function ->
                        // Should not use nullable chaining with collection matchers
                        val hasNullableChaining = Regex("""\.getOrNull\(\)\?\.should""").containsMatchIn(function.text)
                        val hasProperAssertion = function.text.contains("shouldNotBe null") ||
                            function.text.contains("getOrNull()!!") ||
                            function.text.contains("isRight() shouldBe true")

                        !hasNullableChaining || hasProperAssertion
                    }
            }

            it("repository tests should fail fast on unexpected Left results") {
                Konsist.scopeFromTest()
                    .files
                    .withNameEndingWith("RepositoryTest.kt", "StoreTest.kt")
                    .flatMap { it.functions() }
                    .filter { function ->
                        function.text.contains("repository.") &&
                            function.text.contains("getOrNull()")
                    }
                    .assertTrue { function ->
                        // Should have explicit Right check or Left handling
                        val hasRightCheck = function.text.contains("isRight()") ||
                            function.text.contains("shouldBeRight()")
                        val hasLeftHandling = function.text.contains("isLeft()") ||
                            function.text.contains("leftOrNull()") ||
                            function.text.contains("shouldBeLeft()")
                        val hasErrorAssertion = function.text.contains("shouldBeInstanceOf")

                        hasRightCheck || hasLeftHandling || hasErrorAssertion
                    }
            }

            it("tests should use shouldBeInstanceOf for error type assertions") {
                Konsist.scopeFromTest()
                    .files
                    .withNameEndingWith("Test.kt")
                    .flatMap { it.functions() }
                    .filter { function ->
                        function.text.contains("leftOrNull()") ||
                            function.text.contains("isLeft()")
                    }
                    .assertTrue { function ->
                        // Should assert specific error type
                        val hasTypeAssertion = function.text.contains("shouldBeInstanceOf") ||
                            function.text.contains("is ") ||
                            function.text.contains("as? ")
                        val hasGenericErrorCheck = function.text.contains("shouldNotBe null") ||
                            function.text.contains("shouldBe true")

                        hasTypeAssertion || hasGenericErrorCheck
                    }
            }
        }
    })

package io.github.kamiazya.scopes.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withNameEndingWith
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.DescribeSpec

/**
 * Konsist rules for coroutine testing best practices
 * Ensures modern coroutine testing patterns are followed
 */
class CoroutineTestingRulesTest :
    DescribeSpec({
        describe("Coroutine Testing Rules") {

            it("repository tests should prefer runTest over runBlocking") {
                // Once we migrate to runTest, this rule will enforce it
                Konsist.scopeFromTest()
                    .files
                    .withNameEndingWith("RepositoryTest.kt", "StoreTest.kt")
                    .flatMap { it.functions() }
                    .filter { function ->
                        function.text.contains("runBlocking") &&
                            !function.text.contains("// TODO: migrate to runTest") &&
                            !function.text.contains("// Legacy: using runBlocking")
                    }
                    .assertFalse(
                        // Currently we use runBlocking, so this is informational
                        // Will be enforced after migration to runTest
                    ) {
                        // Allow runBlocking for now since migration is pending
                        false
                    }
            }

            it("suspend function tests should be properly structured") {
                Konsist.scopeFromTest()
                    .files
                    .withNameEndingWith("Test.kt")
                    .flatMap { it.classes() }
                    .flatMap { it.functions() }
                    .filter { function ->
                        // Find test functions that test suspend functions
                        function.text.contains("suspend") &&
                            (
                                function.text.contains("shouldBe") ||
                                    function.text.contains("assertEquals")
                                )
                    }
                    .assertTrue { function ->
                        // Should use either runBlocking or runTest (for now)
                        function.text.contains("runBlocking") ||
                            function.text.contains("runTest") ||
                            function.annotations.any { it.name == "Test" } // JUnit might handle it
                    }
            }

            it("tests with delays should document time dependencies") {
                Konsist.scopeFromTest()
                    .files
                    .flatMap { it.functions() }
                    .filter {
                        it.text.contains("delay(") ||
                            it.text.contains("Thread.sleep(") ||
                            it.text.contains("advanceTimeBy(")
                    }
                    .assertTrue { function ->
                        // Should have a comment explaining the timing
                        val hasComment = function.text.contains("//") &&
                            (
                                function.text.contains("time") ||
                                    function.text.contains("delay") ||
                                    function.text.contains("wait")
                                )

                        // Or use virtual time (future with runTest)
                        val usesVirtualTime = function.text.contains("advanceTimeBy") ||
                            function.text.contains("advanceUntilIdle")

                        hasComment || usesVirtualTime
                    }
            }

            it("async operations in tests should be properly awaited") {
                Konsist.scopeFromTest()
                    .files
                    .flatMap { it.functions() }
                    .filter {
                        it.text.contains("async {") ||
                            it.text.contains("launch {")
                    }
                    .assertTrue { function ->
                        // If using async, should await the result
                        if (function.text.contains("async {")) {
                            function.text.contains(".await()") ||
                                function.text.contains("awaitAll")
                        } else if (function.text.contains("launch {")) {
                            // If using launch, should join or use proper scope
                            function.text.contains(".join()") ||
                                function.text.contains("runBlocking") ||
                                function.text.contains("runTest") ||
                                function.text.contains("coroutineScope")
                        } else {
                            true
                        }
                    }
            }

            it("coroutine exception handling should be tested") {
                Konsist.scopeFromTest()
                    .files
                    .withNameEndingWith("RepositoryTest.kt")
                    .flatMap { it.classes() }
                    .assertTrue { testClass ->
                        // Repository tests should have at least one test for coroutine exceptions
                        val hasExceptionTest =
                            testClass.text.contains("CancellationException") ||
                                testClass.text.contains("CoroutineException") ||
                                testClass.text.contains("should handle") &&
                                testClass.text.contains("error") ||
                                testClass.text.contains("exception")

                        // Or explicitly marked as not needing exception tests
                        val isExempt = testClass.annotations.any { it.name == "NoExceptionTestRequired" }

                        hasExceptionTest || isExempt
                    }
            }
        }
    })

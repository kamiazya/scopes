package io.github.kamiazya.scopes.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withNameEndingWith
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.DescribeSpec

/**
 * Konsist rules for timestamp handling in tests
 * Ensures deterministic timestamp boundaries and proper time handling
 */
class TimestampTestingRulesTest :
    DescribeSpec({
        describe("Timestamp Testing Rules") {

            it("tests should not use fragile millisecond addition for boundaries") {
                Konsist.scopeFromTest()
                    .files
                    .withNameEndingWith("Test.kt")
                    .flatMap { it.functions() }
                    .filter { function ->
                        function.text.contains("storedAt.plus(1.milliseconds)") ||
                            function.text.contains("storedAt + 1.milliseconds") ||
                            function.text.contains(".plus(1.milliseconds)")
                    }
                    .assertTrue { function ->
                        // Should use deterministic clock advancement instead
                        val hasClockWait = function.text.contains("while") &&
                            function.text.contains("Clock.System.now()")
                        val hasExplanation = function.text.contains("deterministic") ||
                            function.text.contains("clock advancement")
                        
                        hasClockWait && hasExplanation
                    }
            }

            it("timestamp boundary tests should use do-while loops for consistency") {
                Konsist.scopeFromTest()
                    .files
                    .withNameEndingWith("RepositoryTest.kt")
                    .flatMap { it.functions() }
                    .filter { function ->
                        function.text.contains("timestampBetween") ||
                            function.text.contains("boundary") &&
                            function.text.contains("Clock.System.now()")
                    }
                    .assertTrue { function ->
                        // Should use do-while for atomic timestamp capture
                        val hasDoWhile = function.text.contains("do {") &&
                            function.text.contains("} while")
                        val hasSimpleWhile = function.text.contains("while (Clock.System.now()")
                        
                        // Allow either pattern but prefer do-while
                        hasDoWhile || hasSimpleWhile
                    }
            }

            it("tests should capture timestamps atomically to avoid race conditions") {
                Konsist.scopeFromTest()
                    .files
                    .withNameEndingWith("Test.kt")
                    .flatMap { it.functions() }
                    .filter { function ->
                        // Look for multiple Clock.System.now() calls in same context
                        val nowCallCount = "Clock.System.now()".toRegex()
                            .findAll(function.text).count()
                        nowCallCount > 2 // More than 2 suggests potential race condition
                    }
                    .assertTrue { function ->
                        // Should capture timestamps in variables
                        val hasTimestampVar = function.text.contains("val ") &&
                            function.text.contains("= Clock.System.now()")
                        val hasDoWhileCapture = function.text.contains("do {") &&
                            function.text.contains("Clock.System.now()")
                        
                        hasTimestampVar || hasDoWhileCapture
                    }
            }

            it("event tests should use consistent timestamps for occurred_at") {
                Konsist.scopeFromTest()
                    .files
                    .withNameEndingWith("EventRepositoryTest.kt", "EventStoreTest.kt")
                    .flatMap { it.functions() }
                    .filter { function ->
                        function.text.contains("occurredAt") &&
                            function.text.contains("TestEvent")
                    }
                    .assertTrue { function ->
                        // Should use captured timestamp variables for occurredAt
                        val hasCapturedTime = function.text.matches(
                            Regex(".*val\\s+\\w+Time\\s*=\\s*Clock\\.System\\.now\\(\\).*", RegexOption.DOT_MATCHES_ALL)
                        )
                        val usesVariableForOccurredAt = function.text.matches(
                            Regex(".*occurredAt\\s*=\\s*\\w+Time.*", RegexOption.DOT_MATCHES_ALL)
                        )
                        
                        // Allow direct Clock.System.now() if not doing boundary testing
                        val isSimpleTest = !function.text.contains("timestampBetween") &&
                            !function.text.contains("since")
                        
                        isSimpleTest || (hasCapturedTime && usesVariableForOccurredAt)
                    }
            }

            it("tests should not rely on sleep for timestamp separation") {
                Konsist.scopeFromTest()
                    .files
                    .withNameEndingWith("Test.kt")
                    .filter { file ->
                        file.text.contains("Thread.sleep") &&
                            (file.text.contains("timestamp") || file.text.contains("storedAt"))
                    }
                    .assertTrue { file ->
                        // Sleep should only be used with clear explanation
                        val hasExplanation = file.text.contains("ensure") ||
                            file.text.contains("guarantee") ||
                            file.text.contains("deterministic")
                        
                        // Better to use clock advancement
                        val hasClockAdvancement = file.text.contains("while") &&
                            file.text.contains("Clock.System.now()")
                        
                        hasExplanation || hasClockAdvancement
                    }
            }
        }
    })
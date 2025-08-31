package io.github.kamiazya.scopes.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withNameEndingWith
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.DescribeSpec

/**
 * Konsist rules for Event Sourcing architecture patterns
 * These rules help prevent common timestamp and event handling issues
 */
class EventSourcingArchitectureTest :
    DescribeSpec({
        describe("Event Sourcing Architecture Rules") {

            it("repository tests should use millisecond precision for timestamps") {
                // Repositories working with SQLite should use millisecond precision
                Konsist.scopeFromTest()
                    .files
                    .withNameEndingWith("RepositoryTest.kt")
                    .flatMap { it.classes() }
                    .flatMap { it.functions() }
                    .filter { function ->
                        function.text.contains("Clock.System.now()") &&
                            function.text.contains("occurredAt") &&
                            !function.text.contains("toEpochMilliseconds")
                    }
                    .assertTrue { function ->
                        // Check if the function properly handles millisecond precision
                        val hasMillisecondConversion = function.text.contains("fromEpochMilliseconds") ||
                            function.text.contains("toEpochMilliseconds")
                        val hasComment = function.text.contains("millisecond") ||
                            function.text.contains("SQLite")

                        hasMillisecondConversion || hasComment
                    }
            }

            it("event repository tests should not use future timestamps") {
                // Prevent validation errors from future timestamps
                Konsist.scopeFromTest()
                    .files
                    .withNameEndingWith("EventRepositoryTest.kt", "EventStoreTest.kt")
                    .flatMap { it.classes() }
                    .flatMap { it.functions() }
                    .filter { function ->
                        function.text.contains(".plus(") &&
                            (
                                function.text.contains("seconds") ||
                                    function.text.contains("minutes") ||
                                    function.text.contains("hours")
                                )
                    }
                    .assertTrue { function ->
                        // Allow if it's subtracting time or has explanatory comment
                        val hasMinus = function.text.contains(".minus(") ||
                            function.text.contains("- ")
                        val hasExplanatoryComment = function.text.contains("past") ||
                            function.text.contains("avoid") ||
                            function.text.contains("validation")

                        hasMinus || hasExplanatoryComment
                    }
            }

            it("repository tests should use Thread.sleep sparingly with proper documentation") {
                // Thread.sleep should be documented when used for timestamp separation
                Konsist.scopeFromTest()
                    .files
                    .withNameEndingWith("RepositoryTest.kt")
                    .flatMap { it.classes() }
                    .flatMap { it.functions() }
                    .filter { it.text.contains("Thread.sleep") }
                    .assertTrue { function ->
                        val sleepIndex = function.text.indexOf("Thread.sleep")
                        val textBeforeSleep = function.text.substring(0, sleepIndex)
                        val lastNewlineBeforeSleep = textBeforeSleep.lastIndexOf("\n")
                        val lineWithSleep = if (lastNewlineBeforeSleep >= 0) {
                            function.text.substring(lastNewlineBeforeSleep, sleepIndex + 100.coerceAtMost(function.text.length - sleepIndex))
                        } else {
                            function.text.substring(0, sleepIndex + 100.coerceAtMost(function.text.length - sleepIndex))
                        }

                        // Check if there's a comment near the Thread.sleep
                        lineWithSleep.contains("//") || lineWithSleep.contains("/*")
                    }
            }

            it("event tests should verify both occurred_at and stored_at handling") {
                // Ensure tests understand the difference between occurred_at and stored_at
                Konsist.scopeFromTest()
                    .files
                    .withNameEndingWith("EventRepositoryTest.kt", "EventStoreTest.kt")
                    .flatMap { it.classes() }
                    .filter { it.name?.contains("Test") == true }
                    .assertTrue { testClass ->
                        val hasOccurredAtTest = testClass.text.contains("occurredAt")
                        val hasStoredAtTest = testClass.text.contains("stored_at") ||
                            testClass.text.contains("storedAt")

                        // At least one test should mention both
                        hasOccurredAtTest && hasStoredAtTest
                    }
            }

            it("repository implementations should validate event timestamps") {
                // Ensure repositories validate that events aren't from the future
                Konsist.scopeFromProduction()
                    .files
                    .withNameEndingWith("Repository.kt")
                    .flatMap { it.classes() }
                    .filter {
                        it.name?.contains("Event") == true &&
                            it.name?.contains("Test") != true
                    }
                    .flatMap { it.functions() }
                    .filter { it.name == "store" || it.name == "save" }
                    .assertTrue { function ->
                        val hasValidation = function.text.contains("Clock.System.now()") ||
                            function.text.contains("validate") ||
                            function.text.contains("check") ||
                            function.text.contains("require")
                        val hasErrorHandling = function.text.contains("Either") ||
                            function.text.contains("try") ||
                            function.text.contains("catch")

                        hasValidation || hasErrorHandling
                    }
            }

            it("test events should use consistent timestamp patterns") {
                // Ensure test events follow consistent patterns
                Konsist.scopeFromTest()
                    .files
                    .flatMap { it.classes() }
                    .filter { it.name?.endsWith("Event") == true }
                    .assertTrue { eventClass ->
                        val hasOccurredAt = eventClass.properties().any {
                            it.name == "occurredAt" && it.type?.name?.contains("Instant") == true
                        }

                        // If it's a domain event, it should have occurredAt
                        if (eventClass.hasParentWithName("DomainEvent")) {
                            hasOccurredAt
                        } else {
                            true // Non-domain events don't need occurredAt
                        }
                    }
            }
        }
    })

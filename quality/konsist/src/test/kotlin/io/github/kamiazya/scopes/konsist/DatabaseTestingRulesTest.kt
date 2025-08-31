package io.github.kamiazya.scopes.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withNameEndingWith
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.DescribeSpec

/**
 * Konsist rules for database testing patterns
 * Prevents common SQLite and database testing issues
 */
class DatabaseTestingRulesTest :
    DescribeSpec({
        describe("Database Testing Rules") {

            it("SQLDelight repository tests should properly manage database lifecycle") {
                Konsist.scopeFromTest()
                    .files
                    .withNameEndingWith("RepositoryTest.kt")
                    .filter { it.text.contains("SqlDelight") }
                    .flatMap { it.classes() }
                    .assertTrue { testClass ->
                        val hasBeforeEach = testClass.text.contains("beforeEach") ||
                            testClass.text.contains("beforeTest") ||
                            testClass.text.contains("@BeforeEach")
                        val hasAfterEach = testClass.text.contains("afterEach") ||
                            testClass.text.contains("afterTest") ||
                            testClass.text.contains("@AfterEach")
                        val hasDriverClose = testClass.text.contains("driver.close()") ||
                            testClass.text.contains("database.close()")

                        hasBeforeEach && hasAfterEach && hasDriverClose
                    }
            }

            it("repository tests should use in-memory databases") {
                Konsist.scopeFromTest()
                    .files
                    .withNameEndingWith("RepositoryTest.kt")
                    .filter { it.text.contains("JdbcSqliteDriver") }
                    .assertTrue { file ->
                        file.text.contains("IN_MEMORY") ||
                            file.text.contains(":memory:") ||
                            file.text.contains("inMemory")
                    }
            }

            it("repository tests should not share state between tests") {
                Konsist.scopeFromTest()
                    .files
                    .withNameEndingWith("RepositoryTest.kt")
                    .flatMap { it.classes() }
                    .flatMap { it.properties() }
                    .filter {
                        it.name?.contains("repository") == true ||
                            it.name?.contains("database") == true ||
                            it.name?.contains("driver") == true
                    }
                    .assertTrue { property ->
                        val hasLateinit = property.modifiers.any {
                            it.name == "lateinit"
                        }
                        val isVal = property.text.contains("val ")
                        val isVar = property.text.contains("var ")

                        // Should be lateinit var, not val
                        hasLateinit || (isVar && !isVal)
                    }
            }

            it("event type comparisons should use fully qualified names") {
                Konsist.scopeFromTest()
                    .files
                    .withNameEndingWith("EventRepositoryTest.kt", "EventStoreTest.kt")
                    .flatMap { it.classes() }
                    .flatMap { it.functions() }
                    .filter {
                        it.text.contains("EventType(") &&
                            it.text.contains("shouldBe")
                    }
                    .assertTrue { function ->
                        // Check if EventType contains dots (package separator)
                        val eventTypePattern = """EventType\("([^"]*)"\)""".toRegex()
                        val matches = eventTypePattern.findAll(function.text)

                        matches.all { match ->
                            val className = match.groupValues[1]
                            className.contains(".") || className == "TestEvent" // Allow TestEvent for now
                        }
                    }
            }

            it("repository tests should handle timezone and precision issues") {
                Konsist.scopeFromTest()
                    .files
                    .withNameEndingWith("RepositoryTest.kt")
                    .flatMap { it.classes() }
                    .flatMap { it.functions() }
                    .filter {
                        it.text.contains("Instant") &&
                            (it.text.contains("shouldBe") || it.text.contains("assertEquals"))
                    }
                    .assertTrue { function ->
                        // Check for precision handling
                        val hasPrecisionHandling =
                            function.text.contains("toEpochMilliseconds") ||
                                function.text.contains("fromEpochMilliseconds") ||
                                function.text.contains("truncatedTo") ||
                                function.text.contains("shouldNotBe null") || // Loose comparison
                                function.text.contains("shouldBeCloseTo") ||
                                function.text.contains("within")

                        val hasExplanatoryComment =
                            function.text.contains("precision") ||
                                function.text.contains("millisecond") ||
                                function.text.contains("SQLite")

                        hasPrecisionHandling || hasExplanatoryComment
                    }
            }

            it("repository tests should not hardcode sequence numbers") {
                Konsist.scopeFromTest()
                    .files
                    .withNameEndingWith("RepositoryTest.kt")
                    .flatMap { it.classes() }
                    .flatMap { it.functions() }
                    .filter {
                        it.text.contains("sequenceNumber") &&
                            it.text.contains("shouldBe")
                    }
                    .assertTrue { function ->
                        // Should not have hardcoded sequence numbers like shouldBe 1L
                        val hasHardcodedNumber = function.text.contains("shouldBe 1L") ||
                            function.text.contains("shouldBe 0L") ||
                            function.text.contains("== 1L") ||
                            function.text.contains("== 0L")

                        if (hasHardcodedNumber) {
                            // Allow if it's in an isolated test context
                            function.text.contains("DISTANT_PAST") || // Likely the only event
                                function.text.contains("first()") // Explicitly getting first
                        } else {
                            true
                        }
                    }
            }

            it("getEventsSince tests should understand stored_at vs occurred_at") {
                Konsist.scopeFromTest()
                    .files
                    .withNameEndingWith("EventRepositoryTest.kt", "EventStoreTest.kt")
                    .flatMap { it.classes() }
                    .flatMap { it.functions() }
                    .filter {
                        it.name?.contains("getEventsSince") == true ||
                            it.text.contains("getEventsSince")
                    }
                    .assertTrue { function ->
                        val hasStoredAtMention = function.text.contains("stored_at") ||
                            function.text.contains("storedAt") ||
                            function.text.contains("storage time")

                        val hasThreadSleep = function.text.contains("Thread.sleep")

                        // Should either mention stored_at or use Thread.sleep to control timing
                        hasStoredAtMention || hasThreadSleep
                    }
            }
        }
    })

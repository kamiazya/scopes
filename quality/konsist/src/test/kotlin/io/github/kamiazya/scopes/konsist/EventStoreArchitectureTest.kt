package io.github.kamiazya.scopes.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withNameEndingWith
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.DescribeSpec

/**
 * Architecture tests for the Event Store bounded context to ensure
 * proper implementation patterns and prevent regressions.
 */
class EventStoreArchitectureTest :
    DescribeSpec({

        describe("Event serialization should be properly implemented") {

            it("All event classes should be serializable") {
                Konsist
                    .scopeFromProject()
                    .classes()
                    .withNameEndingWith("Event")
                    .filter {
                        it.packagee?.name?.contains("eventstore") == true &&
                            it.packagee?.name?.contains("domain") == true &&
                            !it.name.contains("Command") // Exclude command classes like StoreEvent
                    }
                    .assertTrue {
                        it.annotations.any { annotation ->
                            annotation.name == "Serializable" ||
                                annotation.fullyQualifiedName == "kotlinx.serialization.Serializable"
                        }
                    }
            }

            it("Repositories should never return unknown or default values for critical fields") {
                Konsist
                    .scopeFromProject()
                    .classes()
                    .withNameEndingWith("Repository")
                    .filter { it.packagee?.name?.contains("eventstore") == true }
                    .flatMap { it.functions() }
                    .assertTrue { function ->
                        // Check that function doesn't contain problematic patterns
                        val functionText = function.text
                        !functionText.contains("\"unknown\"") &&
                            !functionText.contains("\"Unknown\"") &&
                            !functionText.contains("UnknownEvent") &&
                            !functionText.contains("?: \"default\"") &&
                            !functionText.contains("?: \"Unknown\"")
                    }
            }
        }

        describe("Event Store error handling") {

            it("Event Store errors should be properly mapped to application errors") {
                Konsist
                    .scopeFromProject()
                    .classes()
                    .withNameEndingWith("Handler")
                    .filter { it.packagee?.name?.contains("eventstore.application") == true }
                    .flatMap { it.functions() }
                    .filter { it.name == "invoke" }
                    .assertTrue { function ->
                        // Handler should properly map domain errors to application errors
                        val functionText = function.text
                        functionText.contains("mapLeft") ||
                            functionText.contains("EventStoreApplicationError") ||
                            !functionText.contains("return Either.Left")
                    }
            }
        }

        describe("Event metadata consistency") {

            it("All domain events should have proper event type handling") {
                Konsist
                    .scopeFromProject()
                    .files
                    .filter {
                        it.path.contains("eventstore") &&
                            it.path.contains("repository") &&
                            it.path.contains("infrastructure") // Only check implementations, not interfaces
                    }
                    .flatMap { it.functions() }
                    .filter { it.name == "store" || it.name == "save" }
                    .assertTrue { function ->
                        val functionText = function.text
                        // Should use proper event type resolution
                        (
                            functionText.contains("event::class.qualifiedName") ||
                                functionText.contains("event::class.simpleName")
                            ) &&
                            // Should fail fast on missing event type
                            (
                                functionText.contains("throw IllegalArgumentException") ||
                                    functionText.contains("?: throw")
                                )
                    }
            }
        }
    })

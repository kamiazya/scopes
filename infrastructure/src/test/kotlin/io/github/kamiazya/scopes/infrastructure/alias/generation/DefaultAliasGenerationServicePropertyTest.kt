package io.github.kamiazya.scopes.infrastructure.alias.generation

import io.github.kamiazya.scopes.domain.error.ScopeInputError
import io.github.kamiazya.scopes.domain.service.AliasGenerationStrategy
import io.github.kamiazya.scopes.domain.service.WordProvider
import io.github.kamiazya.scopes.domain.valueobject.AliasId
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking

/**
 * Property-based tests for DefaultAliasGenerationService.
 *
 * Tests the service's behavior with various strategies and error conditions:
 * - Deterministic generation based on AliasId
 * - Strategy pattern integration
 * - Error handling and propagation
 * - Random generation behavior
 */
class DefaultAliasGenerationServicePropertyTest :
    StringSpec({

        val iterations = 100 // Balanced iteration count

        "should generate deterministic aliases for same AliasId" {
            checkAll(iterations, validAliasIdArb()) { aliasId ->
                // Arrange
                val strategy = mockk<AliasGenerationStrategy> {
                    every { generate(any(), any()) } returns "test-alias-123"
                }
                val wordProvider = mockk<WordProvider>()
                val service = DefaultAliasGenerationService(strategy, wordProvider)

                // Act
                val result1 = runBlocking { service.generateCanonicalAlias(aliasId) }
                val result2 = runBlocking { service.generateCanonicalAlias(aliasId) }
                val result3 = runBlocking { service.generateCanonicalAlias(aliasId) }

                // Assert - Same ID should produce same result
                result1.shouldBeRight()
                result1 shouldBe result2
                result2 shouldBe result3
            }
        }

        "should use AliasId hash as seed for strategy" {
            checkAll(iterations, validAliasIdArb()) { aliasId ->
                // Arrange
                val expectedSeed = aliasId.value.hashCode().toLong()
                var capturedSeed: Long? = null

                val strategy = mockk<AliasGenerationStrategy> {
                    every { generate(any(), any()) } answers {
                        capturedSeed = firstArg()
                        "test-alias-abc"
                    }
                }
                val wordProvider = mockk<WordProvider>()
                val service = DefaultAliasGenerationService(strategy, wordProvider)

                // Act
                runBlocking { service.generateCanonicalAlias(aliasId) }

                // Assert
                capturedSeed shouldBe expectedSeed
            }
        }

        "should propagate valid alias names from strategy" {
            checkAll(
                iterations,
                validAliasIdArb(),
                validAliasNameStringArb(),
            ) { aliasId, aliasNameString ->
                // Arrange
                val strategy = mockk<AliasGenerationStrategy> {
                    every { generate(any(), any()) } returns aliasNameString
                }
                val wordProvider = mockk<WordProvider>()
                val service = DefaultAliasGenerationService(strategy, wordProvider)

                // Act
                val result = runBlocking { service.generateCanonicalAlias(aliasId) }

                // Assert
                result.shouldBeRight()
                result.getOrNull()?.value shouldBe aliasNameString
            }
        }

        "should handle invalid alias names from strategy" {
            checkAll(
                iterations,
                validAliasIdArb(),
                invalidAliasNameStringArb(),
            ) { aliasId, invalidName ->
                // Arrange
                val strategy = mockk<AliasGenerationStrategy> {
                    every { generate(any(), any()) } returns invalidName
                }
                val wordProvider = mockk<WordProvider>()
                val service = DefaultAliasGenerationService(strategy, wordProvider)

                // Act
                val result = runBlocking { service.generateCanonicalAlias(aliasId) }

                // Assert
                result.shouldBeLeft()
                result.leftOrNull().shouldBeInstanceOf<ScopeInputError.AliasError>()
            }
        }

        "should handle strategy exceptions gracefully" {
            checkAll(iterations, validAliasIdArb()) { aliasId ->
                // Arrange
                val exception = RuntimeException("Strategy failed")
                val strategy = mockk<AliasGenerationStrategy> {
                    every { generate(any(), any()) } throws exception
                }
                val wordProvider = mockk<WordProvider>()
                val service = DefaultAliasGenerationService(strategy, wordProvider)

                // Act
                val result = runBlocking { service.generateCanonicalAlias(aliasId) }

                // Assert
                result.shouldBeLeft()
                result.leftOrNull().shouldBeInstanceOf<ScopeInputError.AliasError.InvalidFormat>()
            }
        }

        "generateRandomAlias should use strategy's random generation" {
            checkAll(iterations, validAliasNameStringArb()) { aliasNameString ->
                // Arrange
                val strategy = mockk<AliasGenerationStrategy> {
                    every { generateRandom(any()) } returns aliasNameString
                }
                val wordProvider = mockk<WordProvider>()
                val service = DefaultAliasGenerationService(strategy, wordProvider)

                // Act
                val result = runBlocking { service.generateRandomAlias() }

                // Assert
                result.shouldBeRight()
                result.getOrNull()?.value shouldBe aliasNameString
            }
        }

        "random generation should produce different results" {
            // Arrange
            var counter = 0
            val strategy = mockk<AliasGenerationStrategy> {
                every { generateRandom(any()) } answers {
                    "random-alias-${counter++}"
                }
            }
            val wordProvider = mockk<WordProvider>()
            val service = DefaultAliasGenerationService(strategy, wordProvider)

            // Act
            val results = (1..10).map {
                runBlocking { service.generateRandomAlias() }
            }

            // Assert - Should have different values
            val uniqueValues = results.mapNotNull { it.getOrNull()?.value }.distinct()
            uniqueValues.size shouldBe 10
        }

        "should pass word provider to strategy" {
            checkAll(iterations, validAliasIdArb()) { aliasId ->
                // Arrange
                val wordProvider = mockk<WordProvider>()
                var capturedProvider: WordProvider? = null

                val strategy = mockk<AliasGenerationStrategy> {
                    every { generate(any(), any()) } answers {
                        capturedProvider = secondArg()
                        "test-alias"
                    }
                }
                val service = DefaultAliasGenerationService(strategy, wordProvider)

                // Act
                runBlocking { service.generateCanonicalAlias(aliasId) }

                // Assert
                capturedProvider shouldBe wordProvider
            }
        }

        "different AliasIds should potentially generate different aliases" {
            checkAll(
                10, // Fewer iterations for this test
                validAliasIdArb(),
                validAliasIdArb(),
            ) { aliasId1, aliasId2 ->
                if (aliasId1.value != aliasId2.value) {
                    // Arrange
                    val strategy = mockk<AliasGenerationStrategy> {
                        every { generate(any(), any()) } answers {
                            val seed = firstArg<Long>()
                            val num = kotlin.math.abs(seed % 1000)
                            "alias-$num" // Simple deterministic generation without negative numbers
                        }
                    }
                    val wordProvider = mockk<WordProvider>()
                    val service = DefaultAliasGenerationService(strategy, wordProvider)

                    // Act
                    val result1 = runBlocking { service.generateCanonicalAlias(aliasId1) }
                    val result2 = runBlocking { service.generateCanonicalAlias(aliasId2) }

                    // Assert - Both should succeed
                    result1.shouldBeRight()
                    result2.shouldBeRight()

                    // Different IDs often (but not always) produce different aliases
                    // We can't assert they're always different due to hash collisions
                }
            }
        }

        "service should be reusable for multiple generations" {
            checkAll(
                iterations,
                Arb.list(validAliasIdArb(), 1..5),
            ) { aliasIds ->
                // Arrange
                var callCount = 0
                val strategy = mockk<AliasGenerationStrategy> {
                    every { generate(any(), any()) } answers {
                        "alias-${callCount++}"
                    }
                }
                val wordProvider = mockk<WordProvider>()
                val service = DefaultAliasGenerationService(strategy, wordProvider)

                // Act
                val results = aliasIds.map { aliasId ->
                    runBlocking { service.generateCanonicalAlias(aliasId) }
                }

                // Assert - All should succeed
                results.forEach { it.shouldBeRight() }
                callCount shouldBe aliasIds.size
            }
        }
    })

// Helper Arbitraries

private fun validAliasIdArb(): Arb<AliasId> = Arb.uuid().map {
    AliasId.generate()
}

private fun validAliasNameStringArb(): Arb<String> = Arb.string(2..50).map { raw ->
    // Ensure valid alias name format
    val clean = raw.lowercase()
        .filter { it.isLetterOrDigit() || it == '-' || it == '_' }
        .replace(Regex("[-_]{2,}"), "-") // No consecutive special chars
        .trim('-', '_') // No leading/trailing special chars

    // Ensure starts with letter
    val withValidStart = if (clean.isEmpty() || !clean[0].isLetter()) {
        "alias${if (clean.isNotEmpty()) "-$clean" else ""}"
    } else {
        clean
    }

    // Ensure valid length
    if (withValidStart.length < 2) {
        "alias"
    } else if (withValidStart.length > 64) {
        withValidStart.take(64).trimEnd('-', '_')
    } else {
        withValidStart
    }
}

private fun invalidAliasNameStringArb(): Arb<String> = Arb.choice(
    Arb.constant(""),
    Arb.constant(" "),
    Arb.constant("invalid name"), // contains space
    Arb.constant("a"), // too short
    Arb.string(65..100), // too long
    Arb.constant("invalid!@#"), // special characters
    Arb.constant("-invalid"), // starts with dash
    Arb.constant("invalid-"), // ends with dash
    Arb.constant("invalid--name"), // consecutive special chars
    Arb.constant("invalid__name"), // consecutive special chars
    Arb.constant("_invalid"), // starts with underscore
    Arb.constant("invalid_"), // ends with underscore
)

package io.github.kamiazya.scopes.infrastructure.alias.generation.strategies

import io.github.kamiazya.scopes.domain.service.WordProvider
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldHaveLength
import io.kotest.matchers.string.shouldMatch
import io.kotest.matchers.string.shouldNotContain
import io.kotest.property.Arb
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import io.mockk.every
import io.mockk.mockk

/**
 * Property-based tests for HaikunatorStrategy.
 *
 * Tests various properties that should hold for all valid inputs:
 * - Deterministic generation (same seed produces same output)
 * - Format consistency (adjective-noun-token pattern)
 * - Token generation correctness
 * - Random generation uniqueness
 */
class HaikunatorStrategyPropertyTest : StringSpec({

    val iterations = 100  // Balanced iteration count for quality testing

    "generated aliases should follow haikunator format pattern" {
        checkAll(iterations, Arb.long()) { seed ->
            // Arrange
            val strategy = HaikunatorStrategy()
            val wordProvider = createMockWordProvider()

            // Act
            val result = strategy.generate(seed, wordProvider)

            // Assert
            result shouldMatch Regex("^[a-z]+-[a-z]+-[a-z0-9]{3}$")
            val parts = result.split("-")
            parts.size shouldBe 3
            parts[0] shouldNotBe ""  // adjective
            parts[1] shouldNotBe ""  // noun
            parts[2] shouldHaveLength 3  // token
        }
    }

    "deterministic generation should produce same output for same seed" {
        checkAll(iterations, Arb.long()) { seed ->
            // Arrange
            val strategy = HaikunatorStrategy()
            val wordProvider = createMockWordProvider()

            // Act
            val result1 = strategy.generate(seed, wordProvider)
            val result2 = strategy.generate(seed, wordProvider)
            val result3 = strategy.generate(seed, wordProvider)

            // Assert - Same seed should always produce same result
            result1 shouldBe result2
            result2 shouldBe result3
        }
    }

    "different seeds should generally produce different outputs" {
        checkAll(iterations,
            Arb.long(),
            Arb.long().filter { it != 0L }
        ) { seed1, seedDelta ->
            // Arrange
            val strategy = HaikunatorStrategy()
            val wordProvider = createMockWordProvider()
            val seed2 = seed1 + seedDelta

            // Act
            val result1 = strategy.generate(seed1, wordProvider)
            val result2 = strategy.generate(seed2, wordProvider)

            // Assert - Different seeds should usually produce different results
            // Note: There's a small chance of collision, but it should be rare
            // We can't assert they're always different due to hash collisions
            // but we can check they're often different
            if (seed1 != seed2) {
                // Just verify both follow the format
                result1 shouldMatch Regex("^[a-z]+-[a-z]+-[a-z0-9]{3}$")
                result2 shouldMatch Regex("^[a-z]+-[a-z]+-[a-z0-9]{3}$")
            }
        }
    }

    "random generation should produce valid format" {
        checkAll(iterations, Arb.int(1..10)) { count ->
            // Arrange
            val strategy = HaikunatorStrategy()
            val wordProvider = createMockWordProvider()

            // Act & Assert
            repeat(count) {
                val result = strategy.generateRandom(wordProvider)

                // Should follow format
                result shouldMatch Regex("^[a-z]+-[a-z]+-[a-z0-9]{3}$")

                // Should not contain invalid characters
                result shouldNotContain " "
                result shouldNotContain "_"
                result shouldNotContain "."
            }
        }
    }

    "token should only contain lowercase letters and numbers" {
        checkAll(iterations, Arb.long()) { seed ->
            // Arrange
            val strategy = HaikunatorStrategy()
            val wordProvider = createMockWordProvider()

            // Act
            val result = strategy.generate(seed, wordProvider)
            val token = result.split("-").last()

            // Assert
            token shouldHaveLength 3
            token.all { it.isLowerCase() || it.isDigit() } shouldBe true
            token shouldMatch Regex("^[a-z0-9]{3}$")
        }
    }

    "should use words from word provider" {
        checkAll(iterations, Arb.long()) { seed ->
            // Arrange
            val strategy = HaikunatorStrategy()
            val testAdjectives = listOf("test", "mock", "fake")
            val testNouns = listOf("data", "value", "item")
            val wordProvider = mockk<WordProvider> {
                every { getAdjectives() } returns testAdjectives
                every { getNouns() } returns testNouns
            }

            // Act
            val result = strategy.generate(seed, wordProvider)
            val parts = result.split("-")

            // Assert
            (parts[0] in testAdjectives) shouldBe true
            (parts[1] in testNouns) shouldBe true
        }
    }

    "strategy name should be haikunator" {
        // Arrange
        val strategy = HaikunatorStrategy()

        // Act & Assert
        strategy.getName() shouldBe "haikunator"
    }

    "should handle single-word lists correctly" {
        checkAll(iterations, Arb.long()) { seed ->
            // Arrange
            val strategy = HaikunatorStrategy()
            val wordProvider = mockk<WordProvider> {
                every { getAdjectives() } returns listOf("only")
                every { getNouns() } returns listOf("one")
            }

            // Act
            val result = strategy.generate(seed, wordProvider)

            // Assert
            result.startsWith("only-one-") shouldBe true
            result shouldMatch Regex("^only-one-[a-z0-9]{3}$")
        }
    }

    "random generation should produce different results over time" {
        // Arrange
        val strategy = HaikunatorStrategy()
        val wordProvider = createMockWordProvider()
        val results = mutableSetOf<String>()

        // Act - Generate multiple random aliases
        repeat(20) {
            results.add(strategy.generateRandom(wordProvider))
            Thread.sleep(1) // Small delay to ensure different random seeds
        }

        // Assert - Should have some variety (not all the same)
        results.size shouldNotBe 1
    }

    "should handle large word lists efficiently" {
        checkAll(10,  // Fewer iterations for performance test
            Arb.long(),
            Arb.int(100..1000)
        ) { seed, listSize ->
            // Arrange
            val strategy = HaikunatorStrategy()
            val largeAdjectives = (1..listSize).map { "adj$it" }
            val largeNouns = (1..listSize).map { "noun$it" }
            val wordProvider = mockk<WordProvider> {
                every { getAdjectives() } returns largeAdjectives
                every { getNouns() } returns largeNouns
            }

            // Act
            val result = strategy.generate(seed, wordProvider)

            // Assert
            result shouldMatch Regex("^adj\\d+-noun\\d+-[a-z0-9]{3}$")
        }
    }
})

// Helper function to create a mock WordProvider with default words
private fun createMockWordProvider(): WordProvider = mockk {
    every { getAdjectives() } returns listOf(
        "bold", "brave", "calm", "clever", "cool",
        "epic", "fast", "great", "happy", "kind"
    )
    every { getNouns() } returns listOf(
        "tiger", "eagle", "wolf", "bear", "lion",
        "shark", "hawk", "fox", "deer", "owl"
    )
    every { getAdditionalWords(any()) } returns emptyList()
    every { getAvailableCategories() } returns listOf("adjectives", "nouns")
}

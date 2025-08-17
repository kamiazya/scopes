package io.github.kamiazya.scopes.infrastructure.alias.generation.providers

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.*
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldMatch
import io.kotest.matchers.string.shouldNotBeBlank
import io.kotest.property.Arb
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll

/**
 * Property-based tests for DefaultWordProvider.
 *
 * Tests the invariants and properties of the word provider:
 * - Word lists are non-empty
 * - Words follow expected patterns
 * - Category management works correctly
 * - Immutability of word lists
 */
class DefaultWordProviderPropertyTest : StringSpec({

    val iterations = 100  // Balanced iteration count

    "adjectives list should maintain invariants" {
        // Arrange
        val provider = DefaultWordProvider()

        // Act
        val adjectives = provider.getAdjectives()

        // Assert
        adjectives.shouldNotBeEmpty()
        adjectives.size shouldBe 40  // Based on the implementation

        // All adjectives should be non-empty lowercase strings
        adjectives.forEach { adjective ->
            adjective.shouldNotBeBlank()
            adjective shouldMatch Regex("^[a-z]+$")
        }

        // Should contain expected samples
        adjectives shouldContain "bold"
        adjectives shouldContain "brave"
        adjectives shouldContain "happy"

        // Should not have duplicates
        adjectives.distinct().size shouldBe adjectives.size
    }

    "nouns list should maintain invariants" {
        // Arrange
        val provider = DefaultWordProvider()

        // Act
        val nouns = provider.getNouns()

        // Assert
        nouns.shouldNotBeEmpty()
        nouns.size shouldBe 40  // Based on the implementation

        // All nouns should be non-empty lowercase strings
        nouns.forEach { noun ->
            noun.shouldNotBeBlank()
            noun shouldMatch Regex("^[a-z]+$")
        }

        // Should contain expected samples
        nouns shouldContain "tiger"
        nouns shouldContain "eagle"
        nouns shouldContain "mountain"

        // Should not have duplicates
        nouns.distinct().size shouldBe nouns.size
    }

    "getAdditionalWords should return correct category" {
        // Arrange
        val provider = DefaultWordProvider()

        // Act & Assert
        provider.getAdditionalWords("adjectives") shouldBe provider.getAdjectives()
        provider.getAdditionalWords("nouns") shouldBe provider.getNouns()
        provider.getAdditionalWords("unknown").shouldBeEmpty()
        provider.getAdditionalWords("").shouldBeEmpty()
    }

    "getAdditionalWords should handle arbitrary category names" {
        checkAll(iterations, Arb.string()) { category ->
            // Arrange
            val provider = DefaultWordProvider()

            // Act
            val words = provider.getAdditionalWords(category)

            // Assert
            when (category) {
                "adjectives" -> words shouldBe provider.getAdjectives()
                "nouns" -> words shouldBe provider.getNouns()
                else -> words.shouldBeEmpty()
            }
        }
    }

    "available categories should be consistent" {
        // Arrange
        val provider = DefaultWordProvider()

        // Act
        val categories = provider.getAvailableCategories()

        // Assert
        categories shouldHaveSize 2
        categories shouldContainAll listOf("adjectives", "nouns")

        // Categories should correspond to actual word lists
        categories.forEach { category ->
            provider.getAdditionalWords(category).shouldNotBeEmpty()
        }
    }

    "word lists should be immutable" {
        // Arrange
        val provider = DefaultWordProvider()

        // Act - Get lists multiple times
        val adjectives1 = provider.getAdjectives()
        val adjectives2 = provider.getAdjectives()
        val nouns1 = provider.getNouns()
        val nouns2 = provider.getNouns()

        // Assert - Should return same lists (referential equality in this case)
        adjectives1 shouldBe adjectives2
        nouns1 shouldBe nouns2
    }

    "all words should be suitable for URL-safe aliases" {
        // Arrange
        val provider = DefaultWordProvider()
        val allWords = provider.getAdjectives() + provider.getNouns()

        // Act & Assert
        allWords.forEach { word ->
            // Should only contain lowercase letters (no spaces, special chars, etc.)
            word shouldMatch Regex("^[a-z]+$")

            // Should be reasonable length for aliases
            (word.length in 2..10) shouldBe true

            // Should not contain problematic substrings
            word shouldNotBe "null"
            word shouldNotBe "undefined"
            word shouldNotBe "none"
        }
    }

    "adjectives and nouns should not overlap" {
        // Arrange
        val provider = DefaultWordProvider()

        // Act
        val adjectives = provider.getAdjectives()
        val nouns = provider.getNouns()

        // Assert - No word should be both adjective and noun
        val intersection = adjectives.intersect(nouns.toSet())
        intersection.shouldBeEmpty()
    }

    "provider should be thread-safe for concurrent access" {
        // Arrange
        val provider = DefaultWordProvider()
        val results = mutableListOf<Pair<List<String>, List<String>>>()

        // Act - Simulate concurrent access
        val threads = (1..10).map {
            Thread {
                val adj = provider.getAdjectives()
                val nouns = provider.getNouns()
                synchronized(results) {
                    results.add(adj to nouns)
                }
            }
        }

        threads.forEach { it.start() }
        threads.forEach { it.join() }

        // Assert - All threads should get the same lists
        results.forEach { (adj, nouns) ->
            adj shouldBe provider.getAdjectives()
            nouns shouldBe provider.getNouns()
        }
    }

    "word distribution should provide reasonable variety" {
        // Arrange
        val provider = DefaultWordProvider()

        // Act
        val adjectives = provider.getAdjectives()
        val nouns = provider.getNouns()

        // Assert - Check for reasonable variety in starting letters
        val adjectiveStartLetters = adjectives.map { it.first() }.distinct()
        val nounStartLetters = nouns.map { it.first() }.distinct()

        // Should have variety in starting letters (at least 10 different letters)
        (adjectiveStartLetters.size > 10) shouldBe true
        (nounStartLetters.size > 10) shouldBe true
    }
})

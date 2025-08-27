package io.github.kamiazya.scopes.scopemanagement.infrastructure.alias.generation.strategies

import io.github.kamiazya.scopes.scopemanagement.domain.service.WordProvider
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldMatch
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import io.mockk.every
import io.mockk.mockk

class HaikunatorStrategyPropertyTest :
    StringSpec({

        "generate should be deterministic for same seed" {
            checkAll(Arb.long(), wordProviderArb()) { seed, wordProvider ->
                // Given
                val strategy = HaikunatorStrategy()

                // When
                val result1 = strategy.generate(seed, wordProvider)
                val result2 = strategy.generate(seed, wordProvider)

                // Then
                result1 shouldBe result2
            }
        }

        "generate should produce different results for different seeds" {
            checkAll(Arb.long(), Arb.long(), wordProviderArb()) { seed1, seed2, wordProvider ->
                // Given
                val strategy = HaikunatorStrategy()

                // When - only test if seeds are different
                if (seed1 != seed2) {
                    val result1 = strategy.generate(seed1, wordProvider)
                    val result2 = strategy.generate(seed2, wordProvider)

                    // Then - results should likely be different
                    // Note: theoretically they could be same due to random collision, but very unlikely
                    if (wordProvider.getAdjectives().size > 1 || wordProvider.getNouns().size > 1) {
                        // Only assert difference if there are multiple options
                        result1 shouldNotBe result2
                    }
                }
            }
        }

        "generate should always follow adjective-noun-token pattern" {
            checkAll(Arb.long(), wordProviderArb()) { seed, wordProvider ->
                // Given
                val strategy = HaikunatorStrategy()

                // When
                val result = strategy.generate(seed, wordProvider)

                // Then
                result shouldMatch Regex("[a-z]+-[a-z]+-[a-z0-9]{3}")
                val parts = result.split("-")
                parts.size shouldBe 3

                // Verify each part
                wordProvider.getAdjectives() shouldBe { it.contains(parts[0]) }
                wordProvider.getNouns() shouldBe { it.contains(parts[1]) }
                parts[2] shouldMatch Regex("[a-z0-9]{3}")
            }
        }

        "generateRandom should follow adjective-noun-token pattern" {
            checkAll(wordProviderArb()) { wordProvider ->
                // Given
                val strategy = HaikunatorStrategy()

                // When
                val result = strategy.generateRandom(wordProvider)

                // Then
                result shouldMatch Regex("[a-z]+-[a-z]+-[a-z0-9]{3}")
                val parts = result.split("-")
                parts.size shouldBe 3

                // Verify each part comes from provider
                wordProvider.getAdjectives() shouldBe { it.contains(parts[0]) }
                wordProvider.getNouns() shouldBe { it.contains(parts[1]) }
                parts[2] shouldMatch Regex("[a-z0-9]{3}")
            }
        }

        "generateRandom should be non-deterministic" {
            checkAll(wordProviderArb()) { wordProvider ->
                // Given
                val strategy = HaikunatorStrategy()

                // When - generate multiple times
                val results = (1..10).map { strategy.generateRandom(wordProvider) }

                // Then - should have some variety (unless word provider is very limited)
                if (wordProvider.getAdjectives().size > 1 && wordProvider.getNouns().size > 1) {
                    val uniqueResults = results.distinct()
                    uniqueResults.size shouldBe { it > 1 } // Should have some variation
                }

                // All should follow pattern
                results.forEach { result ->
                    result shouldMatch Regex("[a-z]+-[a-z]+-[a-z0-9]{3}")
                }
            }
        }

        "getName should always return 'haikunator'" {
            checkAll(Arb.constant(Unit)) {
                // Given
                val strategy = HaikunatorStrategy()

                // When
                val name = strategy.getName()

                // Then
                name shouldBe "haikunator"
            }
        }

        "generate should handle single-word providers correctly" {
            checkAll(Arb.long()) { seed ->
                // Given
                val singleWordProvider = mockk<WordProvider>()
                every { singleWordProvider.getAdjectives() } returns listOf("single")
                every { singleWordProvider.getNouns() } returns listOf("word")

                val strategy = HaikunatorStrategy()

                // When
                val result = strategy.generate(seed, singleWordProvider)

                // Then
                result shouldMatch Regex("single-word-[a-z0-9]{3}")
            }
        }

        "generate should use all available words over multiple generations" {
            checkAll(multiWordProviderArb(), Arb.int(50, 200)) { wordProvider, iterations ->
                // Given
                val strategy = HaikunatorStrategy()
                val adjectives = wordProvider.getAdjectives()
                val nouns = wordProvider.getNouns()

                // When - generate many aliases with different seeds
                val results = (1..iterations).map { i ->
                    strategy.generate(i.toLong(), wordProvider)
                }

                // Then - should use variety of words if available
                val usedAdjectives = results.map { it.split("-")[0] }.distinct()
                val usedNouns = results.map { it.split("-")[1] }.distinct()

                if (adjectives.size > 1) {
                    usedAdjectives.size shouldBe { it > 1 }
                }
                if (nouns.size > 1) {
                    usedNouns.size shouldBe { it > 1 }
                }

                // All used words should be from provider
                usedAdjectives.forEach { adjective ->
                    adjectives shouldBe { it.contains(adjective) }
                }
                usedNouns.forEach { noun ->
                    nouns shouldBe { it.contains(noun) }
                }
            }
        }

        "generate should create valid token characters" {
            checkAll(Arb.long(), wordProviderArb()) { seed, wordProvider ->
                // Given
                val strategy = HaikunatorStrategy()
                val validTokenChars = ('a'..'z') + ('0'..'9')

                // When
                val result = strategy.generate(seed, wordProvider)
                val token = result.split("-")[2]

                // Then
                token.length shouldBe 3
                token.forEach { char ->
                    validTokenChars shouldBe { it.contains(char) }
                }
            }
        }

        "generateRandom should create valid token characters" {
            checkAll(wordProviderArb()) { wordProvider ->
                // Given
                val strategy = HaikunatorStrategy()
                val validTokenChars = ('a'..'z') + ('0'..'9')

                // When
                val result = strategy.generateRandom(wordProvider)
                val token = result.split("-")[2]

                // Then
                token.length shouldBe 3
                token.forEach { char ->
                    validTokenChars shouldBe { it.contains(char) }
                }
            }
        }

        "strategy should handle edge case word providers" {
            checkAll(edgeWordProviderArb(), Arb.long()) { wordProvider, seed ->
                // Given
                val strategy = HaikunatorStrategy()

                // When
                val result = strategy.generate(seed, wordProvider)

                // Then
                result shouldMatch Regex("[a-z]+-[a-z]+-[a-z0-9]{3}")
                val parts = result.split("-")

                wordProvider.getAdjectives() shouldBe { it.contains(parts[0]) }
                wordProvider.getNouns() shouldBe { it.contains(parts[1]) }
            }
        }

        "concurrent generation should maintain deterministic property" {
            checkAll(Arb.long(), wordProviderArb()) { seed, wordProvider ->
                // Given
                val strategy = HaikunatorStrategy()

                // When - simulate concurrent access with same seed
                val results = (1..10).map {
                    strategy.generate(seed, wordProvider)
                }

                // Then - all results should be identical (deterministic)
                val firstResult = results.first()
                results.forEach { result ->
                    result shouldBe firstResult
                }
            }
        }

        "strategy should produce distribution over available options" {
            checkAll(largeWordProviderArb()) { wordProvider ->
                // Given
                val strategy = HaikunatorStrategy()
                val adjectives = wordProvider.getAdjectives()
                val nouns = wordProvider.getNouns()

                // When - generate many aliases with sequential seeds
                val sampleSize = 1000
                val results = (1..sampleSize).map { i ->
                    strategy.generate(i.toLong(), wordProvider)
                }

                // Then - should show good distribution
                val usedAdjectives = results.map { it.split("-")[0] }.distinct()
                val usedNouns = results.map { it.split("-")[1] }.distinct()
                val usedTokens = results.map { it.split("-")[2] }.distinct()

                // Should use significant portion of available words
                usedAdjectives.size shouldBe { it >= adjectives.size / 2 }
                usedNouns.size shouldBe { it >= nouns.size / 2 }
                usedTokens.size shouldBe { it > sampleSize / 10 } // Tokens should show variety
            }
        }
    })

// Test helpers
private fun wordProviderArb(): Arb<WordProvider> = Arb.choice(
    simpleWordProviderArb(),
    multiWordProviderArb(),
    edgeWordProviderArb(),
)

private fun simpleWordProviderArb(): Arb<WordProvider> = arbitrary {
    val mockProvider = mockk<WordProvider>()
    every { mockProvider.getAdjectives() } returns listOf("test", "demo")
    every { mockProvider.getNouns() } returns listOf("alias", "name")
    mockProvider
}

private fun multiWordProviderArb(): Arb<WordProvider> = arbitrary {
    val adjectives = listOf("bold", "swift", "bright", "clever", "cool", "epic")
    val nouns = listOf("tiger", "eagle", "wolf", "bear", "lion", "shark")

    val mockProvider = mockk<WordProvider>()
    every { mockProvider.getAdjectives() } returns adjectives
    every { mockProvider.getNouns() } returns nouns
    mockProvider
}

private fun edgeWordProviderArb(): Arb<WordProvider> = Arb.choice(
    // Single word providers
    arbitrary {
        val mockProvider = mockk<WordProvider>()
        every { mockProvider.getAdjectives() } returns listOf("single")
        every { mockProvider.getNouns() } returns listOf("word")
        mockProvider
    },
    // Long word providers
    arbitrary {
        val mockProvider = mockk<WordProvider>()
        every { mockProvider.getAdjectives() } returns listOf("verylongadjective")
        every { mockProvider.getNouns() } returns listOf("verylongnounword")
        mockProvider
    },
    // Short word providers
    arbitrary {
        val mockProvider = mockk<WordProvider>()
        every { mockProvider.getAdjectives() } returns listOf("a", "b")
        every { mockProvider.getNouns() } returns listOf("x", "y")
        mockProvider
    },
)

private fun largeWordProviderArb(): Arb<WordProvider> = arbitrary {
    val adjectives = listOf(
        "amazing", "bold", "bright", "brilliant", "calm", "clever", "cool", "creative",
        "dynamic", "epic", "fast", "fresh", "great", "happy", "keen", "kind",
        "light", "noble", "quick", "smart", "swift", "wise", "young",
    )
    val nouns = listOf(
        "tiger", "eagle", "wolf", "bear", "lion", "shark", "hawk", "fox",
        "star", "moon", "sun", "tree", "river", "mountain", "ocean", "forest",
        "project", "system", "service", "framework", "platform", "application",
    )

    val mockProvider = mockk<WordProvider>()
    every { mockProvider.getAdjectives() } returns adjectives
    every { mockProvider.getNouns() } returns nouns
    mockProvider
}

package io.github.kamiazya.scopes.scopemanagement.infrastructure.alias.generation.providers

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe

class DefaultWordProviderTest :
    DescribeSpec({

        describe("DefaultWordProvider") {
            lateinit var provider: DefaultWordProvider

            beforeEach {
                provider = DefaultWordProvider()
            }

            describe("getAdjectives") {
                it("should return non-empty list of adjectives") {
                    // When
                    val adjectives = provider.getAdjectives()

                    // Then
                    adjectives.shouldNotBeEmpty()
                    adjectives.shouldHaveSize(40) // Expected size based on implementation
                }

                it("should contain expected adjectives") {
                    // When
                    val adjectives = provider.getAdjectives()

                    // Then
                    adjectives shouldContain "bold"
                    adjectives shouldContain "brave"
                    adjectives shouldContain "calm"
                    adjectives shouldContain "clever"
                    adjectives shouldContain "epic"
                    adjectives shouldContain "swift"
                    adjectives shouldContain "wise"
                }

                it("should return same list on multiple calls") {
                    // When
                    val adjectives1 = provider.getAdjectives()
                    val adjectives2 = provider.getAdjectives()

                    // Then
                    adjectives1 shouldBe adjectives2
                }

                it("should contain only valid lowercase words") {
                    // When
                    val adjectives = provider.getAdjectives()

                    // Then
                    adjectives.forEach { word ->
                        word shouldBe word.lowercase()
                        word.all { it.isLetter() } shouldBe true
                    }
                }
            }

            describe("getNouns") {
                it("should return non-empty list of nouns") {
                    // When
                    val nouns = provider.getNouns()

                    // Then
                    nouns.shouldNotBeEmpty()
                    nouns.shouldHaveSize(40) // Expected size based on implementation
                }

                it("should contain expected nouns") {
                    // When
                    val nouns = provider.getNouns()

                    // Then
                    nouns shouldContain "tiger"
                    nouns shouldContain "eagle"
                    nouns shouldContain "wolf"
                    nouns shouldContain "bear"
                    nouns shouldContain "lion"
                    nouns shouldContain "star"
                    nouns shouldContain "moon"
                }

                it("should return same list on multiple calls") {
                    // When
                    val nouns1 = provider.getNouns()
                    val nouns2 = provider.getNouns()

                    // Then
                    nouns1 shouldBe nouns2
                }

                it("should contain only valid lowercase words") {
                    // When
                    val nouns = provider.getNouns()

                    // Then
                    nouns.forEach { word ->
                        word shouldBe word.lowercase()
                        word.all { it.isLetter() } shouldBe true
                    }
                }

                it("should contain diverse categories of nouns") {
                    // When
                    val nouns = provider.getNouns()

                    // Then - verify different categories are represented
                    val animals = listOf("tiger", "eagle", "wolf", "bear", "lion", "cat", "dog")
                    val nature = listOf("star", "moon", "sun", "tree", "river", "mountain", "ocean")
                    val objects = listOf("sword", "shield", "crown", "bridge", "tower", "castle")

                    nouns shouldContainAll animals.take(3)
                    nouns shouldContainAll nature.take(3)
                    nouns shouldContainAll objects.take(2)
                }
            }

            describe("getAdditionalWords") {
                it("should return adjectives for 'adjectives' category") {
                    // When
                    val result = provider.getAdditionalWords("adjectives")

                    // Then
                    result shouldBe provider.getAdjectives()
                }

                it("should return nouns for 'nouns' category") {
                    // When
                    val result = provider.getAdditionalWords("nouns")

                    // Then
                    result shouldBe provider.getNouns()
                }

                it("should return empty list for unknown category") {
                    // When
                    val result = provider.getAdditionalWords("unknown")

                    // Then
                    result.shouldBeEmpty()
                }

                it("should return empty list for empty string category") {
                    // When
                    val result = provider.getAdditionalWords("")

                    // Then
                    result.shouldBeEmpty()
                }

                it("should handle case-sensitive category names") {
                    // When
                    val upperResult = provider.getAdditionalWords("ADJECTIVES")
                    val mixedResult = provider.getAdditionalWords("Nouns")

                    // Then
                    upperResult.shouldBeEmpty()
                    mixedResult.shouldBeEmpty()
                }
            }

            describe("getAvailableCategories") {
                it("should return list of available categories") {
                    // When
                    val categories = provider.getAvailableCategories()

                    // Then
                    categories.shouldHaveSize(2)
                    categories shouldContain "adjectives"
                    categories shouldContain "nouns"
                }

                it("should return same categories on multiple calls") {
                    // When
                    val categories1 = provider.getAvailableCategories()
                    val categories2 = provider.getAvailableCategories()

                    // Then
                    categories1 shouldBe categories2
                }
            }

            describe("word quality") {
                it("should provide suitable words for alias generation") {
                    // When
                    val adjectives = provider.getAdjectives()
                    val nouns = provider.getNouns()

                    // Then - verify words are suitable for aliases (short, simple)
                    adjectives.forEach { word ->
                        word.length shouldBe { it >= 3 && it <= 8 } // Reasonable length
                    }

                    nouns.forEach { word ->
                        word.length shouldBe { it >= 3 && it <= 10 } // Reasonable length
                    }
                }

                it("should not contain duplicate words in adjectives") {
                    // When
                    val adjectives = provider.getAdjectives()

                    // Then
                    adjectives.size shouldBe adjectives.distinct().size
                }

                it("should not contain duplicate words in nouns") {
                    // When
                    val nouns = provider.getNouns()

                    // Then
                    nouns.size shouldBe nouns.distinct().size
                }

                it("should not have overlap between adjectives and nouns") {
                    // When
                    val adjectives = provider.getAdjectives()
                    val nouns = provider.getNouns()

                    // Then
                    val overlap = adjectives.intersect(nouns.toSet())
                    overlap.shouldBeEmpty()
                }
            }

            describe("integration with HaikunatorStrategy") {
                it("should provide words that work well for alias generation") {
                    // When
                    val adjectives = provider.getAdjectives()
                    val nouns = provider.getNouns()

                    // Then - simulate alias generation
                    val sampleAlias = "${adjectives.first()}-${nouns.first()}-x7k"
                    sampleAlias.matches(Regex("[a-z]+-[a-z]+-[a-z0-9]{3}")) shouldBe true
                }

                it("should provide enough variety for unique aliases") {
                    // When
                    val adjectives = provider.getAdjectives()
                    val nouns = provider.getNouns()

                    // Then - verify sufficient combinations
                    val totalCombinations = adjectives.size * nouns.size * (36 * 36 * 36) // 36^3 for token
                    totalCombinations shouldBe { it > 1_000_000 } // Should provide enough variety
                }
            }
        }
    })

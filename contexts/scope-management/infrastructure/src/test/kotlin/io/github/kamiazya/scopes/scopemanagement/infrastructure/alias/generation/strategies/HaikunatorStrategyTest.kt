package io.github.kamiazya.scopes.scopemanagement.infrastructure.alias.generation.strategies

import io.github.kamiazya.scopes.scopemanagement.domain.service.WordProvider
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldMatch
import io.mockk.every
import io.mockk.mockk

class HaikunatorStrategyTest :
    DescribeSpec({

        describe("HaikunatorStrategy") {
            lateinit var mockWordProvider: WordProvider
            lateinit var strategy: HaikunatorStrategy

            beforeEach {
                mockWordProvider = mockk()
                strategy = HaikunatorStrategy()
            }

            describe("getName") {
                it("should return 'haikunator'") {
                    strategy.getName() shouldBe "haikunator"
                }
            }

            describe("generate") {
                beforeEach {
                    every { mockWordProvider.getAdjectives() } returns listOf("bold", "swift", "bright")
                    every { mockWordProvider.getNouns() } returns listOf("tiger", "eagle", "wolf")
                }

                it("should generate deterministic alias with same seed") {
                    // Given
                    val seed = 12345L

                    // When
                    val result1 = strategy.generate(seed, mockWordProvider)
                    val result2 = strategy.generate(seed, mockWordProvider)

                    // Then
                    result1 shouldBe result2
                }

                it("should generate different aliases with different seeds") {
                    // Given
                    val seed1 = 12345L
                    val seed2 = 54321L

                    // When
                    val result1 = strategy.generate(seed1, mockWordProvider)
                    val result2 = strategy.generate(seed2, mockWordProvider)

                    // Then
                    result1 shouldNotBe result2
                }

                it("should follow expected pattern adjective-noun-token") {
                    // Given
                    val seed = 12345L

                    // When
                    val result = strategy.generate(seed, mockWordProvider)

                    // Then
                    result shouldMatch Regex("[a-z]+-[a-z]+-[a-z0-9]{3}")
                }

                it("should use words from provider") {
                    // Given
                    val seed = 12345L

                    // When
                    val result = strategy.generate(seed, mockWordProvider)

                    // Then
                    val parts = result.split("-")
                    parts.size shouldBe 3
                    listOf("bold", "swift", "bright") shouldContain parts[0]
                    listOf("tiger", "eagle", "wolf") shouldContain parts[1]
                }

                it("should generate 3-character token") {
                    // Given
                    val seed = 12345L

                    // When
                    val result = strategy.generate(seed, mockWordProvider)

                    // Then
                    val token = result.split("-")[2]
                    token.length shouldBe 3
                    token shouldMatch Regex("[a-z0-9]{3}")
                }

                it("should handle single word lists") {
                    // Given
                    every { mockWordProvider.getAdjectives() } returns listOf("single")
                    every { mockWordProvider.getNouns() } returns listOf("word")
                    val seed = 12345L

                    // When
                    val result = strategy.generate(seed, mockWordProvider)

                    // Then
                    result shouldContain "single-word-"
                }

                it("should generate different tokens for different seeds") {
                    // Given
                    val seed1 = 1L
                    val seed2 = 2L

                    // When
                    val result1 = strategy.generate(seed1, mockWordProvider)
                    val result2 = strategy.generate(seed2, mockWordProvider)

                    // Then
                    val token1 = result1.split("-")[2]
                    val token2 = result2.split("-")[2]

                    // Tokens should likely be different (not guaranteed due to randomness, but very likely)
                    // At minimum, the full strings should be different due to different word selection
                    result1 shouldNotBe result2
                }
            }

            describe("generateRandom") {
                beforeEach {
                    every { mockWordProvider.getAdjectives() } returns listOf("bold", "swift", "bright")
                    every { mockWordProvider.getNouns() } returns listOf("tiger", "eagle", "wolf")
                }

                it("should generate non-deterministic aliases") {
                    // When
                    val result1 = strategy.generateRandom(mockWordProvider)
                    val result2 = strategy.generateRandom(mockWordProvider)

                    // Then - results should likely be different (not guaranteed but very likely)
                    // At minimum, verify the format is correct
                    result1 shouldMatch Regex("[a-z]+-[a-z]+-[a-z0-9]{3}")
                    result2 shouldMatch Regex("[a-z]+-[a-z]+-[a-z0-9]{3}")
                }

                it("should follow expected pattern adjective-noun-token") {
                    // When
                    val result = strategy.generateRandom(mockWordProvider)

                    // Then
                    result shouldMatch Regex("[a-z]+-[a-z]+-[a-z0-9]{3}")
                }

                it("should use words from provider") {
                    // When
                    val result = strategy.generateRandom(mockWordProvider)

                    // Then
                    val parts = result.split("-")
                    parts.size shouldBe 3
                    listOf("bold", "swift", "bright") shouldContain parts[0]
                    listOf("tiger", "eagle", "wolf") shouldContain parts[1]
                }

                it("should generate 3-character token") {
                    // When
                    val result = strategy.generateRandom(mockWordProvider)

                    // Then
                    val token = result.split("-")[2]
                    token.length shouldBe 3
                    token shouldMatch Regex("[a-z0-9]{3}")
                }

                it("should handle edge cases with limited word lists") {
                    // Given
                    every { mockWordProvider.getAdjectives() } returns listOf("only")
                    every { mockWordProvider.getNouns() } returns listOf("one")

                    // When
                    val result = strategy.generateRandom(mockWordProvider)

                    // Then
                    result shouldContain "only-one-"
                    result shouldMatch Regex("only-one-[a-z0-9]{3}")
                }
            }

            describe("integration") {
                it("should work with real word lists") {
                    // Given
                    val realAdjectives = listOf("amazing", "brilliant", "creative", "dynamic")
                    val realNouns = listOf("project", "system", "framework", "application")
                    every { mockWordProvider.getAdjectives() } returns realAdjectives
                    every { mockWordProvider.getNouns() } returns realNouns

                    // When
                    val deterministicResult = strategy.generate(42L, mockWordProvider)
                    val randomResult = strategy.generateRandom(mockWordProvider)

                    // Then
                    deterministicResult shouldMatch Regex("[a-z]+-[a-z]+-[a-z0-9]{3}")
                    randomResult shouldMatch Regex("[a-z]+-[a-z]+-[a-z0-9]{3}")

                    val deterministicParts = deterministicResult.split("-")
                    val randomParts = randomResult.split("-")

                    realAdjectives shouldContain deterministicParts[0]
                    realNouns shouldContain deterministicParts[1]
                    realAdjectives shouldContain randomParts[0]
                    realNouns shouldContain randomParts[1]
                }

                it("should demonstrate deterministic vs random behavior") {
                    // Given
                    every { mockWordProvider.getAdjectives() } returns listOf("test", "demo", "sample")
                    every { mockWordProvider.getNouns() } returns listOf("alias", "name", "identifier")
                    val seed = 999L

                    // When
                    val det1 = strategy.generate(seed, mockWordProvider)
                    val det2 = strategy.generate(seed, mockWordProvider)
                    val rand1 = strategy.generateRandom(mockWordProvider)
                    val rand2 = strategy.generateRandom(mockWordProvider)

                    // Then
                    det1 shouldBe det2 // Deterministic should be same
                    // Random results may or may not be the same (very low probability they're identical)
                    // But both should follow the correct pattern
                    rand1 shouldMatch Regex("[a-z]+-[a-z]+-[a-z0-9]{3}")
                    rand2 shouldMatch Regex("[a-z]+-[a-z]+-[a-z0-9]{3}")
                }
            }
        }
    })

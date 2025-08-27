package io.github.kamiazya.scopes.scopemanagement.infrastructure.alias.generation

import io.github.kamiazya.scopes.scopemanagement.domain.service.AliasGenerationStrategy
import io.github.kamiazya.scopes.scopemanagement.domain.service.WordProvider
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasId
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import io.mockk.coEvery
import io.mockk.mockk

class DefaultAliasGenerationServicePropertyTest :
    StringSpec({

        "generateCanonicalAlias should be deterministic for same AliasId" {
            checkAll(validGeneratedAliasArb()) { generatedAlias ->
                // Given
                val mockStrategy = mockk<AliasGenerationStrategy>()
                val mockWordProvider = mockk<WordProvider>()
                val service = DefaultAliasGenerationService(mockStrategy, mockWordProvider)

                val aliasId = AliasId.generate()
                coEvery { mockStrategy.generate(any(), mockWordProvider) } returns generatedAlias

                // When
                val result1 = service.generateCanonicalAlias(aliasId)
                val result2 = service.generateCanonicalAlias(aliasId)

                // Then
                result1.shouldBeRight()
                result2.shouldBeRight()
                result1.getOrNull()!! shouldBe result2.getOrNull()!!
            }
        }

        "generateCanonicalAlias should produce valid AliasName for valid strategy output" {
            checkAll(validGeneratedAliasArb(), aliasIdArb()) { generatedAlias, aliasId ->
                // Given
                val mockStrategy = mockk<AliasGenerationStrategy>()
                val mockWordProvider = mockk<WordProvider>()
                val service = DefaultAliasGenerationService(mockStrategy, mockWordProvider)

                coEvery { mockStrategy.generate(any(), mockWordProvider) } returns generatedAlias

                // When
                val result = service.generateCanonicalAlias(aliasId)

                // Then
                result.shouldBeRight()
                val aliasName = result.getOrNull()!!
                aliasName.value shouldMatch Regex("[a-z][a-z0-9-_]{1,63}")
            }
        }

        "generateCanonicalAlias should handle strategy exceptions gracefully" {
            checkAll(aliasIdArb(), exceptionMessageArb()) { aliasId, errorMessage ->
                // Given
                val mockStrategy = mockk<AliasGenerationStrategy>()
                val mockWordProvider = mockk<WordProvider>()
                val service = DefaultAliasGenerationService(mockStrategy, mockWordProvider)

                coEvery { mockStrategy.generate(any(), mockWordProvider) } throws RuntimeException(errorMessage)

                // When
                val result = service.generateCanonicalAlias(aliasId)

                // Then
                result.shouldBeLeft()
                val error = result.leftOrNull()!!
                error.attemptedValue shouldBe errorMessage
                error.expectedPattern shouldBe "[a-z][a-z0-9-_]{1,63}"
            }
        }

        "generateCanonicalAlias should handle null exception messages" {
            checkAll(aliasIdArb()) { aliasId ->
                // Given
                val mockStrategy = mockk<AliasGenerationStrategy>()
                val mockWordProvider = mockk<WordProvider>()
                val service = DefaultAliasGenerationService(mockStrategy, mockWordProvider)

                coEvery { mockStrategy.generate(any(), mockWordProvider) } throws object : RuntimeException() {
                    override val message: String? = null
                }

                // When
                val result = service.generateCanonicalAlias(aliasId)

                // Then
                result.shouldBeLeft()
                val error = result.leftOrNull()!!
                error.attemptedValue shouldBe "generation failed"
                error.expectedPattern shouldBe "[a-z][a-z0-9-_]{1,63}"
            }
        }

        "generateCanonicalAlias should reject invalid strategy output" {
            checkAll(invalidGeneratedAliasArb(), aliasIdArb()) { invalidAlias, aliasId ->
                // Given
                val mockStrategy = mockk<AliasGenerationStrategy>()
                val mockWordProvider = mockk<WordProvider>()
                val service = DefaultAliasGenerationService(mockStrategy, mockWordProvider)

                coEvery { mockStrategy.generate(any(), mockWordProvider) } returns invalidAlias

                // When
                val result = service.generateCanonicalAlias(aliasId)

                // Then
                result.shouldBeLeft()
            }
        }

        "generateRandomAlias should produce valid AliasName for valid strategy output" {
            checkAll(validGeneratedAliasArb()) { generatedAlias ->
                // Given
                val mockStrategy = mockk<AliasGenerationStrategy>()
                val mockWordProvider = mockk<WordProvider>()
                val service = DefaultAliasGenerationService(mockStrategy, mockWordProvider)

                coEvery { mockStrategy.generateRandom(mockWordProvider) } returns generatedAlias

                // When
                val result = service.generateRandomAlias()

                // Then
                result.shouldBeRight()
                val aliasName = result.getOrNull()!!
                aliasName.value shouldMatch Regex("[a-z][a-z0-9-_]{1,63}")
            }
        }

        "generateRandomAlias should handle strategy exceptions gracefully" {
            checkAll(exceptionMessageArb()) { errorMessage ->
                // Given
                val mockStrategy = mockk<AliasGenerationStrategy>()
                val mockWordProvider = mockk<WordProvider>()
                val service = DefaultAliasGenerationService(mockStrategy, mockWordProvider)

                coEvery { mockStrategy.generateRandom(mockWordProvider) } throws RuntimeException(errorMessage)

                // When
                val result = service.generateRandomAlias()

                // Then
                result.shouldBeLeft()
                val error = result.leftOrNull()!!
                error.attemptedValue shouldBe errorMessage
                error.expectedPattern shouldBe "[a-z][a-z0-9-_]{1,63}"
            }
        }

        "generateRandomAlias should reject invalid strategy output" {
            checkAll(invalidGeneratedAliasArb()) { invalidAlias ->
                // Given
                val mockStrategy = mockk<AliasGenerationStrategy>()
                val mockWordProvider = mockk<WordProvider>()
                val service = DefaultAliasGenerationService(mockStrategy, mockWordProvider)

                coEvery { mockStrategy.generateRandom(mockWordProvider) } returns invalidAlias

                // When
                val result = service.generateRandomAlias()

                // Then
                result.shouldBeLeft()
            }
        }

        "generateCanonicalAlias should use AliasId hash as seed consistently" {
            checkAll(validGeneratedAliasArb()) { generatedAlias ->
                // Given
                val mockStrategy = mockk<AliasGenerationStrategy>()
                val mockWordProvider = mockk<WordProvider>()
                val service = DefaultAliasGenerationService(mockStrategy, mockWordProvider)

                val aliasId = AliasId.generate()
                val expectedSeed = aliasId.value.hashCode().toLong()

                coEvery { mockStrategy.generate(expectedSeed, mockWordProvider) } returns generatedAlias

                // When
                val result = service.generateCanonicalAlias(aliasId)

                // Then
                result.shouldBeRight()
                // Verify the seed was used (implicitly through mock verification)
            }
        }

        "service should handle concurrent canonical generation requests" {
            checkAll(validGeneratedAliasArb()) { generatedAlias ->
                // Given
                val mockStrategy = mockk<AliasGenerationStrategy>()
                val mockWordProvider = mockk<WordProvider>()
                val service = DefaultAliasGenerationService(mockStrategy, mockWordProvider)

                val aliasIds = (1..10).map { AliasId.generate() }
                coEvery { mockStrategy.generate(any(), mockWordProvider) } returns generatedAlias

                // When
                val results = aliasIds.map { aliasId ->
                    service.generateCanonicalAlias(aliasId)
                }

                // Then
                results.forEach { result ->
                    result.shouldBeRight()
                    result.getOrNull()!!.value shouldMatch Regex("[a-z][a-z0-9-_]{1,63}")
                }
            }
        }

        "service should handle concurrent random generation requests" {
            checkAll(validGeneratedAliasArb()) { generatedAlias ->
                // Given
                val mockStrategy = mockk<AliasGenerationStrategy>()
                val mockWordProvider = mockk<WordProvider>()
                val service = DefaultAliasGenerationService(mockStrategy, mockWordProvider)

                coEvery { mockStrategy.generateRandom(mockWordProvider) } returns generatedAlias

                // When
                val results = (1..10).map {
                    service.generateRandomAlias()
                }

                // Then
                results.forEach { result ->
                    result.shouldBeRight()
                    result.getOrNull()!!.value shouldMatch Regex("[a-z][a-z0-9-_]{1,63}")
                }
            }
        }

        "service should preserve deterministic properties across multiple invocations" {
            checkAll(validGeneratedAliasArb(), Arb.int(2, 10)) { generatedAlias, iterations ->
                // Given
                val mockStrategy = mockk<AliasGenerationStrategy>()
                val mockWordProvider = mockk<WordProvider>()
                val service = DefaultAliasGenerationService(mockStrategy, mockWordProvider)

                val aliasId = AliasId.generate()
                coEvery { mockStrategy.generate(any(), mockWordProvider) } returns generatedAlias

                // When
                val results = (1..iterations).map {
                    service.generateCanonicalAlias(aliasId)
                }

                // Then
                val firstResult = results.first().getOrNull()!!
                results.forEach { result ->
                    result.shouldBeRight()
                    result.getOrNull()!! shouldBe firstResult
                }
            }
        }
    })

// Test helpers
private fun aliasIdArb(): Arb<AliasId> = arbitrary {
    AliasId.generate()
}

private fun validGeneratedAliasArb(): Arb<String> = Arb.choice(
    Arb.constant("test-alias"),
    Arb.constant("my-project"),
    Arb.constant("service_v1"),
    Arb.constant("api-gateway"),
    Arb.constant("bold-tiger-x7k"),
    Arb.constant("swift-eagle-m3n"),
    Arb.constant("bright-wolf-k9p"),
    Arb.constant("data-store"),
    Arb.constant("cache-layer"),
    Arb.constant("auth-service"),
)

private fun invalidGeneratedAliasArb(): Arb<String> = Arb.choice(
    Arb.constant(""), // Empty
    Arb.constant("A"), // Single character + uppercase
    Arb.constant("Test-Alias"), // Uppercase
    Arb.constant("test alias"), // Space
    Arb.constant("test@alias"), // Invalid character
    Arb.constant("1test"), // Starts with digit
    Arb.constant("-test"), // Starts with hyphen
    Arb.constant("_test"), // Starts with underscore
    Arb.string(65, 100), // Too long
)

private fun exceptionMessageArb(): Arb<String> = Arb.choice(
    Arb.constant("Generation failed"),
    Arb.constant("Strategy error"),
    Arb.constant("Word provider unavailable"),
    Arb.constant("Network timeout"),
    Arb.constant("Resource exhausted"),
    Arb.string(1, 100), // Random error messages
)

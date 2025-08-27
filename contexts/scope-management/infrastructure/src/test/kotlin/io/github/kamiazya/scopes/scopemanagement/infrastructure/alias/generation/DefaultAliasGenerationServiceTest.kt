package io.github.kamiazya.scopes.scopemanagement.infrastructure.alias.generation

import io.github.kamiazya.scopes.scopemanagement.domain.service.AliasGenerationStrategy
import io.github.kamiazya.scopes.scopemanagement.domain.service.WordProvider
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasId
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.mockk

class DefaultAliasGenerationServiceTest :
    DescribeSpec({

        describe("DefaultAliasGenerationService") {
            lateinit var mockStrategy: AliasGenerationStrategy
            lateinit var mockWordProvider: WordProvider
            lateinit var service: DefaultAliasGenerationService

            beforeEach {
                mockStrategy = mockk()
                mockWordProvider = mockk()
                service = DefaultAliasGenerationService(mockStrategy, mockWordProvider)
            }

            describe("generateCanonicalAlias") {
                it("should generate canonical alias successfully") {
                    // Given
                    val aliasId = AliasId.generate()
                    val expectedAlias = "test-canonical-alias"

                    coEvery {
                        mockStrategy.generate(any(), mockWordProvider)
                    } returns expectedAlias

                    // When
                    val result = service.generateCanonicalAlias(aliasId)

                    // Then
                    result.shouldBeRight()
                    val aliasName = result.getOrNull()!!
                    aliasName.value shouldBe expectedAlias
                }

                it("should be deterministic for same alias ID") {
                    // Given
                    val aliasId = AliasId.generate()
                    val expectedAlias = "deterministic-alias"

                    coEvery {
                        mockStrategy.generate(any(), mockWordProvider)
                    } returns expectedAlias

                    // When
                    val result1 = service.generateCanonicalAlias(aliasId)
                    val result2 = service.generateCanonicalAlias(aliasId)

                    // Then
                    result1.shouldBeRight()
                    result2.shouldBeRight()
                    result1.getOrNull()!!.value shouldBe result2.getOrNull()!!.value
                }

                it("should return error when strategy throws exception") {
                    // Given
                    val aliasId = AliasId.generate()

                    coEvery {
                        mockStrategy.generate(any(), mockWordProvider)
                    } throws RuntimeException("Generation failed")

                    // When
                    val result = service.generateCanonicalAlias(aliasId)

                    // Then
                    result.shouldBeLeft()
                    val error = result.leftOrNull()!!
                    error.shouldBeInstanceOf<io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.InvalidFormat>()
                    error.attemptedValue shouldBe "Generation failed"
                }

                it("should return error when generated alias is invalid") {
                    // Given
                    val aliasId = AliasId.generate()
                    val invalidAlias = "" // Empty string is invalid

                    coEvery {
                        mockStrategy.generate(any(), mockWordProvider)
                    } returns invalidAlias

                    // When
                    val result = service.generateCanonicalAlias(aliasId)

                    // Then
                    result.shouldBeLeft()
                }
            }

            describe("generateRandomAlias") {
                it("should generate random alias successfully") {
                    // Given
                    val expectedAlias = "random-test-alias"

                    coEvery {
                        mockStrategy.generateRandom(mockWordProvider)
                    } returns expectedAlias

                    // When
                    val result = service.generateRandomAlias()

                    // Then
                    result.shouldBeRight()
                    val aliasName = result.getOrNull()!!
                    aliasName.value shouldBe expectedAlias
                }

                it("should return error when strategy throws exception") {
                    // Given
                    coEvery {
                        mockStrategy.generateRandom(mockWordProvider)
                    } throws RuntimeException("Random generation failed")

                    // When
                    val result = service.generateRandomAlias()

                    // Then
                    result.shouldBeLeft()
                    val error = result.leftOrNull()!!
                    error.shouldBeInstanceOf<io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.InvalidFormat>()
                    error.attemptedValue shouldBe "Random generation failed"
                }

                it("should return error when generated alias is invalid") {
                    // Given
                    val invalidAlias = "INVALID_ALIAS" // Uppercase is invalid

                    coEvery {
                        mockStrategy.generateRandom(mockWordProvider)
                    } returns invalidAlias

                    // When
                    val result = service.generateRandomAlias()

                    // Then
                    result.shouldBeLeft()
                }

                it("should handle different generations (non-deterministic)") {
                    // Given
                    val alias1 = "random-alias-1"
                    val alias2 = "random-alias-2"

                    coEvery {
                        mockStrategy.generateRandom(mockWordProvider)
                    } returnsMany listOf(alias1, alias2)

                    // When
                    val result1 = service.generateRandomAlias()
                    val result2 = service.generateRandomAlias()

                    // Then
                    result1.shouldBeRight()
                    result2.shouldBeRight()
                    result1.getOrNull()!!.value shouldBe alias1
                    result2.getOrNull()!!.value shouldBe alias2
                }
            }

            describe("error handling") {
                it("should handle null exception messages") {
                    // Given
                    val aliasId = AliasId.generate()

                    coEvery {
                        mockStrategy.generate(any(), mockWordProvider)
                    } throws object : RuntimeException() {
                        override val message: String? = null
                    }

                    // When
                    val result = service.generateCanonicalAlias(aliasId)

                    // Then
                    result.shouldBeLeft()
                    val error = result.leftOrNull()!!
                    error.shouldBeInstanceOf<io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.InvalidFormat>()
                    error.attemptedValue shouldBe "generation failed"
                }

                it("should preserve expected pattern in error") {
                    // Given
                    val aliasId = AliasId.generate()

                    coEvery {
                        mockStrategy.generate(any(), mockWordProvider)
                    } throws RuntimeException("Test error")

                    // When
                    val result = service.generateCanonicalAlias(aliasId)

                    // Then
                    result.shouldBeLeft()
                    val error = result.leftOrNull()!!
                    error.shouldBeInstanceOf<io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.InvalidFormat>()
                    error.expectedPattern shouldBe "[a-z][a-z0-9-_]{1,63}"
                }
            }
        }
    })

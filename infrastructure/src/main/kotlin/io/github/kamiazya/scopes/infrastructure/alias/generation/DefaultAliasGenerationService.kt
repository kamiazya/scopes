package io.github.kamiazya.scopes.infrastructure.alias.generation

import arrow.core.Either
import arrow.core.left
import io.github.kamiazya.scopes.domain.error.ScopeInputError
import io.github.kamiazya.scopes.domain.error.currentTimestamp
import io.github.kamiazya.scopes.domain.service.AliasGenerationService
import io.github.kamiazya.scopes.domain.service.AliasGenerationStrategy
import io.github.kamiazya.scopes.domain.service.WordProvider
import io.github.kamiazya.scopes.domain.valueobject.AliasId
import io.github.kamiazya.scopes.domain.valueobject.AliasName

/**
 * Default implementation of AliasGenerationService.
 *
 * This implementation uses the Strategy pattern to allow different
 * alias generation algorithms to be used. The strategy and word provider
 * can be injected, making the service flexible and extensible.
 *
 * By default, it uses the Haikunator strategy with the default word provider,
 * but these can be changed through configuration or dependency injection.
 */
class DefaultAliasGenerationService(
    private val strategy: AliasGenerationStrategy,
    private val wordProvider: WordProvider
) : AliasGenerationService {

    /**
     * Generates a canonical alias for a given alias ID.
     *
     * Uses the alias ID's hash as a seed for deterministic generation,
     * ensuring the same ID always produces the same alias name.
     *
     * @param aliasId The alias ID to generate a name for
     * @return Either an error or the generated alias name
     */
    override suspend fun generateCanonicalAlias(aliasId: AliasId): Either<ScopeInputError.AliasError, AliasName> {
        return try {
            // Use the alias ID's hash as seed for deterministic generation
            val seed = aliasId.value.hashCode().toLong()
            val aliasString = strategy.generate(seed, wordProvider)

            AliasName.create(aliasString)
        } catch (e: Exception) {
            // If an exception occurs during generation, wrap it as an invalid format error
            ScopeInputError.AliasError.InvalidFormat(
                occurredAt = currentTimestamp(),
                attemptedValue = e.message ?: "generation failed",
                expectedPattern = "[a-z][a-z0-9-_]{1,63}"
            ).left()
        }
    }

    /**
     * Generates a random alias.
     *
     * Uses the strategy's random generation method to create
     * a non-deterministic alias name.
     *
     * @return Either an error or the generated alias name
     */
    override suspend fun generateRandomAlias(): Either<ScopeInputError.AliasError, AliasName> {
        return try {
            val aliasString = strategy.generateRandom(wordProvider)

            AliasName.create(aliasString)
        } catch (e: Exception) {
            // If an exception occurs during generation, wrap it as an invalid format error
            ScopeInputError.AliasError.InvalidFormat(
                occurredAt = currentTimestamp(),
                attemptedValue = e.message ?: "generation failed",
                expectedPattern = "[a-z][a-z0-9-_]{1,63}"
            ).left()
        }
    }
}


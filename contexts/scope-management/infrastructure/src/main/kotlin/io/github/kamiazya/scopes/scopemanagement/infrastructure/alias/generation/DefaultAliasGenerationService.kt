package io.github.kamiazya.scopes.scopemanagement.infrastructure.alias.generation

import arrow.core.Either
import arrow.core.left
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError
import io.github.kamiazya.scopes.scopemanagement.domain.service.alias.AliasGenerationService
import io.github.kamiazya.scopes.scopemanagement.domain.service.alias.AliasGenerationStrategy
import io.github.kamiazya.scopes.scopemanagement.domain.service.alias.WordProvider
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasId
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasName

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
class DefaultAliasGenerationService(private val strategy: AliasGenerationStrategy, private val wordProvider: WordProvider) : AliasGenerationService {

    /**
     * Generates a canonical alias for a given alias ID.
     *
     * Uses the alias ID's hash as a seed for deterministic generation,
     * ensuring the same ID always produces the same alias name.
     *
     * @param aliasId The alias ID to generate a name for
     * @return Either an error or the generated alias name
     */
    override fun generateCanonicalAlias(aliasId: AliasId): Either<ScopeInputError.AliasError, AliasName> = try {
        // Use the alias ID's hash as seed for deterministic generation
        val seed = aliasId.value.hashCode().toLong()
        val aliasString = strategy.generate(seed, wordProvider)

        AliasName.create(aliasString)
    } catch (e: Exception) {
        // If an exception occurs during generation, wrap it as an invalid format error
        ScopeInputError.AliasError.InvalidFormat(
            attemptedValue = e.message ?: "generation failed",
            patternType = ScopeInputError.AliasError.InvalidFormat.AliasPatternType.LOWERCASE_WITH_HYPHENS,
        ).left()
    }

    /**
     * Generates a random alias.
     *
     * Uses the strategy's random generation method to create
     * a non-deterministic alias name.
     *
     * @return Either an error or the generated alias name
     */
    override fun generateRandomAlias(): Either<ScopeInputError.AliasError, AliasName> = try {
        val aliasString = strategy.generateRandom(wordProvider)

        AliasName.create(aliasString)
    } catch (e: Exception) {
        // If an exception occurs during generation, wrap it as an invalid format error
        ScopeInputError.AliasError.InvalidFormat(
            attemptedValue = e.message ?: "generation failed",
            patternType = ScopeInputError.AliasError.InvalidFormat.AliasPatternType.LOWERCASE_WITH_HYPHENS,
        ).left()
    }
}

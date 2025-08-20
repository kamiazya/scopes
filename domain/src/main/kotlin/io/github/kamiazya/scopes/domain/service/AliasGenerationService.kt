package io.github.kamiazya.scopes.domain.service

import arrow.core.Either
import io.github.kamiazya.scopes.domain.error.ScopeInputError
import io.github.kamiazya.scopes.domain.valueobject.AliasId
import io.github.kamiazya.scopes.domain.valueobject.AliasName

/**
 * Domain service interface for generating aliases.
 *
 * This interface abstracts the alias generation logic, allowing for different
 * implementations (e.g., Haikunator, UUID, Sequential) to be used interchangeably.
 *
 * Following the Dependency Inversion Principle, the domain layer defines this
 * interface while the infrastructure layer provides concrete implementations.
 */
interface AliasGenerationService {

    /**
     * Generates a canonical alias for a given alias ID.
     *
     * The generation should be deterministic based on the alias ID,
     * ensuring the same ID always generates the same alias name.
     * This makes the alias generation self-contained and reproducible.
     *
     * @param aliasId The alias ID to generate a name for
     * @return Either a domain error or the generated alias name
     */
    suspend fun generateCanonicalAlias(aliasId: AliasId): Either<ScopeInputError.AliasError, AliasName>

    /**
     * Generates a random alias.
     *
     * This is non-deterministic and will generate different results each time.
     * Useful for generating unique aliases without a specific seed.
     *
     * @return Either a domain error or the generated alias name
     */
    suspend fun generateRandomAlias(): Either<ScopeInputError.AliasError, AliasName>
}

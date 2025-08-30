package io.github.kamiazya.scopes.scopemanagement.domain.repository

import arrow.core.Either
import io.github.kamiazya.scopes.scopemanagement.domain.entity.ScopeAlias
import io.github.kamiazya.scopes.scopemanagement.domain.error.PersistenceError
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasId
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasName
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasType
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId

/**
 * Repository interface for managing scope aliases.
 *
 * Provides operations for storing, retrieving, and managing scope aliases
 * while maintaining business rules and constraints.
 */
interface ScopeAliasRepository {

    /**
     * Saves a scope alias.
     *
     * @param alias The alias to save
     * @return Either a persistence error or Unit on success
     */
    suspend fun save(alias: ScopeAlias): Either<PersistenceError, Unit>

    /**
     * Finds an alias by its name.
     *
     * @param aliasName The alias name to search for
     * @return Either a persistence error or the found alias (null if not found)
     */
    suspend fun findByAliasName(aliasName: AliasName): Either<PersistenceError, ScopeAlias?>

    /**
     * Finds an alias by its unique ID.
     *
     * @param aliasId The alias ID to search for
     * @return Either a persistence error or the found alias (null if not found)
     */
    suspend fun findById(aliasId: AliasId): Either<PersistenceError, ScopeAlias?>

    /**
     * Finds all aliases for a specific scope.
     *
     * @param scopeId The scope ID to search for
     * @return Either a persistence error or the list of aliases
     */
    suspend fun findByScopeId(scopeId: ScopeId): Either<PersistenceError, List<ScopeAlias>>

    /**
     * Finds the canonical alias for a specific scope.
     *
     * @param scopeId The scope ID to search for
     * @return Either a persistence error or the canonical alias (null if not found)
     */
    suspend fun findCanonicalByScopeId(scopeId: ScopeId): Either<PersistenceError, ScopeAlias?>

    /**
     * Finds aliases by type for a specific scope.
     *
     * @param scopeId The scope ID to search for
     * @param aliasType The type of aliases to find
     * @return Either a persistence error or the list of aliases
     */
    suspend fun findByScopeIdAndType(scopeId: ScopeId, aliasType: AliasType): Either<PersistenceError, List<ScopeAlias>>

    /**
     * Finds aliases whose names start with the given prefix.
     * Used for tab completion and partial matching.
     *
     * @param prefix The prefix to match
     * @param limit Maximum number of results to return
     * @return Either a persistence error or the list of matching aliases
     */
    suspend fun findByAliasNamePrefix(prefix: String, limit: Int = 50): Either<PersistenceError, List<ScopeAlias>>

    /**
     * Checks if an alias name already exists.
     *
     * @param aliasName The alias name to check
     * @return Either a persistence error or true if exists, false otherwise
     */
    suspend fun existsByAliasName(aliasName: AliasName): Either<PersistenceError, Boolean>

    /**
     * Removes an alias by its ID.
     *
     * @param aliasId The alias ID to remove
     * @return Either a persistence error or true if removed, false if not found
     */
    suspend fun removeById(aliasId: AliasId): Either<PersistenceError, Boolean>

    /**
     * Removes an alias by its name.
     *
     * @param aliasName The alias name to remove
     * @return Either a persistence error or true if removed, false if not found
     */
    suspend fun removeByAliasName(aliasName: AliasName): Either<PersistenceError, Boolean>

    /**
     * Removes all aliases for a specific scope.
     * Used when a scope is deleted.
     *
     * @param scopeId The scope ID whose aliases to remove
     * @return Either a persistence error or the number of removed aliases
     */
    suspend fun removeByScopeId(scopeId: ScopeId): Either<PersistenceError, Int>

    /**
     * Updates an existing alias.
     *
     * @param alias The alias to update
     * @return Either a persistence error or true if updated, false if not found
     */
    suspend fun update(alias: ScopeAlias): Either<PersistenceError, Boolean>

    /**
     * Counts the total number of aliases.
     *
     * @return Either a persistence error or the count
     */
    suspend fun count(): Either<PersistenceError, Long>

    /**
     * Lists all aliases with pagination support.
     *
     * @param offset The number of aliases to skip
     * @param limit The maximum number of aliases to return
     * @return Either a persistence error or the list of aliases
     */
    suspend fun listAll(offset: Int = 0, limit: Int = 100): Either<PersistenceError, List<ScopeAlias>>
}

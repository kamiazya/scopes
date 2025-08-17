package io.github.kamiazya.scopes.application.usecase.query

import io.github.kamiazya.scopes.application.dto.DTO

/**
 * Marker interface for query inputs that read state.
 * Queries represent read operations in CQRS pattern.
 */
interface Query : DTO

/**
 * Query to resolve an alias to a scope ID.
 */
data class ResolveAliasQuery(
    val aliasName: String
) : Query

/**
 * Query to list all aliases for a specific scope.
 */
data class ListAliasesForScopeQuery(
    val scopeId: String
) : Query

/**
 * Query to find aliases that start with a given prefix.
 * Used for tab completion and partial matching.
 */
data class FindAliasesByPrefixQuery(
    val prefix: String,
    val limit: Int = 50
) : Query

/**
 * Query to get a scope by its alias.
 * Accepts either canonical or custom alias names.
 */
data class GetScopeByAliasQuery(
    val aliasName: String
) : Query

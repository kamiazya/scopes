package io.github.kamiazya.scopes.scopemanagement.infrastructure.mapper

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.scopemanagement.domain.entity.Scope
import io.github.kamiazya.scopes.scopemanagement.domain.error.PersistenceError
import io.github.kamiazya.scopes.scopemanagement.domain.factory.ScopeFactory
import io.github.kamiazya.scopes.scopemanagement.db.Scopes as DbScope
import io.github.kamiazya.scopes.scopemanagement.db.ScopeAspects as DbScopeAspect

/**
 * Maps between database rows and domain objects.
 * Centralizes all data transformation logic to eliminate duplication.
 */
class ScopeDataMapper(
    private val scopeFactory: ScopeFactory = ScopeFactory()
) {
    
    /**
     * Maps a single database row to a Scope domain object.
     */
    fun mapRow(
        row: DbScope,
        aspectRows: List<DbScopeAspect>
    ): Either<PersistenceError, Scope> {
        val aspectsMap = groupAspectsByKey(aspectRows)
        return scopeFactory.createFromPersistence(
            id = row.id,
            parentId = row.parent_id,
            title = row.title,
            description = row.description,
            status = row.status,
            aspects = aspectsMap
        )
    }
    
    /**
     * Maps multiple database rows to Scope domain objects.
     */
    suspend fun mapRows(
        rows: List<DbScope>,
        aspectsMap: Map<String, List<DbScopeAspect>>
    ): Either<PersistenceError, List<Scope>> = either {
        rows.map { row ->
            val aspectRows = aspectsMap[row.id] ?: emptyList()
            mapRow(row, aspectRows).bind()
        }
    }
    
    /**
     * Groups aspect rows by their key for a single scope.
     */
    private fun groupAspectsByKey(aspectRows: List<DbScopeAspect>): Map<String, List<String>> {
        return aspectRows.groupBy(
            keySelector = { it.aspect_key },
            valueTransform = { it.aspect_value }
        )
    }
    
    /**
     * Loads and groups aspects for multiple scopes.
     */
    fun groupAspectsForScopes(aspectRows: List<DbScopeAspect>): Map<String, List<DbScopeAspect>> {
        return aspectRows.groupBy { it.scope_id }
    }
}
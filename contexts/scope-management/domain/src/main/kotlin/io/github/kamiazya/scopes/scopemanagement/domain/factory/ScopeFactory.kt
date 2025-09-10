package io.github.kamiazya.scopes.scopemanagement.domain.factory

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.right
import arrow.core.toNonEmptyListOrNull
import io.github.kamiazya.scopes.scopemanagement.domain.entity.Scope
import io.github.kamiazya.scopes.scopemanagement.domain.error.PersistenceError
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectKey
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectValue
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.Aspects
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeDescription
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeStatus
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeTitle

/**
 * Factory for creating Scope domain objects from persistence data.
 * This moves domain object construction logic from infrastructure to domain layer,
 * addressing the anemic domain model anti-pattern.
 */
class ScopeFactory {
    
    /**
     * Creates a Scope from raw persistence data.
     * Handles all value object creation and validation.
     */
    fun createFromPersistence(
        id: String,
        parentId: String?,
        title: String,
        description: String?,
        status: String?,
        aspects: Map<String, List<String>>
    ): Either<PersistenceError, Scope> = either {
        val scopeId = ScopeId.create(id).mapLeft { validationError ->
            PersistenceError.DataCorruption(
                entityType = PersistenceError.EntityType.SCOPE,
                entityId = id,
                corruptionType = PersistenceError.DataCorruption.CorruptionType.INVALID_ID_FORMAT,
                details = mapOf("validation_error" to validationError)
            )
        }.bind()
        
        val parentScopeId = parentId?.let {
            ScopeId.create(it).mapLeft { validationError ->
                PersistenceError.DataCorruption(
                    entityType = PersistenceError.EntityType.SCOPE,
                    entityId = id,
                    corruptionType = PersistenceError.DataCorruption.CorruptionType.INVALID_REFERENCE,
                    details = mapOf(
                        "field" to "parent_id",
                        "value" to it,
                        "validation_error" to validationError
                    )
                )
            }.bind()
        }
        
        val scopeTitle = ScopeTitle.create(title).mapLeft { validationError ->
            PersistenceError.DataCorruption(
                entityType = PersistenceError.EntityType.SCOPE,
                entityId = id,
                corruptionType = PersistenceError.DataCorruption.CorruptionType.INVALID_FIELD_VALUE,
                details = mapOf(
                    "field" to "title",
                    "value" to title,
                    "validation_error" to validationError
                )
            )
        }.bind()
        
        val scopeDescription = description?.let {
            ScopeDescription.create(it).mapLeft { validationError ->
                PersistenceError.DataCorruption(
                    entityType = PersistenceError.EntityType.SCOPE,
                    entityId = id,
                    corruptionType = PersistenceError.DataCorruption.CorruptionType.INVALID_FIELD_VALUE,
                    details = mapOf(
                        "field" to "description",
                        "value" to it,
                        "validation_error" to validationError
                    )
                )
            }.bind()
        }
        
        val scopeStatus = status?.let {
            ScopeStatus.valueOf(it)
        } ?: ScopeStatus.Draft
        
        val scopeAspects = createAspects(id, aspects).bind()
        
        Scope(
            id = scopeId,
            parentId = parentScopeId,
            title = scopeTitle,
            description = scopeDescription,
            status = scopeStatus,
            aspects = scopeAspects,
        )
    }
    
    /**
     * Creates Aspects from raw aspect data.
     * Handles aspect key-value mapping and validation.
     */
    private fun createAspects(
        scopeId: String,
        aspectsMap: Map<String, List<String>>
    ): Either<PersistenceError, Aspects> = either {
        val aspectEntries = aspectsMap.mapNotNull { (key, values) ->
            val nonEmptyValues = values.toNonEmptyListOrNull()
            if (nonEmptyValues == null) {
                null // Skip empty aspect groups
            } else {
                val aspectKey = AspectKey.create(key).mapLeft { validationError ->
                    PersistenceError.DataCorruption(
                        entityType = PersistenceError.EntityType.SCOPE,
                        entityId = scopeId,
                        corruptionType = PersistenceError.DataCorruption.CorruptionType.INVALID_FIELD_VALUE,
                        details = mapOf(
                            "field" to "aspect_key",
                            "value" to key,
                            "validation_error" to validationError
                        )
                    )
                }.bind()
                
                val aspectValues = nonEmptyValues.map { value ->
                    AspectValue.create(value).mapLeft { validationError ->
                        PersistenceError.DataCorruption(
                            entityType = PersistenceError.EntityType.SCOPE,
                            entityId = scopeId,
                            corruptionType = PersistenceError.DataCorruption.CorruptionType.INVALID_FIELD_VALUE,
                            details = mapOf(
                                "field" to "aspect_value",
                                "key" to key,
                                "value" to value,
                                "validation_error" to validationError
                            )
                        )
                    }.bind()
                }
                
                aspectKey to aspectValues
            }
        }.toMap()
        
        Aspects.create(aspectEntries).mapLeft { validationError ->
            PersistenceError.DataCorruption(
                entityType = PersistenceError.EntityType.SCOPE,
                entityId = scopeId,
                corruptionType = PersistenceError.DataCorruption.CorruptionType.INCONSISTENT_STATE,
                details = mapOf(
                    "field" to "aspects",
                    "validation_error" to validationError
                )
            )
        }.bind()
    }
}
package io.github.kamiazya.scopes.scopemanagement.domain.error

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Errors related to data persistence operations.
 * 
 * This sealed hierarchy represents all possible persistence layer errors
 * without containing any presentation logic or message formatting.
 * Error messages should be generated at the presentation layer based on
 * the error type and its data.
 */
sealed class PersistenceError : ScopesError() {
    
    /**
     * Database or storage system is unavailable.
     * 
     * @property operation The operation that failed (e.g., "save", "findById")
     * @property cause The underlying exception, if available
     */
    data class StorageUnavailable(
        val operation: PersistenceOperation,
        val cause: Throwable? = null,
        override val occurredAt: Instant = Clock.System.now()
    ) : PersistenceError()
    
    /**
     * Data integrity violation detected.
     * 
     * @property entityType The type of entity with corrupted data
     * @property entityId The ID of the corrupted entity
     * @property corruptionType The specific type of corruption detected
     * @property details Additional context about the corruption
     */
    data class DataCorruption(
        val entityType: EntityType,
        val entityId: String,
        val corruptionType: CorruptionType,
        val details: Map<String, String> = emptyMap(),
        override val occurredAt: Instant = Clock.System.now()
    ) : PersistenceError() {
        
        enum class CorruptionType {
            INVALID_ID_FORMAT,
            INVALID_REFERENCE,
            MISSING_REQUIRED_FIELD,
            INVALID_FIELD_VALUE,
            INCONSISTENT_STATE,
            ORPHANED_RELATION
        }
    }
    
    /**
     * Optimistic locking conflict detected.
     * 
     * @property entityType The type of entity involved
     * @property entityId The ID of the entity
     * @property expectedVersion The version expected by the operation
     * @property actualVersion The actual version in storage
     */
    data class ConcurrencyConflict(
        val entityType: EntityType,
        val entityId: String,
        val expectedVersion: Long,
        val actualVersion: Long,
        override val occurredAt: Instant = Clock.System.now()
    ) : PersistenceError()
    
    /**
     * Entity not found in storage.
     * 
     * @property entityType The type of entity searched for
     * @property searchCriteria How the entity was searched (by ID, by key, etc.)
     * @property searchValue The value used for searching
     */
    data class NotFound(
        val entityType: EntityType,
        val searchCriteria: SearchCriteria,
        val searchValue: String,
        override val occurredAt: Instant = Clock.System.now()
    ) : PersistenceError() {
        
        enum class SearchCriteria {
            BY_ID,
            BY_KEY,
            BY_PARENT_AND_TITLE,
            BY_ALIAS
        }
    }
    
    /**
     * Constraint violation in storage.
     * 
     * @property entityType The type of entity involved
     * @property constraintType The type of constraint violated
     * @property details Additional information about the violation
     */
    data class ConstraintViolation(
        val entityType: EntityType,
        val constraintType: ConstraintType,
        val details: Map<String, String> = emptyMap(),
        override val occurredAt: Instant = Clock.System.now()
    ) : PersistenceError() {
        
        enum class ConstraintType {
            UNIQUE_KEY,
            FOREIGN_KEY,
            CHECK_CONSTRAINT,
            NOT_NULL
        }
    }
    
    /**
     * Types of entities in the persistence layer.
     */
    enum class EntityType {
        SCOPE,
        SCOPE_ALIAS,
        CONTEXT_VIEW,
        ASPECT_DEFINITION,
        ASPECT_PRESET
    }
    
    /**
     * Types of persistence operations.
     */
    enum class PersistenceOperation {
        SAVE,
        UPDATE,
        DELETE,
        FIND_BY_ID,
        FIND_ALL,
        FIND_BY_CRITERIA,
        COUNT,
        EXISTS_CHECK,
        BATCH_OPERATION
    }
}

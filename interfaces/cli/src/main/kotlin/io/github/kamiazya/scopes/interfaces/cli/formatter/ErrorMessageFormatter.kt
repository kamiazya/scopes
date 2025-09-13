package io.github.kamiazya.scopes.interfaces.cli.formatter

import io.github.kamiazya.scopes.scopemanagement.domain.error.PersistenceError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import java.util.Locale

/**
 * Formats error objects into user-friendly messages.
 * 
 * This formatter is responsible for converting domain errors into
 * human-readable messages at the presentation layer. This separation
 * ensures that:
 * 1. Domain layer remains free of presentation concerns
 * 2. Messages can be easily internationalized
 * 3. Different presentation layers can format errors differently
 * 4. Error data structure changes don't affect message generation
 */
class ErrorMessageFormatter(
    private val locale: Locale = Locale.getDefault()
) {
    
    /**
     * Formats a ScopesError into a user-friendly message.
     */
    fun format(error: ScopesError): String {
        return when (error) {
            is PersistenceError -> formatPersistenceError(error)
            is ScopesError.ValidationFailed -> formatValidationError(error)
            is ScopesError.NotFound -> formatNotFoundError(error)
            is ScopesError.AlreadyExists -> formatAlreadyExistsError(error)
            is ScopesError.InvalidOperation -> formatInvalidOperationError(error)
            is ScopesError.SystemError -> formatSystemError(error)
            is ScopesError.Conflict -> formatConflictError(error)
            is ScopesError.ConcurrencyError -> formatConcurrencyError(error)
            is ScopesError.RepositoryError -> formatRepositoryError(error)
            is ScopesError.ScopeStatusTransitionError -> formatStatusTransitionError(error)
            else -> "An unexpected error occurred at ${error.occurredAt}"
        }
    }
    
    private fun formatPersistenceError(error: PersistenceError): String {
        return when (error) {
            is PersistenceError.StorageUnavailable -> {
                val operation = formatOperation(error.operation)
                "Storage unavailable during $operation operation. Please try again later."
            }
            
            is PersistenceError.DataCorruption -> {
                val entity = formatEntityType(error.entityType)
                val corruption = formatCorruptionType(error.corruptionType)
                "Data integrity issue detected in $entity (ID: ${error.entityId}): $corruption"
            }
            
            is PersistenceError.ConcurrencyConflict -> {
                val entity = formatEntityType(error.entityType)
                "The $entity has been modified by another user. Please refresh and try again."
            }
            
            is PersistenceError.NotFound -> {
                val entity = formatEntityType(error.entityType)
                val criteria = formatSearchCriteria(error.searchCriteria)
                "$entity not found $criteria: ${error.searchValue}"
            }
            
            is PersistenceError.ConstraintViolation -> {
                val entity = formatEntityType(error.entityType)
                val constraint = formatConstraintType(error.constraintType)
                "$constraint violation for $entity"
            }
        }
    }
    
    private fun formatValidationError(error: ScopesError.ValidationFailed): String {
        val constraint = when (error.constraint) {
            is ScopesError.ValidationConstraintType.InvalidType -> 
                "Expected ${error.constraint.expectedType} but got ${error.constraint.actualType}"
            is ScopesError.ValidationConstraintType.NotInAllowedValues ->
                "Must be one of: ${error.constraint.allowedValues.joinToString(", ")}"
            is ScopesError.ValidationConstraintType.InvalidFormat ->
                "Must match format: ${error.constraint.expectedFormat}"
            is ScopesError.ValidationConstraintType.MissingRequired ->
                "Missing required fields: ${error.constraint.requiredFields.joinToString(", ")}"
            is ScopesError.ValidationConstraintType.MultipleValuesNotAllowed ->
                "Multiple values not allowed for ${error.constraint.field}"
            is ScopesError.ValidationConstraintType.InvalidValue ->
                error.constraint.reason
        }
        return "Validation failed for ${error.field}: $constraint"
    }
    
    private fun formatNotFoundError(error: ScopesError.NotFound): String {
        return "${error.entityType} not found with ${error.identifierType}: ${error.identifier}"
    }
    
    private fun formatAlreadyExistsError(error: ScopesError.AlreadyExists): String {
        return "${error.entityType} already exists with ${error.identifierType}: ${error.identifier}"
    }
    
    private fun formatInvalidOperationError(error: ScopesError.InvalidOperation): String {
        val reason = error.reason?.let { 
            when (it) {
                ScopesError.InvalidOperation.InvalidOperationReason.INVALID_STATE -> "Invalid state"
                ScopesError.InvalidOperation.InvalidOperationReason.OPERATION_NOT_ALLOWED -> "Operation not allowed"
                ScopesError.InvalidOperation.InvalidOperationReason.MISSING_PREREQUISITE -> "Missing prerequisite"
                ScopesError.InvalidOperation.InvalidOperationReason.INVALID_INPUT -> "Invalid input"
            }
        } ?: "Unknown reason"
        
        return "Invalid operation '${error.operation}': $reason"
    }
    
    private fun formatSystemError(error: ScopesError.SystemError): String {
        val errorType = when (error.errorType) {
            ScopesError.SystemError.SystemErrorType.SERVICE_UNAVAILABLE -> "Service unavailable"
            ScopesError.SystemError.SystemErrorType.SERIALIZATION_FAILED -> "Serialization failed"
            ScopesError.SystemError.SystemErrorType.DESERIALIZATION_FAILED -> "Deserialization failed"
            ScopesError.SystemError.SystemErrorType.QUERY_TIMEOUT -> "Query timeout"
            ScopesError.SystemError.SystemErrorType.CAPACITY_EXCEEDED -> "Capacity exceeded"
            ScopesError.SystemError.SystemErrorType.ACCESS_DENIED -> "Access denied"
            ScopesError.SystemError.SystemErrorType.CONFIGURATION_ERROR -> "Configuration error"
            ScopesError.SystemError.SystemErrorType.EXTERNAL_SERVICE_ERROR -> "External service error"
        }
        
        val service = error.service?.let { " in $it" } ?: ""
        return "System error$service: $errorType"
    }
    
    private fun formatConflictError(error: ScopesError.Conflict): String {
        val conflictType = when (error.conflictType) {
            ScopesError.Conflict.ConflictType.ALREADY_IN_USE -> "Already in use"
            ScopesError.Conflict.ConflictType.HAS_DEPENDENCIES -> "Has dependencies"
            ScopesError.Conflict.ConflictType.DUPLICATE_KEY -> "Duplicate key"
            ScopesError.Conflict.ConflictType.OPTIMISTIC_LOCK_FAILURE -> "Version conflict"
        }
        
        return "${error.resourceType} conflict (${error.resourceId}): $conflictType"
    }
    
    private fun formatConcurrencyError(error: ScopesError.ConcurrencyError): String {
        return "Concurrency conflict for ${error.aggregateType} (${error.aggregateId}). " +
               "Expected version ${error.expectedVersion}, but found ${error.actualVersion}"
    }
    
    private fun formatRepositoryError(error: ScopesError.RepositoryError): String {
        val operation = when (error.operation) {
            ScopesError.RepositoryError.RepositoryOperation.SAVE -> "save"
            ScopesError.RepositoryError.RepositoryOperation.FIND -> "find"
            ScopesError.RepositoryError.RepositoryOperation.DELETE -> "delete"
            ScopesError.RepositoryError.RepositoryOperation.UPDATE -> "update"
            ScopesError.RepositoryError.RepositoryOperation.QUERY -> "query"
            ScopesError.RepositoryError.RepositoryOperation.COUNT -> "count"
        }
        
        return "Repository error in ${error.repositoryName} during $operation operation"
    }
    
    private fun formatStatusTransitionError(error: ScopesError.ScopeStatusTransitionError): String {
        return "Cannot transition scope from ${error.from} to ${error.to}: ${error.reason}"
    }
    
    // Helper methods for formatting enum values
    
    private fun formatOperation(operation: PersistenceError.PersistenceOperation): String {
        return when (operation) {
            PersistenceError.PersistenceOperation.SAVE -> "save"
            PersistenceError.PersistenceOperation.UPDATE -> "update"
            PersistenceError.PersistenceOperation.DELETE -> "delete"
            PersistenceError.PersistenceOperation.FIND_BY_ID -> "find by ID"
            PersistenceError.PersistenceOperation.FIND_ALL -> "find all"
            PersistenceError.PersistenceOperation.FIND_BY_CRITERIA -> "search"
            PersistenceError.PersistenceOperation.COUNT -> "count"
            PersistenceError.PersistenceOperation.EXISTS_CHECK -> "existence check"
            PersistenceError.PersistenceOperation.BATCH_OPERATION -> "batch operation"
        }
    }
    
    private fun formatEntityType(entityType: PersistenceError.EntityType): String {
        return when (entityType) {
            PersistenceError.EntityType.SCOPE -> "Scope"
            PersistenceError.EntityType.SCOPE_ALIAS -> "Scope alias"
            PersistenceError.EntityType.CONTEXT_VIEW -> "Context view"
            PersistenceError.EntityType.ASPECT_DEFINITION -> "Aspect definition"
            PersistenceError.EntityType.ASPECT_PRESET -> "Aspect preset"
        }
    }
    
    private fun formatCorruptionType(corruptionType: PersistenceError.DataCorruption.CorruptionType): String {
        return when (corruptionType) {
            PersistenceError.DataCorruption.CorruptionType.INVALID_ID_FORMAT -> "Invalid ID format"
            PersistenceError.DataCorruption.CorruptionType.INVALID_REFERENCE -> "Invalid reference"
            PersistenceError.DataCorruption.CorruptionType.MISSING_REQUIRED_FIELD -> "Missing required field"
            PersistenceError.DataCorruption.CorruptionType.INVALID_FIELD_VALUE -> "Invalid field value"
            PersistenceError.DataCorruption.CorruptionType.INCONSISTENT_STATE -> "Inconsistent state"
            PersistenceError.DataCorruption.CorruptionType.ORPHANED_RELATION -> "Orphaned relation"
        }
    }
    
    private fun formatSearchCriteria(criteria: PersistenceError.NotFound.SearchCriteria): String {
        return when (criteria) {
            PersistenceError.NotFound.SearchCriteria.BY_ID -> "by ID"
            PersistenceError.NotFound.SearchCriteria.BY_KEY -> "by key"
            PersistenceError.NotFound.SearchCriteria.BY_PARENT_AND_TITLE -> "by parent and title"
            PersistenceError.NotFound.SearchCriteria.BY_ALIAS -> "by alias"
        }
    }
    
    private fun formatConstraintType(constraintType: PersistenceError.ConstraintViolation.ConstraintType): String {
        return when (constraintType) {
            PersistenceError.ConstraintViolation.ConstraintType.UNIQUE_KEY -> "Unique key"
            PersistenceError.ConstraintViolation.ConstraintType.FOREIGN_KEY -> "Foreign key"
            PersistenceError.ConstraintViolation.ConstraintType.CHECK_CONSTRAINT -> "Check constraint"
            PersistenceError.ConstraintViolation.ConstraintType.NOT_NULL -> "Required field"
        }
    }
}
package io.github.kamiazya.scopes.scopemanagement.infrastructure.mapper

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.scopemanagement.domain.error.PersistenceError
import io.github.kamiazya.scopes.scopemanagement.domain.error.PersistenceError.PersistenceOperation

/**
 * Handles database operations with standardized error mapping.
 * This eliminates the duplication of try-catch error handling patterns
 * throughout the repository layer.
 * 
 * The mapper focuses on capturing error information without generating
 * user-facing messages, following the principle of separating
 * error data from presentation logic.
 */
class PersistenceErrorMapper {
    
    /**
     * Executes a database operation and maps any exceptions to PersistenceError.
     * 
     * @param operation The type of persistence operation being performed
     * @param block The database operation to execute
     * @return Either.Right with the result on success, Either.Left with PersistenceError on failure
     */
    inline fun <T> executeWithErrorMapping(
        operation: PersistenceOperation,
        block: () -> T
    ): Either<PersistenceError, T> = try {
        block().right()
    } catch (e: Exception) {
        mapExceptionToPersistenceError(e, operation).left()
    }
    
    /**
     * Executes a suspending database operation with error mapping.
     */
    suspend inline fun <T> executeSuspendingWithErrorMapping(
        operation: PersistenceOperation,
        crossinline block: suspend () -> T
    ): Either<PersistenceError, T> = try {
        block().right()
    } catch (e: Exception) {
        mapExceptionToPersistenceError(e, operation).left()
    }
    
    /**
     * Maps exceptions to appropriate PersistenceError types based on
     * the exception type and context.
     */
    private fun mapExceptionToPersistenceError(
        exception: Exception,
        operation: PersistenceOperation
    ): PersistenceError {
        // In a real implementation, we would analyze the exception type
        // to determine the most appropriate error type.
        // For now, we default to StorageUnavailable.
        return when {
            exception.message?.contains("UNIQUE", ignoreCase = true) == true -> {
                PersistenceError.ConstraintViolation(
                    entityType = PersistenceError.EntityType.SCOPE, // This should be passed as context
                    constraintType = PersistenceError.ConstraintViolation.ConstraintType.UNIQUE_KEY
                )
            }
            exception.message?.contains("FOREIGN KEY", ignoreCase = true) == true -> {
                PersistenceError.ConstraintViolation(
                    entityType = PersistenceError.EntityType.SCOPE,
                    constraintType = PersistenceError.ConstraintViolation.ConstraintType.FOREIGN_KEY
                )
            }
            else -> {
                PersistenceError.StorageUnavailable(
                    operation = operation,
                    cause = exception
                )
            }
        }
    }
}
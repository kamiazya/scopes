package io.github.kamiazya.scopes.scopemanagement.infrastructure.mapper

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.scopemanagement.domain.error.PersistenceError
import kotlinx.datetime.Clock

/**
 * Handles database operations with standardized error mapping.
 * This eliminates the duplication of try-catch error handling patterns
 * throughout the repository layer.
 */
class PersistenceErrorMapper {
    
    /**
     * Executes a database operation and maps any exceptions to PersistenceError.
     * 
     * @param operation The name of the operation for error reporting
     * @param block The database operation to execute
     * @return Either.Right with the result on success, Either.Left with PersistenceError on failure
     */
    inline fun <T> executeWithErrorMapping(
        operation: String,
        block: () -> T
    ): Either<PersistenceError, T> = try {
        block().right()
    } catch (e: Exception) {
        PersistenceError.StorageUnavailable(
            occurredAt = Clock.System.now(),
            operation = operation,
            cause = e,
        ).left()
    }
    
    /**
     * Executes a suspending database operation with error mapping.
     */
    suspend inline fun <T> executeSuspendingWithErrorMapping(
        operation: String,
        crossinline block: suspend () -> T
    ): Either<PersistenceError, T> = try {
        block().right()
    } catch (e: Exception) {
        PersistenceError.StorageUnavailable(
            occurredAt = Clock.System.now(),
            operation = operation,
            cause = e,
        ).left()
    }
}
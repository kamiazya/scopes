package io.github.kamiazya.scopes.scopemanagement.application.port

import arrow.core.Either

/**
 * Port for transaction management.
 *
 * Following Hexagonal Architecture (Ports and Adapters pattern):
 * - This is an output port (driven port) from the application layer
 * - Infrastructure layer will provide the adapter implementation
 * - Enables transaction boundaries for aggregate consistency
 *
 * Transaction management patterns:
 * - Unit of Work: Groups operations that should succeed or fail together
 * - Optimistic Concurrency Control: Via aggregate version checking
 * - Read Committed isolation level as default
 */
interface TransactionManager {
    /**
     * Executes the given block within a transaction boundary.
     *
     * - All operations within the block are executed atomically
     * - Rollback occurs if any operation returns a Left value
     * - Commit occurs only if all operations return Right values
     * - Nested transactions are handled as savepoints
     *
     * @param block The operations to execute within the transaction
     * @return Either the error or the result of the transaction
     */
    suspend fun <E, T> inTransaction(block: suspend () -> Either<E, T>): Either<E, T>
}

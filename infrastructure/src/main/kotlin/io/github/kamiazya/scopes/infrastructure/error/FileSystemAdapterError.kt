package io.github.kamiazya.scopes.infrastructure.error

import kotlinx.datetime.Instant
import kotlin.time.Duration

/**
 * File system adapter errors for storage operations.
 * Covers I/O operations, permissions, and storage issues.
 */
sealed class FileSystemAdapterError : InfrastructureAdapterError() {
    
    /**
     * File I/O operation errors.
     */
    data class IOError(
        val path: String,
        val operation: FileOperation,
        val cause: Throwable,
        override val timestamp: Instant,
        override val correlationId: String? = null
    ) : FileSystemAdapterError() {
        // Retry logic based on operation type
        override val retryable: Boolean = operation.isRetryable()
    }
    
    /**
     * File permission errors.
     */
    data class PermissionError(
        val path: String,
        val requiredPermission: FilePermission,
        val actualPermissions: Set<FilePermission>?,
        val userId: String? = null,
        override val timestamp: Instant,
        override val correlationId: String? = null
    ) : FileSystemAdapterError() {
        // Permission errors are persistent and not retryable
        override val retryable: Boolean = false
    }
    
    /**
     * Storage space errors.
     */
    data class StorageError(
        val path: String,
        val errorType: StorageErrorType,
        val availableBytes: Long?,
        val requiredBytes: Long?,
        override val timestamp: Instant,
        override val correlationId: String? = null
    ) : FileSystemAdapterError() {
        // Only temporary storage issues are retryable
        override val retryable: Boolean = errorType == StorageErrorType.TEMPORARY_FULL
    }
    
    /**
     * File locking and concurrency errors.
     */
    data class ConcurrencyError(
        val path: String,
        val lockType: FileLockType,
        val heldBy: String? = null,
        val waitTime: Duration? = null,
        override val timestamp: Instant,
        override val correlationId: String? = null
    ) : FileSystemAdapterError() {
        // Lock conflicts are transient and retryable
        override val retryable: Boolean = true
    }
}
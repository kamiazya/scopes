package io.github.kamiazya.scopes.contracts.scopemanagement.errors

import kotlin.time.Duration

/**
 * Sealed interface representing all possible errors in the Scope Management contract layer.
 * These errors provide a stable API contract between bounded contexts.
 */
sealed interface ScopeContractError {
    val message: String

    /**
     * Errors related to invalid input data.
     */
    sealed interface InputError : ScopeContractError {
        /**
         * Invalid scope ID format.
         */
        data class InvalidId(val id: String, override val message: String = "Invalid scope ID format: $id") : InputError

        /**
         * Invalid scope title.
         */
        data class InvalidTitle(val title: String, val reason: String, override val message: String = "Invalid scope title: $reason") : InputError

        /**
         * Invalid scope description.
         */
        data class InvalidDescription(val description: String, val reason: String, override val message: String = "Invalid scope description: $reason") :
            InputError

        /**
         * Invalid parent scope ID.
         */
        data class InvalidParentId(val parentId: String, override val message: String = "Invalid parent scope ID: $parentId") : InputError
    }

    /**
     * Errors related to business rule violations.
     */
    sealed interface BusinessError : ScopeContractError {
        /**
         * Scope not found.
         */
        data class NotFound(val scopeId: String, override val message: String = "Scope not found: $scopeId") : BusinessError

        /**
         * Duplicate scope title within the same parent.
         */
        data class DuplicateTitle(
            val title: String,
            val parentId: String?,
            override val message: String = "Duplicate scope title '$title' under parent: ${parentId ?: "root"}",
        ) : BusinessError

        /**
         * Hierarchy constraint violation.
         */
        data class HierarchyViolation(val reason: String, override val message: String = "Hierarchy violation: $reason") : BusinessError

        /**
         * Scope is already deleted.
         */
        data class AlreadyDeleted(val scopeId: String, override val message: String = "Scope is already deleted: $scopeId") : BusinessError

        /**
         * Scope is archived and cannot be modified.
         */
        data class ArchivedScope(val scopeId: String, override val message: String = "Cannot modify archived scope: $scopeId") : BusinessError

        /**
         * Cannot delete scope with children.
         */
        data class HasChildren(val scopeId: String, override val message: String = "Cannot delete scope with children: $scopeId") : BusinessError
    }

    /**
     * Errors related to system/infrastructure issues.
     */
    sealed interface SystemError : ScopeContractError {
        /**
         * Service is temporarily unavailable.
         */
        data class ServiceUnavailable(val service: String, override val message: String = "Service unavailable: $service") : SystemError

        /**
         * Operation timeout.
         */
        data class Timeout(val operation: String, val timeout: Duration, override val message: String = "Operation '$operation' timed out after $timeout") :
            SystemError

        /**
         * Concurrent modification detected.
         */
        data class ConcurrentModification(
            val scopeId: String,
            val expectedVersion: Long,
            val actualVersion: Long,
            override val message: String = "Concurrent modification of scope $scopeId: expected version $expectedVersion but was $actualVersion",
        ) : SystemError
    }
}

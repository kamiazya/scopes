package io.github.kamiazya.scopes.contracts.scopemanagement.errors

import kotlin.time.Duration

/**
 * Sealed interface representing all possible errors in the Scope Management contract layer.
 * These errors provide a stable API contract between bounded contexts.
 */
public sealed interface ScopeContractError {
    public val message: String

    /**
     * Errors related to invalid input data.
     */
    public sealed interface InputError : ScopeContractError {
        /**
         * Invalid scope ID format.
         */
        public data class InvalidId(public val id: String, public override val message: String = "Invalid scope ID format: $id") : InputError

        /**
         * Invalid scope title.
         */
        public data class InvalidTitle(
            public val title: String,
            public val reason: String,
            public override val message: String = "Invalid scope title: $reason",
        ) : InputError

        /**
         * Invalid scope description.
         */
        public data class InvalidDescription(
            public val description: String,
            public val reason: String,
            public override val message: String = "Invalid scope description: $reason",
        ) : InputError

        /**
         * Invalid parent scope ID.
         */
        public data class InvalidParentId(public val parentId: String, public override val message: String = "Invalid parent scope ID: $parentId") : InputError
    }

    /**
     * Errors related to business rule violations.
     */
    public sealed interface BusinessError : ScopeContractError {
        /**
         * Scope not found.
         */
        public data class NotFound(public val scopeId: String, public override val message: String = "Scope not found: $scopeId") : BusinessError

        /**
         * Duplicate scope title within the same parent.
         */
        public data class DuplicateTitle(
            public val title: String,
            public val parentId: String?,
            public override val message: String = "Duplicate scope title '$title' under parent: ${parentId ?: "root"}",
        ) : BusinessError

        /**
         * Hierarchy constraint violation.
         */
        public data class HierarchyViolation(public val reason: String, public override val message: String = "Hierarchy violation: $reason") : BusinessError

        /**
         * Scope is already deleted.
         */
        public data class AlreadyDeleted(public val scopeId: String, public override val message: String = "Scope is already deleted: $scopeId") : BusinessError

        /**
         * Scope is archived and cannot be modified.
         */
        public data class ArchivedScope(public val scopeId: String, public override val message: String = "Cannot modify archived scope: $scopeId") :
            BusinessError

        /**
         * Scope is not archived.
         */
        public data class NotArchived(public val scopeId: String, public override val message: String = "Scope is not archived: $scopeId") : BusinessError

        /**
         * Cannot delete scope with children.
         */
        public data class HasChildren(public val scopeId: String, public override val message: String = "Cannot delete scope with children: $scopeId") :
            BusinessError
    }

    /**
     * Errors related to system/infrastructure issues.
     */
    public sealed interface SystemError : ScopeContractError {
        /**
         * Service is temporarily unavailable.
         */
        public data class ServiceUnavailable(public val service: String, public override val message: String = "Service unavailable: $service") : SystemError

        /**
         * Operation timeout.
         */
        public data class Timeout(
            public val operation: String,
            public val timeout: Duration,
            public override val message: String = "Operation '$operation' timed out after $timeout",
        ) : SystemError

        /**
         * Concurrent modification detected.
         */
        public data class ConcurrentModification(
            public val scopeId: String,
            public val expectedVersion: Long,
            public val actualVersion: Long,
            public override val message: String = "Concurrent modification of scope $scopeId: expected version $expectedVersion but was $actualVersion",
        ) : SystemError
    }
}

package io.github.kamiazya.scopes.contracts.scopemanagement.errors

import kotlin.time.Duration

/**
 * Sealed interface representing all possible errors in the Scope Management contract layer.
 * These errors provide a stable API contract between bounded contexts with rich,
 * structured information for proper error handling.
 *
 * Key design principles:
 * - Rich error information without losing domain knowledge
 * - Structured data instead of plain strings
 * - Type-safe error handling for clients
 * - Clear categorization of error types
 */
public sealed interface ScopeContractError {

    /**
     * Specific validation failures for title.
     */
    public sealed interface TitleValidationFailure {
        public data object Empty : TitleValidationFailure
        public data class TooShort(public val minimumLength: Int, public val actualLength: Int) : TitleValidationFailure
        public data class TooLong(public val maximumLength: Int, public val actualLength: Int) : TitleValidationFailure
        public data class InvalidCharacters(public val prohibitedCharacters: List<Char>) : TitleValidationFailure
    }

    /**
     * Specific validation failures for description.
     */
    public sealed interface DescriptionValidationFailure {
        public data class TooLong(public val maximumLength: Int, public val actualLength: Int) : DescriptionValidationFailure
    }

    /**
     * Specific types of hierarchy violations.
     */
    public sealed interface HierarchyViolationType {
        /**
         * Circular reference detected in hierarchy.
         * @property scopeId The scope being modified
         * @property parentId The proposed parent that would create a cycle
         * @property cyclePath Optional path showing the cycle
         */
        public data class CircularReference(public val scopeId: String, public val parentId: String, public val cyclePath: List<String>? = null) :
            HierarchyViolationType

        /**
         * Maximum hierarchy depth exceeded.
         * @property scopeId The scope being added
         * @property attemptedDepth The depth that would be reached
         * @property maximumDepth The maximum allowed depth
         */
        public data class MaxDepthExceeded(public val scopeId: String, public val attemptedDepth: Int, public val maximumDepth: Int) : HierarchyViolationType

        /**
         * Maximum children per scope exceeded.
         * @property parentId The parent scope ID
         * @property currentChildrenCount Current number of children
         * @property maximumChildren Maximum allowed children
         */
        public data class MaxChildrenExceeded(public val parentId: String, public val currentChildrenCount: Int, public val maximumChildren: Int) :
            HierarchyViolationType

        /**
         * Attempt to set a scope as its own parent.
         * @property scopeId The scope ID
         */
        public data class SelfParenting(public val scopeId: String) : HierarchyViolationType

        /**
         * Parent scope not found.
         * @property scopeId The scope being modified
         * @property parentId The parent scope that was not found
         */
        public data class ParentNotFound(public val scopeId: String, public val parentId: String) : HierarchyViolationType
    }

    /**
     * Errors related to invalid input data.
     */
    public sealed interface InputError : ScopeContractError {
        /**
         * Invalid scope ID format.
         * @property id The invalid ID value
         * @property expectedFormat Optional description of expected format (e.g., "ULID format")
         */
        public data class InvalidId(public val id: String, public val expectedFormat: String? = null) : InputError

        /**
         * Invalid scope title.
         * @property title The invalid title value
         * @property validationFailure Specific reason for validation failure
         */
        public data class InvalidTitle(public val title: String, public val validationFailure: TitleValidationFailure) : InputError

        /**
         * Invalid scope description.
         * @property description The invalid description value
         * @property validationFailure Specific reason for validation failure
         */
        public data class InvalidDescription(public val description: String, public val validationFailure: DescriptionValidationFailure) : InputError

        /**
         * Invalid parent scope ID.
         * @property parentId The invalid parent ID value
         * @property expectedFormat Optional description of expected format
         */
        public data class InvalidParentId(public val parentId: String, public val expectedFormat: String? = null) : InputError
    }

    /**
     * Errors related to business rule violations.
     */
    public sealed interface BusinessError : ScopeContractError {
        /**
         * Scope not found.
         * @property scopeId The ID of the scope that was not found
         */
        public data class NotFound(public val scopeId: String) : BusinessError

        /**
         * Duplicate scope title within the same parent.
         * @property title The duplicate title
         * @property parentId The parent scope ID (null for root scopes)
         * @property existingScopeId The ID of the existing scope with the same title
         */
        public data class DuplicateTitle(public val title: String, public val parentId: String?, public val existingScopeId: String? = null) : BusinessError

        /**
         * Hierarchy constraint violation.
         * @property violation Specific type of hierarchy violation
         */
        public data class HierarchyViolation(public val violation: HierarchyViolationType) : BusinessError

        /**
         * Scope is already deleted.
         * @property scopeId The ID of the deleted scope
         */
        public data class AlreadyDeleted(public val scopeId: String) : BusinessError

        /**
         * Scope is archived and cannot be modified.
         * @property scopeId The ID of the archived scope
         */
        public data class ArchivedScope(public val scopeId: String) : BusinessError

        /**
         * Scope is not archived.
         * @property scopeId The ID of the scope that is not archived
         */
        public data class NotArchived(public val scopeId: String) : BusinessError

        /**
         * Cannot delete scope with children.
         * @property scopeId The ID of the scope that has children
         * @property childrenCount Optional count of children
         */
        public data class HasChildren(public val scopeId: String, public val childrenCount: Int? = null) : BusinessError

        /**
         * Alias not found.
         * @property alias The alias that was not found
         */
        public data class AliasNotFound(public val alias: String) : BusinessError
    }

    /**
     * Errors related to system/infrastructure issues.
     */
    public sealed interface SystemError : ScopeContractError {
        /**
         * Service is temporarily unavailable.
         */
        public data class ServiceUnavailable(public val service: String) : SystemError

        /**
         * Operation timeout.
         */
        public data class Timeout(public val operation: String, public val timeout: Duration) : SystemError

        /**
         * Concurrent modification detected.
         */
        public data class ConcurrentModification(public val scopeId: String, public val expectedVersion: Long, public val actualVersion: Long) : SystemError
    }
}

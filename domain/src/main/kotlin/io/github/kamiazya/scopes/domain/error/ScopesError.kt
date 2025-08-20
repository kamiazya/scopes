package io.github.kamiazya.scopes.domain.error

import io.github.kamiazya.scopes.domain.valueobject.ContextViewId
import io.github.kamiazya.scopes.domain.valueobject.ScopeId
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Scopes project error system reflecting domain knowledge.
 * Design centered on ubiquitous language and business concepts.
 *
 * Note: Error messages are generated in the presentation layer,
 * not in the domain layer, following Clean Architecture principles.
 */

// ===== ROOT ERROR TYPE =====

/**
 * Root type for all errors occurring in the Scopes domain.
 * Classification emphasizing business knowledge and user experience.
 */
sealed class ScopesError {
    /**
     * Contextual timestamp when the error occurred.
     * Used for troubleshooting and log correlation.
     */
    abstract val occurredAt: Instant

    /**
     * Indicator of recoverability.
     */
    abstract val recoverability: ErrorRecoverability
}

// ===== ERROR CATEGORIES =====

/**
 * Errors arising from mismatches between user intent and Scopes constraints.
 *
 * These errors are typically:
 * - User-correctable
 * - Can provide clear correction guidelines
 * - Require explanation of business rules or constraints
 */
sealed class UserIntentionError : ScopesError() {
    override val recoverability: ErrorRecoverability = ErrorRecoverability.USER_RECOVERABLE
}

/**
 * Errors related to Scopes conceptual model or structure.
 *
 * Violations of domain invariants or business rules:
 * - Scope hierarchy constraints
 * - Aspect consistency
 * - Context logical constraints
 */
sealed class ConceptualModelError : ScopesError() {
    override val recoverability: ErrorRecoverability = ErrorRecoverability.DESIGN_RECOVERABLE
}

/**
 * Errors occurring in data persistence or external system integration.
 *
 * Technical issues that users typically cannot resolve directly:
 * - Database issues
 * - File system issues
 * - Network issues
 */
sealed class InfrastructuralError : ScopesError() {
    override val recoverability: ErrorRecoverability = ErrorRecoverability.TECHNICAL_INTERVENTION_REQUIRED
}

// ===== USER INTENTION ERRORS =====

/**
 * Errors related to input values when creating or editing Scopes.
 */
sealed class ScopeInputError : UserIntentionError() {

    /**
     * Errors related to Scope IDs.
     */
    sealed class IdError : ScopeInputError() {

        data class Blank(
            override val occurredAt: Instant,
            val attemptedValue: String
        ) : IdError()

        data class InvalidFormat(
            override val occurredAt: Instant,
            val attemptedValue: String,
            val expectedFormat: String = "ULID"
        ) : IdError()
    }

    /**
     * Errors related to Scope titles.
     */
    sealed class TitleError : ScopeInputError() {

        data class Empty(
            override val occurredAt: Instant,
            val attemptedValue: String
        ) : TitleError()

        data class TooShort(
            override val occurredAt: Instant,
            val attemptedValue: String,
            val minimumLength: Int
        ) : TitleError()

        data class TooLong(
            override val occurredAt: Instant,
            val attemptedValue: String,
            val maximumLength: Int
        ) : TitleError()

        data class ContainsProhibitedCharacters(
            override val occurredAt: Instant,
            val attemptedValue: String,
            val prohibitedCharacters: List<Char>
        ) : TitleError()
    }

    /**
     * Errors related to Scope descriptions.
     */
    sealed class DescriptionError : ScopeInputError() {

        data class TooLong(
            override val occurredAt: Instant,
            val attemptedValue: String,
            val maximumLength: Int
        ) : DescriptionError()
    }

    /**
     * Errors related to Scope aliases.
     */
    sealed class AliasError : ScopeInputError() {

        data class Empty(
            override val occurredAt: Instant,
            val attemptedValue: String
        ) : AliasError()

        data class TooShort(
            override val occurredAt: Instant,
            val attemptedValue: String,
            val minimumLength: Int
        ) : AliasError()

        data class TooLong(
            override val occurredAt: Instant,
            val attemptedValue: String,
            val maximumLength: Int
        ) : AliasError()

        data class InvalidFormat(
            override val occurredAt: Instant,
            val attemptedValue: String,
            val expectedPattern: String
        ) : AliasError()
    }
}

/**
 * Errors related to Aspect definitions and values.
 */
sealed class AspectError : UserIntentionError() {

    /**
     * Errors related to Aspect keys.
     */
    sealed class KeyError : AspectError() {

        data class Empty(
            override val occurredAt: Instant
        ) : KeyError()

        data class InvalidFormat(
            override val occurredAt: Instant,
            val attemptedKey: String,
            val expectedPattern: String
        ) : KeyError()

        data class Reserved(
            override val occurredAt: Instant,
            val attemptedKey: String
        ) : KeyError()
    }

    /**
     * Errors related to Aspect values.
     */
    sealed class ValueError : AspectError() {

        data class Empty(
            override val occurredAt: Instant,
            val aspectKey: String
        ) : ValueError()

        data class NotInAllowedValues(
            override val occurredAt: Instant,
            val aspectKey: String,
            val attemptedValue: String,
            val allowedValues: List<String>
        ) : ValueError()
    }
}

/**
 * Errors related to Context creation and management.
 */
sealed class ContextError : UserIntentionError() {

    /**
     * Errors related to Context naming.
     */
    sealed class NamingError : ContextError() {

        data class Empty(
            override val occurredAt: Instant
        ) : NamingError()

        data class AlreadyExists(
            override val occurredAt: Instant,
            val attemptedName: String,
            val existingContextId: ContextViewId
        ) : NamingError()

        data class InvalidFormat(
            override val occurredAt: Instant,
            val attemptedName: String
        ) : NamingError()
    }

    /**
     * Errors related to filter expressions.
     */
    sealed class FilterError : ContextError() {

        data class InvalidSyntax(
            override val occurredAt: Instant,
            val expression: String,
            val position: Int,
            val reason: String
        ) : FilterError()

        data class UnknownAspect(
            override val occurredAt: Instant,
            val expression: String,
            val unknownAspectKey: String
        ) : FilterError()

        data class LogicalInconsistency(
            override val occurredAt: Instant,
            val expression: String,
            val reason: String
        ) : FilterError()
    }
}

// ===== CONCEPTUAL MODEL ERRORS =====

/**
 * Constraint violations related to Scope hierarchy.
 */
sealed class ScopeHierarchyError : ConceptualModelError() {

    data class CircularReference(
        override val occurredAt: Instant,
        val scopeId: ScopeId,
        val cyclePath: List<ScopeId>
    ) : ScopeHierarchyError()

    data class MaxDepthExceeded(
        override val occurredAt: Instant,
        val scopeId: ScopeId,
        val attemptedDepth: Int,
        val maximumDepth: Int
    ) : ScopeHierarchyError()

    data class MaxChildrenExceeded(
        override val occurredAt: Instant,
        val parentScopeId: ScopeId,
        val currentChildrenCount: Int,
        val maximumChildren: Int
    ) : ScopeHierarchyError()

    data class SelfParenting(
        override val occurredAt: Instant,
        val scopeId: ScopeId
    ) : ScopeHierarchyError()

    data class ParentNotFound(
        override val occurredAt: Instant,
        val scopeId: ScopeId,
        val parentId: ScopeId
    ) : ScopeHierarchyError()

    data class InvalidParentId(
        override val occurredAt: Instant,
        val invalidId: String
    ) : ScopeHierarchyError()

    data class ScopeInHierarchyNotFound(
        override val occurredAt: Instant,
        val scopeId: ScopeId
    ) : ScopeHierarchyError()
}

/**
 * Constraint violations related to Scope uniqueness.
 * Title uniqueness is enforced at ALL levels including root level.
 */
sealed class ScopeUniquenessError : ConceptualModelError() {

    /**
     * Title duplication error.
     * Duplicate titles are forbidden at all levels (root and child).
     * @param parentScopeId The parent scope ID (null for root level)
     * @param existingScopeId The ID of the existing scope with the same title
     */
    data class DuplicateTitle(
        override val occurredAt: Instant,
        val title: String,
        val parentScopeId: ScopeId?,
        val existingScopeId: ScopeId
    ) : ScopeUniquenessError()
}

/**
 * Errors related to AggregateId creation and parsing.
 */
sealed class AggregateIdError : ConceptualModelError() {

    data class InvalidType(
        override val occurredAt: Instant,
        val attemptedType: String,
        val validTypes: Set<String>
    ) : AggregateIdError()

    data class InvalidIdFormat(
        override val occurredAt: Instant,
        val attemptedId: String,
        val expectedFormat: String
    ) : AggregateIdError()

    data class InvalidUriFormat(
        override val occurredAt: Instant,
        val attemptedUri: String,
        val reason: String
    ) : AggregateIdError()

    data class EmptyValue(
        override val occurredAt: Instant,
        val field: String // "type" or "id" or "uri"
    ) : AggregateIdError()
}

/**
 * Constraint violations related to Scope aliases.
 */
sealed class ScopeAliasError : ConceptualModelError() {

    data class DuplicateAlias(
        override val occurredAt: Instant,
        val aliasName: String,
        val existingScopeId: ScopeId,
        val attemptedScopeId: ScopeId
    ) : ScopeAliasError()

    data class CanonicalAliasAlreadyExists(
        override val occurredAt: Instant,
        val scopeId: ScopeId,
        val existingCanonicalAlias: String
    ) : ScopeAliasError()

    data class AliasNotFound(
        override val occurredAt: Instant,
        val aliasName: String
    ) : ScopeAliasError()

    data class CannotRemoveCanonicalAlias(
        override val occurredAt: Instant,
        val scopeId: ScopeId,
        val canonicalAlias: String
    ) : ScopeAliasError()
}

/**
 * Constraint violations related to Context state or usage.
 */
sealed class ContextStateError : ConceptualModelError() {

    data class NotFound(
        override val occurredAt: Instant,
        val contextId: ContextViewId?,
        val contextName: String?
    ) : ContextStateError()

    data class FilterProducesNoResults(
        override val occurredAt: Instant,
        val contextName: String,
        val filterExpression: String
    ) : ContextStateError()
}

// ===== INFRASTRUCTURAL ERRORS =====

/**
 * Errors related to data persistence.
 */
sealed class PersistenceError : InfrastructuralError() {

    data class StorageUnavailable(
        override val occurredAt: Instant,
        val operation: String,
        val cause: Throwable?
    ) : PersistenceError()

    data class DataCorruption(
        override val occurredAt: Instant,
        val entityType: String,
        val entityId: String?,
        val reason: String
    ) : PersistenceError()

    data class ConcurrencyConflict(
        override val occurredAt: Instant,
        val entityType: String,
        val entityId: String,
        val expectedVersion: String,
        val actualVersion: String
    ) : PersistenceError()
}

/**
 * Errors related to external system integration.
 */
sealed class ExternalSystemError : InfrastructuralError() {

    data class ServiceUnavailable(
        override val occurredAt: Instant,
        val serviceName: String,
        val operation: String
    ) : ExternalSystemError()

    data class AuthenticationFailed(
        override val occurredAt: Instant,
        val serviceName: String
    ) : ExternalSystemError()
}

// ===== SUPPORT TYPES =====

/**
 * Enumeration representing error recoverability.
 * Provides guidance for user experience and support strategy.
 */
enum class ErrorRecoverability {
    /**
     * Resolvable by users correcting input or operations.
     * Should provide clear correction guidelines in UI.
     */
    USER_RECOVERABLE,

    /**
     * Requires design or configuration review.
     * Needs deeper understanding or plan revision.
     */
    DESIGN_RECOVERABLE,

    /**
     * Requires system administrator or technical intervention.
     * Difficult for users to resolve directly.
     */
    TECHNICAL_INTERVENTION_REQUIRED
}

// ===== HELPER FUNCTIONS =====

/**
 * Helper function to get current timestamp for error creation.
 * This can be overridden for testing purposes.
 */
fun currentTimestamp(): Instant = Clock.System.now()

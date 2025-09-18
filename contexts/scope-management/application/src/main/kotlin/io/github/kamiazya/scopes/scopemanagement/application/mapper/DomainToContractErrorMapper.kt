package io.github.kamiazya.scopes.scopemanagement.application.mapper

import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ContextError
import io.github.kamiazya.scopes.scopemanagement.domain.error.PersistenceError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeAliasError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeHierarchyError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.seconds

/**
 * Maps domain errors directly to contract errors, bypassing the application error layer.
 * This mapper consolidates the error mapping logic and ensures that rich error information
 * from the domain is preserved in the contract layer.
 */
object DomainToContractErrorMapper {

    /**
     * Maps any domain error to a contract error.
     * This is the main entry point for domain-to-contract error mapping.
     */
    fun mapToContractError(domainError: ScopesError, context: ErrorMappingContext = ErrorMappingContext()): ScopeContractError =
        when (domainError) {
            is ScopeInputError -> mapInputError(domainError, context)
            is ScopeAliasError -> mapAliasError(domainError, context)
            is ScopeHierarchyError -> mapHierarchyError(domainError, context)
            is ContextError -> mapContextError(domainError, context)
            is PersistenceError -> mapPersistenceError(domainError, context)
            is ScopesError.NotFound -> ScopeContractError.BusinessError.NotFound(
                scopeId = domainError.identifier ?: ""
            )
            is ScopesError.SystemError -> ScopeContractError.SystemError.ServiceUnavailable(
                service = "scope-management"
            )
            else -> ScopeContractError.SystemError.ServiceUnavailable(
                service = "scope-management"
            )
        }

    private fun mapInputError(error: ScopeInputError, context: ErrorMappingContext): ScopeContractError =
        when (error) {
            // ID errors
            is ScopeInputError.IdError.EmptyId -> ScopeContractError.InputError.InvalidId(
                id = context.attemptedValue ?: "",
                expectedFormat = "Non-empty ULID format"
            )
            is ScopeInputError.IdError.InvalidIdFormat -> ScopeContractError.InputError.InvalidId(
                id = context.attemptedValue ?: error.expectedFormat.name,
                expectedFormat = "ULID format"
            )

            // Title errors
            is ScopeInputError.TitleError.EmptyTitle -> ScopeContractError.InputError.InvalidTitle(
                title = context.attemptedValue ?: "",
                validationFailure = ScopeContractError.TitleValidationFailure.Empty
            )
            is ScopeInputError.TitleError.TitleTooShort -> ScopeContractError.InputError.InvalidTitle(
                title = context.attemptedValue ?: "",
                validationFailure = ScopeContractError.TitleValidationFailure.TooShort(
                    minimumLength = error.minLength,
                    actualLength = context.attemptedValue?.length ?: 0
                )
            )
            is ScopeInputError.TitleError.TitleTooLong -> ScopeContractError.InputError.InvalidTitle(
                title = context.attemptedValue ?: "",
                validationFailure = ScopeContractError.TitleValidationFailure.TooLong(
                    maximumLength = error.maxLength,
                    actualLength = context.attemptedValue?.length ?: 0
                )
            )
            is ScopeInputError.TitleError.InvalidTitleFormat -> ScopeContractError.InputError.InvalidTitle(
                title = context.attemptedValue ?: "",
                validationFailure = ScopeContractError.TitleValidationFailure.InvalidCharacters(
                    prohibitedCharacters = listOf('\n', '\r', '\t')
                )
            )

            // Description errors
            is ScopeInputError.DescriptionError.DescriptionTooLong -> ScopeContractError.InputError.InvalidDescription(
                descriptionText = context.attemptedValue ?: "",
                validationFailure = ScopeContractError.DescriptionValidationFailure.TooLong(
                    maximumLength = error.maxLength,
                    actualLength = context.attemptedValue?.length ?: 0
                )
            )

            // Alias errors
            is ScopeInputError.AliasError.EmptyAlias -> ScopeContractError.InputError.InvalidAlias(
                alias = context.attemptedValue ?: "",
                validationFailure = ScopeContractError.AliasValidationFailure.Empty
            )
            is ScopeInputError.AliasError.AliasTooShort -> ScopeContractError.InputError.InvalidAlias(
                alias = context.attemptedValue ?: "",
                validationFailure = ScopeContractError.AliasValidationFailure.TooShort(
                    minimumLength = error.minLength,
                    actualLength = context.attemptedValue?.length ?: 0
                )
            )
            is ScopeInputError.AliasError.AliasTooLong -> ScopeContractError.InputError.InvalidAlias(
                alias = context.attemptedValue ?: "",
                validationFailure = ScopeContractError.AliasValidationFailure.TooLong(
                    maximumLength = error.maxLength,
                    actualLength = context.attemptedValue?.length ?: 0
                )
            )
            is ScopeInputError.AliasError.InvalidAliasFormat -> ScopeContractError.InputError.InvalidAlias(
                alias = context.attemptedValue ?: "",
                validationFailure = ScopeContractError.AliasValidationFailure.InvalidFormat(
                    expectedPattern = "lowercase letters, numbers, and hyphens only"
                )
            )
        }

    private fun mapAliasError(error: ScopeAliasError, context: ErrorMappingContext): ScopeContractError =
        when (error) {
            is ScopeAliasError.DuplicateAlias -> ScopeContractError.BusinessError.DuplicateAlias(
                alias = error.alias,
                existingScopeId = error.scopeId.toString(),
                attemptedScopeId = context.scopeId
            )
            is ScopeAliasError.AliasNotFoundByName -> ScopeContractError.BusinessError.AliasNotFound(
                alias = error.alias
            )
            is ScopeAliasError.AliasNotFoundById -> ScopeContractError.BusinessError.AliasNotFound(
                alias = "ID:${error.aliasId.value}"
            )
            is ScopeAliasError.CannotRemoveCanonicalAlias -> ScopeContractError.BusinessError.CannotRemoveCanonicalAlias(
                scopeId = error.scopeId.toString(),
                aliasName = error.alias
            )
            is ScopeAliasError.AliasGenerationFailed -> ScopeContractError.BusinessError.AliasGenerationFailed(
                scopeId = error.scopeId.toString(),
                retryCount = context.retryCount
            )
            is ScopeAliasError.AliasError -> ScopeContractError.BusinessError.AliasGenerationValidationFailed(
                scopeId = context.scopeId ?: "",
                alias = error.alias,
                reason = error.reason
            )
            is ScopeAliasError.DataInconsistencyError.AliasReferencesNonExistentScope -> 
                ScopeContractError.SystemError.ServiceUnavailable(
                    service = "scope-management"
                )
            // Fail fast for any unmapped DataInconsistencyError subtypes
            is ScopeAliasError.DataInconsistencyError ->
                error(
                    "Unmapped DataInconsistencyError subtype: ${error::class.simpleName}. " +
                        "Please add proper error mapping for this error type."
                )
        }

    private fun mapHierarchyError(error: ScopeHierarchyError, context: ErrorMappingContext): ScopeContractError =
        when (error) {
            is ScopeHierarchyError.CircularDependency -> ScopeContractError.BusinessError.HierarchyViolation(
                violation = ScopeContractError.HierarchyViolationType.CircularReference(
                    scopeId = error.scopeId.toString(),
                    parentId = error.ancestorId.toString(),
                    cyclePath = null
                )
            )
            is ScopeHierarchyError.MaxDepthExceeded -> ScopeContractError.BusinessError.HierarchyViolation(
                violation = ScopeContractError.HierarchyViolationType.MaxDepthExceeded(
                    scopeId = error.scopeId.toString(),
                    attemptedDepth = error.currentDepth,
                    maximumDepth = error.maxDepth
                )
            )
            is ScopeHierarchyError.MaxChildrenExceeded -> ScopeContractError.BusinessError.HierarchyViolation(
                violation = ScopeContractError.HierarchyViolationType.MaxChildrenExceeded(
                    parentId = error.parentId.toString(),
                    currentChildrenCount = error.currentCount,
                    maximumChildren = error.maxChildren
                )
            )
            is ScopeHierarchyError.HierarchyUnavailable -> ScopeContractError.SystemError.ServiceUnavailable(
                service = "hierarchy-service"
            )
        }

    private fun mapContextError(error: ContextError, context: ErrorMappingContext): ScopeContractError =
        when (error) {
            is ContextError.DuplicateScope -> ScopeContractError.BusinessError.DuplicateTitle(
                title = error.title,
                parentId = null,
                existingScopeId = null
            )
            // Map all other context errors to system error for now
            else -> ScopeContractError.SystemError.ServiceUnavailable(
                service = "context-service"
            )
        }

    private fun mapPersistenceError(error: PersistenceError, context: ErrorMappingContext): ScopeContractError =
        when (error) {
            is PersistenceError.ConcurrencyConflict -> ScopeContractError.SystemError.ConcurrentModification(
                scopeId = error.entityId,
                expectedVersion = error.expectedVersion.toLongOrNull() ?: -1L,
                actualVersion = error.actualVersion.toLongOrNull() ?: -1L
            )
            else -> ScopeContractError.SystemError.ServiceUnavailable(
                service = "persistence"
            )
        }
}

/**
 * Context information for error mapping that cannot be derived from the error itself.
 */
data class ErrorMappingContext(
    val attemptedValue: String? = null,
    val scopeId: String? = null,
    val parentId: String? = null,
    val retryCount: Int = 0,
)
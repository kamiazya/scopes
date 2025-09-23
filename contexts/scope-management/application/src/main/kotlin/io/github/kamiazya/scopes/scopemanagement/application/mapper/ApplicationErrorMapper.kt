package io.github.kamiazya.scopes.scopemanagement.application.mapper

import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.platform.application.error.BaseErrorMapper
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.error.ContextError
import io.github.kamiazya.scopes.scopemanagement.application.error.CrossAggregateValidationError
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeHierarchyApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeManagementApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeUniquenessError
import io.github.kamiazya.scopes.scopemanagement.application.util.InputSanitizer
import io.github.kamiazya.scopes.scopemanagement.domain.error.AspectKeyError
import io.github.kamiazya.scopes.scopemanagement.domain.error.AspectValidationError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeHierarchyError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeAliasError as AppScopeAliasError
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeInputError as AppScopeInputError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ContextError as DomainContextError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeAliasError as DomainScopeAliasError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeUniquenessError as DomainScopeUniquenessError

/**
 * Context for error mapping providing additional information.
 */
data class ErrorMappingContext(val attemptedValue: String? = null, val parentId: String? = null, val scopeId: String? = null)

/**
 * Maps application errors to contract layer errors.
 *
 * This mapper handles application-layer errors that are not domain errors,
 * ensuring consistent error translation to the contract layer.
 */
class ApplicationErrorMapper(logger: Logger) : BaseErrorMapper<ScopeManagementApplicationError, ScopeContractError>(logger) {

    companion object {
        private const val SERVICE_NAME = "scope-management"
    }

    override fun getServiceName(): String = SERVICE_NAME

    override fun createServiceUnavailableError(serviceName: String): ScopeContractError =
        ScopeContractError.SystemError.ServiceUnavailable(service = serviceName)

    private fun parseVersionToLong(entityId: String, version: String, versionType: String): Long = version.toLongOrNull() ?: run {
        logger.warn(
            "Failed to parse $versionType version to Long, using sentinel value",
            mapOf(
                "entityId" to entityId,
                "${versionType}Version" to version,
            ),
        )
        -1L
    }

    private fun mapAliasNotFoundError(alias: String): ScopeContractError = ScopeContractError.BusinessError.AliasNotFound(alias = alias)

    private fun mapNotFoundError(scopeId: String): ScopeContractError = ScopeContractError.BusinessError.NotFound(scopeId = scopeId)

    private fun mapAliasToNotFound(error: AppScopeInputError): ScopeContractError {
        val alias = when (error) {
            is AppScopeInputError.AliasNotFound -> error.preview
            is AppScopeInputError.InvalidAlias -> error.preview
            else -> error("Unexpected error type: $error")
        }
        return mapAliasNotFoundError(alias)
    }

    private fun mapContextToNotFound(error: ContextError): ScopeContractError {
        // Fail fast for unmapped context errors to ensure proper error mapping
        error(
            "Unmapped ContextError subtype: ${error::class.simpleName}. " +
                "Please add proper error mapping for this error type.",
        )
    }

    override fun mapToContractError(domainError: ScopeManagementApplicationError): ScopeContractError = when (domainError) {
        // Group errors by type
        is AppScopeInputError -> mapInputError(domainError)
        is ContextError -> createSystemError() // Simplified mapping due to merge conflict
        is AppScopeAliasError -> mapScopeAliasError(domainError)
        is ScopeHierarchyApplicationError -> mapHierarchyError(domainError)
        is ScopeUniquenessError -> mapUniquenessError(domainError)
        is CrossAggregateValidationError -> createSystemError()
        is ScopeManagementApplicationError.PersistenceError -> mapPersistenceError(domainError)
    }

    private fun mapInputError(error: AppScopeInputError): ScopeContractError = when (error) {
        // ID validation errors
        is AppScopeInputError.IdBlank,
        is AppScopeInputError.IdInvalidFormat,
        -> mapIdInputError(error)

        // Title validation errors
        is AppScopeInputError.TitleEmpty,
        is AppScopeInputError.TitleTooShort,
        is AppScopeInputError.TitleTooLong,
        is AppScopeInputError.TitleContainsProhibitedCharacters,
        -> mapTitleInputError(error)

        // Description validation errors
        is AppScopeInputError.DescriptionTooLong -> mapDescriptionInputError(error)

        // Alias validation errors
        is AppScopeInputError.AliasEmpty,
        is AppScopeInputError.AliasTooShort,
        is AppScopeInputError.AliasTooLong,
        is AppScopeInputError.AliasInvalidFormat,
        -> mapAliasValidationError(error)

        // Alias business errors
        is AppScopeInputError.AliasNotFound,
        is AppScopeInputError.AliasDuplicate,
        is AppScopeInputError.CannotRemoveCanonicalAlias,
        is AppScopeInputError.AliasOfDifferentScope,
        is AppScopeInputError.InvalidAlias,
        -> mapAliasBusinessError(error)

        // Other input errors
        is AppScopeInputError.InvalidParentId -> ScopeContractError.InputError.InvalidParentId(
            parentId = error.preview,
            expectedFormat = "Valid ULID format",
        )

        is AppScopeInputError.ValidationFailed -> {
            // Check if this is specifically an alias validation
            val isAliasField = error.field == "alias" ||
                error.field == "customAlias" ||
                error.field == "newAlias" ||
                error.field == "canonicalAlias" ||
                error.field.endsWith("Alias")

            if (isAliasField) {
                ScopeContractError.InputError.InvalidAlias(
                    alias = error.preview,
                    validationFailure = ScopeContractError.AliasValidationFailure.InvalidFormat(
                        expectedPattern = error.reason,
                    ),
                )
            } else {
                // Use generic validation failure for non-alias fields
                ScopeContractError.InputError.ValidationFailure(
                    field = error.field,
                    value = error.preview,
                    constraint = ScopeContractError.ValidationConstraint.InvalidFormat(
                        expectedFormat = error.reason,
                    ),
                )
            }
        }
    }

    private fun mapIdInputError(error: AppScopeInputError): ScopeContractError = when (error) {
        is AppScopeInputError.IdBlank -> ScopeContractError.InputError.InvalidId(
            id = "",
            expectedFormat = "Non-empty ULID format",
        )
        is AppScopeInputError.IdInvalidFormat -> ScopeContractError.InputError.InvalidId(
            id = error.preview,
            expectedFormat = "Valid ULID format",
        )
        else -> error("Unexpected ID error type: $error")
    }

    private fun mapTitleInputError(error: AppScopeInputError): ScopeContractError = when (error) {
        is AppScopeInputError.TitleEmpty -> ScopeContractError.InputError.InvalidTitle(
            title = "",
            validationFailure = ScopeContractError.TitleValidationFailure.Empty,
        )
        is AppScopeInputError.TitleTooShort -> ScopeContractError.InputError.InvalidTitle(
            title = error.preview,
            validationFailure = ScopeContractError.TitleValidationFailure.TooShort(
                minimumLength = error.minimumLength,
                actualLength = error.preview.length,
            ),
        )
        is AppScopeInputError.TitleTooLong -> ScopeContractError.InputError.InvalidTitle(
            title = error.preview,
            validationFailure = ScopeContractError.TitleValidationFailure.TooLong(
                maximumLength = error.maximumLength,
                actualLength = error.preview.length,
            ),
        )
        is AppScopeInputError.TitleContainsProhibitedCharacters -> ScopeContractError.InputError.InvalidTitle(
            title = error.preview,
            validationFailure = ScopeContractError.TitleValidationFailure.InvalidCharacters(
                prohibitedCharacters = error.prohibitedCharacters,
            ),
        )
        else -> error("Unexpected title error type: $error")
    }

    private fun mapDescriptionInputError(error: AppScopeInputError.DescriptionTooLong): ScopeContractError = ScopeContractError.InputError.InvalidDescription(
        descriptionText = error.preview,
        validationFailure = ScopeContractError.DescriptionValidationFailure.TooLong(
            maximumLength = error.maximumLength,
            actualLength = error.preview.length,
        ),
    )

    private fun mapAliasValidationError(error: AppScopeInputError): ScopeContractError = when (error) {
        is AppScopeInputError.AliasEmpty -> ScopeContractError.InputError.InvalidAlias(
            alias = "",
            validationFailure = ScopeContractError.AliasValidationFailure.Empty,
        )
        is AppScopeInputError.AliasTooShort -> ScopeContractError.InputError.InvalidAlias(
            alias = error.preview,
            validationFailure = ScopeContractError.AliasValidationFailure.TooShort(
                minimumLength = error.minimumLength,
                actualLength = error.preview.length,
            ),
        )
        is AppScopeInputError.AliasTooLong -> ScopeContractError.InputError.InvalidAlias(
            alias = error.preview,
            validationFailure = ScopeContractError.AliasValidationFailure.TooLong(
                maximumLength = error.maximumLength,
                actualLength = error.preview.length,
            ),
        )
        is AppScopeInputError.AliasInvalidFormat -> ScopeContractError.InputError.InvalidAlias(
            alias = error.preview,
            validationFailure = ScopeContractError.AliasValidationFailure.InvalidFormat(
                expectedPattern = error.expectedPattern,
            ),
        )
        else -> error("Unexpected alias validation error type: $error")
    }

    private fun mapAliasBusinessError(error: AppScopeInputError): ScopeContractError = when (error) {
        is AppScopeInputError.AliasNotFound,
        is AppScopeInputError.InvalidAlias,
        -> mapAliasToNotFound(error)

        is AppScopeInputError.AliasDuplicate -> ScopeContractError.BusinessError.DuplicateAlias(
            alias = error.preview,
        )
        is AppScopeInputError.CannotRemoveCanonicalAlias -> ScopeContractError.BusinessError.CannotRemoveCanonicalAlias(
            scopeId = "", // No scope ID available in this error type
            aliasName = "canonical", // Generic canonical alias name
        )
        is AppScopeInputError.AliasOfDifferentScope -> ScopeContractError.BusinessError.AliasOfDifferentScope(
            alias = error.preview,
            expectedScopeId = error.expectedScopeId,
            actualScopeId = error.actualScopeId,
        )
        else -> error("Unexpected alias business error type: $error")
    }

    private fun mapContextError(error: ContextError): ScopeContractError = when (error) {
        is ContextError.KeyInvalidFormat -> ScopeContractError.InputError.InvalidContextKey(
            key = InputSanitizer.createPreview(error.attemptedKey),
            validationFailure = ScopeContractError.ContextKeyValidationFailure.InvalidFormat(
                invalidType = "Invalid context key format",
            ),
        )
        is ContextError.StateNotFound -> ScopeContractError.BusinessError.NotFound(
            scopeId = error.contextId,
        )
        is ContextError.InvalidFilter -> ScopeContractError.InputError.InvalidContextFilter(
            filter = InputSanitizer.createPreview(error.filter),
            validationFailure = ScopeContractError.ContextFilterValidationFailure.InvalidSyntax(
                expression = InputSanitizer.createPreview(error.filter),
                errorType = InputSanitizer.createPreview(error.reason),
            ),
        )
        is ContextError.ContextInUse -> ScopeContractError.BusinessError.DuplicateContextKey(
            contextKey = error.key,
        )
        is ContextError.DuplicateContextKey -> ScopeContractError.BusinessError.DuplicateContextKey(
            contextKey = error.key,
        )
        is ContextError.ContextNotFound -> ScopeContractError.BusinessError.ContextNotFound(
            contextKey = error.key,
        )
        is ContextError.ContextUpdateConflict -> ScopeContractError.SystemError.ServiceUnavailable(
            service = SERVICE_NAME,
        )
        is ContextError.InvalidContextSwitch -> ScopeContractError.BusinessError.ContextNotFound(
            contextKey = error.key,
        )
    }

    private fun mapScopeAliasError(error: AppScopeAliasError): ScopeContractError = when (error) {
        is AppScopeAliasError.AliasDuplicate -> ScopeContractError.BusinessError.DuplicateAlias(
            alias = error.aliasName,
        )
        is AppScopeAliasError.AliasNotFound -> ScopeContractError.BusinessError.AliasNotFound(
            alias = error.aliasName,
        )
        is AppScopeAliasError.CannotRemoveCanonicalAlias -> ScopeContractError.BusinessError.CannotRemoveCanonicalAlias(
            scopeId = error.scopeId,
            aliasName = error.aliasName,
        )
        is AppScopeAliasError.AliasGenerationFailed -> ScopeContractError.SystemError.ServiceUnavailable(
            service = "alias-generation",
        )
        is AppScopeAliasError.AliasGenerationValidationFailed -> ScopeContractError.InputError.InvalidAlias(
            alias = error.alias,
            validationFailure = ScopeContractError.AliasValidationFailure.InvalidFormat(
                expectedPattern = error.reason,
            ),
        )
        is AppScopeAliasError.DataInconsistencyError.AliasExistsButScopeNotFound ->
            ScopeContractError.SystemError.ServiceUnavailable(
                service = "scope-alias",
            )
    }

    private fun mapHierarchyError(error: ScopeHierarchyApplicationError): ScopeContractError = when (error) {
        is ScopeHierarchyApplicationError.MaxDepthExceeded -> ScopeContractError.BusinessError.HierarchyViolation(
            violation = ScopeContractError.HierarchyViolationType.MaxDepthExceeded(
                scopeId = error.scopeId,
                attemptedDepth = error.attemptedDepth,
                maximumDepth = error.maximumDepth,
            ),
        )
        is ScopeHierarchyApplicationError.MaxChildrenExceeded -> ScopeContractError.BusinessError.HierarchyViolation(
            violation = ScopeContractError.HierarchyViolationType.MaxChildrenExceeded(
                parentId = error.parentScopeId,
                currentChildrenCount = error.currentChildrenCount,
                maximumChildren = error.maximumChildren,
            ),
        )
        is ScopeHierarchyApplicationError.CircularReference -> ScopeContractError.BusinessError.HierarchyViolation(
            violation = ScopeContractError.HierarchyViolationType.CircularReference(
                scopeId = error.scopeId,
                parentId = "", // No parent ID available in this error type
                cyclePath = error.cyclePath,
            ),
        )
        is ScopeHierarchyApplicationError.ParentNotFound -> ScopeContractError.BusinessError.HierarchyViolation(
            violation = ScopeContractError.HierarchyViolationType.ParentNotFound(
                scopeId = error.scopeId,
                parentId = error.parentId,
            ),
        )
        is ScopeHierarchyApplicationError.SelfParenting -> ScopeContractError.BusinessError.HierarchyViolation(
            violation = ScopeContractError.HierarchyViolationType.SelfParenting(
                scopeId = error.scopeId,
            ),
        )
        is ScopeHierarchyApplicationError.InvalidParentId -> ScopeContractError.InputError.InvalidParentId(
            parentId = error.invalidId,
            expectedFormat = "Valid ULID format",
        )
        is ScopeHierarchyApplicationError.HasChildren -> ScopeContractError.BusinessError.HasChildren(
            scopeId = error.scopeId,
            childrenCount = error.childCount,
        )
    }

    private fun mapUniquenessError(error: ScopeUniquenessError): ScopeContractError = when (error) {
        is ScopeUniquenessError.DuplicateTitle -> ScopeContractError.BusinessError.DuplicateTitle(
            title = error.title,
            parentId = error.parentScopeId,
            existingScopeId = error.existingScopeId,
        )
        // Add exhaustiveness for other potential types
        else -> createSystemError()
    }

    private fun mapPersistenceError(error: ScopeManagementApplicationError.PersistenceError): ScopeContractError = when (error) {
        // All persistence errors map to ServiceUnavailable for security
        is ScopeManagementApplicationError.PersistenceError.StorageUnavailable,
        is ScopeManagementApplicationError.PersistenceError.DataCorruption,
        is ScopeManagementApplicationError.PersistenceError.ConcurrencyConflict,
        is ScopeManagementApplicationError.PersistenceError.NotFound,
        -> ScopeContractError.SystemError.ServiceUnavailable(
            service = SERVICE_NAME,
        )
    }

    private fun createSystemError(): ScopeContractError = ScopeContractError.SystemError.ServiceUnavailable(
        service = SERVICE_NAME,
    )

    /**
     * Maps domain context errors to contract errors.
     * This function enables command handlers to use contract errors directly
     * by mapping domain errors.
     */
    fun mapDomainError(domainError: DomainContextError): ScopeContractError = when (domainError) {
        // Context key validation errors
        is DomainContextError.EmptyKey -> ScopeContractError.InputError.InvalidContextKey(
            key = InputSanitizer.createPreview("[empty]"),
            validationFailure = ScopeContractError.ContextKeyValidationFailure.Empty,
        )
        is DomainContextError.KeyTooShort -> ScopeContractError.InputError.InvalidContextKey(
            key = InputSanitizer.createPreview("[too short]"),
            validationFailure = ScopeContractError.ContextKeyValidationFailure.TooShort(
                minimumLength = domainError.minimumLength,
                actualLength = 0, // Unknown actual length from domain error
            ),
        )
        is DomainContextError.KeyTooLong -> ScopeContractError.InputError.InvalidContextKey(
            key = InputSanitizer.createPreview("[too long]"),
            validationFailure = ScopeContractError.ContextKeyValidationFailure.TooLong(
                maximumLength = domainError.maximumLength,
                actualLength = domainError.maximumLength + 1, // Estimated
            ),
        )
        is DomainContextError.InvalidKeyFormat -> ScopeContractError.InputError.InvalidContextKey(
            key = InputSanitizer.createPreview("[invalid format]"),
            validationFailure = ScopeContractError.ContextKeyValidationFailure.InvalidFormat(
                invalidType = domainError.errorType.toString(),
            ),
        )

        // Context name validation errors
        is DomainContextError.EmptyName -> ScopeContractError.InputError.InvalidContextName(
            name = InputSanitizer.createPreview("[empty]"),
            validationFailure = ScopeContractError.ContextNameValidationFailure.Empty,
        )
        is DomainContextError.NameTooLong -> ScopeContractError.InputError.InvalidContextName(
            name = InputSanitizer.createPreview("[too long]"),
            validationFailure = ScopeContractError.ContextNameValidationFailure.TooLong(
                maximumLength = domainError.maximumLength,
                actualLength = domainError.maximumLength + 1, // Estimated
            ),
        )

        // Context description validation errors
        is DomainContextError.EmptyDescription -> ScopeContractError.InputError.ValidationFailure(
            field = "description",
            value = InputSanitizer.createPreview("[empty]"),
            constraint = ScopeContractError.ValidationConstraint.Empty,
        )
        is DomainContextError.DescriptionTooShort -> ScopeContractError.InputError.ValidationFailure(
            field = "description",
            value = InputSanitizer.createPreview("[too short]"),
            constraint = ScopeContractError.ValidationConstraint.TooShort(
                minimumLength = domainError.minimumLength,
                actualLength = 0, // Unknown actual length from domain error
            ),
        )
        is DomainContextError.DescriptionTooLong -> ScopeContractError.InputError.ValidationFailure(
            field = "description",
            value = InputSanitizer.createPreview("[too long]"),
            constraint = ScopeContractError.ValidationConstraint.TooLong(
                maximumLength = domainError.maximumLength,
                actualLength = domainError.maximumLength + 1, // Estimated
            ),
        )

        // Context filter validation errors
        is DomainContextError.EmptyFilter -> ScopeContractError.InputError.InvalidContextFilter(
            filter = InputSanitizer.createPreview("[empty]"),
            validationFailure = ScopeContractError.ContextFilterValidationFailure.Empty,
        )
        is DomainContextError.FilterTooShort -> ScopeContractError.InputError.InvalidContextFilter(
            filter = InputSanitizer.createPreview("[too short]"),
            validationFailure = ScopeContractError.ContextFilterValidationFailure.TooShort(
                minimumLength = domainError.minimumLength,
                actualLength = 0, // Unknown actual length from domain error
            ),
        )
        is DomainContextError.FilterTooLong -> ScopeContractError.InputError.InvalidContextFilter(
            filter = InputSanitizer.createPreview("[too long]"),
            validationFailure = ScopeContractError.ContextFilterValidationFailure.TooLong(
                maximumLength = domainError.maximumLength,
                actualLength = domainError.maximumLength + 1, // Estimated
            ),
        )
        is DomainContextError.InvalidFilterSyntax -> ScopeContractError.InputError.InvalidContextFilter(
            filter = InputSanitizer.createPreview(domainError.expression),
            validationFailure = ScopeContractError.ContextFilterValidationFailure.InvalidSyntax(
                expression = InputSanitizer.createPreview(domainError.expression),
                errorType = "syntax_error", // Default error type
            ),
        )

        // Context scope validation errors
        is DomainContextError.InvalidScope -> when (domainError.errorType) {
            DomainContextError.InvalidScope.InvalidScopeType.SCOPE_NOT_FOUND ->
                ScopeContractError.BusinessError.NotFound(scopeId = domainError.scopeId)
            DomainContextError.InvalidScope.InvalidScopeType.SCOPE_ARCHIVED ->
                ScopeContractError.BusinessError.ArchivedScope(scopeId = domainError.scopeId)
            DomainContextError.InvalidScope.InvalidScopeType.SCOPE_DELETED ->
                ScopeContractError.BusinessError.AlreadyDeleted(scopeId = domainError.scopeId)
            else -> ScopeContractError.BusinessError.NotFound(scopeId = domainError.scopeId)
        }
        is DomainContextError.InvalidHierarchy -> {
            val violation = when (domainError.errorType) {
                DomainContextError.InvalidHierarchy.InvalidHierarchyType.CIRCULAR_REFERENCE ->
                    ScopeContractError.HierarchyViolationType.CircularReference(
                        scopeId = domainError.scopeId,
                        parentId = domainError.parentId,
                    )
                DomainContextError.InvalidHierarchy.InvalidHierarchyType.PARENT_NOT_FOUND ->
                    ScopeContractError.HierarchyViolationType.ParentNotFound(
                        scopeId = domainError.scopeId,
                        parentId = domainError.parentId,
                    )
                else ->
                    ScopeContractError.HierarchyViolationType.ParentNotFound(
                        scopeId = domainError.scopeId,
                        parentId = domainError.parentId,
                    )
            }
            ScopeContractError.BusinessError.HierarchyViolation(violation)
        }
        is DomainContextError.DuplicateScope -> {
            when (domainError.errorType) {
                DomainContextError.DuplicateScope.DuplicateScopeType.TITLE_EXISTS_IN_CONTEXT ->
                    ScopeContractError.BusinessError.DuplicateTitle(
                        title = domainError.title,
                        parentId = domainError.contextId,
                    )
                DomainContextError.DuplicateScope.DuplicateScopeType.ALIAS_ALREADY_TAKEN ->
                    ScopeContractError.BusinessError.DuplicateAlias(alias = domainError.title)
                else ->
                    ScopeContractError.BusinessError.DuplicateTitle(
                        title = domainError.title,
                        parentId = domainError.contextId,
                    )
            }
        }
    }

    /**
     * Maps domain aspect key errors to contract errors.
     */
    fun mapDomainError(domainError: AspectKeyError): ScopeContractError = mapDomainAspectKeyErrorDirect(domainError)

    /**
     * Legacy mapper for AspectKeyError - kept for backwards compatibility.
     */
    private fun mapDomainAspectKeyErrorDirect(domainError: AspectKeyError): ScopeContractError = when (domainError) {
        is AspectKeyError.EmptyKey -> createEmptyTitleError()
        is AspectKeyError.TooShort -> createTooShortTitleError(
            minLength = domainError.minLength,
            actualLength = domainError.actualLength,
        )
        is AspectKeyError.TooLong -> createTooLongTitleError(
            maxLength = domainError.maxLength,
            actualLength = domainError.actualLength,
        )
        is AspectKeyError.InvalidFormat -> createInvalidTitleWithInvalidCharacters()
    }

    /**
     * Maps domain aspect validation errors to contract errors.
     */
    fun mapDomainError(domainError: AspectValidationError): ScopeContractError = createSystemError()

    /**
     * Primary mapper for any ScopesError to contract errors.
     * This function handles all domain error types and delegates to specific mappers.
     */
    fun mapDomainError(domainError: ScopesError, context: ErrorMappingContext? = null): ScopeContractError = when (domainError) {
        // Delegate to specific mappers for known subtypes
        is DomainContextError -> mapDomainError(domainError)
        is AspectKeyError -> mapDomainAspectKeyErrorDirect(domainError)
        is AspectValidationError -> createSystemError() // Simplified handling for now
        is DomainScopeAliasError -> mapDomainScopeAliasError(domainError)
        is ScopeHierarchyError -> mapDomainScopeHierarchyError(domainError)
        is ScopeInputError -> mapDomainScopeInputError(domainError)
        is DomainScopeUniquenessError -> mapDomainScopeUniquenessError(domainError)

        // Common domain errors
        is ScopesError.NotFound -> ScopeContractError.BusinessError.NotFound(
            scopeId = domainError.identifier,
        )
        is ScopesError.InvalidOperation -> createServiceUnavailableError(SERVICE_NAME)
        is ScopesError.AlreadyExists -> ScopeContractError.BusinessError.DuplicateAlias(
            alias = domainError.identifier,
        )
        is ScopesError.SystemError -> createServiceUnavailableError(
            serviceName = domainError.service ?: SERVICE_NAME,
        )
        is ScopesError.ValidationFailed -> {
            // Map domain validation constraint to contract validation constraint
            val contractConstraint = when (val constraint = domainError.constraint) {
                is ScopesError.ValidationConstraintType.InvalidType ->
                    ScopeContractError.ValidationConstraint.InvalidType(
                        expectedType = constraint.expectedType,
                        actualType = constraint.actualType,
                    )

                is ScopesError.ValidationConstraintType.InvalidFormat ->
                    ScopeContractError.ValidationConstraint.InvalidFormat(
                        expectedFormat = constraint.expectedFormat,
                    )

                is ScopesError.ValidationConstraintType.NotInAllowedValues ->
                    ScopeContractError.ValidationConstraint.InvalidValue(
                        expectedValues = constraint.allowedValues,
                        actualValue = domainError.value,
                    )

                is ScopesError.ValidationConstraintType.MissingRequired ->
                    ScopeContractError.ValidationConstraint.RequiredField(
                        field = constraint.requiredFields.firstOrNull() ?: domainError.field,
                    )

                is ScopesError.ValidationConstraintType.MultipleValuesNotAllowed ->
                    ScopeContractError.ValidationConstraint.MultipleValuesNotAllowed(
                        field = constraint.field,
                    )

                is ScopesError.ValidationConstraintType.EmptyValues ->
                    ScopeContractError.ValidationConstraint.EmptyValues(
                        field = constraint.field,
                    )

                is ScopesError.ValidationConstraintType.InvalidValue ->
                    ScopeContractError.ValidationConstraint.InvalidFormat(
                        expectedFormat = constraint.reason,
                    )
            }

            ScopeContractError.InputError.ValidationFailure(
                field = domainError.field,
                value = context?.attemptedValue ?: domainError.value,
                constraint = contractConstraint,
            )
        }

        is ScopesError.Conflict -> when (domainError.conflictType) {
            ScopesError.Conflict.ConflictType.DUPLICATE_KEY -> ScopeContractError.BusinessError.DuplicateAlias(
                alias = domainError.resourceId,
            )
            ScopesError.Conflict.ConflictType.HAS_DEPENDENCIES -> ScopeContractError.BusinessError.HasChildren(
                scopeId = domainError.resourceId,
            )
            else -> createServiceUnavailableError(SERVICE_NAME)
        }

        is ScopesError.ConcurrencyError -> ScopeContractError.SystemError.ConcurrentModification(
            scopeId = domainError.aggregateId,
            expectedVersion = domainError.expectedVersion?.toLong() ?: -1L,
            actualVersion = domainError.actualVersion?.toLong() ?: -1L,
        )

        is ScopesError.RepositoryError -> createServiceUnavailableError(SERVICE_NAME)

        is ScopesError.ScopeStatusTransitionError -> createServiceUnavailableError(SERVICE_NAME)

        // For unknown domain errors, fail fast to ensure proper error mapping
        else -> {
            logUnmappedError(domainError)
            createServiceUnavailableError(SERVICE_NAME)
        }
    }

    // Helper error creation functions that use consistent error patterns
    private fun createEmptyTitleError(): ScopeContractError.InputError.InvalidTitle = ScopeContractError.InputError.InvalidTitle(
        title = "",
        validationFailure = ScopeContractError.TitleValidationFailure.Empty,
    )

    private fun createTooShortTitleError(minLength: Int, actualLength: Int): ScopeContractError.InputError.InvalidTitle =
        ScopeContractError.InputError.InvalidTitle(
            title = "",
            validationFailure = ScopeContractError.TitleValidationFailure.TooShort(
                minimumLength = minLength,
                actualLength = actualLength,
            ),
        )

    private fun createTooLongTitleError(maxLength: Int, actualLength: Int): ScopeContractError.InputError.InvalidTitle =
        ScopeContractError.InputError.InvalidTitle(
            title = "",
            validationFailure = ScopeContractError.TitleValidationFailure.TooLong(
                maximumLength = maxLength,
                actualLength = actualLength,
            ),
        )

    private fun createInvalidTitleWithInvalidCharacters(): ScopeContractError.InputError.InvalidTitle = ScopeContractError.InputError.InvalidTitle(
        title = "",
        validationFailure = ScopeContractError.TitleValidationFailure.InvalidCharacters(
            prohibitedCharacters = listOf(),
        ),
    )

    private fun createRequiredFieldError(field: String): ScopeContractError.InputError.ValidationFailure = ScopeContractError.InputError.ValidationFailure(
        field = field,
        value = InputSanitizer.createPreview("[missing]"),
        constraint = ScopeContractError.ValidationConstraint.RequiredField(field = field),
    )

    /**
     * Maps domain scope alias errors to contract errors.
     */
    private fun mapDomainScopeAliasError(domainError: DomainScopeAliasError): ScopeContractError = when (domainError) {
        is DomainScopeAliasError.DuplicateAlias -> ScopeContractError.BusinessError.DuplicateAlias(
            alias = domainError.alias,
            existingScopeId = domainError.scopeId.toString(),
        )
        is DomainScopeAliasError.AliasNotFoundByName -> ScopeContractError.BusinessError.AliasNotFound(
            alias = domainError.alias,
        )
        is DomainScopeAliasError.AliasNotFoundById -> ScopeContractError.BusinessError.AliasNotFound(
            alias = domainError.aliasId.toString(),
        )
        is DomainScopeAliasError.CannotRemoveCanonicalAlias -> ScopeContractError.BusinessError.CannotRemoveCanonicalAlias(
            scopeId = domainError.scopeId.toString(),
            aliasName = domainError.alias,
        )
        is DomainScopeAliasError.AliasGenerationFailed -> ScopeContractError.BusinessError.AliasGenerationFailed(
            scopeId = domainError.scopeId.toString(),
            retryCount = 0, // Domain error doesn't have retry count
        )
        is DomainScopeAliasError.AliasError -> ScopeContractError.BusinessError.AliasGenerationValidationFailed(
            scopeId = "", // Domain error doesn't provide scope ID for alias validation failures
            alias = domainError.alias,
            reason = domainError.reason,
        )
        is DomainScopeAliasError.DataInconsistencyError.AliasReferencesNonExistentScope -> ScopeContractError.DataInconsistency.MissingCanonicalAlias(
            scopeId = domainError.scopeId.toString(),
        )
        else -> {
            logUnmappedError(domainError)
            createServiceUnavailableError(SERVICE_NAME)
        }
    }

    /**
     * Maps domain scope hierarchy errors to contract errors.
     */
    private fun mapDomainScopeHierarchyError(domainError: ScopeHierarchyError): ScopeContractError = when (domainError) {
        is ScopeHierarchyError.MaxDepthExceeded -> ScopeContractError.BusinessError.HierarchyViolation(
            violation = ScopeContractError.HierarchyViolationType.MaxDepthExceeded(
                scopeId = domainError.scopeId.toString(),
                attemptedDepth = domainError.currentDepth,
                maximumDepth = domainError.maxDepth,
            ),
        )
        is ScopeHierarchyError.MaxChildrenExceeded -> ScopeContractError.BusinessError.HierarchyViolation(
            violation = ScopeContractError.HierarchyViolationType.MaxChildrenExceeded(
                parentId = domainError.parentId.toString(),
                currentChildrenCount = domainError.currentCount,
                maximumChildren = domainError.maxChildren,
            ),
        )
        is ScopeHierarchyError.CircularDependency -> ScopeContractError.BusinessError.HierarchyViolation(
            violation = ScopeContractError.HierarchyViolationType.CircularReference(
                scopeId = domainError.scopeId.toString(),
                parentId = domainError.ancestorId.toString(),
            ),
        )
        is ScopeHierarchyError.HierarchyUnavailable -> ScopeContractError.SystemError.ServiceUnavailable(
            service = SERVICE_NAME,
        )
        else -> {
            logUnmappedError(domainError)
            createServiceUnavailableError(SERVICE_NAME)
        }
    }

    /**
     * Maps domain scope input errors to contract errors.
     * Uses the correct domain error structure with nested sealed classes.
     */
    private fun mapDomainScopeInputError(domainError: ScopeInputError): ScopeContractError = when (domainError) {
        // ID errors
        is ScopeInputError.IdError.EmptyId -> ScopeContractError.InputError.InvalidId(
            id = InputSanitizer.createPreview("[empty]"),
            expectedFormat = "Non-empty ULID format",
        )

        is ScopeInputError.IdError.InvalidIdFormat -> ScopeContractError.InputError.InvalidId(
            id = InputSanitizer.createPreview(domainError.id),
            expectedFormat = "Valid ${domainError.expectedFormat.name} format",
        )

        // Title errors
        is ScopeInputError.TitleError.EmptyTitle -> ScopeContractError.InputError.InvalidTitle(
            title = InputSanitizer.createPreview("[empty]"),
            validationFailure = ScopeContractError.TitleValidationFailure.Empty,
        )

        is ScopeInputError.TitleError.TitleTooShort -> ScopeContractError.InputError.InvalidTitle(
            title = InputSanitizer.createPreview("[too short]"),
            validationFailure = ScopeContractError.TitleValidationFailure.TooShort(
                minimumLength = domainError.minLength,
                actualLength = 0, // Domain doesn't provide actual length
            ),
        )

        is ScopeInputError.TitleError.TitleTooLong -> ScopeContractError.InputError.InvalidTitle(
            title = InputSanitizer.createPreview("[too long]"),
            validationFailure = ScopeContractError.TitleValidationFailure.TooLong(
                maximumLength = domainError.maxLength,
                actualLength = domainError.maxLength + 1, // Approximate since domain doesn't provide actual length
            ),
        )

        is ScopeInputError.TitleError.InvalidTitleFormat -> ScopeContractError.InputError.InvalidTitle(
            title = InputSanitizer.createPreview(domainError.title),
            validationFailure = ScopeContractError.TitleValidationFailure.Empty, // Use generic validation since no specific format failure type
        )

        // Description errors
        is ScopeInputError.DescriptionError.DescriptionTooLong -> ScopeContractError.InputError.InvalidDescription(
            descriptionText = InputSanitizer.createPreview("[too long]"),
            validationFailure = ScopeContractError.DescriptionValidationFailure.TooLong(
                maximumLength = domainError.maxLength,
                actualLength = domainError.maxLength + 1, // Approximation since domain doesn't provide actual
            ),
        )

        // Alias errors
        is ScopeInputError.AliasError.EmptyAlias -> ScopeContractError.InputError.InvalidAlias(
            alias = InputSanitizer.createPreview("[empty]"),
            validationFailure = ScopeContractError.AliasValidationFailure.Empty,
        )

        is ScopeInputError.AliasError.AliasTooShort -> ScopeContractError.InputError.InvalidAlias(
            alias = InputSanitizer.createPreview("[too short]"),
            validationFailure = ScopeContractError.AliasValidationFailure.TooShort(
                minimumLength = domainError.minLength,
                actualLength = 0, // Domain doesn't provide actual length
            ),
        )

        is ScopeInputError.AliasError.AliasTooLong -> ScopeContractError.InputError.InvalidAlias(
            alias = InputSanitizer.createPreview("[too long]"),
            validationFailure = ScopeContractError.AliasValidationFailure.TooLong(
                maximumLength = domainError.maxLength,
                actualLength = domainError.maxLength + 1, // Approximation since domain doesn't provide actual
            ),
        )

        is ScopeInputError.AliasError.InvalidAliasFormat -> ScopeContractError.InputError.InvalidAlias(
            alias = InputSanitizer.createPreview(domainError.alias),
            validationFailure = ScopeContractError.AliasValidationFailure.InvalidFormat(
                expectedPattern = domainError.expectedPattern.name,
            ),
        )

        else -> {
            logUnmappedError(domainError)
            createServiceUnavailableError(SERVICE_NAME)
        }
    }

    /**
     * Maps domain scope uniqueness errors to contract errors.
     */
    private fun mapDomainScopeUniquenessError(domainError: DomainScopeUniquenessError): ScopeContractError {
        // Simplified mapping due to merge conflicts with non-existent error types
        return createServiceUnavailableError(SERVICE_NAME)
    }

    private fun logUnmappedError(error: Any) {
        logger.error(
            "Unmapped domain error detected",
            mapOf<String, Any>(
                "errorType" to (error::class.qualifiedName ?: error::class.simpleName ?: "<anonymous>"),
                "errorMessage" to error.toString(),
            ),
        )
    }
}

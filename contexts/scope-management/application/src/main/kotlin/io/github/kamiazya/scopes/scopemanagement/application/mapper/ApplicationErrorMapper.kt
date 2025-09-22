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
        val key = when (error) {
            is ContextError.ContextNotFound -> error.key
            is ContextError.InvalidContextSwitch -> error.key
            else -> error("Unexpected error type: $error")
        }
        return mapNotFoundError(key)
    }

    override fun mapToContractError(domainError: ScopeManagementApplicationError): ScopeContractError = when (domainError) {
        // Group errors by type
        is AppScopeInputError -> mapInputError(domainError)
        is ContextError -> mapContextError(domainError)
        is AppScopeAliasError -> mapScopeAliasError(domainError)
        is ScopeHierarchyApplicationError -> mapHierarchyError(domainError)
        is ScopeUniquenessError -> mapUniquenessError(domainError)
        is CrossAggregateValidationError -> mapSystemError()
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
        is AppScopeInputError.IdBlank -> ScopeContractError.InputError.InvalidScopeId(
            scopeId = "",
            expectedFormat = "Non-empty ULID format",
        )
        is AppScopeInputError.IdInvalidFormat -> ScopeContractError.InputError.InvalidScopeId(
            scopeId = error.preview,
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
                actualLength = error.actualLength,
            ),
        )
        is AppScopeInputError.TitleTooLong -> ScopeContractError.InputError.InvalidTitle(
            title = error.preview,
            validationFailure = ScopeContractError.TitleValidationFailure.TooLong(
                maximumLength = error.maximumLength,
                actualLength = error.actualLength,
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

    private fun mapDescriptionInputError(error: AppScopeInputError.DescriptionTooLong): ScopeContractError =
        ScopeContractError.InputError.InvalidDescription(
            descriptionText = error.preview,
            validationFailure = ScopeContractError.DescriptionValidationFailure.TooLong(
                maximumLength = error.maximumLength,
                actualLength = error.actualLength,
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
                actualLength = error.actualLength,
            ),
        )
        is AppScopeInputError.AliasTooLong -> ScopeContractError.InputError.InvalidAlias(
            alias = error.preview,
            validationFailure = ScopeContractError.AliasValidationFailure.TooLong(
                maximumLength = error.maximumLength,
                actualLength = error.actualLength,
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
        is AppScopeInputError.CannotRemoveCanonicalAlias -> ScopeContractError.BusinessError.CannotModifyCanonicalAlias(
            scopeId = error.scopeId,
            canonicalAlias = error.canonicalAlias,
            operation = "remove",
        )
        is AppScopeInputError.AliasOfDifferentScope -> ScopeContractError.BusinessError.InvalidAliasTarget(
            alias = error.alias,
            targetScopeId = error.targetScopeId,
            actualScopeId = error.actualScopeId,
        )
        else -> error("Unexpected alias business error type: $error")
    }

    private fun mapContextError(error: ContextError): ScopeContractError = when (error) {
        is ContextError.KeyInvalidFormat -> ScopeContractError.InputError.InvalidContextKey(
            key = InputSanitizer.createPreview(error.attemptedKey),
            validationFailure = ScopeContractError.ContextKeyValidationFailure.InvalidFormat(
                expectedPattern = "Valid context key format",
            ),
        )
        is ContextError.StateNotFound -> ScopeContractError.BusinessError.NotFound(
            scopeId = error.contextId,
        )
        is ContextError.InvalidFilter -> ScopeContractError.InputError.InvalidContextFilter(
            filter = InputSanitizer.createPreview(error.filter),
            validationFailure = ScopeContractError.ContextFilterValidationFailure.InvalidSyntax(
                expression = error.filter,
                errorType = "syntax_error",
                position = null,
            ),
        )
        is ContextError.ContextInUse -> ScopeContractError.BusinessError.OperationNotAllowed(
            resource = "context",
            resourceId = error.key,
            operation = "delete",
            reason = "Context is currently active",
        )
        is ContextError.DuplicateContextKey -> ScopeContractError.BusinessError.DuplicateContextKey(
            key = error.key,
        )
        is ContextError.ContextNotFound,
        is ContextError.InvalidContextSwitch,
        -> mapContextToNotFound(error)

        is ContextError.ContextUpdateConflict -> ScopeContractError.BusinessError.UpdateConflict(
            resourceType = "context",
            resourceId = error.key,
            reason = error.reason,
        )
    }

    private fun mapScopeAliasError(error: AppScopeAliasError): ScopeContractError = when (error) {
        is AppScopeAliasError.DuplicateAlias -> ScopeContractError.BusinessError.DuplicateAlias(
            alias = error.alias,
        )
        is AppScopeAliasError.AliasNotFound -> ScopeContractError.BusinessError.AliasNotFound(
            alias = error.alias,
        )
        is AppScopeAliasError.CannotRemoveCanonicalAlias -> ScopeContractError.BusinessError.CannotModifyCanonicalAlias(
            scopeId = error.scopeId,
            canonicalAlias = error.canonicalAlias,
            operation = "remove",
        )
        is AppScopeAliasError.AliasGenerationFailed -> ScopeContractError.SystemError.ServiceUnavailable(
            service = "alias-generation",
        )
    }

    private fun mapHierarchyError(error: ScopeHierarchyApplicationError): ScopeContractError = when (error) {
        is ScopeHierarchyApplicationError.MaxDepthExceeded -> ScopeContractError.BusinessError.HierarchyDepthExceeded(
            currentDepth = error.currentDepth,
            maxDepth = error.maxDepth,
            scopeId = error.scopeId,
            ancestorChain = error.ancestorChain.map { it.toString() },
        )
        is ScopeHierarchyApplicationError.MaxChildrenExceeded -> ScopeContractError.BusinessError.HierarchyWidthExceeded(
            currentCount = error.currentCount,
            maxChildren = error.maxChildren,
            parentId = error.parentId,
        )
        is ScopeHierarchyApplicationError.CircularReference -> ScopeContractError.BusinessError.CircularHierarchy(
            scopeId = error.scopeId,
            parentId = error.parentId,
            conflictPath = error.conflictPath.map { it.toString() },
        )
        is ScopeHierarchyApplicationError.ParentNotFound -> ScopeContractError.BusinessError.NotFound(
            scopeId = error.parentId,
        )
    }

    private fun mapUniquenessError(error: ScopeUniquenessError): ScopeContractError = when (error) {
        is ScopeUniquenessError.DuplicateTitle -> ScopeContractError.BusinessError.DuplicateTitle(
            title = error.title,
            parentId = error.parentScopeId,
            existingScopeId = error.existingScopeId,
        )
    }

    private fun mapPersistenceError(error: ScopeManagementApplicationError.PersistenceError): ScopeContractError = when (error) {
        // All persistence errors map to ServiceUnavailable for security
        is ScopeManagementApplicationError.PersistenceError.StorageUnavailable,
        is ScopeManagementApplicationError.PersistenceError.DataCorruption,
        is ScopeManagementApplicationError.PersistenceError.ConcurrencyConflict -> ScopeContractError.SystemError.ServiceUnavailable(
            service = SERVICE_NAME,
        )
    }

    private fun mapSystemError(): ScopeContractError = ScopeContractError.SystemError.ServiceUnavailable(
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
            key = "",
            validationFailure = ScopeContractError.ContextKeyValidationFailure.Empty,
        )
        is DomainContextError.KeyTooShort -> ScopeContractError.InputError.InvalidContextKey(
            key = "",
            validationFailure = ScopeContractError.ContextKeyValidationFailure.TooShort(
                minimumLength = domainError.minimumLength,
                actualLength = 0,
            ),
        )
        is DomainContextError.KeyTooLong -> ScopeContractError.InputError.InvalidContextKey(
            key = "",
            validationFailure = ScopeContractError.ContextKeyValidationFailure.TooLong(
                maximumLength = domainError.maximumLength,
                actualLength = 0,
            ),
        )
        is DomainContextError.InvalidKeyFormat -> ScopeContractError.InputError.InvalidContextKey(
            key = "",
            validationFailure = ScopeContractError.ContextKeyValidationFailure.InvalidFormat(
                expectedPattern = "Valid context key format",
            ),
        )

        // Context name validation errors
        is DomainContextError.EmptyName -> ScopeContractError.InputError.InvalidContextName(
            name = "",
            validationFailure = ScopeContractError.ContextNameValidationFailure.Empty,
        )
        is DomainContextError.NameTooLong -> ScopeContractError.InputError.InvalidContextName(
            name = "",
            validationFailure = ScopeContractError.ContextNameValidationFailure.TooLong(
                maximumLength = domainError.maximumLength,
                actualLength = 0,
            ),
        )

        // Context description validation errors
        is DomainContextError.EmptyDescription -> ScopeContractError.InputError.InvalidContextDescription(
            description = "",
            validationFailure = ScopeContractError.ContextDescriptionValidationFailure.Empty,
        )
        is DomainContextError.DescriptionTooShort -> ScopeContractError.InputError.InvalidContextDescription(
            description = "",
            validationFailure = ScopeContractError.ContextDescriptionValidationFailure.TooShort(
                minimumLength = domainError.minimumLength,
                actualLength = 0,
            ),
        )
        is DomainContextError.DescriptionTooLong -> ScopeContractError.InputError.InvalidContextDescription(
            description = "",
            validationFailure = ScopeContractError.ContextDescriptionValidationFailure.TooLong(
                maximumLength = domainError.maximumLength,
                actualLength = 0,
            ),
        )

        // Context filter validation errors
        is DomainContextError.EmptyFilter -> ScopeContractError.InputError.InvalidContextFilter(
            filter = "",
            validationFailure = ScopeContractError.ContextFilterValidationFailure.Empty,
        )
        is DomainContextError.FilterTooShort -> ScopeContractError.InputError.InvalidContextFilter(
            filter = "",
            validationFailure = ScopeContractError.ContextFilterValidationFailure.TooShort(
                minimumLength = domainError.minimumLength,
                actualLength = 0,
            ),
        )
        is DomainContextError.FilterTooLong -> ScopeContractError.InputError.InvalidContextFilter(
            filter = "",
            validationFailure = ScopeContractError.ContextFilterValidationFailure.TooLong(
                maximumLength = domainError.maximumLength,
                actualLength = 0,
            ),
        )
        is DomainContextError.InvalidFilterSyntax -> ScopeContractError.InputError.InvalidContextFilter(
            filter = "",
            validationFailure = ScopeContractError.ContextFilterValidationFailure.InvalidSyntax(
                expression = "",
                errorType = "syntax_error",
                position = null,
            ),
        )
    }

    /**
     * Maps domain aspect key errors to contract errors.
     */
    fun mapDomainError(domainError: AspectKeyError): ScopeContractError = when (domainError) {
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
    fun mapDomainError(domainError: AspectValidationError): ScopeContractError = when (domainError) {
        // AspectKey validation errors
        is AspectValidationError.EmptyAspectKey -> createEmptyTitleError()
        is AspectValidationError.AspectKeyTooShort -> createTooShortTitleError(
            minLength = 1,
            actualLength = 0,
        )
        is AspectValidationError.AspectKeyTooLong -> createTooLongTitleError(
            maxLength = domainError.maxLength,
            actualLength = domainError.actualLength,
        )
        is AspectValidationError.InvalidAspectKeyFormat -> createInvalidTitleWithInvalidCharacters()

        // AspectValue validation errors
        is AspectValidationError.EmptyAspectValue -> createRequiredFieldError(
            field = "aspectValue",
        )
        is AspectValidationError.AspectValueTooShort -> ScopeContractError.InputError.ValidationFailure(
            field = "aspectValue",
            value = "",
            constraint = ScopeContractError.ValidationConstraint.InvalidFormat(
                expectedFormat = "At least 1 character",
            ),
        )
        is AspectValidationError.AspectValueTooLong -> ScopeContractError.InputError.ValidationFailure(
            field = "aspectValue",
            value = "",
            constraint = ScopeContractError.ValidationConstraint.InvalidFormat(
                expectedFormat = "At most ${domainError.maxLength} characters",
            ),
        )

        // AspectValue pattern validation errors
        is AspectValidationError.InvalidAspectValueFormat -> ScopeContractError.InputError.ValidationFailure(
            field = "aspectValue",
            value = "",
            constraint = ScopeContractError.ValidationConstraint.InvalidFormat(
                expectedFormat = "Valid aspect value format",
            ),
        )

        // AllowedValues validation errors
        is AspectValidationError.EmptyAspectAllowedValues -> createRequiredFieldError(
            field = "allowedValues",
        )
        is AspectValidationError.DuplicateAllowedValues -> ScopeContractError.InputError.ValidationFailure(
            field = "allowedValues",
            value = domainError.duplicateValue,
            constraint = ScopeContractError.ValidationConstraint.InvalidFormat(
                expectedFormat = "No duplicate values allowed",
            ),
        )
        is AspectValidationError.TooManyAllowedValues -> ScopeContractError.InputError.ValidationFailure(
            field = "allowedValues",
            value = "${domainError.actualCount} values",
            constraint = ScopeContractError.ValidationConstraint.InvalidFormat(
                expectedFormat = "At most ${domainError.maxCount} values allowed",
            ),
        )
        is AspectValidationError.InvalidAllowedValueFormat -> ScopeContractError.InputError.ValidationFailure(
            field = "allowedValues",
            value = domainError.invalidValue,
            constraint = ScopeContractError.ValidationConstraint.InvalidFormat(
                expectedFormat = "Valid allowed value format",
            ),
        )
    }

    /**
     * Generic mapper for any ScopesError to contract errors.
     * This function delegates to specific mappers based on the error type.
     */
    fun mapDomainError(domainError: ScopesError, context: ErrorMappingContext? = null): ScopeContractError = when (domainError) {
        // Delegate to specific mappers for known subtypes
        is DomainContextError -> mapDomainError(domainError)
        is AspectKeyError -> mapDomainError(domainError)
        is AspectValidationError -> mapDomainError(domainError)

        // Common domain errors
        is ScopesError.NotFound -> ScopeContractError.BusinessError.NotFound(
            scopeId = domainError.identifier,
        )
        is ScopesError.InvalidOperation -> createServiceUnavailableError()
        is ScopesError.AlreadyExists -> ScopeContractError.BusinessError.DuplicateAlias(
            alias = domainError.identifier,
        )
        is ScopesError.SystemError -> createServiceUnavailableError(
            service = domainError.service,
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

                is ScopesError.ValidationConstraintType.OutOfRange ->
                    ScopeContractError.ValidationConstraint.OutOfRange(
                        minimum = constraint.minimum?.toString(),
                        maximum = constraint.maximum?.toString(),
                        actual = constraint.actual?.toString(),
                    )

                is ScopesError.ValidationConstraintType.Required ->
                    ScopeContractError.ValidationConstraint.Required

                is ScopesError.ValidationConstraintType.Custom ->
                    ScopeContractError.ValidationConstraint.InvalidFormat(
                        expectedFormat = constraint.description,
                    )
            }

            ScopeContractError.InputError.ValidationFailure(
                field = domainError.field,
                value = context?.attemptedValue ?: "[unknown]",
                constraint = contractConstraint,
            )
        }

        // For unknown domain errors, fail fast to ensure proper error mapping
        else -> {
            logUnmappedError(domainError)
            createServiceUnavailableError()
        }
    }

    // Helper error creation functions that use consistent error patterns
    private fun createEmptyTitleError(): ScopeContractError.InputError.InvalidTitle = 
        ScopeContractError.InputError.InvalidTitle(
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

    private fun createInvalidTitleWithInvalidCharacters(): ScopeContractError.InputError.InvalidTitle = 
        ScopeContractError.InputError.InvalidTitle(
            title = "",
            validationFailure = ScopeContractError.TitleValidationFailure.InvalidCharacters(
                prohibitedCharacters = listOf(),
            ),
        )

    private fun createRequiredFieldError(field: String): ScopeContractError.InputError.ValidationFailure = 
        ScopeContractError.InputError.ValidationFailure(
            field = field,
            value = "",
            constraint = ScopeContractError.ValidationConstraint.Required,
        )

    private fun logUnmappedError(error: Any) {
        logger.error(
            "Unmapped domain error detected",
            mapOf(
                "errorType" to error::class.qualifiedName,
                "errorMessage" to error.toString(),
            ),
        )
    }
}
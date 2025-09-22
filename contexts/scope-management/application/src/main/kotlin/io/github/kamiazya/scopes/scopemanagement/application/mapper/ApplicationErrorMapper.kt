package io.github.kamiazya.scopes.scopemanagement.application.mapper

import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.platform.application.error.BaseErrorMapper
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.error.ContextError
import io.github.kamiazya.scopes.scopemanagement.application.error.CrossAggregateValidationError
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeHierarchyApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeManagementApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeUniquenessError
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

    // ==============================================
    // Generic Validation Error Creators
    // ==============================================

    /**
     * Generic interface for validation error creation
     */
    private sealed interface ValidationErrorCreator<T> {
        fun createEmpty(): T
        fun createTooShort(minLength: Int, actualLength: Int): T
        fun createTooLong(maxLength: Int, actualLength: Int): T
        fun createInvalidFormat(): T
    }

    /**
     * Title validation error creator
     */
    private object TitleValidationErrorCreator : ValidationErrorCreator<ScopeContractError.InputError.InvalidTitle> {
        override fun createEmpty() = ScopeContractError.InputError.InvalidTitle(
            title = "",
            validationFailure = ScopeContractError.TitleValidationFailure.Empty,
        )

        override fun createTooShort(minLength: Int, actualLength: Int) = ScopeContractError.InputError.InvalidTitle(
            title = "",
            validationFailure = ScopeContractError.TitleValidationFailure.TooShort(
                minimumLength = minLength,
                actualLength = actualLength,
            ),
        )

        override fun createTooLong(maxLength: Int, actualLength: Int) = ScopeContractError.InputError.InvalidTitle(
            title = "",
            validationFailure = ScopeContractError.TitleValidationFailure.TooLong(
                maximumLength = maxLength,
                actualLength = actualLength,
            ),
        )

        override fun createInvalidFormat() = ScopeContractError.InputError.InvalidTitle(
            title = "",
            validationFailure = ScopeContractError.TitleValidationFailure.InvalidCharacters(
                prohibitedCharacters = listOf(),
            ),
        )
    }

    /**
     * Description validation error creator
     */
    private object DescriptionValidationErrorCreator : ValidationErrorCreator<ScopeContractError.InputError.InvalidDescription> {
        override fun createEmpty() = ScopeContractError.InputError.InvalidDescription(
            descriptionText = "",
            validationFailure = ScopeContractError.DescriptionValidationFailure.TooLong(
                maximumLength = 0,
                actualLength = 0,
            ),
        )

        override fun createTooShort(minLength: Int, actualLength: Int) = ScopeContractError.InputError.InvalidDescription(
            descriptionText = "",
            validationFailure = ScopeContractError.DescriptionValidationFailure.TooLong(
                maximumLength = minLength,
                actualLength = actualLength,
            ),
        )

        override fun createTooLong(maxLength: Int, actualLength: Int) = ScopeContractError.InputError.InvalidDescription(
            descriptionText = "",
            validationFailure = ScopeContractError.DescriptionValidationFailure.TooLong(
                maximumLength = maxLength,
                actualLength = actualLength,
            ),
        )

        override fun createInvalidFormat() = createEmpty() // Description doesn't have invalid format
    }

    /**
     * Generic validation error creator for aspect-related errors.
     * Since contract doesn't have specific aspect error types, all map to ServiceUnavailable.
     */
    private class AspectValidationErrorCreator(private val service: String = "aspect-validation") : ValidationErrorCreator<ScopeContractError> {
        override fun createEmpty() = ScopeContractError.SystemError.ServiceUnavailable(service = service)
        override fun createTooShort(minLength: Int, actualLength: Int) = ScopeContractError.SystemError.ServiceUnavailable(service = service)
        override fun createTooLong(maxLength: Int, actualLength: Int) = ScopeContractError.SystemError.ServiceUnavailable(service = service)
        override fun createInvalidFormat() = ScopeContractError.SystemError.ServiceUnavailable(service = service)
    }

    /**
     * Aspect Key validation error creator
     */
    private val aspectKeyValidationErrorCreator = AspectValidationErrorCreator()

    /**
     * Aspect Value validation error creator
     */
    private val aspectValueValidationErrorCreator = AspectValidationErrorCreator()

    /**
     * Context Key validation error creator
     */
    private object ContextKeyValidationErrorCreator : ValidationErrorCreator<ScopeContractError.InputError.InvalidContextKey> {
        override fun createEmpty() = ScopeContractError.InputError.InvalidContextKey(
            key = "",
            validationFailure = ScopeContractError.ContextKeyValidationFailure.Empty,
        )

        override fun createTooShort(minLength: Int, actualLength: Int) = ScopeContractError.InputError.InvalidContextKey(
            key = "",
            validationFailure = ScopeContractError.ContextKeyValidationFailure.TooShort(
                minimumLength = minLength,
                actualLength = actualLength,
            ),
        )

        override fun createTooLong(maxLength: Int, actualLength: Int) = ScopeContractError.InputError.InvalidContextKey(
            key = "",
            validationFailure = ScopeContractError.ContextKeyValidationFailure.TooLong(
                maximumLength = maxLength,
                actualLength = actualLength,
            ),
        )

        override fun createInvalidFormat() = ScopeContractError.InputError.InvalidContextKey(
            key = "",
            validationFailure = ScopeContractError.ContextKeyValidationFailure.InvalidFormat("unknown"),
        )
    }

    /**
     * Context Name validation error creator
     */
    private object ContextNameValidationErrorCreator : ValidationErrorCreator<ScopeContractError.InputError.InvalidContextName> {
        override fun createEmpty() = ScopeContractError.InputError.InvalidContextName(
            name = "",
            validationFailure = ScopeContractError.ContextNameValidationFailure.Empty,
        )

        override fun createTooShort(minLength: Int, actualLength: Int) = createEmpty() // Context name doesn't have too short

        override fun createTooLong(maxLength: Int, actualLength: Int) = ScopeContractError.InputError.InvalidContextName(
            name = "",
            validationFailure = ScopeContractError.ContextNameValidationFailure.TooLong(
                maximumLength = maxLength,
                actualLength = actualLength,
            ),
        )

        override fun createInvalidFormat() = createEmpty() // Context name doesn't have invalid format
    }

    /**
     * Context Filter validation error creator
     */
    private object ContextFilterValidationErrorCreator : ValidationErrorCreator<ScopeContractError.InputError.InvalidContextFilter> {
        override fun createEmpty() = ScopeContractError.InputError.InvalidContextFilter(
            filter = "",
            validationFailure = ScopeContractError.ContextFilterValidationFailure.Empty,
        )

        override fun createTooShort(minLength: Int, actualLength: Int) = ScopeContractError.InputError.InvalidContextFilter(
            filter = "",
            validationFailure = ScopeContractError.ContextFilterValidationFailure.TooShort(
                minimumLength = minLength,
                actualLength = actualLength,
            ),
        )

        override fun createTooLong(maxLength: Int, actualLength: Int) = ScopeContractError.InputError.InvalidContextFilter(
            filter = "",
            validationFailure = ScopeContractError.ContextFilterValidationFailure.TooLong(
                maximumLength = maxLength,
                actualLength = actualLength,
            ),
        )

        override fun createInvalidFormat() = ScopeContractError.InputError.InvalidContextFilter(
            filter = "",
            validationFailure = ScopeContractError.ContextFilterValidationFailure.InvalidSyntax(
                expression = "",
                errorType = "unknown",
                position = null,
            ),
        )
    }

    // ==============================================
    // Generic Validation Mappers
    // ==============================================

    /**
     * Maps common validation error patterns using the appropriate creator
     */
    private fun <T> mapValidationError(error: Any, creator: ValidationErrorCreator<T>): T = when (error) {
        // Empty patterns
        is AspectKeyError.EmptyKey,
        is AspectValidationError.EmptyAspectKey,
        is AspectValidationError.EmptyAspectValue,
        is AspectValidationError.EmptyAspectAllowedValues,
        is DomainContextError.EmptyKey,
        is DomainContextError.EmptyName,
        is DomainContextError.EmptyDescription,
        is DomainContextError.EmptyFilter,
        -> creator.createEmpty()

        // Too short patterns
        is AspectKeyError.TooShort -> creator.createTooShort(error.minLength, error.actualLength)
        is AspectValidationError.AspectKeyTooShort -> creator.createTooShort(1, 0)
        is AspectValidationError.AspectValueTooShort -> creator.createTooShort(1, 0)
        is DomainContextError.KeyTooShort -> creator.createTooShort(error.minimumLength, 0)
        is DomainContextError.FilterTooShort -> creator.createTooShort(error.minimumLength, 0)
        is DomainContextError.DescriptionTooShort -> creator.createTooShort(error.minimumLength, 0)

        // Too long patterns
        is AspectKeyError.TooLong -> creator.createTooLong(error.maxLength, error.actualLength)
        is AspectValidationError.AspectKeyTooLong -> creator.createTooLong(error.maxLength, error.actualLength)
        is AspectValidationError.AspectValueTooLong -> creator.createTooLong(error.maxLength, error.actualLength)
        is DomainContextError.KeyTooLong -> creator.createTooLong(error.maximumLength, 0)
        is DomainContextError.NameTooLong -> creator.createTooLong(error.maximumLength, 0)
        is DomainContextError.DescriptionTooLong -> creator.createTooLong(error.maximumLength, 0)
        is DomainContextError.FilterTooLong -> creator.createTooLong(error.maximumLength, 0)

        // Invalid format patterns
        is AspectKeyError.InvalidFormat,
        is AspectValidationError.InvalidAspectKeyFormat,
        is DomainContextError.InvalidKeyFormat,
        is DomainContextError.InvalidFilterSyntax,
        -> creator.createInvalidFormat()

        else -> error("Unexpected error type: $error")
    }

    // ==============================================
    // Main Error Mapping
    // ==============================================

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
        -> mapAliasInputError(error)

        // Business rule errors - map to NotFound
        is AppScopeInputError.AliasNotFound,
        is AppScopeInputError.InvalidAlias,
        -> mapAliasToNotFound(error)

        is AppScopeInputError.CannotRemoveCanonicalAlias -> ScopeContractError.BusinessError.CannotRemoveCanonicalAlias(scopeId = "", aliasName = "")

        // Invalid parent ID
        is AppScopeInputError.InvalidParentId -> ScopeContractError.InputError.InvalidParentId(
            parentId = error.parentId,
            expectedFormat = "ULID format",
        )

        // Duplicate alias
        is AppScopeInputError.AliasDuplicate -> ScopeContractError.BusinessError.DuplicateAlias(
            alias = error.alias,
            existingScopeId = null,
            attemptedScopeId = null,
        )

        // Alias belongs to different scope
        is AppScopeInputError.AliasOfDifferentScope -> ScopeContractError.BusinessError.AliasOfDifferentScope(
            alias = error.alias,
            expectedScopeId = error.expectedScopeId,
            actualScopeId = error.actualScopeId,
        )
    }

    private fun mapIdInputError(error: AppScopeInputError): ScopeContractError = when (error) {
        is AppScopeInputError.IdBlank -> ScopeContractError.InputError.InvalidId(
            id = "",
            expectedFormat = "ULID format",
        )
        is AppScopeInputError.IdInvalidFormat -> ScopeContractError.InputError.InvalidId(
            id = error.attemptedValue,
            expectedFormat = error.expectedFormat,
        )
        else -> error("Unexpected error type: $error")
    }

    private fun mapTitleInputError(error: AppScopeInputError): ScopeContractError = when (error) {
        is AppScopeInputError.TitleEmpty -> TitleValidationErrorCreator.createEmpty()
        is AppScopeInputError.TitleTooShort -> TitleValidationErrorCreator.createTooShort(error.minimumLength, error.attemptedValue.length)
        is AppScopeInputError.TitleTooLong -> TitleValidationErrorCreator.createTooLong(error.maximumLength, error.attemptedValue.length)
        is AppScopeInputError.TitleContainsProhibitedCharacters -> TitleValidationErrorCreator.createInvalidFormat()
        else -> error("Unexpected error type: $error")
    }

    private fun mapDescriptionInputError(error: AppScopeInputError): ScopeContractError = when (error) {
        is AppScopeInputError.DescriptionTooLong -> DescriptionValidationErrorCreator.createTooLong(
            error.maximumLength,
            error.attemptedValue.length,
        )
        else -> error("Unexpected error type: $error")
    }

    private fun mapAliasInputError(error: AppScopeInputError): ScopeContractError = when (error) {
        is AppScopeInputError.AliasEmpty -> ScopeContractError.InputError.InvalidAlias(
            alias = error.alias,
            validationFailure = ScopeContractError.AliasValidationFailure.Empty,
        )
        is AppScopeInputError.AliasTooShort -> ScopeContractError.InputError.InvalidAlias(
            alias = error.alias,
            validationFailure = ScopeContractError.AliasValidationFailure.TooShort(
                minimumLength = error.minimumLength,
                actualLength = error.alias.length,
            ),
        )
        is AppScopeInputError.AliasTooLong -> ScopeContractError.InputError.InvalidAlias(
            alias = error.alias,
            validationFailure = ScopeContractError.AliasValidationFailure.TooLong(
                maximumLength = error.maximumLength,
                actualLength = error.alias.length,
            ),
        )
        is AppScopeInputError.AliasInvalidFormat -> ScopeContractError.InputError.InvalidAlias(
            alias = error.alias,
            validationFailure = ScopeContractError.AliasValidationFailure.InvalidFormat(
                expectedPattern = error.expectedPattern,
            ),
        )
        else -> error("Unexpected error type: $error")
    }

    private fun mapAliasNotFoundError(alias: String): ScopeContractError = ScopeContractError.BusinessError.AliasNotFound(alias = alias)

    private fun mapAliasToNotFound(error: AppScopeInputError): ScopeContractError {
        val alias = when (error) {
            is AppScopeInputError.AliasNotFound -> error.alias
            is AppScopeInputError.InvalidAlias -> error.alias
            else -> error("Unexpected error type: $error")
        }
        return mapAliasNotFoundError(alias)
    }

    private fun mapScopeAliasError(error: AppScopeAliasError): ScopeContractError = when (error) {
        is AppScopeAliasError.AliasDuplicate -> ScopeContractError.BusinessError.DuplicateAlias(
            alias = error.aliasName,
            existingScopeId = error.existingScopeId,
            attemptedScopeId = error.attemptedScopeId,
        )
        is AppScopeAliasError.AliasNotFound -> ScopeContractError.BusinessError.AliasNotFound(alias = error.aliasName)
        is AppScopeAliasError.CannotRemoveCanonicalAlias -> ScopeContractError.BusinessError.CannotRemoveCanonicalAlias(
            scopeId = error.scopeId,
            aliasName = error.aliasName,
        )
        is AppScopeAliasError.AliasGenerationFailed -> ScopeContractError.BusinessError.AliasGenerationFailed(
            scopeId = error.scopeId,
            retryCount = error.retryCount,
        )
        is AppScopeAliasError.AliasGenerationValidationFailed -> ScopeContractError.BusinessError.AliasGenerationValidationFailed(
            scopeId = error.scopeId,
            alias = error.alias,
            reason = error.reason,
        )
        is AppScopeAliasError.DataInconsistencyError.AliasExistsButScopeNotFound -> ScopeContractError.SystemError.ServiceUnavailable(
            service = SERVICE_NAME,
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
        is ScopeHierarchyApplicationError.ParentNotFound -> ScopeContractError.BusinessError.HierarchyViolation(
            violation = ScopeContractError.HierarchyViolationType.ParentNotFound(
                scopeId = error.scopeId,
                parentId = error.parentId,
            ),
        )
        is ScopeHierarchyApplicationError.CircularReference -> ScopeContractError.BusinessError.HierarchyViolation(
            violation = ScopeContractError.HierarchyViolationType.CircularReference(
                scopeId = error.scopeId,
                parentId = error.cyclePath.lastOrNull() ?: "",
                cyclePath = error.cyclePath,
            ),
        )
        is ScopeHierarchyApplicationError.SelfParenting -> ScopeContractError.BusinessError.HierarchyViolation(
            violation = ScopeContractError.HierarchyViolationType.SelfParenting(
                scopeId = error.scopeId,
            ),
        )
        is ScopeHierarchyApplicationError.InvalidParentId -> ScopeContractError.InputError.InvalidParentId(
            parentId = error.invalidId,
            expectedFormat = "ULID format",
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
    }

    private fun mapContextError(error: ContextError): ScopeContractError = when (error) {
        // Context view errors
        is ContextError.ContextNotFound -> mapContextToNotFound(error)
        is ContextError.InvalidContextSwitch -> mapContextToNotFound(error)
        is ContextError.DuplicateContextKey -> ScopeContractError.BusinessError.DuplicateContextKey(
            contextKey = error.key,
        )
        is ContextError.KeyInvalidFormat -> ScopeContractError.InputError.InvalidContextKey(
            key = error.attemptedKey,
            validationFailure = ScopeContractError.ContextKeyValidationFailure.InvalidFormat("invalid format"),
        )
        is ContextError.StateNotFound -> ScopeContractError.SystemError.ServiceUnavailable(service = SERVICE_NAME)
        is ContextError.InvalidFilter -> ScopeContractError.InputError.InvalidContextFilter(
            filter = error.filter,
            validationFailure = ScopeContractError.ContextFilterValidationFailure.InvalidSyntax(
                expression = error.filter,
                errorType = error.reason,
                position = null,
            ),
        )
        is ContextError.ContextInUse,
        is ContextError.ContextUpdateConflict,
        -> ScopeContractError.SystemError.ServiceUnavailable(service = SERVICE_NAME)
    }

    private fun mapNotFoundError(key: String): ScopeContractError = ScopeContractError.BusinessError.ContextNotFound(contextKey = key)

    private fun mapContextToNotFound(error: ContextError): ScopeContractError {
        val key = when (error) {
            is ContextError.ContextNotFound -> error.key
            is ContextError.InvalidContextSwitch -> error.key
            else -> error("Unexpected error type: $error")
        }
        return mapNotFoundError(key)
    }

    override fun mapSystemError(): ScopeContractError = createServiceUnavailableError(SERVICE_NAME)

    private fun mapPersistenceError(error: ScopeManagementApplicationError.PersistenceError): ScopeContractError = when (error) {
        is ScopeManagementApplicationError.PersistenceError.StorageUnavailable,
        is ScopeManagementApplicationError.PersistenceError.DataCorruption,
        is ScopeManagementApplicationError.PersistenceError.ConcurrencyConflict,
        -> ScopeContractError.SystemError.ServiceUnavailable(
            service = SERVICE_NAME,
        )
        is ScopeManagementApplicationError.PersistenceError.NotFound -> ScopeContractError.BusinessError.NotFound(
            scopeId = "",
        )
    }

    /**
     * Maps domain input errors to contract errors.
     */
    fun mapDomainError(domainError: ScopeInputError): ScopeContractError = when (domainError) {
        // Title errors
        is ScopeInputError.TitleError.EmptyTitle -> TitleValidationErrorCreator.createEmpty()
        is ScopeInputError.TitleError.TitleTooShort -> TitleValidationErrorCreator.createTooShort(
            domainError.minLength,
            0, // actual length not available in domain error
        )
        is ScopeInputError.TitleError.TitleTooLong -> TitleValidationErrorCreator.createTooLong(
            domainError.maxLength,
            0, // actual length not available in domain error
        )
        is ScopeInputError.TitleError.InvalidTitleFormat -> TitleValidationErrorCreator.createInvalidFormat()

        // Description errors
        is ScopeInputError.DescriptionError.DescriptionTooLong -> DescriptionValidationErrorCreator.createTooLong(
            domainError.maxLength,
            0, // actual length not available in domain error
        )

        // ID errors
        is ScopeInputError.IdError.EmptyId -> ScopeContractError.InputError.InvalidId(
            id = "",
            expectedFormat = "ULID format",
        )
        is ScopeInputError.IdError.InvalidIdFormat -> ScopeContractError.InputError.InvalidId(
            id = domainError.id,
            expectedFormat = when (domainError.expectedFormat) {
                ScopeInputError.IdError.InvalidIdFormat.IdFormatType.ULID -> "ULID format"
                ScopeInputError.IdError.InvalidIdFormat.IdFormatType.UUID -> "UUID format"
                ScopeInputError.IdError.InvalidIdFormat.IdFormatType.NUMERIC_ID -> "Numeric ID"
                ScopeInputError.IdError.InvalidIdFormat.IdFormatType.CUSTOM_FORMAT -> "Custom format"
            },
        )

        // Alias errors
        is ScopeInputError.AliasError.EmptyAlias,
        is ScopeInputError.AliasError.AliasTooShort,
        is ScopeInputError.AliasError.AliasTooLong,
        is ScopeInputError.AliasError.InvalidAliasFormat,
        -> ScopeContractError.SystemError.ServiceUnavailable(service = SERVICE_NAME)
    }

    /**
     * Maps domain scope alias errors to contract errors.
     */
    fun mapDomainError(domainError: DomainScopeAliasError): ScopeContractError = when (domainError) {
        is DomainScopeAliasError.AliasNotFoundById -> mapAliasNotFoundError("")
        is DomainScopeAliasError.AliasNotFoundByName -> mapAliasNotFoundError(domainError.alias)
        is DomainScopeAliasError.DuplicateAlias -> ScopeContractError.BusinessError.DuplicateAlias(
            alias = domainError.alias,
            existingScopeId = domainError.scopeId.toString(),
            attemptedScopeId = "",
        )
        is DomainScopeAliasError.CannotRemoveCanonicalAlias -> ScopeContractError.BusinessError.CannotRemoveCanonicalAlias(
            scopeId = domainError.scopeId.toString(),
            aliasName = domainError.alias,
        )
        is DomainScopeAliasError.AliasGenerationFailed,
        is DomainScopeAliasError.DataInconsistencyError.AliasReferencesNonExistentScope,
        is DomainScopeAliasError.AliasError,
        -> ScopeContractError.SystemError.ServiceUnavailable(
            service = SERVICE_NAME,
        )
    }

    /**
     * Maps domain context errors to contract errors.
     */
    fun mapDomainError(domainError: DomainContextError): ScopeContractError = when (domainError) {
        // Context key validation errors
        is DomainContextError.EmptyKey,
        is DomainContextError.KeyTooShort,
        is DomainContextError.KeyTooLong,
        is DomainContextError.InvalidKeyFormat,
        -> mapValidationError(domainError, ContextKeyValidationErrorCreator)

        // Context name validation errors
        is DomainContextError.EmptyName -> ContextNameValidationErrorCreator.createEmpty()
        is DomainContextError.NameTooLong -> ContextNameValidationErrorCreator.createTooLong(domainError.maximumLength, 0)

        // Context description validation errors
        is DomainContextError.EmptyDescription,
        is DomainContextError.DescriptionTooShort,
        is DomainContextError.DescriptionTooLong,
        -> mapValidationError(domainError, DescriptionValidationErrorCreator)

        // Context filter validation errors
        is DomainContextError.EmptyFilter,
        is DomainContextError.FilterTooShort,
        is DomainContextError.FilterTooLong,
        is DomainContextError.InvalidFilterSyntax,
        -> mapValidationError(domainError, ContextFilterValidationErrorCreator)

        // Business rule validation errors
        is DomainContextError.InvalidScope -> ScopeContractError.BusinessError.NotFound(scopeId = domainError.scopeId)
        is DomainContextError.InvalidHierarchy -> ScopeContractError.BusinessError.HierarchyViolation(
            violation = ScopeContractError.HierarchyViolationType.ParentNotFound(
                scopeId = domainError.scopeId,
                parentId = domainError.parentId,
            ),
        )
        is DomainContextError.DuplicateScope -> ScopeContractError.BusinessError.DuplicateTitle(
            title = domainError.title,
            parentId = domainError.contextId,
        )
    }

    /**
     * Maps domain aspect key errors to contract errors.
     * Since contract doesn't have specific aspect error types, map to generic system error.
     */
    fun mapDomainError(domainError: AspectKeyError): ScopeContractError = ScopeContractError.SystemError.ServiceUnavailable(service = "aspect-validation")

    /**
     * Maps domain aspect validation errors to contract errors.
     * Since contract doesn't have specific aspect error types, map to generic system error.
     */
    fun mapDomainError(domainError: AspectValidationError): ScopeContractError =
        ScopeContractError.SystemError.ServiceUnavailable(service = "aspect-validation")

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
        is ScopesError.NotFound -> ScopeContractError.BusinessError.NotFound(scopeId = domainError.identifier)
        is ScopesError.InvalidOperation -> createServiceUnavailableError(SERVICE_NAME)
        is ScopesError.AlreadyExists -> ScopeContractError.BusinessError.DuplicateAlias(
            alias = domainError.identifier,
            existingScopeId = "",
            attemptedScopeId = "",
        )
        is ScopesError.SystemError -> createServiceUnavailableError(domainError.service ?: SERVICE_NAME)
        is ScopesError.ValidationFailed -> when (val constraint = domainError.constraint) {
            is ScopesError.ValidationConstraintType.InvalidType -> ScopeContractError.InputError.InvalidId(
                id = domainError.value,
                expectedFormat = constraint.expectedType,
            )
            is ScopesError.ValidationConstraintType.InvalidFormat -> ScopeContractError.InputError.InvalidId(
                id = domainError.value,
                expectedFormat = constraint.expectedFormat,
            )
            else -> TitleValidationErrorCreator.createEmpty()
        }
        is ScopesError.Conflict -> when (domainError.conflictType) {
            ScopesError.Conflict.ConflictType.DUPLICATE_KEY -> ScopeContractError.BusinessError.DuplicateTitle(
                title = domainError.resourceId,
                parentId = null,
            )
            ScopesError.Conflict.ConflictType.HAS_DEPENDENCIES -> ScopeContractError.BusinessError.HasChildren(
                scopeId = domainError.resourceId,
                childrenCount = 1,
            )
            else -> ScopeContractError.SystemError.ServiceUnavailable(service = SERVICE_NAME)
        }
        is ScopesError.ConcurrencyError -> ScopeContractError.SystemError.ConcurrentModification(
            scopeId = domainError.aggregateId,
            expectedVersion = domainError.expectedVersion?.toLong() ?: -1L,
            actualVersion = domainError.actualVersion?.toLong() ?: -1L,
        )
        is ScopesError.RepositoryError -> when (domainError.failure) {
            ScopesError.RepositoryError.RepositoryFailure.STORAGE_UNAVAILABLE,
            ScopesError.RepositoryError.RepositoryFailure.TIMEOUT,
            ScopesError.RepositoryError.RepositoryFailure.ACCESS_DENIED,
            ScopesError.RepositoryError.RepositoryFailure.CONSTRAINT_VIOLATION,
            ScopesError.RepositoryError.RepositoryFailure.OPERATION_FAILED,
            ScopesError.RepositoryError.RepositoryFailure.CORRUPTED_DATA,
            null,
            -> createServiceUnavailableError(SERVICE_NAME)
        }

        // Delegate to specific domain error mappers
        is ScopeInputError -> mapDomainError(domainError)
        is DomainScopeAliasError -> mapDomainError(domainError)
        is ScopeHierarchyError -> mapDomainHierarchyError(domainError)
        is DomainScopeUniquenessError -> mapDomainUniquenessError(domainError)

        // Handle other domain errors
        else -> createServiceUnavailableError(SERVICE_NAME)
    }

    private fun mapDomainHierarchyError(domainError: ScopeHierarchyError): ScopeContractError = when (domainError) {
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
                cyclePath = emptyList(),
            ),
        )
        is ScopeHierarchyError.HierarchyUnavailable -> ScopeContractError.SystemError.ServiceUnavailable(
            service = SERVICE_NAME,
        )
    }

    private fun mapDomainUniquenessError(domainError: DomainScopeUniquenessError): ScopeContractError = when (domainError) {
        is DomainScopeUniquenessError.DuplicateTitleInContext -> ScopeContractError.BusinessError.DuplicateTitle(
            title = domainError.title,
            parentId = domainError.parentId?.toString(),
        )
        is DomainScopeUniquenessError.DuplicateIdentifier -> ScopeContractError.BusinessError.DuplicateTitle(
            title = domainError.identifier,
            parentId = null,
        )
    }
}

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

    private fun mapAliasNotFoundError(alias: String): ScopeContractError = ScopeContractError.BusinessError.AliasNotFound(alias = alias)

    private fun mapNotFoundError(scopeId: String): ScopeContractError = ScopeContractError.BusinessError.NotFound(scopeId = scopeId)

    /**
     * Helper method to create InvalidDescription with TooLong validation failure.
     */
    private fun createInvalidDescriptionTooLong(maxLength: Int, actualLength: Int): ScopeContractError.InputError.InvalidDescription =
        ScopeContractError.InputError.InvalidDescription(
            descriptionText = "",
            validationFailure = ScopeContractError.DescriptionValidationFailure.TooLong(
                maximumLength = maxLength,
                actualLength = actualLength,
            ),
        )

    private fun mapAliasToNotFound(error: AppScopeInputError): ScopeContractError {
        val alias = when (error) {
            is AppScopeInputError.AliasNotFound -> error.alias
            is AppScopeInputError.InvalidAlias -> error.alias
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
            parentId = error.parentId,
            expectedFormat = "Valid ULID format",
        )
    }

    private fun mapIdInputError(error: AppScopeInputError): ScopeContractError.InputError.InvalidId = when (error) {
        is AppScopeInputError.IdBlank -> ScopeContractError.InputError.InvalidId(
            id = error.attemptedValue,
            expectedFormat = "Non-empty ULID format",
        )
        is AppScopeInputError.IdInvalidFormat -> ScopeContractError.InputError.InvalidId(
            id = error.attemptedValue,
            expectedFormat = error.expectedFormat,
        )
        else -> error("Unexpected ID error type: $error")
    }

    private fun createInvalidTitle(title: String, validationFailure: ScopeContractError.TitleValidationFailure): ScopeContractError.InputError.InvalidTitle =
        ScopeContractError.InputError.InvalidTitle(title = title, validationFailure = validationFailure)

    private fun mapTitleInputError(error: AppScopeInputError): ScopeContractError.InputError.InvalidTitle = when (error) {
        is AppScopeInputError.TitleEmpty -> createInvalidTitle(
            title = error.attemptedValue,
            validationFailure = ScopeContractError.TitleValidationFailure.Empty,
        )
        is AppScopeInputError.TitleTooShort -> createInvalidTitle(
            title = error.attemptedValue,
            validationFailure = ScopeContractError.TitleValidationFailure.TooShort(
                minimumLength = error.minimumLength,
                actualLength = error.attemptedValue.length,
            ),
        )
        is AppScopeInputError.TitleTooLong -> createInvalidTitle(
            title = error.attemptedValue,
            validationFailure = ScopeContractError.TitleValidationFailure.TooLong(
                maximumLength = error.maximumLength,
                actualLength = error.attemptedValue.length,
            ),
        )
        is AppScopeInputError.TitleContainsProhibitedCharacters -> createInvalidTitle(
            title = error.attemptedValue,
            validationFailure = ScopeContractError.TitleValidationFailure.InvalidCharacters(
                prohibitedCharacters = error.prohibitedCharacters,
            ),
        )
        else -> error("Unexpected title error type: $error")
    }

    private fun mapDescriptionInputError(error: AppScopeInputError.DescriptionTooLong): ScopeContractError.InputError.InvalidDescription =
        ScopeContractError.InputError.InvalidDescription(
            descriptionText = error.attemptedValue,
            validationFailure = ScopeContractError.DescriptionValidationFailure.TooLong(
                maximumLength = error.maximumLength,
                actualLength = error.attemptedValue.length,
            ),
        )

    private fun createInvalidAlias(alias: String, validationFailure: ScopeContractError.AliasValidationFailure): ScopeContractError.InputError.InvalidAlias =
        ScopeContractError.InputError.InvalidAlias(alias = alias, validationFailure = validationFailure)

    private fun mapAliasValidationError(error: AppScopeInputError): ScopeContractError.InputError.InvalidAlias = when (error) {
        is AppScopeInputError.AliasEmpty -> createInvalidAlias(
            alias = error.alias,
            validationFailure = ScopeContractError.AliasValidationFailure.Empty,
        )
        is AppScopeInputError.AliasTooShort -> createInvalidAlias(
            alias = error.alias,
            validationFailure = ScopeContractError.AliasValidationFailure.TooShort(
                minimumLength = error.minimumLength,
                actualLength = error.alias.length,
            ),
        )
        is AppScopeInputError.AliasTooLong -> createInvalidAlias(
            alias = error.alias,
            validationFailure = ScopeContractError.AliasValidationFailure.TooLong(
                maximumLength = error.maximumLength,
                actualLength = error.alias.length,
            ),
        )
        is AppScopeInputError.AliasInvalidFormat -> createInvalidAlias(
            alias = error.alias,
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
            alias = error.alias,
        )
        is AppScopeInputError.CannotRemoveCanonicalAlias -> ScopeContractError.BusinessError.CannotRemoveCanonicalAlias(
            scopeId = "", // No scopeId in application error
            aliasName = "", // No aliasName in application error
        )
        is AppScopeInputError.AliasOfDifferentScope -> ScopeContractError.BusinessError.AliasOfDifferentScope(
            alias = error.alias,
            expectedScopeId = error.expectedScopeId,
            actualScopeId = error.actualScopeId,
        )
        else -> error("Unexpected alias business error type: $error")
    }

    private fun mapContextError(error: ContextError): ScopeContractError = when (error) {
        // Not found errors
        is ContextError.ContextNotFound,
        is ContextError.InvalidContextSwitch,
        -> mapContextToNotFound(error)
        is ContextError.StateNotFound -> mapNotFoundError(error.contextId)

        // Other errors
        is ContextError.DuplicateContextKey -> ScopeContractError.BusinessError.DuplicateAlias(
            alias = error.key,
        )
        is ContextError.KeyInvalidFormat -> ScopeContractError.InputError.InvalidId(
            id = error.attemptedKey,
            expectedFormat = "Valid context key format",
        )

        // Context in use - map to business error for better user experience
        is ContextError.ContextInUse -> ScopeContractError.BusinessError.DuplicateContextKey(
            contextKey = error.key,
            existingContextId = null, // Context is in use but we don't have the specific context ID
        )

        // Context update conflict - map to concurrency error with context key as identifier
        is ContextError.ContextUpdateConflict -> ScopeContractError.SystemError.ConcurrentModification(
            scopeId = error.key, // Use context key as the identifier for the conflict
            expectedVersion = 0L, // We don't have version information from the application error
            actualVersion = 1L,
        )

        // Invalid filter - map to input validation error
        is ContextError.InvalidFilter -> ScopeContractError.InputError.InvalidContextFilter(
            filter = error.filter,
            validationFailure = ScopeContractError.ContextFilterValidationFailure.InvalidSyntax(
                expression = error.filter,
                errorType = error.reason,
                position = null, // Position not available from application error
            ),
        )
    }

    private fun mapScopeAliasError(error: AppScopeAliasError): ScopeContractError = when (error) {
        // Business errors
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

        // System errors
        is AppScopeAliasError.AliasGenerationFailed,
        is AppScopeAliasError.AliasGenerationValidationFailed,
        is AppScopeAliasError.DataInconsistencyError.AliasExistsButScopeNotFound,
        -> mapSystemError()
    }

    private fun mapHierarchyError(error: ScopeHierarchyApplicationError): ScopeContractError = when (error) {
        is ScopeHierarchyApplicationError.CircularReference -> ScopeContractError.BusinessError.HierarchyViolation(
            violation = ScopeContractError.HierarchyViolationType.CircularReference(
                scopeId = error.scopeId,
                parentId = error.cyclePath.firstOrNull() ?: "",
                cyclePath = error.cyclePath,
            ),
        )
        is ScopeHierarchyApplicationError.HasChildren -> ScopeContractError.BusinessError.HasChildren(
            scopeId = error.scopeId,
            childrenCount = error.childCount,
        )
        is ScopeHierarchyApplicationError.InvalidParentId -> ScopeContractError.InputError.InvalidParentId(
            parentId = error.invalidId,
            expectedFormat = "Valid ULID format",
        )
        is ScopeHierarchyApplicationError.MaxChildrenExceeded -> ScopeContractError.BusinessError.HierarchyViolation(
            violation = ScopeContractError.HierarchyViolationType.MaxChildrenExceeded(
                parentId = error.parentScopeId,
                currentChildrenCount = error.currentChildrenCount,
                maximumChildren = error.maximumChildren,
            ),
        )
        is ScopeHierarchyApplicationError.MaxDepthExceeded -> ScopeContractError.BusinessError.HierarchyViolation(
            violation = ScopeContractError.HierarchyViolationType.MaxDepthExceeded(
                scopeId = error.scopeId,
                attemptedDepth = error.attemptedDepth,
                maximumDepth = error.maximumDepth,
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
    }

    private fun mapUniquenessError(error: ScopeUniquenessError): ScopeContractError = when (error) {
        is ScopeUniquenessError.DuplicateTitle -> ScopeContractError.BusinessError.DuplicateTitle(
            title = error.title,
            parentId = error.parentScopeId,
            existingScopeId = error.existingScopeId,
        )
    }

    private fun mapPersistenceError(error: ScopeManagementApplicationError.PersistenceError): ScopeContractError = when (error) {
        is ScopeManagementApplicationError.PersistenceError.ConcurrencyConflict -> {
            val expectedVersion = parseVersionToLong(error.entityId, error.expectedVersion, "expected")
            val actualVersion = parseVersionToLong(error.entityId, error.actualVersion, "actual")
            ScopeContractError.SystemError.ConcurrentModification(
                scopeId = error.entityId,
                expectedVersion = expectedVersion,
                actualVersion = actualVersion,
            )
        }
        is ScopeManagementApplicationError.PersistenceError.NotFound -> mapNotFoundError(error.entityId ?: "")

        // System errors
        is ScopeManagementApplicationError.PersistenceError.DataCorruption,
        is ScopeManagementApplicationError.PersistenceError.StorageUnavailable,
        -> mapSystemError()
    }

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
                invalidType = when (domainError.errorType) {
                    DomainContextError.InvalidKeyFormat.InvalidKeyFormatType.INVALID_CHARACTERS -> "invalid-characters"
                    DomainContextError.InvalidKeyFormat.InvalidKeyFormatType.RESERVED_KEYWORD -> "reserved-keyword"
                    DomainContextError.InvalidKeyFormat.InvalidKeyFormatType.STARTS_WITH_NUMBER -> "starts-with-number"
                    DomainContextError.InvalidKeyFormat.InvalidKeyFormatType.CONTAINS_SPACES -> "contains-spaces"
                    DomainContextError.InvalidKeyFormat.InvalidKeyFormatType.INVALID_PATTERN -> "invalid-pattern"
                },
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
        is DomainContextError.EmptyDescription -> ScopeContractError.InputError.InvalidDescription(
            descriptionText = "",
            validationFailure = ScopeContractError.DescriptionValidationFailure.TooLong(
                maximumLength = 0,
                actualLength = 0,
            ),
        )
        is DomainContextError.DescriptionTooShort -> ScopeContractError.InputError.InvalidDescription(
            descriptionText = "",
            validationFailure = ScopeContractError.DescriptionValidationFailure.TooLong(
                maximumLength = domainError.minimumLength,
                actualLength = 0,
            ),
        )
        is DomainContextError.DescriptionTooLong -> ScopeContractError.InputError.InvalidDescription(
            descriptionText = "",
            validationFailure = ScopeContractError.DescriptionValidationFailure.TooLong(
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
            filter = domainError.expression,
            validationFailure = ScopeContractError.ContextFilterValidationFailure.InvalidSyntax(
                expression = domainError.expression,
                errorType = when (domainError.errorType) {
                    is DomainContextError.FilterSyntaxErrorType.EmptyQuery -> "empty-query"
                    is DomainContextError.FilterSyntaxErrorType.EmptyExpression -> "empty-expression"
                    is DomainContextError.FilterSyntaxErrorType.UnexpectedCharacter -> "unexpected-character"
                    is DomainContextError.FilterSyntaxErrorType.UnterminatedString -> "unterminated-string"
                    is DomainContextError.FilterSyntaxErrorType.UnexpectedToken -> "unexpected-token"
                    is DomainContextError.FilterSyntaxErrorType.MissingClosingParen -> "missing-closing-paren"
                    is DomainContextError.FilterSyntaxErrorType.ExpectedExpression -> "expected-expression"
                    is DomainContextError.FilterSyntaxErrorType.ExpectedIdentifier -> "expected-identifier"
                    is DomainContextError.FilterSyntaxErrorType.ExpectedOperator -> "expected-operator"
                    is DomainContextError.FilterSyntaxErrorType.ExpectedValue -> "expected-value"
                    is DomainContextError.FilterSyntaxErrorType.UnbalancedParentheses -> "unbalanced-parentheses"
                    is DomainContextError.FilterSyntaxErrorType.UnbalancedQuotes -> "unbalanced-quotes"
                    is DomainContextError.FilterSyntaxErrorType.EmptyOperator -> "empty-operator"
                    is DomainContextError.FilterSyntaxErrorType.InvalidSyntax -> "invalid-syntax"
                },
                position = when (val errorType = domainError.errorType) {
                    is DomainContextError.FilterSyntaxErrorType.UnexpectedCharacter -> errorType.position
                    is DomainContextError.FilterSyntaxErrorType.UnterminatedString -> errorType.position
                    is DomainContextError.FilterSyntaxErrorType.UnexpectedToken -> errorType.position
                    is DomainContextError.FilterSyntaxErrorType.MissingClosingParen -> errorType.position
                    is DomainContextError.FilterSyntaxErrorType.ExpectedExpression -> errorType.position
                    is DomainContextError.FilterSyntaxErrorType.ExpectedIdentifier -> errorType.position
                    is DomainContextError.FilterSyntaxErrorType.ExpectedOperator -> errorType.position
                    is DomainContextError.FilterSyntaxErrorType.ExpectedValue -> errorType.position
                    else -> null
                },
            ),
        )

        // Business rule validation errors
        is DomainContextError.InvalidScope -> ScopeContractError.BusinessError.NotFound(
            scopeId = domainError.scopeId,
        )
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
     */
    fun mapDomainError(domainError: AspectKeyError): ScopeContractError = when (domainError) {
        is AspectKeyError.EmptyKey -> ScopeContractError.InputError.InvalidTitle(
            title = "",
            validationFailure = ScopeContractError.TitleValidationFailure.Empty,
        )
        is AspectKeyError.TooShort -> ScopeContractError.InputError.InvalidTitle(
            title = "",
            validationFailure = ScopeContractError.TitleValidationFailure.TooShort(
                minimumLength = domainError.minLength,
                actualLength = domainError.actualLength,
            ),
        )
        is AspectKeyError.TooLong -> ScopeContractError.InputError.InvalidTitle(
            title = "",
            validationFailure = ScopeContractError.TitleValidationFailure.TooLong(
                maximumLength = domainError.maxLength,
                actualLength = domainError.actualLength,
            ),
        )
        is AspectKeyError.InvalidFormat -> ScopeContractError.InputError.InvalidTitle(
            title = "",
            validationFailure = ScopeContractError.TitleValidationFailure.InvalidCharacters(
                prohibitedCharacters = listOf(),
            ),
        )
    }

    /**
     * Maps domain aspect validation errors to contract errors.
     */
    fun mapDomainError(domainError: AspectValidationError): ScopeContractError = when (domainError) {
        // AspectKey validation errors
        is AspectValidationError.EmptyAspectKey -> ScopeContractError.InputError.InvalidTitle(
            title = "",
            validationFailure = ScopeContractError.TitleValidationFailure.Empty,
        )
        is AspectValidationError.AspectKeyTooShort -> ScopeContractError.InputError.InvalidTitle(
            title = "",
            validationFailure = ScopeContractError.TitleValidationFailure.TooShort(
                minimumLength = 1,
                actualLength = 0,
            ),
        )
        is AspectValidationError.AspectKeyTooLong -> ScopeContractError.InputError.InvalidTitle(
            title = "",
            validationFailure = ScopeContractError.TitleValidationFailure.TooLong(
                maximumLength = domainError.maxLength,
                actualLength = domainError.actualLength,
            ),
        )
        is AspectValidationError.InvalidAspectKeyFormat -> ScopeContractError.InputError.InvalidTitle(
            title = "",
            validationFailure = ScopeContractError.TitleValidationFailure.InvalidCharacters(
                prohibitedCharacters = listOf(),
            ),
        )

        // AspectValue validation errors
        is AspectValidationError.EmptyAspectValue -> createInvalidDescriptionTooLong(0, 0)
        is AspectValidationError.AspectValueTooShort -> createInvalidDescriptionTooLong(1, 0)
        is AspectValidationError.AspectValueTooLong -> createInvalidDescriptionTooLong(
            domainError.maxLength,
            domainError.actualLength,
        )

        // AspectDefinition validation errors
        is AspectValidationError.EmptyAspectAllowedValues -> createInvalidDescriptionTooLong(0, 0)
        is AspectValidationError.DuplicateAspectAllowedValues -> ScopeContractError.BusinessError.DuplicateTitle(
            title = "Duplicate allowed values",
            parentId = null,
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
        is ScopesError.InvalidOperation -> createServiceUnavailableError(SERVICE_NAME)
        is ScopesError.AlreadyExists -> ScopeContractError.BusinessError.DuplicateAlias(
            alias = domainError.identifier,
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
            else -> ScopeContractError.InputError.InvalidTitle(
                title = domainError.value,
                validationFailure = ScopeContractError.TitleValidationFailure.Empty,
            )
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
            else -> ScopeContractError.SystemError.ServiceUnavailable(
                service = SERVICE_NAME,
            )
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
            ScopesError.RepositoryError.RepositoryFailure.OPERATION_FAILED,
            ScopesError.RepositoryError.RepositoryFailure.CORRUPTED_DATA,
            -> createServiceUnavailableError(SERVICE_NAME)
            ScopesError.RepositoryError.RepositoryFailure.CONSTRAINT_VIOLATION -> ScopeContractError.BusinessError.DuplicateTitle(
                title = context?.attemptedValue ?: "",
                parentId = context?.parentId,
            )
            null -> createServiceUnavailableError(SERVICE_NAME)
        }
        is ScopesError.ScopeStatusTransitionError -> createServiceUnavailableError(SERVICE_NAME)

        // Domain-specific errors that extend ScopesError (delegation through inheritance)
        is ScopeInputError -> {
            // Create app error for mapInputError
            val appError = when (domainError) {
                is ScopeInputError.TitleError.EmptyTitle -> AppScopeInputError.TitleEmpty(attemptedValue = "")
                is ScopeInputError.TitleError.TitleTooShort -> AppScopeInputError.TitleTooShort(
                    attemptedValue = "",
                    minimumLength = domainError.minLength,
                )
                is ScopeInputError.TitleError.TitleTooLong -> AppScopeInputError.TitleTooLong(
                    attemptedValue = "",
                    maximumLength = domainError.maxLength,
                )
                is ScopeInputError.TitleError.InvalidTitleFormat -> AppScopeInputError.TitleContainsProhibitedCharacters(
                    attemptedValue = domainError.title,
                    prohibitedCharacters = listOf(),
                )
                is ScopeInputError.DescriptionError.DescriptionTooLong -> AppScopeInputError.DescriptionTooLong(
                    attemptedValue = "",
                    maximumLength = domainError.maxLength,
                )
                is ScopeInputError.IdError.EmptyId -> AppScopeInputError.IdBlank(attemptedValue = "")
                is ScopeInputError.IdError.InvalidIdFormat -> AppScopeInputError.IdInvalidFormat(
                    attemptedValue = domainError.id,
                    expectedFormat = domainError.expectedFormat.toString(),
                )
                // Handle alias error types
                is ScopeInputError.AliasError.EmptyAlias -> AppScopeInputError.AliasEmpty(alias = "")
                is ScopeInputError.AliasError.AliasTooShort -> AppScopeInputError.AliasTooShort(
                    alias = "",
                    minimumLength = domainError.minLength,
                )
                is ScopeInputError.AliasError.AliasTooLong -> AppScopeInputError.AliasTooLong(
                    alias = "",
                    maximumLength = domainError.maxLength,
                )
                is ScopeInputError.AliasError.InvalidAliasFormat -> AppScopeInputError.AliasInvalidFormat(
                    alias = domainError.alias,
                    expectedPattern = domainError.expectedPattern.toString(),
                )
            }
            mapInputError(appError)
        }
        is ScopeHierarchyError -> {
            // Map domain hierarchy errors to contract hierarchy violations
            when (domainError) {
                is ScopeHierarchyError.CircularDependency ->
                    ScopeContractError.BusinessError.HierarchyViolation(
                        violation = ScopeContractError.HierarchyViolationType.CircularReference(
                            scopeId = domainError.scopeId.toString(),
                            parentId = domainError.ancestorId.toString(),
                            cyclePath = listOf(domainError.scopeId.toString(), domainError.ancestorId.toString()),
                        ),
                    )
                is ScopeHierarchyError.MaxDepthExceeded ->
                    ScopeContractError.BusinessError.HierarchyViolation(
                        violation = ScopeContractError.HierarchyViolationType.MaxDepthExceeded(
                            scopeId = domainError.scopeId.toString(),
                            attemptedDepth = domainError.currentDepth,
                            maximumDepth = domainError.maxDepth,
                        ),
                    )
                is ScopeHierarchyError.MaxChildrenExceeded ->
                    ScopeContractError.BusinessError.HierarchyViolation(
                        violation = ScopeContractError.HierarchyViolationType.MaxChildrenExceeded(
                            parentId = domainError.parentId.toString(),
                            currentChildrenCount = domainError.currentCount,
                            maximumChildren = domainError.maxChildren,
                        ),
                    )
                is ScopeHierarchyError.HierarchyUnavailable ->
                    ScopeContractError.SystemError.ServiceUnavailable(
                        service = "Scope hierarchy service",
                    )
            }
        }
        is DomainScopeUniquenessError -> {
            // Map domain uniqueness errors directly to contract errors
            when (domainError) {
                is DomainScopeUniquenessError.DuplicateTitleInContext ->
                    ScopeContractError.BusinessError.DuplicateTitle(
                        title = domainError.title,
                        parentId = domainError.parentId?.toString(),
                        existingScopeId = domainError.existingId.toString(),
                    )
                is DomainScopeUniquenessError.DuplicateIdentifier ->
                    ScopeContractError.BusinessError.DuplicateAlias(
                        alias = domainError.identifier,
                        existingScopeId = null,
                        attemptedScopeId = null,
                    )
            }
        }
        is DomainScopeAliasError -> {
            // Direct mapping to contract error without intermediate app error
            when (domainError) {
                is DomainScopeAliasError.DuplicateAlias -> ScopeContractError.BusinessError.DuplicateAlias(
                    alias = domainError.alias,
                    existingScopeId = domainError.scopeId.toString(),
                    attemptedScopeId = null,
                )
                is DomainScopeAliasError.AliasGenerationFailed -> ScopeContractError.BusinessError.AliasGenerationFailed(
                    scopeId = domainError.scopeId.toString(),
                    retryCount = 0, // Not available from domain error
                )
                is DomainScopeAliasError.AliasError -> ScopeContractError.BusinessError.AliasGenerationValidationFailed(
                    scopeId = "", // Not available from domain error
                    alias = domainError.alias,
                    reason = domainError.reason,
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
                is DomainScopeAliasError.DataInconsistencyError.AliasReferencesNonExistentScope ->
                    ScopeContractError.SystemError.ServiceUnavailable(
                        service = SERVICE_NAME,
                    )
                is DomainScopeAliasError.DataInconsistencyError -> error(
                    "Unmapped DataInconsistencyError subtype: ${domainError::class.simpleName}",
                )
            }
        }

        // Other errors - map to system error
        else -> {
            logger.warn(
                "Unmapped domain error type, using ServiceUnavailable",
                mapOf(
                    "errorType" to (domainError::class.qualifiedName ?: domainError::class.simpleName ?: "UnknownError"),
                    "message" to domainError.toString(),
                ),
            )
            createServiceUnavailableError(SERVICE_NAME)
        }
    }
}

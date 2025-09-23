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

    private fun createDuplicateContextKeyError(contextKey: String, existingContextId: String? = null): ScopeContractError.BusinessError.DuplicateContextKey =
        ScopeContractError.BusinessError.DuplicateContextKey(
            contextKey = contextKey,
            existingContextId = existingContextId,
        )

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

    private fun mapIdInputError(error: AppScopeInputError): ScopeContractError.InputError.InvalidId = when (error) {
        is AppScopeInputError.IdBlank -> ScopeContractError.InputError.InvalidId(
            id = error.preview,
            expectedFormat = "Non-empty ULID format",
        )
        is AppScopeInputError.IdInvalidFormat -> ScopeContractError.InputError.InvalidId(
            id = error.preview,
            expectedFormat = error.expectedFormat,
        )
        else -> error("Unexpected ID error type: $error")
    }

    private fun createInvalidTitle(title: String, validationFailure: ScopeContractError.TitleValidationFailure): ScopeContractError.InputError.InvalidTitle =
        ScopeContractError.InputError.InvalidTitle(title = title, validationFailure = validationFailure)

    /**
     * Creates an InvalidTitle error with InvalidCharacters validation failure.
     * This is a common pattern for aspect validation errors.
     */
    private fun createInvalidTitleWithInvalidCharacters(
        title: String = "",
        prohibitedCharacters: List<Char> = listOf(),
    ): ScopeContractError.InputError.InvalidTitle = createInvalidTitle(
        title = title,
        validationFailure = ScopeContractError.TitleValidationFailure.InvalidCharacters(
            prohibitedCharacters = prohibitedCharacters,
        ),
    )

    /**
     * Creates a ValidationFailure error with RequiredField constraint.
     */
    private fun createRequiredFieldError(field: String, value: String = ""): ScopeContractError.InputError.ValidationFailure =
        ScopeContractError.InputError.ValidationFailure(
            field = field,
            value = value,
            constraint = ScopeContractError.ValidationConstraint.RequiredField(
                field = field,
            ),
        )

    /**
     * Creates an InvalidTitle error with Empty validation failure.
     * This is commonly used for empty key/title errors.
     */
    private fun createEmptyTitleError(): ScopeContractError.InputError.InvalidTitle = ScopeContractError.InputError.InvalidTitle(
        title = "",
        validationFailure = ScopeContractError.TitleValidationFailure.Empty,
    )

    /**
     * Creates an InvalidTitle error with TooShort validation failure.
     */
    private fun createTooShortTitleError(minLength: Int, actualLength: Int): ScopeContractError.InputError.InvalidTitle =
        ScopeContractError.InputError.InvalidTitle(
            title = "",
            validationFailure = ScopeContractError.TitleValidationFailure.TooShort(
                minimumLength = minLength,
                actualLength = actualLength,
            ),
        )

    /**
     * Creates an InvalidTitle error with TooLong validation failure.
     */
    private fun createTooLongTitleError(maxLength: Int, actualLength: Int): ScopeContractError.InputError.InvalidTitle =
        ScopeContractError.InputError.InvalidTitle(
            title = "",
            validationFailure = ScopeContractError.TitleValidationFailure.TooLong(
                maximumLength = maxLength,
                actualLength = actualLength,
            ),
        )

    /**
     * Creates a ServiceUnavailable error.
     */
    private fun createServiceUnavailableError(service: String? = null): ScopeContractError.SystemError.ServiceUnavailable =
        ScopeContractError.SystemError.ServiceUnavailable(
            service = service ?: SERVICE_NAME,
        )

    private fun mapTitleInputError(error: AppScopeInputError): ScopeContractError.InputError.InvalidTitle = when (error) {
        is AppScopeInputError.TitleEmpty -> createInvalidTitle(
            title = error.preview,
            validationFailure = ScopeContractError.TitleValidationFailure.Empty,
        )
        is AppScopeInputError.TitleTooShort -> createInvalidTitle(
            title = error.preview,
            validationFailure = ScopeContractError.TitleValidationFailure.TooShort(
                minimumLength = error.minimumLength,
                actualLength = error.preview.length,
            ),
        )
        is AppScopeInputError.TitleTooLong -> createInvalidTitle(
            title = error.preview,
            validationFailure = ScopeContractError.TitleValidationFailure.TooLong(
                maximumLength = error.maximumLength,
                actualLength = error.preview.length,
            ),
        )
        is AppScopeInputError.TitleContainsProhibitedCharacters -> createInvalidTitle(
            title = error.preview,
            validationFailure = ScopeContractError.TitleValidationFailure.InvalidCharacters(
                prohibitedCharacters = error.prohibitedCharacters,
            ),
        )
        else -> error("Unexpected title error type: $error")
    }

    private fun mapDescriptionInputError(error: AppScopeInputError.DescriptionTooLong): ScopeContractError.InputError.InvalidDescription =
        ScopeContractError.InputError.InvalidDescription(
            descriptionText = error.preview,
            validationFailure = ScopeContractError.DescriptionValidationFailure.TooLong(
                maximumLength = error.maximumLength,
                actualLength = error.preview.length,
            ),
        )

    private fun createInvalidAlias(alias: String, validationFailure: ScopeContractError.AliasValidationFailure): ScopeContractError.InputError.InvalidAlias =
        ScopeContractError.InputError.InvalidAlias(alias = alias, validationFailure = validationFailure)

    private fun mapAliasValidationError(error: AppScopeInputError): ScopeContractError.InputError.InvalidAlias = when (error) {
        is AppScopeInputError.AliasEmpty -> createInvalidAlias(
            alias = error.preview,
            validationFailure = ScopeContractError.AliasValidationFailure.Empty,
        )
        is AppScopeInputError.AliasTooShort -> createInvalidAlias(
            alias = error.preview,
            validationFailure = ScopeContractError.AliasValidationFailure.TooShort(
                minimumLength = error.minimumLength,
                actualLength = error.preview.length,
            ),
        )
        is AppScopeInputError.AliasTooLong -> createInvalidAlias(
            alias = error.preview,
            validationFailure = ScopeContractError.AliasValidationFailure.TooLong(
                maximumLength = error.maximumLength,
                actualLength = error.preview.length,
            ),
        )
        is AppScopeInputError.AliasInvalidFormat -> createInvalidAlias(
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
            scopeId = "", // No scopeId in application error
            aliasName = "", // No aliasName in application error
        )
        is AppScopeInputError.AliasOfDifferentScope -> ScopeContractError.BusinessError.AliasOfDifferentScope(
            alias = error.preview,
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
        is ContextError.DuplicateContextKey -> createDuplicateContextKeyError(error.key, error.existingContextId)
        is ContextError.KeyInvalidFormat -> ScopeContractError.InputError.InvalidId(
            id = error.attemptedKey,
            expectedFormat = "Valid context key format",
        )

        // Context in use - map to business error for better user experience
        is ContextError.ContextInUse -> createDuplicateContextKeyError(error.key)

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
            title = InputSanitizer.createPreview(domainError.title),
            parentId = domainError.contextId,
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
                expectedFormat = "Minimum length: 1",
            ),
        )
        is AspectValidationError.AspectValueTooLong -> ScopeContractError.InputError.ValidationFailure(
            field = "aspectValue",
            value = "",
            constraint = ScopeContractError.ValidationConstraint.InvalidFormat(
                expectedFormat = "Maximum length: ${domainError.maxLength}",
            ),
        )

        // AspectDefinition validation errors
        is AspectValidationError.EmptyAspectAllowedValues -> ScopeContractError.InputError.ValidationFailure(
            field = "allowedValues",
            value = "",
            constraint = ScopeContractError.ValidationConstraint.EmptyValues(
                field = "allowedValues",
            ),
        )
        is AspectValidationError.DuplicateAspectAllowedValues -> ScopeContractError.InputError.ValidationFailure(
            field = "allowedValues",
            value = "",
            constraint = ScopeContractError.ValidationConstraint.InvalidValue(
                expectedValues = null,
                actualValue = "duplicate values",
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
                is ScopesError.ValidationConstraintType.NotInAllowedValues ->
                    ScopeContractError.ValidationConstraint.InvalidValue(
                        expectedValues = constraint.allowedValues,
                        actualValue = InputSanitizer.createPreview(domainError.value),
                    )
                is ScopesError.ValidationConstraintType.MissingRequired ->
                    ScopeContractError.ValidationConstraint.RequiredField(
                        field = domainError.field,
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
                    ScopeContractError.ValidationConstraint.InvalidValue(
                        expectedValues = null, // Generic invalid value without specific allowed values
                        actualValue = InputSanitizer.createPreview(domainError.value),
                    )
            }

            ScopeContractError.InputError.ValidationFailure(
                field = domainError.field,
                value = InputSanitizer.createPreview(domainError.value),
                constraint = contractConstraint,
            )
        }
        is ScopesError.Conflict -> when (domainError.conflictType) {
            ScopesError.Conflict.ConflictType.DUPLICATE_KEY -> ScopeContractError.BusinessError.DuplicateTitle(
                title = InputSanitizer.createPreview(domainError.resourceId),
                parentId = null,
            )
            ScopesError.Conflict.ConflictType.HAS_DEPENDENCIES -> ScopeContractError.BusinessError.HasChildren(
                scopeId = domainError.resourceId,
                childrenCount = 1,
            )
            else -> createServiceUnavailableError()
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
            -> createServiceUnavailableError()
            ScopesError.RepositoryError.RepositoryFailure.CONSTRAINT_VIOLATION -> ScopeContractError.BusinessError.DuplicateTitle(
                title = InputSanitizer.createPreview(context?.attemptedValue ?: ""),
                parentId = context?.parentId,
            )
            null -> createServiceUnavailableError()
        }
        is ScopesError.ScopeStatusTransitionError -> createServiceUnavailableError()

        // Domain-specific errors that extend ScopesError (delegation through inheritance)
        is ScopeInputError -> {
            // Create app error for mapInputError
            val appError = when (domainError) {
                is ScopeInputError.TitleError.EmptyTitle -> AppScopeInputError.TitleEmpty(preview = InputSanitizer.createPreview(""))
                is ScopeInputError.TitleError.TitleTooShort -> AppScopeInputError.TitleTooShort(
                    preview = InputSanitizer.createPreview(""),
                    minimumLength = domainError.minLength,
                )
                is ScopeInputError.TitleError.TitleTooLong -> AppScopeInputError.TitleTooLong(
                    preview = InputSanitizer.createPreview(""),
                    maximumLength = domainError.maxLength,
                )
                is ScopeInputError.TitleError.InvalidTitleFormat -> AppScopeInputError.TitleContainsProhibitedCharacters(
                    preview = InputSanitizer.createPreview(domainError.title),
                    prohibitedCharacters = listOf(),
                )
                is ScopeInputError.DescriptionError.DescriptionTooLong -> AppScopeInputError.DescriptionTooLong(
                    preview = InputSanitizer.createPreview(""),
                    maximumLength = domainError.maxLength,
                )
                is ScopeInputError.IdError.EmptyId -> AppScopeInputError.IdBlank(preview = InputSanitizer.createPreview(""))
                is ScopeInputError.IdError.InvalidIdFormat -> AppScopeInputError.IdInvalidFormat(
                    preview = InputSanitizer.createPreview(domainError.id),
                    expectedFormat = domainError.expectedFormat.toString(),
                )
                // Handle alias error types
                is ScopeInputError.AliasError.EmptyAlias -> AppScopeInputError.AliasEmpty(preview = InputSanitizer.createPreview(""))
                is ScopeInputError.AliasError.AliasTooShort -> AppScopeInputError.AliasTooShort(
                    preview = InputSanitizer.createPreview(""),
                    minimumLength = domainError.minLength,
                )
                is ScopeInputError.AliasError.AliasTooLong -> AppScopeInputError.AliasTooLong(
                    preview = InputSanitizer.createPreview(""),
                    maximumLength = domainError.maxLength,
                )
                is ScopeInputError.AliasError.InvalidAliasFormat -> AppScopeInputError.AliasInvalidFormat(
                    preview = InputSanitizer.createPreview(domainError.alias),
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
                    createServiceUnavailableError(
                        service = "Scope hierarchy service",
                    )
            }
        }
        is DomainScopeUniquenessError -> {
            // Map domain uniqueness errors directly to contract errors
            when (domainError) {
                is DomainScopeUniquenessError.DuplicateTitleInContext ->
                    ScopeContractError.BusinessError.DuplicateTitle(
                        title = InputSanitizer.createPreview(domainError.title),
                        parentId = domainError.parentId?.toString(),
                        existingScopeId = domainError.existingId.toString(),
                    )
                is DomainScopeUniquenessError.DuplicateIdentifier ->
                    ScopeContractError.BusinessError.DuplicateAlias(
                        alias = InputSanitizer.createPreview(domainError.identifier),
                        existingScopeId = null,
                        attemptedScopeId = null,
                    )
            }
        }
        is DomainScopeAliasError -> {
            // Direct mapping to contract error without intermediate app error
            when (domainError) {
                is DomainScopeAliasError.DuplicateAlias -> ScopeContractError.BusinessError.DuplicateAlias(
                    alias = domainError.aliasName.value,
                    existingScopeId = domainError.existingScopeId.value,
                    attemptedScopeId = domainError.attemptedScopeId.value,
                )
                is DomainScopeAliasError.AliasGenerationFailed -> ScopeContractError.BusinessError.AliasGenerationFailed(
                    scopeId = domainError.scopeId.toString(),
                    retryCount = 0, // Not available from domain error
                )
                is DomainScopeAliasError.AliasError -> ScopeContractError.BusinessError.AliasGenerationValidationFailed(
                    scopeId = "", // Not available from domain error
                    alias = InputSanitizer.createPreview(domainError.alias),
                    reason = domainError.reason,
                )
                is DomainScopeAliasError.AliasNotFoundByName -> ScopeContractError.BusinessError.AliasNotFound(
                    alias = InputSanitizer.createPreview(domainError.alias),
                )
                is DomainScopeAliasError.AliasNotFoundById -> ScopeContractError.BusinessError.AliasNotFound(
                    alias = domainError.aliasId.toString(),
                )
                is DomainScopeAliasError.CannotRemoveCanonicalAlias -> ScopeContractError.BusinessError.CannotRemoveCanonicalAlias(
                    scopeId = domainError.scopeId.toString(),
                    aliasName = InputSanitizer.createPreview(domainError.alias),
                )
                is DomainScopeAliasError.DataInconsistencyError.AliasReferencesNonExistentScope ->
                    createServiceUnavailableError()
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
            createServiceUnavailableError()
        }
    }
}

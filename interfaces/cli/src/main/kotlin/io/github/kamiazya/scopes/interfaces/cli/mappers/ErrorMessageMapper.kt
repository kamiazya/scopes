package io.github.kamiazya.scopes.interfaces.cli.mappers

import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.scopemanagement.domain.error.AggregateIdError
import io.github.kamiazya.scopes.scopemanagement.domain.error.AspectKeyError
import io.github.kamiazya.scopes.scopemanagement.domain.error.AspectValidationError
import io.github.kamiazya.scopes.scopemanagement.domain.error.AspectValueError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ContextError
import io.github.kamiazya.scopes.scopemanagement.domain.error.HierarchyPolicyError
import io.github.kamiazya.scopes.scopemanagement.domain.error.PersistenceError
import io.github.kamiazya.scopes.scopemanagement.domain.error.QueryParseError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeAliasError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeHierarchyError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeNotFoundError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeUniquenessError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.domain.error.UserPreferencesIntegrationError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ValidationError

/**
 * Maps domain and contract errors to user-friendly messages for CLI output.
 */
object ErrorMessageMapper {
    // Message template helpers
    private object MessageTemplates {
        fun notFound(entityType: String, id: String) = "$entityType not found: $id"
        fun tooShort(field: String, min: Int) = "$field is too short: minimum $min characters"
        fun tooLong(field: String, max: Int) = "$field is too long: maximum $max characters"
        fun invalidFormat(field: String, value: String) = "Invalid $field format: $value"
        fun cannotBeEmpty(field: String) = "$field cannot be empty"
        fun alreadyExists(field: String, value: String) = "$field '$value' already exists"
        fun duplicate(field: String, value: String, context: String? = null) = if (context != null) {
            "$field '$value' already exists in $context"
        } else {
            "$field '$value' already exists"
        }
    }

    // Presentation helpers
    private fun presentAliasPattern(patternType: ScopeInputError.AliasError.InvalidAliasFormat.AliasPatternType): String = when (patternType) {
        ScopeInputError.AliasError.InvalidAliasFormat.AliasPatternType.LOWERCASE_WITH_HYPHENS -> "lowercase letters with hyphens"
        ScopeInputError.AliasError.InvalidAliasFormat.AliasPatternType.ALPHANUMERIC -> "alphanumeric characters"
        ScopeInputError.AliasError.InvalidAliasFormat.AliasPatternType.ULID_LIKE -> "ULID-like format"
        ScopeInputError.AliasError.InvalidAliasFormat.AliasPatternType.CUSTOM_PATTERN -> "custom pattern"
    }

    private fun presentInvalidScopeType(errorType: ContextError.InvalidScope.InvalidScopeType): String = when (errorType) {
        ContextError.InvalidScope.InvalidScopeType.SCOPE_NOT_FOUND -> "scope not found"
        ContextError.InvalidScope.InvalidScopeType.SCOPE_ARCHIVED -> "scope is archived"
        ContextError.InvalidScope.InvalidScopeType.SCOPE_DELETED -> "scope is deleted"
        ContextError.InvalidScope.InvalidScopeType.INSUFFICIENT_PERMISSIONS -> "insufficient permissions"
        ContextError.InvalidScope.InvalidScopeType.INVALID_STATE -> "invalid state"
    }

    private fun presentInvalidHierarchyType(errorType: ContextError.InvalidHierarchy.InvalidHierarchyType): String = when (errorType) {
        ContextError.InvalidHierarchy.InvalidHierarchyType.CIRCULAR_REFERENCE -> "circular reference detected"
        ContextError.InvalidHierarchy.InvalidHierarchyType.DEPTH_LIMIT_EXCEEDED -> "depth limit exceeded"
        ContextError.InvalidHierarchy.InvalidHierarchyType.PARENT_NOT_FOUND -> "parent not found"
        ContextError.InvalidHierarchy.InvalidHierarchyType.INVALID_PARENT_TYPE -> "invalid parent type"
        ContextError.InvalidHierarchy.InvalidHierarchyType.CROSS_CONTEXT_HIERARCHY -> "cross-context hierarchy not allowed"
    }

    private fun presentDuplicateScopeType(errorType: ContextError.DuplicateScope.DuplicateScopeType): String = when (errorType) {
        ContextError.DuplicateScope.DuplicateScopeType.TITLE_EXISTS_IN_CONTEXT -> "title already exists in context"
        ContextError.DuplicateScope.DuplicateScopeType.ALIAS_ALREADY_TAKEN -> "alias already taken"
        ContextError.DuplicateScope.DuplicateScopeType.IDENTIFIER_CONFLICT -> "identifier conflict"
    }

    private fun presentInvalidKeyFormatType(errorType: ContextError.InvalidKeyFormat.InvalidKeyFormatType): String = when (errorType) {
        ContextError.InvalidKeyFormat.InvalidKeyFormatType.INVALID_CHARACTERS -> "invalid characters"
        ContextError.InvalidKeyFormat.InvalidKeyFormatType.RESERVED_KEYWORD -> "reserved keyword"
        ContextError.InvalidKeyFormat.InvalidKeyFormatType.STARTS_WITH_NUMBER -> "starts with number"
        ContextError.InvalidKeyFormat.InvalidKeyFormatType.CONTAINS_SPACES -> "contains spaces"
        ContextError.InvalidKeyFormat.InvalidKeyFormatType.INVALID_PATTERN -> "invalid pattern"
    }

    // Main error mapping function
    fun getMessage(error: ScopesError): String = when (error) {
        is ScopeError -> handleScopeError(error)
        is ScopeInputError -> handleScopeInputError(error)
        is ScopeNotFoundError -> MessageTemplates.notFound("Scope", error.scopeId.value)
        is ScopeUniquenessError -> handleScopeUniquenessError(error)
        is ScopeHierarchyError -> handleScopeHierarchyError(error)
        is PersistenceError -> handlePersistenceError(error)
        is AggregateIdError -> handleAggregateIdError(error)
        is ContextError -> handleContextError(error)
        is AspectValidationError -> handleAspectValidationError(error)
        is AspectKeyError -> handleAspectKeyError(error)
        is AspectValueError -> handleAspectValueError(error)
        is HierarchyPolicyError -> handleHierarchyPolicyError(error)
        is ScopeAliasError -> handleScopeAliasError(error)
        is UserPreferencesIntegrationError -> handleUserPreferencesIntegrationError(error)
        is QueryParseError -> handleQueryParseError(error)
        is ValidationError -> handleValidationError(error)
        is ScopesError.InvalidOperation -> handleInvalidOperation(error)
        is ScopesError.AlreadyExists -> handleAlreadyExists(error)
        is ScopesError.NotFound -> handleNotFound(error)
        is ScopesError.SystemError -> handleSystemError(error)
        is ScopesError.ValidationFailed -> handleValidationFailed(error)
        is ScopesError.Conflict -> handleConflict(error)
        is ScopesError.ConcurrencyError -> handleConcurrencyError(error)
        is ScopesError.RepositoryError -> handleRepositoryError(error)
        is ScopesError.ScopeStatusTransitionError -> handleScopeStatusTransitionError(error)
    }

    // Error-specific handlers
    private fun handleScopeError(error: ScopeError): String = when (error) {
        is ScopeError.NotFound -> MessageTemplates.notFound("Scope", error.scopeId.value)
        is ScopeError.ParentNotFound -> MessageTemplates.notFound("Parent scope", error.parentId.value)
        is ScopeError.DuplicateTitle -> formatDuplicateTitle(error.title, error.parentId)
        is ScopeError.AlreadyDeleted -> "Scope is already deleted: ${error.scopeId.value}"
        is ScopeError.AlreadyArchived -> "Scope is already archived: ${error.scopeId.value}"
        is ScopeError.NotArchived -> "Scope is not archived: ${error.scopeId.value}"
        is ScopeError.VersionMismatch -> formatVersionMismatch(error)
    }

    private fun handleScopeInputError(error: ScopeInputError): String = when (error) {
        is ScopeInputError.IdError -> handleIdError(error)
        is ScopeInputError.TitleError -> handleTitleError(error)
        is ScopeInputError.DescriptionError -> handleDescriptionError(error)
        is ScopeInputError.AliasError -> handleAliasError(error)
    }

    private fun handleIdError(error: ScopeInputError.IdError): String = when (error) {
        is ScopeInputError.IdError.EmptyId -> MessageTemplates.cannotBeEmpty("Scope ID")
        is ScopeInputError.IdError.InvalidIdFormat -> MessageTemplates.invalidFormat("scope ID", error.id)
    }

    private fun handleTitleError(error: ScopeInputError.TitleError): String = when (error) {
        is ScopeInputError.TitleError.EmptyTitle -> MessageTemplates.cannotBeEmpty("Scope title")
        is ScopeInputError.TitleError.TitleTooShort -> MessageTemplates.tooShort("Scope title", error.minLength)
        is ScopeInputError.TitleError.TitleTooLong -> MessageTemplates.tooLong("Scope title", error.maxLength)
        is ScopeInputError.TitleError.InvalidTitleFormat -> MessageTemplates.invalidFormat("scope title", error.title)
    }

    private fun handleDescriptionError(error: ScopeInputError.DescriptionError): String = when (error) {
        is ScopeInputError.DescriptionError.DescriptionTooLong -> MessageTemplates.tooLong("Scope description", error.maxLength)
    }

    private fun handleAliasError(error: ScopeInputError.AliasError): String = when (error) {
        is ScopeInputError.AliasError.EmptyAlias -> MessageTemplates.cannotBeEmpty("Scope alias")
        is ScopeInputError.AliasError.AliasTooShort -> MessageTemplates.tooShort("Scope alias", error.minLength)
        is ScopeInputError.AliasError.AliasTooLong -> MessageTemplates.tooLong("Scope alias", error.maxLength)
        is ScopeInputError.AliasError.InvalidAliasFormat -> "Invalid scope alias format: expected ${presentAliasPattern(error.expectedPattern)}"
    }

    private fun handleScopeUniquenessError(error: ScopeUniquenessError): String = when (error) {
        is ScopeUniquenessError.DuplicateTitleInContext -> formatDuplicateTitleInContext(error.title, error.parentId)
        is ScopeUniquenessError.DuplicateIdentifier -> "Duplicate identifier: ${error.identifier} (type: ${error.identifierType})"
    }

    private fun handleScopeHierarchyError(error: ScopeHierarchyError): String = when (error) {
        is ScopeHierarchyError.CircularDependency ->
            "Circular dependency detected for scope ${error.scopeId.value} with ancestor ${error.ancestorId.value}"
        is ScopeHierarchyError.MaxDepthExceeded ->
            "Maximum hierarchy depth exceeded for scope ${error.scopeId.value}: " +
                "current depth ${error.currentDepth}, maximum ${error.maxDepth}"
        is ScopeHierarchyError.MaxChildrenExceeded ->
            "Maximum children exceeded for parent ${error.parentId.value}: " +
                "current ${error.currentCount}, maximum ${error.maxChildren}"
        is ScopeHierarchyError.HierarchyUnavailable ->
            "Hierarchy operation '${error.operation}' failed${formatOptionalScope(error.scopeId)}: ${error.reason}"
    }

    private fun handlePersistenceError(error: PersistenceError): String = when (error) {
        is PersistenceError.ConcurrencyConflict ->
            "Concurrency conflict for ${error.entityType} ${error.entityId}: " +
                "expected version ${error.expectedVersion}, actual ${error.actualVersion}"
    }

    private fun handleAggregateIdError(error: AggregateIdError): String = when (error) {
        is AggregateIdError.InvalidFormat ->
            "Invalid aggregate ID format '${error.value}': ${error.formatError.name.lowercase().replace('_', ' ')}"
    }

    private fun handleContextError(error: ContextError): String = when (error) {
        is ContextError.EmptyKey -> MessageTemplates.cannotBeEmpty("Context view key")
        is ContextError.KeyTooShort -> "Context view key must be at least ${error.minimumLength} characters"
        is ContextError.KeyTooLong -> "Context view key must be at most ${error.maximumLength} characters"
        is ContextError.InvalidKeyFormat -> "Invalid context view key format: ${presentInvalidKeyFormatType(error.errorType)}"
        is ContextError.EmptyName -> MessageTemplates.cannotBeEmpty("Context view name")
        is ContextError.NameTooLong -> MessageTemplates.tooLong("Context view name", error.maximumLength)
        is ContextError.EmptyDescription -> MessageTemplates.cannotBeEmpty("Context view description")
        is ContextError.DescriptionTooShort -> "Context view description must be at least ${error.minimumLength} characters"
        is ContextError.DescriptionTooLong -> "Context view description must be at most ${error.maximumLength} characters"
        is ContextError.EmptyFilter -> MessageTemplates.cannotBeEmpty("Context view filter")
        is ContextError.FilterTooShort -> "Context view filter must be at least ${error.minimumLength} characters"
        is ContextError.FilterTooLong -> "Context view filter must be at most ${error.maximumLength} characters"
        is ContextError.InvalidFilterSyntax -> "Invalid filter syntax: ${formatFilterSyntaxError(error.errorType)}"
        is ContextError.InvalidScope -> "Invalid scope '${error.scopeId}': ${presentInvalidScopeType(error.errorType)}"
        is ContextError.InvalidHierarchy ->
            "Invalid hierarchy for scope '${error.scopeId}' with parent '${error.parentId}': " +
                presentInvalidHierarchyType(error.errorType)
        is ContextError.DuplicateScope ->
            "Duplicate scope title '${error.title}'${formatOptionalContext(error.contextId)}: " +
                presentDuplicateScopeType(error.errorType)
    }

    private fun formatFilterSyntaxError(errorType: ContextError.FilterSyntaxErrorType): String = when (errorType) {
        is ContextError.FilterSyntaxErrorType.EmptyQuery -> "Empty query"
        is ContextError.FilterSyntaxErrorType.EmptyExpression -> "Empty expression"
        is ContextError.FilterSyntaxErrorType.UnexpectedCharacter ->
            "Unexpected character '${errorType.char}' at position ${errorType.position}"
        is ContextError.FilterSyntaxErrorType.UnterminatedString ->
            "Unterminated string at position ${errorType.position}"
        is ContextError.FilterSyntaxErrorType.UnexpectedToken ->
            "Unexpected token at position ${errorType.position}"
        is ContextError.FilterSyntaxErrorType.MissingClosingParen ->
            "Missing closing parenthesis at position ${errorType.position}"
        is ContextError.FilterSyntaxErrorType.ExpectedExpression ->
            "Expected expression at position ${errorType.position}"
        is ContextError.FilterSyntaxErrorType.ExpectedIdentifier ->
            "Expected identifier at position ${errorType.position}"
        is ContextError.FilterSyntaxErrorType.ExpectedOperator ->
            "Expected operator at position ${errorType.position}"
        is ContextError.FilterSyntaxErrorType.ExpectedValue ->
            "Expected value at position ${errorType.position}"
        is ContextError.FilterSyntaxErrorType.UnbalancedParentheses -> "Unbalanced parentheses"
        is ContextError.FilterSyntaxErrorType.UnbalancedQuotes -> "Unbalanced quotes"
        is ContextError.FilterSyntaxErrorType.EmptyOperator -> "Empty operator"
        is ContextError.FilterSyntaxErrorType.InvalidSyntax -> "Invalid syntax"
    }

    private fun handleAspectValidationError(error: AspectValidationError): String = when (error) {
        is AspectValidationError.EmptyAspectKey -> MessageTemplates.cannotBeEmpty("Aspect key")
        is AspectValidationError.AspectKeyTooShort -> "Aspect key is too short"
        is AspectValidationError.AspectKeyTooLong -> "Aspect key is too long: max ${error.maxLength}, actual ${error.actualLength}"
        is AspectValidationError.InvalidAspectKeyFormat -> "Invalid aspect key format"
        is AspectValidationError.EmptyAspectValue -> MessageTemplates.cannotBeEmpty("Aspect value")
        is AspectValidationError.AspectValueTooShort -> "Aspect value is too short"
        is AspectValidationError.AspectValueTooLong -> "Aspect value is too long: max ${error.maxLength}, actual ${error.actualLength}"
        is AspectValidationError.EmptyAspectAllowedValues -> "Aspect allowed values cannot be empty"
        is AspectValidationError.DuplicateAspectAllowedValues -> "Aspect allowed values contain duplicates"
    }

    private fun handleAspectKeyError(error: AspectKeyError): String = when (error) {
        is AspectKeyError.EmptyKey -> MessageTemplates.cannotBeEmpty("Aspect key")
        is AspectKeyError.TooShort -> "Aspect key is too short: length ${error.actualLength}, minimum ${error.minLength} characters"
        is AspectKeyError.TooLong -> "Aspect key is too long: length ${error.actualLength}, maximum ${error.maxLength} characters"
        is AspectKeyError.InvalidFormat -> "Invalid aspect key format"
    }

    private fun handleAspectValueError(error: AspectValueError): String = when (error) {
        is AspectValueError.EmptyValue -> MessageTemplates.cannotBeEmpty("Aspect value")
        is AspectValueError.TooLong -> "Aspect value is too long: length ${error.actualLength}, maximum ${error.maxLength} characters"
    }

    private fun handleHierarchyPolicyError(error: HierarchyPolicyError): String = when (error) {
        is HierarchyPolicyError.InvalidMaxDepth -> "Invalid maximum depth: ${error.attemptedValue}"
        is HierarchyPolicyError.InvalidMaxChildrenPerScope -> "Invalid maximum children per scope: ${error.attemptedValue}"
    }

    private fun handleScopeAliasError(error: ScopeAliasError): String = when (error) {
        is ScopeAliasError.AliasNotFoundByName -> "Alias not found: ${error.alias}"
        is ScopeAliasError.AliasNotFoundById -> "Alias not found by ID: ${error.aliasId.value}"
        is ScopeAliasError.DuplicateAlias -> "Duplicate alias '${error.alias}': already assigned to scope ${error.scopeId.value}"
        is ScopeAliasError.CannotRemoveCanonicalAlias -> "Cannot remove canonical alias '${error.alias}' from scope ${error.scopeId.value}"
        is ScopeAliasError.AliasGenerationFailed -> "Failed to generate alias for scope ${error.scopeId.value}: ${error.reason}"
        is ScopeAliasError.AliasError -> "Alias error for '${error.alias}': ${error.reason}"
        is ScopeAliasError.DataInconsistencyError.AliasReferencesNonExistentScope ->
            "Data inconsistency: alias with ID '${error.aliasId.value}' references non-existent scope ${error.scopeId.value}"
    }

    private fun handleUserPreferencesIntegrationError(error: UserPreferencesIntegrationError): String = when (error) {
        is UserPreferencesIntegrationError.ServiceUnavailable ->
            "User preferences service unavailable${formatRetryAfter(error.retryAfter)}"
        is UserPreferencesIntegrationError.HierarchySettingsNotFound -> "Hierarchy settings not found in user preferences"
        is UserPreferencesIntegrationError.InvalidHierarchySettings -> "Invalid hierarchy settings: ${error.validationErrors.joinToString(", ")}"
        is UserPreferencesIntegrationError.MalformedResponse -> "Malformed preferences response"
        is UserPreferencesIntegrationError.RequestTimeout -> "Preferences request timed out after ${error.timeoutDuration}"
    }

    private fun handleQueryParseError(error: QueryParseError): String = when (error) {
        is QueryParseError.EmptyQuery -> "Empty query"
        is QueryParseError.UnexpectedCharacter -> "Unexpected character '${error.char}' at position ${error.position}"
        is QueryParseError.UnterminatedString -> "Unterminated string at position ${error.position}"
        is QueryParseError.UnexpectedToken -> "Unexpected token at position ${error.position}"
        is QueryParseError.MissingClosingParen -> "Missing closing parenthesis at position ${error.position}"
        is QueryParseError.ExpectedExpression -> "Expected expression at position ${error.position}"
        is QueryParseError.ExpectedIdentifier -> "Expected identifier at position ${error.position}"
        is QueryParseError.ExpectedOperator -> "Expected operator at position ${error.position}"
        is QueryParseError.ExpectedValue -> "Expected value at position ${error.position}"
    }

    private fun handleValidationError(error: ValidationError): String = when (error) {
        is ValidationError.InvalidNumericValue -> "Invalid numeric value '${error.value.value}' for aspect '${error.aspectKey.value}'"
        is ValidationError.InvalidBooleanValue -> "Invalid boolean value '${error.value.value}' for aspect '${error.aspectKey.value}'"
        is ValidationError.ValueNotInAllowedList ->
            "Value '${error.value.value}' for aspect '${error.aspectKey.value}' is not in allowed list: " +
                error.allowedValues.joinToString { it.value }
        is ValidationError.MultipleValuesNotAllowed -> "Multiple values not allowed for aspect '${error.aspectKey.value}'"
        is ValidationError.RequiredAspectsMissing -> "Required aspects missing: ${error.missingKeys.joinToString { it.value }}"
        is ValidationError.InvalidDurationValue -> "Invalid duration value '${error.value.value}' for aspect '${error.aspectKey.value}'"
    }

    private fun handleInvalidOperation(error: ScopesError.InvalidOperation): String =
        "Invalid operation: ${error.operation} on ${error.entityType ?: "entity"}" +
            "${formatOptionalEntityId(error.entityId)}" +
            "${formatOptionalReason(error.reason)}"

    private fun handleAlreadyExists(error: ScopesError.AlreadyExists): String =
        "${error.entityType} with ${error.identifierType} '${error.identifier}' already exists"

    private fun handleNotFound(error: ScopesError.NotFound): String = when (error.identifierType) {
        "alias" -> "Scope not found by alias: ${error.identifier}"
        "id" -> "${error.entityType} with id '${error.identifier}' not found"
        else -> "${error.entityType} '${error.identifier}' not found"
    }

    private fun handleSystemError(error: ScopesError.SystemError): String = "System error: ${error.errorType.name.lowercase().replace('_', ' ')}" +
        " in ${error.service ?: "unknown service"}" +
        "${formatOperationContext(error.context)}"

    private fun handleValidationFailed(error: ScopesError.ValidationFailed): String {
        val constraintMessage = formatValidationConstraint(error.constraint)
        return "Validation failed for '${error.field}': $constraintMessage"
    }

    private fun formatValidationConstraint(constraint: ScopesError.ValidationConstraintType): String = when (constraint) {
        is ScopesError.ValidationConstraintType.InvalidType ->
            "Expected ${constraint.expectedType} but got ${constraint.actualType}"
        is ScopesError.ValidationConstraintType.NotInAllowedValues ->
            "Value must be one of: ${constraint.allowedValues.joinToString(", ")}"
        is ScopesError.ValidationConstraintType.InvalidFormat ->
            "Invalid format. Expected: ${constraint.expectedFormat}"
        is ScopesError.ValidationConstraintType.MissingRequired ->
            "Missing required fields: ${constraint.requiredFields.joinToString(", ")}"
        is ScopesError.ValidationConstraintType.MultipleValuesNotAllowed ->
            "Multiple values not allowed for field '${constraint.field}'"
        is ScopesError.ValidationConstraintType.InvalidValue ->
            constraint.reason
    }

    private fun handleConflict(error: ScopesError.Conflict): String = when (error.conflictType) {
        ScopesError.Conflict.ConflictType.ALREADY_IN_USE ->
            "${error.resourceType} '${error.resourceId}' is already in use"
        ScopesError.Conflict.ConflictType.HAS_DEPENDENCIES ->
            "${error.resourceType} '${error.resourceId}' has dependencies${formatUsageCount(error.details["usage_count"])}"
        else -> "${error.conflictType} conflict for ${error.resourceType} '${error.resourceId}'"
    }

    private fun handleConcurrencyError(error: ScopesError.ConcurrencyError): String =
        "Concurrent modification detected for ${error.aggregateType} '${error.aggregateId}'" +
            "${formatVersionInfo(error.expectedVersion, error.actualVersion)}"

    private fun handleRepositoryError(error: ScopesError.RepositoryError): String {
        val operation = error.operation.name.lowercase().replace('_', ' ')
        return "Repository error during $operation operation on ${error.entityType ?: "entity"}"
    }

    private fun handleScopeStatusTransitionError(error: ScopesError.ScopeStatusTransitionError): String =
        "Invalid status transition from ${error.from} to ${error.to}: ${error.reason}"

    // Contract error mapping
    fun getMessage(error: ScopeContractError): String = when (error) {
        is ScopeContractError.InputError -> handleContractInputError(error)
        is ScopeContractError.BusinessError -> handleContractBusinessError(error)
        is ScopeContractError.SystemError -> handleContractSystemError(error)
    }

    private fun handleContractInputError(error: ScopeContractError.InputError): String = when (error) {
        is ScopeContractError.InputError.InvalidId ->
            "Invalid ID format: ${error.id}${formatExpectedFormat(error.expectedFormat)}"
        is ScopeContractError.InputError.InvalidTitle -> formatTitleValidationFailure(error.validationFailure)
        is ScopeContractError.InputError.InvalidDescription -> formatDescriptionValidationFailure(error.validationFailure)
        is ScopeContractError.InputError.InvalidParentId ->
            "Invalid parent ID: ${error.parentId}${formatExpectedFormat(error.expectedFormat)}"
    }

    private fun formatTitleValidationFailure(failure: ScopeContractError.TitleValidationFailure): String = when (failure) {
        is ScopeContractError.TitleValidationFailure.Empty -> MessageTemplates.cannotBeEmpty("Title")
        is ScopeContractError.TitleValidationFailure.TooShort -> MessageTemplates.tooShort("Title", failure.minimumLength)
        is ScopeContractError.TitleValidationFailure.TooLong -> MessageTemplates.tooLong("Title", failure.maximumLength)
        is ScopeContractError.TitleValidationFailure.InvalidCharacters ->
            "Title contains invalid characters: ${failure.prohibitedCharacters.joinToString()}"
    }

    private fun formatDescriptionValidationFailure(failure: ScopeContractError.DescriptionValidationFailure): String = when (failure) {
        is ScopeContractError.DescriptionValidationFailure.TooLong -> MessageTemplates.tooLong("Description", failure.maximumLength)
    }

    private fun handleContractBusinessError(error: ScopeContractError.BusinessError): String = when (error) {
        is ScopeContractError.BusinessError.NotFound -> "Not found: ${error.scopeId}"
        is ScopeContractError.BusinessError.DuplicateTitle -> formatContractDuplicateTitle(error.title, error.parentId)
        is ScopeContractError.BusinessError.HierarchyViolation -> formatHierarchyViolation(error.violation)
        is ScopeContractError.BusinessError.AlreadyDeleted -> "Already deleted: ${error.scopeId}"
        is ScopeContractError.BusinessError.ArchivedScope -> "Cannot modify archived scope: ${error.scopeId}"
        is ScopeContractError.BusinessError.NotArchived -> "Scope is not archived: ${error.scopeId}"
        is ScopeContractError.BusinessError.HasChildren ->
            "Cannot delete scope with children: ${error.scopeId}${formatChildrenCount(error.childrenCount)}"
        is ScopeContractError.BusinessError.AliasNotFound -> "Alias not found: ${error.alias}"
        is ScopeContractError.BusinessError.DuplicateAlias -> MessageTemplates.alreadyExists("Alias", error.alias)
        is ScopeContractError.BusinessError.CannotRemoveCanonicalAlias -> "Cannot remove canonical alias: ${error.alias}"
    }

    private fun formatHierarchyViolation(violation: ScopeContractError.HierarchyViolationType): String = when (violation) {
        is ScopeContractError.HierarchyViolationType.CircularReference ->
            "Circular reference detected: ${violation.scopeId} -> ${violation.parentId}"
        is ScopeContractError.HierarchyViolationType.MaxDepthExceeded ->
            "Maximum depth exceeded: ${violation.attemptedDepth} (max: ${violation.maximumDepth})"
        is ScopeContractError.HierarchyViolationType.MaxChildrenExceeded ->
            "Maximum children exceeded for ${violation.parentId}: ${violation.currentChildrenCount} (max: ${violation.maximumChildren})"
        is ScopeContractError.HierarchyViolationType.SelfParenting ->
            "Cannot set scope ${violation.scopeId} as its own parent"
        is ScopeContractError.HierarchyViolationType.ParentNotFound ->
            "Parent ${violation.parentId} not found for scope ${violation.scopeId}"
    }

    private fun handleContractSystemError(error: ScopeContractError.SystemError): String = when (error) {
        is ScopeContractError.SystemError.ServiceUnavailable -> "Service unavailable: ${error.service}"
        is ScopeContractError.SystemError.Timeout -> "Operation timeout: ${error.operation} (${error.timeout})"
        is ScopeContractError.SystemError.ConcurrentModification ->
            "Concurrent modification detected for ${error.scopeId} (expected: ${error.expectedVersion}, actual: ${error.actualVersion})"
    }

    // Format helpers
    private fun formatDuplicateTitle(title: String, parentId: Any?): String = if (parentId != null) {
        "Scope with title '$title' already exists under parent"
    } else {
        "Root scope with title '$title' already exists"
    }

    private fun formatDuplicateTitleInContext(title: String, parentId: Any?): String = if (parentId != null) {
        "Scope with title '$title' already exists at parent"
    } else {
        "Scope with title '$title' already exists at root level"
    }

    private fun formatContractDuplicateTitle(title: String, parentId: String?): String =
        "Duplicate title '$title'${parentId?.let { " under parent $it" } ?: " at root level"}"

    private fun formatVersionMismatch(error: ScopeError.VersionMismatch): String =
        "Version mismatch for scope ${error.scopeId.value}: expected ${error.expectedVersion} but was ${error.actualVersion}"

    private fun formatOptionalScope(scopeId: Any?): String = scopeId?.let { " for scope $it" } ?: ""

    private fun formatOptionalContext(contextId: String?): String = contextId?.let { " in context $it" } ?: ""

    private fun formatOptionalEntityId(entityId: String?): String = entityId?.let { " with id '$it'" } ?: ""

    private fun formatOptionalReason(reason: ScopesError.InvalidOperation.InvalidOperationReason?): String =
        reason?.let { " (${it.name.lowercase().replace('_', ' ')})" } ?: ""

    private fun formatRetryAfter(retryAfter: Any?): String = retryAfter?.let { ", retry after: $it" } ?: ""

    // Cause messages are presentation concerns and should not be propagated from lower layers

    private fun formatOperationContext(context: Map<String, Any>?): String = context?.get("operation")?.let { " during $it" } ?: ""

    private fun formatUsageCount(usageCount: Any?): String = usageCount?.let { " ($it usages)" } ?: ""

    private fun formatVersionInfo(expectedVersion: Int?, actualVersion: Int?): String =
        expectedVersion?.let { " (expected: $it, actual: $actualVersion)" } ?: ""

    private fun formatExpectedFormat(expectedFormat: String?): String = expectedFormat?.let { " (expected: $it)" } ?: ""

    private fun formatChildrenCount(childrenCount: Int?): String = childrenCount?.let { " ($it children)" } ?: ""
}

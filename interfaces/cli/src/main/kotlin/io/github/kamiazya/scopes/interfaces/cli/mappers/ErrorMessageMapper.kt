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

    fun getMessage(error: ScopesError): String = when (error) {
        is ScopeError -> when (error) {
            is ScopeError.NotFound -> "Scope not found: ${error.scopeId.value}"
            is ScopeError.ParentNotFound -> "Parent scope not found: ${error.parentId.value}"
            is ScopeError.DuplicateTitle -> if (error.parentId != null) {
                "Scope with title '${error.title}' already exists under parent"
            } else {
                "Root scope with title '${error.title}' already exists"
            }
            is ScopeError.AlreadyDeleted -> "Scope is already deleted: ${error.scopeId.value}"
            is ScopeError.AlreadyArchived -> "Scope is already archived: ${error.scopeId.value}"
            is ScopeError.NotArchived -> "Scope is not archived: ${error.scopeId.value}"
            is ScopeError.VersionMismatch ->
                "Version mismatch for scope ${error.scopeId.value}: " +
                    "expected ${error.expectedVersion} but was ${error.actualVersion}"
        }
        is ScopeInputError -> when (error) {
            is ScopeInputError.IdError.EmptyId -> "Scope ID cannot be empty"
            is ScopeInputError.IdError.InvalidIdFormat -> "Invalid scope ID format: ${error.id}"
            is ScopeInputError.TitleError.EmptyTitle -> "Scope title cannot be empty"
            is ScopeInputError.TitleError.TitleTooShort -> "Scope title is too short: minimum ${error.minLength} characters"
            is ScopeInputError.TitleError.TitleTooLong -> "Scope title is too long: maximum ${error.maxLength} characters"
            is ScopeInputError.TitleError.InvalidTitleFormat -> "Invalid scope title format: ${error.title}"
            is ScopeInputError.DescriptionError.DescriptionTooLong -> "Scope description is too long: maximum ${error.maxLength} characters"
            is ScopeInputError.AliasError.EmptyAlias -> "Scope alias cannot be empty"
            is ScopeInputError.AliasError.AliasTooShort -> "Scope alias is too short: minimum ${error.minLength} characters"
            is ScopeInputError.AliasError.AliasTooLong -> "Scope alias is too long: maximum ${error.maxLength} characters"
            is ScopeInputError.AliasError.InvalidAliasFormat -> "Invalid scope alias format: expected ${presentAliasPattern(error.expectedPattern)}"
        }
        is ScopeNotFoundError -> "Scope not found: ${error.scopeId.value}"
        is ScopeUniquenessError -> when (error) {
            is ScopeUniquenessError.DuplicateTitleInContext -> if (error.parentId != null) {
                "Scope with title '${error.title}' already exists at parent"
            } else {
                "Scope with title '${error.title}' already exists at root level"
            }
            is ScopeUniquenessError.DuplicateIdentifier -> "Duplicate identifier: ${error.identifier} (type: ${error.identifierType})"
        }
        is ScopeHierarchyError -> when (error) {
            is ScopeHierarchyError.CircularDependency -> "Circular dependency detected for scope ${error.scopeId.value} with ancestor ${error.ancestorId.value}"
            is ScopeHierarchyError.MaxDepthExceeded ->
                "Maximum hierarchy depth exceeded for scope ${error.scopeId.value}: " +
                    "current depth ${error.currentDepth}, maximum ${error.maxDepth}"
            is ScopeHierarchyError.MaxChildrenExceeded ->
                "Maximum children exceeded for parent ${error.parentId.value}: " +
                    "current ${error.currentCount}, maximum ${error.maxChildren}"
            is ScopeHierarchyError.HierarchyUnavailable ->
                "Hierarchy operation '${error.operation}' failed${error.scopeId?.let { " for scope ${it.value}" } ?: ""}: ${error.reason}"
        }
        is PersistenceError -> when (error) {
            is PersistenceError.ConcurrencyConflict ->
                "Concurrency conflict for ${error.entityType} ${error.entityId}: " +
                    "expected version ${error.expectedVersion}, actual ${error.actualVersion}"
        }
        is AggregateIdError -> when (error) {
            is AggregateIdError.InvalidFormat -> "Invalid aggregate ID format '${error.value}': ${error.formatError.name.lowercase().replace('_', ' ')}"
        }
        is ContextError -> when (error) {
            is ContextError.EmptyKey -> "Context view key cannot be empty"
            is ContextError.KeyTooShort -> "Context view key must be at least ${error.minimumLength} characters"
            is ContextError.KeyTooLong -> "Context view key must be at most ${error.maximumLength} characters"
            is ContextError.InvalidKeyFormat -> "Invalid context view key format: ${presentInvalidKeyFormatType(error.errorType)}"
            is ContextError.EmptyName -> "Context view name cannot be empty"
            is ContextError.NameTooLong -> "Context view name is too long: maximum ${error.maximumLength} characters"
            is ContextError.EmptyDescription -> "Context view description cannot be empty"
            is ContextError.DescriptionTooShort -> "Context view description must be at least ${error.minimumLength} characters"
            is ContextError.DescriptionTooLong -> "Context view description must be at most ${error.maximumLength} characters"
            is ContextError.EmptyFilter -> "Context view filter cannot be empty"
            is ContextError.FilterTooShort -> "Context view filter must be at least ${error.minimumLength} characters"
            is ContextError.FilterTooLong -> "Context view filter must be at most ${error.maximumLength} characters"
            is ContextError.InvalidFilterSyntax ->
                "Invalid filter syntax: ${
                    when (val errorType = error.errorType) {
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
                        is ContextError.FilterSyntaxErrorType.UnbalancedParentheses ->
                            "Unbalanced parentheses"
                        is ContextError.FilterSyntaxErrorType.UnbalancedQuotes ->
                            "Unbalanced quotes"
                        is ContextError.FilterSyntaxErrorType.EmptyOperator ->
                            "Empty operator"
                        is ContextError.FilterSyntaxErrorType.InvalidSyntax ->
                            "Invalid syntax"
                    }
                }"
            is ContextError.InvalidScope -> "Invalid scope '${error.scopeId}': ${presentInvalidScopeType(error.errorType)}"
            is ContextError.InvalidHierarchy -> "Invalid hierarchy for scope '${error.scopeId}' with parent '${error.parentId}': ${presentInvalidHierarchyType(
                error.errorType,
            )}"
            is ContextError.DuplicateScope -> "Duplicate scope title '${error.title}'${error.contextId?.let {
                " in context $it"
            } ?: ""}: ${presentDuplicateScopeType(error.errorType)}"
        }
        is AspectValidationError -> when (error) {
            is AspectValidationError.EmptyAspectKey -> "Aspect key cannot be empty"
            is AspectValidationError.AspectKeyTooShort -> "Aspect key is too short"
            is AspectValidationError.AspectKeyTooLong -> "Aspect key is too long: max ${error.maxLength}, actual ${error.actualLength}"
            is AspectValidationError.InvalidAspectKeyFormat -> "Invalid aspect key format"
            is AspectValidationError.EmptyAspectValue -> "Aspect value cannot be empty"
            is AspectValidationError.AspectValueTooShort -> "Aspect value is too short"
            is AspectValidationError.AspectValueTooLong -> "Aspect value is too long: max ${error.maxLength}, actual ${error.actualLength}"
            is AspectValidationError.EmptyAspectAllowedValues -> "Aspect allowed values cannot be empty"
            is AspectValidationError.DuplicateAspectAllowedValues -> "Aspect allowed values contain duplicates"
        }
        is AspectKeyError -> when (error) {
            is AspectKeyError.EmptyKey -> "Aspect key cannot be empty"
            is AspectKeyError.TooShort -> "Aspect key is too short: length ${error.actualLength}, minimum ${error.minLength} characters"
            is AspectKeyError.TooLong -> "Aspect key is too long: length ${error.actualLength}, maximum ${error.maxLength} characters"
            is AspectKeyError.InvalidFormat -> "Invalid aspect key format"
        }
        is AspectValueError -> when (error) {
            is AspectValueError.EmptyValue -> "Aspect value cannot be empty"
            is AspectValueError.TooLong -> "Aspect value is too long: length ${error.actualLength}, maximum ${error.maxLength} characters"
        }
        is HierarchyPolicyError -> when (error) {
            is HierarchyPolicyError.InvalidMaxDepth -> "Invalid maximum depth: ${error.attemptedValue}"
            is HierarchyPolicyError.InvalidMaxChildrenPerScope -> "Invalid maximum children per scope: ${error.attemptedValue}"
        }
        is ScopeAliasError -> when (error) {
            is ScopeAliasError.AliasNotFoundByName -> "Alias not found: ${error.alias}"
            is ScopeAliasError.AliasNotFoundById -> "Alias not found by ID: ${error.aliasId.value}"
            is ScopeAliasError.DuplicateAlias -> "Duplicate alias '${error.alias}': already assigned to scope ${error.scopeId.value}"
            is ScopeAliasError.CannotRemoveCanonicalAlias -> "Cannot remove canonical alias '${error.alias}' from scope ${error.scopeId.value}"
            is ScopeAliasError.AliasGenerationFailed -> "Failed to generate alias for scope ${error.scopeId.value}: ${error.reason}"
            is ScopeAliasError.AliasError -> "Alias error for '${error.alias}': ${error.reason}"
            is ScopeAliasError.DataInconsistencyError.AliasReferencesNonExistentScope ->
                "Data inconsistency: alias with ID '${error.aliasId.value}' references non-existent scope ${error.scopeId.value}"
        }
        is UserPreferencesIntegrationError -> when (error) {
            is UserPreferencesIntegrationError.ServiceUnavailable -> "User preferences service unavailable${error.retryAfter?.let {
                ", retry after: $it"
            } ?: ""}"
            is UserPreferencesIntegrationError.HierarchySettingsNotFound -> "Hierarchy settings not found in user preferences"
            is UserPreferencesIntegrationError.InvalidHierarchySettings -> "Invalid hierarchy settings: ${error.validationErrors.joinToString(", ")}"
            is UserPreferencesIntegrationError.MalformedResponse -> "Malformed preferences response${error.cause?.let { ": ${it.message}" } ?: ""}"
            is UserPreferencesIntegrationError.RequestTimeout -> "Preferences request timed out after ${error.timeoutDuration}"
        }
        is QueryParseError -> when (error) {
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
        is ScopesError.InvalidOperation -> "Invalid operation: ${error.operation} on ${error.entityType ?: "entity"}${error.entityId?.let {
            " with id '$it'"
        } ?: ""}${error.reason?.let { " (${it.name.lowercase().replace('_', ' ')})" } ?: ""}"
        is ScopesError.AlreadyExists -> "${error.entityType} with ${error.identifierType} '${error.identifier}' already exists"
        is ScopesError.NotFound -> when (error.identifierType) {
            "alias" -> "Scope not found by alias: ${error.identifier}"
            "id" -> "${error.entityType} with id '${error.identifier}' not found"
            else -> "${error.entityType} '${error.identifier}' not found"
        }
        is ScopesError.SystemError -> "System error: ${error.errorType.name.lowercase().replace(
            '_',
            ' ',
        )} in ${error.service ?: "unknown service"}${error.context?.get("operation")?.let {
            " during $it"
        } ?: ""}"
        is ScopesError.ValidationFailed -> {
            val constraintMessage = when (val constraint = error.constraint) {
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
            "Validation failed for '${error.field}': $constraintMessage"
        }
        is ScopesError.Conflict -> when (error.conflictType) {
            ScopesError.Conflict.ConflictType.ALREADY_IN_USE ->
                "${error.resourceType} '${error.resourceId}' is already in use"
            ScopesError.Conflict.ConflictType.HAS_DEPENDENCIES ->
                "${error.resourceType} '${error.resourceId}' has dependencies${error.details["usage_count"]?.let { " ($it usages)" } ?: ""}"
            else -> "${error.conflictType} conflict for ${error.resourceType} '${error.resourceId}'"
        }
        is ScopesError.ConcurrencyError -> "Concurrent modification detected for ${error.aggregateType} '${error.aggregateId}'${error.expectedVersion?.let {
            " (expected: $it, actual: ${error.actualVersion})"
        } ?: ""}"
        is ScopesError.RepositoryError -> {
            val operation = error.operation.name.lowercase().replace('_', ' ')
            "Repository error during $operation operation on ${error.entityType ?: "entity"}"
        }
        is ValidationError -> when (error) {
            is ValidationError.InvalidNumericValue -> "Invalid numeric value '${error.value.value}' for aspect '${error.aspectKey.value}'"
            is ValidationError.InvalidBooleanValue -> "Invalid boolean value '${error.value.value}' for aspect '${error.aspectKey.value}'"
            is ValidationError.ValueNotInAllowedList ->
                "Value '${error.value.value}' for aspect '${error.aspectKey.value}' is not in allowed list: " +
                    error.allowedValues.joinToString { it.value }
            is ValidationError.MultipleValuesNotAllowed -> "Multiple values not allowed for aspect '${error.aspectKey.value}'"
            is ValidationError.RequiredAspectsMissing -> "Required aspects missing: ${error.missingKeys.joinToString { it.value }}"
            is ValidationError.InvalidDurationValue -> "Invalid duration value '${error.value.value}' for aspect '${error.aspectKey.value}'"
        }
        is ScopesError.ScopeStatusTransitionError -> "Invalid status transition from ${error.from} to ${error.to}: ${error.reason}"
    }

    /**
     * Maps contract errors to user-friendly messages.
     */
    fun getMessage(error: ScopeContractError): String = when (error) {
        is ScopeContractError.InputError -> when (error) {
            is ScopeContractError.InputError.InvalidId ->
                "Invalid ID format: ${error.id}${error.expectedFormat?.let { " (expected: $it)" } ?: ""}"
            is ScopeContractError.InputError.InvalidTitle -> when (val failure = error.validationFailure) {
                is ScopeContractError.TitleValidationFailure.Empty -> "Title cannot be empty"
                is ScopeContractError.TitleValidationFailure.TooShort ->
                    "Title too short: minimum ${failure.minimumLength} characters"
                is ScopeContractError.TitleValidationFailure.TooLong ->
                    "Title too long: maximum ${failure.maximumLength} characters"
                is ScopeContractError.TitleValidationFailure.InvalidCharacters ->
                    "Title contains invalid characters: ${failure.prohibitedCharacters.joinToString()}"
            }
            is ScopeContractError.InputError.InvalidDescription -> when (val failure = error.validationFailure) {
                is ScopeContractError.DescriptionValidationFailure.TooLong ->
                    "Description too long: maximum ${failure.maximumLength} characters"
            }
            is ScopeContractError.InputError.InvalidParentId ->
                "Invalid parent ID: ${error.parentId}${error.expectedFormat?.let { " (expected: $it)" } ?: ""}"
        }
        is ScopeContractError.BusinessError -> when (error) {
            is ScopeContractError.BusinessError.NotFound -> "Not found: ${error.scopeId}"
            is ScopeContractError.BusinessError.DuplicateTitle ->
                "Duplicate title '${error.title}'${error.parentId?.let { " under parent $it" } ?: " at root level"}"
            is ScopeContractError.BusinessError.HierarchyViolation -> when (val violation = error.violation) {
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
            is ScopeContractError.BusinessError.AlreadyDeleted -> "Already deleted: ${error.scopeId}"
            is ScopeContractError.BusinessError.ArchivedScope -> "Cannot modify archived scope: ${error.scopeId}"
            is ScopeContractError.BusinessError.NotArchived -> "Scope is not archived: ${error.scopeId}"
            is ScopeContractError.BusinessError.HasChildren ->
                "Cannot delete scope with children: ${error.scopeId}${error.childrenCount?.let { " ($it children)" } ?: ""}"
            is ScopeContractError.BusinessError.AliasNotFound -> "Alias not found: ${error.alias}"
            is ScopeContractError.BusinessError.DuplicateAlias -> "Alias already exists: ${error.alias}"
            is ScopeContractError.BusinessError.CannotRemoveCanonicalAlias ->
                "Cannot remove canonical alias: ${error.alias}"
        }
        is ScopeContractError.SystemError -> when (error) {
            is ScopeContractError.SystemError.ServiceUnavailable ->
                "Service unavailable: ${error.service}"
            is ScopeContractError.SystemError.Timeout ->
                "Operation timeout: ${error.operation} (${error.timeout})"
            is ScopeContractError.SystemError.ConcurrentModification ->
                "Concurrent modification detected for ${error.scopeId} (expected: ${error.expectedVersion}, actual: ${error.actualVersion})"
        }
    }
}

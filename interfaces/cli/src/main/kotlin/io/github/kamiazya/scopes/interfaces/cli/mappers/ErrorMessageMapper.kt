package io.github.kamiazya.scopes.interfaces.cli.mappers

import io.github.kamiazya.scopes.scopemanagement.domain.error.AggregateIdError
import io.github.kamiazya.scopes.scopemanagement.domain.error.AggregateVersionError
import io.github.kamiazya.scopes.scopemanagement.domain.error.AspectKeyError
import io.github.kamiazya.scopes.scopemanagement.domain.error.AspectValidationError
import io.github.kamiazya.scopes.scopemanagement.domain.error.AspectValueError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ContextError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ContextManagementError
import io.github.kamiazya.scopes.scopemanagement.domain.error.EventIdError
import io.github.kamiazya.scopes.scopemanagement.domain.error.HierarchyPolicyError
import io.github.kamiazya.scopes.scopemanagement.domain.error.PersistenceError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeAliasError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeHierarchyError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeNotFoundError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeUniquenessError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.domain.error.UserPreferencesIntegrationError

/**
 * Maps domain errors to user-friendly messages for CLI output.
 */
object ErrorMessageMapper {
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
            is ScopeInputError.IdError.Blank -> "Scope ID cannot be blank: '${error.attemptedValue}'"
            is ScopeInputError.IdError.InvalidFormat -> "Invalid scope ID format: ${error.attemptedValue}"
            is ScopeInputError.TitleError.Empty -> "Scope title cannot be empty"
            is ScopeInputError.TitleError.TooShort -> "Scope title is too short: minimum ${error.minimumLength} characters"
            is ScopeInputError.TitleError.TooLong -> "Scope title is too long: maximum ${error.maximumLength} characters"
            is ScopeInputError.TitleError.ContainsProhibitedCharacters ->
                "Scope title contains prohibited characters: ${error.prohibitedCharacters.joinToString()}"
            is ScopeInputError.DescriptionError.TooLong -> "Scope description is too long: maximum ${error.maximumLength} characters"
            is ScopeInputError.AliasError.Empty -> "Scope alias cannot be empty"
            is ScopeInputError.AliasError.TooShort -> "Scope alias is too short: minimum ${error.minimumLength} characters"
            is ScopeInputError.AliasError.TooLong -> "Scope alias is too long: maximum ${error.maximumLength} characters"
            is ScopeInputError.AliasError.InvalidFormat -> "Invalid scope alias format: expected ${error.expectedPattern}"
        }
        is ScopeNotFoundError -> "Scope not found: ${error.scopeId.value}"
        is ScopeUniquenessError -> when (error) {
            is ScopeUniquenessError.DuplicateTitle -> if (error.parentScopeId != null) {
                "Scope with title '${error.title}' already exists at parent"
            } else {
                "Scope with title '${error.title}' already exists at root level"
            }
            is ScopeUniquenessError.DuplicateIdentifier -> "Duplicate identifier: ${error.identifier}"
        }
        is ScopeHierarchyError -> when (error) {
            is ScopeHierarchyError.CircularReference -> "Circular reference detected for scope ${error.scopeId.value} with parent ${error.parentId.value}"
            is ScopeHierarchyError.CircularPath -> "Circular path detected for scope ${error.scopeId.value}"
            is ScopeHierarchyError.MaxDepthExceeded -> "Maximum hierarchy depth exceeded: attempted ${error.attemptedDepth}, maximum ${error.maximumDepth}"
            is ScopeHierarchyError.HasChildren -> "Cannot perform operation: scope ${error.scopeId.value} has children"
            is ScopeHierarchyError.InvalidParentId -> "Invalid parent ID: ${error.invalidId}"
            is ScopeHierarchyError.MaxChildrenExceeded ->
                "Maximum children exceeded for parent ${error.parentScopeId.value}: " +
                    "current ${error.currentChildrenCount}, maximum ${error.maximumChildren}"
            is ScopeHierarchyError.ParentNotFound -> "Parent ${error.parentId.value} not found for scope ${error.scopeId.value}"
            is ScopeHierarchyError.ScopeInHierarchyNotFound -> "Scope ${error.scopeId.value} not found in hierarchy"
            is ScopeHierarchyError.SelfParenting -> "Scope ${error.scopeId.value} cannot be its own parent"
            is ScopeHierarchyError.HierarchyUnavailable ->
                "Hierarchy operation '${error.operation}' failed${error.scopeId?.let { " for scope ${it.value}" } ?: ""}: ${error.reason}"
        }
        is PersistenceError -> when (error) {
            is PersistenceError.StorageUnavailable -> "Storage unavailable for operation '${error.operation}'"
            is PersistenceError.DataCorruption -> "Data corruption detected for ${error.entityType}${error.entityId?.let { " $it" } ?: ""}: ${error.reason}"
            is PersistenceError.ConcurrencyConflict ->
                "Concurrency conflict for ${error.entityType} ${error.entityId}: " +
                    "expected version ${error.expectedVersion}, actual ${error.actualVersion}"
            is PersistenceError.NotFound -> "${error.entityType} not found${error.entityId?.let { ": $it" } ?: ""}"
        }
        is AggregateIdError -> when (error) {
            is AggregateIdError.InvalidType -> "Invalid aggregate type '${error.attemptedType}', valid types: ${error.validTypes.joinToString()}"
            is AggregateIdError.InvalidIdFormat -> "Invalid aggregate ID format '${error.attemptedId}', expected: ${error.expectedFormat}"
            is AggregateIdError.InvalidUriFormat -> "Invalid URI format '${error.attemptedUri}': ${error.reason}"
            is AggregateIdError.EmptyValue -> "Empty value for field: ${error.field}"
            is AggregateIdError.InvalidFormat -> "Invalid format for ${error.value}: ${error.message}"
        }
        is AggregateVersionError -> when (error) {
            is AggregateVersionError.NegativeVersion -> "Negative version not allowed: ${error.attemptedVersion}"
            is AggregateVersionError.VersionOverflow -> "Version overflow: current ${error.currentVersion}, max ${error.maxVersion}"
            is AggregateVersionError.InvalidVersionTransition -> "Invalid version transition: from ${error.currentVersion} to ${error.attemptedVersion}"
        }
        is EventIdError -> when (error) {
            is EventIdError.EmptyValue -> "Empty value for field: ${error.field}"
            is EventIdError.InvalidEventType -> "Invalid event type '${error.attemptedType}': ${error.reason}"
            is EventIdError.InvalidUriFormat -> "Invalid URI format '${error.attemptedUri}': ${error.reason}"
            is EventIdError.UlidError -> "ULID error: ${error.reason}"
        }
        is ContextError -> when (error) {
            is ContextError.BlankId -> "Context view ID cannot be blank: '${error.attemptedValue}'"
            is ContextError.InvalidIdFormat -> "Invalid context view ID format: ${error.attemptedValue}, expected: ${error.expectedFormat}"
            is ContextError.EmptyName -> "Context view name cannot be empty: '${error.attemptedValue}'"
            is ContextError.InvalidNameFormat -> "Invalid context view name format: ${error.attemptedValue}, expected: ${error.expectedPattern}"
            is ContextError.NameTooLong -> "Context view name is too long: maximum ${error.maximumLength} characters"
            is ContextError.DuplicateName -> "Context view with name '${error.attemptedName}' already exists"
            is ContextError.ContextNotFound -> "Context view not found${error.contextId?.let {
                ": $it"
            } ?: ""}${error.contextName?.let { " (name: $it)" } ?: ""}"
            is ContextError.InvalidFilter -> "Invalid filter '${error.filter}': ${error.reason}"
            is ContextError.EmptyKey -> "Context view key cannot be empty"
            is ContextError.KeyTooShort -> "Context view key must be at least ${error.minimumLength} characters"
            is ContextError.KeyTooLong -> "Context view key must be at most ${error.maximumLength} characters"
            is ContextError.InvalidKeyFormat -> "Invalid context view key format: ${error.reason}"
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
            // New domain validation error cases
            is ContextError.InvalidScope -> "Invalid scope '${error.scopeId}': ${error.reason}"
            is ContextError.InvalidHierarchy -> "Invalid hierarchy for scope '${error.scopeId}' with parent '${error.parentId}': ${error.reason}"
            is ContextError.DuplicateScope -> "Duplicate scope title '${error.title}'${error.contextId?.let { " in context $it" } ?: ""}: ${error.reason}"
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
            is AspectKeyError.TooShort -> "Aspect key '${error.value}' is too short: minimum ${error.minLength} characters"
            is AspectKeyError.TooLong -> "Aspect key '${error.value}' is too long: maximum ${error.maxLength} characters"
            is AspectKeyError.InvalidFormat -> "Invalid aspect key format: ${error.value}"
        }
        is AspectValueError -> when (error) {
            is AspectValueError.EmptyValue -> "Aspect value cannot be empty"
            is AspectValueError.TooLong -> "Aspect value '${error.value}' is too long: maximum ${error.maxLength} characters"
        }
        is HierarchyPolicyError -> when (error) {
            is HierarchyPolicyError.InvalidMaxDepth -> "Invalid maximum depth: ${error.attemptedValue}, minimum allowed: ${error.minimumAllowed}"
            is HierarchyPolicyError.InvalidMaxChildrenPerScope ->
                "Invalid maximum children per scope: ${error.attemptedValue}, minimum allowed: ${error.minimumAllowed}"
        }
        is ScopeAliasError -> when (error) {
            is ScopeAliasError.AliasNotFound -> "Alias not found: ${error.aliasName}"
            is ScopeAliasError.AliasNotFoundById -> "Alias not found by ID: ${error.aliasId.value}"
            is ScopeAliasError.DuplicateAlias -> "Duplicate alias '${error.aliasName}': already assigned to scope ${error.existingScopeId.value}"
            is ScopeAliasError.CannotRemoveCanonicalAlias -> "Cannot remove canonical alias '${error.aliasName}' from scope ${error.scopeId.value}"
            is ScopeAliasError.AliasGenerationFailed -> "Failed to generate alias for scope ${error.scopeId.value} after ${error.retryCount} attempts"
            is ScopeAliasError.AliasGenerationValidationFailed -> "Alias generation validation failed for scope ${error.scopeId.value}: ${error.reason}"
            is ScopeAliasError.DataInconsistencyError.AliasExistsButScopeNotFound ->
                "Data inconsistency: alias '${error.aliasName}' references non-existent scope ${error.scopeId.value}"
        }
        is UserPreferencesIntegrationError -> when (error) {
            is UserPreferencesIntegrationError.PreferencesServiceUnavailable -> "User preferences service unavailable${error.retryAfter?.let {
                ", retry after: $it"
            } ?: ""}"
            is UserPreferencesIntegrationError.HierarchySettingsNotFound -> "Hierarchy settings not found in user preferences"
            is UserPreferencesIntegrationError.InvalidHierarchySettings -> "Invalid hierarchy settings: ${error.validationErrors.joinToString(", ")}"
            is UserPreferencesIntegrationError.MalformedPreferencesResponse -> "Malformed preferences response: expected ${error.expectedFormat}"
            is UserPreferencesIntegrationError.PreferencesRequestTimeout -> "Preferences request timed out after ${error.timeout}"
        }
        is ContextManagementError -> "Context management error"
        is ScopesError.InvalidOperation -> "Invalid operation: ${error.message}"
        is ScopesError.AlreadyExists -> "Already exists: ${error.message}"
        is ScopesError.NotFound -> "Not found: ${error.message}"
        is ScopesError.SystemError -> "System error: ${error.message}"
        is ScopesError.ValidationFailed -> "Validation failed: ${error.message}"
        is ScopesError.Conflict -> "Conflict: ${error.message}"
        is ScopesError.ConcurrencyError -> "Concurrency error: ${error.message}${error.expectedVersion?.let {
            " (expected: $it, actual: ${error.actualVersion})"
        } ?: ""}"
        is ScopesError.RepositoryError -> "Repository error: ${error.message}${error.operation?.let { " during $it" } ?: ""}"
    }
}

package io.github.kamiazya.scopes.scopemanagement.application.error
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError as DomainScopeInputError

/**
 * Service for mapping domain input validation errors to application layer errors.
 * Centralizes error mapping logic to avoid duplication across command handlers.
 */
class ScopeInputErrorMappingService {

    private val errorPresenter = ScopeInputErrorPresenter()

    fun mapIdError(error: DomainScopeInputError.IdError, id: String): ScopeInputError = when (error) {
        is DomainScopeInputError.IdError.EmptyId ->
            ScopeInputError.IdBlank(id)
        is DomainScopeInputError.IdError.InvalidIdFormat ->
            ScopeInputError.IdInvalidFormat(id, errorPresenter.presentIdFormat(error.expectedFormat))
    }

    fun mapTitleError(error: DomainScopeInputError.TitleError, title: String): ScopeInputError = when (error) {
        is DomainScopeInputError.TitleError.EmptyTitle ->
            ScopeInputError.TitleEmpty(title)
        is DomainScopeInputError.TitleError.TitleTooShort ->
            ScopeInputError.TitleTooShort(title, error.minLength)
        is DomainScopeInputError.TitleError.TitleTooLong ->
            ScopeInputError.TitleTooLong(title, error.maxLength)
        is DomainScopeInputError.TitleError.InvalidTitleFormat ->
            ScopeInputError.TitleContainsProhibitedCharacters(title, listOf('<', '>', '&', '"'))
    }

    fun mapAliasError(error: DomainScopeInputError.AliasError, alias: String): ScopeInputError = when (error) {
        is DomainScopeInputError.AliasError.EmptyAlias ->
            ScopeInputError.AliasEmpty(alias)
        is DomainScopeInputError.AliasError.AliasTooShort ->
            ScopeInputError.AliasTooShort(alias, error.minLength)
        is DomainScopeInputError.AliasError.AliasTooLong ->
            ScopeInputError.AliasTooLong(alias, error.maxLength)
        is DomainScopeInputError.AliasError.InvalidAliasFormat ->
            ScopeInputError.AliasInvalidFormat(alias, errorPresenter.presentAliasPattern(error.expectedPattern))
    }

    fun mapDescriptionError(error: DomainScopeInputError.DescriptionError, description: String): ScopeInputError = when (error) {
        is DomainScopeInputError.DescriptionError.DescriptionTooLong ->
            ScopeInputError.DescriptionTooLong(description, error.maxLength)
    }
}

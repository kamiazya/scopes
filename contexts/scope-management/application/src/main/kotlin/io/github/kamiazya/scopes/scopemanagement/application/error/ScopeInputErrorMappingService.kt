package io.github.kamiazya.scopes.scopemanagement.application.error
import io.github.kamiazya.scopes.scopemanagement.application.util.InputSanitizer
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError as DomainScopeInputError

/**
 * Service for mapping domain input validation errors to application layer errors.
 * Centralizes error mapping logic to avoid duplication across command handlers.
 */
class ScopeInputErrorMappingService {

    private val errorPresenter = ScopeInputErrorPresenter()

    fun mapIdError(error: DomainScopeInputError.IdError, id: String): ScopeInputError = when (error) {
        is DomainScopeInputError.IdError.EmptyId ->
            ScopeInputError.IdBlank(InputSanitizer.createPreview(id))
        is DomainScopeInputError.IdError.InvalidIdFormat ->
            ScopeInputError.IdInvalidFormat(InputSanitizer.createPreview(id), errorPresenter.presentIdFormat(error.expectedFormat))
    }

    fun mapTitleError(error: DomainScopeInputError.TitleError, title: String): ScopeInputError = when (error) {
        is DomainScopeInputError.TitleError.EmptyTitle ->
            ScopeInputError.TitleEmpty(InputSanitizer.createPreview(title))
        is DomainScopeInputError.TitleError.TitleTooShort ->
            ScopeInputError.TitleTooShort(InputSanitizer.createPreview(title), error.minLength)
        is DomainScopeInputError.TitleError.TitleTooLong ->
            ScopeInputError.TitleTooLong(InputSanitizer.createPreview(title), error.maxLength)
        is DomainScopeInputError.TitleError.InvalidTitleFormat ->
            ScopeInputError.TitleContainsProhibitedCharacters(InputSanitizer.createPreview(title), listOf('<', '>', '&', '"'))
    }

    fun mapAliasError(error: DomainScopeInputError.AliasError, alias: String): ScopeInputError = when (error) {
        is DomainScopeInputError.AliasError.EmptyAlias ->
            ScopeInputError.AliasEmpty(InputSanitizer.createPreview(alias))
        is DomainScopeInputError.AliasError.AliasTooShort ->
            ScopeInputError.AliasTooShort(InputSanitizer.createPreview(alias), error.minLength)
        is DomainScopeInputError.AliasError.AliasTooLong ->
            ScopeInputError.AliasTooLong(InputSanitizer.createPreview(alias), error.maxLength)
        is DomainScopeInputError.AliasError.InvalidAliasFormat ->
            ScopeInputError.AliasInvalidFormat(InputSanitizer.createPreview(alias), errorPresenter.presentAliasPattern(error.expectedPattern))
    }

    fun mapDescriptionError(error: DomainScopeInputError.DescriptionError, description: String): ScopeInputError = when (error) {
        is DomainScopeInputError.DescriptionError.DescriptionTooLong ->
            ScopeInputError.DescriptionTooLong(InputSanitizer.createPreview(description), error.maxLength)
    }
}

package io.github.kamiazya.scopes.scopemanagement.infrastructure.adapters

import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.platform.application.error.BaseErrorMapper
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.error.ApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeInputError as AppScopeInputError

/**
 * Maps application errors to contract layer errors.
 *
 * This mapper handles application-layer errors that are not domain errors,
 * ensuring consistent error translation to the contract layer.
 */
class ApplicationErrorMapper(logger: Logger) : BaseErrorMapper<ApplicationError, ScopeContractError>(logger) {
    override fun mapToContractError(domainError: ApplicationError): ScopeContractError = when (domainError) {
        is AppScopeInputError.AliasNotFound -> ScopeContractError.BusinessError.AliasNotFound(
            alias = domainError.attemptedValue,
        )
        is AppScopeInputError.IdBlank -> ScopeContractError.InputError.InvalidId(
            id = domainError.attemptedValue,
            expectedFormat = "Non-empty ULID format",
        )
        is AppScopeInputError.IdInvalidFormat -> ScopeContractError.InputError.InvalidId(
            id = domainError.attemptedValue,
            expectedFormat = domainError.expectedFormat,
        )
        is AppScopeInputError.TitleEmpty -> ScopeContractError.InputError.InvalidTitle(
            title = domainError.attemptedValue,
            validationFailure = ScopeContractError.TitleValidationFailure.Empty,
        )
        is AppScopeInputError.TitleTooShort -> ScopeContractError.InputError.InvalidTitle(
            title = domainError.attemptedValue,
            validationFailure = ScopeContractError.TitleValidationFailure.TooShort(
                minimumLength = domainError.minimumLength,
                actualLength = domainError.attemptedValue.length,
            ),
        )
        is AppScopeInputError.TitleTooLong -> ScopeContractError.InputError.InvalidTitle(
            title = domainError.attemptedValue,
            validationFailure = ScopeContractError.TitleValidationFailure.TooLong(
                maximumLength = domainError.maximumLength,
                actualLength = domainError.attemptedValue.length,
            ),
        )
        is AppScopeInputError.TitleContainsProhibitedCharacters -> ScopeContractError.InputError.InvalidTitle(
            title = domainError.attemptedValue,
            validationFailure = ScopeContractError.TitleValidationFailure.InvalidCharacters(
                prohibitedCharacters = domainError.prohibitedCharacters,
            ),
        )
        is AppScopeInputError.AliasDuplicate -> ScopeContractError.BusinessError.DuplicateAlias(
            alias = domainError.attemptedValue,
        )
        else -> handleUnmappedError(
            domainError,
            ScopeContractError.SystemError.ServiceUnavailable(service = "scope-management"),
        )
    }
}

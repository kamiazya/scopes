package io.github.kamiazya.scopes.scopemanagement.application.service.error

import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeInputErrorMappingService
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeManagementApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.error.toGenericApplicationError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeNotFoundError
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId

/**
 * Centralized service for mapping domain errors to application errors.
 * Reduces duplication across handlers by providing consistent error mapping patterns.
 */
class CentralizedErrorMappingService {

    private val inputErrorMappingService = ScopeInputErrorMappingService()

    /**
     * Map scope ID parsing errors with consistent context.
     */
    fun mapScopeIdError(
        error: io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.IdError,
        id: String,
        context: String = "scope-operation",
    ): ScopeManagementApplicationError = inputErrorMappingService.mapIdError(error, id)

    /**
     * Map title validation errors with consistent context.
     */
    fun mapTitleError(
        error: io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.TitleError,
        title: String,
        context: String = "scope-operation",
    ): ScopeManagementApplicationError = inputErrorMappingService.mapTitleError(error, title)

    /**
     * Map description validation errors with consistent context.
     */
    fun mapDescriptionError(
        error: io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.DescriptionError,
        description: String,
        context: String = "scope-operation",
    ): ScopeManagementApplicationError = inputErrorMappingService.mapDescriptionError(error, description)

    /**
     * Map scope not found errors with consistent context.
     */
    fun mapScopeNotFoundError(scopeId: ScopeId, context: String = "scope-operation"): ScopeManagementApplicationError =
        ScopeNotFoundError(scopeId = scopeId).toGenericApplicationError()

    /**
     * Map generic domain errors to application errors.
     */
    fun mapDomainError(
        error: io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError,
        context: String = "scope-operation",
    ): ScopeManagementApplicationError = error.toGenericApplicationError()

    /**
     * Map repository errors to application errors.
     */
    fun mapRepositoryError(error: io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError, operation: String): ScopeManagementApplicationError =
        error.toGenericApplicationError()
}

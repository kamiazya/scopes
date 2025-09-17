package io.github.kamiazya.scopes.scopemanagement.infrastructure.adapters

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.contracts.scopemanagement.AspectQueryPort
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetAspectDefinitionQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.ListAspectDefinitionsQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.ValidateAspectValueQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.types.AspectDefinition
import io.github.kamiazya.scopes.scopemanagement.application.query.dto.GetAspectDefinition
import io.github.kamiazya.scopes.scopemanagement.application.query.dto.ListAspectDefinitions
import io.github.kamiazya.scopes.scopemanagement.application.query.handler.aspect.GetAspectDefinitionHandler
import io.github.kamiazya.scopes.scopemanagement.application.query.handler.aspect.ListAspectDefinitionsHandler
import io.github.kamiazya.scopes.scopemanagement.application.usecase.ValidateAspectValueUseCase
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError

/**
 * Query port adapter implementation for Aspect operations.
 * Handles query operations that read aspect definition data.
 */
public class AspectQueryPortAdapter(
    private val getAspectDefinitionHandler: GetAspectDefinitionHandler,
    private val listAspectDefinitionsHandler: ListAspectDefinitionsHandler,
    private val validateAspectValueUseCase: ValidateAspectValueUseCase,
) : AspectQueryPort {

    override suspend fun getAspectDefinition(query: GetAspectDefinitionQuery): Either<ScopeContractError, AspectDefinition?> {
        val result = getAspectDefinitionHandler(GetAspectDefinition(query.key))

        return result.fold(
            ifLeft = { error -> mapScopesErrorToScopeContractError(error).left() },
            ifRight = { aspectDefinitionDto ->
                aspectDefinitionDto?.toContractAspectDefinition().right()
            },
        )
    }

    override suspend fun listAspectDefinitions(query: ListAspectDefinitionsQuery): Either<ScopeContractError, List<AspectDefinition>> {
        val result = listAspectDefinitionsHandler(ListAspectDefinitions())

        return result.fold(
            ifLeft = { error -> mapScopesErrorToScopeContractError(error).left() },
            ifRight = { aspectDefinitionDtos ->
                aspectDefinitionDtos.map { it.toContractAspectDefinition() }.right()
            },
        )
    }

    override suspend fun validateAspectValue(query: ValidateAspectValueQuery): Either<ScopeContractError, List<String>> {
        val result = if (query.values.size == 1) {
            validateAspectValueUseCase(ValidateAspectValueUseCase.Query(query.key, query.values.first()))
                .map { listOf(it.value) }
        } else {
            validateAspectValueUseCase(ValidateAspectValueUseCase.MultipleQuery(mapOf(query.key to query.values)))
                .map { result ->
                    result.values.firstOrNull()?.map { it.value } ?: emptyList()
                }
        }

        return result.fold(
            ifLeft = { error -> mapScopesErrorToScopeContractError(error).left() },
            ifRight = { validatedValues ->
                validatedValues.right()
            },
        )
    }

    /**
     * Maps domain errors to contract layer errors for query operations.
     */
    private fun mapScopesErrorToScopeContractError(error: ScopesError): ScopeContractError = when (error) {
        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError.NotFound ->
            ScopeContractError.BusinessError.NotFound(error.identifier)
        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError.ValidationFailed ->
            // For aspect validation errors, we map to a generic InvalidId error with descriptive format
            // since aspects don't have specific error types in the contract
            when (val constraint = error.constraint) {
                is ScopesError.ValidationConstraintType.InvalidType ->
                    ScopeContractError.InputError.InvalidId(
                        error.value,
                        "Expected type: ${constraint.expectedType}, but got: ${constraint.actualType}",
                    )
                is ScopesError.ValidationConstraintType.MissingRequired ->
                    ScopeContractError.InputError.InvalidId(
                        error.value,
                        "Required field '${error.field}' is missing",
                    )
                is ScopesError.ValidationConstraintType.InvalidFormat ->
                    ScopeContractError.InputError.InvalidId(
                        error.value,
                        "Invalid format. Expected: ${constraint.expectedFormat ?: "valid format"}",
                    )
                is ScopesError.ValidationConstraintType.NotInAllowedValues ->
                    ScopeContractError.InputError.InvalidId(
                        error.value,
                        "Value must be one of: ${constraint.allowedValues.joinToString(", ")}",
                    )
                is ScopesError.ValidationConstraintType.InvalidValue ->
                    ScopeContractError.InputError.InvalidId(
                        error.value,
                        constraint.reason,
                    )
                is ScopesError.ValidationConstraintType.MultipleValuesNotAllowed ->
                    ScopeContractError.InputError.InvalidId(
                        error.value,
                        "Multiple values not allowed for field '${constraint.field}'",
                    )
            }
        else -> ScopeContractError.SystemError.ServiceUnavailable("AspectService")
    }

    private fun io.github.kamiazya.scopes.scopemanagement.application.dto.aspect.AspectDefinitionDto.toContractAspectDefinition() = AspectDefinition(
        key = this.key,
        description = this.description ?: "",
        type = this.type.toString(),
        createdAt = kotlinx.datetime.Clock.System.now(),
        updatedAt = kotlinx.datetime.Clock.System.now(),
    )
}

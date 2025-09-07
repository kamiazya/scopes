package io.github.kamiazya.scopes.scopemanagement.infrastructure.adapters

import io.github.kamiazya.scopes.contracts.scopemanagement.AspectQueryPort
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetAspectDefinitionQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.ListAspectDefinitionsQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.ValidateAspectValueQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.results.GetAspectDefinitionResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.ListAspectDefinitionsResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.ValidateAspectValueResult
import io.github.kamiazya.scopes.contracts.scopemanagement.types.AspectDefinition
import io.github.kamiazya.scopes.contracts.scopemanagement.types.ValidationFailure
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

    override suspend fun getAspectDefinition(query: GetAspectDefinitionQuery): GetAspectDefinitionResult {
        val result = getAspectDefinitionHandler(GetAspectDefinition(query.key))

        return result.fold(
            ifLeft = { error -> mapScopesErrorToGetResponse(error, query.key) },
            ifRight = { aspectDefinitionDto ->
                GetAspectDefinitionResult.Success(
                    aspectDefinitionDto?.toContractAspectDefinition(),
                )
            },
        )
    }

    override suspend fun listAspectDefinitions(query: ListAspectDefinitionsQuery): ListAspectDefinitionsResult {
        val result = listAspectDefinitionsHandler(ListAspectDefinitions())

        return result.fold(
            ifLeft = { _ ->
                // List errors are rare, return empty list
                ListAspectDefinitionsResult.Success(emptyList())
            },
            ifRight = { aspectDefinitionDtos ->
                ListAspectDefinitionsResult.Success(
                    aspectDefinitionDtos.map { it.toContractAspectDefinition() },
                )
            },
        )
    }

    override suspend fun validateAspectValue(query: ValidateAspectValueQuery): ValidateAspectValueResult {
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
            ifLeft = { error -> mapScopesErrorToValidateResponse(error, query.key) },
            ifRight = { validatedValues ->
                ValidateAspectValueResult.Success(validatedValues)
            },
        )
    }

    private fun mapScopesErrorToGetResponse(error: ScopesError, key: String): GetAspectDefinitionResult = when (error) {
        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError.NotFound ->
            GetAspectDefinitionResult.NotFound(key)
        else -> GetAspectDefinitionResult.NotFound(key)
    }

    private fun mapScopesErrorToValidateResponse(error: ScopesError, key: String): ValidateAspectValueResult = when (error) {
        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError.ValidationFailed ->
            when (val constraint = error.constraint) {
                is ScopesError.ValidationConstraintType.InvalidType ->
                    ValidateAspectValueResult.ValidationFailed(
                        key,
                        ValidationFailure.InvalidType(constraint.expectedType),
                    )
                is ScopesError.ValidationConstraintType.MissingRequired ->
                    ValidateAspectValueResult.ValidationFailed(
                        key,
                        ValidationFailure.Empty,
                    )
                is ScopesError.ValidationConstraintType.InvalidFormat ->
                    ValidateAspectValueResult.ValidationFailed(
                        key,
                        ValidationFailure.InvalidFormat(constraint.expectedFormat),
                    )
                is ScopesError.ValidationConstraintType.NotInAllowedValues ->
                    ValidateAspectValueResult.ValidationFailed(
                        key,
                        ValidationFailure.NotInAllowedValues(constraint.allowedValues),
                    )
                is ScopesError.ValidationConstraintType.InvalidValue ->
                    ValidateAspectValueResult.ValidationFailed(
                        key,
                        ValidationFailure.InvalidFormat(constraint.reason),
                    )
                is ScopesError.ValidationConstraintType.MultipleValuesNotAllowed ->
                    ValidateAspectValueResult.ValidationFailed(
                        key,
                        ValidationFailure.MultipleValuesNotAllowed,
                    )
            }
        else -> ValidateAspectValueResult.ValidationFailed(
            key,
            ValidationFailure.InvalidType("validation error"),
        )
    }

    private fun io.github.kamiazya.scopes.scopemanagement.application.dto.aspect.AspectDefinitionDto.toContractAspectDefinition() = AspectDefinition(
        key = this.key,
        description = this.description ?: "",
        type = this.type.toString(),
        createdAt = kotlinx.datetime.Clock.System.now(),
        updatedAt = kotlinx.datetime.Clock.System.now(),
    )
}

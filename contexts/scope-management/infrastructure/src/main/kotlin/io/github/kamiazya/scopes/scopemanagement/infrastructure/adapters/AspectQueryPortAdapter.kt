package io.github.kamiazya.scopes.scopemanagement.infrastructure.adapters

import io.github.kamiazya.scopes.contracts.scopemanagement.AspectQueryPort
import io.github.kamiazya.scopes.contracts.scopemanagement.aspect.AspectContract
import io.github.kamiazya.scopes.contracts.scopemanagement.aspect.GetAspectDefinitionQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.aspect.ListAspectDefinitionsQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.aspect.ValidateAspectValueQuery
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

    override suspend fun getAspectDefinition(query: GetAspectDefinitionQuery): AspectContract.GetAspectDefinitionResponse {
        val result = getAspectDefinitionHandler(GetAspectDefinition(query.key))

        return result.fold(
            ifLeft = { error -> mapScopesErrorToGetResponse(error, query.key) },
            ifRight = { aspectDefinitionDto ->
                AspectContract.GetAspectDefinitionResponse.Success(
                    aspectDefinitionDto?.toContractAspectDefinition(),
                )
            },
        )
    }

    override suspend fun listAspectDefinitions(query: ListAspectDefinitionsQuery): AspectContract.ListAspectDefinitionsResponse {
        val result = listAspectDefinitionsHandler(ListAspectDefinitions())

        return result.fold(
            ifLeft = { _ ->
                // List errors are rare, return empty list
                AspectContract.ListAspectDefinitionsResponse.Success(emptyList())
            },
            ifRight = { aspectDefinitionDtos ->
                AspectContract.ListAspectDefinitionsResponse.Success(
                    aspectDefinitionDtos.map { it.toContractAspectDefinition() },
                )
            },
        )
    }

    override suspend fun validateAspectValue(query: ValidateAspectValueQuery): AspectContract.ValidateAspectValueResponse {
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
                AspectContract.ValidateAspectValueResponse.Success(validatedValues)
            },
        )
    }

    private fun mapScopesErrorToGetResponse(error: ScopesError, key: String): AspectContract.GetAspectDefinitionResponse = when (error) {
        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError.NotFound ->
            AspectContract.GetAspectDefinitionResponse.NotFound(key)
        else -> AspectContract.GetAspectDefinitionResponse.NotFound(key)
    }

    private fun mapScopesErrorToValidateResponse(error: ScopesError, key: String): AspectContract.ValidateAspectValueResponse = when (error) {
        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError.ValidationFailed ->
            when (val constraint = error.constraint) {
                is ScopesError.ValidationConstraintType.InvalidType ->
                    AspectContract.ValidateAspectValueResponse.ValidationFailed(
                        key,
                        AspectContract.ValidationFailure.InvalidType(constraint.expectedType),
                    )
                is ScopesError.ValidationConstraintType.MissingRequired ->
                    AspectContract.ValidateAspectValueResponse.ValidationFailed(
                        key,
                        AspectContract.ValidationFailure.Empty,
                    )
                is ScopesError.ValidationConstraintType.InvalidFormat ->
                    AspectContract.ValidateAspectValueResponse.ValidationFailed(
                        key,
                        AspectContract.ValidationFailure.InvalidFormat(constraint.expectedFormat),
                    )
                is ScopesError.ValidationConstraintType.NotInAllowedValues ->
                    AspectContract.ValidateAspectValueResponse.ValidationFailed(
                        key,
                        AspectContract.ValidationFailure.NotInAllowedValues(constraint.allowedValues),
                    )
                is ScopesError.ValidationConstraintType.InvalidValue ->
                    AspectContract.ValidateAspectValueResponse.ValidationFailed(
                        key,
                        AspectContract.ValidationFailure.InvalidFormat(constraint.reason),
                    )
                is ScopesError.ValidationConstraintType.MultipleValuesNotAllowed ->
                    AspectContract.ValidateAspectValueResponse.ValidationFailed(
                        key,
                        AspectContract.ValidationFailure.MultipleValuesNotAllowed,
                    )
            }
        else -> AspectContract.ValidateAspectValueResponse.ValidationFailed(
            key,
            AspectContract.ValidationFailure.InvalidType("validation error"),
        )
    }

    private fun io.github.kamiazya.scopes.scopemanagement.application.dto.aspect.AspectDefinitionDto.toContractAspectDefinition() =
        io.github.kamiazya.scopes.contracts.scopemanagement.aspect.AspectDefinition(
            key = this.key,
            description = this.description ?: "",
            type = this.type.toString(),
            createdAt = kotlinx.datetime.Clock.System.now(),
            updatedAt = kotlinx.datetime.Clock.System.now(),
        )
}

package io.github.kamiazya.scopes.interfaces.cli.adapters

import arrow.core.Either
import io.github.kamiazya.scopes.scopemanagement.application.command.DefineAspectUseCase
import io.github.kamiazya.scopes.scopemanagement.application.command.aspect.DeleteAspectDefinitionUseCase
import io.github.kamiazya.scopes.scopemanagement.application.command.aspect.UpdateAspectDefinitionUseCase
import io.github.kamiazya.scopes.scopemanagement.application.query.aspect.GetAspectDefinitionUseCase
import io.github.kamiazya.scopes.scopemanagement.application.query.aspect.ListAspectDefinitionsUseCase
import io.github.kamiazya.scopes.scopemanagement.application.usecase.ValidateAspectValueUseCase
import io.github.kamiazya.scopes.scopemanagement.domain.entity.AspectDefinition
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectType
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectValue

/**
 * Adapter for aspect-related CLI commands.
 * Maps between CLI commands and application use cases.
 */
class AspectCommandAdapter(
    private val defineAspectUseCase: DefineAspectUseCase,
    private val getAspectDefinitionUseCase: GetAspectDefinitionUseCase,
    private val updateAspectDefinitionUseCase: UpdateAspectDefinitionUseCase,
    private val deleteAspectDefinitionUseCase: DeleteAspectDefinitionUseCase,
    private val listAspectDefinitionsUseCase: ListAspectDefinitionsUseCase,
    private val validateAspectValueUseCase: ValidateAspectValueUseCase,
) {
    /**
     * Define a new aspect.
     */
    suspend fun defineAspect(key: String, description: String, type: AspectType): Either<ScopesError, AspectDefinition> =
        defineAspectUseCase(DefineAspectUseCase.Command(key, description, type))

    /**
     * Get an aspect definition by key.
     */
    suspend fun getAspectDefinition(key: String): Either<ScopesError, AspectDefinition?> = getAspectDefinitionUseCase(GetAspectDefinitionUseCase.Query(key))

    /**
     * Update an aspect definition.
     */
    suspend fun updateAspectDefinition(key: String, description: String? = null): Either<ScopesError, AspectDefinition> =
        updateAspectDefinitionUseCase(UpdateAspectDefinitionUseCase.Command(key, description))

    /**
     * Delete an aspect definition.
     */
    suspend fun deleteAspectDefinition(key: String): Either<ScopesError, Unit> = deleteAspectDefinitionUseCase(DeleteAspectDefinitionUseCase.Command(key))

    /**
     * List all aspect definitions.
     */
    suspend fun listAspectDefinitions(): Either<ScopesError, List<AspectDefinition>> = listAspectDefinitionsUseCase(ListAspectDefinitionsUseCase.Query())

    /**
     * Validate aspect values against their definitions.
     */
    suspend fun validateAspectValue(key: String, values: List<String>): Either<ScopesError, List<AspectValue>> = if (values.size == 1) {
        validateAspectValueUseCase(ValidateAspectValueUseCase.Query(key, values.first())).map { listOf(it) }
    } else {
        validateAspectValueUseCase(ValidateAspectValueUseCase.MultipleQuery(mapOf(key to values))).map {
            it.values.first()
        }
    }
}

package io.github.kamiazya.scopes.scopemanagement.infrastructure.bootstrap

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.domain.entity.AspectDefinition
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.domain.repository.AspectDefinitionRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectKey
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectValue

/**
 * Bootstrap class for initializing standard aspect preset definitions.
 *
 * This class is responsible for creating the standard aspect definitions
 * (priority, status, type) on first application startup.
 */
class AspectPresetBootstrap(private val aspectDefinitionRepository: AspectDefinitionRepository, private val logger: Logger) {
    /**
     * Initialize standard aspect presets if they don't already exist.
     * This operation is idempotent - existing definitions will not be overwritten.
     */
    suspend fun initialize(): Either<ScopesError, Unit> = either {
        logger.info("Initializing standard aspect presets")

        // Define standard presets
        val presets = listOf(
            createPriorityPreset(),
            createStatusPreset(),
            createTypePreset(),
        )

        // Save each preset if it doesn't already exist
        presets.forEach { preset ->
            val existingDefinition = aspectDefinitionRepository.findByKey(preset.key)
                .mapLeft { error -> ScopesError.InvalidOperation("Failed to find aspect: $error") }
                .bind()
            if (existingDefinition == null) {
                aspectDefinitionRepository.save(preset)
                    .mapLeft { error -> ScopesError.InvalidOperation("Failed to save aspect: $error") }
                    .bind()
                logger.info("Created aspect preset: ${preset.key.value}")
            } else {
                logger.debug("Aspect preset already exists: ${preset.key.value}")
            }
        }

        logger.info("Aspect preset initialization completed")
    }

    private suspend fun createPriorityPreset(): AspectDefinition = either<ScopesError, AspectDefinition> {
        val key = AspectKey.create("priority").bind()
        val values = listOf(
            AspectValue.create("low").bind(),
            AspectValue.create("medium").bind(),
            AspectValue.create("high").bind(),
        )
        AspectDefinition.createOrdered(
            key = key,
            allowedValues = values,
            description = "Task priority level",
            allowMultiple = false,
        ).bind()
    }.getOrNull()!!

    private suspend fun createStatusPreset(): AspectDefinition = either<ScopesError, AspectDefinition> {
        val key = AspectKey.create("status").bind()
        AspectDefinition.createText(
            key = key,
            description = "Task status",
            allowMultiple = false,
        )
    }.getOrNull()!!

    private suspend fun createTypePreset(): AspectDefinition = either<ScopesError, AspectDefinition> {
        val key = AspectKey.create("type").bind()
        AspectDefinition.createText(
            key = key,
            description = "Task type classification",
            allowMultiple = false,
        )
    }.getOrNull()!!

    companion object {
        // Standard preset values - these can be used as reference throughout the application
        val PRIORITY_VALUES = listOf("low", "medium", "high")
        val STATUS_VALUES = listOf("todo", "ready", "in-progress", "blocked", "done")
        val TYPE_VALUES = listOf("feature", "bug", "chore", "doc")
    }
}

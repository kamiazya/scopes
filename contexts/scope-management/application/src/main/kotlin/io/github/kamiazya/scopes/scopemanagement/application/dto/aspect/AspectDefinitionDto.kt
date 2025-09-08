package io.github.kamiazya.scopes.scopemanagement.application.dto.aspect

/**
 * Data Transfer Object for AspectDefinition.
 *
 * Used to transfer aspect definition data between application and external layers.
 */
data class AspectDefinitionDto(val key: String, val type: String, val description: String?, val allowMultiple: Boolean)

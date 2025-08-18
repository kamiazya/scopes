package io.github.kamiazya.scopes.application.dto

/**
 * Result DTO for aspect definition operations.
 * Uses sealed class pattern for type safety and better API design.
 */
sealed class AspectDefinitionResult : DTO {
    abstract val key: String
    abstract val description: String?
    abstract val isDefault: kotlin.Boolean
    abstract val allowMultiple: kotlin.Boolean

    /**
     * Text-based aspect definition result.
     */
    data class Text(
        override val key: String,
        override val description: String? = null,
        override val isDefault: kotlin.Boolean = false,
        override val allowMultiple: kotlin.Boolean = false
    ) : AspectDefinitionResult()

    /**
     * Numeric aspect definition result.
     */
    data class Numeric(
        override val key: String,
        override val description: String? = null,
        override val isDefault: kotlin.Boolean = false,
        override val allowMultiple: kotlin.Boolean = false
    ) : AspectDefinitionResult()

    /**
     * Boolean aspect definition result.
     */
    data class BooleanType(
        override val key: String,
        override val description: String? = null,
        override val isDefault: kotlin.Boolean = false,
        override val allowMultiple: kotlin.Boolean = false
    ) : AspectDefinitionResult()

    /**
     * Ordered aspect definition result with allowed values.
     */
    data class Ordered(
        override val key: String,
        val allowedValues: List<String>,
        override val description: String? = null,
        override val isDefault: kotlin.Boolean = false,
        override val allowMultiple: kotlin.Boolean = false
    ) : AspectDefinitionResult()
}


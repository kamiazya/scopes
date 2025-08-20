package io.github.kamiazya.scopes.application.usecase.command

/**
 * Command to save or update an aspect definition.
 * This creates a user customization that overrides the default definition.
 */
sealed class SaveAspectDefinition : Command {
    abstract val key: String
    abstract val description: String?
    abstract val allowMultiple: Boolean

    /**
     * Save a text-based aspect definition.
     */
    data class Text(
        override val key: String,
        override val description: String? = null,
        override val allowMultiple: Boolean = false,
    ) : SaveAspectDefinition()

    /**
     * Save a numeric aspect definition.
     */
    data class Numeric(
        override val key: String,
        override val description: String? = null,
        override val allowMultiple: Boolean = false,
    ) : SaveAspectDefinition()

    /**
     * Save a boolean aspect definition.
     */
    data class BooleanType(
        override val key: String,
        override val description: String? = null,
        override val allowMultiple: Boolean = false,
    ) : SaveAspectDefinition()

    /**
     * Save an ordered aspect definition with allowed values.
     */
    data class Ordered(
        override val key: String,
        val allowedValues: List<String>,
        override val description: String? = null,
        override val allowMultiple: Boolean = false,
    ) : SaveAspectDefinition()
}

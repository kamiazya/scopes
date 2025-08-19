package io.github.kamiazya.scopes.domain.valueobject

import arrow.core.Either
import arrow.core.left
import arrow.core.right

/**
 * Value object representing a context view description.
 * 
 * Constraints:
 * - Can be empty (optional field)
 * - Maximum length of 1000 characters
 * - Must be trimmed (no leading/trailing whitespace)
 */
@JvmInline
value class ContextViewDescription private constructor(val value: String) {
    
    override fun toString(): String = value
    
    companion object {
        const val MAX_LENGTH = 1000
        
        /**
         * Create a ContextViewDescription from a string value.
         * Empty strings are allowed for this optional field.
         * 
         * @param value The description text
         * @return Either an error or the ContextViewDescription
         */
        fun create(value: String): Either<String, ContextViewDescription> {
            val trimmed = value.trim()
            
            return when {
                trimmed.length > MAX_LENGTH -> "Context view description cannot exceed $MAX_LENGTH characters".left()
                else -> ContextViewDescription(trimmed).right()
            }
        }
        
        /**
         * Create an empty description.
         */
        fun empty(): ContextViewDescription = ContextViewDescription("")
    }
}
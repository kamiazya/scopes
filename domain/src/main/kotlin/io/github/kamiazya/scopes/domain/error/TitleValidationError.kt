package io.github.kamiazya.scopes.domain.error

/**
 * Title validation specific errors with detailed context.
 */
sealed class TitleValidationError : ScopeValidationServiceError() {
    
    /**
     * Represents an empty title validation error.
     */
    object EmptyTitle : TitleValidationError()
    
    /**
     * Represents a title that is too short.
     * 
     * @param minLength The minimum allowed length
     * @param actualLength The actual length of the title
     * @param title The title that was too short
     */
    data class TitleTooShort(
        val minLength: Int,
        val actualLength: Int,
        val title: String
    ) : TitleValidationError()
    
    /**
     * Represents a title that is too long.
     * 
     * @param maxLength The maximum allowed length  
     * @param actualLength The actual length of the title
     * @param title The title that was too long
     */
    data class TitleTooLong(
        val maxLength: Int,
        val actualLength: Int,
        val title: String
    ) : TitleValidationError()
    
    /**
     * Represents a title containing invalid characters.
     * 
     * @param title The title with invalid characters
     * @param invalidCharacters The characters that are not allowed
     * @param position The position of the first invalid character
     */
    data class InvalidCharacters(
        val title: String,
        val invalidCharacters: Set<Char>,
        val position: Int
    ) : TitleValidationError()
}
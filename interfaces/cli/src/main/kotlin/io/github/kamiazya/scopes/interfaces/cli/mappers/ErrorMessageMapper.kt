package io.github.kamiazya.scopes.interfaces.cli.mappers

/**
 * Maps domain and contract errors to user-friendly messages for CLI output.
 *
 * TODO: This file needs to be updated to match the current error structures after the error refactoring.
 * For now, providing a basic implementation to allow compilation.
 */
object ErrorMessageMapper {

    /**
     * Converts a generic error to a user-friendly message.
     *
     * TODO: Implement proper error mapping once error structures are finalized.
     */
    fun toUserMessage(error: Any): String = when {
        error.toString().contains("NotFound") -> "The requested item was not found"
        error.toString().contains("InvalidFormat") -> "Invalid format provided"
        error.toString().contains("TooLong") -> "Input is too long"
        error.toString().contains("TooShort") -> "Input is too short"
        error.toString().contains("Duplicate") -> "Duplicate entry detected"
        error.toString().contains("Circular") -> "Circular reference detected"
        error.toString().contains("Archived") -> "Item is archived"
        error.toString().contains("Deleted") -> "Item is deleted"
        else -> "An error occurred: $error"
    }
}

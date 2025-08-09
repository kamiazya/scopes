package io.github.kamiazya.scopes.application.usecase

/**
 * Base interface for all use cases following functional programming principles.
 * Use cases are the application's entry points that orchestrate domain logic.
 * 
 * @param I Input type (Command or Query)
 * @param O Output type (typically a Result DTO)
 */
fun interface UseCase<I, O> {
    /**
     * Execute the use case with the given input.
     * Implementation should handle all error cases and return appropriate Result types.
     */
    suspend operator fun invoke(input: I): O
}

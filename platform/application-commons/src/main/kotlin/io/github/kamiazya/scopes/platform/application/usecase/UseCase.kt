package io.github.kamiazya.scopes.platform.application.usecase

import arrow.core.Either
import org.jmolecules.architecture.hexagonal.PrimaryPort

/**
 * Base interface for all use cases following functional programming principles.
 * Use cases are the application's entry points that orchestrate domain logic.
 *
 * Enforces type-safe error handling by requiring each UseCase to specify
 * its specific error type. This enables compile-time verification of which
 * errors can be returned by each UseCase implementation.
 *
 * Marked with @PrimaryPort to indicate this is a driving port in hexagonal architecture,
 * representing the application's public API for use case execution.
 *
 * @param I Input type (Command or Query)
 * @param E Error type (UseCase-specific error sealed class)
 * @param T Success result type (typically a Result DTO)
 */
@PrimaryPort
fun interface UseCase<I, E, T> {
    /**
     * Execute the use case with the given input.
     *
     * @param input The command or query input
     * @return Either<E, T> where:
     *   - Left contains UseCase-specific errors
     *   - Right contains the success result DTO
     */
    suspend operator fun invoke(input: I): Either<E, T>
}

package io.github.kamiazya.scopes.interfaces.cli.extensions

import arrow.core.Either
import arrow.core.fold
import com.github.ajalt.clikt.core.CliktError
import io.github.kamiazya.scopes.contracts.scope.error.ScopeContractError
import io.github.kamiazya.scopes.interfaces.cli.mapper.ContractErrorMessageMapper

/**
 * Extension functions for Either to integrate with Clikt framework.
 * These functions provide a Kotlin-idiomatic way to handle errors in CLI commands.
 */

/**
 * Converts an Either to a successful result or throws a CliktError.
 * This maintains integration with Clikt's error handling while being more idiomatic.
 */
fun <E, A> Either<E, A>.toCliktResult(errorMapper: (E) -> String): A =
    fold(
        ifLeft = { error -> throw CliktError(errorMapper(error)) },
        ifRight = { it }
    )

/**
 * Specialized version for ScopeContractError.
 */
fun <A> Either<ScopeContractError, A>.toCliktResult(): A =
    toCliktResult { "Error: ${ContractErrorMessageMapper.getMessage(it)}" }

/**
 * Alternative pattern using runCatching for operations that might throw.
 * Useful when interfacing with Java libraries or legacy code.
 */
inline fun <T> cliktCatching(block: () -> T): T =
    runCatching(block).getOrElse { throwable ->
        throw when (throwable) {
            is CliktError -> throwable
            else -> CliktError(throwable.message ?: "An unexpected error occurred")
        }
    }
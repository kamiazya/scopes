package io.github.kamiazya.scopes.platform.commons.result

import arrow.core.Either

typealias Result<E, A> = Either<E, A>

fun <A> Result.Companion.success(value: A): Result<Nothing, A> = Either.Right(value)
fun <E> Result.Companion.failure(error: E): Result<E, Nothing> = Either.Left(error)

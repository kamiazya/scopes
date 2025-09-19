package io.github.kamiazya.scopes.scopemanagement.domain.valueobject

import arrow.core.Either
import io.github.kamiazya.scopes.scopemanagement.domain.error.DomainValidationError
import kotlin.ConsistentCopyVisibility

/**
 * Value object for pagination parameters with validation.
 * Encapsulates the business rules for valid pagination values.
 */
@ConsistentCopyVisibility
data class Pagination private constructor(val offset: Int, val limit: Int) {
    companion object {
        private const val MIN_OFFSET = 0
        private const val MIN_LIMIT = 1
        private const val MAX_LIMIT = 1000

        /**
         * Create pagination parameters with validation.
         *
         * @param offset The offset value (must be >= 0)
         * @param limit The limit value (must be 1-1000)
         * @return Either validation error or valid pagination
         */
        fun create(offset: Int, limit: Int): Either<DomainValidationError.PaginationViolation, Pagination> = when {
            offset < MIN_OFFSET ->
                Either.Left(DomainValidationError.PaginationViolation.OffsetTooSmall(offset, MIN_OFFSET))
            limit < MIN_LIMIT ->
                Either.Left(DomainValidationError.PaginationViolation.LimitTooSmall(limit, MIN_LIMIT))
            limit > MAX_LIMIT ->
                Either.Left(DomainValidationError.PaginationViolation.LimitTooLarge(limit, MAX_LIMIT))
            else ->
                Either.Right(Pagination(offset, limit))
        }

        /**
         * Create pagination with default values.
         */
        fun default(): Pagination = Pagination(offset = 0, limit = 50)
    }

    /**
     * Calculate the SQL OFFSET value for database queries.
     */
    fun sqlOffset(): Int = offset

    /**
     * Calculate the SQL LIMIT value for database queries.
     */
    fun sqlLimit(): Int = limit

    /**
     * Check if this pagination would have a next page given total count.
     */
    fun hasNextPage(totalCount: Int): Boolean = try {
        Math.addExact(offset, limit) < totalCount
    } catch (e: ArithmeticException) {
        false // If overflow would occur, there's no next page
    }

    /**
     * Check if this pagination would have a previous page.
     */
    fun hasPreviousPage(): Boolean = offset > 0

    /**
     * Create pagination for the next page.
     */
    fun nextPage(): Either<DomainValidationError.PaginationViolation, Pagination> {
        // Check for overflow before adding
        val nextOffset = try {
            Math.addExact(offset, limit)
        } catch (e: ArithmeticException) {
            // If overflow would occur, return MAX_VALUE which will fail validation
            Int.MAX_VALUE
        }
        return create(nextOffset, limit)
    }

    /**
     * Create pagination for the previous page.
     */
    fun previousPage(): Either<DomainValidationError.PaginationViolation, Pagination> = create(
        maxOf(0, offset - limit),
        limit,
    )
}

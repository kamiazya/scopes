package io.github.kamiazya.scopes.scopemanagement.domain.valueobject

import io.github.kamiazya.scopes.scopemanagement.domain.error.DomainValidationError
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll

/**
 * Tests for Pagination value object.
 *
 * Business rules:
 * - Offset must be >= 0
 * - Limit must be between 1 and 1000
 * - Provides utility methods for navigation and SQL generation
 */
class PaginationTest :
    StringSpec({

        "should create valid pagination with typical values" {
            val result = Pagination.create(offset = 0, limit = 10)
            val pagination = result.shouldBeRight()
            pagination.offset shouldBe 0
            pagination.limit shouldBe 10
        }

        "should create valid pagination at boundaries" {
            // Minimum values
            val minResult = Pagination.create(offset = 0, limit = 1)
            val minPagination = minResult.shouldBeRight()
            minPagination.offset shouldBe 0
            minPagination.limit shouldBe 1

            // Maximum limit
            val maxResult = Pagination.create(offset = 0, limit = 1000)
            val maxPagination = maxResult.shouldBeRight()
            maxPagination.offset shouldBe 0
            maxPagination.limit shouldBe 1000

            // Large offset (valid)
            val largeOffsetResult = Pagination.create(offset = 999999, limit = 50)
            val largePagination = largeOffsetResult.shouldBeRight()
            largePagination.offset shouldBe 999999
            largePagination.limit shouldBe 50
        }

        "should reject negative offset" {
            val result = Pagination.create(offset = -1, limit = 10)
            result.shouldBeLeft()
            result.leftOrNull() shouldBe DomainValidationError.PaginationViolation.OffsetTooSmall(-1, 0)
        }

        "should reject zero limit" {
            val result = Pagination.create(offset = 0, limit = 0)
            result.shouldBeLeft()
            result.leftOrNull() shouldBe DomainValidationError.PaginationViolation.LimitTooSmall(0, 1)
        }

        "should reject negative limit" {
            val result = Pagination.create(offset = 0, limit = -5)
            result.shouldBeLeft()
            result.leftOrNull() shouldBe DomainValidationError.PaginationViolation.LimitTooSmall(-5, 1)
        }

        "should reject limit that is too large" {
            val result = Pagination.create(offset = 0, limit = 1001)
            result.shouldBeLeft()
            result.leftOrNull() shouldBe DomainValidationError.PaginationViolation.LimitTooLarge(1001, 1000)
        }

        "should reject both invalid offset and limit (offset checked first)" {
            val result = Pagination.create(offset = -1, limit = 1001)
            result.shouldBeLeft()
            // Offset validation happens first
            result.leftOrNull() shouldBe DomainValidationError.PaginationViolation.OffsetTooSmall(-1, 0)
        }

        "should create default pagination" {
            val defaultPagination = Pagination.default()
            defaultPagination.offset shouldBe 0
            defaultPagination.limit shouldBe 50
        }

        "should calculate SQL offset correctly" {
            val pagination = Pagination.create(offset = 100, limit = 25).getOrNull()!!
            pagination.sqlOffset() shouldBe 100
        }

        "should calculate SQL limit correctly" {
            val pagination = Pagination.create(offset = 100, limit = 25).getOrNull()!!
            pagination.sqlLimit() shouldBe 25
        }

        "should correctly identify when there is a next page" {
            val pagination = Pagination.create(offset = 0, limit = 10).getOrNull()!!

            // Has next page when total > offset + limit
            pagination.hasNextPage(15) shouldBe true
            pagination.hasNextPage(50) shouldBe true

            // No next page when total <= offset + limit
            pagination.hasNextPage(10) shouldBe false
            pagination.hasNextPage(5) shouldBe false
            pagination.hasNextPage(0) shouldBe false
        }

        "should correctly identify when there is a previous page" {
            // No previous page when offset is 0
            val firstPage = Pagination.create(offset = 0, limit = 10).getOrNull()!!
            firstPage.hasPreviousPage() shouldBe false

            // Has previous page when offset > 0
            val secondPage = Pagination.create(offset = 10, limit = 10).getOrNull()!!
            secondPage.hasPreviousPage() shouldBe true

            val laterPage = Pagination.create(offset = 100, limit = 25).getOrNull()!!
            laterPage.hasPreviousPage() shouldBe true
        }

        "should create next page correctly" {
            val firstPage = Pagination.create(offset = 0, limit = 10).getOrNull()!!
            val nextPageResult = firstPage.nextPage()
            val nextPage = nextPageResult.shouldBeRight()
            nextPage.offset shouldBe 10
            nextPage.limit shouldBe 10

            // Test multiple iterations
            val thirdPageResult = nextPage.nextPage()
            val thirdPage = thirdPageResult.shouldBeRight()
            thirdPage.offset shouldBe 20
            thirdPage.limit shouldBe 10
        }

        "should create previous page correctly" {
            // From second page back to first
            val secondPage = Pagination.create(offset = 10, limit = 10).getOrNull()!!
            val prevPageResult = secondPage.previousPage()
            val prevPage = prevPageResult.shouldBeRight()
            prevPage.offset shouldBe 0
            prevPage.limit shouldBe 10

            // From third page back to second
            val thirdPage = Pagination.create(offset = 20, limit = 10).getOrNull()!!
            val secondPageResult = thirdPage.previousPage()
            val actualSecondPage = secondPageResult.shouldBeRight()
            actualSecondPage.offset shouldBe 10
            actualSecondPage.limit shouldBe 10
        }

        "should handle previous page when already at first page" {
            val firstPage = Pagination.create(offset = 0, limit = 10).getOrNull()!!
            val prevPageResult = firstPage.previousPage()
            val prevPage = prevPageResult.shouldBeRight()
            prevPage.offset shouldBe 0 // Should stay at 0
            prevPage.limit shouldBe 10
        }

        "should handle previous page with partial offset" {
            // If offset is 5 and limit is 10, previous page should go to offset 0
            val partialPage = Pagination.create(offset = 5, limit = 10).getOrNull()!!
            val prevPageResult = partialPage.previousPage()
            val prevPage = prevPageResult.shouldBeRight()
            prevPage.offset shouldBe 0 // maxOf(0, 5 - 10) = 0
            prevPage.limit shouldBe 10
        }

        "should handle next page overflow protection" {
            // Create pagination near Integer.MAX_VALUE that would cause overflow
            val nearMaxPagination = Pagination.create(offset = Int.MAX_VALUE - 5, limit = 10).getOrNull()!!
            val nextPageResult = nearMaxPagination.nextPage()

            // Should succeed but return Int.MAX_VALUE due to overflow protection
            nextPageResult.shouldBeRight()
            nextPageResult.getOrNull()?.offset shouldBe Int.MAX_VALUE
            nextPageResult.getOrNull()?.limit shouldBe 10
        }

        "should maintain limit when navigating pages" {
            val originalLimit = 25
            val page = Pagination.create(offset = 50, limit = originalLimit).getOrNull()!!

            val nextPage = page.nextPage().getOrNull()!!
            nextPage.limit shouldBe originalLimit

            val prevPage = page.previousPage().getOrNull()!!
            prevPage.limit shouldBe originalLimit
        }

        "should handle edge case navigation scenarios" {
            // Large offset with small limit
            val largePage = Pagination.create(offset = 1000000, limit = 1).getOrNull()!!

            val nextPageResult = largePage.nextPage()
            val nextPage = nextPageResult.shouldBeRight()
            nextPage.offset shouldBe 1000001
            nextPage.limit shouldBe 1

            val prevPageResult = largePage.previousPage()
            val prevPage = prevPageResult.shouldBeRight()
            prevPage.offset shouldBe 999999
            prevPage.limit shouldBe 1
        }

        "should verify toString implementation (if any)" {
            val pagination = Pagination.create(offset = 10, limit = 20).getOrNull()!!
            // Data classes automatically provide toString
            val stringRepr = pagination.toString()
            stringRepr shouldContain "10"
            stringRepr shouldContain "20"
        }

        // Property-based testing
        "should always create valid pagination for valid inputs" {
            checkAll(
                Arb.int(0..10000), // Valid offsets
                Arb.int(1..1000), // Valid limits
            ) { offset, limit ->
                val result = Pagination.create(offset, limit)
                val pagination = result.shouldBeRight()
                pagination.offset shouldBe offset
                pagination.limit shouldBe limit
            }
        }

        "should always reject invalid offsets" {
            checkAll(Arb.int(Int.MIN_VALUE..-1)) { invalidOffset ->
                val result = Pagination.create(invalidOffset, 10)
                result.shouldBeLeft()
                val error = result.leftOrNull() as? DomainValidationError.PaginationViolation.OffsetTooSmall
                error?.offset shouldBe invalidOffset
                error?.minOffset shouldBe 0
            }
        }

        "should always reject invalid limits" {
            checkAll(Arb.int(Int.MIN_VALUE..0)) { invalidLimit ->
                val result = Pagination.create(0, invalidLimit)
                result.shouldBeLeft()
                val error = result.leftOrNull() as? DomainValidationError.PaginationViolation.LimitTooSmall
                error?.limit shouldBe invalidLimit
                error?.minLimit shouldBe 1
            }
        }

        "should always reject limits that are too large" {
            checkAll(Arb.int(1001..10000)) { tooLargeLimit ->
                val result = Pagination.create(0, tooLargeLimit)
                result.shouldBeLeft()
                val error = result.leftOrNull() as? DomainValidationError.PaginationViolation.LimitTooLarge
                error?.limit shouldBe tooLargeLimit
                error?.maxLimit shouldBe 1000
            }
        }

        "should handle realistic pagination scenarios" {
            // Typical web application pagination
            val webPage1 = Pagination.create(offset = 0, limit = 20).getOrNull()!!
            webPage1.hasNextPage(100) shouldBe true
            webPage1.hasPreviousPage() shouldBe false

            val webPage3 = webPage1.nextPage().getOrNull()!!.nextPage().getOrNull()!!
            webPage3.offset shouldBe 40
            webPage3.hasNextPage(100) shouldBe true
            webPage3.hasPreviousPage() shouldBe true

            // API pagination
            val apiPage = Pagination.create(offset = 500, limit = 100).getOrNull()!!
            apiPage.sqlOffset() shouldBe 500
            apiPage.sqlLimit() shouldBe 100
            apiPage.hasNextPage(1000) shouldBe true
            apiPage.hasNextPage(550) shouldBe false
        }
    })

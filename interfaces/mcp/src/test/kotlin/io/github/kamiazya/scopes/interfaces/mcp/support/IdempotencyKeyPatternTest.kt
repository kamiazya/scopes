package io.github.kamiazya.scopes.interfaces.mcp.support

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class IdempotencyKeyPatternTest : DescribeSpec({
    describe("IdempotencyService.IDEMPOTENCY_KEY_PATTERN") {
        val pattern = IdempotencyService.IDEMPOTENCY_KEY_PATTERN

        it("should accept valid idempotency keys") {
            val validKeys = listOf(
                "12345678",                      // Minimum 8 characters
                "a-b_c-d_",                      // Mix of allowed characters
                "ABCDEFGH",                      // Uppercase
                "abcdefgh",                      // Lowercase
                "123_ABC-",                      // Mix of all types
                "a".repeat(128),                 // Maximum 128 characters
                "test-key-123",                  // Typical format
                "request_12345678",              // Underscore format
                "2024-01-01_request_123"         // Date-like format
            )

            validKeys.forEach { key ->
                pattern.matches(key) shouldBe true
            }
        }

        it("should reject invalid idempotency keys") {
            val invalidKeys = listOf(
                "",                              // Empty
                "1234567",                       // Too short (7 chars)
                "a".repeat(129),                 // Too long (129 chars)
                "test key",                      // Contains space
                "test@key",                      // Contains @
                "test#key",                      // Contains #
                "test.key",                      // Contains .
                "test/key",                      // Contains /
                "test\\key",                     // Contains \
                "test:key",                      // Contains :
                "test;key",                      // Contains ;
                "test,key",                      // Contains ,
                "test<key",                      // Contains <
                "test>key",                      // Contains >
                "test?key",                      // Contains ?
                "test[key",                      // Contains [
                "test]key",                      // Contains ]
                "test{key",                      // Contains {
                "test}key",                      // Contains }
                "test|key",                      // Contains |
                "test~key",                      // Contains ~
                "test!key",                      // Contains !
                "test\"key",                     // Contains "
                "test'key",                      // Contains '
                "test`key",                      // Contains `
                "test\$key",                     // Contains $
                "test%key",                      // Contains %
                "test^key",                      // Contains ^
                "test&key",                      // Contains &
                "test*key",                      // Contains *
                "test(key",                      // Contains (
                "test)key",                      // Contains )
                "test+key",                      // Contains +
                "test=key",                      // Contains =
                "テストキー",                      // Non-ASCII characters
                "test\nkey",                     // Contains newline
                "test\tkey",                     // Contains tab
                "test\rkey"                      // Contains carriage return
            )

            invalidKeys.forEach { key ->
                pattern.matches(key) shouldBe false
            }
        }

        it("regex and string constants should be in sync") {
            // Ensure the Regex pattern matches the string constant
            pattern.pattern shouldBe IdempotencyService.IDEMPOTENCY_KEY_PATTERN_STRING
            
            // Also verify the expected pattern value
            IdempotencyService.IDEMPOTENCY_KEY_PATTERN_STRING shouldBe "^[A-Za-z0-9_-]{8,128}$"
        }
    }
})
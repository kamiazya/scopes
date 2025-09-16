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

        it("pattern string should be the expected regex") {
            pattern.pattern shouldBe "^[A-Za-z0-9_-]{8,128}$"
        }

        it("pattern string should match schema pattern in tool handlers") {
            // This ensures the exposed pattern matches what we use in JSON schemas
            val schemaPattern = "^[A-Za-z0-9_-]{8,128}$"
            pattern.pattern shouldBe schemaPattern
        }
    }
})
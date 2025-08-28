package io.github.kamiazya.scopes.scopemanagement.eventstore.valueobject

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class DeviceIdTest :
    DescribeSpec({
        describe("DeviceId creation") {
            it("should create valid device ID with alphanumeric characters") {
                val deviceId = DeviceId("device-123_ABC")
                deviceId.value shouldBe "device-123_ABC"
            }

            it("should reject blank device ID") {
                shouldThrow<IllegalArgumentException> {
                    DeviceId("")
                }.message shouldBe "Device ID cannot be blank"
            }

            it("should reject whitespace-only device ID") {
                shouldThrow<IllegalArgumentException> {
                    DeviceId("   ")
                }.message shouldBe "Device ID cannot be blank"
            }

            it("should reject device ID exceeding 64 characters") {
                val longId = "a".repeat(65)
                shouldThrow<IllegalArgumentException> {
                    DeviceId(longId)
                }.message shouldBe "Device ID cannot exceed 64 characters"
            }

            it("should reject device ID with invalid characters") {
                shouldThrow<IllegalArgumentException> {
                    DeviceId("device@123")
                }.message shouldBe "Device ID can only contain alphanumeric characters, hyphens, and underscores"
            }

            it("should accept device ID with exactly 64 characters") {
                val exactId = "a".repeat(64)
                val deviceId = DeviceId(exactId)
                deviceId.value shouldBe exactId
            }
        }

        describe("DeviceId generation") {
            it("should generate unique device IDs") {
                val id1 = DeviceId.generate()
                val id2 = DeviceId.generate()

                id1.value shouldNotBe id2.value
            }

            it("should generate valid UUID format") {
                repeat(10) {
                    val deviceId = DeviceId.generate()
                    // UUID format validation
                    deviceId.value.length shouldBe 36
                    deviceId.value.count { it == '-' } shouldBe 4
                }
            }
        }

        describe("DeviceId.fromStringOrNull") {
            it("should return DeviceId for valid input") {
                val result = DeviceId.fromStringOrNull("valid-device-id")
                result shouldNotBe null
                result?.value shouldBe "valid-device-id"
            }

            it("should return null for invalid input") {
                val result = DeviceId.fromStringOrNull("invalid@id")
                result shouldBe null
            }

            it("should return null for blank input") {
                val result = DeviceId.fromStringOrNull("")
                result shouldBe null
            }
        }

        describe("DeviceId validation") {
            it("should handle valid alphanumeric strings with hyphens and underscores") {
                val validStrings = listOf("abc123", "device-1", "test_device", "A1B2C3")
                for (validString in validStrings) {
                    val deviceId = DeviceId(validString)
                    deviceId.value shouldBe validString
                }
            }
        }
    })

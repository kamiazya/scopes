package io.github.kamiazya.scopes.platform.infrastructure.grpc

import arrow.core.Either
import arrow.core.right
import io.github.kamiazya.scopes.platform.observability.logging.ConsoleLogger
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class EndpointResolverTest :
    DescribeSpec({
        describe("EndpointResolver") {
            val logger = ConsoleLogger()
            val resolver = EndpointResolver(logger)

            describe("parseAddress") {
                // Using reflection to test the private parseAddress method
                val parseAddressMethod = EndpointResolver::class.java.getDeclaredMethod(
                    "parseAddress",
                    String::class.java,
                ).apply {
                    isAccessible = true
                }

                fun parseAddress(address: String): Either<EndpointResolver.EndpointError, Pair<String, Int>> {
                    @Suppress("UNCHECKED_CAST")
                    return parseAddressMethod.invoke(resolver, address) as Either<EndpointResolver.EndpointError, Pair<String, Int>>
                }

                context("IPv4 addresses") {
                    it("should parse standard IPv4 address with port") {
                        val result = parseAddress("127.0.0.1:8080")
                        result.shouldBe(("127.0.0.1" to 8080).right())
                    }

                    it("should parse localhost with port") {
                        val result = parseAddress("localhost:50051")
                        result.shouldBe(("localhost" to 50051).right())
                    }

                    it("should parse hostname with port") {
                        val result = parseAddress("example.com:443")
                        result.shouldBe(("example.com" to 443).right())
                    }
                }

                context("IPv6 addresses") {
                    it("should parse IPv6 loopback with brackets") {
                        val result = parseAddress("[::1]:8080")
                        result.shouldBe(("::1" to 8080).right())
                    }

                    it("should parse full IPv6 address with brackets") {
                        val result = parseAddress("[2001:db8:85a3:8d3:1319:8a2e:370:7348]:443")
                        result.shouldBe(("2001:db8:85a3:8d3:1319:8a2e:370:7348" to 443).right())
                    }

                    it("should parse shortened IPv6 address with brackets") {
                        val result = parseAddress("[2001:db8::1]:50051")
                        result.shouldBe(("2001:db8::1" to 50051).right())
                    }

                    it("should reject IPv6 address without brackets") {
                        val result = parseAddress("2001:db8::1:8080")
                        result.isLeft() shouldBe true
                        val error = result.leftOrNull()
                        error.shouldBeInstanceOf<EndpointResolver.EndpointError.InvalidFormat>()
                        error.message shouldBe "Invalid address format: '2001:db8::1:8080'. IPv6 addresses must be enclosed in brackets: '[2001:db8::1]:8080'"
                    }
                }

                context("port validation") {
                    it("should accept port 1") {
                        val result = parseAddress("localhost:1")
                        result.shouldBe(("localhost" to 1).right())
                    }

                    it("should accept port 65535") {
                        val result = parseAddress("localhost:65535")
                        result.shouldBe(("localhost" to 65535).right())
                    }

                    it("should reject port 0") {
                        val result = parseAddress("localhost:0")
                        result.isLeft() shouldBe true
                        val error = result.leftOrNull()
                        error.shouldBeInstanceOf<EndpointResolver.EndpointError.InvalidFormat>()
                        error.message shouldBe "Invalid port in address: 'localhost:0'. Port must be between 1 and 65535"
                    }

                    it("should reject port > 65535") {
                        val result = parseAddress("localhost:65536")
                        result.isLeft() shouldBe true
                        val error = result.leftOrNull()
                        error.shouldBeInstanceOf<EndpointResolver.EndpointError.InvalidFormat>()
                    }

                    it("should reject non-numeric port") {
                        val result = parseAddress("localhost:abc")
                        result.isLeft() shouldBe true
                        val error = result.leftOrNull()
                        error.shouldBeInstanceOf<EndpointResolver.EndpointError.InvalidFormat>()
                    }
                }

                context("invalid formats") {
                    it("should reject empty host") {
                        val result = parseAddress(":8080")
                        result.isLeft() shouldBe true
                        val error = result.leftOrNull()
                        error.shouldBeInstanceOf<EndpointResolver.EndpointError.InvalidFormat>()
                        error.message shouldBe "Empty host in address: ':8080'"
                    }

                    it("should reject address without port") {
                        val result = parseAddress("localhost")
                        result.isLeft() shouldBe true
                        val error = result.leftOrNull()
                        error.shouldBeInstanceOf<EndpointResolver.EndpointError.InvalidFormat>()
                    }

                    it("should reject empty string") {
                        val result = parseAddress("")
                        result.isLeft() shouldBe true
                        val error = result.leftOrNull()
                        error.shouldBeInstanceOf<EndpointResolver.EndpointError.InvalidFormat>()
                    }

                    it("should reject malformed IPv6 brackets") {
                        val result = parseAddress("[::1")
                        result.isLeft() shouldBe true
                        val error = result.leftOrNull()
                        error.shouldBeInstanceOf<EndpointResolver.EndpointError.InvalidFormat>()
                    }
                }

                context("edge cases") {
                    it("should handle spaces around address") {
                        val result = parseAddress("  localhost : 8080  ")
                        result.shouldBe(("localhost" to 8080).right())
                    }

                    it("should handle IPv6 with spaces") {
                        val result = parseAddress(" [::1] : 8080 ")
                        result.shouldBe(("::1" to 8080).right())
                    }
                }
            }
        }
    })

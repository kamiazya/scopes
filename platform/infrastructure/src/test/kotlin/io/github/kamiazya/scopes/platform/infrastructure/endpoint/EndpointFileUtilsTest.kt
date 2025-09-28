package io.github.kamiazya.scopes.platform.infrastructure.endpoint

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.File

class EndpointFileUtilsTest :
    DescribeSpec({

        describe("EndpointFileUtils") {
            describe("getDefaultEndpointFile") {
                context("when on macOS/Darwin") {
                    it("should return the correct path format") {
                        val originalOsName = System.getProperty("os.name")
                        try {
                            System.setProperty("os.name", "Mac OS X")
                            val endpointFile = EndpointFileUtils.getDefaultEndpointFile()

                            endpointFile.path shouldContain "Library/Application Support/scopes/run"
                            endpointFile.name shouldBe "scopesd.endpoint"
                        } finally {
                            System.setProperty("os.name", originalOsName)
                        }
                    }
                }

                context("when on Windows") {
                    it("should return the correct path format") {
                        val originalOsName = System.getProperty("os.name")
                        try {
                            System.setProperty("os.name", "Windows 10")
                            val endpointFile = EndpointFileUtils.getDefaultEndpointFile()

                            endpointFile.path shouldContain "scopes/run"
                            endpointFile.name shouldBe "scopesd.endpoint"
                        } finally {
                            System.setProperty("os.name", originalOsName)
                        }
                    }
                }

                context("when on Linux") {
                    it("should return the correct path format") {
                        val originalOsName = System.getProperty("os.name")
                        try {
                            System.setProperty("os.name", "Linux")
                            val endpointFile = EndpointFileUtils.getDefaultEndpointFile()

                            // Should use XDG_RUNTIME_DIR or fall back to ~/.local/share
                            endpointFile.name shouldBe "scopesd.endpoint"
                            endpointFile.path shouldContain "scopes"
                        } finally {
                            System.setProperty("os.name", originalOsName)
                        }
                    }
                }
            }

            describe("ensureEndpointDirectoryExists") {
                it("should create parent directories when they don't exist") {
                    val tempDir = kotlin.io.path.createTempDirectory("scopes-test").toFile()
                    val endpointFile = File(tempDir, "nested/path/scopesd.endpoint")

                    val result = EndpointFileUtils.ensureEndpointDirectoryExists(endpointFile)

                    result shouldBe true
                    endpointFile.parentFile.exists() shouldBe true

                    // Clean up
                    tempDir.deleteRecursively()
                }
            }

            describe("setSecurePermissions") {
                it("should set permissions on Unix-like systems") {
                    val tempFile = kotlin.io.path.createTempFile("test-endpoint", ".tmp").toFile()

                    try {
                        val result = EndpointFileUtils.setSecurePermissions(tempFile)

                        // Should succeed regardless of platform
                        result shouldBe true
                    } finally {
                        tempFile.delete()
                    }
                }
            }
        }
    })

package io.github.kamiazya.scopes.platform.infrastructure.grpc.interceptors

import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.Deadline
import io.grpc.MethodDescriptor
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.longs.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.*
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class DeadlineClientInterceptorTest :
    DescribeSpec({

        describe("DeadlineClientInterceptor") {

            describe("with default timeout") {
                val interceptor = DeadlineClientInterceptor()

                it("should add default deadline when none exists") {
                    // Arrange
                    val mockMethod = mockk<MethodDescriptor<String, String>>()
                    val mockCallOptions = CallOptions.DEFAULT
                    val mockChannel = mockk<Channel>()
                    val mockClientCall = mockk<ClientCall<String, String>>()
                    val capturedCallOptions = slot<CallOptions>()

                    every { mockChannel.newCall(mockMethod, capture(capturedCallOptions)) } returns mockClientCall

                    // Act
                    interceptor.interceptCall(mockMethod, mockCallOptions, mockChannel)

                    // Assert
                    verify { mockChannel.newCall(mockMethod, any()) }
                    val deadline = capturedCallOptions.captured.deadline
                    deadline shouldNotBe null

                    // Should be approximately 30 seconds from now (with some tolerance)
                    val actualDeadlineMs = deadline!!.timeRemaining(TimeUnit.MILLISECONDS)
                    val tolerance = 1000L // 1 second tolerance
                    kotlin.math.abs(actualDeadlineMs - 30_000L).shouldBeLessThan(tolerance)
                }

                it("should preserve existing deadline when already set") {
                    // Arrange
                    val mockMethod = mockk<MethodDescriptor<String, String>>()
                    val existingDeadline = Deadline.after(10, TimeUnit.SECONDS)
                    val mockCallOptions = CallOptions.DEFAULT.withDeadline(existingDeadline)
                    val mockChannel = mockk<Channel>()
                    val mockClientCall = mockk<ClientCall<String, String>>()
                    val capturedCallOptions = slot<CallOptions>()

                    every { mockChannel.newCall(mockMethod, capture(capturedCallOptions)) } returns mockClientCall

                    // Act
                    interceptor.interceptCall(mockMethod, mockCallOptions, mockChannel)

                    // Assert
                    verify { mockChannel.newCall(mockMethod, any()) }
                    capturedCallOptions.captured.deadline shouldBe existingDeadline
                }
            }

            describe("with custom timeout") {
                val customTimeout = 5.seconds
                val interceptor = DeadlineClientInterceptor(customTimeout)

                it("should add custom deadline when none exists") {
                    // Arrange
                    val mockMethod = mockk<MethodDescriptor<String, String>>()
                    val mockCallOptions = CallOptions.DEFAULT
                    val mockChannel = mockk<Channel>()
                    val mockClientCall = mockk<ClientCall<String, String>>()
                    val capturedCallOptions = slot<CallOptions>()

                    every { mockChannel.newCall(mockMethod, capture(capturedCallOptions)) } returns mockClientCall

                    // Act
                    interceptor.interceptCall(mockMethod, mockCallOptions, mockChannel)

                    // Assert
                    verify { mockChannel.newCall(mockMethod, any()) }
                    val deadline = capturedCallOptions.captured.deadline
                    deadline shouldNotBe null

                    // Should be approximately 5 seconds from now
                    val actualDeadlineMs = deadline!!.timeRemaining(TimeUnit.MILLISECONDS)
                    val tolerance = 1000L // 1 second tolerance
                    kotlin.math.abs(actualDeadlineMs - 5_000L).shouldBeLessThan(tolerance)
                }
            }

            describe("edge cases") {
                val interceptor = DeadlineClientInterceptor(100.milliseconds)

                it("should handle very short timeouts") {
                    // Arrange
                    val mockMethod = mockk<MethodDescriptor<String, String>>()
                    val mockCallOptions = CallOptions.DEFAULT
                    val mockChannel = mockk<Channel>()
                    val mockClientCall = mockk<ClientCall<String, String>>()
                    val capturedCallOptions = slot<CallOptions>()

                    every { mockChannel.newCall(mockMethod, capture(capturedCallOptions)) } returns mockClientCall

                    // Act
                    interceptor.interceptCall(mockMethod, mockCallOptions, mockChannel)

                    // Assert
                    verify { mockChannel.newCall(mockMethod, any()) }
                    capturedCallOptions.captured.deadline shouldNotBe null
                }
            }
        }
    })

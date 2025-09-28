package io.github.kamiazya.scopes.platform.infrastructure.grpc.interceptors

import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import io.grpc.Status
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.*

class RequestLoggingClientInterceptorTest :
    DescribeSpec({

        describe("RequestLoggingClientInterceptor") {
            val mockLogger = mockk<Logger>(relaxed = true)
            val interceptor = RequestLoggingClientInterceptor(mockLogger)

            afterTest {
                clearMocks(mockLogger)
            }

            it("should log request start with debug level") {
                // Arrange
                val methodName = "test.Service/TestMethod"
                val mockMethod = mockk<MethodDescriptor<String, String>>()
                every { mockMethod.fullMethodName } returns methodName

                val mockCallOptions = CallOptions.DEFAULT
                val mockChannel = mockk<Channel>()
                val mockClientCall = mockk<ClientCall<String, String>>()
                val mockListener = mockk<ClientCall.Listener<String>>(relaxed = true)
                val headers = Metadata()
                headers.put(CorrelationIdClientInterceptor.CORRELATION_ID_KEY, "test-corr-id")

                every { mockChannel.newCall(mockMethod, mockCallOptions) } returns mockClientCall
                every { mockClientCall.start(any(), any()) } just Runs

                // Act
                val interceptedCall = interceptor.interceptCall(mockMethod, mockCallOptions, mockChannel)
                interceptedCall.start(mockListener, headers)

                // Assert
                verify {
                    mockLogger.debug(
                        "gRPC client request",
                        mapOf(
                            "method" to methodName,
                            "correlation_id" to "test-corr-id",
                        ),
                    )
                }
            }

            it("should log successful response with info level") {
                // Arrange
                val methodName = "test.Service/TestMethod"
                val mockMethod = mockk<MethodDescriptor<String, String>>()
                every { mockMethod.fullMethodName } returns methodName

                val mockCallOptions = CallOptions.DEFAULT
                val mockChannel = mockk<Channel>()
                val mockClientCall = mockk<ClientCall<String, String>>()
                val mockListener = mockk<ClientCall.Listener<String>>(relaxed = true)
                val capturedListener = slot<ClientCall.Listener<String>>()

                every { mockChannel.newCall(mockMethod, mockCallOptions) } returns mockClientCall
                every { mockClientCall.start(capture(capturedListener), any()) } just Runs

                // Act
                val interceptedCall = interceptor.interceptCall(mockMethod, mockCallOptions, mockChannel)
                interceptedCall.start(mockListener, Metadata())

                // Simulate successful response
                capturedListener.captured.onClose(Status.OK, Metadata())

                // Assert
                verify {
                    mockLogger.info(
                        "gRPC client response",
                        withArg { context ->
                            context["method"] == methodName &&
                                context["status"] == "OK" &&
                                context.containsKey("elapsed_ms") &&
                                context["correlation_id"] == ""
                        },
                    )
                }
            }

            it("should log error response with warn level and error details") {
                // Arrange
                val methodName = "test.Service/TestMethod"
                val errorDescription = "Resource not found"
                val mockMethod = mockk<MethodDescriptor<String, String>>()
                every { mockMethod.fullMethodName } returns methodName

                val mockCallOptions = CallOptions.DEFAULT
                val mockChannel = mockk<Channel>()
                val mockClientCall = mockk<ClientCall<String, String>>()
                val mockListener = mockk<ClientCall.Listener<String>>(relaxed = true)
                val capturedListener = slot<ClientCall.Listener<String>>()

                every { mockChannel.newCall(mockMethod, mockCallOptions) } returns mockClientCall
                every { mockClientCall.start(capture(capturedListener), any()) } just Runs

                // Act
                val interceptedCall = interceptor.interceptCall(mockMethod, mockCallOptions, mockChannel)
                interceptedCall.start(mockListener, Metadata())

                // Simulate error response
                val errorStatus = Status.NOT_FOUND.withDescription(errorDescription)
                capturedListener.captured.onClose(errorStatus, Metadata())

                // Assert
                verify {
                    mockLogger.warn(
                        "gRPC client response",
                        withArg { context ->
                            context["method"] == methodName &&
                                context["status"] == "NOT_FOUND" &&
                                context.containsKey("elapsed_ms")
                        },
                    )

                    mockLogger.debug(
                        "gRPC client error details",
                        mapOf(
                            "method" to methodName,
                            "correlation_id" to "",
                            "error_description" to errorDescription,
                        ),
                    )
                }
            }
        }
    })

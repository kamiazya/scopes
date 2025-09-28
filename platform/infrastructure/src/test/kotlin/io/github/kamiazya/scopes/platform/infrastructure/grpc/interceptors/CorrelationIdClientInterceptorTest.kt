package io.github.kamiazya.scopes.platform.infrastructure.grpc.interceptors

import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.Context
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.*
import java.util.UUID

class CorrelationIdClientInterceptorTest :
    DescribeSpec({

        describe("CorrelationIdClientInterceptor") {
            val interceptor = CorrelationIdClientInterceptor()

            it("should add correlation ID to headers when not present") {
                // Arrange
                val mockMethod = mockk<MethodDescriptor<String, String>>()
                val mockCallOptions = CallOptions.DEFAULT
                val mockChannel = mockk<Channel>()
                val mockClientCall = mockk<ClientCall<String, String>>()
                val mockListener = mockk<ClientCall.Listener<String>>()
                val capturedHeaders = slot<Metadata>()

                every { mockChannel.newCall(mockMethod, mockCallOptions) } returns mockClientCall
                every { mockClientCall.start(any(), capture(capturedHeaders)) } just Runs

                // Act
                val interceptedCall = interceptor.interceptCall(mockMethod, mockCallOptions, mockChannel)
                val headers = Metadata()
                interceptedCall.start(mockListener, headers)

                // Assert
                verify { mockClientCall.start(any(), any()) }
                val correlationId = capturedHeaders.captured.get(CorrelationIdClientInterceptor.CORRELATION_ID_KEY)
                correlationId shouldNotBe null
                // Should be a valid UUID
                UUID.fromString(correlationId)
            }

            it("should use existing correlation ID from context") {
                // Arrange
                val existingCorrelationId = "test-correlation-id-123"
                val mockMethod = mockk<MethodDescriptor<String, String>>()
                val mockCallOptions = CallOptions.DEFAULT
                val mockChannel = mockk<Channel>()
                val mockClientCall = mockk<ClientCall<String, String>>()
                val mockListener = mockk<ClientCall.Listener<String>>()
                val capturedHeaders = slot<Metadata>()

                every { mockChannel.newCall(mockMethod, mockCallOptions) } returns mockClientCall
                every { mockClientCall.start(any(), capture(capturedHeaders)) } just Runs

                // Set correlation ID in context
                val context = Context.current().withValue(
                    CorrelationIdClientInterceptor.CTX_KEY,
                    existingCorrelationId,
                )
                val prevContext = context.attach()

                try {
                    // Act
                    val interceptedCall = interceptor.interceptCall(mockMethod, mockCallOptions, mockChannel)
                    val headers = Metadata()
                    interceptedCall.start(mockListener, headers)

                    // Assert
                    verify { mockClientCall.start(any(), any()) }
                    capturedHeaders.captured.get(CorrelationIdClientInterceptor.CORRELATION_ID_KEY) shouldBe existingCorrelationId
                } finally {
                    context.detach(prevContext)
                }
            }
        }
    })

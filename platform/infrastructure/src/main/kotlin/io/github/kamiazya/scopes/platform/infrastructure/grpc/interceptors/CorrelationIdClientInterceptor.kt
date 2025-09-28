package io.github.kamiazya.scopes.platform.infrastructure.grpc.interceptors

import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.ClientInterceptor
import io.grpc.Context
import io.grpc.ForwardingClientCall
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import java.util.UUID

/**
 * Client interceptor that ensures a correlation ID is present in outgoing requests.
 * If no correlation ID is in the current context, it generates one.
 */
class CorrelationIdClientInterceptor : ClientInterceptor {
    companion object {
        val CORRELATION_ID_KEY: Metadata.Key<String> =
            Metadata.Key.of("x-correlation-id", Metadata.ASCII_STRING_MARSHALLER)
        val CTX_KEY: Context.Key<String> = Context.key("correlationId")
    }

    override fun <ReqT : Any?, RespT : Any?> interceptCall(
        method: MethodDescriptor<ReqT, RespT>,
        callOptions: CallOptions,
        next: Channel,
    ): ClientCall<ReqT, RespT> = object : ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
        next.newCall(method, callOptions),
    ) {
        override fun start(responseListener: Listener<RespT>, headers: Metadata) {
            // Get correlation ID from context or generate a new one
            val correlationId = CTX_KEY.get() ?: UUID.randomUUID().toString()

            // Add correlation ID to headers
            headers.put(CORRELATION_ID_KEY, correlationId)

            // Store in context for downstream use
            val newCtx = Context.current().withValue(CTX_KEY, correlationId)
            val previousCtx = newCtx.attach()
            try {
                super.start(responseListener, headers)
            } finally {
                newCtx.detach(previousCtx)
            }
        }
    }
}

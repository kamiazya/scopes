package io.github.kamiazya.scopes.interfaces.daemon.interceptors

import io.grpc.Context
import io.grpc.Contexts
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import java.util.UUID

/**
 * Server interceptor that ensures a correlation ID is present in the metadata.
 * If missing, it generates one and makes it available via Context for downstream logging.
 */
class CorrelationIdServerInterceptor : ServerInterceptor {
    companion object {
        val CORRELATION_ID_KEY: Metadata.Key<String> = Metadata.Key.of("x-correlation-id", Metadata.ASCII_STRING_MARSHALLER)
        val CTX_KEY: Context.Key<String> = Context.key("correlationId")
    }

    override fun <ReqT : Any?, RespT : Any?> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>,
    ): ServerCall.Listener<ReqT> {
        val corr = headers.get(CORRELATION_ID_KEY) ?: UUID.randomUUID().toString()
        val ctx = Context.current().withValue(CTX_KEY, corr)
        return Contexts.interceptCall(ctx, call, headers, next)
    }
}

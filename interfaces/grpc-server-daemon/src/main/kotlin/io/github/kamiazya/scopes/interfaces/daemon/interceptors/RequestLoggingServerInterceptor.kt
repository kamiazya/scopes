package io.github.kamiazya.scopes.interfaces.daemon.interceptors

import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.grpc.ForwardingServerCall
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.Status

/**
 * Simple unary request logging interceptor that logs method, correlation ID and status with latency.
 */
class RequestLoggingServerInterceptor(private val logger: Logger) : ServerInterceptor {
    override fun <ReqT : Any?, RespT : Any?> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>,
    ): ServerCall.Listener<ReqT> {
        val method = call.methodDescriptor.fullMethodName
        val corr = CorrelationIdServerInterceptor.CTX_KEY.get() ?: headers.get(CorrelationIdServerInterceptor.CORRELATION_ID_KEY)
        val start = System.nanoTime()

        val forwardingCall = object : ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(call) {
            override fun close(status: Status, trailers: Metadata) {
                val elapsedMs = (System.nanoTime() - start) / 1_000_000
                logger.info(
                    "gRPC unary",
                    mapOf(
                        "method" to method,
                        "status" to status.code.toString(),
                        "elapsed_ms" to elapsedMs,
                        "correlation_id" to (corr ?: ""),
                    ),
                )
                super.close(status, trailers)
            }
        }

        return next.startCall(forwardingCall, headers)
    }
}

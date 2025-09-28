package io.github.kamiazya.scopes.platform.infrastructure.grpc.interceptors

import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.ClientInterceptor
import io.grpc.ForwardingClientCall
import io.grpc.ForwardingClientCallListener
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import io.grpc.Status

/**
 * Client interceptor that logs outgoing gRPC requests with method, correlation ID,
 * status, and latency information.
 */
class RequestLoggingClientInterceptor(private val logger: Logger) : ClientInterceptor {

    override fun <ReqT : Any?, RespT : Any?> interceptCall(
        method: MethodDescriptor<ReqT, RespT>,
        callOptions: CallOptions,
        next: Channel,
    ): ClientCall<ReqT, RespT> {
        val methodName = method.fullMethodName
        val start = System.nanoTime()

        return object : ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
            next.newCall(method, callOptions),
        ) {
            private var correlationId: String? = null

            override fun start(responseListener: Listener<RespT>, headers: Metadata) {
                // Capture correlation ID from headers
                correlationId = headers.get(CorrelationIdClientInterceptor.CORRELATION_ID_KEY)
                    ?: CorrelationIdClientInterceptor.CTX_KEY.get()

                // Log request start
                logger.debug(
                    "gRPC client request",
                    mapOf(
                        "method" to methodName,
                        "correlation_id" to (correlationId ?: ""),
                    ),
                )

                // Wrap the response listener to log response
                val loggingListener = object : ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT>(responseListener) {
                    override fun onClose(status: Status, trailers: Metadata) {
                        val elapsedMs = (System.nanoTime() - start) / 1_000_000

                        val logLevel = if (status.isOk) "info" else "warn"
                        val logMessage = "gRPC client response"
                        val logContext = mapOf(
                            "method" to methodName,
                            "status" to status.code.toString(),
                            "elapsed_ms" to elapsedMs,
                            "correlation_id" to (correlationId ?: ""),
                        )

                        when (logLevel) {
                            "info" -> logger.info(logMessage, logContext)
                            "warn" -> logger.warn(logMessage, logContext)
                            else -> logger.info(logMessage, logContext)
                        }

                        if (!status.isOk && status.description != null) {
                            logger.debug(
                                "gRPC client error details",
                                mapOf(
                                    "method" to methodName,
                                    "correlation_id" to (correlationId ?: ""),
                                    "error_description" to (status.description ?: ""),
                                ) as Map<String, Any>,
                            )
                        }

                        super.onClose(status, trailers)
                    }
                }

                super.start(loggingListener, headers)
            }
        }
    }
}

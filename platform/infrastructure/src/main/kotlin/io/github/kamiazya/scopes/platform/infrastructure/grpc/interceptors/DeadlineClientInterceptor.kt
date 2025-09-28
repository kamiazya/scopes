package io.github.kamiazya.scopes.platform.infrastructure.grpc.interceptors

import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.ClientInterceptor
import io.grpc.MethodDescriptor
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

/**
 * Client interceptor that adds a deadline to all gRPC calls.
 * This prevents calls from hanging indefinitely on network issues.
 */
class DeadlineClientInterceptor(private val defaultTimeout: Duration = Duration.parse("30s")) : ClientInterceptor {

    override fun <ReqT, RespT> interceptCall(method: MethodDescriptor<ReqT, RespT>, callOptions: CallOptions, next: Channel): ClientCall<ReqT, RespT> {
        // Only add deadline if not already set
        val optionsWithDeadline = if (callOptions.deadline == null) {
            callOptions.withDeadlineAfter(defaultTimeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)
        } else {
            callOptions
        }

        return next.newCall(method, optionsWithDeadline)
    }
}

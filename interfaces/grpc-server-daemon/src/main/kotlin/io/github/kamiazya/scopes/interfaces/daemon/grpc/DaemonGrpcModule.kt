package io.github.kamiazya.scopes.interfaces.daemon.grpc

import io.github.kamiazya.scopes.interfaces.daemon.grpc.services.TaskGatewayServiceImpl
import kotlinx.serialization.json.Json
import org.koin.dsl.module

/**
 * Koin module for daemon gRPC services.
 */
val daemonGrpcModule = module {
    // JSON configuration for serialization
    single {
        Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            isLenient = true
        }
    }

    // gRPC service implementations
    single {
        TaskGatewayServiceImpl(
            scopeManagementCommandPort = getOrNull(),
            scopeManagementQueryPort = getOrNull(),
            contextViewCommandPort = getOrNull(),
            contextViewQueryPort = getOrNull(),
            json = get(),
            logger = get(),
        )
    }
}

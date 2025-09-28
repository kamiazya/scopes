package io.github.kamiazya.scopes.interfaces.grpc.client.daemon.di

import io.github.kamiazya.scopes.interfaces.cli.grpc.GatewayClient
import io.github.kamiazya.scopes.interfaces.cli.grpc.GrpcClient
import io.github.kamiazya.scopes.platform.infrastructure.grpc.EndpointResolver
import kotlinx.serialization.json.Json
import org.koin.dsl.module

/**
 * Koin module for gRPC client dependencies
 */
val grpcClientModule = module {
    // Endpoint resolver for daemon communication
    single<EndpointResolver> { EndpointResolver(logger = get()) }
    
    // JSON serializer for protocol communication
    single<Json> { Json { ignoreUnknownKeys = true } }
    
    // gRPC Client implementations
    single { GrpcClient(endpointResolver = get(), logger = get()) }
    single { GatewayClient(endpointResolver = get(), logger = get(), json = get()) }
}
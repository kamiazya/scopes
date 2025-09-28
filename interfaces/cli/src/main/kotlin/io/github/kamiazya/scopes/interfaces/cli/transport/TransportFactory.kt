package io.github.kamiazya.scopes.interfaces.cli.transport

import io.github.kamiazya.scopes.interfaces.cli.adapters.AliasCommandAdapter
import io.github.kamiazya.scopes.interfaces.cli.adapters.AliasQueryAdapter
import io.github.kamiazya.scopes.interfaces.cli.adapters.ContextCommandAdapter
import io.github.kamiazya.scopes.interfaces.cli.adapters.ContextQueryAdapter
import io.github.kamiazya.scopes.interfaces.cli.adapters.ScopeCommandAdapter
import io.github.kamiazya.scopes.interfaces.cli.adapters.ScopeQueryAdapter
import io.github.kamiazya.scopes.interfaces.cli.grpc.GatewayClient
import io.github.kamiazya.scopes.interfaces.cli.resolvers.ScopeParameterResolver
import io.github.kamiazya.scopes.platform.infrastructure.grpc.EndpointResolver
import io.github.kamiazya.scopes.platform.observability.logging.Logger

/**
 * Factory for creating Transport instances based on configuration.
 */
class TransportFactory(
    private val scopeCommandAdapter: ScopeCommandAdapter?,
    private val scopeQueryAdapter: ScopeQueryAdapter?,
    private val aliasCommandAdapter: AliasCommandAdapter?,
    private val aliasQueryAdapter: AliasQueryAdapter?,
    private val contextCommandAdapter: ContextCommandAdapter?,
    private val contextQueryAdapter: ContextQueryAdapter?,
    private val parameterResolver: ScopeParameterResolver?,
    private val logger: Logger,
) {

    /**
     * Creates a transport instance based on the provided configuration.
     */
    fun create(config: TransportConfig): Transport = when (config.type) {
        TransportType.LOCAL -> createLocalTransport()
        TransportType.GRPC -> createGrpcTransport(config)
    }

    /**
     * Creates a transport instance based on environment configuration.
     */
    fun createFromEnvironment(): Transport {
        try {
            logger.info("TransportFactory.createFromEnvironment() called")

            val transportType = System.getenv("SCOPES_TRANSPORT")?.lowercase()
            val config = when (transportType) {
                "grpc" -> TransportConfig(
                    type = TransportType.GRPC,
                    grpcEndpoint = System.getenv("SCOPESD_ENDPOINT"),
                )
                else -> TransportConfig(type = TransportType.LOCAL)
            }

            logger.info(
                "Creating transport",
                mapOf(
                    "type" to config.type.name,
                    "grpcEndpoint" to (config.grpcEndpoint ?: "null"),
                ) as Map<String, Any>,
            )

            val transport = create(config)
            logger.info("Transport created successfully: ${transport::class.simpleName}")
            return transport
        } catch (e: Exception) {
            logger.error("Failed to create transport", mapOf("error" to (e.message ?: "Unknown error")), e)
            throw e
        }
    }

    private fun createLocalTransport(): LocalTransport {
        try {
            logger.info("Creating LocalTransport with adapters")

            val transport = LocalTransport(
                scopeCommandAdapter = scopeCommandAdapter
                    ?: throw IllegalStateException("ScopeCommandAdapter required for local transport"),
                scopeQueryAdapter = scopeQueryAdapter
                    ?: throw IllegalStateException("ScopeQueryAdapter required for local transport"),
                aliasCommandAdapter = aliasCommandAdapter
                    ?: throw IllegalStateException("AliasCommandAdapter required for local transport"),
                aliasQueryAdapter = aliasQueryAdapter
                    ?: throw IllegalStateException("AliasQueryAdapter required for local transport"),
                contextCommandAdapter = contextCommandAdapter
                    ?: throw IllegalStateException("ContextCommandAdapter required for local transport"),
                contextQueryAdapter = contextQueryAdapter
                    ?: throw IllegalStateException("ContextQueryAdapter required for local transport"),
                parameterResolver = parameterResolver
                    ?: throw IllegalStateException("ScopeParameterResolver required for local transport"),
                logger = logger,
            )

            logger.info("LocalTransport created successfully")
            return transport
        } catch (e: Exception) {
            logger.error("Failed to create LocalTransport", mapOf("error" to (e.message ?: "Unknown error")), e)
            throw e
        }
    }

    private fun createGrpcTransport(config: TransportConfig): GrpcTransport {
        // Note: Dependencies should be injected from DI container
        // This factory method should not manually create these instances
        throw UnsupportedOperationException("GrpcTransport creation requires DI injection - use TransportFactory from DI container")
    }
}

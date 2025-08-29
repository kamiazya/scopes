package io.github.kamiazya.scopes.eventstore.infrastructure.factory

/**
 * Factory for creating Event Store instances.
 * @deprecated Use eventStoreInfrastructureModule with Koin dependency injection
 */
@Deprecated(
    "Use eventStoreInfrastructureModule with Koin dependency injection",
    ReplaceWith("eventStoreInfrastructureModule", "io.github.kamiazya.scopes.eventstore.infrastructure.di.eventStoreInfrastructureModule"),
)
object EventStoreFactory

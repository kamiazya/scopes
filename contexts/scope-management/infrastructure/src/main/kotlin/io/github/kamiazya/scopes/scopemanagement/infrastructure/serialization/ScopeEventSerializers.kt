@file:OptIn(ExperimentalSerializationApi::class)

package io.github.kamiazya.scopes.scopemanagement.infrastructure.serialization

import arrow.core.toNonEmptyListOrNull
import io.github.kamiazya.scopes.scopemanagement.domain.event.*
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.*
import io.github.kamiazya.scopes.scopemanagement.infrastructure.serialization.ScopeEventSerializerHelpers.deserializeAggregateId
import io.github.kamiazya.scopes.scopemanagement.infrastructure.serialization.ScopeEventSerializerHelpers.deserializeAggregateVersion
import io.github.kamiazya.scopes.scopemanagement.infrastructure.serialization.ScopeEventSerializerHelpers.deserializeAliasId
import io.github.kamiazya.scopes.scopemanagement.infrastructure.serialization.ScopeEventSerializerHelpers.deserializeAliasName
import io.github.kamiazya.scopes.scopemanagement.infrastructure.serialization.ScopeEventSerializerHelpers.deserializeAspectKey
import io.github.kamiazya.scopes.scopemanagement.infrastructure.serialization.ScopeEventSerializerHelpers.deserializeAspectValue
import io.github.kamiazya.scopes.scopemanagement.infrastructure.serialization.ScopeEventSerializerHelpers.deserializeEventId
import io.github.kamiazya.scopes.scopemanagement.infrastructure.serialization.ScopeEventSerializerHelpers.deserializeScopeDescription
import io.github.kamiazya.scopes.scopemanagement.infrastructure.serialization.ScopeEventSerializerHelpers.deserializeScopeId
import io.github.kamiazya.scopes.scopemanagement.infrastructure.serialization.ScopeEventSerializerHelpers.deserializeScopeTitle
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Custom serializers for scope management domain events.
 *
 * These serializers handle the conversion between domain events and their
 * serializable representations, allowing domain classes to remain free
 * from serialization framework dependencies.
 */
object ScopeEventSerializers {

    object ScopeCreatedSerializer : KSerializer<ScopeCreated> {
        private val surrogateSerializer = SerializableScopeCreated.serializer()
        override val descriptor: SerialDescriptor = surrogateSerializer.descriptor

        override fun serialize(encoder: Encoder, value: ScopeCreated) {
            val surrogate = SerializableScopeCreated(
                aggregateId = value.aggregateId.value,
                eventId = value.eventId.value,
                occurredAt = value.occurredAt,
                aggregateVersion = value.aggregateVersion.value,
                metadata = value.metadata?.let { ScopeEventMappers.mapMetadata(it) },
                scopeId = value.scopeId.value,
                title = value.title.value,
                description = value.description?.value,
                parentId = value.parentId?.value,
            )
            encoder.encodeSerializableValue(surrogateSerializer, surrogate)
        }

        override fun deserialize(decoder: Decoder): ScopeCreated {
            val surrogate = decoder.decodeSerializableValue(surrogateSerializer)
            return ScopeCreated(
                aggregateId = deserializeAggregateId(surrogate.aggregateId),
                eventId = deserializeEventId(surrogate.eventId),
                occurredAt = surrogate.occurredAt,
                aggregateVersion = deserializeAggregateVersion(surrogate.aggregateVersion),
                metadata = surrogate.metadata?.let { ScopeEventMappers.mapMetadataFromSurrogate(it) },
                scopeId = deserializeScopeId(surrogate.scopeId),
                title = deserializeScopeTitle(surrogate.title),
                description = surrogate.description?.let { deserializeScopeDescription(it) },
                parentId = surrogate.parentId?.let { deserializeScopeId(it) },
            )
        }
    }

    object ScopeDeletedSerializer : KSerializer<ScopeDeleted> {
        private val surrogateSerializer = SerializableScopeDeleted.serializer()
        override val descriptor: SerialDescriptor = surrogateSerializer.descriptor

        override fun serialize(encoder: Encoder, value: ScopeDeleted) {
            val surrogate = SerializableScopeDeleted(
                aggregateId = value.aggregateId.value,
                eventId = value.eventId.value,
                occurredAt = value.occurredAt,
                aggregateVersion = value.aggregateVersion.value,
                metadata = value.metadata?.let { ScopeEventMappers.mapMetadata(it) },
                scopeId = value.scopeId.value,
            )
            encoder.encodeSerializableValue(surrogateSerializer, surrogate)
        }

        override fun deserialize(decoder: Decoder): ScopeDeleted {
            val surrogate = decoder.decodeSerializableValue(surrogateSerializer)
            return ScopeDeleted(
                aggregateId = deserializeAggregateId(surrogate.aggregateId),
                eventId = deserializeEventId(surrogate.eventId),
                occurredAt = surrogate.occurredAt,
                aggregateVersion = deserializeAggregateVersion(surrogate.aggregateVersion),
                metadata = surrogate.metadata?.let { ScopeEventMappers.mapMetadataFromSurrogate(it) },
                scopeId = deserializeScopeId(surrogate.scopeId),
            )
        }
    }

    object ScopeArchivedSerializer : KSerializer<ScopeArchived> {
        private val surrogateSerializer = SerializableScopeArchived.serializer()
        override val descriptor: SerialDescriptor = surrogateSerializer.descriptor

        override fun serialize(encoder: Encoder, value: ScopeArchived) {
            val surrogate = SerializableScopeArchived(
                aggregateId = value.aggregateId.value,
                eventId = value.eventId.value,
                occurredAt = value.occurredAt,
                aggregateVersion = value.aggregateVersion.value,
                metadata = value.metadata?.let { ScopeEventMappers.mapMetadata(it) },
                scopeId = value.scopeId.value,
                reason = value.reason,
            )
            encoder.encodeSerializableValue(surrogateSerializer, surrogate)
        }

        override fun deserialize(decoder: Decoder): ScopeArchived {
            val surrogate = decoder.decodeSerializableValue(surrogateSerializer)
            return ScopeArchived(
                aggregateId = deserializeAggregateId(surrogate.aggregateId),
                eventId = deserializeEventId(surrogate.eventId),
                occurredAt = surrogate.occurredAt,
                aggregateVersion = deserializeAggregateVersion(surrogate.aggregateVersion),
                metadata = surrogate.metadata?.let { ScopeEventMappers.mapMetadataFromSurrogate(it) },
                scopeId = deserializeScopeId(surrogate.scopeId),
                reason = surrogate.reason,
            )
        }
    }

    object ScopeRestoredSerializer : KSerializer<ScopeRestored> {
        private val surrogateSerializer = SerializableScopeRestored.serializer()
        override val descriptor: SerialDescriptor = surrogateSerializer.descriptor

        override fun serialize(encoder: Encoder, value: ScopeRestored) {
            val surrogate = SerializableScopeRestored(
                aggregateId = value.aggregateId.value,
                eventId = value.eventId.value,
                occurredAt = value.occurredAt,
                aggregateVersion = value.aggregateVersion.value,
                metadata = value.metadata?.let { ScopeEventMappers.mapMetadata(it) },
                scopeId = value.scopeId.value,
            )
            encoder.encodeSerializableValue(surrogateSerializer, surrogate)
        }

        override fun deserialize(decoder: Decoder): ScopeRestored {
            val surrogate = decoder.decodeSerializableValue(surrogateSerializer)
            return ScopeRestored(
                aggregateId = deserializeAggregateId(surrogate.aggregateId),
                eventId = deserializeEventId(surrogate.eventId),
                occurredAt = surrogate.occurredAt,
                aggregateVersion = deserializeAggregateVersion(surrogate.aggregateVersion),
                metadata = surrogate.metadata?.let { ScopeEventMappers.mapMetadataFromSurrogate(it) },
                scopeId = deserializeScopeId(surrogate.scopeId),
            )
        }
    }

    object ScopeTitleUpdatedSerializer : KSerializer<ScopeTitleUpdated> {
        private val surrogateSerializer = SerializableScopeTitleUpdated.serializer()
        override val descriptor: SerialDescriptor = surrogateSerializer.descriptor

        override fun serialize(encoder: Encoder, value: ScopeTitleUpdated) {
            val surrogate = SerializableScopeTitleUpdated(
                aggregateId = value.aggregateId.value,
                eventId = value.eventId.value,
                occurredAt = value.occurredAt,
                aggregateVersion = value.aggregateVersion.value,
                metadata = value.metadata?.let { ScopeEventMappers.mapMetadata(it) },
                scopeId = value.scopeId.value,
                oldTitle = value.oldTitle.value,
                newTitle = value.newTitle.value,
            )
            encoder.encodeSerializableValue(surrogateSerializer, surrogate)
        }

        override fun deserialize(decoder: Decoder): ScopeTitleUpdated {
            val surrogate = decoder.decodeSerializableValue(surrogateSerializer)
            return ScopeTitleUpdated(
                aggregateId = deserializeAggregateId(surrogate.aggregateId),
                eventId = deserializeEventId(surrogate.eventId),
                occurredAt = surrogate.occurredAt,
                aggregateVersion = deserializeAggregateVersion(surrogate.aggregateVersion),
                metadata = surrogate.metadata?.let { ScopeEventMappers.mapMetadataFromSurrogate(it) },
                scopeId = deserializeScopeId(surrogate.scopeId),
                oldTitle = deserializeScopeTitle(surrogate.oldTitle),
                newTitle = deserializeScopeTitle(surrogate.newTitle),
            )
        }
    }

    object ScopeDescriptionUpdatedSerializer : KSerializer<ScopeDescriptionUpdated> {
        private val surrogateSerializer = SerializableScopeDescriptionUpdated.serializer()
        override val descriptor: SerialDescriptor = surrogateSerializer.descriptor

        override fun serialize(encoder: Encoder, value: ScopeDescriptionUpdated) {
            val surrogate = SerializableScopeDescriptionUpdated(
                aggregateId = value.aggregateId.value,
                eventId = value.eventId.value,
                occurredAt = value.occurredAt,
                aggregateVersion = value.aggregateVersion.value,
                metadata = value.metadata?.let { ScopeEventMappers.mapMetadata(it) },
                scopeId = value.scopeId.value,
                oldDescription = value.oldDescription?.value,
                newDescription = value.newDescription?.value,
            )
            encoder.encodeSerializableValue(surrogateSerializer, surrogate)
        }

        override fun deserialize(decoder: Decoder): ScopeDescriptionUpdated {
            val surrogate = decoder.decodeSerializableValue(surrogateSerializer)
            return ScopeDescriptionUpdated(
                aggregateId = deserializeAggregateId(surrogate.aggregateId),
                eventId = deserializeEventId(surrogate.eventId),
                occurredAt = surrogate.occurredAt,
                aggregateVersion = deserializeAggregateVersion(surrogate.aggregateVersion),
                metadata = surrogate.metadata?.let { ScopeEventMappers.mapMetadataFromSurrogate(it) },
                scopeId = deserializeScopeId(surrogate.scopeId),
                oldDescription = surrogate.oldDescription?.let { deserializeScopeDescription(it) },
                newDescription = surrogate.newDescription?.let { deserializeScopeDescription(it) },
            )
        }
    }

    object ScopeParentChangedSerializer : KSerializer<ScopeParentChanged> {
        private val surrogateSerializer = SerializableScopeParentChanged.serializer()
        override val descriptor: SerialDescriptor = surrogateSerializer.descriptor

        override fun serialize(encoder: Encoder, value: ScopeParentChanged) {
            val surrogate = SerializableScopeParentChanged(
                aggregateId = value.aggregateId.value,
                eventId = value.eventId.value,
                occurredAt = value.occurredAt,
                aggregateVersion = value.aggregateVersion.value,
                metadata = value.metadata?.let { ScopeEventMappers.mapMetadata(it) },
                scopeId = value.scopeId.value,
                oldParentId = value.oldParentId?.value,
                newParentId = value.newParentId?.value,
            )
            encoder.encodeSerializableValue(surrogateSerializer, surrogate)
        }

        override fun deserialize(decoder: Decoder): ScopeParentChanged {
            val surrogate = decoder.decodeSerializableValue(surrogateSerializer)
            return ScopeParentChanged(
                aggregateId = deserializeAggregateId(surrogate.aggregateId),
                eventId = deserializeEventId(surrogate.eventId),
                occurredAt = surrogate.occurredAt,
                aggregateVersion = deserializeAggregateVersion(surrogate.aggregateVersion),
                metadata = surrogate.metadata?.let { ScopeEventMappers.mapMetadataFromSurrogate(it) },
                scopeId = deserializeScopeId(surrogate.scopeId),
                oldParentId = surrogate.oldParentId?.let { deserializeScopeId(it) },
                newParentId = surrogate.newParentId?.let { deserializeScopeId(it) },
            )
        }
    }

    object ScopeAspectAddedSerializer : KSerializer<ScopeAspectAdded> {
        private val surrogateSerializer = SerializableScopeAspectAdded.serializer()
        override val descriptor: SerialDescriptor = surrogateSerializer.descriptor

        override fun serialize(encoder: Encoder, value: ScopeAspectAdded) {
            val surrogate = SerializableScopeAspectAdded(
                aggregateId = value.aggregateId.value,
                eventId = value.eventId.value,
                occurredAt = value.occurredAt,
                aggregateVersion = value.aggregateVersion.value,
                metadata = value.metadata?.let { ScopeEventMappers.mapMetadata(it) },
                scopeId = value.scopeId.value,
                aspectKey = value.aspectKey.value,
                aspectValues = value.aspectValues.map { it.value },
            )
            encoder.encodeSerializableValue(surrogateSerializer, surrogate)
        }

        override fun deserialize(decoder: Decoder): ScopeAspectAdded {
            val surrogate = decoder.decodeSerializableValue(surrogateSerializer)
            val aspectValues = surrogate.aspectValues.map { deserializeAspectValue(it) }
            return ScopeAspectAdded(
                aggregateId = deserializeAggregateId(surrogate.aggregateId),
                eventId = deserializeEventId(surrogate.eventId),
                occurredAt = surrogate.occurredAt,
                aggregateVersion = deserializeAggregateVersion(surrogate.aggregateVersion),
                metadata = surrogate.metadata?.let { ScopeEventMappers.mapMetadataFromSurrogate(it) },
                scopeId = deserializeScopeId(surrogate.scopeId),
                aspectKey = deserializeAspectKey(surrogate.aspectKey),
                aspectValues = aspectValues.toNonEmptyListOrNull() ?: error("Aspect values list cannot be empty"),
            )
        }
    }

    object ScopeAspectRemovedSerializer : KSerializer<ScopeAspectRemoved> {
        private val surrogateSerializer = SerializableScopeAspectRemoved.serializer()
        override val descriptor: SerialDescriptor = surrogateSerializer.descriptor

        override fun serialize(encoder: Encoder, value: ScopeAspectRemoved) {
            val surrogate = SerializableScopeAspectRemoved(
                aggregateId = value.aggregateId.value,
                eventId = value.eventId.value,
                occurredAt = value.occurredAt,
                aggregateVersion = value.aggregateVersion.value,
                metadata = value.metadata?.let { ScopeEventMappers.mapMetadata(it) },
                scopeId = value.scopeId.value,
                aspectKey = value.aspectKey.value,
            )
            encoder.encodeSerializableValue(surrogateSerializer, surrogate)
        }

        override fun deserialize(decoder: Decoder): ScopeAspectRemoved {
            val surrogate = decoder.decodeSerializableValue(surrogateSerializer)
            return ScopeAspectRemoved(
                aggregateId = deserializeAggregateId(surrogate.aggregateId),
                eventId = deserializeEventId(surrogate.eventId),
                occurredAt = surrogate.occurredAt,
                aggregateVersion = deserializeAggregateVersion(surrogate.aggregateVersion),
                metadata = surrogate.metadata?.let { ScopeEventMappers.mapMetadataFromSurrogate(it) },
                scopeId = deserializeScopeId(surrogate.scopeId),
                aspectKey = deserializeAspectKey(surrogate.aspectKey),
            )
        }
    }

    object ScopeAspectsClearedSerializer : KSerializer<ScopeAspectsCleared> {
        private val surrogateSerializer = SerializableScopeAspectsCleared.serializer()
        override val descriptor: SerialDescriptor = surrogateSerializer.descriptor

        override fun serialize(encoder: Encoder, value: ScopeAspectsCleared) {
            val surrogate = SerializableScopeAspectsCleared(
                aggregateId = value.aggregateId.value,
                eventId = value.eventId.value,
                occurredAt = value.occurredAt,
                aggregateVersion = value.aggregateVersion.value,
                metadata = value.metadata?.let { ScopeEventMappers.mapMetadata(it) },
                scopeId = value.scopeId.value,
            )
            encoder.encodeSerializableValue(surrogateSerializer, surrogate)
        }

        override fun deserialize(decoder: Decoder): ScopeAspectsCleared {
            val surrogate = decoder.decodeSerializableValue(surrogateSerializer)
            return ScopeAspectsCleared(
                aggregateId = deserializeAggregateId(surrogate.aggregateId),
                eventId = deserializeEventId(surrogate.eventId),
                occurredAt = surrogate.occurredAt,
                aggregateVersion = deserializeAggregateVersion(surrogate.aggregateVersion),
                metadata = surrogate.metadata?.let { ScopeEventMappers.mapMetadataFromSurrogate(it) },
                scopeId = deserializeScopeId(surrogate.scopeId),
            )
        }
    }

    object ScopeAspectsUpdatedSerializer : KSerializer<ScopeAspectsUpdated> {
        private val surrogateSerializer = SerializableScopeAspectsUpdated.serializer()
        override val descriptor: SerialDescriptor = surrogateSerializer.descriptor

        override fun serialize(encoder: Encoder, value: ScopeAspectsUpdated) {
            val surrogate = SerializableScopeAspectsUpdated(
                aggregateId = value.aggregateId.value,
                eventId = value.eventId.value,
                occurredAt = value.occurredAt,
                aggregateVersion = value.aggregateVersion.value,
                metadata = value.metadata?.let { ScopeEventMappers.mapMetadata(it) },
                scopeId = value.scopeId.value,
                oldAspects = ScopeEventMappers.mapAspectsToSurrogate(value.oldAspects),
                newAspects = ScopeEventMappers.mapAspectsToSurrogate(value.newAspects),
            )
            encoder.encodeSerializableValue(surrogateSerializer, surrogate)
        }

        override fun deserialize(decoder: Decoder): ScopeAspectsUpdated {
            val surrogate = decoder.decodeSerializableValue(surrogateSerializer)
            return ScopeAspectsUpdated(
                aggregateId = deserializeAggregateId(surrogate.aggregateId),
                eventId = deserializeEventId(surrogate.eventId),
                occurredAt = surrogate.occurredAt,
                aggregateVersion = deserializeAggregateVersion(surrogate.aggregateVersion),
                metadata = surrogate.metadata?.let { ScopeEventMappers.mapMetadataFromSurrogate(it) },
                scopeId = deserializeScopeId(surrogate.scopeId),
                oldAspects = ScopeEventMappers.mapAspectsFromSurrogate(surrogate.oldAspects),
                newAspects = ScopeEventMappers.mapAspectsFromSurrogate(surrogate.newAspects),
            )
        }
    }

    // Alias event serializers

    object AliasAssignedSerializer : KSerializer<AliasAssigned> {
        private val surrogateSerializer = SerializableAliasAssigned.serializer()
        override val descriptor: SerialDescriptor = surrogateSerializer.descriptor

        override fun serialize(encoder: Encoder, value: AliasAssigned) {
            val surrogate = SerializableAliasAssigned(
                aggregateId = value.aggregateId.value,
                eventId = value.eventId.value,
                occurredAt = value.occurredAt,
                aggregateVersion = value.aggregateVersion.value,
                metadata = value.metadata?.let { ScopeEventMappers.mapMetadata(it) },
                aliasId = value.aliasId.value,
                aliasName = value.aliasName.value,
                scopeId = value.scopeId.value,
                aliasType = value.aliasType.name,
            )
            encoder.encodeSerializableValue(surrogateSerializer, surrogate)
        }

        override fun deserialize(decoder: Decoder): AliasAssigned {
            val surrogate = decoder.decodeSerializableValue(surrogateSerializer)
            return AliasAssigned(
                aggregateId = deserializeAggregateId(surrogate.aggregateId),
                eventId = deserializeEventId(surrogate.eventId),
                occurredAt = surrogate.occurredAt,
                aggregateVersion = deserializeAggregateVersion(surrogate.aggregateVersion),
                aliasId = deserializeAliasId(surrogate.aliasId),
                aliasName = deserializeAliasName(surrogate.aliasName),
                scopeId = deserializeScopeId(surrogate.scopeId),
                aliasType = AliasType.valueOf(surrogate.aliasType),
            )
        }
    }

    object AliasRemovedSerializer : KSerializer<AliasRemoved> {
        private val surrogateSerializer = SerializableAliasRemoved.serializer()
        override val descriptor: SerialDescriptor = surrogateSerializer.descriptor

        override fun serialize(encoder: Encoder, value: AliasRemoved) {
            val surrogate = SerializableAliasRemoved(
                aggregateId = value.aggregateId.value,
                eventId = value.eventId.value,
                occurredAt = value.occurredAt,
                aggregateVersion = value.aggregateVersion.value,
                metadata = value.metadata?.let { ScopeEventMappers.mapMetadata(it) },
                aliasId = value.aliasId.value,
                aliasName = value.aliasName.value,
                scopeId = value.scopeId.value,
                aliasType = value.aliasType.name,
                removedAt = value.removedAt,
            )
            encoder.encodeSerializableValue(surrogateSerializer, surrogate)
        }

        override fun deserialize(decoder: Decoder): AliasRemoved {
            val surrogate = decoder.decodeSerializableValue(surrogateSerializer)
            return AliasRemoved(
                aggregateId = deserializeAggregateId(surrogate.aggregateId),
                eventId = deserializeEventId(surrogate.eventId),
                occurredAt = surrogate.occurredAt,
                aggregateVersion = deserializeAggregateVersion(surrogate.aggregateVersion),
                aliasId = deserializeAliasId(surrogate.aliasId),
                aliasName = deserializeAliasName(surrogate.aliasName),
                scopeId = deserializeScopeId(surrogate.scopeId),
                aliasType = AliasType.valueOf(surrogate.aliasType),
                removedAt = surrogate.removedAt,
            )
        }
    }

    object AliasNameChangedSerializer : KSerializer<AliasNameChanged> {
        private val surrogateSerializer = SerializableAliasNameChanged.serializer()
        override val descriptor: SerialDescriptor = surrogateSerializer.descriptor

        override fun serialize(encoder: Encoder, value: AliasNameChanged) {
            val surrogate = SerializableAliasNameChanged(
                aggregateId = value.aggregateId.value,
                eventId = value.eventId.value,
                occurredAt = value.occurredAt,
                aggregateVersion = value.aggregateVersion.value,
                metadata = value.metadata?.let { ScopeEventMappers.mapMetadata(it) },
                aliasId = value.aliasId.value,
                scopeId = value.scopeId.value,
                oldAliasName = value.oldAliasName.value,
                newAliasName = value.newAliasName.value,
            )
            encoder.encodeSerializableValue(surrogateSerializer, surrogate)
        }

        override fun deserialize(decoder: Decoder): AliasNameChanged {
            val surrogate = decoder.decodeSerializableValue(surrogateSerializer)
            return AliasNameChanged(
                aggregateId = deserializeAggregateId(surrogate.aggregateId),
                eventId = deserializeEventId(surrogate.eventId),
                occurredAt = surrogate.occurredAt,
                aggregateVersion = deserializeAggregateVersion(surrogate.aggregateVersion),
                aliasId = deserializeAliasId(surrogate.aliasId),
                scopeId = deserializeScopeId(surrogate.scopeId),
                oldAliasName = deserializeAliasName(surrogate.oldAliasName),
                newAliasName = deserializeAliasName(surrogate.newAliasName),
            )
        }
    }

    object CanonicalAliasReplacedSerializer : KSerializer<CanonicalAliasReplaced> {
        private val surrogateSerializer = SerializableCanonicalAliasReplaced.serializer()
        override val descriptor: SerialDescriptor = surrogateSerializer.descriptor

        override fun serialize(encoder: Encoder, value: CanonicalAliasReplaced) {
            val surrogate = SerializableCanonicalAliasReplaced(
                aggregateId = value.aggregateId.value,
                eventId = value.eventId.value,
                occurredAt = value.occurredAt,
                aggregateVersion = value.aggregateVersion.value,
                metadata = value.metadata?.let { ScopeEventMappers.mapMetadata(it) },
                scopeId = value.scopeId.value,
                oldAliasId = value.oldAliasId.value,
                oldAliasName = value.oldAliasName.value,
                newAliasId = value.newAliasId.value,
                newAliasName = value.newAliasName.value,
            )
            encoder.encodeSerializableValue(surrogateSerializer, surrogate)
        }

        override fun deserialize(decoder: Decoder): CanonicalAliasReplaced {
            val surrogate = decoder.decodeSerializableValue(surrogateSerializer)
            return CanonicalAliasReplaced(
                aggregateId = deserializeAggregateId(surrogate.aggregateId),
                eventId = deserializeEventId(surrogate.eventId),
                occurredAt = surrogate.occurredAt,
                aggregateVersion = deserializeAggregateVersion(surrogate.aggregateVersion),
                scopeId = deserializeScopeId(surrogate.scopeId),
                oldAliasId = deserializeAliasId(surrogate.oldAliasId),
                oldAliasName = deserializeAliasName(surrogate.oldAliasName),
                newAliasId = deserializeAliasId(surrogate.newAliasId),
                newAliasName = deserializeAliasName(surrogate.newAliasName),
            )
        }
    }
}

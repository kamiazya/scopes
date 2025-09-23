package io.github.kamiazya.scopes.scopemanagement.domain.aggregate

import arrow.core.getOrElse
import arrow.core.nonEmptyListOf
import io.github.kamiazya.scopes.platform.domain.value.AggregateVersion
import io.github.kamiazya.scopes.platform.domain.value.EventId
import io.github.kamiazya.scopes.scopemanagement.domain.event.AliasAssigned
import io.github.kamiazya.scopes.scopemanagement.domain.event.AliasNameChanged
import io.github.kamiazya.scopes.scopemanagement.domain.event.AliasRemoved
import io.github.kamiazya.scopes.scopemanagement.domain.event.CanonicalAliasReplaced
import io.github.kamiazya.scopes.scopemanagement.domain.event.ScopeAspectAdded
import io.github.kamiazya.scopes.scopemanagement.domain.event.ScopeAspectRemoved
import io.github.kamiazya.scopes.scopemanagement.domain.event.ScopeAspectsCleared
import io.github.kamiazya.scopes.scopemanagement.domain.event.ScopeAspectsUpdated
import io.github.kamiazya.scopes.scopemanagement.domain.event.ScopeCreated
import io.github.kamiazya.scopes.scopemanagement.domain.event.ScopeDeleted
import io.github.kamiazya.scopes.scopemanagement.domain.event.ScopeDescriptionUpdated
import io.github.kamiazya.scopes.scopemanagement.domain.event.ScopeParentChanged
import io.github.kamiazya.scopes.scopemanagement.domain.event.ScopeTitleUpdated
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasId
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasName
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasType
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectKey
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectValue
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.Aspects
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeDescription
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeStatus
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeTitle
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.datetime.Clock

class ScopeAggregateTest :
    DescribeSpec({

        describe("ScopeAggregate event sourcing") {

            describe("Scope creation events") {
                it("should apply ScopeCreated event correctly") {
                    // Given
                    val scopeId = ScopeId.generate()
                    val aggregateId = scopeId.toAggregateId().getOrElse { throw RuntimeException(it.toString()) }
                    val title = ScopeTitle.create("Test Scope").getOrElse { throw RuntimeException(it.toString()) }
                    val description = ScopeDescription.create("Test Description").getOrElse { throw RuntimeException(it.toString()) }
                    val parentId = ScopeId.generate()
                    val now = Clock.System.now()

                    val aggregate = ScopeAggregate.empty(aggregateId)

                    val event = ScopeCreated(
                        aggregateId = aggregateId,
                        eventId = EventId.generate(),
                        occurredAt = now,
                        aggregateVersion = AggregateVersion.initial().increment(),
                        scopeId = scopeId,
                        title = title,
                        description = description,
                        parentId = parentId,
                    )

                    // When
                    val result = aggregate.applyEvent(event)

                    // Then
                    result.scopeId shouldBe scopeId
                    result.title shouldBe title
                    result.description shouldBe description
                    result.parentId shouldBe parentId
                    result.status shouldBe ScopeStatus.default()
                    result.aspects shouldBe Aspects.empty()
                    result.version.value.toLong() shouldBe 1L
                    result.createdAt shouldBe now
                    result.updatedAt shouldBe now
                }

                it("should apply ScopeTitleUpdated event correctly") {
                    // Given
                    val aggregate = createTestAggregate()
                    val oldTitle = aggregate.title!!
                    val newTitle = ScopeTitle.create("Updated Title").getOrElse { throw RuntimeException(it.toString()) }
                    val now = Clock.System.now()

                    val event = ScopeTitleUpdated(
                        aggregateId = aggregate.id,
                        eventId = EventId.generate(),
                        occurredAt = now,
                        aggregateVersion = AggregateVersion.initial().increment(),
                        scopeId = aggregate.scopeId!!,
                        oldTitle = oldTitle,
                        newTitle = newTitle,
                    )

                    // When
                    val result = aggregate.applyEvent(event)

                    // Then
                    result.title shouldBe newTitle
                    result.updatedAt shouldBe now
                    result.version.value.toLong() shouldBe (aggregate.version.value.toLong() + 1L)
                }

                it("should apply ScopeDescriptionUpdated event correctly") {
                    // Given
                    val aggregate = createTestAggregate()
                    val oldDescription = aggregate.description
                    val newDescription = ScopeDescription.create("Updated Description").getOrElse { throw RuntimeException(it.toString()) }
                    val now = Clock.System.now()

                    val event = ScopeDescriptionUpdated(
                        aggregateId = aggregate.id,
                        eventId = EventId.generate(),
                        occurredAt = now,
                        aggregateVersion = AggregateVersion.initial().increment(),
                        scopeId = aggregate.scopeId!!,
                        oldDescription = oldDescription,
                        newDescription = newDescription,
                    )

                    // When
                    val result = aggregate.applyEvent(event)

                    // Then
                    result.description shouldBe newDescription
                    result.updatedAt shouldBe now
                    result.version.value.toLong() shouldBe (aggregate.version.value.toLong() + 1L)
                }

                it("should apply ScopeParentChanged event correctly") {
                    // Given
                    val aggregate = createTestAggregate()
                    val oldParentId = aggregate.parentId
                    val newParentId = ScopeId.generate()
                    val now = Clock.System.now()

                    val event = ScopeParentChanged(
                        aggregateId = aggregate.id,
                        eventId = EventId.generate(),
                        occurredAt = now,
                        aggregateVersion = AggregateVersion.initial().increment(),
                        scopeId = aggregate.scopeId!!,
                        oldParentId = oldParentId,
                        newParentId = newParentId,
                    )

                    // When
                    val result = aggregate.applyEvent(event)

                    // Then
                    result.parentId shouldBe newParentId
                    result.updatedAt shouldBe now
                    result.version.value.toLong() shouldBe (aggregate.version.value.toLong() + 1L)
                }

                it("should apply ScopeDeleted event correctly") {
                    // Given
                    val aggregate = createTestAggregate()
                    val now = Clock.System.now()

                    val event = ScopeDeleted(
                        aggregateId = aggregate.id,
                        eventId = EventId.generate(),
                        occurredAt = now,
                        aggregateVersion = AggregateVersion.initial().increment(),
                        scopeId = aggregate.scopeId!!,
                    )

                    // When
                    val result = aggregate.applyEvent(event)

                    // Then
                    result.isDeleted shouldBe true
                    result.updatedAt shouldBe now
                    result.version.value.toLong() shouldBe (aggregate.version.value.toLong() + 1L)
                }
            }

            describe("Alias events") {
                it("should apply AliasAssigned event correctly") {
                    // Given
                    val aggregate = createTestAggregate()
                    val aliasId = AliasId.generate()
                    val aliasName = AliasName.create("test-alias").getOrElse { throw RuntimeException(it.toString()) }
                    val now = Clock.System.now()

                    val event = AliasAssigned(
                        aggregateId = aggregate.id,
                        eventId = EventId.generate(),
                        occurredAt = now,
                        aggregateVersion = AggregateVersion.initial().increment(),
                        aliasId = aliasId,
                        aliasName = aliasName,
                        scopeId = aggregate.scopeId!!,
                        aliasType = AliasType.CANONICAL,
                    )

                    // When
                    val result = aggregate.applyEvent(event)

                    // Then
                    result.aliases.size shouldBe 1
                    result.aliases[aliasId] shouldNotBe null
                    result.aliases[aliasId]!!.aliasName shouldBe aliasName
                    result.aliases[aliasId]!!.aliasType shouldBe AliasType.CANONICAL
                    result.canonicalAliasId shouldBe aliasId
                    result.updatedAt shouldBe now
                    result.version.value.toLong() shouldBe (aggregate.version.value.toLong() + 1L)
                }

                it("should apply AliasRemoved event correctly") {
                    // Given
                    val aggregate = createTestAggregateWithAlias()
                    val aliasId = aggregate.aliases.keys.first()
                    val aliasRecord = aggregate.aliases[aliasId]!!
                    val now = Clock.System.now()

                    val event = AliasRemoved(
                        aggregateId = aggregate.id,
                        eventId = EventId.generate(),
                        occurredAt = now,
                        aggregateVersion = AggregateVersion.initial().increment(),
                        aliasId = aliasId,
                        aliasName = aliasRecord.aliasName,
                        scopeId = aggregate.scopeId!!,
                        aliasType = aliasRecord.aliasType,
                        removedAt = now,
                    )

                    // When
                    val result = aggregate.applyEvent(event)

                    // Then
                    result.aliases shouldNotBe aggregate.aliases
                    result.aliases.containsKey(aliasId) shouldBe false
                    result.updatedAt shouldBe now
                    result.version.value.toLong() shouldBe (aggregate.version.value.toLong() + 1L)
                }

                it("should apply CanonicalAliasReplaced event correctly") {
                    // Given
                    val aggregate = createTestAggregateWithAlias()
                    val oldAliasId = aggregate.canonicalAliasId!!
                    val oldAliasRecord = aggregate.aliases[oldAliasId]!!
                    val newAliasId = AliasId.generate()
                    val newAliasName = AliasName.create("new-canonical-alias").getOrElse { throw RuntimeException(it.toString()) }
                    val now = Clock.System.now()

                    val event = CanonicalAliasReplaced(
                        aggregateId = aggregate.id,
                        eventId = EventId.generate(),
                        occurredAt = now,
                        aggregateVersion = AggregateVersion.initial().increment(),
                        scopeId = aggregate.scopeId!!,
                        oldAliasId = oldAliasId,
                        oldAliasName = oldAliasRecord.aliasName,
                        newAliasId = newAliasId,
                        newAliasName = newAliasName,
                    )

                    // When
                    val result = aggregate.applyEvent(event)

                    // Then
                    result.canonicalAliasId shouldBe newAliasId
                    result.aliases[oldAliasId]!!.aliasType shouldBe AliasType.CUSTOM
                    result.aliases[newAliasId]!!.aliasType shouldBe AliasType.CANONICAL
                    result.aliases[newAliasId]!!.aliasName shouldBe newAliasName
                    result.updatedAt shouldBe now
                    result.version.value.toLong() shouldBe (aggregate.version.value.toLong() + 1L)
                }

                it("should apply AliasNameChanged event correctly") {
                    // Given
                    val aggregate = createTestAggregateWithAlias()
                    val aliasId = aggregate.aliases.keys.first()
                    val oldAliasName = aggregate.aliases[aliasId]!!.aliasName
                    val newAliasName = AliasName.create("changed-alias-name").getOrElse { throw RuntimeException(it.toString()) }
                    val now = Clock.System.now()

                    val event = AliasNameChanged(
                        aggregateId = aggregate.id,
                        eventId = EventId.generate(),
                        occurredAt = now,
                        aggregateVersion = AggregateVersion.initial().increment(),
                        aliasId = aliasId,
                        scopeId = aggregate.scopeId!!,
                        oldAliasName = oldAliasName,
                        newAliasName = newAliasName,
                    )

                    // When
                    val result = aggregate.applyEvent(event)

                    // Then
                    result.aliases[aliasId]!!.aliasName shouldBe newAliasName
                    result.updatedAt shouldBe now
                    result.version.value.toLong() shouldBe (aggregate.version.value.toLong() + 1L)
                }
            }

            describe("Aspect events") {
                it("should apply ScopeAspectAdded event correctly") {
                    // Given
                    val aggregate = createTestAggregate()
                    val aspectKey = AspectKey.create("priority").getOrElse { throw RuntimeException(it.toString()) }
                    val aspectValues = nonEmptyListOf(AspectValue.create("high").getOrElse { throw RuntimeException(it.toString()) })
                    val now = Clock.System.now()

                    val event = ScopeAspectAdded(
                        aggregateId = aggregate.id,
                        eventId = EventId.generate(),
                        occurredAt = now,
                        aggregateVersion = AggregateVersion.initial().increment(),
                        scopeId = aggregate.scopeId!!,
                        aspectKey = aspectKey,
                        aspectValues = aspectValues,
                    )

                    // When
                    val result = aggregate.applyEvent(event)

                    // Then
                    result.aspects.contains(aspectKey) shouldBe true
                    result.aspects.get(aspectKey) shouldBe aspectValues
                    result.updatedAt shouldBe now
                    result.version.value.toLong() shouldBe (aggregate.version.value.toLong() + 1L)
                }

                it("should apply ScopeAspectRemoved event correctly") {
                    // Given
                    val aggregate = createTestAggregateWithAspects()
                    val aspectKey = AspectKey.create("priority").getOrElse { throw RuntimeException(it.toString()) }
                    val now = Clock.System.now()

                    val event = ScopeAspectRemoved(
                        aggregateId = aggregate.id,
                        eventId = EventId.generate(),
                        occurredAt = now,
                        aggregateVersion = AggregateVersion.initial().increment(),
                        scopeId = aggregate.scopeId!!,
                        aspectKey = aspectKey,
                    )

                    // When
                    val result = aggregate.applyEvent(event)

                    // Then
                    result.aspects.contains(aspectKey) shouldBe false
                    result.updatedAt shouldBe now
                    result.version.value.toLong() shouldBe (aggregate.version.value.toLong() + 1L)
                }

                it("should apply ScopeAspectsCleared event correctly") {
                    // Given
                    val aggregate = createTestAggregateWithAspects()
                    val now = Clock.System.now()

                    val event = ScopeAspectsCleared(
                        aggregateId = aggregate.id,
                        eventId = EventId.generate(),
                        occurredAt = now,
                        aggregateVersion = AggregateVersion.initial().increment(),
                        scopeId = aggregate.scopeId!!,
                    )

                    // When
                    val result = aggregate.applyEvent(event)

                    // Then
                    result.aspects shouldBe Aspects.empty()
                    result.updatedAt shouldBe now
                    result.version.value.toLong() shouldBe (aggregate.version.value.toLong() + 1L)
                }

                it("should apply ScopeAspectsUpdated event correctly") {
                    // Given
                    val aggregate = createTestAggregateWithAspects()
                    val newAspects = Aspects.of(
                        AspectKey.create("status").getOrElse { throw RuntimeException(it.toString()) } to
                            nonEmptyListOf(AspectValue.create("done").getOrElse { throw RuntimeException(it.toString()) }),
                    )
                    val now = Clock.System.now()

                    val event = ScopeAspectsUpdated(
                        aggregateId = aggregate.id,
                        eventId = EventId.generate(),
                        occurredAt = now,
                        aggregateVersion = AggregateVersion.initial().increment(),
                        scopeId = aggregate.scopeId!!,
                        oldAspects = aggregate.aspects,
                        newAspects = newAspects,
                    )

                    // When
                    val result = aggregate.applyEvent(event)

                    // Then
                    result.aspects shouldBe newAspects
                    result.updatedAt shouldBe now
                    result.version.value.toLong() shouldBe (aggregate.version.value.toLong() + 1L)
                }
            }
        }

        describe("ScopeAggregate business logic") {

            describe("create operations") {
                it("should create aggregate with proper initial state") {
                    // Given
                    val title = "Test Scope"
                    val description = "Test Description"
                    val parentId = ScopeId.generate()

                    // When
                    val result = ScopeAggregate.create(title, description, parentId)

                    // Then
                    result.isRight() shouldBe true
                    val aggregate = result.getOrElse { throw RuntimeException(it.toString()) }
                    aggregate.scopeId shouldNotBe null
                    aggregate.title shouldBe ScopeTitle.create(title).getOrElse { throw RuntimeException(it.toString()) }
                    aggregate.description shouldBe ScopeDescription.create(description).getOrElse { throw RuntimeException(it.toString()) }
                    aggregate.parentId shouldBe parentId
                    aggregate.status shouldBe ScopeStatus.default()
                    aggregate.aspects shouldBe Aspects.empty()
                    aggregate.isDeleted shouldBe false
                    aggregate.isArchived shouldBe false
                }
            }

            describe("alias operations") {
                it("should add alias correctly") {
                    // Given
                    val aggregate = createTestAggregate()
                    val aliasName = AliasName.create("test-alias").getOrElse { throw RuntimeException(it.toString()) }

                    // When
                    val result = aggregate.addAlias(aliasName)

                    // Then
                    result.isRight() shouldBe true
                    val updatedAggregate = result.getOrElse { throw RuntimeException(it.toString()) }
                    updatedAggregate.aliases.size shouldBe 1
                    updatedAggregate.canonicalAliasId shouldNotBe null
                }

                it("should not allow duplicate alias names") {
                    // Given
                    val aggregate = createTestAggregateWithAlias()
                    val existingAliasName = aggregate.aliases.values.first().aliasName

                    // When
                    val result = aggregate.addAlias(existingAliasName)

                    // Then
                    result.isLeft() shouldBe true
                }

                it("should find alias by name") {
                    // Given
                    val aggregate = createTestAggregateWithAlias()
                    val aliasName = aggregate.aliases.values.first().aliasName

                    // When
                    val result = aggregate.findAliasByName(aliasName)

                    // Then
                    result shouldNotBe null
                    result!!.aliasName shouldBe aliasName
                }
            }
        }
    })

private fun createTestAggregate(): ScopeAggregate {
    val scopeId = ScopeId.generate()
    val aggregateId = scopeId.toAggregateId().getOrElse { throw RuntimeException(it.toString()) }
    val title = ScopeTitle.create("Test Scope").getOrElse { throw RuntimeException(it.toString()) }
    val description = ScopeDescription.create("Test Description").getOrElse { throw RuntimeException(it.toString()) }
    val now = Clock.System.now()

    return ScopeAggregate(
        id = aggregateId,
        version = AggregateVersion.initial().increment(),
        createdAt = now,
        updatedAt = now,
        scopeId = scopeId,
        title = title,
        description = description,
        parentId = null,
        status = ScopeStatus.default(),
        aspects = Aspects.empty(),
        aliases = emptyMap(),
        canonicalAliasId = null,
        isDeleted = false,
        isArchived = false,
    )
}

private fun createTestAggregateWithAlias(): ScopeAggregate {
    val aggregate = createTestAggregate()
    val aliasId = AliasId.generate()
    val aliasName = AliasName.create("test-alias").getOrElse { throw RuntimeException(it.toString()) }
    val now = Clock.System.now()

    val aliasRecord = AliasRecord(
        aliasId = aliasId,
        aliasName = aliasName,
        aliasType = AliasType.CANONICAL,
        createdAt = now,
        updatedAt = now,
    )

    return aggregate.copy(
        aliases = mapOf(aliasId to aliasRecord),
        canonicalAliasId = aliasId,
    )
}

private fun createTestAggregateWithAspects(): ScopeAggregate {
    val aggregate = createTestAggregate()
    val aspects = Aspects.of(
        AspectKey.create("priority").getOrElse { throw RuntimeException(it.toString()) } to
            nonEmptyListOf(AspectValue.create("high").getOrElse { throw RuntimeException(it.toString()) }),
        AspectKey.create("type").getOrElse { throw RuntimeException(it.toString()) } to
            nonEmptyListOf(AspectValue.create("feature").getOrElse { throw RuntimeException(it.toString()) }),
    )

    return aggregate.copy(aspects = aspects)
}

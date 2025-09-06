package io.github.kamiazya.scopes.devicesync.domain.entity

import io.github.kamiazya.scopes.devicesync.domain.valueobject.ConflictType
import io.github.kamiazya.scopes.devicesync.domain.valueobject.DeviceId
import io.github.kamiazya.scopes.devicesync.domain.valueobject.ResolutionAction
import io.github.kamiazya.scopes.devicesync.domain.valueobject.VectorClock
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.datetime.Clock

class SyncConflictTest :
    DescribeSpec({
        describe("SyncConflict") {
            val deviceA = DeviceId("device-a")
            val deviceB = DeviceId("device-b")
            val localClock = VectorClock.single(deviceA, 5)
            val remoteClock = VectorClock.single(deviceB, 3)

            describe("isResolved and isPending") {
                it("should be pending when not resolved") {
                    val conflict = SyncConflict(
                        localEventId = "event-1",
                        remoteEventId = "event-2",
                        aggregateId = "aggregate-1",
                        localVersion = 2,
                        remoteVersion = 3,
                        localVectorClock = localClock,
                        remoteVectorClock = remoteClock,
                        conflictType = ConflictType.CONCURRENT_MODIFICATION,
                    )

                    conflict.isResolved() shouldBe false
                    conflict.isPending() shouldBe true
                }

                it("should be resolved when resolution is set") {
                    val conflict = SyncConflict(
                        localEventId = "event-1",
                        remoteEventId = "event-2",
                        aggregateId = "aggregate-1",
                        localVersion = 2,
                        remoteVersion = 3,
                        localVectorClock = localClock,
                        remoteVectorClock = remoteClock,
                        conflictType = ConflictType.CONCURRENT_MODIFICATION,
                        resolution = ResolutionAction.KEPT_LOCAL,
                        resolvedAt = Clock.System.now(),
                    )

                    conflict.isResolved() shouldBe true
                    conflict.isPending() shouldBe false
                }
            }

            describe("isTrueConflict") {
                it("should be true conflict for concurrent modifications") {
                    val concurrentLocal = VectorClock(mapOf("device-a" to 5L, "device-b" to 2L))
                    val concurrentRemote = VectorClock(mapOf("device-a" to 3L, "device-b" to 4L))

                    val conflict = SyncConflict(
                        localEventId = "event-1",
                        remoteEventId = "event-2",
                        aggregateId = "aggregate-1",
                        localVersion = 2,
                        remoteVersion = 3,
                        localVectorClock = concurrentLocal,
                        remoteVectorClock = concurrentRemote,
                        conflictType = ConflictType.CONCURRENT_MODIFICATION,
                    )

                    conflict.isTrueConflict() shouldBe true
                }

                it("should always be true for version mismatch") {
                    val conflict = SyncConflict(
                        localEventId = "event-1",
                        remoteEventId = "event-2",
                        aggregateId = "aggregate-1",
                        localVersion = 2,
                        remoteVersion = 3,
                        localVectorClock = localClock,
                        remoteVectorClock = remoteClock,
                        conflictType = ConflictType.VERSION_MISMATCH,
                    )

                    conflict.isTrueConflict() shouldBe true
                }

                it("should always be true for missing dependency") {
                    val conflict = SyncConflict(
                        localEventId = "event-1",
                        remoteEventId = "event-2",
                        aggregateId = "aggregate-1",
                        localVersion = 2,
                        remoteVersion = 3,
                        localVectorClock = localClock,
                        remoteVectorClock = remoteClock,
                        conflictType = ConflictType.MISSING_DEPENDENCY,
                    )

                    conflict.isTrueConflict() shouldBe true
                }
            }

            describe("severity") {
                it("should be CRITICAL for missing dependencies") {
                    val conflict = SyncConflict(
                        localEventId = "event-1",
                        remoteEventId = "event-2",
                        aggregateId = "aggregate-1",
                        localVersion = 2,
                        remoteVersion = 3,
                        localVectorClock = localClock,
                        remoteVectorClock = remoteClock,
                        conflictType = ConflictType.MISSING_DEPENDENCY,
                    )

                    conflict.severity() shouldBe ConflictSeverity.CRITICAL
                }

                it("should be HIGH for large version mismatches") {
                    val conflict = SyncConflict(
                        localEventId = "event-1",
                        remoteEventId = "event-2",
                        aggregateId = "aggregate-1",
                        localVersion = 2,
                        remoteVersion = 5,
                        localVectorClock = localClock,
                        remoteVectorClock = remoteClock,
                        conflictType = ConflictType.VERSION_MISMATCH,
                    )

                    conflict.severity() shouldBe ConflictSeverity.HIGH
                }

                it("should be MEDIUM for concurrent modifications") {
                    val conflict = SyncConflict(
                        localEventId = "event-1",
                        remoteEventId = "event-2",
                        aggregateId = "aggregate-1",
                        localVersion = 2,
                        remoteVersion = 3,
                        localVectorClock = localClock,
                        remoteVectorClock = remoteClock,
                        conflictType = ConflictType.CONCURRENT_MODIFICATION,
                    )

                    conflict.severity() shouldBe ConflictSeverity.MEDIUM
                }

                it("should be LOW for minor version mismatches") {
                    val conflict = SyncConflict(
                        localEventId = "event-1",
                        remoteEventId = "event-2",
                        aggregateId = "aggregate-1",
                        localVersion = 2,
                        remoteVersion = 3,
                        localVectorClock = localClock,
                        remoteVectorClock = remoteClock,
                        conflictType = ConflictType.VERSION_MISMATCH,
                    )

                    conflict.severity() shouldBe ConflictSeverity.LOW
                }
            }

            describe("suggestResolution") {
                it("should suggest ACCEPTED_REMOTE when local happened before remote") {
                    val localBefore = VectorClock.single(deviceA, 3)
                    val remoteAfter = VectorClock(mapOf("device-a" to 5L, "device-b" to 2L))

                    val conflict = SyncConflict(
                        localEventId = "event-1",
                        remoteEventId = "event-2",
                        aggregateId = "aggregate-1",
                        localVersion = 2,
                        remoteVersion = 3,
                        localVectorClock = localBefore,
                        remoteVectorClock = remoteAfter,
                        conflictType = ConflictType.CONCURRENT_MODIFICATION,
                    )

                    conflict.suggestResolution() shouldBe ResolutionAction.ACCEPTED_REMOTE
                }

                it("should suggest KEPT_LOCAL when remote happened before local") {
                    val localAfter = VectorClock(mapOf("device-a" to 5L, "device-b" to 4L))
                    val remoteBefore = VectorClock.single(deviceB, 3)

                    val conflict = SyncConflict(
                        localEventId = "event-1",
                        remoteEventId = "event-2",
                        aggregateId = "aggregate-1",
                        localVersion = 2,
                        remoteVersion = 3,
                        localVectorClock = localAfter,
                        remoteVectorClock = remoteBefore,
                        conflictType = ConflictType.CONCURRENT_MODIFICATION,
                    )

                    conflict.suggestResolution() shouldBe ResolutionAction.KEPT_LOCAL
                }

                it("should suggest DEFERRED for concurrent modifications") {
                    val concurrentLocal = VectorClock(mapOf("device-a" to 5L, "device-b" to 2L))
                    val concurrentRemote = VectorClock(mapOf("device-a" to 3L, "device-b" to 4L))

                    val conflict = SyncConflict(
                        localEventId = "event-1",
                        remoteEventId = "event-2",
                        aggregateId = "aggregate-1",
                        localVersion = 2,
                        remoteVersion = 3,
                        localVectorClock = concurrentLocal,
                        remoteVectorClock = concurrentRemote,
                        conflictType = ConflictType.CONCURRENT_MODIFICATION,
                    )

                    conflict.suggestResolution() shouldBe ResolutionAction.DEFERRED
                }

                it("should suggest DEFERRED for missing dependencies") {
                    val conflict = SyncConflict(
                        localEventId = "event-1",
                        remoteEventId = "event-2",
                        aggregateId = "aggregate-1",
                        localVersion = 2,
                        remoteVersion = 3,
                        localVectorClock = localClock,
                        remoteVectorClock = remoteClock,
                        conflictType = ConflictType.MISSING_DEPENDENCY,
                    )

                    conflict.suggestResolution() shouldBe ResolutionAction.DEFERRED
                }
            }

            describe("resolve") {
                it("should mark conflict as resolved") {
                    val conflict = SyncConflict(
                        localEventId = "event-1",
                        remoteEventId = "event-2",
                        aggregateId = "aggregate-1",
                        localVersion = 2,
                        remoteVersion = 3,
                        localVectorClock = localClock,
                        remoteVectorClock = remoteClock,
                        conflictType = ConflictType.CONCURRENT_MODIFICATION,
                    )

                    val resolved = conflict.resolve(ResolutionAction.KEPT_LOCAL)
                    resolved.resolution shouldBe ResolutionAction.KEPT_LOCAL
                    resolved.resolvedAt shouldNotBe null
                    resolved.isResolved() shouldBe true
                }

                it("should throw exception when already resolved") {
                    val conflict = SyncConflict(
                        localEventId = "event-1",
                        remoteEventId = "event-2",
                        aggregateId = "aggregate-1",
                        localVersion = 2,
                        remoteVersion = 3,
                        localVectorClock = localClock,
                        remoteVectorClock = remoteClock,
                        conflictType = ConflictType.CONCURRENT_MODIFICATION,
                        resolution = ResolutionAction.KEPT_LOCAL,
                        resolvedAt = Clock.System.now(),
                    )

                    shouldThrow<IllegalArgumentException> {
                        conflict.resolve(ResolutionAction.ACCEPTED_REMOTE)
                    }
                }
            }

            describe("defer") {
                it("should resolve as deferred") {
                    val conflict = SyncConflict(
                        localEventId = "event-1",
                        remoteEventId = "event-2",
                        aggregateId = "aggregate-1",
                        localVersion = 2,
                        remoteVersion = 3,
                        localVectorClock = localClock,
                        remoteVectorClock = remoteClock,
                        conflictType = ConflictType.CONCURRENT_MODIFICATION,
                    )

                    val deferred = conflict.defer()
                    deferred.resolution shouldBe ResolutionAction.DEFERRED
                    deferred.isResolved() shouldBe true
                }
            }

            describe("merge") {
                it("should resolve as merged") {
                    val conflict = SyncConflict(
                        localEventId = "event-1",
                        remoteEventId = "event-2",
                        aggregateId = "aggregate-1",
                        localVersion = 2,
                        remoteVersion = 3,
                        localVectorClock = localClock,
                        remoteVectorClock = remoteClock,
                        conflictType = ConflictType.CONCURRENT_MODIFICATION,
                    )

                    val merged = conflict.merge()
                    merged.resolution shouldBe ResolutionAction.MERGED
                    merged.isResolved() shouldBe true
                }
            }

            describe("detect") {
                it("should return null when local happened before remote") {
                    val localBefore = VectorClock.single(deviceA, 3)
                    val remoteAfter = VectorClock(mapOf("device-a" to 5L, "device-b" to 2L))

                    val conflict = SyncConflict.detect(
                        localEventId = "event-1",
                        remoteEventId = "event-2",
                        aggregateId = "aggregate-1",
                        localVersion = 2,
                        remoteVersion = 3,
                        localVectorClock = localBefore,
                        remoteVectorClock = remoteAfter,
                    )

                    conflict shouldBe null
                }

                it("should return null when remote happened before local") {
                    val localAfter = VectorClock(mapOf("device-a" to 5L, "device-b" to 4L))
                    val remoteBefore = VectorClock.single(deviceB, 3)

                    val conflict = SyncConflict.detect(
                        localEventId = "event-1",
                        remoteEventId = "event-2",
                        aggregateId = "aggregate-1",
                        localVersion = 2,
                        remoteVersion = 3,
                        localVectorClock = localAfter,
                        remoteVectorClock = remoteBefore,
                    )

                    conflict shouldBe null
                }

                it("should detect concurrent modification") {
                    val concurrentLocal = VectorClock(mapOf("device-a" to 5L, "device-b" to 2L))
                    val concurrentRemote = VectorClock(mapOf("device-a" to 3L, "device-b" to 4L))

                    val conflict = SyncConflict.detect(
                        localEventId = "event-1",
                        remoteEventId = "event-2",
                        aggregateId = "aggregate-1",
                        localVersion = 2,
                        remoteVersion = 3,
                        localVectorClock = concurrentLocal,
                        remoteVectorClock = concurrentRemote,
                    )

                    conflict shouldNotBe null
                    conflict!!.conflictType shouldBe ConflictType.CONCURRENT_MODIFICATION
                }

                it("should return null for identical clocks with same version") {
                    val clock = VectorClock.single(deviceA, 5)

                    val conflict = SyncConflict.detect(
                        localEventId = "event-1",
                        remoteEventId = "event-2",
                        aggregateId = "aggregate-1",
                        localVersion = 2,
                        remoteVersion = 2,
                        localVectorClock = clock,
                        remoteVectorClock = clock,
                    )

                    conflict shouldBe null
                }
            }
        }
    })

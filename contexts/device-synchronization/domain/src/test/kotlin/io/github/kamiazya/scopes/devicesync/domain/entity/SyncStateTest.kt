package io.github.kamiazya.scopes.devicesync.domain.entity

import io.github.kamiazya.scopes.devicesync.domain.valueobject.DeviceId
import io.github.kamiazya.scopes.devicesync.domain.valueobject.VectorClock
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class SyncStateTest :
    DescribeSpec({
        describe("SyncState") {
            val deviceId = DeviceId.generate()
            val vectorClock = VectorClock.empty()
            val testTime = Instant.parse("2024-01-01T12:00:00Z")
            val earlierTime = Instant.parse("2024-01-01T10:00:00Z")
            val laterTime = Instant.parse("2024-01-01T14:00:00Z")

            describe("needsSync") {
                it("should not need sync when in progress") {
                    val state = SyncState(
                        _deviceId = deviceId,
                        lastSyncAt = testTime,
                        remoteVectorClock = vectorClock,
                        lastSuccessfulPush = null,
                        lastSuccessfulPull = null,
                        syncStatus = SyncStatus.IN_PROGRESS,
                        pendingChanges = 10,
                    )
                    state.needsSync() shouldBe false
                }

                it("should not need sync when offline") {
                    val state = SyncState(
                        _deviceId = deviceId,
                        lastSyncAt = testTime,
                        remoteVectorClock = vectorClock,
                        lastSuccessfulPush = null,
                        lastSuccessfulPull = null,
                        syncStatus = SyncStatus.OFFLINE,
                        pendingChanges = 10,
                    )
                    state.needsSync() shouldBe false
                }

                it("should need sync when there are pending changes") {
                    val state = SyncState(
                        _deviceId = deviceId,
                        lastSyncAt = testTime,
                        remoteVectorClock = vectorClock,
                        lastSuccessfulPush = null,
                        lastSuccessfulPull = null,
                        syncStatus = SyncStatus.SUCCESS,
                        pendingChanges = 5,
                    )
                    state.needsSync() shouldBe true
                }

                it("should need sync when last sync failed") {
                    val state = SyncState(
                        _deviceId = deviceId,
                        lastSyncAt = testTime,
                        remoteVectorClock = vectorClock,
                        lastSuccessfulPush = null,
                        lastSuccessfulPull = null,
                        syncStatus = SyncStatus.FAILED,
                        pendingChanges = 0,
                    )
                    state.needsSync() shouldBe true
                }
            }

            describe("canSync") {
                it("should be able to sync when status is SUCCESS") {
                    val state = SyncState(
                        _deviceId = deviceId,
                        lastSyncAt = testTime,
                        remoteVectorClock = vectorClock,
                        lastSuccessfulPush = null,
                        lastSuccessfulPull = null,
                        syncStatus = SyncStatus.SUCCESS,
                    )
                    state.canSync() shouldBe true
                }

                it("should not be able to sync when already in progress") {
                    val state = SyncState(
                        _deviceId = deviceId,
                        lastSyncAt = testTime,
                        remoteVectorClock = vectorClock,
                        lastSuccessfulPush = null,
                        lastSuccessfulPull = null,
                        syncStatus = SyncStatus.IN_PROGRESS,
                    )
                    state.canSync() shouldBe false
                }

                it("should not be able to sync when offline") {
                    val state = SyncState(
                        _deviceId = deviceId,
                        lastSyncAt = null,
                        remoteVectorClock = vectorClock,
                        lastSuccessfulPush = null,
                        lastSuccessfulPull = null,
                        syncStatus = SyncStatus.OFFLINE,
                    )
                    state.canSync() shouldBe false
                }
            }

            describe("startSync") {
                it("should transition to IN_PROGRESS status") {
                    val state = SyncState(
                        _deviceId = deviceId,
                        lastSyncAt = null,
                        remoteVectorClock = vectorClock,
                        lastSuccessfulPush = null,
                        lastSuccessfulPull = null,
                        syncStatus = SyncStatus.NEVER_SYNCED,
                    )

                    val newState = state.startSync(testTime)
                    newState.syncStatus shouldBe SyncStatus.IN_PROGRESS
                    newState.lastSyncAt shouldBe testTime
                }

                it("should throw exception when cannot sync") {
                    val state = SyncState(
                        _deviceId = deviceId,
                        lastSyncAt = testTime,
                        remoteVectorClock = vectorClock,
                        lastSuccessfulPush = null,
                        lastSuccessfulPull = null,
                        syncStatus = SyncStatus.IN_PROGRESS,
                    )

                    shouldThrow<IllegalArgumentException> {
                        state.startSync(testTime)
                    }
                }
            }

            describe("markSyncSuccess") {
                it("should mark sync as successful with proper state updates") {
                    val state = SyncState(
                        _deviceId = deviceId,
                        lastSyncAt = laterTime,
                        remoteVectorClock = vectorClock,
                        lastSuccessfulPush = null,
                        lastSuccessfulPull = null,
                        syncStatus = SyncStatus.IN_PROGRESS,
                        pendingChanges = 5,
                    )

                    val newClock = vectorClock.increment(deviceId)
                    val newState = state.markSyncSuccess(
                        eventsPushed = 10,
                        eventsPulled = 5,
                        newRemoteVectorClock = newClock,
                        now = laterTime,
                    )

                    newState.syncStatus shouldBe SyncStatus.SUCCESS
                    newState.pendingChanges shouldBe 0
                    newState.remoteVectorClock shouldBe newClock
                    newState.lastSuccessfulPush shouldNotBe null
                    newState.lastSuccessfulPull shouldNotBe null
                }

                it("should not update push timestamp when no events pushed") {
                    val lastPush = earlierTime
                    val state = SyncState(
                        _deviceId = deviceId,
                        lastSyncAt = laterTime,
                        remoteVectorClock = vectorClock,
                        lastSuccessfulPush = lastPush,
                        lastSuccessfulPull = null,
                        syncStatus = SyncStatus.IN_PROGRESS,
                        pendingChanges = 0,
                    )

                    val newState = state.markSyncSuccess(
                        eventsPushed = 0,
                        eventsPulled = 5,
                        newRemoteVectorClock = vectorClock,
                        now = testTime,
                    )

                    newState.lastSuccessfulPush shouldBe lastPush
                    newState.lastSuccessfulPull shouldNotBe null
                }

                it("should throw exception when not in progress") {
                    val state = SyncState(
                        _deviceId = deviceId,
                        lastSyncAt = testTime,
                        remoteVectorClock = vectorClock,
                        lastSuccessfulPush = null,
                        lastSuccessfulPull = null,
                        syncStatus = SyncStatus.SUCCESS,
                    )

                    shouldThrow<IllegalArgumentException> {
                        state.markSyncSuccess(
                            eventsPushed = 10,
                            eventsPulled = 5,
                            newRemoteVectorClock = vectorClock,
                            now = testTime,
                        )
                    }
                }
            }

            describe("markSyncFailed") {
                it("should mark sync as failed") {
                    val state = SyncState(
                        _deviceId = deviceId,
                        lastSyncAt = testTime,
                        remoteVectorClock = vectorClock,
                        lastSuccessfulPush = null,
                        lastSuccessfulPull = null,
                        syncStatus = SyncStatus.IN_PROGRESS,
                    )

                    val newState = state.markSyncFailed("Network error")
                    newState.syncStatus shouldBe SyncStatus.FAILED
                }

                it("should throw exception when not in progress") {
                    val state = SyncState(
                        _deviceId = deviceId,
                        lastSyncAt = testTime,
                        remoteVectorClock = vectorClock,
                        lastSuccessfulPush = null,
                        lastSuccessfulPull = null,
                        syncStatus = SyncStatus.SUCCESS,
                    )

                    shouldThrow<IllegalArgumentException> {
                        state.markSyncFailed()
                    }
                }
            }

            describe("markOffline and markOnline") {
                it("should mark device as offline") {
                    val state = SyncState(
                        _deviceId = deviceId,
                        lastSyncAt = testTime,
                        remoteVectorClock = vectorClock,
                        lastSuccessfulPush = null,
                        lastSuccessfulPull = null,
                        syncStatus = SyncStatus.SUCCESS,
                    )

                    val offlineState = state.markOffline()
                    offlineState.syncStatus shouldBe SyncStatus.OFFLINE
                }

                it("should mark device as online with NEVER_SYNCED if never synced") {
                    val state = SyncState(
                        _deviceId = deviceId,
                        lastSyncAt = null,
                        remoteVectorClock = vectorClock,
                        lastSuccessfulPush = null,
                        lastSuccessfulPull = null,
                        syncStatus = SyncStatus.OFFLINE,
                    )

                    val onlineState = state.markOnline()
                    onlineState.syncStatus shouldBe SyncStatus.NEVER_SYNCED
                }

                it("should mark device as online with SUCCESS if previously synced") {
                    val state = SyncState(
                        _deviceId = deviceId,
                        lastSyncAt = testTime,
                        remoteVectorClock = vectorClock,
                        lastSuccessfulPush = null,
                        lastSuccessfulPull = null,
                        syncStatus = SyncStatus.OFFLINE,
                    )

                    val onlineState = state.markOnline()
                    onlineState.syncStatus shouldBe SyncStatus.SUCCESS
                }
            }

            describe("incrementPendingChanges") {
                it("should increment pending changes by specified count") {
                    val state = SyncState(
                        _deviceId = deviceId,
                        lastSyncAt = testTime,
                        remoteVectorClock = vectorClock,
                        lastSuccessfulPush = null,
                        lastSuccessfulPull = null,
                        syncStatus = SyncStatus.SUCCESS,
                        pendingChanges = 5,
                    )

                    val newState = state.incrementPendingChanges(3)
                    newState.pendingChanges shouldBe 8
                }

                it("should increment by 1 by default") {
                    val state = SyncState(
                        _deviceId = deviceId,
                        lastSyncAt = testTime,
                        remoteVectorClock = vectorClock,
                        lastSuccessfulPush = null,
                        lastSuccessfulPull = null,
                        syncStatus = SyncStatus.SUCCESS,
                        pendingChanges = 5,
                    )

                    val newState = state.incrementPendingChanges()
                    newState.pendingChanges shouldBe 6
                }

                it("should throw exception for non-positive count") {
                    val state = SyncState(
                        _deviceId = deviceId,
                        lastSyncAt = testTime,
                        remoteVectorClock = vectorClock,
                        lastSuccessfulPush = null,
                        lastSuccessfulPull = null,
                        syncStatus = SyncStatus.SUCCESS,
                    )

                    shouldThrow<IllegalArgumentException> {
                        state.incrementPendingChanges(0)
                    }

                    shouldThrow<IllegalArgumentException> {
                        state.incrementPendingChanges(-1)
                    }
                }
            }

            describe("timeSinceLastSync") {
                it("should calculate time since last sync") {
                    val lastSync = testTime - 2.hours
                    val state = SyncState(
                        _deviceId = deviceId,
                        lastSyncAt = lastSync,
                        remoteVectorClock = vectorClock,
                        lastSuccessfulPush = null,
                        lastSuccessfulPull = null,
                        syncStatus = SyncStatus.SUCCESS,
                    )

                    val currentTime = lastSync + 2.hours
                    val duration = state.timeSinceLastSync(currentTime)
                    duration shouldBe 2.hours
                    (duration!! >= (2.hours - 1.minutes)) shouldBe true
                }

                it("should return null if never synced") {
                    val state = SyncState(
                        _deviceId = deviceId,
                        lastSyncAt = null,
                        remoteVectorClock = vectorClock,
                        lastSuccessfulPush = null,
                        lastSuccessfulPull = null,
                        syncStatus = SyncStatus.NEVER_SYNCED,
                    )

                    state.timeSinceLastSync(testTime) shouldBe null
                }
            }

            describe("isStale") {
                it("should be stale if never synced") {
                    val state = SyncState(
                        _deviceId = deviceId,
                        lastSyncAt = null,
                        remoteVectorClock = vectorClock,
                        lastSuccessfulPush = null,
                        lastSuccessfulPull = null,
                        syncStatus = SyncStatus.NEVER_SYNCED,
                    )

                    state.isStale(1.hours, testTime) shouldBe true
                }

                it("should be stale if threshold exceeded") {
                    val lastSync = testTime - 2.hours
                    val state = SyncState(
                        _deviceId = deviceId,
                        lastSyncAt = lastSync,
                        remoteVectorClock = vectorClock,
                        lastSuccessfulPush = null,
                        lastSuccessfulPull = null,
                        syncStatus = SyncStatus.SUCCESS,
                    )

                    state.isStale(1.hours, testTime) shouldBe true
                }

                it("should not be stale if within threshold") {
                    val lastSync = testTime - 30.minutes
                    val state = SyncState(
                        _deviceId = deviceId,
                        lastSyncAt = lastSync,
                        remoteVectorClock = vectorClock,
                        lastSuccessfulPush = null,
                        lastSuccessfulPull = null,
                        syncStatus = SyncStatus.SUCCESS,
                    )

                    state.isStale(1.hours, testTime) shouldBe false
                }
            }
        }
    })

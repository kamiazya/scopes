package io.github.kamiazya.scopes.devicesync.infrastructure.repository

import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.devicesync.domain.entity.SyncState
import io.github.kamiazya.scopes.devicesync.domain.entity.SyncStatus
import io.github.kamiazya.scopes.devicesync.domain.error.SynchronizationError
import io.github.kamiazya.scopes.devicesync.domain.valueobject.DeviceId
import io.github.kamiazya.scopes.devicesync.domain.valueobject.VectorClock
import io.github.kamiazya.scopes.devicesync.infrastructure.sqldelight.SqlDelightDatabaseProvider
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock

class SqlDelightSynchronizationRepositoryTest :
    DescribeSpec({

        describe("SqlDelightSynchronizationRepository") {
            lateinit var repository: SqlDelightSynchronizationRepository
            lateinit var database: AutoCloseable

            beforeEach {
                val db = SqlDelightDatabaseProvider.createInMemoryDatabase()
                database = db
                repository = SqlDelightSynchronizationRepository(db.deviceQueries, db.vectorClockQueries)
            }

            afterEach {
                (database as? AutoCloseable)?.close()
            }

            describe("registerDevice") {
                it("should register a new device") {
                    // Given
                    val deviceId = DeviceId("test-device-001")

                    // When
                    val result = runBlocking { repository.registerDevice(deviceId) }

                    // Then
                    result shouldBe Unit.right()
                }

                it("should handle registering the same device twice") {
                    // Given
                    val deviceId = DeviceId("duplicate-device")
                    runBlocking { repository.registerDevice(deviceId) }

                    // When
                    val result = runBlocking { repository.registerDevice(deviceId) }

                    // Then
                    result shouldBe Unit.right() // Upsert behavior should handle duplicates
                }

                it("should initialize device with NEVER_SYNCED status") {
                    // Given
                    val deviceId = DeviceId("new-device")

                    // When
                    runBlocking { repository.registerDevice(deviceId) }
                    val syncState = runBlocking { repository.getSyncState(deviceId) }

                    // Then
                    syncState.isRight() shouldBe true
                    val state = syncState.getOrNull()
                    state?.syncStatus shouldBe SyncStatus.NEVER_SYNCED
                    state?.pendingChanges shouldBe 0
                    state?.lastSyncAt shouldBe null
                    state?.lastSuccessfulPush shouldBe null
                    state?.lastSuccessfulPull shouldBe null
                }
            }

            describe("unregisterDevice") {
                it("should unregister an existing device") {
                    // Given
                    val deviceId = DeviceId("device-to-remove")
                    runBlocking { repository.registerDevice(deviceId) }

                    // When
                    val result = runBlocking { repository.unregisterDevice(deviceId) }

                    // Then
                    result shouldBe Unit.right()
                }

                it("should remove device from known devices list") {
                    // Given
                    val deviceId = DeviceId("device-to-unregister")
                    runBlocking { repository.registerDevice(deviceId) }

                    // When
                    runBlocking { repository.unregisterDevice(deviceId) }
                    val devices = runBlocking { repository.listKnownDevices() }

                    // Then
                    devices.isRight() shouldBe true
                    devices.getOrNull() shouldNotBe null
                    devices.getOrNull()?.contains(deviceId) shouldBe false
                }

                it("should clean up vector clock entries when unregistering") {
                    // Given
                    val deviceId = DeviceId("device-with-clock")
                    val vectorClock = VectorClock(mapOf("device1" to 10L, "device2" to 20L))
                    
                    runBlocking {
                        repository.registerDevice(deviceId)
                        val syncState = SyncState(
                            deviceId = deviceId,
                            lastSyncAt = Clock.System.now(),
                            remoteVectorClock = vectorClock,
                            lastSuccessfulPush = null,
                            lastSuccessfulPull = null,
                            syncStatus = SyncStatus.UP_TO_DATE,
                            pendingChanges = 0,
                        )
                        repository.updateSyncState(syncState)
                    }

                    // When
                    runBlocking { repository.unregisterDevice(deviceId) }
                    val syncState = runBlocking { repository.getSyncState(deviceId) }

                    // Then
                    syncState.isLeft() shouldBe true
                    syncState.leftOrNull().shouldBeInstanceOf<SynchronizationError.InvalidDeviceError>()
                }
            }

            describe("getSyncState") {
                it("should retrieve sync state for registered device") {
                    // Given
                    val deviceId = DeviceId("sync-state-device")
                    val now = Clock.System.now()
                    
                    runBlocking {
                        repository.registerDevice(deviceId)
                        val syncState = SyncState(
                            deviceId = deviceId,
                            lastSyncAt = now,
                            remoteVectorClock = VectorClock(mapOf("device1" to 100L)),
                            lastSuccessfulPush = now,
                            lastSuccessfulPull = now,
                            syncStatus = SyncStatus.UP_TO_DATE,
                            pendingChanges = 5,
                        )
                        repository.updateSyncState(syncState)
                    }

                    // When
                    val result = runBlocking { repository.getSyncState(deviceId) }

                    // Then
                    result.isRight() shouldBe true
                    val state = result.getOrNull()
                    state?.deviceId shouldBe deviceId
                    state?.syncStatus shouldBe SyncStatus.UP_TO_DATE
                    state?.pendingChanges shouldBe 5
                    state?.lastSyncAt shouldNotBe null
                    state?.remoteVectorClock?.clocks?.get("device1") shouldBe 100L
                }

                it("should return error for non-existent device") {
                    // Given
                    val nonExistentDevice = DeviceId("non-existent")

                    // When
                    val result = runBlocking { repository.getSyncState(nonExistentDevice) }

                    // Then
                    result.isLeft() shouldBe true
                    val error = result.leftOrNull()
                    error.shouldBeInstanceOf<SynchronizationError.InvalidDeviceError>()
                    error.deviceId shouldBe nonExistentDevice.value
                    error.configurationIssue shouldBe SynchronizationError.ConfigurationIssue.INVALID_DEVICE_ID
                }
            }

            describe("updateSyncState") {
                it("should update all sync state fields") {
                    // Given
                    val deviceId = DeviceId("update-test-device")
                    val initialTime = Clock.System.now()
                    val updatedTime = initialTime.plus(kotlinx.datetime.DateTimeUnit.HOUR, 1)
                    
                    runBlocking { repository.registerDevice(deviceId) }

                    val updatedState = SyncState(
                        deviceId = deviceId,
                        lastSyncAt = updatedTime,
                        remoteVectorClock = VectorClock(mapOf("device1" to 50L, "device2" to 75L)),
                        lastSuccessfulPush = updatedTime,
                        lastSuccessfulPull = updatedTime,
                        syncStatus = SyncStatus.SYNCING,
                        pendingChanges = 10,
                    )

                    // When
                    val updateResult = runBlocking { repository.updateSyncState(updatedState) }
                    val retrievedState = runBlocking { repository.getSyncState(deviceId) }

                    // Then
                    updateResult shouldBe Unit.right()
                    retrievedState.isRight() shouldBe true
                    
                    val state = retrievedState.getOrNull()
                    state?.lastSyncAt shouldBe updatedTime
                    state?.lastSuccessfulPush shouldBe updatedTime
                    state?.lastSuccessfulPull shouldBe updatedTime
                    state?.syncStatus shouldBe SyncStatus.SYNCING
                    state?.pendingChanges shouldBe 10
                    state?.remoteVectorClock?.clocks shouldBe mapOf("device1" to 50L, "device2" to 75L)
                }

                it("should handle partial updates with null values") {
                    // Given
                    val deviceId = DeviceId("partial-update-device")
                    runBlocking { repository.registerDevice(deviceId) }

                    val partialState = SyncState(
                        deviceId = deviceId,
                        lastSyncAt = null,
                        remoteVectorClock = VectorClock(emptyMap()),
                        lastSuccessfulPush = null,
                        lastSuccessfulPull = Clock.System.now(),
                        syncStatus = SyncStatus.PUSH_REQUIRED,
                        pendingChanges = 3,
                    )

                    // When
                    val result = runBlocking { repository.updateSyncState(partialState) }

                    // Then
                    result shouldBe Unit.right()
                    
                    val state = runBlocking { repository.getSyncState(deviceId) }
                    state.getOrNull()?.lastSyncAt shouldBe null
                    state.getOrNull()?.lastSuccessfulPush shouldBe null
                    state.getOrNull()?.lastSuccessfulPull shouldNotBe null
                }
            }

            describe("getLocalVectorClock and updateLocalVectorClock") {
                it("should get and update local vector clock") {
                    // Given
                    val localClock = VectorClock(
                        mapOf(
                            "device1" to 100L,
                            "device2" to 200L,
                            "device3" to 300L,
                        ),
                    )

                    // When
                    val updateResult = runBlocking { repository.updateLocalVectorClock(localClock) }
                    val getResult = runBlocking { repository.getLocalVectorClock() }

                    // Then
                    updateResult shouldBe Unit.right()
                    getResult.isRight() shouldBe true
                    getResult.getOrNull()?.clocks shouldBe localClock.clocks
                }

                it("should return empty vector clock initially") {
                    // When
                    val result = runBlocking { repository.getLocalVectorClock() }

                    // Then
                    result.isRight() shouldBe true
                    result.getOrNull()?.clocks shouldBe emptyMap()
                }

                it("should handle vector clock updates correctly") {
                    // Given
                    val initialClock = VectorClock(mapOf("device1" to 10L, "device2" to 20L))
                    val updatedClock = VectorClock(mapOf("device1" to 15L, "device3" to 30L))

                    // When
                    runBlocking {
                        repository.updateLocalVectorClock(initialClock)
                        repository.updateLocalVectorClock(updatedClock)
                    }
                    val result = runBlocking { repository.getLocalVectorClock() }

                    // Then
                    result.isRight() shouldBe true
                    val clock = result.getOrNull()
                    clock?.clocks?.get("device1") shouldBe 15L // Updated
                    clock?.clocks?.containsKey("device2") shouldBe false // Removed
                    clock?.clocks?.get("device3") shouldBe 30L // Added
                }
            }

            describe("listKnownDevices") {
                it("should list all registered devices") {
                    // Given
                    val devices = listOf(
                        DeviceId("device-001"),
                        DeviceId("device-002"),
                        DeviceId("device-003"),
                    )

                    runBlocking {
                        devices.forEach { repository.registerDevice(it) }
                    }

                    // When
                    val result = runBlocking { repository.listKnownDevices() }

                    // Then
                    result.isRight() shouldBe true
                    val knownDevices = result.getOrNull()
                    knownDevices shouldHaveSize 3
                    devices.forEach { device ->
                        knownDevices shouldContain device
                    }
                }

                it("should return empty list when no devices registered") {
                    // When
                    val result = runBlocking { repository.listKnownDevices() }

                    // Then
                    result shouldBe emptyList<DeviceId>().right()
                }
            }

            describe("vector clock management") {
                it("should handle complex vector clock scenarios") {
                    // Given
                    val deviceId = DeviceId("complex-clock-device")
                    runBlocking { repository.registerDevice(deviceId) }

                    // Scenario: Multiple updates with different components
                    val clock1 = VectorClock(mapOf("A" to 1L, "B" to 2L, "C" to 3L))
                    val clock2 = VectorClock(mapOf("A" to 5L, "B" to 2L, "D" to 4L))
                    val clock3 = VectorClock(mapOf("E" to 10L))

                    // When
                    runBlocking {
                        val state1 = SyncState(
                            deviceId = deviceId,
                            lastSyncAt = Clock.System.now(),
                            remoteVectorClock = clock1,
                            lastSuccessfulPush = null,
                            lastSuccessfulPull = null,
                            syncStatus = SyncStatus.UP_TO_DATE,
                            pendingChanges = 0,
                        )
                        repository.updateSyncState(state1)

                        val state2 = state1.copy(remoteVectorClock = clock2)
                        repository.updateSyncState(state2)

                        val state3 = state1.copy(remoteVectorClock = clock3)
                        repository.updateSyncState(state3)
                    }

                    val finalState = runBlocking { repository.getSyncState(deviceId) }

                    // Then
                    finalState.isRight() shouldBe true
                    val vectorClock = finalState.getOrNull()?.remoteVectorClock
                    vectorClock?.clocks shouldBe mapOf("E" to 10L)
                }
            }

            describe("error handling") {
                it("should handle database errors gracefully") {
                    // Given
                    val deviceId = DeviceId("error-test-device")
                    (database as? AutoCloseable)?.close()

                    // When
                    val operations = listOf(
                        runBlocking { repository.registerDevice(deviceId) },
                        runBlocking { repository.getSyncState(deviceId) },
                        runBlocking { repository.updateSyncState(
                            SyncState(
                                deviceId = deviceId,
                                lastSyncAt = Clock.System.now(),
                                remoteVectorClock = VectorClock(emptyMap()),
                                lastSuccessfulPush = null,
                                lastSuccessfulPull = null,
                                syncStatus = SyncStatus.UP_TO_DATE,
                                pendingChanges = 0,
                            )
                        ) },
                        runBlocking { repository.getLocalVectorClock() },
                        runBlocking { repository.updateLocalVectorClock(VectorClock(emptyMap())) },
                        runBlocking { repository.listKnownDevices() },
                        runBlocking { repository.unregisterDevice(deviceId) },
                    )

                    // Then
                    operations.forEach { result ->
                        result.isLeft() shouldBe true
                        // Different operations return different error types
                        val error = result.leftOrNull()
                        error shouldNotBe null
                    }
                }
            }

            describe("sync status transitions") {
                it("should handle all sync status values") {
                    // Given
                    val deviceId = DeviceId("status-test-device")
                    runBlocking { repository.registerDevice(deviceId) }

                    val statuses = listOf(
                        SyncStatus.NEVER_SYNCED,
                        SyncStatus.UP_TO_DATE,
                        SyncStatus.SYNCING,
                        SyncStatus.PUSH_REQUIRED,
                        SyncStatus.PULL_REQUIRED,
                        SyncStatus.CONFLICT,
                        SyncStatus.ERROR,
                    )

                    // When/Then
                    statuses.forEach { status ->
                        val syncState = SyncState(
                            deviceId = deviceId,
                            lastSyncAt = Clock.System.now(),
                            remoteVectorClock = VectorClock(emptyMap()),
                            lastSuccessfulPush = null,
                            lastSuccessfulPull = null,
                            syncStatus = status,
                            pendingChanges = 0,
                        )

                        val updateResult = runBlocking { repository.updateSyncState(syncState) }
                        val getResult = runBlocking { repository.getSyncState(deviceId) }

                        updateResult shouldBe Unit.right()
                        getResult.getOrNull()?.syncStatus shouldBe status
                    }
                }
            }
        }
    })
package io.github.kamiazya.scopes.scopemanagement.eventstore.model

import io.github.kamiazya.scopes.scopemanagement.eventstore.valueobject.DeviceId
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class VectorClockTest :
    DescribeSpec({

        val deviceA = DeviceId("device-a")
        val deviceB = DeviceId("device-b")
        val deviceC = DeviceId("device-c")

        describe("VectorClock creation") {
            it("should create empty vector clock") {
                val clock = VectorClock.empty()
                clock.clocks shouldBe emptyMap()
            }

            it("should create single device clock") {
                val clock = VectorClock.single(deviceA, 5L)
                clock.getTimestamp(deviceA) shouldBe 5L
            }
        }

        describe("VectorClock increment") {
            it("should increment existing device clock") {
                val clock = VectorClock.single(deviceA, 5L)
                val incremented = clock.increment(deviceA)

                incremented.getTimestamp(deviceA) shouldBe 6L
            }

            it("should increment non-existing device clock from 0") {
                val clock = VectorClock.empty()
                val incremented = clock.increment(deviceA)

                incremented.getTimestamp(deviceA) shouldBe 1L
            }

            it("should not modify original clock") {
                val original = VectorClock.single(deviceA, 5L)
                val incremented = original.increment(deviceA)

                original.getTimestamp(deviceA) shouldBe 5L
                incremented.getTimestamp(deviceA) shouldBe 6L
            }
        }

        describe("VectorClock merge") {
            it("should merge non-overlapping clocks") {
                val clockA = VectorClock.single(deviceA, 3L)
                val clockB = VectorClock.single(deviceB, 2L)

                val merged = clockA.merge(clockB)

                merged.getTimestamp(deviceA) shouldBe 3L
                merged.getTimestamp(deviceB) shouldBe 2L
            }

            it("should take maximum value for overlapping devices") {
                val clock1 = VectorClock(
                    mapOf(
                        deviceA.value to 5L,
                        deviceB.value to 2L,
                    ),
                )
                val clock2 = VectorClock(
                    mapOf(
                        deviceA.value to 3L,
                        deviceB.value to 4L,
                        deviceC.value to 1L,
                    ),
                )

                val merged = clock1.merge(clock2)

                merged.getTimestamp(deviceA) shouldBe 5L
                merged.getTimestamp(deviceB) shouldBe 4L
                merged.getTimestamp(deviceC) shouldBe 1L
            }

            it("should be commutative") {
                val clock1 = VectorClock.single(deviceA, 3L)
                val clock2 = VectorClock.single(deviceB, 2L)

                val merged1 = clock1.merge(clock2)
                val merged2 = clock2.merge(clock1)

                merged1.clocks shouldBe merged2.clocks
            }
        }

        describe("VectorClock happened-before relationship") {
            it("should detect happened-before for single device") {
                val earlier = VectorClock.single(deviceA, 1L)
                val later = VectorClock.single(deviceA, 2L)

                earlier.happenedBefore(later) shouldBe true
                later.happenedBefore(earlier) shouldBe false
            }

            it("should detect happened-before for multiple devices") {
                val clock1 = VectorClock(
                    mapOf(
                        deviceA.value to 1L,
                        deviceB.value to 2L,
                    ),
                )
                val clock2 = VectorClock(
                    mapOf(
                        deviceA.value to 2L,
                        deviceB.value to 3L,
                    ),
                )

                clock1.happenedBefore(clock2) shouldBe true
                clock2.happenedBefore(clock1) shouldBe false
            }

            it("should not detect happened-before for concurrent events") {
                val clock1 = VectorClock(
                    mapOf(
                        deviceA.value to 2L,
                        deviceB.value to 1L,
                    ),
                )
                val clock2 = VectorClock(
                    mapOf(
                        deviceA.value to 1L,
                        deviceB.value to 2L,
                    ),
                )

                clock1.happenedBefore(clock2) shouldBe false
                clock2.happenedBefore(clock1) shouldBe false
            }

            it("should not consider identical clocks as happened-before") {
                val clock1 = VectorClock.single(deviceA, 5L)
                val clock2 = VectorClock.single(deviceA, 5L)

                clock1.happenedBefore(clock2) shouldBe false
                clock2.happenedBefore(clock1) shouldBe false
            }
        }

        describe("VectorClock concurrency detection") {
            it("should detect concurrent events") {
                val clock1 = VectorClock(
                    mapOf(
                        deviceA.value to 2L,
                        deviceB.value to 1L,
                    ),
                )
                val clock2 = VectorClock(
                    mapOf(
                        deviceA.value to 1L,
                        deviceB.value to 2L,
                    ),
                )

                clock1.isConcurrentWith(clock2) shouldBe true
                clock2.isConcurrentWith(clock1) shouldBe true
            }

            it("should not detect concurrency for happened-before relationship") {
                val earlier = VectorClock.single(deviceA, 1L)
                val later = VectorClock.single(deviceA, 2L)

                earlier.isConcurrentWith(later) shouldBe false
                later.isConcurrentWith(earlier) shouldBe false
            }

            it("should not detect concurrency for identical clocks") {
                val clock1 = VectorClock.single(deviceA, 5L)
                val clock2 = VectorClock.single(deviceA, 5L)

                clock1.isConcurrentWith(clock2) shouldBe false
            }
        }

        describe("VectorClock timestamp retrieval") {
            it("should return 0 for non-existing device") {
                val clock = VectorClock.single(deviceA, 5L)
                clock.getTimestamp(deviceB) shouldBe 0L
            }

            it("should return correct timestamp for existing device") {
                val clock = VectorClock.single(deviceA, 5L)
                clock.getTimestamp(deviceA) shouldBe 5L
            }
        }
    })

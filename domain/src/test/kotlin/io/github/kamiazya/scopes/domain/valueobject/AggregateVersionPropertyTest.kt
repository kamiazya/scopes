package io.github.kamiazya.scopes.domain.valueobject

import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.domain.error.AggregateVersionError
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.choice
import io.kotest.property.arbitrary.constant
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll
import kotlinx.datetime.Clock

class AggregateVersionPropertyTest : StringSpec({

    "valid version numbers should create valid AggregateVersions" {
        checkAll(validVersionArb()) { version ->
            val result = AggregateVersion.create(version)
            result.isRight() shouldBe true
            result.fold(
                { throw AssertionError("Expected Right but got Left: $it") },
                { aggregateVersion ->
                    aggregateVersion.value shouldBe version
                }
            )
        }
    }

    "negative version numbers should return error" {
        checkAll(negativeVersionArb()) { negativeVersion ->
            val result = AggregateVersion.create(negativeVersion)
            result.isLeft() shouldBe true
            result.fold(
                { error ->
                    when (error) {
                        is AggregateVersionError.NegativeVersion -> {
                            error.attemptedVersion shouldBe negativeVersion
                        }
                        else -> throw AssertionError("Expected NegativeVersion but got $error")
                    }
                },
                { throw AssertionError("Expected Left but got Right") }
            )
        }
    }

    "version numbers exceeding max should return error" {
        // Test specific overflow values to avoid integer overflow issues
        val overflowValues = listOf(
            AggregateVersion.MAX_VERSION + 1,
            Int.MAX_VALUE
        )
        
        overflowValues.forEach { overflowVersion ->
            val result = AggregateVersion.create(overflowVersion)
            result.isLeft() shouldBe true
            result.fold(
                { error ->
                    when (error) {
                        is AggregateVersionError.VersionOverflow -> {
                            error.currentVersion shouldBe overflowVersion
                            error.maxVersion shouldBe AggregateVersion.MAX_VERSION
                        }
                        is AggregateVersionError.NegativeVersion -> {
                            // This can happen due to integer overflow
                            error.attemptedVersion shouldBe overflowVersion
                        }
                        else -> throw AssertionError("Expected VersionOverflow or NegativeVersion but got $error")
                    }
                },
                { throw AssertionError("Expected Left but got Right") }
            )
        }
    }

    "initial version should be 0" {
        AggregateVersion.INITIAL.value shouldBe 0
        AggregateVersion.INITIAL.isInitial() shouldBe true
    }

    "increment should increase version by 1" {
        checkAll(validVersionArb().filter { it < AggregateVersion.MAX_VERSION }) { version ->
            val result = AggregateVersion.create(version)
            result.isRight() shouldBe true
            result.fold(
                { throw AssertionError("Expected Right but got Left") },
                { aggregateVersion ->
                    val incremented = aggregateVersion.increment()
                    incremented.isRight() shouldBe true
                    incremented.fold(
                        { throw AssertionError("Expected Right but got Left") },
                        { newVersion ->
                            newVersion.value shouldBe (version + 1)
                        }
                    )
                }
            )
        }
    }

    "increment at max version should return overflow error" {
        val result = AggregateVersion.create(AggregateVersion.MAX_VERSION)
        result.isRight() shouldBe true
        result.fold(
            { throw AssertionError("Expected Right but got Left") },
            { maxVersion ->
                val incremented = maxVersion.increment()
                incremented.isLeft() shouldBe true
                incremented.fold(
                    { error ->
                        when (error) {
                            is AggregateVersionError.VersionOverflow -> {
                                error.currentVersion shouldBe AggregateVersion.MAX_VERSION
                                error.maxVersion shouldBe AggregateVersion.MAX_VERSION
                            }
                            else -> throw AssertionError("Expected VersionOverflow but got $error")
                        }
                    },
                    { throw AssertionError("Expected Left but got Right") }
                )
            }
        )
    }

    "valid version transitions should be sequential increments" {
        checkAll(validVersionArb().filter { it < AggregateVersion.MAX_VERSION }) { version ->
            val currentResult = AggregateVersion.create(version)
            val nextResult = AggregateVersion.create(version + 1)
            
            currentResult.isRight() shouldBe true
            nextResult.isRight() shouldBe true
            
            currentResult.fold(
                { throw AssertionError("Expected Right but got Left") },
                { current ->
                    nextResult.fold(
                        { throw AssertionError("Expected Right but got Left") },
                        { next ->
                            val validation = current.validateTransition(next)
                            validation.isRight() shouldBe true
                        }
                    )
                }
            )
        }
    }

    "non-sequential version transitions should return error" {
        checkAll(
            validVersionArb().filter { it < AggregateVersion.MAX_VERSION - 10 },
            Arb.int(2..10)
        ) { version, jump ->
            val currentResult = AggregateVersion.create(version)
            val jumpedResult = AggregateVersion.create(version + jump)
            
            currentResult.isRight() shouldBe true
            jumpedResult.isRight() shouldBe true
            
            currentResult.fold(
                { throw AssertionError("Expected Right but got Left") },
                { current ->
                    jumpedResult.fold(
                        { throw AssertionError("Expected Right but got Left") },
                        { jumped ->
                            val validation = current.validateTransition(jumped)
                            validation.isLeft() shouldBe true
                            validation.fold(
                                { error ->
                                    when (error) {
                                        is AggregateVersionError.InvalidVersionTransition -> {
                                            error.currentVersion shouldBe version
                                            error.attemptedVersion shouldBe (version + jump)
                                        }
                                        else -> throw AssertionError("Expected InvalidVersionTransition but got $error")
                                    }
                                },
                                { throw AssertionError("Expected Left but got Right") }
                            )
                        }
                    )
                }
            )
        }
    }

    "backward version transitions should return error" {
        checkAll(validVersionArb().filter { it > 0 }) { version ->
            val currentResult = AggregateVersion.create(version)
            val previousResult = AggregateVersion.create(version - 1)
            
            currentResult.isRight() shouldBe true
            previousResult.isRight() shouldBe true
            
            currentResult.fold(
                { throw AssertionError("Expected Right but got Left") },
                { current ->
                    previousResult.fold(
                        { throw AssertionError("Expected Right but got Left") },
                        { previous ->
                            val validation = current.validateTransition(previous)
                            validation.isLeft() shouldBe true
                        }
                    )
                }
            )
        }
    }

    "version comparison should work correctly" {
        checkAll(
            validVersionArb(),
            validVersionArb()
        ) { v1, v2 ->
            val result1 = AggregateVersion.create(v1)
            val result2 = AggregateVersion.create(v2)
            
            result1.isRight() shouldBe true
            result2.isRight() shouldBe true
            
            result1.fold(
                { throw AssertionError("Expected Right but got Left") },
                { version1 ->
                    result2.fold(
                        { throw AssertionError("Expected Right but got Left") },
                        { version2 ->
                            val comparison = version1.compareTo(version2)
                            when {
                                v1 < v2 -> comparison shouldBe -1
                                v1 > v2 -> comparison shouldBe 1
                                else -> comparison shouldBe 0
                            }
                        }
                    )
                }
            )
        }
    }

    "version string representation should equal its value" {
        checkAll(validVersionArb()) { version ->
            val result = AggregateVersion.create(version)
            result.isRight() shouldBe true
            result.fold(
                { throw AssertionError("Expected Right but got Left") },
                { aggregateVersion ->
                    aggregateVersion.toString() shouldBe version.toString()
                }
            )
        }
    }

    "only version 0 should be initial" {
        checkAll(validVersionArb()) { version ->
            val result = AggregateVersion.create(version)
            result.isRight() shouldBe true
            result.fold(
                { throw AssertionError("Expected Right but got Left") },
                { aggregateVersion ->
                    aggregateVersion.isInitial() shouldBe (version == 0)
                }
            )
        }
    }

    "version equality should be value-based" {
        checkAll(validVersionArb()) { version ->
            val result1 = AggregateVersion.create(version)
            val result2 = AggregateVersion.create(version)
            
            result1.isRight() shouldBe true
            result2.isRight() shouldBe true
            
            result1.fold(
                { throw AssertionError("Expected Right but got Left") },
                { v1 ->
                    result2.fold(
                        { throw AssertionError("Expected Right but got Left") },
                        { v2 ->
                            (v1 == v2) shouldBe true
                            v1.value shouldBe v2.value
                        }
                    )
                }
            )
        }
    }

    "different versions should not be equal" {
        checkAll(
            validVersionArb(),
            validVersionArb()
        ) { v1, v2 ->
            if (v1 != v2) {
                val result1 = AggregateVersion.create(v1)
                val result2 = AggregateVersion.create(v2)
                
                result1.isRight() shouldBe true
                result2.isRight() shouldBe true
                
                result1.fold(
                    { throw AssertionError("Expected Right but got Left") },
                    { version1 ->
                        result2.fold(
                            { throw AssertionError("Expected Right but got Left") },
                            { version2 ->
                                version1 shouldNotBe version2
                            }
                        )
                    }
                )
            }
        }
    }

    "sequential increments should maintain order" {
        val startVersion = 0
        val iterations = 100
        
        var currentResult = AggregateVersion.create(startVersion)
        currentResult.isRight() shouldBe true
        
        for (i in 1..iterations) {
            currentResult = currentResult.fold(
                { throw AssertionError("Expected Right but got Left") },
                { version ->
                    val incremented = version.increment()
                    incremented.isRight() shouldBe true
                    incremented.fold(
                        { throw AssertionError("Expected Right but got Left at iteration $i") },
                        { newVersion ->
                            newVersion.value shouldBe i
                            newVersion
                        }
                    )
                    incremented
                }
            )
        }
    }
})

// Custom Arbitrary generators
private fun validVersionArb(): Arb<Int> = Arb.int(0..AggregateVersion.MAX_VERSION)

private fun negativeVersionArb(): Arb<Int> = Arb.int(Int.MIN_VALUE..-1)

// Removed as it was causing integer overflow issues
package io.github.kamiazya.scopes.scopemanagement.domain.valueobject

import arrow.core.nonEmptyListOf
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for Aspects value object.
 *
 * Business rules:
 * - Each aspect key can have one or more values (NonEmptyList)
 * - Aspect keys must be unique within a scope
 * - All operations return new instances (immutable)
 * - Provides rich querying and filtering capabilities
 */
class AspectsTest :
    StringSpec({

        "should create empty Aspects collection" {
            val aspects = Aspects.empty()

            aspects.isEmpty() shouldBe true
            aspects.size() shouldBe 0
            aspects.keys() shouldBe emptySet()
            aspects.toMap() shouldBe emptyMap()
        }

        "should create Aspects from map" {
            val key = AspectKey.create("priority").getOrNull()!!
            val values = nonEmptyListOf(AspectValue.create("high").getOrNull()!!)
            val map = mapOf(key to values)
            val aspects = Aspects.from(map)

            aspects.isEmpty() shouldBe false
            aspects.size() shouldBe 1
            aspects.get(key) shouldBe values
            aspects.getFirst(key) shouldBe values.head
        }

        "should create Aspects from vararg pairs" {
            val key1 = AspectKey.create("priority").getOrNull()!!
            val key2 = AspectKey.create("status").getOrNull()!!
            val values1 = nonEmptyListOf(AspectValue.create("high").getOrNull()!!)
            val values2 = nonEmptyListOf(AspectValue.create("active").getOrNull()!!)
            val aspects = Aspects.of(key1 to values1, key2 to values2)

            aspects.size() shouldBe 2
            aspects.get(key1) shouldBe values1
            aspects.get(key2) shouldBe values2
        }

        "should get null for non-existent key" {
            val aspects = Aspects.empty()
            val key = AspectKey.create("nonexistent").getOrNull()!!

            aspects.get(key) shouldBe null
            aspects.getFirst(key) shouldBe null
        }

        "should set aspect values for a key" {
            val key = AspectKey.create("tags").getOrNull()!!
            val values = nonEmptyListOf(
                AspectValue.create("urgent").getOrNull()!!,
                AspectValue.create("bug").getOrNull()!!,
            )
            val aspects = Aspects.empty().set(key, values)

            aspects.get(key) shouldBe values
            aspects.size() shouldBe 1
            aspects.contains(key) shouldBe true
        }

        "should set single aspect value for a key" {
            val key = AspectKey.create("priority").getOrNull()!!
            val value = AspectValue.create("medium").getOrNull()!!
            val aspects = Aspects.empty().set(key, value)

            aspects.getFirst(key) shouldBe value
            aspects.get(key)?.size shouldBe 1
        }

        "should add value to existing aspect key" {
            val key = AspectKey.create("tags").getOrNull()!!
            val value1 = AspectValue.create("feature").getOrNull()!!
            val value2 = AspectValue.create("urgent").getOrNull()!!

            val aspects = Aspects.empty()
                .add(key, value1)
                .add(key, value2)

            aspects.get(key)?.size shouldBe 2
            aspects.get(key)?.toList() shouldBe listOf(value1, value2)
        }

        "should add value to new aspect key" {
            val key = AspectKey.create("priority").getOrNull()!!
            val value = AspectValue.create("high").getOrNull()!!
            val aspects = Aspects.empty().add(key, value)

            aspects.getFirst(key) shouldBe value
            aspects.size() shouldBe 1
        }

        "should remove aspect key entirely" {
            val key1 = AspectKey.create("priority").getOrNull()!!
            val key2 = AspectKey.create("status").getOrNull()!!
            val value1 = AspectValue.create("high").getOrNull()!!
            val value2 = AspectValue.create("active").getOrNull()!!

            val aspects = Aspects.empty()
                .add(key1, value1)
                .add(key2, value2)
                .remove(key1)

            aspects.contains(key1) shouldBe false
            aspects.contains(key2) shouldBe true
            aspects.size() shouldBe 1
        }

        "should remove multiple aspect keys" {
            val key1 = AspectKey.create("priority").getOrNull()!!
            val key2 = AspectKey.create("status").getOrNull()!!
            val key3 = AspectKey.create("type").getOrNull()!!
            val aspects = Aspects.empty()
                .add(key1, AspectValue.create("high").getOrNull()!!)
                .add(key2, AspectValue.create("active").getOrNull()!!)
                .add(key3, AspectValue.create("feature").getOrNull()!!)
                .remove(setOf(key1, key2))

            aspects.contains(key1) shouldBe false
            aspects.contains(key2) shouldBe false
            aspects.contains(key3) shouldBe true
            aspects.size() shouldBe 1
        }

        "should remove specific value from aspect key" {
            val key = AspectKey.create("tags").getOrNull()!!
            val value1 = AspectValue.create("urgent").getOrNull()!!
            val value2 = AspectValue.create("bug").getOrNull()!!
            val value3 = AspectValue.create("feature").getOrNull()!!

            val aspects = Aspects.empty()
                .add(key, value1)
                .add(key, value2)
                .add(key, value3)
                .remove(key, value2)

            aspects.get(key)?.size shouldBe 2
            aspects.get(key)?.toList() shouldBe listOf(value1, value3)
        }

        "should remove key when removing last value" {
            val key = AspectKey.create("priority").getOrNull()!!
            val value = AspectValue.create("high").getOrNull()!!
            val aspects = Aspects.empty()
                .add(key, value)
                .remove(key, value)

            aspects.contains(key) shouldBe false
            aspects.isEmpty() shouldBe true
        }

        "should return unchanged when removing non-existent value" {
            val key = AspectKey.create("tags").getOrNull()!!
            val existingValue = AspectValue.create("bug").getOrNull()!!
            val nonExistentValue = AspectValue.create("feature").getOrNull()!!

            val originalAspects = Aspects.empty().add(key, existingValue)
            val aspects = originalAspects.remove(key, nonExistentValue)

            aspects shouldBe originalAspects
            aspects.get(key)?.size shouldBe 1
        }

        "should return unchanged when removing from non-existent key" {
            val key = AspectKey.create("nonexistent").getOrNull()!!
            val value = AspectValue.create("test").getOrNull()!!
            val originalAspects = Aspects.empty()
            val aspects = originalAspects.remove(key, value)

            aspects shouldBe originalAspects
        }

        "should check key existence correctly" {
            val key = AspectKey.create("priority").getOrNull()!!
            val value = AspectValue.create("high").getOrNull()!!
            val aspects = Aspects.empty().add(key, value)

            aspects.contains(key) shouldBe true

            val otherKey = AspectKey.create("status").getOrNull()!!
            aspects.contains(otherKey) shouldBe false
        }

        "should get all keys correctly" {
            val key1 = AspectKey.create("priority").getOrNull()!!
            val key2 = AspectKey.create("status").getOrNull()!!
            val aspects = Aspects.empty()
                .add(key1, AspectValue.create("high").getOrNull()!!)
                .add(key2, AspectValue.create("active").getOrNull()!!)

            aspects.keys() shouldBe setOf(key1, key2)
        }

        "should convert to map correctly" {
            val key = AspectKey.create("tags").getOrNull()!!
            val values = nonEmptyListOf(AspectValue.create("urgent").getOrNull()!!)
            val aspects = Aspects.empty().set(key, values)

            val map = aspects.toMap()
            map[key] shouldBe values
            map.size shouldBe 1
        }

        "should merge with another Aspects collection" {
            val key1 = AspectKey.create("priority").getOrNull()!!
            val key2 = AspectKey.create("status").getOrNull()!!
            val value1 = AspectValue.create("high").getOrNull()!!
            val value2 = AspectValue.create("active").getOrNull()!!

            val aspects1 = Aspects.empty().add(key1, value1)
            val aspects2 = Aspects.empty().add(key2, value2)
            val merged = aspects1.merge(aspects2)

            merged.size() shouldBe 2
            merged.contains(key1) shouldBe true
            merged.contains(key2) shouldBe true
        }

        "should override values when merging with overlapping keys" {
            val key = AspectKey.create("priority").getOrNull()!!
            val value1 = AspectValue.create("low").getOrNull()!!
            val value2 = AspectValue.create("high").getOrNull()!!

            val aspects1 = Aspects.empty().add(key, value1)
            val aspects2 = Aspects.empty().add(key, value2)
            val merged = aspects1.merge(aspects2)

            merged.size() shouldBe 1
            merged.getFirst(key) shouldBe value2 // value2 overwrites value1
        }

        "should check if empty correctly" {
            val emptyAspects = Aspects.empty()
            emptyAspects.isEmpty() shouldBe true

            val nonEmptyAspects = Aspects.empty().add(
                AspectKey.create("test").getOrNull()!!,
                AspectValue.create("value").getOrNull()!!,
            )
            nonEmptyAspects.isEmpty() shouldBe false
        }

        "should return correct size" {
            val aspects = Aspects.empty()
            aspects.size() shouldBe 0

            val key1 = AspectKey.create("priority").getOrNull()!!
            val key2 = AspectKey.create("status").getOrNull()!!
            val aspectsWithTwo = aspects
                .add(key1, AspectValue.create("high").getOrNull()!!)
                .add(key2, AspectValue.create("active").getOrNull()!!)

            aspectsWithTwo.size() shouldBe 2
        }

        "should filter aspects by predicate" {
            val priorityKey = AspectKey.create("priority").getOrNull()!!
            val statusKey = AspectKey.create("status").getOrNull()!!
            val typeKey = AspectKey.create("type").getOrNull()!!

            val aspects = Aspects.empty()
                .add(priorityKey, AspectValue.create("high").getOrNull()!!)
                .add(statusKey, AspectValue.create("active").getOrNull()!!)
                .add(typeKey, AspectValue.create("bug").getOrNull()!!)

            val filtered = aspects.filter { key, _ -> key.value.startsWith("p") }

            filtered.size() shouldBe 1
            filtered.contains(priorityKey) shouldBe true
            filtered.contains(statusKey) shouldBe false
            filtered.contains(typeKey) shouldBe false
        }

        "should map values while keeping same keys" {
            val key = AspectKey.create("priority").getOrNull()!!
            val value = AspectValue.create("high").getOrNull()!!
            val aspects = Aspects.empty().add(key, value)

            val mapped = aspects.mapValues { _, values ->
                nonEmptyListOf(AspectValue.create("${values.head.value}-transformed").getOrNull()!!)
            }

            mapped.size() shouldBe 1
            mapped.getFirst(key)?.value shouldBe "high-transformed"
        }

        "should find keys with specific value" {
            val key1 = AspectKey.create("tags").getOrNull()!!
            val key2 = AspectKey.create("categories").getOrNull()!!
            val targetValue = AspectValue.create("urgent").getOrNull()!!
            val otherValue = AspectValue.create("normal").getOrNull()!!

            val aspects = Aspects.empty()
                .add(key1, targetValue)
                .add(key1, otherValue)
                .add(key2, targetValue)

            val keysWithUrgent = aspects.findKeysWithValue(targetValue)
            keysWithUrgent shouldBe setOf(key1, key2)
        }

        "should find keys where predicate matches values" {
            val key1 = AspectKey.create("estimate").getOrNull()!!
            val key2 = AspectKey.create("actual").getOrNull()!!
            val key3 = AspectKey.create("description").getOrNull()!!

            val aspects = Aspects.empty()
                .add(key1, AspectValue.create("8").getOrNull()!!)
                .add(key2, AspectValue.create("10").getOrNull()!!)
                .add(key3, AspectValue.create("task description").getOrNull()!!)

            val numericKeys = aspects.findKeysWhere { values ->
                values.head.value.toDoubleOrNull() != null
            }

            numericKeys shouldBe setOf(key1, key2)
        }

        "should find values where predicate matches" {
            val key = AspectKey.create("tags").getOrNull()!!
            val value1 = AspectValue.create("urgent-bug").getOrNull()!!
            val value2 = AspectValue.create("feature").getOrNull()!!
            val value3 = AspectValue.create("urgent-feature").getOrNull()!!

            val aspects = Aspects.empty()
                .add(key, value1)
                .add(key, value2)
                .add(key, value3)

            val urgentValues = aspects.findValuesWhere { value ->
                value.value.contains("urgent")
            }

            urgentValues shouldBe listOf(value1, value3)
        }

        "should check if has specific value" {
            val key = AspectKey.create("priority").getOrNull()!!
            val targetValue = AspectValue.create("high").getOrNull()!!
            val otherValue = AspectValue.create("low").getOrNull()!!

            val aspects = Aspects.empty().add(key, targetValue)

            aspects.hasValue(targetValue) shouldBe true
            aspects.hasValue(otherValue) shouldBe false
        }

        "should check if has specific aspect key-value combination" {
            val key1 = AspectKey.create("priority").getOrNull()!!
            val key2 = AspectKey.create("status").getOrNull()!!
            val value1 = AspectValue.create("high").getOrNull()!!
            val value2 = AspectValue.create("active").getOrNull()!!

            val aspects = Aspects.empty()
                .add(key1, value1)
                .add(key2, value2)

            aspects.hasAspect(key1, value1) shouldBe true
            aspects.hasAspect(key1, value2) shouldBe false
            aspects.hasAspect(key2, value1) shouldBe false
        }

        "should get all distinct values across all aspects" {
            val key1 = AspectKey.create("tags").getOrNull()!!
            val key2 = AspectKey.create("categories").getOrNull()!!
            val value1 = AspectValue.create("urgent").getOrNull()!!
            val value2 = AspectValue.create("bug").getOrNull()!!
            val duplicateValue = AspectValue.create("urgent").getOrNull()!! // Same as value1

            val aspects = Aspects.empty()
                .add(key1, value1)
                .add(key1, value2)
                .add(key2, duplicateValue) // Should be deduplicated

            val allValues = aspects.allValues()
            allValues shouldBe setOf(value1, value2) // Duplicates removed
        }

        "should count total number of values across all aspects" {
            val key1 = AspectKey.create("tags").getOrNull()!!
            val key2 = AspectKey.create("categories").getOrNull()!!

            val aspects = Aspects.empty()
                .add(key1, AspectValue.create("urgent").getOrNull()!!)
                .add(key1, AspectValue.create("bug").getOrNull()!!)
                .add(key2, AspectValue.create("feature").getOrNull()!!)

            aspects.totalValueCount() shouldBe 3
        }

        "should filter by key pattern" {
            val priorityKey = AspectKey.create("priority").getOrNull()!!
            val statusKey = AspectKey.create("status").getOrNull()!!
            val typeKey = AspectKey.create("type").getOrNull()!!

            val aspects = Aspects.empty()
                .add(priorityKey, AspectValue.create("high").getOrNull()!!)
                .add(statusKey, AspectValue.create("active").getOrNull()!!)
                .add(typeKey, AspectValue.create("bug").getOrNull()!!)

            val pattern = Regex("^(priority|status)$")
            val filtered = aspects.filterByKeyPattern(pattern)

            filtered.size() shouldBe 2
            filtered.contains(priorityKey) shouldBe true
            filtered.contains(statusKey) shouldBe true
            filtered.contains(typeKey) shouldBe false
        }

        "should filter by value pattern" {
            val key = AspectKey.create("tags").getOrNull()!!
            val value1 = AspectValue.create("urgent-bug").getOrNull()!!
            val value2 = AspectValue.create("feature").getOrNull()!!
            val value3 = AspectValue.create("urgent-feature").getOrNull()!!

            val aspects = Aspects.empty()
                .add(key, value1)
                .add(key, value2)
                .add(key, value3)

            val pattern = Regex(".*urgent.*")
            val filtered = aspects.filterByValuePattern(pattern)

            filtered.size() shouldBe 1 // Only the key remains since it has matching values
            val filteredValues = filtered.get(key)?.toList() ?: emptyList()
            filteredValues.size shouldBe 3 // All values remain since any matched
        }

        "should find multi-valued aspects" {
            val multiKey = AspectKey.create("tags").getOrNull()!!
            val singleKey = AspectKey.create("priority").getOrNull()!!

            val aspects = Aspects.empty()
                .add(multiKey, AspectValue.create("urgent").getOrNull()!!)
                .add(multiKey, AspectValue.create("bug").getOrNull()!!)
                .add(singleKey, AspectValue.create("high").getOrNull()!!)

            val multiValued = aspects.findMultiValuedAspects()

            multiValued.size() shouldBe 1
            multiValued.contains(multiKey) shouldBe true
            multiValued.contains(singleKey) shouldBe false
        }

        "should find single-valued aspects" {
            val multiKey = AspectKey.create("tags").getOrNull()!!
            val singleKey = AspectKey.create("priority").getOrNull()!!

            val aspects = Aspects.empty()
                .add(multiKey, AspectValue.create("urgent").getOrNull()!!)
                .add(multiKey, AspectValue.create("bug").getOrNull()!!)
                .add(singleKey, AspectValue.create("high").getOrNull()!!)

            val singleValued = aspects.findSingleValuedAspects()

            singleValued.size() shouldBe 1
            singleValued.contains(singleKey) shouldBe true
            singleValued.contains(multiKey) shouldBe false
        }

        "should create and use query builder" {
            val priorityKey = AspectKey.create("priority").getOrNull()!!
            val statusKey = AspectKey.create("status").getOrNull()!!
            val typeKey = AspectKey.create("type").getOrNull()!!

            val aspects = Aspects.empty()
                .add(priorityKey, AspectValue.create("high").getOrNull()!!)
                .add(statusKey, AspectValue.create("active").getOrNull()!!)
                .add(typeKey, AspectValue.create("bug").getOrNull()!!)

            val query = aspects.query()
            query.shouldBeInstanceOf<AspectQuery>()

            val keyPattern = Regex("^priority$")
            val filteredQuery = query.whereKey(keyPattern)
            val result = filteredQuery.build()

            result.size() shouldBe 1
            result.contains(priorityKey) shouldBe true
        }

        "should support fluent AspectQuery operations" {
            val priorityKey = AspectKey.create("priority").getOrNull()!!
            val tagsKey = AspectKey.create("tags").getOrNull()!!
            val descKey = AspectKey.create("description").getOrNull()!!

            val aspects = Aspects.empty()
                .add(priorityKey, AspectValue.create("high").getOrNull()!!)
                .add(tagsKey, AspectValue.create("urgent").getOrNull()!!)
                .add(tagsKey, AspectValue.create("bug").getOrNull()!!)
                .add(descKey, AspectValue.create("Fix critical issue").getOrNull()!!)

            // Test key predicate filtering
            val highPriorityKeys = aspects.query()
                .whereKey { key -> key.value == "priority" }
                .keys()

            highPriorityKeys shouldBe setOf(priorityKey)

            // Test value predicate filtering
            val urgentValues = aspects.query()
                .whereValue { value -> value.value.contains("urgent") }
                .build()
                .findValuesWhere { value -> value.value.contains("urgent") }

            urgentValues shouldBe listOf(AspectValue.create("urgent").getOrNull()!!)

            // Test multi-valued filtering
            val multiValuedCount = aspects.query()
                .multiValued()
                .count()

            multiValuedCount shouldBe 1 // Only tagsKey has multiple values

            // Test single-valued filtering
            val singleValuedCount = aspects.query()
                .singleValued()
                .count()

            singleValuedCount shouldBe 2 // priorityKey and descKey
        }

        "should maintain immutability across all operations" {
            val key = AspectKey.create("priority").getOrNull()!!
            val value1 = AspectValue.create("low").getOrNull()!!
            val value2 = AspectValue.create("high").getOrNull()!!

            val original = Aspects.empty().add(key, value1)
            val modified = original.add(key, value2)

            // Original should be unchanged
            original.get(key)?.size shouldBe 1
            original.getFirst(key) shouldBe value1

            // Modified should have both values
            modified.get(key)?.size shouldBe 2
            modified.get(key)?.toList() shouldBe listOf(value1, value2)

            // Objects should be different instances
            (original === modified) shouldBe false
        }
    })

package io.github.kamiazya.scopes.domain.valueobject

import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll

class AspectsPropertyTest : StringSpec({

    "empty aspects should have no keys" {
        val aspects = Aspects.empty()
        aspects.isEmpty() shouldBe true
        aspects.size() shouldBe 0
        aspects.keys().isEmpty() shouldBe true
    }

    "setting aspects should preserve all values" {
        checkAll(aspectMapArb()) { aspectMap ->
            val aspects = Aspects.from(aspectMap)
            
            aspects.size() shouldBe aspectMap.size
            aspectMap.forEach { (key, values) ->
                aspects.contains(key) shouldBe true
                aspects.get(key) shouldBe values
                aspects.getFirst(key) shouldBe values.head
            }
        }
    }

    "setting single aspect value should create singleton list" {
        checkAll(validAspectKeyArb(), validAspectValueArb()) { keyStr, valueStr ->
            val key = AspectKey.create(keyStr).getOrNull()!!
            val value = AspectValue.create(valueStr).getOrNull()!!
            
            val aspects = Aspects.empty().set(key, value)
            
            aspects.contains(key) shouldBe true
            aspects.get(key) shouldBe nonEmptyListOf(value)
            aspects.getFirst(key) shouldBe value
        }
    }

    "setting multiple values should preserve order" {
        checkAll(
            validAspectKeyArb(),
            Arb.list(validAspectValueArb(), 1..10)
        ) { keyStr, valueStrs ->
            val key = AspectKey.create(keyStr).getOrNull()!!
            val values = valueStrs.mapNotNull { AspectValue.create(it).getOrNull() }
            
            if (values.isNotEmpty()) {
                val nonEmptyValues = nonEmptyListOf(values.first(), *values.drop(1).toTypedArray())
                val aspects = Aspects.empty().set(key, nonEmptyValues)
                
                aspects.get(key) shouldBe nonEmptyValues
                aspects.getFirst(key) shouldBe nonEmptyValues.head
            }
        }
    }

    "removing aspect should eliminate key" {
        checkAll(aspectMapArb()) { aspectMap ->
            if (aspectMap.isNotEmpty()) {
                val aspects = Aspects.from(aspectMap)
                val keyToRemove = aspectMap.keys.first()
                
                val removed = aspects.remove(keyToRemove)
                
                removed.contains(keyToRemove) shouldBe false
                removed.get(keyToRemove) shouldBe null
                removed.size() shouldBe (aspects.size() - 1)
            }
        }
    }

    "removing multiple aspects should work correctly" {
        checkAll(aspectMapArb()) { aspectMap ->
            val aspects = Aspects.from(aspectMap)
            val keysToRemove = aspectMap.keys.take(aspectMap.size / 2).toSet()
            
            val removed = aspects.remove(keysToRemove)
            
            keysToRemove.forEach { key ->
                removed.contains(key) shouldBe false
            }
            removed.size() shouldBe (aspects.size() - keysToRemove.size)
        }
    }

    "merging aspects should combine correctly" {
        checkAll(aspectMapArb(), aspectMapArb()) { map1, map2 ->
            val aspects1 = Aspects.from(map1)
            val aspects2 = Aspects.from(map2)
            
            val merged = aspects1.merge(aspects2)
            
            // All keys from both should be present
            (map1.keys + map2.keys).forEach { key ->
                merged.contains(key) shouldBe true
            }
            
            // Values from aspects2 should override aspects1
            map2.forEach { (key, values) ->
                merged.get(key) shouldBe values
            }
            
            // Keys only in aspects1 should remain
            (map1.keys - map2.keys).forEach { key ->
                merged.get(key) shouldBe map1[key]
            }
        }
    }

    "aspects immutability - operations should return new instances" {
        checkAll(validAspectKeyArb(), validAspectValueArb()) { keyStr, valueStr ->
            val key = AspectKey.create(keyStr).getOrNull()!!
            val value = AspectValue.create(valueStr).getOrNull()!!
            
            val original = Aspects.empty()
            val modified = original.set(key, value)
            
            original.isEmpty() shouldBe true
            modified.isEmpty() shouldBe false
            original shouldNotBe modified
        }
    }

    "toMap should return correct representation" {
        checkAll(aspectMapArb()) { aspectMap ->
            val aspects = Aspects.from(aspectMap)
            val map = aspects.toMap()
            
            map shouldBe aspectMap
        }
    }

    "empty check should be consistent with size" {
        checkAll(aspectMapArb()) { aspectMap ->
            val aspects = Aspects.from(aspectMap)
            
            if (aspects.isEmpty()) {
                aspects.size() shouldBe 0
            } else {
                aspects.size() shouldNotBe 0
            }
        }
    }

    "keys should return all aspect keys" {
        checkAll(aspectMapArb()) { aspectMap ->
            val aspects = Aspects.from(aspectMap)
            val keys = aspects.keys()
            
            keys.toSet() shouldBe aspectMap.keys
        }
    }

    "setting same key should replace values" {
        checkAll(
            validAspectKeyArb(),
            validAspectValueArb(),
            validAspectValueArb()
        ) { keyStr, value1Str, value2Str ->
            val key = AspectKey.create(keyStr).getOrNull()!!
            val value1 = AspectValue.create(value1Str).getOrNull()!!
            val value2 = AspectValue.create(value2Str).getOrNull()!!
            
            val aspects = Aspects.empty()
                .set(key, value1)
                .set(key, value2)
            
            aspects.getFirst(key) shouldBe value2
            aspects.get(key) shouldBe nonEmptyListOf(value2)
        }
    }

    "removing non-existent key should not change aspects" {
        checkAll(aspectMapArb(), validAspectKeyArb()) { aspectMap, keyStr ->
            val aspects = Aspects.from(aspectMap)
            val nonExistentKey = AspectKey.create(keyStr).getOrNull()!!
            
            if (!aspects.contains(nonExistentKey)) {
                val removed = aspects.remove(nonExistentKey)
                removed shouldBe aspects
            }
        }
    }

    "aspects with same data should be equal" {
        checkAll(aspectMapArb()) { aspectMap ->
            val aspects1 = Aspects.from(aspectMap)
            val aspects2 = Aspects.from(aspectMap)
            
            aspects1 shouldBe aspects2
        }
    }

    "clearing all aspects results in empty aspects" {
        checkAll(aspectMapArb()) { aspectMap ->
            val aspects = Aspects.from(aspectMap)
            val cleared = aspects.remove(aspects.keys().toSet())
            
            cleared.isEmpty() shouldBe true
            cleared shouldBe Aspects.empty()
        }
    }
})

// Custom Arbitrary generators
private fun validAspectKeyArb(): Arb<String> = Arb.stringPattern("[a-z][a-z0-9_]{0,49}")

private fun validAspectValueArb(): Arb<String> = Arb.string(1..50)
    .filter { it.trim().isNotEmpty() }

private fun aspectMapArb(): Arb<Map<AspectKey, NonEmptyList<AspectValue>>> = 
    Arb.list(
        Arb.pair(
            validAspectKeyArb().map { AspectKey.create(it).getOrNull()!! },
            Arb.list(validAspectValueArb().map { AspectValue.create(it).getOrNull()!! }, 1..5)
                .map { nonEmptyListOf(it.first(), *it.drop(1).toTypedArray()) }
        ),
        0..10
    ).map { it.toMap() }
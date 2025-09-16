package io.github.kamiazya.scopes.interfaces.mcp.tools

import io.github.kamiazya.scopes.interfaces.mcp.tools.handlers.*
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldNotBeNull

class ToolAnnotationsTest :
    StringSpec({

        "read-only tools expose readOnlyHint" {
            listOf(
                ScopeGetToolHandler(),
                ScopeChildrenToolHandler(),
                ScopesRootsToolHandler(),
                ScopesListAliasesToolHandler(),
                AliasResolveToolHandler(),
            ).forEach { h ->
                val ann = h.annotations.shouldNotBeNull()
                ann.readOnlyHint.shouldBeTrue()
                ann.destructiveHint.shouldBeFalse()
            }
        }

        "destructive tools expose destructiveHint" {
            listOf(
                ScopeCreateToolHandler(),
                ScopeUpdateToolHandler(),
                ScopeDeleteToolHandler(),
                AliasesAddToolHandler(),
                AliasesRemoveToolHandler(),
                AliasesSetCanonicalCamelToolHandler(),
            ).forEach { h ->
                val ann = h.annotations.shouldNotBeNull()
                ann.readOnlyHint.shouldBeFalse()
                ann.destructiveHint.shouldBeTrue()
            }
        }
    })

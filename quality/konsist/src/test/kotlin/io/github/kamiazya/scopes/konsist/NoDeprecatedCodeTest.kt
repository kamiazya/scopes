package io.github.kamiazya.scopes.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import io.kotest.core.spec.style.StringSpec

/**
 * Test to ensure no deprecated code exists in the codebase.
 *
 * Since we're not maintaining backward compatibility until stable release,
 * we should remove deprecated code instead of keeping it around.
 */
class NoDeprecatedCodeTest :
    StringSpec({

        "no classes should be deprecated" {
            Konsist
                .scopeFromProduction()
                .classes()
                .assertFalse { clazz ->
                    clazz.hasAnnotationOf(Deprecated::class)
                }
        }

        "no interfaces should be deprecated" {
            Konsist
                .scopeFromProduction()
                .interfaces()
                .assertFalse { it.hasAnnotationOf(Deprecated::class) }
        }

        "no functions should be deprecated except for platform migration" {
            Konsist
                .scopeFromProduction()
                .functions()
                .filter { !it.resideInPackage("..platform.commons.id..") }
                .assertFalse { it.hasAnnotationOf(Deprecated::class) }
        }

        "no properties should be deprecated" {
            Konsist
                .scopeFromProduction()
                .properties()
                .assertFalse { it.hasAnnotationOf(Deprecated::class) }
        }

        "no objects should be deprecated" {
            Konsist
                .scopeFromProduction()
                .objects()
                .assertFalse { it.hasAnnotationOf(Deprecated::class) }
        }
    })

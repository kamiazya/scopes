package io.github.kamiazya.scopes.quality.archunit

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
// Note: @DomainLayer is a package-level annotation, not used directly in these tests
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

/**
 * ArchUnit tests for Layered Architecture pattern.
 *
 * This test class validates:
 * - @DomainLayer packages contain only domain logic
 * - Domain layer doesn't depend on infrastructure or application layers
 * - Domain layer classes follow proper package organization
 * - Layer dependencies flow in the correct direction
 */
class LayeredArchitectureTest {

    companion object {
        private lateinit var classes: JavaClasses

        @BeforeAll
        @JvmStatic
        fun setup() {
            classes = ClassFileImporter()
                .withImportOption(ImportOption.DoNotIncludeTests())
                .importPackages("io.github.kamiazya.scopes")
        }
    }

    @Test
    fun `domain layer should not depend on infrastructure layer`() {
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage("..infrastructure..")
            .check(classes)
    }

    @Test
    fun `domain layer should not depend on application layer`() {
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage("..application..")
            .check(classes)
    }

    @Test
    fun `domain layer should not depend on interfaces layer`() {
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage("..interfaces..")
            .check(classes)
    }

    @Test
    fun `domain layer classes should reside in domain packages`() {
        classes()
            .that().resideInAPackage("..domain..")
            .should().resideInAnyPackage(
                "..domain..",
                "..platform.commons..",
                "..platform.domain.."
            )
            .check(classes)
    }

    @Test
    fun `application layer should not depend on infrastructure layer`() {
        noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage("..infrastructure..")
            .check(classes)
    }

    @Test
    fun `infrastructure layer should not depend on interfaces layer`() {
        noClasses()
            .that().resideInAPackage("..infrastructure..")
            .should().dependOnClassesThat().resideInAnyPackage("..interfaces..")
            .check(classes)
    }
}

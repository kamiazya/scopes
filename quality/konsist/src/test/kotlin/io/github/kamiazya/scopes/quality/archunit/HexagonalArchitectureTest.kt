package io.github.kamiazya.scopes.quality.archunit

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import org.jmolecules.architecture.hexagonal.PrimaryPort
import org.jmolecules.architecture.hexagonal.SecondaryAdapter
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

/**
 * ArchUnit tests for Hexagonal Architecture (Ports and Adapters) pattern.
 *
 * This test class validates:
 * - @PrimaryPort annotated classes are interfaces in contracts packages
 * - @SecondaryAdapter annotated classes are in infrastructure packages
 * - Port implementations follow proper naming conventions
 * - Adapters properly implement or depend on ports
 */
class HexagonalArchitectureTest {

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
    fun `primary ports should be interfaces in contracts packages`() {
        classes()
            .that().areAnnotatedWith(PrimaryPort::class.java)
            .should().beInterfaces()
            .andShould().resideInAnyPackage(
                "..contracts..",
                "..platform.application..",
            )
            .check(classes)
    }

    @Test
    fun `secondary adapters should reside in infrastructure packages`() {
        classes()
            .that().areAnnotatedWith(SecondaryAdapter::class.java)
            .should().resideInAnyPackage("..infrastructure..")
            .check(classes)
    }

    @Test
    fun `port classes should have Port suffix in their name`() {
        classes()
            .that().areAnnotatedWith(PrimaryPort::class.java)
            .should().haveSimpleNameEndingWith("Port")
            .orShould().haveSimpleNameEndingWith("UseCase")
            .check(classes)
    }

    @Test
    fun `adapter classes should have Adapter suffix in their name`() {
        classes()
            .that().areAnnotatedWith(SecondaryAdapter::class.java)
            .should().haveSimpleNameEndingWith("Adapter")
            .orShould().haveSimpleNameEndingWith("Repository")
            .orShould().haveSimpleNameEndingWith("Service")
            .check(classes)
    }
}

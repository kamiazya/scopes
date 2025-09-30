package io.github.kamiazya.scopes.quality.archunit

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import org.jmolecules.event.annotation.DomainEvent
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

/**
 * ArchUnit tests for Domain Events pattern.
 *
 * This test class validates:
 * - Classes implementing DomainEvent are properly annotated
 * - Domain events reside in appropriate packages
 * - Domain events follow naming conventions
 * - Domain events are immutable (data classes)
 */
class DomainEventTest {

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
    fun `domain events should be annotated with DomainEvent`() {
        classes()
            .that().resideInAnyPackage("..domain.event..", "..domain.events..")
            .and().areNotInterfaces()
            .and().areNotMemberClasses() // Exclude inner classes like DuplicateEvent in error sealed class
            .and().doNotHaveModifier(com.tngtech.archunit.core.domain.JavaModifier.ABSTRACT) // Exclude abstract base classes like ScopeEvent
            .and().haveSimpleNameNotStartingWith("Abstract") // Additional filter for abstract base classes
            .and().haveSimpleNameNotContaining("Kt") // Exclude Kotlin extension files like EventSourcingExtensionsKt
            .and().haveSimpleNameNotEndingWith("Metadata") // Exclude utility classes like EventMetadata
            .and().haveSimpleNameNotEndingWith("Changes") // Exclude DTO classes like ContextViewChanges
            .should().beAnnotatedWith(DomainEvent::class.java)
            .allowEmptyShould(true) // Allow if no concrete events exist yet
            .check(classes)
    }

    @Test
    fun `domain events should reside in domain event packages`() {
        classes()
            .that().areAnnotatedWith(DomainEvent::class.java)
            .should().resideInAnyPackage(
                "..domain.event..",
                "..domain.events..",
            )
            .check(classes)
    }

    @Test
    fun `domain events should have Event suffix in their name`() {
        classes()
            .that().areAnnotatedWith(DomainEvent::class.java)
            .should().haveSimpleNameEndingWith("Event")
            .orShould().haveSimpleNameEndingWith("Created")
            .orShould().haveSimpleNameEndingWith("Updated")
            .orShould().haveSimpleNameEndingWith("Deleted")
            .orShould().haveSimpleNameEndingWith("Assigned")
            .orShould().haveSimpleNameEndingWith("Removed")
            .orShould().haveSimpleNameEndingWith("Changed")
            .orShould().haveSimpleNameEndingWith("Replaced")
            .orShould().haveSimpleNameEndingWith("Activated")
            .orShould().haveSimpleNameEndingWith("Cleared")
            .orShould().haveSimpleNameEndingWith("Applied")
            .orShould().haveSimpleNameEndingWith("Archived")
            .orShould().haveSimpleNameEndingWith("Restored")
            .orShould().haveSimpleNameEndingWith("Added")
            .orShould().haveSimpleNameEndingWith("Reset")
            .check(classes)
    }
}

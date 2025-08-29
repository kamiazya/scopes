package io.github.kamiazya.scopes.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withNameEndingWith
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EventStoreArchitectureTest {

    @Nested
    inner class `Event serialization should be properly implemented` {

        @Test
        fun `All event classes should be serializable`() {
            Konsist
                .scopeFromProject()
                .classes()
                .filter {
                    it.resideInPackage("..domain..event..") ||
                        it.name.endsWith("Event") ||
                        it.name.endsWith("DomainEvent")
                }
                .filter { !it.name.contains("Test") }
                .assertTrue { eventClass ->
                    // Check if class has @Serializable annotation
                    val hasSerializableAnnotation = eventClass.annotations
                        .any { it.name == "Serializable" }

                    // Check if class implements a serializable interface
                    val implementsSerializable = eventClass.parents()
                        .any { parent ->
                            parent.name.contains("Serializable") ||
                                parent.name.contains("DomainEvent")
                        }

                    hasSerializableAnnotation || implementsSerializable
                }
        }

        @Test
        fun `EventSerializer implementations should not return empty strings`() {
            Konsist
                .scopeFromProject()
                .classes()
                .filter {
                    it.name.contains("EventSerializer") ||
                        it.hasInterface { i -> i.name == "EventSerializer" }
                }
                .flatMap { it.functions() }
                .filter { it.name == "serialize" || it.name == "deserialize" }
                .assertTrue { function ->
                    // Check that function doesn't return empty string
                    !function.text.contains("\"\"") &&
                        !function.text.contains("return \"\"") &&
                        !function.text.contains("= \"\"")
                }
        }
    }

    @Nested
    inner class `Event store sequence numbers should be handled safely` {

        @Test
        fun `Sequence number generation should not use manual increment`() {
            Konsist
                .scopeFromProject()
                .files
                .filter { it.path.contains("EventRepository") }
                .flatMap { it.functions() }
                .filter {
                    it.text.contains("sequenceNumber") ||
                        it.text.contains("sequence_number")
                }
                .assertTrue { function ->
                    val hasUnsafePattern = function.text.let { text ->
                        // Look for patterns like "lastSequence + 1" or "max() + 1"
                        (text.contains("+ 1") && text.contains("sequence")) ||
                            (text.contains("max()") && text.contains("sequence")) ||
                            text.contains("SELECT MAX(sequence") ||
                            text.contains(".last()") &&
                            text.contains("sequence")
                    }

                    !hasUnsafePattern
                }
        }
    }

    @Nested
    inner class `Error mapping should be accurate` {

        @Test
        fun `Port adapters should map errors specifically`() {
            Konsist
                .scopeFromProject()
                .classes()
                .withNameEndingWith("PortAdapter", "Adapter")
                .flatMap { it.functions() }
                .filter { function ->
                    function.text.contains("mapLeft") ||
                        function.text.contains("leftMap") ||
                        function.text.contains("error ->")
                }
                .assertTrue { function ->
                    // Check for proper error discrimination
                    val hasProperErrorMapping = function.text.let { text ->
                        text.contains("when (error)") ||
                            text.contains("when (it)") ||
                            text.contains("is ") ||
                            // pattern matching
                            text.contains("error.toContract") // dedicated mapping function
                    }

                    // Should not have blanket error mapping
                    val hasBlanketMapping = function.text.let { text ->
                        text.contains("error ->") &&
                            !text.contains("when") &&
                            !text.contains("is ") &&
                            text.lines().count { it.contains("->") } == 1
                    }

                    hasProperErrorMapping || !hasBlanketMapping
                }
        }

        @Test
        fun `Error types should be specific to their context`() {
            Konsist
                .scopeFromProject()
                .classes()
                .filter { it.name.endsWith("Error") || it.name.endsWith("Exception") }
                .filter { it.resideInPackage("..domain..") }
                .assertTrue { errorClass ->
                    // Domain errors should be sealed classes or enums for exhaustive handling
                    errorClass.hasSealedModifier ||
                        errorClass.hasEnumModifier ||
                        errorClass.parents().any { it.name.contains("sealed") }
                }
        }
    }

    @Nested
    inner class `Repository patterns should be consistent` {

        @Test
        fun `Repository methods should return Either type for error handling`() {
            Konsist
                .scopeFromProject()
                .interfaces()
                .withNameEndingWith("Repository")
                .filter { it.resideInPackage("..domain..") }
                .flatMap { it.functions() }
                .filter { !it.hasPrivateModifier }
                .assertTrue { function ->
                    val returnType = function.returnType?.name ?: ""

                    // Check if returns Either or similar Result type
                    returnType.contains("Either<") ||
                        returnType.contains("Result<") ||
                        returnType == "Unit" ||
                        // fire-and-forget operations
                        function.hasSuspendModifier &&
                        returnType.contains("Flow<") // streaming
                }
        }

        @Test
        fun `Infrastructure repositories should implement domain interfaces`() {
            Konsist
                .scopeFromProject()
                .classes()
                .withNameEndingWith("Repository")
                .filter { it.resideInPackage("..infrastructure..") }
                .filter { !it.name.contains("Test") }
                .assertTrue { repoClass ->
                    // Should implement a domain repository interface
                    repoClass.hasInterface { it.resideInPackage("..domain..") }
                }
        }
    }
}

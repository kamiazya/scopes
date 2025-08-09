package io.github.kamiazya.scopes.domain.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Test class for TitleNormalizer utility.
 */
class TitleNormalizerTest {

    @Test
    fun `normalize should trim leading and trailing whitespace`() {
        assertEquals("my task", TitleNormalizer.normalize("  my task  "))
        assertEquals("my task", TitleNormalizer.normalize("\tmy task\n"))
        assertEquals("my task", TitleNormalizer.normalize("   my task   "))
    }

    @Test
    fun `normalize should collapse internal whitespace sequences to single spaces`() {
        assertEquals("my task", TitleNormalizer.normalize("my  task"))
        assertEquals("my task", TitleNormalizer.normalize("my\t\ttask"))
        assertEquals("my task", TitleNormalizer.normalize("my\n\ntask"))
        assertEquals("my task", TitleNormalizer.normalize("my    task"))
        assertEquals("my task item", TitleNormalizer.normalize("my  \t\n  task   item"))
    }

    @Test
    fun `normalize should convert to lowercase`() {
        assertEquals("my task", TitleNormalizer.normalize("MY TASK"))
        assertEquals("my task", TitleNormalizer.normalize("My Task"))
        assertEquals("my task", TitleNormalizer.normalize("mY tAsK"))
    }

    @Test
    fun `normalize should handle comprehensive cases`() {
        // Combining all transformations
        assertEquals("my task", TitleNormalizer.normalize("  MY  \t\nTASK  "))
        assertEquals("hello world", TitleNormalizer.normalize("\t\tHELLO\n\n   WORLD\t"))
        assertEquals("test case", TitleNormalizer.normalize("   Test    \t\n  Case   "))
    }

    @Test
    fun `normalize should handle empty and single character strings`() {
        assertEquals("", TitleNormalizer.normalize(""))
        assertEquals("", TitleNormalizer.normalize("   "))
        assertEquals("a", TitleNormalizer.normalize("A"))
        assertEquals("a", TitleNormalizer.normalize(" A "))
    }

    @Test
    fun `normalize should handle strings with only whitespace differences`() {
        val input1 = "My Task"
        val input2 = "my  task"
        val input3 = "MY\tTASK\n"
        val input4 = "  my    task  "
        
        val normalized1 = TitleNormalizer.normalize(input1)
        val normalized2 = TitleNormalizer.normalize(input2)
        val normalized3 = TitleNormalizer.normalize(input3)
        val normalized4 = TitleNormalizer.normalize(input4)
        
        assertEquals("my task", normalized1)
        assertEquals("my task", normalized2)
        assertEquals("my task", normalized3)
        assertEquals("my task", normalized4)
        
        // All should be equal after normalization
        assertEquals(normalized1, normalized2)
        assertEquals(normalized2, normalized3)
        assertEquals(normalized3, normalized4)
    }
}
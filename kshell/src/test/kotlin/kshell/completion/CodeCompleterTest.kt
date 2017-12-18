package kshell.completion

import org.junit.Assert.*
import org.junit.Test

class CodeCompleterTest {
    @Test
    fun skipFromRight() {
        assertEquals(1, "1.to".skipFromRight())
        assertEquals("1.toString(".length - 1, "1.toString(".skipFromRight())
        assertEquals(0, "println".skipFromRight())
    }
}
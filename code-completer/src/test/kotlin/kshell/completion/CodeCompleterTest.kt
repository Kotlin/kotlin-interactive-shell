package completion

import kshell.completion.skipFromRight
import org.junit.Assert.*
import org.junit.Test

class CodeCompleterTest {
    @Test
    fun skipFromRight() {
        assertEquals(1, "1.to".skipFromRight())
        assertEquals("1.toString(".length - 1, "1.toString(".skipFromRight())
        assertEquals(-1, "println".skipFromRight())
    }
}
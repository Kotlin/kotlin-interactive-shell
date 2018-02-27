package sparklin.kshell

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.PrintWriter

class SingleLineOutputStreamTest {
    @Test
    fun testPrintln() {
        val stream = SingleLineOutputStream()
        val out = PrintWriter(stream)
        assertEquals("", stream.last())
        out.println("hello")
        out.flush()
        assertEquals("hello", stream.last())
    }
}
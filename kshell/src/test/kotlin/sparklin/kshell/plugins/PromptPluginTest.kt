package sparklin.kshell.plugins

import org.junit.Assert.*
import org.junit.Test

class PromptPluginTest {
    @Test
    fun testFormat() {
        val types = mapOf("s" to { "S" }, "ss" to {"Z"})
        assertEquals("hello SZ{} %d", PromptPlugin.format("hello %s%{ss}{} %d", types))
    }
}
package org.jetbrains.kotlin.ki.shell

import org.junit.Assert.*
import org.junit.Test

class UtilTest {
    @Test
    fun testSmartSplit() {
        assertEquals(listOf("Location", "Welcome  to india",
                "Bangalore", "Channai", "IT city", "Mysore"), smartSplit("Location \"Welcome  to india\" Bangalore Channai \"IT city\"  Mysore"))
    }

    @Test
    fun testGlobToRegex() {
        assertMatch("*.name", "com.github.khud.org.jetbrains.kotlin.ki.shell.plugins.LoadFilePlugin.Load.name")
        assertMatch("com.github.*.LoadFilePlugin.Load.name", "com.github.khud.org.jetbrains.kotlin.ki.shell.plugins.LoadFilePlugin.Load.name")
        assertNotMatch("*.name", "com.github.khud.org.jetbrains.kotlin.ki.shell.plugins.LoadFilePlugin.Load.name.temp")
        assertMatch("*.n?me", "com.github.khud.org.jetbrains.kotlin.ki.shell.plugins.LoadFilePlugin.Load.name")
        assertNotMatch("*.n?me", "com.github.khud.org.jetbrains.kotlin.ki.shell.plugins.LoadFilePlugin.Load.nme")
    }

    private fun assertMatch(glob: String, line: String) {
        val regex = globToRegex(glob).toRegex()
        assertTrue(regex.matches(line))
    }

    private fun assertNotMatch(glob: String, line: String) {
        val regex = globToRegex(glob).toRegex()
        assertFalse(regex.matches(line))
    }
}
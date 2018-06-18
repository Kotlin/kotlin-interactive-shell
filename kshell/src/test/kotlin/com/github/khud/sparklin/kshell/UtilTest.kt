package com.github.khud.sparklin.kshell

import org.junit.Test

import org.junit.Assert.*

class UtilTest {

    @Test
    fun testGlobToRegex() {
        assertMatch("*.name", "com.github.khud.com.github.khud.sparklin.kshell.plugins.LoadFilePlugin.Load.name")
        assertMatch("com.github.*.LoadFilePlugin.Load.name", "com.github.khud.com.github.khud.sparklin.kshell.plugins.LoadFilePlugin.Load.name")
        assertNotMatch("*.name", "com.github.khud.com.github.khud.sparklin.kshell.plugins.LoadFilePlugin.Load.name.temp")
        assertMatch("*.n?me", "com.github.khud.com.github.khud.sparklin.kshell.plugins.LoadFilePlugin.Load.name")
        assertNotMatch("*.n?me", "com.github.khud.com.github.khud.sparklin.kshell.plugins.LoadFilePlugin.Load.nme")
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
package com.github.khud.sparklin.kshell.plugins

import org.junit.Assert.*
import org.junit.Test
import com.github.khud.sparklin.kshell.plugins.ConfigPlugin.Companion.groupByClass

class ConfigPluginTest {
    @Test
    fun testGroupByClass() {
        assertEquals(mapOf("com.prod" to listOf("x", "y")), groupByClass(listOf("com.prod.x", "com.prod.y")))
        assertEquals(mapOf(null to listOf("x", "y")), groupByClass(listOf("x", "y")))
    }
}
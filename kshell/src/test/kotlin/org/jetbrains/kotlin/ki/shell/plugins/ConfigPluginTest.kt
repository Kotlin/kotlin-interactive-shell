package org.jetbrains.kotlin.ki.shell.plugins

import org.jetbrains.kotlin.ki.shell.plugins.ConfigPlugin.Companion.groupByClass
import org.junit.Assert.assertEquals
import org.junit.Test

class ConfigPluginTest {
    @Test
    fun testGroupByClass() {
        assertEquals(mapOf("com.prod" to listOf("x", "y")), groupByClass(listOf("com.prod.x", "com.prod.y")))
        assertEquals(mapOf(null to listOf("x", "y")), groupByClass(listOf("x", "y")))
    }
}
package org.jetbrains.kotlinx.ki.shell

import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import kotlin.script.experimental.api.ResultValue

class ResultRenderingTest {
    @get:Rule
    val shells = TestShells()

    private val shell by lazy { shells.create() }

    private fun render(code: String): String {
        val result = shell.evalSuccess(code) as? ResultValue.Value ?: return ""
        return shell.renderResult(result)
    }

    private fun renderValue(code: String): String = render(code).substringAfter(" = ")

    @Test
    fun resultIsRenderedWithNameAndType() {
        assertEquals("res0: Int = 2", render("1 + 1"))
        assertEquals("res1: String = abc", render("\"abc\""))
    }

    @Test
    fun unsignedValuesAreRenderedAsUnsigned() {
        assertEquals("res0: UInt = 1", render("1u"))
        assertEquals("res1: ULong = 18446744073709551615", render("ULong.MAX_VALUE"))
        assertEquals("18446744073709551615", renderValue("0UL - 1UL"))
        assertEquals("4294967295", renderValue("UInt.MAX_VALUE"))
        assertEquals("65535", renderValue("UShort.MAX_VALUE"))
        assertEquals("255", renderValue("UByte.MAX_VALUE"))
    }

    @Test
    fun nullableAndNestedUnsignedValues() {
        assertEquals("4294967295", renderValue("UInt.MAX_VALUE as UInt?"))
        assertEquals("null", renderValue("null as UInt?"))
        assertEquals("[18446744073709551615]", renderValue("listOf(ULong.MAX_VALUE)"))
    }

    @Test
    fun userValueClassesUseTheirToString() {
        render("@JvmInline value class Meters(val value: Int)")
        assertEquals("Meters(value=5)", renderValue("Meters(5)"))
        render("@JvmInline value class Name(val value: String) { override fun toString() = \"Name: \$value\" }")
        assertEquals("Name: ki", renderValue("Name(\"ki\")"))
    }

    @Test
    fun resultValueClassFromStdlib() {
        assertEquals("Success(1)", renderValue("runCatching { 1 }"))
    }
}

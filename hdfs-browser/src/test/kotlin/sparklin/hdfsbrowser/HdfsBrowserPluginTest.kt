package sparklin.hdfsbrowser

import org.junit.Test

import org.junit.Assert.*
import sparklin.hdfsbrowser.HdfsBrowserPlugin.Companion.ParseResult.*

class HdfsBrowserPluginTest {

    @Test
    fun parseOpts() {
        assertEquals(ParsedOptions("hd", "/home/user"), HdfsBrowserPlugin.parseOpts("-hd /home/user", "hd"))
        assertEquals(ParseError("Unrecognized options: d"), HdfsBrowserPlugin.parseOpts("-hd /home/user", "h"))
        assertEquals(ParsedOptions("hd", ""), HdfsBrowserPlugin.parseOpts("-hd", "hd"))
        assertEquals(ParsedOptions("", ""), HdfsBrowserPlugin.parseOpts("", "hd"))
        assertEquals(ParseError("Missing options"), HdfsBrowserPlugin.parseOpts("-", "hd"))
    }
}
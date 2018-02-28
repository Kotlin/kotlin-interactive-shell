package sparklin.kshell

import java.io.OutputStream

class SingleLineOutputStream : OutputStream() {
    private val lineBuilder = StringBuilder()
    private val buf = StringBuilder()
    private var line: String = ""

    override fun write(b: Int) {
        if (b == '\n'.toInt()) {
            line = lineBuilder.toString()
            buf.append(line)
            buf.append('\n')
            lineBuilder.setLength(0)
        } else {
            lineBuilder.append(b.toChar())
        }
    }

    fun last(): String = line

    fun clear() {
        buf.setLength(0)
    }
    
    override fun toString(): String = buf.toString()
}
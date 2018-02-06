package sparklin.kshell

import java.io.OutputStream

class SingleLineOutputStream : OutputStream() {
    private val builder = StringBuilder()
    private var line: String = ""

    override fun write(b: Int) {
        if (b == '\n'.toInt()) {
            line = builder.toString()
            builder.setLength(0)
        } else {
            builder.append(b.toChar())
        }
    }

    override fun toString(): String = line
}
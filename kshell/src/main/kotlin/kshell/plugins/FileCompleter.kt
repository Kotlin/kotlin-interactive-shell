package kshell.plugins

import java.io.File
import kshell.console.Completer


open class FileCompleter : Completer {

    override fun complete(buffer: String, cursor: Int, candidates: MutableList<CharSequence>): Int {
        var buffer = buffer

        checkNotNull<List<CharSequence>>(candidates)

        if (OS_IS_WINDOWS) {
            buffer = buffer.replace('/', '\\')
        }

        // remove command (:load and so on) from the data to be completed
        val p = buffer.indexOf(' ')
        var translated: String = buffer.substring(p + 1)

        val homeDir = userHome

        // Special character: ~ maps to the user's home directory
        if (translated.startsWith("~" + separator())) {
            translated = homeDir.path + translated.substring(1)
        } else if (translated.startsWith("~")) {
            translated = homeDir.parentFile.absolutePath
        } else if (!File(translated).isAbsolute) {
            val cwd = userDir.absolutePath
            translated = cwd + separator() + translated
        }

        val file = File(translated)
        val dir: File?

        if (translated.endsWith(separator())) {
            dir = file
        } else {
            dir = file.parentFile
        }

        val entries = if (dir == null) arrayOfNulls<File>(0) else dir.listFiles()

        return matchFiles(buffer, translated, entries, candidates)
    }

    protected fun separator(): String {
        return File.separator
    }

    private val userHome: File
        get() = File(System.getProperty("user.home"))//Configuration.getUserHome()

    private val userDir: File
        get() = File(".")

    private fun matchFiles(buffer: String, translated: String, files: Array<out File?>, candidates: MutableList<CharSequence>): Int {
        var matches = 0

        // first pass: just count the matches
        for (file in files) {
            if (file!!.absolutePath.startsWith(translated)) {
                matches++
            }
        }
        for (file in files) {
            if (file!!.absolutePath.startsWith(translated)) {
                val name = file!!.name + if (matches == 1 && file.isDirectory) separator() else " "
                candidates.add(render(file!!, name).toString())
            }
        }

        var index = buffer.lastIndexOf(separator())

        if (index < 0) index = buffer.lastIndexOf(' ')

        return index + separator().length
    }

    protected fun render(file: File, name: CharSequence): CharSequence {
        return name
    }

    companion object {
        // TODO: Handle files with spaces in them

        private val OS_IS_WINDOWS: Boolean

        init {
            val os = "mac" // FIXME: Configuration.getOsName()
            OS_IS_WINDOWS = os.contains("windows")
        }
    }
}
package kshell.command

import lib.jline.console.completer.Completer
import lib.jline.internal.Configuration
import java.io.File

/**
 * Completer for :load command. Eventually based on jline FileNameCompleter.
 * Created by vitaly.khudobakhshov on 18/04/17.
 */
open class FileCompleter : Completer {

    override fun complete(buffer: String?, cursor: Int, candidates: MutableList<CharSequence>): Int {
        var buffer = buffer
        // buffer can be null
        checkNotNull<List<CharSequence>>(candidates)

        if (buffer == null) {
            buffer = ""
        }

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

    protected val userHome: File
        get() = Configuration.getUserHome()

    protected val userDir: File
        get() = File(".")

    protected fun matchFiles(buffer: String, translated: String, files: Array<out File?>?, candidates: MutableList<CharSequence>): Int {
        if (files == null) {
            return -1
        }

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
            val os = Configuration.getOsName()
            OS_IS_WINDOWS = os.contains("windows")
        }
    }
}
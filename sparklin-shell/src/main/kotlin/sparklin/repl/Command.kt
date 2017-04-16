package sparklin.repl

/**
 * Created by vitaly.khudobakhshov on 16/04/17.
 */
abstract class Command(val cmd: String,
                       val short: Char,
                       val description: String,
                       val ignoreCase: Boolean = true) {
    var repl: AdvancedRepl? = null

    internal fun match(line: String): Boolean {
        val ind = line.indexOf(' ')
        val command = if (ind < 0) line else line.substring(0, ind)
        return command.startsWith(":$short", ignoreCase = ignoreCase) || command.startsWith(":$cmd", ignoreCase = ignoreCase)
    }

    abstract fun execute(line: String): Unit

    override fun toString(): String {
        return "$cmd or $short\t$description"
    }
}
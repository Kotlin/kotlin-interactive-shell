package sparklin.repl

/**
 * Created by vitaly.khudobakhshov on 16/04/17.
 */
open class CommandHelp: Command("help", 'h', "Print this help") {
    override fun execute(line: String) {
        val repl = this.repl!!
        val help = repl.commands.map(Command::toString).joinToString(separator = "\n")
        repl.reader.println(help)
    }
}

package kshell.console

interface Completer {
    fun complete(buffer: String, cursor: Int, candidates: MutableList<CharSequence>): Int

    companion object {
        val DEFAULT_COMPLETER = object: Completer {
            override fun complete(buffer: String, cursor: Int, candidates: MutableList<CharSequence>): Int = -1
        }
    }
}
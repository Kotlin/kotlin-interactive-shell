package org.jetbrains.kotlinx.ki.shell

import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.scripting.ide_services.compiler.KJvmReplCompilerWithIdeServices
import org.jline.reader.Candidate
import org.jline.reader.Completer
import org.jline.reader.LineReader
import org.jline.reader.ParsedLine
import java.util.concurrent.atomic.AtomicInteger
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.api.valueOrThrow
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.util.toSourceCodePosition

class KotlinCompleter(
        val compiler: KJvmReplCompilerWithIdeServices,
        val getCompilationConfiguration: () -> ScriptCompilationConfiguration,
        val getCurrentLineNo: () -> Int,
        val incompleteLines: ArrayList<String>
) : Completer {
    val currentCompletionNo = AtomicInteger()

    override fun complete(reader: LineReader?, line: ParsedLine?, candidates: MutableList<Candidate>?) {
        val snippet = line?.line()?.let {
            val source = if (incompleteLines.isEmpty() )it else (incompleteLines + it).joinToString(separator = "\n")
            source.toScriptSource("Line_${getCurrentLineNo()}_completion_${currentCompletionNo.incrementAndGet()}")
        } ?: return
        val position = line.cursor().toSourceCodePosition(snippet).let {
            if (incompleteLines.isEmpty()) it
            else SourceCode.Position(it.line + incompleteLines.size, it.col)
        }
        val res = runBlocking {
            compiler.complete(snippet, position, getCompilationConfiguration())
        }
        res.valueOrThrow().forEach {
            candidates!!.add(Candidate(it.text))
        }
    }
}

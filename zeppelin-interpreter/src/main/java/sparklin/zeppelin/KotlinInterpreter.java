package sparklin.zeppelin;

public class KotlinInterpreter {
}

/*
package sparklin.zeppelin

import org.apache.zeppelin.interpreter.Interpreter
import org.apache.zeppelin.interpreter.InterpreterContext
import org.apache.zeppelin.interpreter.InterpreterResult
import sparklin.kshell.KShell
import java.util.*

class KotlinInterpreter(properties: Properties) : Interpreter(properties) {
    var shell: KShell? = null
    override fun cancel(interpreterContext: InterpreterContext?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getProgress(interpreterContext: InterpreterContext?): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun close() {
        shell?.cleanUp()
    }

    override fun interpret(line: String?, interpreterContext: InterpreterContext?): InterpreterResult {
        if (line == null || line.isBlank()) {
            return InterpreterResult(InterpreterResult.Code.SUCCESS)
        }
        TODO("not implemented")
    }

    override fun getFormType(): FormType {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun open() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}
 */
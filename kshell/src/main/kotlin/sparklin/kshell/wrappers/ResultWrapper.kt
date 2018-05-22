package sparklin.kshell.wrappers

import com.github.khud.kshell.repl.EvalError
import com.github.khud.kshell.repl.EvalResult
import com.github.khud.kshell.repl.Result


class ResultWrapper(val result: Result<EvalResult, EvalError>) {
    enum class Status {
        SUCCESS, ERROR, INCOMPLETE
    }

    fun getStatus(): Status =
            when (result) {
                is Result.Error -> if (result.error.isIncomplete) Status.INCOMPLETE else Status.ERROR
                is Result.Success -> Status.SUCCESS
            }

    fun getMessage(): String? =
            when (result) {
                is Result.Error -> result.error.message
                is Result.Success -> if (result.data is EvalResult.ValueResult) result.data.toString() else null
            }

    fun getMessageOrEmpty(): String = getMessage() ?: ""

    fun getErrorCause(): Exception? = ((result as? Result.Error)?.error as? EvalError.RuntimeError)?.cause
}


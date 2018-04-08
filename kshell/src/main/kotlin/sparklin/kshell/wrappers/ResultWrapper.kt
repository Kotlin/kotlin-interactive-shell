package sparklin.kshell.wrappers

import sparklin.kshell.repl.EvalError
import sparklin.kshell.repl.EvalResult
import sparklin.kshell.repl.Result


class ResultWrapper(val result: Result<EvalResult, EvalError>) {
    enum class Status {
        SUCCESS, ERROR, INCOMPLETE
    }

    fun getStatus(): Status =
            when (result) {
                is Result.Error -> Status.ERROR
                is Result.Incomplete -> Status.INCOMPLETE
                is Result.Success -> Status.SUCCESS
            }

    fun getMessage(): String? =
            when (result) {
                is Result.Error -> result.error.message
                is Result.Incomplete -> null
                is Result.Success -> if (result.data is EvalResult.ValueResult) result.data.toString() else null
            }

    fun getMessageOrEmpty(): String = getMessage() ?: ""

    fun getErrorCause(): Exception? = ((result as? Result.Error)?.error as? EvalError.RuntimeError)?.cause
}


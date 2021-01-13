package org.jetbrains.kotlinx.ki.shell.wrappers

import kotlin.script.experimental.api.ResultValue
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptDiagnostic


class ResultWrapper(val result: ResultWithDiagnostics<*>, val isCompiled: Boolean) {
    enum class Status {
        SUCCESS, ERROR, INCOMPLETE
    }

    fun getStatus(): Status =
            when (result) {
                is ResultWithDiagnostics.Failure -> {
                    if (result.reports.any { it.code == ScriptDiagnostic.incompleteCode }) Status.INCOMPLETE
                    else Status.ERROR
                }
                is ResultWithDiagnostics.Success -> Status.SUCCESS
            }

    fun getMessage(): String? =
            when (result) {
                is ResultWithDiagnostics.Failure -> result.reports.find { it.severity == ScriptDiagnostic.Severity.ERROR }?.message
                is ResultWithDiagnostics.Success -> {
                    val result = result.value
                    if (result is ResultValue.Value) result.value.toString() else null
                }
            }

    fun getMessageOrEmpty(): String = getMessage() ?: ""

    fun getErrorCause(): Throwable? = (result as? ResultWithDiagnostics.Success)?.value?.let {
        (it as? ResultValue.Error)?.error?.cause
    }
}


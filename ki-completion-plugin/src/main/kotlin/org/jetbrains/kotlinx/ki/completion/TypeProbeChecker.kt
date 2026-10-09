package org.jetbrains.kotlinx.ki.completion

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirPropertyChecker
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.extensions.replHistoryProvider
import org.jetbrains.kotlin.fir.types.ConeErrorType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.renderReadable

object TypeProbeChecker : FirPropertyChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirProperty) {
        val sink = (context.session.replHistoryProvider as? CompletionSinkOwner)?.completionSink ?: return
        if (declaration.name.asString() != TYPE_PROBE || !sink.isActive) return
        val type = declaration.returnTypeRef.coneType
        if (type !is ConeErrorType) sink.setType(type.renderReadable())
    }
}

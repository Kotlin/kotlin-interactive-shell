package org.jetbrains.kotlinx.ki.completion

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirQualifiedAccessExpressionChecker
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.types.ConeErrorType
import org.jetbrains.kotlin.fir.types.resolvedType

object CompletionMarkerChecker : FirQualifiedAccessExpressionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirQualifiedAccessExpression) {
        val name = (expression.calleeReference as? FirNamedReference)?.name ?: return
        if (name.isSpecial || !name.asString().contains(COMPLETION_MARKER) || !CompletionSink.isActive) return
        val prefix = name.asString().substringBefore(COMPLETION_MARKER)
        val collector = ScopeCollector(context, prefix, expression)
        when (val receiver = expression.explicitReceiver) {
            null -> collector.collectWithoutReceiver()
            is FirResolvedQualifier -> collector.collectForQualifier(receiver)
            else -> {
                val type = receiver.resolvedType
                if (type !is ConeErrorType) collector.collectForReceiver(type)
            }
        }
        CompletionSink.addCandidates(collector.result())
    }
}

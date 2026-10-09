package org.jetbrains.kotlinx.ki.completion

import org.jetbrains.kotlin.fir.declarations.FirImport
import org.jetbrains.kotlin.fir.extensions.FirReplHistoryProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirReplSnippetSymbol
import org.jetbrains.kotlin.scripting.compiler.plugin.services.FirReplHistoryProviderImpl

class KiReplHistoryProvider(
    private val delegate: FirReplHistoryProvider = FirReplHistoryProviderImpl(),
) : FirReplHistoryProvider(), CompletionSinkOwner {
    override val completionSink = CompletionSink()

    override fun getSnippets(): Iterable<FirReplSnippetSymbol> = delegate.getSnippets()

    override fun putSnippet(symbol: FirReplSnippetSymbol) = delegate.putSnippet(symbol)

    override fun putImportedSnippet(symbol: FirReplSnippetSymbol) = delegate.putImportedSnippet(symbol)

    override fun isImportedSnippet(symbol: FirReplSnippetSymbol): Boolean = delegate.isImportedSnippet(symbol)

    override fun isFirstSnippet(symbol: FirReplSnippetSymbol): Boolean = delegate.isFirstSnippet(symbol)

    override fun getSnippetCount(): Int = delegate.getSnippetCount()

    override fun removeSnippets(symbols: Collection<FirReplSnippetSymbol>) = delegate.removeSnippets(symbols)

    override fun getSnippetImports(symbol: FirReplSnippetSymbol): List<FirImport>? = delegate.getSnippetImports(symbol)
}

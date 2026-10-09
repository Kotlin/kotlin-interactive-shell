package org.jetbrains.kotlinx.ki.completion

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirReplSnippet
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.declarations.staticScope
import org.jetbrains.kotlin.fir.declarations.utils.isReplSnippetDeclaration
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.extensions.replHistoryProvider
import org.jetbrains.kotlin.fir.resolve.calls.FirSyntheticPropertiesScope
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.lookupSuperTypes
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.scope
import org.jetbrains.kotlin.fir.resolve.symbol
import org.jetbrains.kotlin.fir.resolve.toClassSymbol
import org.jetbrains.kotlin.fir.scopes.CallableCopyTypeCalculator
import org.jetbrains.kotlin.fir.scopes.FirContainingNamesAwareScope
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.createImportingScopes
import org.jetbrains.kotlin.fir.scopes.impl.FirAbstractSimpleImportingScope
import org.jetbrains.kotlin.fir.scopes.impl.FirAbstractStarImportingScope
import org.jetbrains.kotlin.fir.scopes.impl.FirDefaultStarImportingScope
import org.jetbrains.kotlin.fir.scopes.impl.FirPackageMemberScope
import org.jetbrains.kotlin.fir.scopes.impl.nestedClassifierScope
import org.jetbrains.kotlin.fir.scopes.processClassifiersByName
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeDefinitelyNotNullType
import org.jetbrains.kotlin.fir.types.ConeIntegerLiteralType
import org.jetbrains.kotlin.fir.types.ConeIntersectionType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeTypeParameterType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.fir.types.lowerBoundIfFlexible
import org.jetbrains.kotlin.fir.types.renderReadable
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds

@OptIn(SymbolInternals::class, DirectDeclarationsAccess::class)
class ScopeCollector(
    private val context: CheckerContext,
    private val prefix: String,
    private val marker: FirElement,
) {
    private val session = context.session
    private val scopeSession = context.scopeSession
    private val items = LinkedHashSet<CompletionCandidate>()
    private val snippetClass = context.containingElements.zipWithNext()
        .firstOrNull { (parent, child) -> parent is FirReplSnippet && child is FirClass }?.second as FirClass?
    private val snippetDeclarationsBeforeMarker = snippetClass?.let { snippetDeclarationsBeforeMarker(it) }.orEmpty()

    fun result(): List<CompletionCandidate> = items.toList()

    fun collectForReceiver(type: ConeKotlinType) {
        val receiverType = type.approximated()
        addMembers(receiverType, includeInvisible = false)
        addExtensions(receiverType)
    }

    fun collectForQualifier(qualifier: FirResolvedQualifier) {
        val classSymbol = qualifier.qualifierSymbol?.fullyExpandedClass()
        if (classSymbol == null) {
            addFromPackage(qualifier.packageFqName)
            return
        }
        classSymbol.staticScope(session, scopeSession)?.let { addFromScope(it, includeInvisible = false) }
        session.nestedClassifierScope(classSymbol.fir)?.let { addFromScope(it, includeInvisible = false) }
        val objectType = when {
            classSymbol.classKind == ClassKind.OBJECT -> classSymbol.defaultType()
            else -> classSymbol.companionObjectSymbol?.defaultType()
        }
        objectType?.let { collectForReceiver(it) }
    }

    fun collectWithoutReceiver() {
        val implicitReceivers = collectLocals()
        implicitReceivers.forEach { receiverType ->
            addMembers(receiverType, includeInvisible = true)
            addExtensions(receiverType)
        }
        historyDeclarations().forEach { addDeclaration(it, extensionsAllowed = false) }
        for (scope in importingScopes()) {
            forEachCallableIn(scope) { if (it.receiverParameterSymbol == null) addCallable(it) }
            forEachClassifierIn(scope) { addClassifier(it) }
        }
    }

    private fun collectLocals(): List<ConeKotlinType> {
        val implicitReceivers = mutableListOf<ConeKotlinType>()
        val path = context.containingElements
        for ((index, element) in path.withIndex()) {
            val next = path.getOrNull(index + 1)
            when (element) {
                is FirBlock -> for (statement in element.statements) {
                    if (statement === next) break
                    addDeclaration(statement, extensionsAllowed = false)
                }
                is FirFunction -> {
                    element.valueParameters.forEach { addCallable(it.symbol) }
                    element.contextParameters.forEach { addCallable(it.symbol) }
                    element.receiverParameter?.typeRef?.coneType?.let { implicitReceivers.add(0, it) }
                }
                is FirClass ->
                    if (element === snippetClass) snippetDeclarationsBeforeMarker.forEach { addDeclaration(it, extensionsAllowed = false) }
                    else implicitReceivers.add(0, element.symbol.defaultType())
                else -> {}
            }
        }
        return implicitReceivers
    }

    // The declarations of the current snippet are members of its class, including the result property that holds the marker
    private fun snippetDeclarationsBeforeMarker(snippetClass: FirClass): List<FirDeclaration> {
        val markerStart = marker.source?.startOffset ?: return emptyList()
        return snippetClass.declarations
            .filter { it.isReplSnippetDeclaration == true && (it.source?.endOffset ?: Int.MAX_VALUE) <= markerStart }
    }

    private fun FirCallableSymbol<*>.isLaterInCurrentSnippet(): Boolean {
        val currentClass = snippetClass ?: return false
        return containingClassLookupTag()?.classId == currentClass.symbol.classId && snippetDeclarationsBeforeMarker.none { it.symbol == this }
    }

    private fun historyDeclarations(): List<FirDeclaration> {
        val snippet = context.containingElements.firstOrNull { it is FirReplSnippet } as? FirReplSnippet ?: return emptyList()
        val provider = session.replHistoryProvider ?: return emptyList()
        return provider.getSnippets().takeWhile { it != snippet.symbol }.flatMap { previous ->
            previous.snippetClassSymbol.declarationSymbols.filter { it.isReplSnippetDeclaration == true }.map { it.fir }
        }.toList()
    }

    private fun addDeclaration(declaration: FirElement, extensionsAllowed: Boolean) {
        when (declaration) {
            is FirProperty -> if (extensionsAllowed || declaration.receiverParameter == null) addCallable(declaration.symbol)
            is FirNamedFunction -> if (extensionsAllowed || declaration.receiverParameter == null) addCallable(declaration.symbol)
            is FirRegularClass -> addClassifier(declaration.symbol)
            is FirTypeAlias -> addClassifier(declaration.symbol)
        }
    }

    private fun addMembers(type: ConeKotlinType, includeInvisible: Boolean) {
        val scope = type.scope(session, scopeSession, CallableCopyTypeCalculator.DoNothing, FirResolvePhase.STATUS) ?: return
        addFromScope(scope, includeInvisible)
        val synthetic = FirSyntheticPropertiesScope.createIfSyntheticNamesProviderIsDefined(session, type, scope)
        if (synthetic is FirContainingNamesAwareScope) addFromScope(synthetic, includeInvisible)
    }

    private fun addExtensions(receiverType: ConeKotlinType) {
        val receiverClassIds = classIdsOf(receiverType)
        fun addIfApplicable(symbol: FirCallableSymbol<*>) {
            val extensionReceiver = symbol.resolvedReceiverType ?: return
            if (isApplicable(extensionReceiver, receiverClassIds)) addCallable(symbol)
        }
        collectLocalCallables().forEach(::addIfApplicable)
        historyDeclarations().forEach { declaration ->
            when (declaration) {
                is FirProperty -> addIfApplicable(declaration.symbol)
                is FirNamedFunction -> addIfApplicable(declaration.symbol)
                else -> {}
            }
        }
        for (scope in importingScopes()) forEachCallableIn(scope, ::addIfApplicable)
    }

    private fun collectLocalCallables(): List<FirCallableSymbol<*>> {
        val path = context.containingElements
        return path.withIndex().flatMap { (index, element) ->
            val next = path.getOrNull(index + 1)
            if (element !is FirBlock) emptyList()
            else element.statements.takeWhile { it !== next }.mapNotNull {
                when (it) {
                    is FirProperty -> it.symbol
                    is FirNamedFunction -> it.symbol
                    else -> null
                }
            }
        }
    }

    private fun importingScopes(): List<FirScope> {
        val file = context.containingFileSymbol?.fir ?: return emptyList()
        return createImportingScopes(file, session, scopeSession)
    }

    private fun forEachCallableIn(scope: FirScope, action: (FirCallableSymbol<*>) -> Unit) {
        for (name in callableNamesIn(scope)) {
            if (!matches(name)) continue
            scope.processFunctionsByName(name) { if (it.isVisibleOutside()) action(it) }
            scope.processPropertiesByName(name) { if (it.isVisibleOutside()) action(it) }
        }
    }

    private fun forEachClassifierIn(scope: FirScope, action: (FirClassifierSymbol<*>) -> Unit) {
        for (name in classifierNamesIn(scope)) {
            if (!matches(name)) continue
            scope.processClassifiersByName(name) { action(it) }
        }
    }

    private fun callableNamesIn(scope: FirScope): Set<Name> = when (scope) {
        is FirDefaultStarImportingScope -> callableNamesIn(scope.first) + callableNamesIn(scope.second)
        is FirAbstractStarImportingScope -> scope.starImports.filter { it.relativeParentClassName == null }
            .flatMapTo(hashSetOf()) { packageCallableNames(it.packageFqName) }
        is FirAbstractSimpleImportingScope -> scope.simpleImports.keys
        is FirPackageMemberScope -> packageCallableNames(scope.fqName)
        is FirContainingNamesAwareScope -> scope.getCallableNames()
        else -> emptySet()
    }

    private fun classifierNamesIn(scope: FirScope): Set<Name> = when (scope) {
        is FirDefaultStarImportingScope -> classifierNamesIn(scope.first) + classifierNamesIn(scope.second)
        is FirAbstractStarImportingScope -> scope.starImports.filter { it.relativeParentClassName == null }
            .flatMapTo(hashSetOf()) { packageClassifierNames(it.packageFqName) }
        is FirAbstractSimpleImportingScope -> scope.simpleImports.keys
        is FirPackageMemberScope -> packageClassifierNames(scope.fqName)
        is FirContainingNamesAwareScope -> scope.getClassifierNames()
        else -> emptySet()
    }

    private fun packageCallableNames(packageFqName: FqName): Set<Name> =
        session.symbolProvider.symbolNamesProvider.getTopLevelCallableNamesInPackage(packageFqName).orEmpty()

    private fun packageClassifierNames(packageFqName: FqName): Set<Name> =
        session.symbolProvider.symbolNamesProvider.getTopLevelClassifierNamesInPackage(packageFqName).orEmpty()

    private fun addFromPackage(packageFqName: FqName) {
        val scope = FirPackageMemberScope(packageFqName, session)
        forEachCallableIn(scope) { addCallable(it) }
        forEachClassifierIn(scope) { addClassifier(it) }
    }

    private fun addFromScope(scope: FirContainingNamesAwareScope, includeInvisible: Boolean) {
        for (name in scope.getCallableNames()) {
            if (!matches(name)) continue
            scope.processFunctionsByName(name) { if (includeInvisible || it.isVisibleOutside()) addCallable(it) }
            scope.processPropertiesByName(name) { if (includeInvisible || it.isVisibleOutside()) addCallable(it) }
        }
        for (name in scope.getClassifierNames()) {
            if (!matches(name)) continue
            scope.processClassifiersByName(name) { addClassifier(it) }
        }
    }

    private fun FirCallableSymbol<*>.isVisibleOutside(): Boolean {
        val visibility = resolvedStatus.visibility
        return visibility != Visibilities.Private && visibility != Visibilities.PrivateToThis &&
                visibility != Visibilities.Protected && visibility != Visibilities.Internal
    }

    private fun matches(name: Name): Boolean {
        if (name.isSpecial) return false
        val text = name.asString()
        return text.startsWith(prefix) && COMPLETION_MARKER !in text && text != TYPE_PROBE && !text.startsWith("$$")
    }

    private fun addCallable(symbol: FirCallableSymbol<*>) {
        val name = symbol.callableId?.callableName ?: (symbol as? FirVariableSymbol<*>)?.name ?: return
        if (!matches(name) || symbol.isLaterInCurrentSnippet()) return
        items += when (symbol) {
            is FirNamedFunctionSymbol -> CompletionCandidate(
                name.asString(), CompletionCandidate.Kind.FUNCTION, render { symbol.resolvedReturnType }, renderParameters(symbol)
            )
            is FirVariableSymbol<*> -> CompletionCandidate(name.asString(), CompletionCandidate.Kind.PROPERTY, render { symbol.resolvedReturnType })
            else -> return
        }
    }

    private fun addClassifier(symbol: FirClassifierSymbol<*>) {
        val classLike = symbol as? FirClassLikeSymbol<*> ?: return
        val name = classLike.classId.shortClassName
        if (matches(name)) items += CompletionCandidate(name.asString(), CompletionCandidate.Kind.CLASS)
    }

    private fun renderParameters(symbol: FirNamedFunctionSymbol): String =
        symbol.valueParameterSymbols.joinToString(", ", "(", ")") { "${it.name}: ${render { it.resolvedReturnType }}" }

    private inline fun render(type: () -> ConeKotlinType): String? =
        try {
            type().renderReadable()
        } catch (_: Throwable) {
            null
        }

    private fun ConeKotlinType.approximated(): ConeKotlinType =
        if (this is ConeIntegerLiteralType) getApproximatedType() else this

    private fun FirClassLikeSymbol<*>.fullyExpandedClass(): FirRegularClassSymbol? =
        when (this) {
            is FirRegularClassSymbol -> this
            else -> classId.constructClassLikeType().fullyExpandedType(session).lookupTag.toClassSymbol(session) as? FirRegularClassSymbol
        }

    private fun classIdsOf(type: ConeKotlinType): Set<ClassId> {
        val result = hashSetOf(StandardClassIds.Any)
        fun visit(current: ConeKotlinType) {
            when (val expanded = current.approximated().fullyExpandedType(session).lowerBoundIfFlexible()) {
                is ConeClassLikeType -> {
                    result += expanded.lookupTag.classId
                    val symbol = expanded.lookupTag.toClassSymbol(session) ?: return
                    lookupSuperTypes(symbol, lookupInterfaces = true, deep = true, useSiteSession = session)
                        .forEach { result += it.lookupTag.classId }
                }
                is ConeTypeParameterType -> expanded.lookupTag.symbol.resolvedBounds.forEach { visit(it.coneType) }
                is ConeIntersectionType -> expanded.intersectedTypes.forEach(::visit)
                is ConeDefinitelyNotNullType -> visit(expanded.original)
                else -> {}
            }
        }
        visit(type)
        return result
    }

    private fun isApplicable(extensionReceiver: ConeKotlinType, receiverClassIds: Set<ClassId>): Boolean =
        when (val expanded = extensionReceiver.fullyExpandedType(session).lowerBoundIfFlexible()) {
            is ConeTypeParameterType ->
                expanded.lookupTag.symbol.resolvedBounds.all { isApplicable(it.coneType, receiverClassIds) }
            is ConeClassLikeType -> expanded.lookupTag.classId.let { it == StandardClassIds.Any || it in receiverClassIds }
            is ConeDefinitelyNotNullType -> isApplicable(expanded.original, receiverClassIds)
            is ConeIntersectionType -> expanded.intersectedTypes.all { isApplicable(it, receiverClassIds) }
            else -> false
        }
}

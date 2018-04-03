@file:Suppress("UNUSED_PARAMETER")

package sparklin.kshell.repl

import org.jetbrains.kotlin.cli.jvm.compiler.CliLightClassGenerationSupport
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.cli.jvm.repl.DelegatePackageMemberDeclarationProvider
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.descriptors.ClassDescriptorWithResolutionScopes
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingTraceContext
import org.jetbrains.kotlin.resolve.LazyTopDownAnalyzer
import org.jetbrains.kotlin.resolve.TopDownAnalysisContext
import org.jetbrains.kotlin.resolve.TopDownAnalysisMode
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfoFactory
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics
import org.jetbrains.kotlin.resolve.lazy.*
import org.jetbrains.kotlin.resolve.lazy.data.KtClassLikeInfo
import org.jetbrains.kotlin.resolve.lazy.declarations.*
import org.jetbrains.kotlin.resolve.scopes.ImportingScope
import org.jetbrains.kotlin.resolve.scopes.LexicalChainedScope
import org.jetbrains.kotlin.resolve.scopes.LexicalScopeKind
import org.jetbrains.kotlin.resolve.scopes.utils.parentsWithSelf
import org.jetbrains.kotlin.resolve.scopes.utils.replaceImportingScopes
import java.util.NoSuchElementException

class CodeAnalyzer(environment: KotlinCoreEnvironment) {

    private val topDownAnalysisContext: TopDownAnalysisContext
    private val topDownAnalyzer: LazyTopDownAnalyzer
    private val resolveSession: ResolveSession
    private val scriptDeclarationFactory: ScriptMutableDeclarationProviderFactory

//    private val analyzerState = AnalyzerState()

    val module: ModuleDescriptorImpl

    val trace: BindingTraceContext = CliLightClassGenerationSupport.NoScopeRecordCliBindingTrace()

    init {
        // Module source scope is empty because all binary classes are in the dependency module, and all source classes are guaranteed
        // to be found via ResolveSession. The latter is true as long as light classes are not needed in REPL (which is currently true
        // because no symbol declared in the REPL session can be used from Java)
        val container = TopDownAnalyzerFacadeForJVM.createContainer(
                environment.project,
                emptyList(),
                trace,
                environment.configuration,
                environment::createPackagePartProvider,
                { _, _ -> ScriptMutableDeclarationProviderFactory() }
        )

        this.module = container.get<ModuleDescriptorImpl>()
        this.scriptDeclarationFactory = container.get<ScriptMutableDeclarationProviderFactory>()
        this.resolveSession = container.get<ResolveSession>()
        this.topDownAnalysisContext = TopDownAnalysisContext(
                TopDownAnalysisMode.TopLevelDeclarations, DataFlowInfoFactory.EMPTY, resolveSession.declarationScopeProvider
        )
        this.topDownAnalyzer = container.get<LazyTopDownAnalyzer>()
    }

    sealed class AnalyzerResult(val diagnostics: Diagnostics) {
        class Error(diagnostics: Diagnostics) : AnalyzerResult(diagnostics)
        class Success(val descriptor: ClassDescriptorWithResolutionScopes, diagnostics: Diagnostics) : AnalyzerResult(diagnostics)
    }

    fun doAnalyze(linePsi: KtFile, codeLine: CodeLine): AnalyzerResult {
        trace.clearDiagnostics()
//        analyzerState.submitLine(linePsi, codeLine)
        scriptDeclarationFactory.setDelegateFactory(FileBasedDeclarationProviderFactory(resolveSession.storageManager, listOf(linePsi)))
        val context = topDownAnalyzer.analyzeDeclarations(topDownAnalysisContext.topDownAnalysisMode, listOf(linePsi))
        val diagnostics = trace.bindingContext.diagnostics
        val hasErrors = diagnostics.any { it.severity == Severity.ERROR }
        return if (hasErrors) {
//            analyzerState.lineFailure(linePsi, codeLine)
            AnalyzerResult.Error(diagnostics)
        } else {
            val descriptor = context.allClasses.first() // FIXME?
//            analyzerState.lineSuccess(linePsi, codeLine, descriptor)
            AnalyzerResult.Success(descriptor, diagnostics)
        }
    }

    private class ScriptMutableDeclarationProviderFactory : DeclarationProviderFactory {
        private lateinit var delegateFactory: DeclarationProviderFactory
        private lateinit var rootPackageProvider: AdaptablePackageMemberDeclarationProvider

        fun setDelegateFactory(delegateFactory: DeclarationProviderFactory) {
            this.delegateFactory = delegateFactory

            val provider = delegateFactory.getPackageMemberDeclarationProvider(FqName.ROOT)!!
            try {
                rootPackageProvider.addDelegateProvider(provider)
            } catch (e: UninitializedPropertyAccessException) {
                rootPackageProvider = AdaptablePackageMemberDeclarationProvider(provider)
            }
        }

        override fun getClassMemberDeclarationProvider(classLikeInfo: KtClassLikeInfo): ClassMemberDeclarationProvider {
            return delegateFactory.getClassMemberDeclarationProvider(classLikeInfo)
        }

        override fun getPackageMemberDeclarationProvider(packageFqName: FqName): PackageMemberDeclarationProvider? {
            if (packageFqName.isRoot) {
                return rootPackageProvider
            }

            return delegateFactory.getPackageMemberDeclarationProvider(packageFqName)
        }

        override fun diagnoseMissingPackageFragment(fqName: FqName, file: KtFile?) {
            delegateFactory.diagnoseMissingPackageFragment(fqName, file)
        }

        class AdaptablePackageMemberDeclarationProvider(
                private var delegateProvider: PackageMemberDeclarationProvider
        ) : DelegatePackageMemberDeclarationProvider(delegateProvider) {
            fun addDelegateProvider(provider: PackageMemberDeclarationProvider) {
                delegateProvider = CombinedPackageMemberDeclarationProvider(listOf(provider, delegateProvider))

                delegate = delegateProvider
            }
        }
    }

//    class AnalyzerState {
//        private val successfulLines = mutableListOf<LineInfo.SuccessfulLine>()
//        private val submittedLines = hashMapOf<KtFile, LineInfo>()
//
////        fun resetToLine(lineId: ILineId): List<ReplCodeLine> {
////            val removed = successfulLines.resetToLine(lineId.no)
////            removed.forEach { submittedLines.remove(it.second.linePsi) }
////            return removed.map { it.first }
////        }
////
////        fun reset(): List<ReplCodeLine> {
////            submittedLines.clear()
////            return successfulLines.reset().map { it.first }
////        }
//
//        fun submitLine(ktFile: KtFile, codeLine: CodeLine) {
//            val line = LineInfo.SubmittedLine(ktFile, successfulLines.lastOrNull())
//            submittedLines[ktFile] = line
//            ktFile.fileScopesCustomizer = object : FileScopesCustomizer {
//                override fun createFileScopes(fileScopeFactory: FileScopeFactory): FileScopes {
//                    return lineInfo(ktFile)?.let { computeFileScopes(it, fileScopeFactory) }
//                            ?: fileScopeFactory.createScopesForFile(ktFile)
//                }
//            }
//        }
//
//        fun lineSuccess(ktFile: KtFile, codeLine: CodeLine, scriptDescriptor: ClassDescriptorWithResolutionScopes) {
//            val successfulLine = LineInfo.SuccessfulLine(ktFile, successfulLines.lastOrNull(), scriptDescriptor)
//            submittedLines[ktFile] = successfulLine
//            successfulLines.add(/*ompiledReplCodeLine(ktFile.name, codeLine), */successfulLine)
//        }
//
//        fun lineFailure(ktFile: KtFile, codeLine: CodeLine) {
//            submittedLines[ktFile] = LineInfo.FailedLine(ktFile, successfulLines.lastOrNull())
//        }
//
//        private fun lineInfo(ktFile: KtFile) = submittedLines[ktFile]
//
//        // use sealed?
//        private sealed class LineInfo {
//            abstract val linePsi: KtFile
//            abstract val parentLine: SuccessfulLine?
//
//            class SubmittedLine(override val linePsi: KtFile, override val parentLine: SuccessfulLine?) : LineInfo()
//            class SuccessfulLine(override val linePsi: KtFile, override val parentLine: SuccessfulLine?, val lineDescriptor: ClassDescriptorWithResolutionScopes) : LineInfo()
//            class FailedLine(override val linePsi: KtFile, override val parentLine: SuccessfulLine?) : LineInfo()
//        }
//
//        private fun computeFileScopes(lineInfo: LineInfo, fileScopeFactory: FileScopeFactory): FileScopes? {
//            val previousLineDescriptor = lineInfo.parentLine?.lineDescriptor ?: return null
//
//            val lexicalScopeAfterLastLine = previousLineDescriptor.scopeForInitializerResolution
//            val lastLineImports = lexicalScopeAfterLastLine.parentsWithSelf.firstIsInstance<ImportingScope>()
//
//            val scopesForThisLine = fileScopeFactory.createScopesForFile(lineInfo.linePsi, lastLineImports)
//
//            // Create scope that wraps previous line lexical scope and adds imports from this line
//            val newScope = LexicalChainedScope(
//                    lexicalScopeAfterLastLine.replaceImportingScopes(scopesForThisLine.importingScope),
//                    previousLineDescriptor,
//                    false,
//                    null,
//                    LexicalScopeKind.CODE_BLOCK,
//                    listOf(previousLineDescriptor.unsubstitutedMemberScope))
//
//            return FileScopes(newScope, scopesForThisLine.importingScope, scopesForThisLine.importResolver)
//            // create scope that wraps previous line lexical scope and adds imports from this line
////            val lexicalScopeAfterLastLine = lineInfo.parentLine?.lineDescriptor?.scopeForInitializerResolution ?: return null
////            val lastLineImports = lexicalScopeAfterLastLine.parentsWithSelf.first { it is ImportingScope } as ImportingScope
////            val scopesForThisLine = fileScopeFactory.createScopesForFile(lineInfo.linePsi, lastLineImports)
////            val combinedLexicalScopes = lexicalScopeAfterLastLine.replaceImportingScopes(scopesForThisLine.importingScope)
////            return FileScopes(combinedLexicalScopes, scopesForThisLine.importingScope, scopesForThisLine.importResolver)
//        }
//
//        inline fun <reified T> Sequence<*>.firstIsInstance(): T {
//            for (element in this) if (element is T) return element
//            throw NoSuchElementException("No element of given type found")
//        }
//    }
}

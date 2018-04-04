package sparklin.kshell.repl

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.KotlinCodegenFacade
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import kotlin.concurrent.write


class ReplCompiler(disposable: Disposable,
                   private val compilerConfiguration: CompilerConfiguration,
                   messageCollector: MessageCollector
) {

    constructor(compilerConfiguration: CompilerConfiguration, messageCollector: MessageCollector) :
            this(Disposer.newDisposable(), compilerConfiguration, messageCollector)

    private val checker = ReplChecker(disposable, compilerConfiguration, messageCollector)

    private val analyzerEngine = CodeAnalyzer(checker.environment)

    fun compile(state: State, codeLine: CodeLine, previousStage: List<Snippet> = listOf()): Result<Pair<List<Snippet>, CompiledClasses>> {
        state.lock.write {
            val lineResult = checker.check(state, codeLine, true)
            val checkedLine = when (lineResult) {
                is Result.Incomplete -> return Result.Incomplete()
                is Result.Error -> return Result.Error(lineResult.message, lineResult.location)
                is Result.Success -> lineResult.data
            }

            val psiFile = checkedLine.psiFile
            val errorHolder = checkedLine.errorHolder

            val generatedClassname = makeFileBaseName(codeLine)

            val snippets = psiToSnippets(psiFile, generatedClassname)

            val (actualSnippets, deferredSnippets) = checkOverloads(snippets)

            val import = state.snippets.filterNamed().map { it.copy() as NamedSnippet }

            import.shadow(previousStage)

            val code = generateKotlinCodeFor(generatedClassname, import/*state.snippets.filterNamed()*/ + previousStage.filterNamed(), actualSnippets)

            println(">>>>> code >>>>>\n" + code + "\n>>>>>>>>>>>>>>")
            val result = checker.check(state, CodeLine(codeLine.no, code, codeLine.part), false)
            val psiForObject = when (result) {
                is Result.Success -> result.data.psiFile
                else -> throw IllegalStateException("Should never happen")
            }

            val analysisResult = analyzerEngine.doAnalyze(psiForObject, codeLine)

            AnalyzerWithCompilerReport.reportDiagnostics(analysisResult.diagnostics, errorHolder)

            val descriptor = when (analysisResult) {
                is CodeAnalyzer.AnalyzerResult.Error -> return Result.Error(errorHolder.renderedDiagnostics)
                is CodeAnalyzer.AnalyzerResult.Success -> analysisResult.descriptor
            }

            val expression = psiForObject.getChildOfType<KtObjectDeclaration>()
                    ?.declarations
                    ?.find { it.name == RESULT_FIELD_NAME }
                    ?.getChildOfType<KtCallExpression>()

            val type = expression?.let {
                analyzerEngine.trace.bindingContext.getType(it)
            }?.let {
                DescriptorRenderer.FQ_NAMES_IN_TYPES.renderType(it)
            }

            val generationState = GenerationState.Builder(
                    psiForObject.project,
                    ClassBuilderFactories.binaries(false),
                    analyzerEngine.module,
                    analyzerEngine.trace.bindingContext,
                    listOf(psiForObject),
                    compilerConfiguration
            ).build()

            generationState.beforeCompile()

            KotlinCodegenFacade.generatePackage(
                    generationState,
                    psiForObject.packageFqName,
                    setOf(psiForObject.containingKtFile),
                    org.jetbrains.kotlin.codegen.CompilationErrorHandler.THROW_EXCEPTION)

            generationState.factory.asList().forEach { println(it) } // print classes

            actualSnippets.filterNamed().forEach {
                if (it is FunctionSnippet) {
                    val typeParameters = it.psi.typeParameters.map { it.name!! }
                    it.parametersTypes = it.psi.valueParameters
                            .joinToString(separator = ",") { p ->
                                qName(p.typeReference!!.typeElement!!.text, state.snippets, typeParameters)
                            }
                }
            }

            if (type != null && type != "kotlin.Unit") {
                snippets.add(SyntheticImportSnippet(generatedClassname, RESULT_FIELD_NAME, "res${state.resultIndex.getAndIncrement()}"))
            }

            val classes = CompiledClasses(
                    generatedClassname,
                    generationState.factory.asList().map { CompiledClassData(it.relativePath, it.asByteArray()) },
                    false,
                    type)

            return if (deferredSnippets.isNotEmpty()) {
                val otherCode = deferredSnippets.joinToString(separator = "\n") { it.code() }
                val otherResult = compile(state, CodeLine(codeLine.no, otherCode, codeLine.part + 1), actualSnippets)

                when (otherResult) {
                    is Result.Error -> otherResult
                    is Result.Incomplete -> throw IllegalStateException("Should never happen")
                    is Result.Success -> Result.Success(Pair(actualSnippets + otherResult.data.first, classes + otherResult.data.second))
                }
            } else {
                Result.Success(Pair(snippets, classes))
            }
        }
    }

    private fun qName(parameterType: String, snippets: List<Snippet>, typeParameters: List<String>): String {
        val ind = typeParameters.indexOf(parameterType)
        return if (ind < 0) {
            snippets.filterDeclarations()
                    .findLast { !it.shadowed && it.name == parameterType }
                    ?.let { "${it.klass}.$parameterType" } ?: parameterType
        } else {
            "#$ind"
        }
    }

    private operator fun CompiledClasses.plus(other: CompiledClasses) =
            CompiledClasses(other.mainClassName, this.classes + other.classes, other.hasResult, other.type)

    private fun psiToSnippets(psiFile: KtFile, generatedClassname: String): MutableList<Snippet> {
        val snippets = mutableListOf<Snippet>()
        psiFile.getChildOfType<KtScript>()?.declarations?.forEach {
            snippets.add(when (it) {
                is KtProperty -> PropertySnippet(generatedClassname, it.name!!, it)
                is KtFunction -> FunctionSnippet(generatedClassname, it.name!!, it)
                is KtScriptInitializer -> InitializerSnippet(generatedClassname, it)
                is KtObjectDeclaration -> ObjectSnippet(generatedClassname, it.name!!, it)
                is KtClass -> ClassSnippet(generatedClassname, it.name!!, it)
                else -> throw IllegalArgumentException("Unexpected top level declaration: ${it.javaClass.kotlin}")
            })
        }
        psiFile.getChildOfType<KtScript>()?.getChildOfType<KtImportList>()?.imports?.forEach {
            snippets.add(ImportSnippet(generatedClassname, it))
        }
        return snippets
    }

    private fun checkOverloads(snippets: List<Snippet>): Pair<List<Snippet>, List<Snippet>> {
        val actual = mutableListOf<Snippet>()
        val deferred = mutableListOf<Snippet>()

        val initializers = mutableListOf<InitializerSnippet>()
        val imports = mutableListOf<ImportSnippet>()

        var current = actual

        snippets.forEach {
            when (it) {
                is NamedSnippet -> {
                    // FIXME: classes and functions are in the same namespace?
                    if (actual.containsWithName(it.name)) current = deferred
                    current.add(it)
                }
                is ImportSnippet -> imports.add(it)
                is InitializerSnippet -> initializers.add(it)
            }
        }

        if (deferred.isNotEmpty()) {
            deferred.addAll(0, imports)
            deferred.addAll(initializers)
        } else {
            actual.addAll(0, imports)
            actual.addAll(initializers)
        }

        return Pair(actual, deferred)
    }

    private fun generateKotlinCodeFor(classname: String, imports: List<NamedSnippet>, snippets: List<Snippet>): String {
        val code = StringBuilder()
        imports.forEach {
            if (it is FunctionSnippet) {
                if (!it.shadowed) code.appendln(it.toImportStatement())
            } else {
                code.appendln(it.toImportStatement())
            }
        }
        snippets.filter { it is ImportSnippet }.forEach { code.appendln(it.code()) }
        code.appendln("object $classname {")
        snippets.filter { it is DeclarationSnippet }.forEach { code.appendln(it.code()) }
        code.appendln("val __run = {")
        snippets.filter { it is InitializerSnippet }.forEach { code.appendln(it.code()) }
        code.appendln("}")
        code.appendln("val $RESULT_FIELD_NAME=__run()")
        code.appendln("}")
        return code.toString()
    }

    companion object {
        const val RESULT_FIELD_NAME = "__result"
    }

    private inline fun <reified T : PsiElement> PsiElement.getChildOfType(): T? {
        return PsiTreeUtil.getChildOfType(this, T::class.java)
    }
}
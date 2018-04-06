package sparklin.kshell.repl

import com.intellij.openapi.Disposable
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
    private val checker = ReplChecker(disposable, compilerConfiguration, messageCollector)

    private val analyzerEngine = CodeAnalyzer(checker.environment)

    fun compile(state: ReplState, codeLine: CodeLine, previousStage: List<Snippet> = listOf()): Result<CompilationData, EvalError.CompileError> {
        state.lock.write {
            val lineResult = checker.check(state, codeLine, true)
            val checkedLine = when (lineResult) {
                is Result.Incomplete -> return Result.Incomplete()
                is Result.Error -> return Result.Error(lineResult.error)
                is Result.Success -> lineResult.data
            }

            val psiFile = checkedLine.psiFile
            val errorHolder = checkedLine.errorHolder

            val generatedClassname = makeFileBaseName(codeLine)

            val snippets = psiToSnippets(psiFile, generatedClassname)

            val permanents = state.history.filter<SyntheticImportSnippet>().map { it.alias }
            val conflict = snippets.filter<DeclarationSnippet>().find { permanents.contains(it.name) }

            if (conflict != null) {
                return Result.Error(EvalError.CompileError("${conflict.name} cannot be replaced"))
            }

            val (actualSnippets, deferredSnippets) = checkOverloads(snippets)

            val import = state.history.map { it.copy() }

            import.shadow(previousStage)

            val code = generateKotlinCodeFor(generatedClassname, import + previousStage, actualSnippets)

            val result = checker.check(state, CodeLine(codeLine.no, code, codeLine.part), false)
            val psiForObject = when (result) {
                is Result.Success -> result.data.psiFile
                else -> throw IllegalStateException("Should never happen")
            }

            val analysisResult = analyzerEngine.doAnalyze(psiForObject, codeLine)

            AnalyzerWithCompilerReport.reportDiagnostics(analysisResult.diagnostics, errorHolder)

            if (analysisResult is CodeAnalyzer.AnalyzerResult.Error)
                return Result.Error(EvalError.CompileError(errorHolder.renderedDiagnostics))

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

            actualSnippets.filter<NamedSnippet>().forEach {
                if (it is FunctionSnippet) {
                    it.parametersTypes = canonicalParameterTypes(state.history, it)
                }
            }

            val hasResult = type != null && type != "kotlin.Unit"

            var valueResultVariable: String? = null

            if (hasResult) {
                valueResultVariable = "res${state.resultIndex.getAndIncrement()}"
                snippets.add(SyntheticImportSnippet(generatedClassname, RESULT_FIELD_NAME, valueResultVariable))
            }

            val classes = CompiledClasses(
                    generatedClassname,
                    generationState.factory.asList().map { CompiledClassData(it.relativePath, it.asByteArray()) },
                    hasResult,
                    valueResultVariable,
                    type)

            return if (deferredSnippets.isNotEmpty()) {
                val otherCode = deferredSnippets.joinToString(separator = "\n") { it.code() }
                val otherResult = compile(state, CodeLine(codeLine.no, otherCode, codeLine.part + 1), actualSnippets)

                when (otherResult) {
                    is Result.Error -> otherResult
                    is Result.Incomplete -> throw IllegalStateException("Should never happen")
                    is Result.Success -> Result.Success(CompilationData(actualSnippets + otherResult.data.snippets, classes + otherResult.data.classes))
                }
            } else {
                Result.Success(CompilationData(snippets, classes))
            }
        }
    }

    private fun canonicalParameterTypes(history: List<Snippet>, func: FunctionSnippet): String {
        fun qName(parameterType: String, snippets: List<Snippet>, typeParameters: List<String>): String {
            val ind = typeParameters.indexOf(parameterType)
            return if (ind < 0) {
                snippets.filter<DeclarationSnippet>()
                        .findLast { !it.shadowed && it.name == parameterType }
                        ?.let { "${it.klass}.$parameterType" } ?: parameterType
            } else {
                "#$ind"
            }
        }

        fun mkString(typeElement: KtTypeElement, history: List<Snippet>, typeParameters: List<String>): String {
            val name = typeElement.getChildOfType<KtExpression>()!!.text
            val args = typeElement.typeArgumentsAsTypes.map { mkString(it.typeElement!!, history, typeParameters) }
            return qName(name, history, typeParameters) + if (args.isNotEmpty()) "<" + args.joinToString(separator = ",") + ">" else ""
        }

        val typeParameters = func.psi.typeParameters.map { it.name!! }

        return func.psi.valueParameters
                .joinToString(separator = ",") { p ->
                    mkString(p.typeReference!!.getChildOfType()!!, history, typeParameters)
                }
    }

    private operator fun CompiledClasses.plus(other: CompiledClasses) =
            CompiledClasses(other.mainClassName, this.classes + other.classes, other.hasResult, other.valueResultVariable, other.type)

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
        psiFile.getChildOfType<KtImportList>()?.imports?.forEach {
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
                    if (actual.containsWithName(it.name)) current = deferred
                    current.add(it)
                }
                is ImportSnippet -> imports.add(it)
                is InitializerSnippet -> initializers.add(it)
            }
        }

        actual.addAll(0, imports)
        if (deferred.isNotEmpty()) {
            deferred.addAll(0, imports)
            deferred.addAll(initializers)
        } else {
            actual.addAll(initializers)
        }

        return Pair(actual, deferred)
    }

    private fun generateKotlinCodeFor(classname: String, imports: List<Snippet>, snippets: List<Snippet>): String {
        val code = StringBuilder()

        imports.filter<NamedSnippet>().forEach {
            when (it) {
                is DeclarationSnippet -> if (!it.shadowed) code.appendln(it.toImportStatement())
                else -> code.appendln(it.toImportStatement())
            }
        }

        (imports.filter<ImportSnippet>() + snippets.filter())
                .map { it.code() }
                .distinct()
                .forEach { code.appendln(it) }

        code.appendln("object $classname {")
        snippets.filter<DeclarationSnippet>().forEach { code.appendln(it.code()) }
        code.appendln("val __run = {")
        snippets.filter<InitializerSnippet>().forEach { code.appendln(it.code()) }
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
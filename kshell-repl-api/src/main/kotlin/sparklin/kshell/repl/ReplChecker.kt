package sparklin.kshell.repl

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.PsiFileFactoryImpl
import com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.*
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.repl.KOTLIN_REPL_JVM_TARGET_PROPERTY
import org.jetbrains.kotlin.cli.jvm.repl.messages.DiagnosticMessageHolder
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtFile
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.concurrent.write


class ReplChecker(
        disposable: Disposable,
        private val compilerConfiguration: CompilerConfiguration,
        messageCollector: MessageCollector
) {

    val environment = run {
        compilerConfiguration.apply {
            put<MessageCollector>(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)
            put(JVMConfigurationKeys.RETAIN_OUTPUT_IN_MEMORY, true)

            if (get(JVMConfigurationKeys.JVM_TARGET) == null) {
                put(JVMConfigurationKeys.JVM_TARGET,
                        System.getProperty(KOTLIN_REPL_JVM_TARGET_PROPERTY)?.let { JvmTarget.fromString(it) }
                                ?: if (getJavaVersion() >= 0x10008) JvmTarget.JVM_1_8 else JvmTarget.JVM_1_6)
            }
        }
        KotlinCoreEnvironment.createForProduction(disposable, compilerConfiguration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
    }

    private val psiFileFactory: PsiFileFactoryImpl = PsiFileFactory.getInstance(environment.project) as PsiFileFactoryImpl

    private fun createDiagnosticHolder() = ConsoleDiagnosticMessageHolder()

    fun check(state: ReplState, codeLine: CodeLine, isScript: Boolean): Result<CheckedCode, EvalError.CompileError> {
        state.lock.write {
            val fileName = makeFileBaseName(codeLine) + (if (isScript) ".kts" else ".kt")
            val virtualFile =
                    LightVirtualFile(fileName, KotlinLanguage.INSTANCE, StringUtil.convertLineSeparators(codeLine.code)).apply {
                        charset = CharsetToolkit.UTF8_CHARSET
                    }

            val psiFile: KtFile =  psiFileFactory.trySetupPsiForFile(virtualFile, KotlinLanguage.INSTANCE, true, false) as KtFile?
                        ?: error("File not analyzed at line ${codeLine.no}: ${codeLine.code}")

            val errorHolder = createDiagnosticHolder()

            val syntaxErrorReport = AnalyzerWithCompilerReport.reportSyntaxErrors(psiFile, errorHolder)

            return when {
                syntaxErrorReport.isHasErrors && syntaxErrorReport.isAllErrorsAtEof -> Result.Incomplete()
                syntaxErrorReport.isHasErrors -> Result.Error(EvalError.CompileError(errorHolder.renderMessage()))
                else -> Result.Success(CheckedCode(psiFile, errorHolder))
            }
        }
    }
}

class ConsoleDiagnosticMessageHolder : MessageCollectorBasedReporter, DiagnosticMessageHolder {
    override val renderedDiagnostics: String
        get() = renderMessage()

    private val outputStream = ByteArrayOutputStream()

    override val messageCollector: GroupingMessageCollector = GroupingMessageCollector(
            PrintingMessageCollector(PrintStream(outputStream), MessageRenderer.WITHOUT_PATHS, false),
            false)

    fun renderMessage(): String {
        messageCollector.flush()
        return outputStream.toString("UTF-8")
    }
}
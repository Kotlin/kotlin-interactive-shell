package completion

import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.cli.jvm.compiler.CliLightClassGenerationSupport
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.descriptors.ModuleDescriptor

import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.LazyTopDownAnalyzer
import org.jetbrains.kotlin.resolve.TopDownAnalysisMode
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.util.KotlinFrontEndException

object KotlinAnalyzer {
//    private fun resolve(file: KtFile, environment: KotlinCommonEnvironment): AnalysisResultWithProvider {
//        return when (environment) {
//            is KotlinScriptEnvironment -> EclipseAnalyzerFacadeForJVM.analyzeScript(environment, file)
//            is KotlinEnvironment -> EclipseAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(environment, listOf(file))
//            else -> throw IllegalArgumentException("Could not analyze file with environment: $environment")
//        }
//    }

    public fun analyzeScript(
            environment: KotlinCoreEnvironment,
            scriptFile: KtFile): AnalysisResultWithProvider {

//        if (environment.isInitializingScriptDefinitions) {
//            // We can't start resolve when script definitions are not initialized
//            return AnalysisResultWithProvider.EMPTY
//        }

        val trace = CliLightClassGenerationSupport.CliBindingTrace()

        val container = TopDownAnalyzerFacadeForJVM.createContainer(
                environment.project,
                setOf(scriptFile),
                trace,
                environment.configuration,
                environment::createPackagePartProvider,
                { storageManager: StorageManager, files: Collection<KtFile> -> FileBasedDeclarationProviderFactory(storageManager, files) }
        )

        try {
            container.get<LazyTopDownAnalyzer>().analyzeDeclarations(TopDownAnalysisMode.TopLevelDeclarations, setOf(scriptFile))
        } catch(e: KotlinFrontEndException) {
//          Editor will break if we do not catch this exception
//          and will not be able to save content without reopening it.
//          In IDEA this exception throws only in CLI
            println("**** " + e)
            //KotlinLogger.logError(e)
        }

        return AnalysisResultWithProvider(
                AnalysisResult.success(trace.getBindingContext(), container.get<ModuleDescriptor>()),
                container)
    }
}

data class AnalysisResultWithProvider(val analysisResult: AnalysisResult, val componentProvider: ComponentProvider?) {
//    companion object {
//        val EMPTY = AnalysisResultWithProvider(AnalysisResult.EMPTY, null)
//    }
}
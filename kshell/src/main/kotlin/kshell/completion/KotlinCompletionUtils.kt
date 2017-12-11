package kshell.completion

import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.util.text.StringUtilRt
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.PsiFileFactoryImpl
import com.intellij.testFramework.LightVirtualFile
import kshell.KShell
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.codeInsight.ReferenceVariantsHelper
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter


object KotlinCompletionUtils {

    private val KOTLIN_DUMMY_IDENTIFIER = "KotlinRulezzz"

    fun applicableNameFor(prefix: String, name: Name): Boolean {
        return !name.isSpecial && applicableNameFor(prefix, name.identifier)
    }

    fun applicableNameFor(prefix: String, completion: String): Boolean {
        return completion.startsWith(prefix) ||
                completion.toLowerCase().startsWith(prefix) //|| SearchPattern.camelCaseMatch(prefix, completion)
    }

    fun getReferenceVariants(repl: KShell,
                                    simpleNameExpression: KtSimpleNameExpression,
                                    nameFilter: (Name) -> Boolean,
                                    identifierPart: String?
    ): Collection<DeclarationDescriptor> {
        val ktFile = simpleNameExpression.getContainingKtFile()
        val (analysisResult, container) = KotlinAnalyzer.analyzeScript(getReplEnvironment(repl), ktFile)
        if (container == null) return emptyList()

//        val inDescriptor = simpleNameExpression
//                .getReferencedNameElement()
//                .getResolutionScope(analysisResult.bindingContext)
//                .ownerDescriptor

//        val showNonVisibleMembers = false
//                !JavaPlugin.getDefault().getPreferenceStore().getBoolean(PreferenceConstants.CODEASSIST_SHOW_VISIBLE_PROPOSALS)
//
//        val visibilityFilter = { descriptor: DeclarationDescriptor ->
//            when (descriptor) {
//                is TypeParameterDescriptor -> descriptor.isVisible(inDescriptor)
//
//                is DeclarationDescriptorWithVisibility -> {
//                    showNonVisibleMembers || descriptor.isVisible(inDescriptor, analysisResult.bindingContext, simpleNameExpression)
//                }
//
//                else -> true
//            }
//        }

        val visibilityFilter = { _: DeclarationDescriptor -> true }
        val collectAll = (identifierPart == null || identifierPart.length > 2) // || !KotlinScriptEnvironment.isScript(file)
        val kind = if (collectAll) DescriptorKindFilter.ALL else DescriptorKindFilter.CALLABLES

        return ReferenceVariantsHelper(
                analysisResult.bindingContext,
                KotlinResolutionFacade(repl, container, analysisResult.moduleDescriptor),
                analysisResult.moduleDescriptor,
                visibilityFilter).getReferenceVariants(
                simpleNameExpression, kind, nameFilter)
    }

    fun getPsiElement(repl: KShell, sourceCode: String, identOffset: Int): PsiElement? {
        val sourceCodeWithMarker = StringBuilder(sourceCode).insert(identOffset, KOTLIN_DUMMY_IDENTIFIER).toString()
        val jetFile: KtFile? = parseText(repl, StringUtilRt.convertLineSeparators(sourceCodeWithMarker))

        if (jetFile == null) return null

        val offsetWithourCR = identOffset // FIXME: LineEndUtil.convertCrToDocumentOffset(sourceCodeWithMarker, identOffset, editor.document)
        return jetFile.findElementAt(offsetWithourCR)
    }


    fun parseText(repl: KShell, text: String): KtFile? {
        StringUtil.assertValidSeparators(text)

        val project = getReplEnvironment(repl).project

        val virtualFile = KotlinLightVirtualFile(text)
        virtualFile.setCharset(CharsetToolkit.UTF8_CHARSET)

        val psiFileFactory = PsiFileFactory.getInstance(project) as PsiFileFactoryImpl

        return psiFileFactory.trySetupPsiForFile(virtualFile, KotlinLanguage.INSTANCE, true, false) as? KtFile
    }
}

val KOTLIN_REPL_FILENAME = "script.kts"

public class KotlinLightVirtualFile(text: String) :
        LightVirtualFile(KOTLIN_REPL_FILENAME, KotlinLanguage.INSTANCE, text) {

    override fun getPath(): String = "myMagicPath" //FIXME
}



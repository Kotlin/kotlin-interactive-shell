package completion

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import kshell.KShell
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.DescriptorUtils

class KotlinCompletion(val repl: KShell) {
    fun generateCompletionProposals(text: String, offset: Int): List<CompletionProposal> {
        val (identifierPart, identifierStart) = getIdentifierInfo(text, offset)
        val psiElement = KotlinCompletionUtils.getPsiElement(repl, text, identifierStart)
        val simpleNameExpression = PsiTreeUtil.getParentOfType(psiElement, KtSimpleNameExpression::class.java)

        val proposals = arrayListOf<CompletionProposal>().apply {
            if (simpleNameExpression != null) {
                addAll(collectCompletionProposals(generateBasicCompletionProposals(identifierPart, simpleNameExpression), identifierPart))

                if (identifierPart.isNotBlank()) {
                    addAll(generateNonImportedCompletionProposals(identifierPart, simpleNameExpression))
                }

            }
            if (psiElement != null) {
//                addAll(generateKeywordProposals(identifierPart, psiElement))
//                addAll(generateTemplateProposals(psiElement.containingFile, offset, identifierPart))
            }
        }

        return proposals
    }

    private fun collectCompletionProposals(descriptors: Collection<DeclarationDescriptor>, part: String): List<CompletionProposal> {
        return descriptors.map { descriptor ->
            val completion = descriptor.name.identifier
            val presentableString = DescriptorRenderer.ONLY_NAMES_WITH_SHORT_TYPES.render(descriptor)
            val containmentPresentableString = if (descriptor is ClassDescriptor) {
                val fqName = DescriptorUtils.getFqName(descriptor)
                if (fqName.isRoot) "<root>" else fqName.parent().asString()
            } else {
                null
            }
            CompletionProposal(completion)
//            val proposal = KotlinCompletionProposal(
//                    completion,
//                    image,
//                    presentableString,
//                    containmentPresentableString,
//                    null,
//                    completion,
//                    part)
//
//            withKotlinInsertHandler(descriptor, proposal, part)
        }
    }

    private fun generateBasicCompletionProposals(identifierPart: String, expression: KtSimpleNameExpression): Collection<DeclarationDescriptor> {


        val nameFilter: (Name) -> Boolean = { name -> KotlinCompletionUtils.applicableNameFor(identifierPart, name) }

        return KotlinCompletionUtils.getReferenceVariants(repl, expression, nameFilter, identifierPart)
    }

    private fun generateNonImportedCompletionProposals(
            identifierPart: String,
            expression: KtSimpleNameExpression//,
            /*javaProject: IJavaProject*/): List<CompletionProposal> {
        return listOf()
//        val file = editor.eclipseFile ?: return emptyList()
//        val ktFile = editor.parsedFile ?: return emptyList()
//
//        return lookupNonImportedTypes(expression, identifierPart, ktFile, javaProject).map {
//            val imageDescriptor = JavaElementImageProvider.getTypeImageDescriptor(false, false, it.modifiers, false)
//            val image = JavaPlugin.getImageDescriptorRegistry().get(imageDescriptor)
//
//            KotlinImportCompletionProposal(it, image, file, identifierPart)
//        }
    }


    private fun generateKeywordProposals(identifierPart: String, expression: PsiElement): List<CompletionProposal> {
        return listOf()
//        val callTypeAndReceiver = if (expression is KtSimpleNameExpression) CallTypeAndReceiver.detect(expression) else null

//        return arrayListOf<String>().apply {
//            KeywordCompletion.complete(expression, identifierPart, true) { keywordProposal ->
//                if (!KotlinCompletionUtils.applicableNameFor(identifierPart, keywordProposal)) return@complete
//
//                when (keywordProposal) {
//                    "break", "continue" -> {
//                        if (expression is KtSimpleNameExpression) {
//                            addAll(breakOrContinueExpressionItems(expression, keywordProposal))
//                        }
//                    }
//
//                    "class" -> {
//                        if (callTypeAndReceiver !is CallTypeAndReceiver.CALLABLE_REFERENCE) {
//                            add(keywordProposal)
//                        }
//                    }
//
//                    "this", "return" -> {
//                        if (expression is KtExpression) {
//                            add(keywordProposal)
//                        }
//                    }
//
//                    else -> add(keywordProposal)
//                }
//            }
//        }.map { KotlinKeywordCompletionProposal(it, identifierPart) }
    }

    private fun generateTemplateProposals(
            psiFile: PsiFile, offset: Int, identifierPart: String): List<CompletionProposal> {
          return listOf()
//        val contextTypeIds = KotlinApplicableTemplateContext.getApplicableContextTypeIds(viewer, psiFile, offset - identifierPart.length)
//        val region = Region(offset - identifierPart.length, identifierPart.length)
//
//        val templateIcon = JavaPluginImages.get(JavaPluginImages.IMG_OBJS_TEMPLATE)
//        val templates = KotlinApplicableTemplateContext.getTemplatesByContextTypeIds(contextTypeIds)
//
//        return templates
//                .filter { it.name.startsWith(identifierPart) }
//                .map {
//                    val templateContext = createTemplateContext(region, it.contextTypeId)
//                    TemplateProposal(it, templateContext, region, templateIcon)
//                }

    }


}

data class CompletionProposal(val completion: String)

data class IdentifierInfo(val identifierPart: String, val identifierStart: Int)

fun getIdentifierInfo(text: String, offset: Int): IdentifierInfo {
    var identStartOffset = offset
    while ((identStartOffset != 0) && Character.isUnicodeIdentifierPart(text[identStartOffset - 1])) {
        identStartOffset--
    }
    return IdentifierInfo(text.substring(identStartOffset, offset), identStartOffset)
}
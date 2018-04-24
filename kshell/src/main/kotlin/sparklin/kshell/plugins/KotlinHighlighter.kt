package sparklin.kshell.plugins

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.lexer.KtKeywordToken
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtUserType
import sparklin.kshell.org.jline.utils.AttributedString
import sparklin.kshell.org.jline.utils.AttributedStringBuilder
import sparklin.kshell.org.jline.utils.AttributedStyle
import sparklin.kshell.repl.ReplChecker
import sparklin.kshell.repl.ReplState
import sparklin.kshell.repl.Result
import sparklin.kshell.repl.SourceCode

class KotlinHighlighter(private val state: ReplState, private val checker: () -> ReplChecker,
                        private val styles: SyntaxPlugin.HighlightStyles): BaseHighlighter {


    override fun highlight(buffer: String, offset: Int): AttributedString {
        require(offset >= 0)
        val code = buffer.substring(offset)
        val fragment = CodeFragment(code)
        val lineResult = checker().check(state, fragment, true)

        val psi = when (lineResult) {
            is Result.Error -> lineResult.error.psiFile
            is Result.Success -> lineResult.data.psiFile
        }

        val sb = AttributedStringBuilder()
        if (offset != 0) sb.append(buffer.substring(0, offset))
        for (i in code.indices) {
            psi.findElementAt(i)?.let { element ->
                val st = when {
                    element.isKeyword() -> styles.keyword
                    element.isFunction() -> styles.function
                    element.isNumber() -> styles.number
                    element.isString() -> styles.string
                    element.isStringTemplate() -> styles.stringTemplate
                    element.isType() -> styles.type
                    else -> null
                } ?: AttributedStyle.DEFAULT
                sb.style(st)
                sb.append(code[i])
            }
        }
        return sb.toAttributedString()
    }

    private fun PsiElement.isKeyword() = node?.elementType is KtKeywordToken
    private fun PsiElement.isType() = isIdentifier() && parent?.parent is KtUserType
    private fun PsiElement.isFunction() = isIdentifier() && parent is KtFunction //|| parent?.parent is KtCallExpression)
    private fun PsiElement.isNumber() = checkElementType("INTEGER_LITERAL",  "FLOAT_CONSTANT")
    private fun PsiElement.isString() = checkElementType("REGULAR_STRING_PART", "OPEN_QUOTE", "CLOSING_QUOTE")
    private fun PsiElement.isStringTemplate() = node?.elementType.toString().contains("TEMPLATE_ENTRY")
    private fun PsiElement.isIdentifier() = checkElementType("IDENTIFIER")

    private fun PsiElement.checkElementType(s: String, vararg ss: String): Boolean {
        val e = node?.elementType.toString()
        if (e == s) return true
        return ss.any { it == e }
    }

    private class CodeFragment(override val code: String): SourceCode {
        override val no: Int = 0
        override val part: Int = 0
        override fun mkFileName(): String = "Fragment"
        override fun nextPart(codePart: String): SourceCode = throw UnsupportedOperationException("Should never happen")
        override fun replace(code: String): SourceCode = throw UnsupportedOperationException("Should never happen")
    }
}
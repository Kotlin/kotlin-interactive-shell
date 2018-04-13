package sparklin.kshell.plugins

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.lexer.KtKeywordToken
import org.jetbrains.kotlin.psi.*
import sparklin.kshell.org.jline.reader.Highlighter
import sparklin.kshell.org.jline.reader.LineReader
import sparklin.kshell.org.jline.utils.AttributedString
import sparklin.kshell.org.jline.utils.AttributedStringBuilder
import sparklin.kshell.org.jline.utils.AttributedStyle
import sparklin.kshell.repl.Code
import sparklin.kshell.repl.ReplChecker
import sparklin.kshell.repl.ReplState
import sparklin.kshell.repl.Result
import java.util.concurrent.atomic.AtomicInteger

class KotlinHighlighter(private val state: ReplState,
                        private val checker: ReplChecker,
                        private val styles: Map<KotlinElement, AttributedStyle>): Highlighter {
    private val counter = AtomicInteger(0)

    override fun highlight(reader: LineReader, buffer: String): AttributedString {
        return highlight(buffer)
    }

    fun highlight(buffer: String): AttributedString {
        if (buffer.startsWith(":") && !buffer.startsWith("::")) {
            return AttributedStringBuilder().append(buffer).toAttributedString()
        }

        val fragment = CodeFragment(counter.getAndIncrement(), buffer)
        val lineResult = checker.check(state, fragment, true)

        val psi = when (lineResult) {
            is Result.Error -> lineResult.error.psiFile
            is Result.Success -> lineResult.data.psiFile
        }

        val sb = AttributedStringBuilder()
        for (i in buffer.indices) {
            psi.findElementAt(i)?.let { element ->
                val st = when {
                    element.isKeyword() -> style(KotlinElement.KEYWORD)
                    element.isFunction() -> style(KotlinElement.FUNCTION)
                    element.isNumber() -> style(KotlinElement.NUMBER)
                    element.isString() -> style(KotlinElement.STRING)
                    element.isStringTemplate() -> style(KotlinElement.STRING_TEMPLATE)
                    element.isType() -> style(KotlinElement.TYPE)
                    else -> AttributedStyle.DEFAULT
                }
                sb.style(st)
                sb.append(buffer[i])
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
    private fun style(element: KotlinElement): AttributedStyle = styles.getOrDefault(element, AttributedStyle.DEFAULT)

    class CodeFragment(val no: Int, val src: String): Code {
        override fun mkFileName(): String = "Fragment_$no"

        override fun source(): String = src
    }

    enum class KotlinElement {
        KEYWORD,
        FUNCTION,
        VARIABLE,
        PROPERTY,
        TYPE,
        TYPE_PARAMETER,
        NUMBER,
        STRING,
        STRING_TEMPLATE
    }
}
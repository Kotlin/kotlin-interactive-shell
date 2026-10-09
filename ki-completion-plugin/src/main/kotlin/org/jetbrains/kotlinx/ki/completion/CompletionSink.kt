package org.jetbrains.kotlinx.ki.completion

const val COMPLETION_MARKER = "__ki_c0mpl__"
const val TYPE_PROBE = "__ki_type__"

// [type] is the type of a property or the return type of a function, [parameters] is the parameter list of a function
data class CompletionCandidate(val name: String, val kind: Kind, val type: String? = null, val parameters: String? = null) {
    enum class Kind { FUNCTION, PROPERTY, CLASS, PACKAGE }
}

object CompletionSink {
    private var requestId: String? = null
    private var candidates: MutableList<CompletionCandidate>? = null
    private var type: String? = null

    @Synchronized
    fun begin(requestId: String) {
        this.requestId = requestId
        candidates = null
        type = null
    }

    @Synchronized
    fun collected(requestId: String): List<CompletionCandidate>? =
        if (requestId == this.requestId) candidates?.toList() else null

    @Synchronized
    fun type(requestId: String): String? =
        if (requestId == this.requestId) type else null

    @Synchronized
    fun end(requestId: String) {
        if (requestId == this.requestId) begin("")
    }

    @Synchronized
    internal fun addCandidates(items: Collection<CompletionCandidate>) {
        if (requestId.isNullOrEmpty()) return
        (candidates ?: mutableListOf<CompletionCandidate>().also { candidates = it }).addAll(items)
    }

    @Synchronized
    internal fun setType(rendered: String) {
        if (requestId.isNullOrEmpty()) return
        type = rendered
    }

    internal val isActive: Boolean
        @Synchronized get() = !requestId.isNullOrEmpty()
}

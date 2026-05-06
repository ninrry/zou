package luzzr.zou.core.markdown

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MarkdownPreviewTextExtractor @Inject constructor() {

    fun extractPreview(
        markdown: String?,
        maxLength: Int = 120,
    ): String {
        if (markdown.isNullOrBlank()) return ""
        val normalized = markdown
            .replace(Regex("""!\[[^\]]*]\([^)]+\)"""), " ")
            .replace(Regex("""[#>*`_\-\[\]\(\)]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
        return if (normalized.length <= maxLength) {
            normalized
        } else {
            normalized.take(maxLength).trimEnd() + "…"
        }
    }
}

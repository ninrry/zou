package luzzr.zou.core.markdown

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MarkdownImageReferenceParser @Inject constructor() {

    private val imageRegex = Regex("""!\[[^\]]*]\(local://media/([^)]+)\)""")

    fun extractMediaIds(markdown: String?): Set<String> {
        if (markdown.isNullOrBlank()) return emptySet()
        return imageRegex.findAll(markdown)
            .map { it.groupValues[1] }
            .toSet()
    }
}

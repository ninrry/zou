package luzzr.zou.core.markdown

import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownHelpersTest {

    private val previewTextExtractor = MarkdownPreviewTextExtractor()
    private val imageReferenceParser = MarkdownImageReferenceParser()

    @Test
    fun extractsPreviewTextWithoutMarkdownSyntaxAndImages() {
        val preview = previewTextExtractor.extractPreview(
            """
            # Title
            正文 **bold**
            ![image](local://media/media-1)
            """.trimIndent(),
        )

        assertEquals("Title 正文 bold", preview)
    }

    @Test
    fun extractsLocalMediaIdsFromMarkdown() {
        val ids = imageReferenceParser.extractMediaIds(
            """
            ![image](local://media/media-1)
            文本
            ![image](local://media/media-2)
            """.trimIndent(),
        )

        assertEquals(setOf("media-1", "media-2"), ids)
    }
}

package luzzr.zou.core.markdown

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownRendererParsingTest {

    @Test
    fun `parseInlineParts splits text and image in order`() {
        val parts = parseInlineParts("开头 ![image](local://media/a1) 结尾")

        assertEquals(3, parts.size)
        assertTrue(parts[0] is MarkdownInlinePart.Text)
        assertTrue(parts[1] is MarkdownInlinePart.Image)
        assertTrue(parts[2] is MarkdownInlinePart.Text)
        assertEquals("开头 ", (parts[0] as MarkdownInlinePart.Text).value)
        assertEquals("local://media/a1", (parts[1] as MarkdownInlinePart.Image).destination)
        assertEquals(" 结尾", (parts[2] as MarkdownInlinePart.Text).value)
    }

    @Test
    fun `parseInlineParts keeps multiple images`() {
        val parts = parseInlineParts(
            "A ![image](local://media/first) B ![image](local://media/second) C"
        )

        val imageSources = parts.filterIsInstance<MarkdownInlinePart.Image>().map { it.destination }

        assertEquals(listOf("local://media/first", "local://media/second"), imageSources)
    }
}

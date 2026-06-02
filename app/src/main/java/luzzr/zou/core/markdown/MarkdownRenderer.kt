package luzzr.zou.core.markdown

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import java.io.File

@Composable
fun MarkdownRenderer(
    markdown: String,
    mediaLookup: Map<String, String>,
    modifier: Modifier = Modifier,
) {
    val blocks = remember(markdown) { parseMarkdownBlocks(markdown) }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Heading -> MarkdownText(
                    annotated = buildInlineAnnotatedString(block.text),
                    style = when (block.level) {
                        1 -> MaterialTheme.typography.headlineMedium
                        2 -> MaterialTheme.typography.headlineSmall
                        3 -> MaterialTheme.typography.titleLarge
                        else -> MaterialTheme.typography.titleMedium
                    },
                )

                is MarkdownBlock.Paragraph -> MarkdownFlowContent(
                    source = block.text,
                    mediaLookup = mediaLookup,
                    textStyle = MaterialTheme.typography.bodyLarge,
                )

                is MarkdownBlock.BlockQuote -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .background(
                                color = MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(999.dp),
                            ),
                    )
                    MarkdownFlowContent(
                        modifier = Modifier.weight(1f),
                        source = block.text,
                        mediaLookup = mediaLookup,
                        textStyle = MaterialTheme.typography.bodyLarge,
                    )
                }

                is MarkdownBlock.BulletList -> MarkdownList(
                    items = block.items,
                    ordered = false,
                    mediaLookup = mediaLookup,
                )

                is MarkdownBlock.OrderedList -> MarkdownList(
                    items = block.items,
                    ordered = true,
                    mediaLookup = mediaLookup,
                )

                is MarkdownBlock.CodeBlock -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(16.dp),
                        )
                        .padding(14.dp),
                ) {
                    Text(
                        text = block.code,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = FontFamily.Monospace,
                    )
                }

                is MarkdownBlock.Image -> MarkdownImage(
                    destination = block.destination,
                    mediaLookup = mediaLookup,
                )

                MarkdownBlock.Divider -> HorizontalDivider()
            }
        }
    }
}

@Composable
private fun MarkdownText(
    annotated: AnnotatedString,
    style: TextStyle,
) {
    val uriHandler = LocalUriHandler.current
    val hasLink = annotated.getStringAnnotations(
        tag = linkTag,
        start = 0,
        end = annotated.length,
    ).isNotEmpty()

    if (hasLink) {
        var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
        Text(
            text = annotated,
            style = style.copy(color = MaterialTheme.colorScheme.onSurface),
            modifier = Modifier.pointerInput(annotated) {
                detectTapGestures { position ->
                    val layoutResult = textLayoutResult ?: return@detectTapGestures
                    val offset = layoutResult.getOffsetForPosition(position)
                    annotated.getStringAnnotations(linkTag, offset, offset)
                        .firstOrNull()
                        ?.let { uriHandler.openUri(it.item) }
                }
            },
            onTextLayout = { textLayoutResult = it },
        )
    } else {
        Text(
            text = annotated,
            style = style,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun MarkdownList(
    items: List<String>,
    ordered: Boolean,
    mediaLookup: Map<String, String>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEachIndexed { index, item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = if (ordered) "${index + 1}." else "\u2022",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                MarkdownFlowContent(
                    modifier = Modifier.weight(1f),
                    source = item,
                    mediaLookup = mediaLookup,
                    textStyle = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@Composable
private fun MarkdownFlowContent(
    source: String,
    mediaLookup: Map<String, String>,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
) {
    val inlineParts = remember(source) { parseInlineParts(source) }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        inlineParts.forEach { part ->
            when (part) {
                is MarkdownInlinePart.Text -> {
                    if (part.value.isNotBlank()) {
                        MarkdownText(
                            annotated = buildInlineAnnotatedString(part.value),
                            style = textStyle,
                        )
                    }
                }

                is MarkdownInlinePart.Image -> MarkdownImage(
                    destination = part.destination,
                    mediaLookup = mediaLookup,
                )
            }
        }
    }
}

@Composable
private fun MarkdownImage(
    destination: String,
    mediaLookup: Map<String, String>,
) {
    val isLocalMedia = destination.startsWith(localMediaPrefix)
    val mediaId = destination.removePrefix(localMediaPrefix)
    val localPath = mediaLookup[mediaId]
    val imageFile = remember(localPath) { localPath?.let(::File) }
    val context = LocalContext.current
    var loadState by remember(localPath) { mutableStateOf(MarkdownImageLoadState.Idle) }

    if (isLocalMedia && imageFile?.exists() == true && loadState != MarkdownImageLoadState.Error) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageFile)
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.FillWidth,
                onLoading = { loadState = MarkdownImageLoadState.Loading },
                onSuccess = { loadState = MarkdownImageLoadState.Success },
                onError = { loadState = MarkdownImageLoadState.Error },
            )
            if (loadState == MarkdownImageLoadState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(vertical = 24.dp)
                        .size(28.dp),
                    strokeWidth = 2.5.dp,
                )
            }
        }
    } else {
        MarkdownImageFallback()
    }
}

@Composable
private fun MarkdownImageFallback() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp),
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(Color(0xFFFFB74D), RoundedCornerShape(999.dp)),
        )
        Text(
            text = "\u56fe\u7247\u4e0d\u53ef\u7528",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun parseMarkdownBlocks(markdown: String): List<MarkdownBlock> {
    if (markdown.isBlank()) return emptyList()
    val lines = markdown.lines()
    val blocks = mutableListOf<MarkdownBlock>()
    var index = 0

    while (index < lines.size) {
        val line = lines[index]
        val trimmed = line.trim()

        when {
            trimmed.isBlank() -> index += 1
            trimmed == "---" || trimmed == "***" -> {
                blocks += MarkdownBlock.Divider
                index += 1
            }

            trimmed.startsWith("```") -> {
                val codeLines = mutableListOf<String>()
                index += 1
                while (index < lines.size && !lines[index].trim().startsWith("```")) {
                    codeLines += lines[index]
                    index += 1
                }
                if (index < lines.size) index += 1
                blocks += MarkdownBlock.CodeBlock(codeLines.joinToString("\n"))
            }

            headingRegex.matches(trimmed) -> {
                val match = headingRegex.find(trimmed)!!
                blocks += MarkdownBlock.Heading(
                    level = match.groupValues[1].length,
                    text = match.groupValues[2],
                )
                index += 1
            }

            imageRegex.matches(trimmed) -> {
                val match = imageRegex.find(trimmed)!!
                blocks += MarkdownBlock.Image(match.groupValues[1])
                index += 1
            }

            trimmed.startsWith("> ") -> {
                val quoteLines = mutableListOf<String>()
                while (index < lines.size && lines[index].trim().startsWith("> ")) {
                    quoteLines += lines[index].trim().removePrefix("> ")
                    index += 1
                }
                blocks += MarkdownBlock.BlockQuote(quoteLines.joinToString("\n"))
            }

            bulletRegex.matches(trimmed) -> {
                val items = mutableListOf<String>()
                while (index < lines.size && bulletRegex.matches(lines[index].trim())) {
                    items += lines[index].trim().removeRange(0, 2)
                    index += 1
                }
                blocks += MarkdownBlock.BulletList(items)
            }

            orderedRegex.matches(trimmed) -> {
                val items = mutableListOf<String>()
                while (index < lines.size && orderedRegex.matches(lines[index].trim())) {
                    items += lines[index].trim().replaceFirst(orderedRegex, "")
                    index += 1
                }
                blocks += MarkdownBlock.OrderedList(items)
            }

            else -> {
                val paragraphLines = mutableListOf<String>()
                while (
                    index < lines.size &&
                    lines[index].trim().isNotBlank() &&
                    !startsNewBlock(lines[index].trim())
                ) {
                    paragraphLines += lines[index].trim()
                    index += 1
                }
                blocks += MarkdownBlock.Paragraph(paragraphLines.joinToString("\n"))
            }
        }
    }
    return blocks
}

private fun startsNewBlock(trimmed: String): Boolean {
    return trimmed == "---" ||
        trimmed == "***" ||
        trimmed.startsWith("```") ||
        headingRegex.matches(trimmed) ||
        imageRegex.matches(trimmed) ||
        trimmed.startsWith("> ") ||
        bulletRegex.matches(trimmed) ||
        orderedRegex.matches(trimmed)
}

internal fun parseInlineParts(source: String): List<MarkdownInlinePart> {
    if (source.isBlank()) return listOf(MarkdownInlinePart.Text(source))
    val parts = mutableListOf<MarkdownInlinePart>()
    var cursor = 0

    inlineImageRegex.findAll(source).forEach { match ->
        val start = match.range.first
        val endExclusive = match.range.last + 1
        if (cursor < start) {
            parts += MarkdownInlinePart.Text(source.substring(cursor, start))
        }
        parts += MarkdownInlinePart.Image(match.groupValues[1])
        cursor = endExclusive
    }

    if (cursor < source.length) {
        parts += MarkdownInlinePart.Text(source.substring(cursor))
    }

    return if (parts.isEmpty()) {
        listOf(MarkdownInlinePart.Text(source))
    } else {
        parts
    }
}

private fun buildInlineAnnotatedString(text: String): AnnotatedString {
    return buildAnnotatedString {
        appendInlineSegment(
            source = text,
            builder = this,
        )
    }
}

private fun appendInlineSegment(
    source: String,
    builder: AnnotatedString.Builder,
) {
    var index = 0
    while (index < source.length) {
        when {
            source.startsWith("**", index) -> {
                val end = source.indexOf("**", startIndex = index + 2)
                if (end != -1) {
                    builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    appendInlineSegment(source.substring(index + 2, end), builder)
                    builder.pop()
                    index = end + 2
                } else {
                    builder.append(source[index])
                    index += 1
                }
            }

            source.startsWith("*", index) -> {
                val end = source.indexOf('*', startIndex = index + 1)
                if (end != -1) {
                    builder.pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    appendInlineSegment(source.substring(index + 1, end), builder)
                    builder.pop()
                    index = end + 1
                } else {
                    builder.append(source[index])
                    index += 1
                }
            }

            source.startsWith("`", index) -> {
                val end = source.indexOf('`', startIndex = index + 1)
                if (end != -1) {
                    builder.pushStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = Color(0x1A000000),
                        ),
                    )
                    builder.append(source.substring(index + 1, end))
                    builder.pop()
                    index = end + 1
                } else {
                    builder.append(source[index])
                    index += 1
                }
            }

            source.startsWith("[", index) -> {
                val closingText = source.indexOf("](", startIndex = index + 1)
                val closingUrl = if (closingText != -1) {
                    source.indexOf(')', startIndex = closingText + 2)
                } else {
                    -1
                }
                if (closingText != -1 && closingUrl != -1) {
                    val textPart = source.substring(index + 1, closingText)
                    val urlPart = source.substring(closingText + 2, closingUrl)
                    builder.pushStringAnnotation(linkTag, urlPart)
                    builder.pushStyle(
                        SpanStyle(
                            color = Color(0xFF5A6FD8),
                            textDecoration = TextDecoration.Underline,
                        ),
                    )
                    appendInlineSegment(textPart, builder)
                    builder.pop()
                    builder.pop()
                    index = closingUrl + 1
                } else {
                    builder.append(source[index])
                    index += 1
                }
            }

            else -> {
                builder.append(source[index])
                index += 1
            }
        }
    }
}

internal sealed interface MarkdownInlinePart {
    data class Text(val value: String) : MarkdownInlinePart
    data class Image(val destination: String) : MarkdownInlinePart
}

private sealed interface MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock
    data class Paragraph(val text: String) : MarkdownBlock
    data class BlockQuote(val text: String) : MarkdownBlock
    data class BulletList(val items: List<String>) : MarkdownBlock
    data class OrderedList(val items: List<String>) : MarkdownBlock
    data class CodeBlock(val code: String) : MarkdownBlock
    data class Image(val destination: String) : MarkdownBlock
    data object Divider : MarkdownBlock
}

private enum class MarkdownImageLoadState {
    Idle,
    Loading,
    Success,
    Error,
}

private val headingRegex = Regex("""^(#{1,6})\s+(.*)$""")
private val imageRegex = Regex("""^!\[[^\]]*]\(([^)]+)\)$""")
private val inlineImageRegex = Regex("""!\[[^\]]*]\(([^)]+)\)""")
private val bulletRegex = Regex("""^[-*+]\s+.*$""")
private val orderedRegex = Regex("""^\d+\.\s+""")

private const val linkTag = "link"
private const val localMediaPrefix = "local://media/"

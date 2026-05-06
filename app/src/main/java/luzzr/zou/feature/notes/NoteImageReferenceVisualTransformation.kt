package luzzr.zou.feature.notes

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

object NoteImageReferenceVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val source = text.text
        val matches = imageReferenceRegex.findAll(source).toList()
        if (matches.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val transformed = StringBuilder()
        val originalToTransformed = IntArray(source.length + 1)
        val transformedToOriginal = mutableListOf<Int>()
        var sourceIndex = 0
        var transformedIndex = 0
        var imageIndex = 1

        matches.forEach { match ->
            while (sourceIndex < match.range.first) {
                originalToTransformed[sourceIndex] = transformedIndex
                transformed.append(source[sourceIndex])
                transformedToOriginal += sourceIndex
                sourceIndex += 1
                transformedIndex += 1
            }

            val placeholder = "\u3014\u56fe\u7247$imageIndex\u3015"
            repeat(match.value.length) { offset ->
                originalToTransformed[match.range.first + offset] = transformedIndex
            }
            placeholder.forEach {
                transformed.append(it)
                transformedToOriginal += match.range.first
                transformedIndex += 1
            }
            sourceIndex = match.range.last + 1
            imageIndex += 1
        }

        while (sourceIndex < source.length) {
            originalToTransformed[sourceIndex] = transformedIndex
            transformed.append(source[sourceIndex])
            transformedToOriginal += sourceIndex
            sourceIndex += 1
            transformedIndex += 1
        }

        originalToTransformed[source.length] = transformedIndex
        transformedToOriginal += source.length

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                return originalToTransformed[offset.coerceIn(0, originalToTransformed.lastIndex)]
            }

            override fun transformedToOriginal(offset: Int): Int {
                return transformedToOriginal[offset.coerceIn(0, transformedToOriginal.lastIndex)]
            }
        }

        return TransformedText(
            text = AnnotatedString(transformed.toString()),
            offsetMapping = offsetMapping,
        )
    }

    private val imageReferenceRegex = Regex("""!\[[^\]]*]\(local://media/([^)]+)\)""")
}

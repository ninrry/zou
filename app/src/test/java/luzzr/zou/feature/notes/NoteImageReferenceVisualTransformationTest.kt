package luzzr.zou.feature.notes

import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.assertEquals
import org.junit.Test

class NoteImageReferenceVisualTransformationTest {

    @Test
    fun `image markdown is shown as readable placeholder`() {
        val transformed = NoteImageReferenceVisualTransformation.filter(
            AnnotatedString("\u4f60\u597d ![image](local://media/abc) \u4e16\u754c"),
        )

        assertEquals("\u4f60\u597d \u3014\u56fe\u72471\u3015 \u4e16\u754c", transformed.text.text)
    }
}

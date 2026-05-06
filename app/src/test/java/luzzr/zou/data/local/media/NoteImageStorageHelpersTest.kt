package luzzr.zou.data.local.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NoteImageStorageHelpersTest {

    @Test
    fun `calculateSafeSampleSize reduces very large images before full decode`() {
        assertEquals(2, calculateSafeSampleSize(width = 5000, height = 3000, maxDimension = 2048))
        assertEquals(4, calculateSafeSampleSize(width = 10000, height = 10000, maxDimension = 2048))
        assertEquals(1, calculateSafeSampleSize(width = 2048, height = 1024, maxDimension = 2048))
    }

    @Test
    fun `calculateScaledDimensions clamps the longest edge to the max size`() {
        assertEquals(2048 to 1024, calculateScaledDimensions(width = 4000, height = 2000, maxDimension = 2048))
        assertEquals(1024 to 2048, calculateScaledDimensions(width = 2000, height = 4000, maxDimension = 2048))
        assertEquals(1200 to 900, calculateScaledDimensions(width = 1200, height = 900, maxDimension = 2048))
    }

    @Test
    fun `resolveNormalizedImageOutput keeps alpha images as png`() {
        val output = resolveNormalizedImageOutput(hasAlpha = true)

        assertEquals("png", output.extension)
        assertEquals("image/png", output.mimeType)
        assertNull(output.quality)
    }

    @Test
    fun `resolveNormalizedImageOutput stores opaque images as jpeg`() {
        val output = resolveNormalizedImageOutput(hasAlpha = false)

        assertEquals("jpg", output.extension)
        assertEquals("image/jpeg", output.mimeType)
        assertEquals(85, output.quality)
    }
}

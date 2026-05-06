package luzzr.zou.data.local.media

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocalNoteImageStorageTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var storage: LocalNoteImageStorage
    private lateinit var sourceRoot: File

    @Before
    fun setUp() {
        storage = LocalNoteImageStorage(context)
        sourceRoot = File(context.cacheDir, "local-note-image-storage-test").apply {
            deleteRecursively()
            mkdirs()
        }
    }

    @After
    fun tearDown() {
        sourceRoot.deleteRecursively()
        File(context.filesDir, "media/notes").deleteRecursively()
    }

    @Test
    fun importImageScalesLargeOpaqueBitmapsAndStoresJpeg() = runBlocking {
        val source = createBitmapFile(
            name = "large-source.jpg",
            width = 3000,
            height = 1500,
            sourceFormat = Bitmap.CompressFormat.JPEG,
            hasTransparency = false,
        )

        val stored = storage.importImage("note-large", Uri.fromFile(source).toString())
        val decoded = BitmapFactory.decodeFile(stored.localPath)

        assertEquals("image/jpeg", stored.mimeType)
        assertTrue(stored.localPath.endsWith(".jpg"))
        assertNotNull(decoded)
        assertTrue(maxOf(decoded!!.width, decoded.height) <= 2048)
        decoded.recycle()
    }

    @Test
    fun importImagePreservesAlphaByReencodingAsPng() = runBlocking {
        val source = createBitmapFile(
            name = "alpha-source.png",
            width = 400,
            height = 200,
            sourceFormat = Bitmap.CompressFormat.PNG,
            hasTransparency = true,
        )

        val stored = storage.importImage("note-alpha", Uri.fromFile(source).toString())
        val decoded = BitmapFactory.decodeFile(stored.localPath)

        assertEquals("image/png", stored.mimeType)
        assertTrue(stored.localPath.endsWith(".png"))
        assertNotNull(decoded)
        assertTrue(decoded!!.hasAlpha())
        decoded.recycle()
    }

    @Test
    fun importImageAppliesExifOrientationBeforeSaving() = runBlocking {
        val source = createBitmapFile(
            name = "rotated-source.jpg",
            width = 320,
            height = 120,
            sourceFormat = Bitmap.CompressFormat.JPEG,
            hasTransparency = false,
        ).also { file ->
            ExifInterface(file.absolutePath).apply {
                setAttribute(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_ROTATE_90.toString(),
                )
                saveAttributes()
            }
        }

        val stored = storage.importImage("note-rotated", Uri.fromFile(source).toString())
        val decoded = BitmapFactory.decodeFile(stored.localPath)

        assertNotNull(decoded)
        assertEquals(120, decoded!!.width)
        assertEquals(320, decoded.height)
        decoded.recycle()
    }

    @Test
    fun importImageRejectsInvalidFilesWithoutLeavingArtifacts() = runBlocking {
        val source = File(sourceRoot, "invalid.jpg").apply {
            writeText("not an image")
        }
        val noteDirectory = File(context.filesDir, "media/notes/note-invalid")

        try {
            storage.importImage("note-invalid", Uri.fromFile(source).toString())
            fail("Expected invalid image import to throw")
        } catch (expected: IllegalArgumentException) {
            assertEquals("\u65e0\u6cd5\u8bfb\u53d6\u56fe\u7247\uff0c\u8bf7\u9009\u62e9\u6709\u6548\u56fe\u7247\u540e\u91cd\u8bd5\u3002", expected.message)
        }

        assertTrue(!noteDirectory.exists() || noteDirectory.listFiles().isNullOrEmpty())
    }

    private fun createBitmapFile(
        name: String,
        width: Int,
        height: Int,
        sourceFormat: Bitmap.CompressFormat,
        hasTransparency: Boolean,
    ): File {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(if (hasTransparency) 0x44FFAA00 else 0xFFFFAA00.toInt())
        if (hasTransparency) {
            bitmap.setPixel(0, 0, 0x00000000)
        }

        return File(sourceRoot, name).also { file ->
            file.outputStream().use { output ->
                bitmap.compress(sourceFormat, 100, output)
            }
            bitmap.recycle()
        }
    }
}

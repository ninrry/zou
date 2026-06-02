package luzzr.zou.data.local.media

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class StoredNoteImage(
    val mediaId: String,
    val localPath: String,
    val mimeType: String,
    val sizeBytes: Long,
)

interface NoteImageStorage {
    suspend fun importImage(
        noteId: String,
        sourceUri: String,
    ): StoredNoteImage

    fun deleteImage(localPath: String)
}

@Singleton
class LocalNoteImageStorage @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : NoteImageStorage {

    override suspend fun importImage(
        noteId: String,
        sourceUri: String,
    ): StoredNoteImage = withContext(Dispatchers.IO) {
        runCatching {
            importNormalizedImage(
                noteId = noteId,
                uri = Uri.parse(sourceUri),
                mediaId = UUID.randomUUID().toString(),
            )
        }.getOrElse { throwable ->
            when (throwable) {
                is IllegalArgumentException -> throw throwable
                else -> throw IllegalStateException(importFailureMessage, throwable)
            }
        }
    }

    override fun deleteImage(localPath: String) {
        runCatching {
            File(localPath).takeIf { it.exists() }?.delete()
        }
    }

    private fun importNormalizedImage(
        noteId: String,
        uri: Uri,
        mediaId: String,
    ): StoredNoteImage {
        val noteDirectory = File(context.filesDir, "media/notes/$noteId")
        require(noteDirectory.exists() || noteDirectory.mkdirs()) { saveFailureMessage }

        val bounds = readImageBounds(
            contentResolver = context.contentResolver,
            uri = uri,
        )
        val orientation = readOrientation(
            contentResolver = context.contentResolver,
            uri = uri,
        )
        val sampleSize = calculateSafeSampleSize(
            width = bounds.width,
            height = bounds.height,
            maxDimension = maxImportedImageDimensionPx,
        )
        val sampledBitmap = decodeBitmap(
            contentResolver = context.contentResolver,
            uri = uri,
            sampleSize = sampleSize,
        ) ?: throw IllegalArgumentException(invalidImageMessage)

        var transformedBitmap: Bitmap = sampledBitmap
        var normalizedBitmap: Bitmap = sampledBitmap
        try {
            transformedBitmap = applyOrientationIfNeeded(
                bitmap = sampledBitmap,
                orientation = orientation,
            )
            normalizedBitmap = scaleBitmapIfNeeded(
                bitmap = transformedBitmap,
                maxDimension = maxImportedImageDimensionPx,
            )
            val output = resolveNormalizedImageOutput(normalizedBitmap.hasAlpha())
            val targetFile = File(noteDirectory, "$mediaId.${output.extension}")
            val tempFile = File(noteDirectory, "$mediaId.${output.extension}.tmp")

            writeBitmapToFile(
                bitmap = normalizedBitmap,
                output = output,
                tempFile = tempFile,
                targetFile = targetFile,
            )

            return StoredNoteImage(
                mediaId = mediaId,
                localPath = targetFile.absolutePath,
                mimeType = output.mimeType,
                sizeBytes = targetFile.length(),
            )
        } finally {
            if (normalizedBitmap !== transformedBitmap && !normalizedBitmap.isRecycled) {
                normalizedBitmap.recycle()
            }
            if (transformedBitmap !== sampledBitmap && !transformedBitmap.isRecycled) {
                transformedBitmap.recycle()
            }
            if (!sampledBitmap.isRecycled) {
                sampledBitmap.recycle()
            }
        }
    }

    private fun readImageBounds(
        contentResolver: ContentResolver,
        uri: Uri,
    ): ImageBounds {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { invalidImageMessage }
            BitmapFactory.decodeStream(input, null, options)
        }
        if (options.outWidth <= 0 || options.outHeight <= 0) {
            throw IllegalArgumentException(invalidImageMessage)
        }
        return ImageBounds(
            width = options.outWidth,
            height = options.outHeight,
        )
    }

    private fun decodeBitmap(
        contentResolver: ContentResolver,
        uri: Uri,
        sampleSize: Int,
    ): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { invalidImageMessage }
            BitmapFactory.decodeStream(input, null, options)
        }
    }

    private fun readOrientation(
        contentResolver: ContentResolver,
        uri: Uri,
    ): Int {
        return runCatching {
            contentResolver.openInputStream(uri).use { input ->
                if (input == null) {
                    ExifInterface.ORIENTATION_UNDEFINED
                } else {
                    ExifInterface(input).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_UNDEFINED,
                    )
                }
            }
        }.getOrDefault(ExifInterface.ORIENTATION_UNDEFINED)
    }

    private fun applyOrientationIfNeeded(
        bitmap: Bitmap,
        orientation: Int,
    ): Bitmap {
        val matrix = Matrix().applyOrientation(orientation)
        if (matrix.isIdentity) {
            return bitmap
        }
        return Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true,
        )
    }

    private fun scaleBitmapIfNeeded(
        bitmap: Bitmap,
        maxDimension: Int,
    ): Bitmap {
        val (targetWidth, targetHeight) = calculateScaledDimensions(
            width = bitmap.width,
            height = bitmap.height,
            maxDimension = maxDimension,
        )
        if (bitmap.width == targetWidth && bitmap.height == targetHeight) {
            return bitmap
        }
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }

    private fun writeBitmapToFile(
        bitmap: Bitmap,
        output: NormalizedImageOutput,
        tempFile: File,
        targetFile: File,
    ) {
        runCatching {
            tempFile.outputStream().use { stream ->
                val compressSucceeded = bitmap.compress(
                    output.format.toBitmapCompressFormat(),
                    output.quality ?: pngQuality,
                    stream,
                )
                require(compressSucceeded) { saveFailureMessage }
            }

            if (targetFile.exists() && !targetFile.delete()) {
                throw IllegalStateException(saveFailureMessage)
            }
            if (!tempFile.renameTo(targetFile)) {
                throw IllegalStateException(saveFailureMessage)
            }
        }.getOrElse { throwable ->
            tempFile.delete()
            targetFile.delete()
            throw throwable
        }
    }

    private data class ImageBounds(
        val width: Int,
        val height: Int,
    )

    private companion object {
        const val maxImportedImageDimensionPx = 2048
        const val pngQuality = 100
        const val invalidImageMessage = "\u65e0\u6cd5\u8bfb\u53d6\u56fe\u7247\uff0c\u8bf7\u9009\u62e9\u6709\u6548\u56fe\u7247\u540e\u91cd\u8bd5\u3002"
        const val saveFailureMessage = "\u4fdd\u5b58\u56fe\u7247\u5931\u8d25\uff0c\u8bf7\u91cd\u8bd5\u3002"
        const val importFailureMessage = "\u63d2\u5165\u56fe\u7247\u5931\u8d25\uff0c\u8bf7\u91cd\u65b0\u9009\u62e9\u56fe\u7247\u540e\u91cd\u8bd5\u3002"
    }
}

internal data class NormalizedImageOutput(
    val extension: String,
    val mimeType: String,
    val quality: Int?,
    val format: NormalizedImageEncodeFormat,
)

internal enum class NormalizedImageEncodeFormat {
    Png,
    Jpeg,
}

internal fun calculateSafeSampleSize(
    width: Int,
    height: Int,
    maxDimension: Int,
): Int {
    require(width > 0) { "width must be > 0" }
    require(height > 0) { "height must be > 0" }
    require(maxDimension > 0) { "maxDimension must be > 0" }

    var sampleSize = 1
    while (width / sampleSize > maxDimension * 2 || height / sampleSize > maxDimension * 2) {
        sampleSize *= 2
    }
    return sampleSize
}

internal fun calculateScaledDimensions(
    width: Int,
    height: Int,
    maxDimension: Int,
): Pair<Int, Int> {
    require(width > 0) { "width must be > 0" }
    require(height > 0) { "height must be > 0" }
    require(maxDimension > 0) { "maxDimension must be > 0" }

    val largestSide = max(width, height)
    if (largestSide <= maxDimension) {
        return width to height
    }

    val scale = maxDimension.toFloat() / largestSide.toFloat()
    val scaledWidth = max(1, (width * scale).roundToInt())
    val scaledHeight = max(1, (height * scale).roundToInt())
    return scaledWidth to scaledHeight
}

internal fun resolveNormalizedImageOutput(hasAlpha: Boolean): NormalizedImageOutput {
    return if (hasAlpha) {
        NormalizedImageOutput(
            extension = "png",
            mimeType = "image/png",
            quality = null,
            format = NormalizedImageEncodeFormat.Png,
        )
    } else {
        NormalizedImageOutput(
            extension = "jpg",
            mimeType = "image/jpeg",
            quality = jpegQuality,
            format = NormalizedImageEncodeFormat.Jpeg,
        )
    }
}

private const val jpegQuality = 85

private fun NormalizedImageEncodeFormat.toBitmapCompressFormat(): Bitmap.CompressFormat {
    return when (this) {
        NormalizedImageEncodeFormat.Png -> Bitmap.CompressFormat.PNG
        NormalizedImageEncodeFormat.Jpeg -> Bitmap.CompressFormat.JPEG
    }
}

private fun Matrix.applyOrientation(orientation: Int): Matrix {
    when (orientation) {
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
        ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> {
            postRotate(90f)
            postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
        ExifInterface.ORIENTATION_TRANSVERSE -> {
            postRotate(-90f)
            postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
        else -> Unit
    }
    return this
}

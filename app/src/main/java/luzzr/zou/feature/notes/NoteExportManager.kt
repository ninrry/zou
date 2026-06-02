package luzzr.zou.feature.notes

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import luzzr.zou.core.time.TimeProvider
import luzzr.zou.domain.repository.NoteRepository

data class NoteExportShareRequest(
    val uri: Uri,
    val mimeType: String,
    val subject: String,
)

class NoteExportManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val noteRepository: NoteRepository,
    private val timeProvider: TimeProvider,
) {

    suspend fun export(noteIds: List<String>): Result<NoteExportShareRequest> = withContext(Dispatchers.IO) {
        runCatching {
            require(noteIds.isNotEmpty()) { "No notes selected for export." }
            buildShareRequest(noteIds)
        }
    }

    private suspend fun buildShareRequest(noteIds: List<String>): NoteExportShareRequest {
        val noteDetails = noteIds.mapNotNull { noteRepository.getNote(it) }
        require(noteDetails.isNotEmpty()) { "Selected notes are no longer available." }

        val exportDir = prepareExportDirectory()
        val hasImages = noteDetails.any { it.images.isNotEmpty() }
        val isSingle = noteDetails.size == 1

        return if (isSingle && !hasImages) {
            val detail = noteDetails.first()
            val title = detail.note.title.ifBlank { "未命名笔记" }
            val fileName = "${sanitizeFileName(title)}_${timeProvider.nowMillis()}.md"
            val tempFile = File(exportDir, fileName)
            tempFile.writeText(detail.note.contentMarkdown.orEmpty(), Charsets.UTF_8)

            NoteExportShareRequest(
                uri = tempFile.toContentUri(),
                mimeType = "text/markdown",
                subject = title,
            )
        } else {
            val zipName = "Zou_Notes_Export_${timeProvider.nowMillis()}.zip"
            val tempZipFile = File(exportDir, zipName)
            val usedEntryNames = mutableSetOf<String>()

            ZipOutputStream(FileOutputStream(tempZipFile)).use { zos ->
                noteDetails.forEach { detail ->
                    val noteTitle = sanitizeFileName(detail.note.title.ifBlank { "未命名笔记" })
                    val mdFileName = uniqueEntryName(
                        preferred = if (isSingle) "index.md" else "$noteTitle.md",
                        usedEntryNames = usedEntryNames,
                    )

                    var content = detail.note.contentMarkdown.orEmpty()
                    detail.images.forEach { image ->
                        val extension = image.mimeType.toExportExtension()
                        val localPlaceholder = "local://media/${image.mediaId}"
                        val relativePath = uniqueEntryName(
                            preferred = "images/${image.mediaId}.$extension",
                            usedEntryNames = usedEntryNames,
                        )
                        content = content.replace(localPlaceholder, relativePath)

                        val imageFile = File(image.localPath)
                        if (imageFile.exists()) {
                            zos.putNextEntry(ZipEntry(relativePath))
                            imageFile.inputStream().use { it.copyTo(zos) }
                            zos.closeEntry()
                        }
                    }

                    zos.putNextEntry(ZipEntry(mdFileName))
                    zos.write(content.toByteArray(Charsets.UTF_8))
                    zos.closeEntry()
                }
            }

            NoteExportShareRequest(
                uri = tempZipFile.toContentUri(),
                mimeType = "application/zip",
                subject = "Zou 笔记批量导出",
            )
        }
    }

    private fun prepareExportDirectory(): File {
        return File(context.cacheDir, EXPORT_CACHE_DIR).apply {
            mkdirs()
            val cutoff = timeProvider.nowMillis() - EXPORT_CACHE_TTL_MILLIS
            listFiles()
                ?.filter { file -> file.lastModified() < cutoff }
                ?.forEach { file -> file.delete() }
        }
    }

    private fun File.toContentUri(): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            this,
        )
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[\\\\/:*?\"<>|\\r\\n]"), "_").ifBlank { "未命名笔记" }
    }

    private fun uniqueEntryName(preferred: String, usedEntryNames: MutableSet<String>): String {
        if (usedEntryNames.add(preferred)) return preferred

        val dotIndex = preferred.lastIndexOf('.')
        val base = if (dotIndex > 0) preferred.substring(0, dotIndex) else preferred
        val extension = if (dotIndex > 0) preferred.substring(dotIndex) else ""
        var index = 2
        while (true) {
            val candidate = "$base-$index$extension"
            if (usedEntryNames.add(candidate)) return candidate
            index += 1
        }
    }

    private fun String.toExportExtension(): String {
        return when (this) {
            "image/png" -> "png"
            "image/gif" -> "gif"
            "image/webp" -> "webp"
            else -> "jpg"
        }
    }

    private companion object {
        const val EXPORT_CACHE_DIR = "note_exports"
        const val EXPORT_CACHE_TTL_MILLIS = 24L * 60L * 60L * 1_000L
    }
}

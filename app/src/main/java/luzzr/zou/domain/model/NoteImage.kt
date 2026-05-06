package luzzr.zou.domain.model

data class NoteImage(
    val mediaId: String,
    val ownerId: String,
    val localPath: String,
    val mimeType: String,
    val sizeBytes: Long,
    val isDeleted: Boolean = false,
)

package luzzr.zou.domain.model

data class InsertedNoteImage(
    val mediaId: String,
    val markdownReference: String,
    val localPath: String,
    val mimeType: String,
)

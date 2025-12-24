package com.famy.tree.domain.model

data class Media(
    val id: Long = 0,
    val memberId: Long,
    val filePath: String,
    val type: MediaKind,
    val title: String? = null,
    val description: String? = null,
    val dateTaken: Long? = null,
    val fileSize: Long = 0,
    val mimeType: String? = null,
    val thumbnailPath: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val formattedSize: String
        get() {
            return when {
                fileSize < 1024 -> "$fileSize B"
                fileSize < 1024 * 1024 -> "${fileSize / 1024} KB"
                fileSize < 1024 * 1024 * 1024 -> "${fileSize / (1024 * 1024)} MB"
                else -> "${fileSize / (1024 * 1024 * 1024)} GB"
            }
        }
}

enum class MediaKind {
    PHOTO,
    DOCUMENT,
    VIDEO,
    AUDIO,
    OTHER;

    val displayName: String
        get() = when (this) {
            PHOTO -> "Photo"
            DOCUMENT -> "Document"
            VIDEO -> "Video"
            AUDIO -> "Audio"
            OTHER -> "File"
        }

    companion object {
        fun fromString(value: String): MediaKind {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: OTHER
        }

        fun fromMimeType(mimeType: String?): MediaKind = when {
            mimeType == null -> OTHER
            mimeType.startsWith("image/") -> PHOTO
            mimeType.startsWith("video/") -> VIDEO
            mimeType.startsWith("audio/") -> AUDIO
            mimeType == "application/pdf" -> DOCUMENT
            mimeType.startsWith("text/") -> DOCUMENT
            else -> OTHER
        }
    }
}

data class MediaWithMember(
    val media: Media,
    val member: FamilyMember
)

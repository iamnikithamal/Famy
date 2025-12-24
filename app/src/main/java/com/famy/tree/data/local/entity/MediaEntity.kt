package com.famy.tree.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "media",
    foreignKeys = [
        ForeignKey(
            entity = FamilyMemberEntity::class,
            parentColumns = ["id"],
            childColumns = ["member_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["member_id"]),
        Index(value = ["media_type"]),
        Index(value = ["created_at"]),
        Index(value = ["member_id", "media_type"])
    ]
)
data class MediaEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "member_id")
    val memberId: Long,

    @ColumnInfo(name = "file_path")
    val filePath: String,

    @ColumnInfo(name = "media_type")
    val mediaType: String,

    @ColumnInfo(name = "title")
    val title: String? = null,

    @ColumnInfo(name = "description")
    val description: String? = null,

    @ColumnInfo(name = "date_taken")
    val dateTaken: Long? = null,

    @ColumnInfo(name = "file_size")
    val fileSize: Long = 0,

    @ColumnInfo(name = "mime_type")
    val mimeType: String? = null,

    @ColumnInfo(name = "thumbnail_path")
    val thumbnailPath: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

object MediaType {
    const val PHOTO = "PHOTO"
    const val DOCUMENT = "DOCUMENT"
    const val VIDEO = "VIDEO"
    const val AUDIO = "AUDIO"
    const val OTHER = "OTHER"

    fun fromMimeType(mimeType: String?): String = when {
        mimeType == null -> OTHER
        mimeType.startsWith("image/") -> PHOTO
        mimeType.startsWith("video/") -> VIDEO
        mimeType.startsWith("audio/") -> AUDIO
        mimeType == "application/pdf" -> DOCUMENT
        mimeType.startsWith("text/") -> DOCUMENT
        else -> OTHER
    }
}

package com.famy.tree.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "life_events",
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
        Index(value = ["event_type"]),
        Index(value = ["event_date"]),
        Index(value = ["member_id", "event_date"])
    ]
)
data class LifeEventEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "member_id")
    val memberId: Long,

    @ColumnInfo(name = "event_type")
    val eventType: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "description")
    val description: String? = null,

    @ColumnInfo(name = "event_date")
    val eventDate: Long? = null,

    @ColumnInfo(name = "event_place")
    val eventPlace: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

object LifeEventType {
    const val BIRTH = "BIRTH"
    const val DEATH = "DEATH"
    const val MARRIAGE = "MARRIAGE"
    const val DIVORCE = "DIVORCE"
    const val GRADUATION = "GRADUATION"
    const val OCCUPATION = "OCCUPATION"
    const val RESIDENCE = "RESIDENCE"
    const val IMMIGRATION = "IMMIGRATION"
    const val EMIGRATION = "EMIGRATION"
    const val MILITARY = "MILITARY"
    const val RELIGION = "RELIGION"
    const val MEDICAL = "MEDICAL"
    const val ACHIEVEMENT = "ACHIEVEMENT"
    const val CUSTOM = "CUSTOM"

    fun getDisplayName(type: String): String = when (type) {
        BIRTH -> "Birth"
        DEATH -> "Death"
        MARRIAGE -> "Marriage"
        DIVORCE -> "Divorce"
        GRADUATION -> "Graduation"
        OCCUPATION -> "Career"
        RESIDENCE -> "Residence"
        IMMIGRATION -> "Immigration"
        EMIGRATION -> "Emigration"
        MILITARY -> "Military Service"
        RELIGION -> "Religious Event"
        MEDICAL -> "Medical Event"
        ACHIEVEMENT -> "Achievement"
        CUSTOM -> "Event"
        else -> type.lowercase().replaceFirstChar { it.uppercase() }
    }
}

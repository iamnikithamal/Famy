package com.famy.tree.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "family_members",
    foreignKeys = [
        ForeignKey(
            entity = FamilyTreeEntity::class,
            parentColumns = ["id"],
            childColumns = ["tree_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["tree_id"]),
        Index(value = ["first_name"]),
        Index(value = ["last_name"]),
        Index(value = ["birth_date"]),
        Index(value = ["death_date"]),
        Index(value = ["is_living"]),
        Index(value = ["generation"]),
        Index(value = ["created_at"]),
        Index(value = ["updated_at"]),
        Index(value = ["tree_id", "first_name", "last_name"])
    ]
)
data class FamilyMemberEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "tree_id")
    val treeId: Long,

    @ColumnInfo(name = "first_name")
    val firstName: String,

    @ColumnInfo(name = "middle_name")
    val middleName: String? = null,

    @ColumnInfo(name = "last_name")
    val lastName: String? = null,

    @ColumnInfo(name = "maiden_name")
    val maidenName: String? = null,

    @ColumnInfo(name = "nickname")
    val nickname: String? = null,

    @ColumnInfo(name = "gender")
    val gender: String = "UNKNOWN",

    @ColumnInfo(name = "photo_path")
    val photoPath: String? = null,

    @ColumnInfo(name = "birth_date")
    val birthDate: Long? = null,

    @ColumnInfo(name = "birth_place")
    val birthPlace: String? = null,

    @ColumnInfo(name = "birth_place_latitude")
    val birthPlaceLatitude: Double? = null,

    @ColumnInfo(name = "birth_place_longitude")
    val birthPlaceLongitude: Double? = null,

    @ColumnInfo(name = "death_date")
    val deathDate: Long? = null,

    @ColumnInfo(name = "death_place")
    val deathPlace: String? = null,

    @ColumnInfo(name = "death_place_latitude")
    val deathPlaceLatitude: Double? = null,

    @ColumnInfo(name = "death_place_longitude")
    val deathPlaceLongitude: Double? = null,

    @ColumnInfo(name = "is_living")
    val isLiving: Boolean = true,

    @ColumnInfo(name = "biography")
    val biography: String? = null,

    @ColumnInfo(name = "occupation")
    val occupation: String? = null,

    @ColumnInfo(name = "education")
    val education: String? = null,

    @ColumnInfo(name = "interests")
    val interests: String? = null,

    @ColumnInfo(name = "career_status")
    val careerStatus: String = "UNKNOWN",

    @ColumnInfo(name = "relationship_status")
    val relationshipStatus: String = "UNKNOWN",

    @ColumnInfo(name = "religion")
    val religion: String? = null,

    @ColumnInfo(name = "nationality")
    val nationality: String? = null,

    @ColumnInfo(name = "notes")
    val notes: String? = null,

    @ColumnInfo(name = "custom_fields")
    val customFields: String? = null,

    @ColumnInfo(name = "generation")
    val generation: Int = 0,

    @ColumnInfo(name = "paternal_line")
    val paternalLine: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    val fullName: String
        get() = buildString {
            append(firstName)
            lastName?.let { append(" $it") }
        }

    val displayName: String
        get() = nickname ?: fullName
}

package com.famy.tree.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "relationships",
    foreignKeys = [
        ForeignKey(
            entity = FamilyMemberEntity::class,
            parentColumns = ["id"],
            childColumns = ["member_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FamilyMemberEntity::class,
            parentColumns = ["id"],
            childColumns = ["related_member_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["member_id"]),
        Index(value = ["related_member_id"]),
        Index(value = ["relationship_type"]),
        Index(value = ["member_id", "related_member_id", "relationship_type"], unique = true)
    ]
)
data class RelationshipEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "member_id")
    val memberId: Long,

    @ColumnInfo(name = "related_member_id")
    val relatedMemberId: Long,

    @ColumnInfo(name = "relationship_type")
    val relationshipType: String,

    @ColumnInfo(name = "start_date")
    val startDate: Long? = null,

    @ColumnInfo(name = "end_date")
    val endDate: Long? = null,

    @ColumnInfo(name = "start_place")
    val startPlace: String? = null,

    @ColumnInfo(name = "notes")
    val notes: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

object RelationshipType {
    const val PARENT = "PARENT"
    const val CHILD = "CHILD"
    const val SPOUSE = "SPOUSE"
    const val SIBLING = "SIBLING"
    const val EX_SPOUSE = "EX_SPOUSE"

    fun getInverse(type: String): String = when (type) {
        PARENT -> CHILD
        CHILD -> PARENT
        SPOUSE -> SPOUSE
        EX_SPOUSE -> EX_SPOUSE
        SIBLING -> SIBLING
        else -> type
    }

    fun isSymmetric(type: String): Boolean = type in listOf(SPOUSE, EX_SPOUSE, SIBLING)
}

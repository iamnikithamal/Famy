package com.famy.tree.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "family_trees",
    indices = [
        Index(value = ["name"]),
        Index(value = ["created_at"]),
        Index(value = ["updated_at"])
    ]
)
data class FamilyTreeEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "description")
    val description: String? = null,

    @ColumnInfo(name = "cover_image_path")
    val coverImagePath: String? = null,

    @ColumnInfo(name = "root_member_id")
    val rootMemberId: Long? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

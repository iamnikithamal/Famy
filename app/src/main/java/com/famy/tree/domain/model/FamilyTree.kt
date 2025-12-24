package com.famy.tree.domain.model

data class FamilyTree(
    val id: Long = 0,
    val name: String,
    val description: String? = null,
    val coverImagePath: String? = null,
    val rootMemberId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun create(name: String, description: String? = null): FamilyTree {
            return FamilyTree(name = name, description = description)
        }
    }
}

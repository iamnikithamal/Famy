package com.famy.tree.domain.model

data class Relationship(
    val id: Long = 0,
    val memberId: Long,
    val relatedMemberId: Long,
    val type: RelationshipKind,
    val startDate: Long? = null,
    val endDate: Long? = null,
    val startPlace: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

enum class RelationshipKind {
    PARENT,
    CHILD,
    SPOUSE,
    SIBLING,
    EX_SPOUSE;

    val inverse: RelationshipKind
        get() = when (this) {
            PARENT -> CHILD
            CHILD -> PARENT
            SPOUSE -> SPOUSE
            EX_SPOUSE -> EX_SPOUSE
            SIBLING -> SIBLING
        }

    val isSymmetric: Boolean
        get() = this in listOf(SPOUSE, EX_SPOUSE, SIBLING)

    val displayName: String
        get() = when (this) {
            PARENT -> "Parent"
            CHILD -> "Child"
            SPOUSE -> "Spouse"
            SIBLING -> "Sibling"
            EX_SPOUSE -> "Ex-Spouse"
        }

    companion object {
        fun fromString(value: String): RelationshipKind {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: PARENT
        }
    }
}

data class RelationshipWithMember(
    val relationship: Relationship,
    val relatedMember: FamilyMember
)

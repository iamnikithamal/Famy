package com.famy.tree.domain.model

data class FamilyStatistics(
    val totalMembers: Int = 0,
    val livingMembers: Int = 0,
    val deceasedMembers: Int = 0,
    val maleCount: Int = 0,
    val femaleCount: Int = 0,
    val otherGenderCount: Int = 0,
    val unknownGenderCount: Int = 0,
    val generations: Int = 0,
    val averageLifespan: Double? = null,
    val averageChildrenPerPerson: Double? = null,
    val oldestLiving: FamilyMember? = null,
    val youngestMember: FamilyMember? = null,
    val longestLived: FamilyMember? = null,
    val mostCommonFirstNames: List<NameCount> = emptyList(),
    val mostCommonLastNames: List<NameCount> = emptyList(),
    val birthsByMonth: Map<Int, Int> = emptyMap(),
    val birthsByDecade: Map<Int, Int> = emptyMap(),
    val membersByGeneration: Map<Int, Int> = emptyMap()
)

data class NameCount(
    val name: String,
    val count: Int
)

data class TimelineEvent(
    val date: Long,
    val type: TimelineEventType,
    val member: FamilyMember,
    val relatedMember: FamilyMember? = null,
    val title: String,
    val description: String? = null,
    val place: String? = null
)

enum class TimelineEventType {
    BIRTH,
    DEATH,
    MARRIAGE,
    DIVORCE,
    CUSTOM
}

data class SearchFilter(
    val query: String = "",
    val generation: Int? = null,
    val paternalLine: Boolean? = null,
    val isLiving: Boolean? = null,
    val gender: Gender? = null,
    val birthDateStart: Long? = null,
    val birthDateEnd: Long? = null,
    val deathDateStart: Long? = null,
    val deathDateEnd: Long? = null
) {
    val isActive: Boolean
        get() = query.isNotBlank() ||
                generation != null ||
                paternalLine != null ||
                isLiving != null ||
                gender != null ||
                birthDateStart != null ||
                birthDateEnd != null ||
                deathDateStart != null ||
                deathDateEnd != null

    fun matches(member: FamilyMember): Boolean {
        if (query.isNotBlank()) {
            val queryLower = query.lowercase()
            val matchesName = member.firstName.lowercase().contains(queryLower) ||
                    member.lastName?.lowercase()?.contains(queryLower) == true ||
                    member.maidenName?.lowercase()?.contains(queryLower) == true ||
                    member.nickname?.lowercase()?.contains(queryLower) == true
            if (!matchesName) return false
        }

        if (generation != null && member.generation != generation) return false
        if (paternalLine != null && member.paternalLine != paternalLine) return false
        if (isLiving != null && member.isLiving != isLiving) return false
        if (gender != null && member.gender != gender) return false

        member.birthDate?.let { birthDate ->
            if (birthDateStart != null && birthDate < birthDateStart) return false
            if (birthDateEnd != null && birthDate > birthDateEnd) return false
        }

        if (!member.isLiving) {
            member.deathDate?.let { deathDate ->
                if (deathDateStart != null && deathDate < deathDateStart) return false
                if (deathDateEnd != null && deathDate > deathDateEnd) return false
            }
        }

        return true
    }
}

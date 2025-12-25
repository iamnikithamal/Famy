package com.famy.tree.domain.model

import java.util.Calendar

data class FamilyMember(
    val id: Long = 0,
    val treeId: Long,
    val firstName: String,
    val middleName: String? = null,
    val lastName: String? = null,
    val maidenName: String? = null,
    val nickname: String? = null,
    val gender: Gender = Gender.UNKNOWN,
    val photoPath: String? = null,
    val birthDate: Long? = null,
    val birthPlace: String? = null,
    val birthPlaceLatitude: Double? = null,
    val birthPlaceLongitude: Double? = null,
    val deathDate: Long? = null,
    val deathPlace: String? = null,
    val deathPlaceLatitude: Double? = null,
    val deathPlaceLongitude: Double? = null,
    val isLiving: Boolean = true,
    val biography: String? = null,
    val occupation: String? = null,
    val education: String? = null,
    val interests: List<String> = emptyList(),
    val careerStatus: CareerStatus = CareerStatus.UNKNOWN,
    val relationshipStatus: RelationshipStatus = RelationshipStatus.UNKNOWN,
    val religion: String? = null,
    val nationality: String? = null,
    val notes: String? = null,
    val customFields: Map<String, String> = emptyMap(),
    val generation: Int = 0,
    val paternalLine: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val fullName: String
        get() = buildString {
            append(firstName)
            lastName?.let { append(" $it") }
        }

    val displayName: String
        get() = nickname ?: fullName

    val age: Int?
        get() {
            val birth = birthDate ?: return null
            val end = if (isLiving) System.currentTimeMillis() else (deathDate ?: System.currentTimeMillis())
            return calculateYearsDifference(birth, end)
        }

    val lifespan: Int?
        get() {
            if (isLiving) return null
            val birth = birthDate ?: return null
            val death = deathDate ?: return null
            return calculateYearsDifference(birth, death)
        }

    private fun calculateYearsDifference(startMillis: Long, endMillis: Long): Int {
        val startCal = Calendar.getInstance().apply { timeInMillis = startMillis }
        val endCal = Calendar.getInstance().apply { timeInMillis = endMillis }
        var years = endCal.get(Calendar.YEAR) - startCal.get(Calendar.YEAR)
        if (endCal.get(Calendar.DAY_OF_YEAR) < startCal.get(Calendar.DAY_OF_YEAR)) {
            years--
        }
        return years.coerceAtLeast(0)
    }

    companion object {
        fun create(
            treeId: Long,
            firstName: String,
            lastName: String? = null,
            gender: Gender = Gender.UNKNOWN
        ): FamilyMember {
            return FamilyMember(
                treeId = treeId,
                firstName = firstName,
                lastName = lastName,
                gender = gender
            )
        }
    }
}

enum class Gender {
    MALE,
    FEMALE,
    OTHER,
    UNKNOWN;

    companion object {
        fun fromString(value: String): Gender {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: UNKNOWN
        }
    }
}

enum class CareerStatus(val displayName: String) {
    UNKNOWN("Unknown"),
    STUDENT("Student"),
    EMPLOYED("Employed"),
    SELF_EMPLOYED("Self-Employed"),
    UNEMPLOYED("Unemployed"),
    RETIRED("Retired"),
    HOMEMAKER("Homemaker"),
    MILITARY("Military"),
    ENTREPRENEUR("Entrepreneur"),
    FREELANCER("Freelancer");

    companion object {
        fun fromString(value: String): CareerStatus {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: UNKNOWN
        }
    }
}

enum class RelationshipStatus(val displayName: String) {
    UNKNOWN("Unknown"),
    SINGLE("Single"),
    IN_RELATIONSHIP("In a Relationship"),
    ENGAGED("Engaged"),
    MARRIED("Married"),
    DOMESTIC_PARTNERSHIP("Domestic Partnership"),
    SEPARATED("Separated"),
    DIVORCED("Divorced"),
    WIDOWED("Widowed");

    companion object {
        fun fromString(value: String): RelationshipStatus {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: UNKNOWN
        }
    }
}

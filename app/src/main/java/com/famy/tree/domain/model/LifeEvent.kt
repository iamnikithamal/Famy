package com.famy.tree.domain.model

data class LifeEvent(
    val id: Long = 0,
    val memberId: Long,
    val type: LifeEventKind,
    val title: String,
    val description: String? = null,
    val eventDate: Long? = null,
    val eventPlace: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

enum class LifeEventKind {
    BIRTH,
    DEATH,
    MARRIAGE,
    DIVORCE,
    GRADUATION,
    OCCUPATION,
    RESIDENCE,
    IMMIGRATION,
    EMIGRATION,
    MILITARY,
    RELIGION,
    MEDICAL,
    ACHIEVEMENT,
    CUSTOM;

    val displayName: String
        get() = when (this) {
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
        }

    val icon: String
        get() = when (this) {
            BIRTH -> "cake"
            DEATH -> "sentiment_very_dissatisfied"
            MARRIAGE -> "favorite"
            DIVORCE -> "heart_broken"
            GRADUATION -> "school"
            OCCUPATION -> "work"
            RESIDENCE -> "home"
            IMMIGRATION -> "flight_land"
            EMIGRATION -> "flight_takeoff"
            MILITARY -> "military_tech"
            RELIGION -> "church"
            MEDICAL -> "local_hospital"
            ACHIEVEMENT -> "emoji_events"
            CUSTOM -> "event"
        }

    companion object {
        fun fromString(value: String): LifeEventKind {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: CUSTOM
        }
    }
}

data class LifeEventWithMember(
    val event: LifeEvent,
    val member: FamilyMember
)

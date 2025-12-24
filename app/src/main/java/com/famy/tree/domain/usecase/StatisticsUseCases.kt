package com.famy.tree.domain.usecase

import com.famy.tree.domain.model.FamilyMember
import com.famy.tree.domain.model.FamilyStatistics
import com.famy.tree.domain.model.Gender
import com.famy.tree.domain.model.LifeEventKind
import com.famy.tree.domain.model.NameCount
import com.famy.tree.domain.model.RelationshipKind
import com.famy.tree.domain.model.TimelineEvent
import com.famy.tree.domain.model.TimelineEventType
import com.famy.tree.domain.repository.FamilyMemberRepository
import com.famy.tree.domain.repository.LifeEventRepository
import com.famy.tree.domain.repository.RelationshipRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject

class GetFamilyStatisticsUseCase @Inject constructor(
    private val memberRepository: FamilyMemberRepository,
    private val relationshipRepository: RelationshipRepository
) {
    suspend operator fun invoke(treeId: Long): FamilyStatistics {
        val members = memberRepository.getMembersByTree(treeId)
        if (members.isEmpty()) return FamilyStatistics()

        val livingMembers = members.filter { it.isLiving }
        val deceasedMembers = members.filter { !it.isLiving }

        val maleCount = members.count { it.gender == Gender.MALE }
        val femaleCount = members.count { it.gender == Gender.FEMALE }
        val otherCount = members.count { it.gender == Gender.OTHER }
        val unknownCount = members.count { it.gender == Gender.UNKNOWN }

        val generations = (memberRepository.getMaxGeneration(treeId) - memberRepository.getMinGeneration(treeId)) + 1

        val lifespans = deceasedMembers.mapNotNull { it.lifespan }
        val averageLifespan = if (lifespans.isNotEmpty()) {
            lifespans.average()
        } else null

        val childCounts = members.map { member ->
            relationshipRepository.getRelationshipCountByType(member.id, RelationshipKind.CHILD)
        }
        val averageChildren = if (childCounts.isNotEmpty()) {
            childCounts.average()
        } else null

        val oldestLiving = livingMembers
            .filter { it.birthDate != null }
            .minByOrNull { it.birthDate!! }

        val youngestMember = members
            .filter { it.birthDate != null }
            .maxByOrNull { it.birthDate!! }

        val longestLived = deceasedMembers
            .filter { it.lifespan != null }
            .maxByOrNull { it.lifespan!! }

        val firstNames = members.groupBy { it.firstName.lowercase() }
            .map { (name, list) -> NameCount(name.replaceFirstChar { it.uppercase() }, list.size) }
            .filter { it.count > 1 }
            .sortedByDescending { it.count }
            .take(10)

        val lastNames = members.mapNotNull { it.lastName?.lowercase() }
            .groupBy { it }
            .map { (name, list) -> NameCount(name.replaceFirstChar { it.uppercase() }, list.size) }
            .filter { it.count > 1 }
            .sortedByDescending { it.count }
            .take(10)

        val birthsByMonth = members
            .mapNotNull { it.birthDate }
            .map { date ->
                Calendar.getInstance().apply { timeInMillis = date }.get(Calendar.MONTH) + 1
            }
            .groupBy { it }
            .mapValues { it.value.size }

        val birthsByDecade = members
            .mapNotNull { it.birthDate }
            .map { date ->
                val year = Calendar.getInstance().apply { timeInMillis = date }.get(Calendar.YEAR)
                (year / 10) * 10
            }
            .groupBy { it }
            .mapValues { it.value.size }

        val membersByGeneration = members
            .groupBy { it.generation }
            .mapValues { it.value.size }

        return FamilyStatistics(
            totalMembers = members.size,
            livingMembers = livingMembers.size,
            deceasedMembers = deceasedMembers.size,
            maleCount = maleCount,
            femaleCount = femaleCount,
            otherGenderCount = otherCount,
            unknownGenderCount = unknownCount,
            generations = generations,
            averageLifespan = averageLifespan,
            averageChildrenPerPerson = averageChildren,
            oldestLiving = oldestLiving,
            youngestMember = youngestMember,
            longestLived = longestLived,
            mostCommonFirstNames = firstNames,
            mostCommonLastNames = lastNames,
            birthsByMonth = birthsByMonth,
            birthsByDecade = birthsByDecade,
            membersByGeneration = membersByGeneration
        )
    }

    fun observe(treeId: Long): Flow<FamilyStatistics> {
        return memberRepository.observeMembersByTree(treeId).map { members ->
            if (members.isEmpty()) return@map FamilyStatistics()

            val livingMembers = members.filter { it.isLiving }
            val deceasedMembers = members.filter { !it.isLiving }

            val maleCount = members.count { it.gender == Gender.MALE }
            val femaleCount = members.count { it.gender == Gender.FEMALE }

            val maxGen = members.maxOfOrNull { it.generation } ?: 0
            val minGen = members.minOfOrNull { it.generation } ?: 0
            val generations = maxGen - minGen + 1

            val lifespans = deceasedMembers.mapNotNull { it.lifespan }
            val averageLifespan = if (lifespans.isNotEmpty()) lifespans.average() else null

            FamilyStatistics(
                totalMembers = members.size,
                livingMembers = livingMembers.size,
                deceasedMembers = deceasedMembers.size,
                maleCount = maleCount,
                femaleCount = femaleCount,
                generations = generations,
                averageLifespan = averageLifespan
            )
        }
    }
}

class GetTimelineEventsUseCase @Inject constructor(
    private val memberRepository: FamilyMemberRepository,
    private val lifeEventRepository: LifeEventRepository,
    private val relationshipRepository: RelationshipRepository
) {
    suspend operator fun invoke(treeId: Long, filterType: TimelineEventType? = null): List<TimelineEvent> {
        val members = memberRepository.getMembersByTree(treeId)
        val memberMap = members.associateBy { it.id }
        val events = mutableListOf<TimelineEvent>()

        members.forEach { member ->
            if (filterType == null || filterType == TimelineEventType.BIRTH) {
                member.birthDate?.let { date ->
                    events.add(
                        TimelineEvent(
                            date = date,
                            type = TimelineEventType.BIRTH,
                            member = member,
                            title = "${member.displayName} was born",
                            place = member.birthPlace
                        )
                    )
                }
            }

            if (filterType == null || filterType == TimelineEventType.DEATH) {
                if (!member.isLiving) {
                    member.deathDate?.let { date ->
                        events.add(
                            TimelineEvent(
                                date = date,
                                type = TimelineEventType.DEATH,
                                member = member,
                                title = "${member.displayName} passed away",
                                description = member.lifespan?.let { "Lived $it years" },
                                place = member.deathPlace
                            )
                        )
                    }
                }
            }

            if (filterType == null || filterType == TimelineEventType.MARRIAGE) {
                val spouseRelationships = relationshipRepository.getRelationshipsByType(member.id, RelationshipKind.SPOUSE)
                spouseRelationships.forEach { rel ->
                    if (rel.memberId < rel.relatedMemberId && rel.startDate != null) {
                        val spouse = memberMap[rel.relatedMemberId]
                        if (spouse != null) {
                            events.add(
                                TimelineEvent(
                                    date = rel.startDate,
                                    type = TimelineEventType.MARRIAGE,
                                    member = member,
                                    relatedMember = spouse,
                                    title = "${member.displayName} married ${spouse.displayName}",
                                    place = rel.startPlace
                                )
                            )
                        }
                    }
                }
            }

            if (filterType == null || filterType == TimelineEventType.CUSTOM) {
                val lifeEvents = lifeEventRepository.getEvents(member.id)
                    .filter { it.type != LifeEventKind.BIRTH && it.type != LifeEventKind.DEATH }
                lifeEvents.forEach { event ->
                    event.eventDate?.let { date ->
                        events.add(
                            TimelineEvent(
                                date = date,
                                type = TimelineEventType.CUSTOM,
                                member = member,
                                title = event.title,
                                description = event.description,
                                place = event.eventPlace
                            )
                        )
                    }
                }
            }
        }

        return events.sortedBy { it.date }
    }

    fun observe(treeId: Long): Flow<List<TimelineEvent>> {
        return combine(
            memberRepository.observeMembersByTree(treeId),
            lifeEventRepository.observeEventsByTree(treeId),
            relationshipRepository.observeRelationshipsByTree(treeId)
        ) { members, lifeEvents, relationships ->
            val memberMap = members.associateBy { it.id }
            val events = mutableListOf<TimelineEvent>()

            members.forEach { member ->
                member.birthDate?.let { date ->
                    events.add(
                        TimelineEvent(
                            date = date,
                            type = TimelineEventType.BIRTH,
                            member = member,
                            title = "${member.displayName} was born",
                            place = member.birthPlace
                        )
                    )
                }

                if (!member.isLiving) {
                    member.deathDate?.let { date ->
                        events.add(
                            TimelineEvent(
                                date = date,
                                type = TimelineEventType.DEATH,
                                member = member,
                                title = "${member.displayName} passed away",
                                description = member.lifespan?.let { "Lived $it years" },
                                place = member.deathPlace
                            )
                        )
                    }
                }
            }

            relationships
                .filter { it.type == RelationshipKind.SPOUSE && it.memberId < it.relatedMemberId && it.startDate != null }
                .forEach { rel ->
                    val member = memberMap[rel.memberId]
                    val spouse = memberMap[rel.relatedMemberId]
                    if (member != null && spouse != null && rel.startDate != null) {
                        events.add(
                            TimelineEvent(
                                date = rel.startDate,
                                type = TimelineEventType.MARRIAGE,
                                member = member,
                                relatedMember = spouse,
                                title = "${member.displayName} married ${spouse.displayName}",
                                place = rel.startPlace
                            )
                        )
                    }
                }

            events.sortedBy { it.date }
        }
    }
}

package com.famy.tree.ui.screen.statistics

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.famy.tree.domain.model.FamilyMember
import com.famy.tree.domain.model.Gender
import com.famy.tree.domain.repository.FamilyMemberRepository
import com.famy.tree.domain.repository.RelationshipRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

data class StatisticsUiState(
    val totalMembers: Int = 0,
    val livingMembers: Int = 0,
    val deceasedMembers: Int = 0,
    val maleCount: Int = 0,
    val femaleCount: Int = 0,
    val otherCount: Int = 0,
    val unknownGenderCount: Int = 0,
    val generations: Int = 0,
    val averageLifespan: Double? = null,
    val oldestLiving: FamilyMember? = null,
    val youngestMember: FamilyMember? = null,
    val longestLived: FamilyMember? = null,
    val mostCommonFirstNames: List<NameCount> = emptyList(),
    val mostCommonLastNames: List<NameCount> = emptyList(),
    val birthsByMonth: Map<Int, Int> = emptyMap(),
    val birthsByDecade: Map<Int, Int> = emptyMap(),
    val generationBreakdown: Map<Int, Int> = emptyMap(),
    val averageChildrenPerPerson: Double? = null,
    val totalRelationships: Int = 0,
    val isLoading: Boolean = true
)

data class NameCount(
    val name: String,
    val count: Int
)

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    memberRepository: FamilyMemberRepository,
    relationshipRepository: RelationshipRepository
) : ViewModel() {

    private val treeId: Long = savedStateHandle.get<Long>("treeId") ?: 0L

    val uiState: StateFlow<StatisticsUiState> = combine(
        memberRepository.observeMembersByTree(treeId),
        relationshipRepository.observeRelationshipsByTree(treeId)
    ) { members, relationships ->
        if (members.isEmpty()) {
            return@combine StatisticsUiState(isLoading = false)
        }

        val livingMembers = members.filter { it.isLiving }
        val deceasedMembers = members.filter { !it.isLiving }

        val genderCounts = members.groupBy { it.gender }
        val maleCount = genderCounts[Gender.MALE]?.size ?: 0
        val femaleCount = genderCounts[Gender.FEMALE]?.size ?: 0
        val otherCount = genderCounts[Gender.OTHER]?.size ?: 0
        val unknownCount = genderCounts[Gender.UNKNOWN]?.size ?: 0

        val generations = members.maxOfOrNull { it.generation }?.let { it + 1 } ?: 0

        val lifespans = deceasedMembers.mapNotNull { it.lifespan?.toDouble() }
        val averageLifespan = if (lifespans.isNotEmpty()) {
            lifespans.average()
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

        val firstNameCounts = members
            .groupBy { it.firstName.lowercase() }
            .mapValues { it.value.size }
            .entries
            .sortedByDescending { it.value }
            .take(10)
            .map { NameCount(it.key.replaceFirstChar { c -> c.uppercase() }, it.value) }

        val lastNameCounts = members
            .mapNotNull { it.lastName }
            .groupBy { it.lowercase() }
            .mapValues { it.value.size }
            .entries
            .sortedByDescending { it.value }
            .take(10)
            .map { NameCount(it.key.replaceFirstChar { c -> c.uppercase() }, it.value) }

        val calendar = Calendar.getInstance()
        val birthsByMonth = members
            .mapNotNull { it.birthDate }
            .map { date ->
                calendar.timeInMillis = date
                calendar.get(Calendar.MONTH)
            }
            .groupBy { it }
            .mapValues { it.value.size }

        val birthsByDecade = members
            .mapNotNull { it.birthDate }
            .map { date ->
                calendar.timeInMillis = date
                (calendar.get(Calendar.YEAR) / 10) * 10
            }
            .groupBy { it }
            .mapValues { it.value.size }
            .toSortedMap()

        val generationBreakdown = members
            .groupBy { it.generation }
            .mapValues { it.value.size }
            .toSortedMap()

        val childRelationships = relationships.filter {
            it.type == com.famy.tree.domain.model.RelationshipKind.CHILD
        }
        val parentsWithChildren = childRelationships.map { it.memberId }.distinct()
        val averageChildren = if (parentsWithChildren.isNotEmpty()) {
            childRelationships.size.toDouble() / parentsWithChildren.size
        } else null

        StatisticsUiState(
            totalMembers = members.size,
            livingMembers = livingMembers.size,
            deceasedMembers = deceasedMembers.size,
            maleCount = maleCount,
            femaleCount = femaleCount,
            otherCount = otherCount,
            unknownGenderCount = unknownCount,
            generations = generations,
            averageLifespan = averageLifespan,
            oldestLiving = oldestLiving,
            youngestMember = youngestMember,
            longestLived = longestLived,
            mostCommonFirstNames = firstNameCounts,
            mostCommonLastNames = lastNameCounts,
            birthsByMonth = birthsByMonth,
            birthsByDecade = birthsByDecade,
            generationBreakdown = generationBreakdown,
            averageChildrenPerPerson = averageChildren,
            totalRelationships = relationships.size,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StatisticsUiState()
    )
}

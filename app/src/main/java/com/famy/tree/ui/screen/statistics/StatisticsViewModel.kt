package com.famy.tree.ui.screen.statistics

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.famy.tree.domain.model.FamilyMember
import com.famy.tree.domain.model.Gender
import com.famy.tree.domain.model.Relationship
import com.famy.tree.domain.model.RelationshipKind
import com.famy.tree.domain.repository.FamilyMemberRepository
import com.famy.tree.domain.repository.RelationshipRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
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
        computeStatistics(members, relationships)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = StatisticsUiState()
        )

    private fun computeStatistics(
        members: List<FamilyMember>,
        relationships: List<Relationship>
    ): StatisticsUiState {
        if (members.isEmpty()) {
            return StatisticsUiState(isLoading = false)
        }

        // Single-pass categorization for living/deceased and gender counts
        var livingCount = 0
        var deceasedCount = 0
        var maleCount = 0
        var femaleCount = 0
        var otherCount = 0
        var unknownCount = 0
        var maxGeneration = 0

        val deceasedWithLifespan = mutableListOf<FamilyMember>()
        val livingWithBirthDate = mutableListOf<FamilyMember>()
        val membersWithBirthDate = mutableListOf<FamilyMember>()

        // Name frequency maps - use single pass
        val firstNameCounts = mutableMapOf<String, Int>()
        val lastNameCounts = mutableMapOf<String, Int>()

        // Birth date aggregations
        val birthMonthCounts = mutableMapOf<Int, Int>()
        val birthDecadeCounts = mutableMapOf<Int, Int>()
        val generationCounts = mutableMapOf<Int, Int>()

        val calendar = Calendar.getInstance()

        // Single pass through all members
        for (member in members) {
            // Living/deceased
            if (member.isLiving) {
                livingCount++
                if (member.birthDate != null) {
                    livingWithBirthDate.add(member)
                }
            } else {
                deceasedCount++
                if (member.lifespan != null) {
                    deceasedWithLifespan.add(member)
                }
            }

            // Gender counts
            when (member.gender) {
                Gender.MALE -> maleCount++
                Gender.FEMALE -> femaleCount++
                Gender.OTHER -> otherCount++
                Gender.UNKNOWN -> unknownCount++
            }

            // Generation tracking
            if (member.generation > maxGeneration) {
                maxGeneration = member.generation
            }
            generationCounts[member.generation] = (generationCounts[member.generation] ?: 0) + 1

            // Birth date tracking
            member.birthDate?.let { birthDate ->
                membersWithBirthDate.add(member)
                calendar.timeInMillis = birthDate
                val month = calendar.get(Calendar.MONTH)
                val decade = (calendar.get(Calendar.YEAR) / 10) * 10
                birthMonthCounts[month] = (birthMonthCounts[month] ?: 0) + 1
                birthDecadeCounts[decade] = (birthDecadeCounts[decade] ?: 0) + 1
            }

            // Name frequency - lowercase once
            val firstNameLower = member.firstName.lowercase()
            firstNameCounts[firstNameLower] = (firstNameCounts[firstNameLower] ?: 0) + 1

            member.lastName?.let { lastName ->
                val lastNameLower = lastName.lowercase()
                lastNameCounts[lastNameLower] = (lastNameCounts[lastNameLower] ?: 0) + 1
            }
        }

        // Compute derived statistics
        val averageLifespan = if (deceasedWithLifespan.isNotEmpty()) {
            deceasedWithLifespan.sumOf { it.lifespan!!.toDouble() } / deceasedWithLifespan.size
        } else null

        val oldestLiving = livingWithBirthDate.minByOrNull { it.birthDate!! }
        val youngestMember = membersWithBirthDate.maxByOrNull { it.birthDate!! }
        val longestLived = deceasedWithLifespan.maxByOrNull { it.lifespan!! }

        // Top 10 names - sort only the entries we need
        val topFirstNames = firstNameCounts.entries
            .sortedByDescending { it.value }
            .take(10)
            .map { NameCount(it.key.replaceFirstChar { c -> c.uppercase() }, it.value) }

        val topLastNames = lastNameCounts.entries
            .sortedByDescending { it.value }
            .take(10)
            .map { NameCount(it.key.replaceFirstChar { c -> c.uppercase() }, it.value) }

        // Child relationship statistics
        val childRelationships = relationships.filter { it.type == RelationshipKind.CHILD }
        val parentsWithChildren = childRelationships.map { it.memberId }.toSet()
        val averageChildren = if (parentsWithChildren.isNotEmpty()) {
            childRelationships.size.toDouble() / parentsWithChildren.size
        } else null

        return StatisticsUiState(
            totalMembers = members.size,
            livingMembers = livingCount,
            deceasedMembers = deceasedCount,
            maleCount = maleCount,
            femaleCount = femaleCount,
            otherCount = otherCount,
            unknownGenderCount = unknownCount,
            generations = maxGeneration + 1,
            averageLifespan = averageLifespan,
            oldestLiving = oldestLiving,
            youngestMember = youngestMember,
            longestLived = longestLived,
            mostCommonFirstNames = topFirstNames,
            mostCommonLastNames = topLastNames,
            birthsByMonth = birthMonthCounts,
            birthsByDecade = birthDecadeCounts.toSortedMap(),
            generationBreakdown = generationCounts.toSortedMap(),
            averageChildrenPerPerson = averageChildren,
            totalRelationships = relationships.size,
            isLoading = false
        )
    }
}

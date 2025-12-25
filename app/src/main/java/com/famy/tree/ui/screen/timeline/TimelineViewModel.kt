package com.famy.tree.ui.screen.timeline

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.famy.tree.domain.model.FamilyMember
import com.famy.tree.domain.model.LifeEvent
import com.famy.tree.domain.model.LifeEventKind
import com.famy.tree.domain.repository.FamilyMemberRepository
import com.famy.tree.domain.repository.LifeEventRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

enum class TimelineEventType {
    BIRTH, DEATH, MARRIAGE, CUSTOM
}

data class TimelineEvent(
    val id: Long,
    val memberId: Long,
    val memberName: String,
    val memberPhotoPath: String?,
    val type: TimelineEventType,
    val title: String,
    val description: String?,
    val date: Long,
    val place: String?
)

data class TimelineUiState(
    val events: List<TimelineEvent> = emptyList(),
    val groupedEvents: Map<Int, List<TimelineEvent>> = emptyMap(),
    val selectedFilter: TimelineFilter = TimelineFilter.ALL,
    val yearRange: IntRange = IntRange.EMPTY,
    val isLoading: Boolean = true,
    val error: String? = null
)

enum class TimelineFilter {
    ALL, BIRTHS, DEATHS, MARRIAGES
}

@HiltViewModel
class TimelineViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val memberRepository: FamilyMemberRepository,
    private val lifeEventRepository: LifeEventRepository
) : ViewModel() {

    private val treeId: Long = savedStateHandle.get<Long>("treeId") ?: 0L
    private val _selectedFilter = MutableStateFlow(TimelineFilter.ALL)
    private val _isLoading = MutableStateFlow(true)

    val uiState: StateFlow<TimelineUiState> = combine(
        memberRepository.observeMembersByTree(treeId),
        lifeEventRepository.observeEventsByTree(treeId),
        _selectedFilter
    ) { members: List<FamilyMember>, lifeEvents: List<LifeEvent>, filter: TimelineFilter ->
        _isLoading.value = false
        computeTimeline(members, lifeEvents, filter)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TimelineUiState()
        )

    private fun computeTimeline(
        members: List<FamilyMember>,
        lifeEvents: List<LifeEvent>,
        filter: TimelineFilter
    ): TimelineUiState {
        val memberMap = members.associateBy { it.id }

        // Pre-calculate capacity to avoid list resizing
        val estimatedSize = members.size * 2 + lifeEvents.size
        val timelineEvents = ArrayList<TimelineEvent>(estimatedSize)

        // Build events efficiently
        for (member in members) {
            member.birthDate?.let { birthDate ->
                timelineEvents.add(
                    TimelineEvent(
                        id = member.id * 1000 + 1,
                        memberId = member.id,
                        memberName = member.fullName,
                        memberPhotoPath = member.photoPath,
                        type = TimelineEventType.BIRTH,
                        title = "Birth of ${member.firstName}",
                        description = null,
                        date = birthDate,
                        place = member.birthPlace
                    )
                )
            }

            if (!member.isLiving) {
                member.deathDate?.let { deathDate ->
                    timelineEvents.add(
                        TimelineEvent(
                            id = member.id * 1000 + 2,
                            memberId = member.id,
                            memberName = member.fullName,
                            memberPhotoPath = member.photoPath,
                            type = TimelineEventType.DEATH,
                            title = "Death of ${member.firstName}",
                            description = member.lifespan?.let { "Lived $it years" },
                            date = deathDate,
                            place = member.deathPlace
                        )
                    )
                }
            }
        }

        for (event in lifeEvents) {
            val member = memberMap[event.memberId]
            if (member != null && event.eventDate != null) {
                val eventType = when (event.type) {
                    LifeEventKind.MARRIAGE -> TimelineEventType.MARRIAGE
                    else -> TimelineEventType.CUSTOM
                }
                timelineEvents.add(
                    TimelineEvent(
                        id = event.id * 1000 + 3,
                        memberId = event.memberId,
                        memberName = member.fullName,
                        memberPhotoPath = member.photoPath,
                        type = eventType,
                        title = event.title,
                        description = event.description,
                        date = event.eventDate,
                        place = event.eventPlace
                    )
                )
            }
        }

        // Filter based on type - avoid creating intermediate lists when possible
        val filteredEvents = when (filter) {
            TimelineFilter.ALL -> timelineEvents.sortedByDescending { it.date }
            TimelineFilter.BIRTHS -> timelineEvents.filter { it.type == TimelineEventType.BIRTH }.sortedByDescending { it.date }
            TimelineFilter.DEATHS -> timelineEvents.filter { it.type == TimelineEventType.DEATH }.sortedByDescending { it.date }
            TimelineFilter.MARRIAGES -> timelineEvents.filter { it.type == TimelineEventType.MARRIAGE }.sortedByDescending { it.date }
        }

        // Group by year using single Calendar instance
        val calendar = Calendar.getInstance()
        val groupedByYear = sortedMapOf<Int, MutableList<TimelineEvent>>(reverseOrder())
        var minYear = Int.MAX_VALUE
        var maxYear = Int.MIN_VALUE

        for (event in filteredEvents) {
            calendar.timeInMillis = event.date
            val year = calendar.get(Calendar.YEAR)
            groupedByYear.getOrPut(year) { mutableListOf() }.add(event)
            if (year < minYear) minYear = year
            if (year > maxYear) maxYear = year
        }

        val yearRange = if (filteredEvents.isNotEmpty()) {
            minYear..maxYear
        } else {
            IntRange.EMPTY
        }

        return TimelineUiState(
            events = filteredEvents,
            groupedEvents = groupedByYear,
            selectedFilter = filter,
            yearRange = yearRange,
            isLoading = false
        )
    }

    fun setFilter(filter: TimelineFilter) {
        _selectedFilter.value = filter
    }
}

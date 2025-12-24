package com.famy.tree.ui.screen.members

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.famy.tree.domain.model.FamilyMember
import com.famy.tree.domain.model.Gender
import com.famy.tree.domain.repository.FamilyMemberRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

enum class MemberSortOption {
    NAME_ASC,
    NAME_DESC,
    AGE_ASC,
    AGE_DESC,
    GENERATION,
    RECENT
}

data class MembersUiState(
    val members: List<FamilyMember> = emptyList(),
    val filteredMembers: List<FamilyMember> = emptyList(),
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val selectedGender: Gender? = null,
    val selectedGeneration: Int? = null,
    val showLivingOnly: Boolean = false,
    val sortOption: MemberSortOption = MemberSortOption.NAME_ASC,
    val totalCount: Int = 0,
    val filteredCount: Int = 0,
    val error: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MembersViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val memberRepository: FamilyMemberRepository
) : ViewModel() {

    private val treeId: Long = savedStateHandle.get<Long>("treeId") ?: 0L

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedGender = MutableStateFlow<Gender?>(null)
    private val _selectedGeneration = MutableStateFlow<Int?>(null)
    private val _showLivingOnly = MutableStateFlow(false)
    private val _sortOption = MutableStateFlow(MemberSortOption.NAME_ASC)

    private val allMembers = memberRepository.observeMembersByTree(treeId)

    val generations: StateFlow<List<Int>> = allMembers.map { members ->
        members.map { it.generation }.distinct().sorted()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val uiState: StateFlow<MembersUiState> = combine(
        allMembers,
        _searchQuery,
        _selectedGender,
        _selectedGeneration,
        _showLivingOnly,
        _sortOption
    ) { array ->
        val members = array[0] as List<FamilyMember>
        val query = array[1] as String
        val gender = array[2] as Gender?
        val generation = array[3] as Int?
        val livingOnly = array[4] as Boolean
        val sort = array[5] as MemberSortOption

        val filtered = members.filter { member ->
            val matchesQuery = query.isEmpty() ||
                    member.firstName.contains(query, ignoreCase = true) ||
                    member.lastName?.contains(query, ignoreCase = true) == true ||
                    member.nickname?.contains(query, ignoreCase = true) == true

            val matchesGender = gender == null || member.gender == gender
            val matchesGeneration = generation == null || member.generation == generation
            val matchesLiving = !livingOnly || member.isLiving

            matchesQuery && matchesGender && matchesGeneration && matchesLiving
        }

        val sorted = when (sort) {
            MemberSortOption.NAME_ASC -> filtered.sortedBy { it.fullName.lowercase() }
            MemberSortOption.NAME_DESC -> filtered.sortedByDescending { it.fullName.lowercase() }
            MemberSortOption.AGE_ASC -> filtered.sortedBy { it.age ?: Int.MAX_VALUE }
            MemberSortOption.AGE_DESC -> filtered.sortedByDescending { it.age ?: 0 }
            MemberSortOption.GENERATION -> filtered.sortedBy { it.generation }
            MemberSortOption.RECENT -> filtered.sortedByDescending { it.updatedAt }
        }

        MembersUiState(
            members = members,
            filteredMembers = sorted,
            isLoading = false,
            searchQuery = query,
            selectedGender = gender,
            selectedGeneration = generation,
            showLivingOnly = livingOnly,
            sortOption = sort,
            totalCount = members.size,
            filteredCount = sorted.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MembersUiState()
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setGenderFilter(gender: Gender?) {
        _selectedGender.value = gender
    }

    fun setGenerationFilter(generation: Int?) {
        _selectedGeneration.value = generation
    }

    fun setLivingOnlyFilter(livingOnly: Boolean) {
        _showLivingOnly.value = livingOnly
    }

    fun setSortOption(option: MemberSortOption) {
        _sortOption.value = option
    }

    fun clearFilters() {
        _searchQuery.value = ""
        _selectedGender.value = null
        _selectedGeneration.value = null
        _showLivingOnly.value = false
        _sortOption.value = MemberSortOption.NAME_ASC
    }

    val hasActiveFilters: Boolean
        get() = _searchQuery.value.isNotEmpty() ||
                _selectedGender.value != null ||
                _selectedGeneration.value != null ||
                _showLivingOnly.value
}

package com.famy.tree.ui.screen.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.famy.tree.domain.model.FamilyMember
import com.famy.tree.domain.model.FamilyTree
import com.famy.tree.domain.model.Gender
import com.famy.tree.domain.repository.FamilyMemberRepository
import com.famy.tree.domain.repository.FamilyTreeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class SearchUiState(
    val searchQuery: String = "",
    val results: List<SearchResult> = emptyList(),
    val recentSearches: List<String> = emptyList(),
    val selectedGender: Gender? = null,
    val showLivingOnly: Boolean = false,
    val selectedTreeId: Long? = null,
    val trees: List<FamilyTree> = emptyList(),
    val isLoading: Boolean = false,
    val hasSearched: Boolean = false
)

data class SearchResult(
    val member: FamilyMember,
    val treeName: String,
    val matchedField: String
)

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val memberRepository: FamilyMemberRepository,
    private val treeRepository: FamilyTreeRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedGender = MutableStateFlow<Gender?>(null)
    private val _showLivingOnly = MutableStateFlow(false)
    private val _selectedTreeId = MutableStateFlow<Long?>(null)
    private val _recentSearches = MutableStateFlow<List<String>>(emptyList())
    private val _isLoading = MutableStateFlow(false)
    private val _hasSearched = MutableStateFlow(false)

    private val searchResults = combine(
        _searchQuery.debounce(300),
        _selectedGender,
        _showLivingOnly,
        _selectedTreeId,
        treeRepository.observeAllTrees()
    ) { query, gender, livingOnly, treeId, trees ->
        SearchParams(query, gender, livingOnly, treeId, trees)
    }.flatMapLatest { params ->
        _isLoading.value = params.query.isNotBlank()
        if (params.query.isBlank()) {
            _hasSearched.value = false
            kotlinx.coroutines.flow.flowOf(emptyList())
        } else {
            _hasSearched.value = true
            memberRepository.searchAllMembers(params.query).combine(
                kotlinx.coroutines.flow.flowOf(params)
            ) { members, searchParams ->
                _isLoading.value = false
                val treeMap = searchParams.trees.associateBy { it.id }
                members
                    .filter { member ->
                        val genderMatch = searchParams.gender == null || member.gender == searchParams.gender
                        val livingMatch = !searchParams.livingOnly || member.isLiving
                        val treeMatch = searchParams.treeId == null || member.treeId == searchParams.treeId
                        genderMatch && livingMatch && treeMatch
                    }
                    .map { member ->
                        val matchedField = findMatchedField(member, searchParams.query)
                        SearchResult(
                            member = member,
                            treeName = treeMap[member.treeId]?.name ?: "Unknown Tree",
                            matchedField = matchedField
                        )
                    }
                    .sortedBy { it.member.fullName }
            }
        }
    }

    val uiState: StateFlow<SearchUiState> = combine(
        _searchQuery,
        searchResults,
        _recentSearches,
        _selectedGender,
        _showLivingOnly,
        _selectedTreeId,
        treeRepository.observeAllTrees(),
        _isLoading,
        _hasSearched
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        SearchUiState(
            searchQuery = values[0] as String,
            results = values[1] as List<SearchResult>,
            recentSearches = values[2] as List<String>,
            selectedGender = values[3] as Gender?,
            showLivingOnly = values[4] as Boolean,
            selectedTreeId = values[5] as Long?,
            trees = values[6] as List<FamilyTree>,
            isLoading = values[7] as Boolean,
            hasSearched = values[8] as Boolean
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SearchUiState()
    )

    private fun findMatchedField(member: FamilyMember, query: String): String {
        val lowerQuery = query.lowercase()
        return when {
            member.firstName.lowercase().contains(lowerQuery) -> "First name"
            member.lastName?.lowercase()?.contains(lowerQuery) == true -> "Last name"
            member.maidenName?.lowercase()?.contains(lowerQuery) == true -> "Maiden name"
            member.nickname?.lowercase()?.contains(lowerQuery) == true -> "Nickname"
            member.birthPlace?.lowercase()?.contains(lowerQuery) == true -> "Birth place"
            member.deathPlace?.lowercase()?.contains(lowerQuery) == true -> "Death place"
            member.occupation?.lowercase()?.contains(lowerQuery) == true -> "Occupation"
            member.biography?.lowercase()?.contains(lowerQuery) == true -> "Biography"
            member.notes?.lowercase()?.contains(lowerQuery) == true -> "Notes"
            else -> "Name"
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setGenderFilter(gender: Gender?) {
        _selectedGender.value = gender
    }

    fun setLivingOnlyFilter(livingOnly: Boolean) {
        _showLivingOnly.value = livingOnly
    }

    fun setTreeFilter(treeId: Long?) {
        _selectedTreeId.value = treeId
    }

    fun clearFilters() {
        _selectedGender.value = null
        _showLivingOnly.value = false
        _selectedTreeId.value = null
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _hasSearched.value = false
    }

    fun addToRecentSearches(query: String) {
        if (query.isNotBlank()) {
            val current = _recentSearches.value.toMutableList()
            current.remove(query)
            current.add(0, query)
            _recentSearches.value = current.take(10)
        }
    }

    val hasActiveFilters: Boolean
        get() = _selectedGender.value != null || _showLivingOnly.value || _selectedTreeId.value != null

    private data class SearchParams(
        val query: String,
        val gender: Gender?,
        val livingOnly: Boolean,
        val treeId: Long?,
        val trees: List<FamilyTree>
    )
}

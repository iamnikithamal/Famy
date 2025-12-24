package com.famy.tree.ui.screen.relationship

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.famy.tree.domain.model.FamilyMember
import com.famy.tree.domain.model.RelationshipKind
import com.famy.tree.domain.repository.FamilyMemberRepository
import com.famy.tree.domain.repository.RelationshipRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddRelationshipUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val currentMember: FamilyMember? = null,
    val relationshipType: RelationshipKind = RelationshipKind.PARENT,
    val availableMembers: List<FamilyMember> = emptyList(),
    val filteredMembers: List<FamilyMember> = emptyList(),
    val searchQuery: String = "",
    val selectedMemberId: Long? = null,
    val startDate: Long? = null,
    val startPlace: String = "",
    val notes: String = "",
    val showCreateNewOption: Boolean = true,
    val error: String? = null,
    val savedSuccessfully: Boolean = false
)

@HiltViewModel
class AddRelationshipViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val memberRepository: FamilyMemberRepository,
    private val relationshipRepository: RelationshipRepository
) : ViewModel() {

    private val memberId: Long = savedStateHandle.get<Long>("memberId") ?: 0L
    private val relationshipTypeString: String = savedStateHandle.get<String>("relationshipType") ?: "PARENT"

    private val _uiState = MutableStateFlow(AddRelationshipUiState())
    val uiState: StateFlow<AddRelationshipUiState> = _uiState.asStateFlow()

    val treeId: Long
        get() = _uiState.value.currentMember?.treeId ?: 0L

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                val relationshipType = RelationshipKind.fromString(relationshipTypeString)
                val member = memberRepository.getMember(memberId)

                if (member == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Member not found"
                        )
                    }
                    return@launch
                }

                val allMembers = memberRepository.observeMembersByTree(member.treeId).first()
                val availableMembers = filterAvailableMembers(member, allMembers, relationshipType)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentMember = member,
                        relationshipType = relationshipType,
                        availableMembers = availableMembers,
                        filteredMembers = availableMembers,
                        showCreateNewOption = true
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load data"
                    )
                }
            }
        }
    }

    private suspend fun filterAvailableMembers(
        currentMember: FamilyMember,
        allMembers: List<FamilyMember>,
        relationshipType: RelationshipKind
    ): List<FamilyMember> {
        val existingRelationshipIds = mutableSetOf<Long>()

        val existingRelationships = relationshipRepository.getRelationships(currentMember.id)
        existingRelationshipIds.addAll(existingRelationships.map { it.relatedMemberId })

        val allExistingRelationships = relationshipRepository.getAllRelationships(currentMember.id)
        val relatedIdsOfSameType = allExistingRelationships
            .filter { it.type == relationshipType }
            .map { it.relatedMemberId }
        existingRelationshipIds.addAll(relatedIdsOfSameType)

        return allMembers.filter { member ->
            member.id != currentMember.id && member.id !in existingRelationshipIds
        }.sortedBy { it.fullName }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { state ->
            val filtered = if (query.isBlank()) {
                state.availableMembers
            } else {
                state.availableMembers.filter { member ->
                    member.fullName.contains(query, ignoreCase = true) ||
                            member.firstName.contains(query, ignoreCase = true) ||
                            (member.lastName?.contains(query, ignoreCase = true) == true) ||
                            (member.maidenName?.contains(query, ignoreCase = true) == true) ||
                            (member.nickname?.contains(query, ignoreCase = true) == true)
                }
            }
            state.copy(
                searchQuery = query,
                filteredMembers = filtered
            )
        }
    }

    fun selectMember(memberId: Long?) {
        _uiState.update { it.copy(selectedMemberId = memberId) }
    }

    fun updateStartDate(date: Long?) {
        _uiState.update { it.copy(startDate = date) }
    }

    fun updateStartPlace(place: String) {
        _uiState.update { it.copy(startPlace = place) }
    }

    fun updateNotes(notes: String) {
        _uiState.update { it.copy(notes = notes) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun saveRelationship(onSuccess: () -> Unit) {
        val state = _uiState.value
        val selectedId = state.selectedMemberId

        if (selectedId == null) {
            _uiState.update { it.copy(error = "Please select a family member") }
            return
        }

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isSaving = true) }

                val exists = relationshipRepository.relationshipExists(
                    memberId = memberId,
                    relatedMemberId = selectedId,
                    type = state.relationshipType
                )

                if (exists) {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = "This relationship already exists"
                        )
                    }
                    return@launch
                }

                relationshipRepository.createRelationship(
                    memberId = memberId,
                    relatedMemberId = selectedId,
                    type = state.relationshipType,
                    startDate = state.startDate,
                    startPlace = state.startPlace.takeIf { it.isNotBlank() },
                    notes = state.notes.takeIf { it.isNotBlank() }
                )

                if (state.relationshipType.isSymmetric) {
                    val inverseExists = relationshipRepository.relationshipExists(
                        memberId = selectedId,
                        relatedMemberId = memberId,
                        type = state.relationshipType
                    )

                    if (!inverseExists) {
                        relationshipRepository.createRelationship(
                            memberId = selectedId,
                            relatedMemberId = memberId,
                            type = state.relationshipType,
                            startDate = state.startDate,
                            startPlace = state.startPlace.takeIf { it.isNotBlank() },
                            notes = state.notes.takeIf { it.isNotBlank() }
                        )
                    }
                } else {
                    val inverseType = state.relationshipType.inverse
                    val inverseExists = relationshipRepository.relationshipExists(
                        memberId = selectedId,
                        relatedMemberId = memberId,
                        type = inverseType
                    )

                    if (!inverseExists) {
                        relationshipRepository.createRelationship(
                            memberId = selectedId,
                            relatedMemberId = memberId,
                            type = inverseType,
                            startDate = state.startDate,
                            startPlace = state.startPlace.takeIf { it.isNotBlank() },
                            notes = state.notes.takeIf { it.isNotBlank() }
                        )
                    }
                }

                _uiState.update {
                    it.copy(
                        isSaving = false,
                        savedSuccessfully = true
                    )
                }
                onSuccess()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = e.message ?: "Failed to save relationship"
                    )
                }
            }
        }
    }

    fun getRelationshipTypeDisplayName(): String {
        return when (_uiState.value.relationshipType) {
            RelationshipKind.PARENT -> "Parent"
            RelationshipKind.CHILD -> "Child"
            RelationshipKind.SPOUSE -> "Spouse"
            RelationshipKind.SIBLING -> "Sibling"
            RelationshipKind.EX_SPOUSE -> "Ex-Spouse"
        }
    }

    fun getDateFieldLabel(): String {
        return when (_uiState.value.relationshipType) {
            RelationshipKind.SPOUSE, RelationshipKind.EX_SPOUSE -> "Marriage Date"
            else -> "Relationship Date"
        }
    }

    fun getPlaceFieldLabel(): String {
        return when (_uiState.value.relationshipType) {
            RelationshipKind.SPOUSE, RelationshipKind.EX_SPOUSE -> "Marriage Place"
            else -> "Place"
        }
    }

    fun shouldShowDateField(): Boolean {
        return _uiState.value.relationshipType in listOf(
            RelationshipKind.SPOUSE,
            RelationshipKind.EX_SPOUSE
        )
    }

    fun shouldShowPlaceField(): Boolean {
        return _uiState.value.relationshipType in listOf(
            RelationshipKind.SPOUSE,
            RelationshipKind.EX_SPOUSE
        )
    }
}

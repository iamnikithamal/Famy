package com.famy.tree.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.famy.tree.domain.model.FamilyMember
import com.famy.tree.domain.model.FamilyTree
import com.famy.tree.domain.repository.FamilyMemberRepository
import com.famy.tree.domain.repository.FamilyTreeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val trees: List<FamilyTree> = emptyList(),
    val recentMembers: List<FamilyMember> = emptyList(),
    val totalMembers: Int = 0,
    val totalGenerations: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val treeRepository: FamilyTreeRepository,
    private val memberRepository: FamilyMemberRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<HomeUiState> = combine(
        treeRepository.observeAllTrees(),
        memberRepository.observeRecentMembers(5),
        memberRepository.observeTotalMemberCount(),
        _isLoading,
        _error
    ) { trees, recentMembers, totalMembers, isLoading, error ->
        val maxGen = trees.maxOfOrNull { tree ->
            memberRepository.getMaxGeneration(tree.id)
        } ?: 0

        HomeUiState(
            trees = trees,
            recentMembers = recentMembers,
            totalMembers = totalMembers,
            totalGenerations = maxGen + 1,
            isLoading = false,
            error = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    private val _showCreateDialog = MutableStateFlow(false)
    val showCreateDialog: StateFlow<Boolean> = _showCreateDialog.asStateFlow()

    fun showCreateTreeDialog() {
        _showCreateDialog.value = true
    }

    fun hideCreateTreeDialog() {
        _showCreateDialog.value = false
    }

    fun createTree(name: String, description: String?) {
        viewModelScope.launch {
            try {
                treeRepository.createTree(name, description)
                _showCreateDialog.value = false
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun deleteTree(treeId: Long) {
        viewModelScope.launch {
            try {
                treeRepository.deleteTree(treeId)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}

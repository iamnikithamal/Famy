package com.famy.tree.ui.screen.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.famy.tree.domain.model.FamilyMember
import com.famy.tree.domain.model.LifeEvent
import com.famy.tree.domain.model.Media
import com.famy.tree.domain.model.RelationshipKind
import com.famy.tree.domain.model.RelationshipWithMember
import com.famy.tree.domain.repository.FamilyMemberRepository
import com.famy.tree.domain.repository.LifeEventRepository
import com.famy.tree.domain.repository.MediaRepository
import com.famy.tree.domain.usecase.DeleteMemberUseCase
import com.famy.tree.domain.usecase.GetMemberRelationshipsUseCase
import com.famy.tree.domain.usecase.RemoveRelationshipUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject

data class ProfileUiState(
    val member: FamilyMember? = null,
    val relationships: List<RelationshipWithMember> = emptyList(),
    val parents: List<RelationshipWithMember> = emptyList(),
    val spouses: List<RelationshipWithMember> = emptyList(),
    val children: List<RelationshipWithMember> = emptyList(),
    val siblings: List<RelationshipWithMember> = emptyList(),
    val lifeEvents: List<LifeEvent> = emptyList(),
    val media: List<Media> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val memberRepository: FamilyMemberRepository,
    private val getMemberRelationships: GetMemberRelationshipsUseCase,
    private val lifeEventRepository: LifeEventRepository,
    private val mediaRepository: MediaRepository,
    private val deleteMemberUseCase: DeleteMemberUseCase,
    private val removeRelationshipUseCase: RemoveRelationshipUseCase
) : ViewModel() {

    private val memberId: Long = savedStateHandle.get<Long>("memberId") ?: 0L

    private val _isLoading = MutableStateFlow(true)
    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ProfileUiState> = combine(
        memberRepository.observeMember(memberId),
        getMemberRelationships.observe(memberId),
        lifeEventRepository.observeEventsByMember(memberId),
        mediaRepository.observeMediaByMember(memberId),
        _isLoading,
        _error
    ) { array ->
        val member = array[0] as FamilyMember?
        val relationships = array[1] as List<RelationshipWithMember>
        val events = array[2] as List<LifeEvent>
        val media = array[3] as List<Media>
        val isLoading = array[4] as Boolean
        val error = array[5] as String?

        val parents = relationships.filter { it.relationship.type == RelationshipKind.PARENT }
        val spouses = relationships.filter {
            it.relationship.type == RelationshipKind.SPOUSE ||
            it.relationship.type == RelationshipKind.EX_SPOUSE
        }
        val children = relationships.filter { it.relationship.type == RelationshipKind.CHILD }
        val siblings = relationships.filter { it.relationship.type == RelationshipKind.SIBLING }

        ProfileUiState(
            member = member,
            relationships = relationships,
            parents = parents,
            spouses = spouses,
            children = children,
            siblings = siblings,
            lifeEvents = events.sortedBy { it.eventDate },
            media = media,
            isLoading = member == null && isLoading,
            error = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProfileUiState()
    )

    private val _showDeleteDialog = MutableStateFlow(false)
    val showDeleteDialog: StateFlow<Boolean> = _showDeleteDialog.asStateFlow()

    fun showDeleteDialog() {
        _showDeleteDialog.value = true
    }

    fun hideDeleteDialog() {
        _showDeleteDialog.value = false
    }

    fun deleteMember(onDeleted: () -> Unit) {
        viewModelScope.launch {
            try {
                deleteMemberUseCase(memberId)
                onDeleted()
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun removeRelationship(relationshipId: Long) {
        viewModelScope.launch {
            try {
                removeRelationshipUseCase(relationshipId)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun updatePhoto(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val member = uiState.value.member ?: return@launch

                val photoDir = File(context.filesDir, "photos")
                if (!photoDir.exists()) photoDir.mkdirs()

                val photoFile = File(photoDir, "${member.id}_${UUID.randomUUID()}.jpg")

                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(photoFile).use { output ->
                        input.copyTo(output)
                    }
                }

                member.photoPath?.let { oldPath ->
                    File(oldPath).delete()
                }

                memberRepository.updateMemberPhoto(memberId, photoFile.absolutePath)
            } catch (e: Exception) {
                _error.value = "Failed to update photo: ${e.message}"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    val treeId: Long
        get() = uiState.value.member?.treeId ?: 0L
}

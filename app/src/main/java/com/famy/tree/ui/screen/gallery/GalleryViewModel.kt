package com.famy.tree.ui.screen.gallery

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.famy.tree.domain.model.FamilyMember
import com.famy.tree.domain.model.Media
import com.famy.tree.domain.model.MediaKind
import com.famy.tree.domain.model.MediaWithMember
import com.famy.tree.domain.repository.FamilyMemberRepository
import com.famy.tree.domain.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GalleryUiState(
    val isLoading: Boolean = true,
    val mediaItems: List<MediaWithMember> = emptyList(),
    val filteredMedia: List<MediaWithMember> = emptyList(),
    val selectedFilter: MediaKind? = null,
    val searchQuery: String = "",
    val selectedMemberId: Long? = null,
    val members: List<FamilyMember> = emptyList(),
    val totalSize: String = "0 B",
    val totalCount: Int = 0,
    val selectedMedia: Media? = null,
    val showMediaViewer: Boolean = false,
    val error: String? = null,
    val isDeleting: Boolean = false,
    val viewMode: GalleryViewMode = GalleryViewMode.GRID
)

enum class GalleryViewMode {
    GRID,
    LIST
}

@HiltViewModel
class GalleryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val mediaRepository: MediaRepository,
    private val memberRepository: FamilyMemberRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val treeId: Long = savedStateHandle.get<Long>("treeId") ?: 0L

    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    init {
        loadGallery()
    }

    private fun loadGallery() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                val members = memberRepository.observeMembersByTree(treeId).first()
                val mediaFlow = mediaRepository.observeMediaByTree(treeId)

                mediaFlow.collect { mediaList ->
                    val membersMap = members.associateBy { it.id }
                    val mediaWithMembers = mediaList.mapNotNull { media ->
                        val member = membersMap[media.memberId]
                        if (member != null) {
                            MediaWithMember(media, member)
                        } else null
                    }.sortedByDescending { it.media.createdAt }

                    val totalSize = formatSize(mediaList.sumOf { it.fileSize })

                    _uiState.update { state ->
                        val filtered = filterMedia(mediaWithMembers, state)
                        state.copy(
                            isLoading = false,
                            mediaItems = mediaWithMembers,
                            filteredMedia = filtered,
                            members = members,
                            totalSize = totalSize,
                            totalCount = mediaList.size
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load gallery"
                    )
                }
            }
        }
    }

    fun setFilter(filter: MediaKind?) {
        _uiState.update { state ->
            val newState = state.copy(selectedFilter = filter)
            newState.copy(filteredMedia = filterMedia(state.mediaItems, newState))
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { state ->
            val newState = state.copy(searchQuery = query)
            newState.copy(filteredMedia = filterMedia(state.mediaItems, newState))
        }
    }

    fun setMemberFilter(memberId: Long?) {
        _uiState.update { state ->
            val newState = state.copy(selectedMemberId = memberId)
            newState.copy(filteredMedia = filterMedia(state.mediaItems, newState))
        }
    }

    fun setViewMode(mode: GalleryViewMode) {
        _uiState.update { it.copy(viewMode = mode) }
    }

    fun selectMedia(media: Media?) {
        _uiState.update { it.copy(selectedMedia = media, showMediaViewer = media != null) }
    }

    fun dismissMediaViewer() {
        _uiState.update { it.copy(selectedMedia = null, showMediaViewer = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearFilters() {
        _uiState.update { state ->
            val newState = state.copy(
                selectedFilter = null,
                searchQuery = "",
                selectedMemberId = null
            )
            newState.copy(filteredMedia = state.mediaItems)
        }
    }

    val hasActiveFilters: Boolean
        get() = with(_uiState.value) {
            selectedFilter != null || searchQuery.isNotBlank() || selectedMemberId != null
        }

    fun deleteMedia(mediaId: Long) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isDeleting = true) }
                mediaRepository.deleteMedia(mediaId)
                _uiState.update { it.copy(isDeleting = false, selectedMedia = null, showMediaViewer = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isDeleting = false,
                        error = e.message ?: "Failed to delete media"
                    )
                }
            }
        }
    }

    fun addMedia(
        memberId: Long,
        uri: Uri,
        title: String? = null,
        description: String? = null
    ) {
        viewModelScope.launch {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val fileName = getFileName(uri) ?: "file_${System.currentTimeMillis()}"
                    val mimeType = context.contentResolver.getType(uri)

                    mediaRepository.addMedia(
                        memberId = memberId,
                        inputStream = inputStream,
                        fileName = fileName,
                        mimeType = mimeType,
                        title = title,
                        description = description
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to add media")
                }
            }
        }
    }

    private fun getFileName(uri: Uri): String? {
        return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            if (nameIndex >= 0) cursor.getString(nameIndex) else null
        }
    }

    private fun filterMedia(
        items: List<MediaWithMember>,
        state: GalleryUiState
    ): List<MediaWithMember> {
        return items.filter { item ->
            val matchesFilter = state.selectedFilter == null || item.media.type == state.selectedFilter
            val matchesMember = state.selectedMemberId == null || item.media.memberId == state.selectedMemberId
            val matchesSearch = state.searchQuery.isBlank() ||
                    item.media.title?.contains(state.searchQuery, ignoreCase = true) == true ||
                    item.media.description?.contains(state.searchQuery, ignoreCase = true) == true ||
                    item.member.fullName.contains(state.searchQuery, ignoreCase = true)
            matchesFilter && matchesMember && matchesSearch
        }
    }

    private fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> String.format("%.1f GB", bytes.toDouble() / (1024 * 1024 * 1024))
        }
    }

    fun getMediaTypeStats(): Map<MediaKind, Int> {
        return _uiState.value.mediaItems.groupBy { it.media.type }
            .mapValues { it.value.size }
    }
}

package com.famy.tree.ui.screen.tree

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.famy.tree.domain.model.FamilyMember
import com.famy.tree.domain.model.FamilyTree
import com.famy.tree.domain.model.Relationship
import com.famy.tree.domain.model.TreeBounds
import com.famy.tree.domain.model.TreeLayoutConfig
import com.famy.tree.domain.model.TreeLayoutType
import com.famy.tree.domain.model.TreeNode
import com.famy.tree.domain.repository.FamilyMemberRepository
import com.famy.tree.domain.repository.FamilyTreeRepository
import com.famy.tree.domain.repository.RelationshipRepository
import com.famy.tree.domain.usecase.BuildTreeStructureUseCase
import com.famy.tree.domain.usecase.CalculateTreeLayoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class TreeUiState(
    val tree: FamilyTree? = null,
    val members: List<FamilyMember> = emptyList(),
    val relationships: List<Relationship> = emptyList(),
    val rootNode: TreeNode? = null,
    val layoutNodes: List<TreeNode> = emptyList(),
    val bounds: TreeBounds = TreeBounds.EMPTY,
    val layoutConfig: TreeLayoutConfig = TreeLayoutConfig(),
    val selectedMemberId: Long? = null,
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class TreeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val treeRepository: FamilyTreeRepository,
    private val memberRepository: FamilyMemberRepository,
    private val relationshipRepository: RelationshipRepository,
    private val buildTreeStructure: BuildTreeStructureUseCase,
    private val calculateTreeLayout: CalculateTreeLayoutUseCase
) : ViewModel() {

    private val treeId: Long = savedStateHandle.get<Long>("treeId") ?: 0L

    private val _layoutConfig = MutableStateFlow(TreeLayoutConfig())
    private val _scale = MutableStateFlow(1f)
    private val _offsetX = MutableStateFlow(0f)
    private val _offsetY = MutableStateFlow(0f)
    private val _selectedMemberId = MutableStateFlow<Long?>(null)
    private val _isLoading = MutableStateFlow(true)
    private val _error = MutableStateFlow<String?>(null)
    private val _rootNode = MutableStateFlow<TreeNode?>(null)
    private val _layoutNodes = MutableStateFlow<List<TreeNode>>(emptyList())
    private val _bounds = MutableStateFlow(TreeBounds.EMPTY)

    // Track data versions to detect actual changes
    private var lastMemberIds: Set<Long> = emptySet()
    private var lastMemberUpdateTimes: Map<Long, Long> = emptyMap()
    private var lastRelationshipCount: Int = 0
    private var lastRootMemberId: Long? = null
    private var lastLayoutType: TreeLayoutType? = null

    // Create a flow that only emits when tree structure actually changes
    private val treeDataFlow = combine(
        treeRepository.observeTree(treeId),
        memberRepository.observeMembersByTree(treeId),
        relationshipRepository.observeRelationshipsByTree(treeId),
        _layoutConfig
    ) { tree, members, relationships, config ->
        TreeData(tree, members, relationships, config)
    }.distinctUntilChanged { old, new ->
        // Only consider equal if structural data hasn't changed
        val memberIds = new.members.map { it.id }.toSet()
        val memberUpdateTimes = new.members.associate { it.id to it.updatedAt }

        val sameMembers = memberIds == lastMemberIds &&
                memberUpdateTimes == lastMemberUpdateTimes
        val sameRelationships = new.relationships.size == lastRelationshipCount
        val sameRoot = new.tree?.rootMemberId == lastRootMemberId
        val sameLayout = new.config.layoutType == lastLayoutType

        // Update tracking variables
        lastMemberIds = memberIds
        lastMemberUpdateTimes = memberUpdateTimes
        lastRelationshipCount = new.relationships.size
        lastRootMemberId = new.tree?.rootMemberId
        lastLayoutType = new.config.layoutType

        sameMembers && sameRelationships && sameRoot && sameLayout
    }

    val uiState: StateFlow<TreeUiState> = combine(
        treeDataFlow,
        _scale,
        _offsetX,
        _offsetY,
        _selectedMemberId
    ) { data, scale, offsetX, offsetY, selectedMemberId ->
        if (data.members.isNotEmpty()) {
            rebuildTree(data.tree, data.members, data.relationships, data.config)
        }

        TreeUiState(
            tree = data.tree,
            members = data.members,
            relationships = data.relationships,
            rootNode = _rootNode.value,
            layoutNodes = _layoutNodes.value,
            bounds = _bounds.value,
            layoutConfig = data.config,
            selectedMemberId = selectedMemberId,
            scale = scale,
            offsetX = offsetX,
            offsetY = offsetY,
            isLoading = false,
            error = _error.value
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TreeUiState()
    )

    private data class TreeData(
        val tree: FamilyTree?,
        val members: List<FamilyMember>,
        val relationships: List<Relationship>,
        val config: TreeLayoutConfig
    )

    val scale: StateFlow<Float> = _scale.asStateFlow()
    val offsetX: StateFlow<Float> = _offsetX.asStateFlow()
    val offsetY: StateFlow<Float> = _offsetY.asStateFlow()
    val selectedMemberId: StateFlow<Long?> = _selectedMemberId.asStateFlow()

    private suspend fun rebuildTree(
        tree: FamilyTree?,
        members: List<FamilyMember>,
        relationships: List<Relationship>,
        config: TreeLayoutConfig
    ) {
        try {
            // Move expensive tree building operations to background thread
            withContext(Dispatchers.Default) {
                val rootNode = buildTreeStructure(treeId, tree?.rootMemberId)
                _rootNode.value = rootNode

                if (rootNode != null) {
                    val (nodes, bounds) = calculateTreeLayout(rootNode, config)
                    _layoutNodes.value = nodes
                    _bounds.value = bounds
                } else {
                    _layoutNodes.value = emptyList()
                    _bounds.value = TreeBounds.EMPTY
                }
            }
        } catch (e: Exception) {
            _error.value = e.message
        }
    }

    fun setLayoutType(type: TreeLayoutType) {
        _layoutConfig.value = _layoutConfig.value.copy(layoutType = type)
    }

    fun setScale(scale: Float) {
        _scale.value = scale.coerceIn(0.2f, 3f)
    }

    fun setOffset(x: Float, y: Float) {
        _offsetX.value = x
        _offsetY.value = y
    }

    fun onScaleChange(scaleDelta: Float) {
        val newScale = (_scale.value * scaleDelta).coerceIn(0.2f, 3f)
        _scale.value = newScale
    }

    fun onPan(panX: Float, panY: Float) {
        _offsetX.value += panX
        _offsetY.value += panY
    }

    fun selectMember(memberId: Long?) {
        _selectedMemberId.value = memberId
    }

    fun centerOnMember(memberId: Long) {
        val node = _layoutNodes.value.find { it.member.id == memberId } ?: return
        val config = _layoutConfig.value
        _offsetX.value = -(node.x + config.nodeWidth / 2)
        _offsetY.value = -(node.y + config.nodeHeight / 2)
    }

    fun resetView() {
        _scale.value = 1f
        _offsetX.value = 0f
        _offsetY.value = 0f
    }

    fun setRootMember(memberId: Long) {
        viewModelScope.launch {
            try {
                treeRepository.setRootMember(treeId, memberId)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}

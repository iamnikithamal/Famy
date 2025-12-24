package com.famy.tree.domain.usecase

import com.famy.tree.domain.model.FamilyMember
import com.famy.tree.domain.model.FamilyTree
import com.famy.tree.domain.model.RelationshipKind
import com.famy.tree.domain.model.TreeBounds
import com.famy.tree.domain.model.TreeLayoutConfig
import com.famy.tree.domain.model.TreeNode
import com.famy.tree.domain.repository.FamilyMemberRepository
import com.famy.tree.domain.repository.FamilyTreeRepository
import com.famy.tree.domain.repository.RelationshipRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetAllTreesUseCase @Inject constructor(
    private val treeRepository: FamilyTreeRepository
) {
    operator fun invoke(): Flow<List<FamilyTree>> = treeRepository.observeAllTrees()
}

class GetTreeUseCase @Inject constructor(
    private val treeRepository: FamilyTreeRepository
) {
    operator fun invoke(treeId: Long): Flow<FamilyTree?> = treeRepository.observeTree(treeId)
    suspend fun get(treeId: Long): FamilyTree? = treeRepository.getTree(treeId)
}

class CreateTreeUseCase @Inject constructor(
    private val treeRepository: FamilyTreeRepository
) {
    suspend operator fun invoke(name: String, description: String? = null): FamilyTree {
        return treeRepository.createTree(name, description)
    }
}

class UpdateTreeUseCase @Inject constructor(
    private val treeRepository: FamilyTreeRepository
) {
    suspend operator fun invoke(tree: FamilyTree) {
        treeRepository.updateTree(tree)
    }
}

class DeleteTreeUseCase @Inject constructor(
    private val treeRepository: FamilyTreeRepository
) {
    suspend operator fun invoke(treeId: Long) {
        treeRepository.deleteTree(treeId)
    }
}

class BuildTreeStructureUseCase @Inject constructor(
    private val memberRepository: FamilyMemberRepository,
    private val relationshipRepository: RelationshipRepository
) {
    suspend operator fun invoke(treeId: Long, rootMemberId: Long?): TreeNode? {
        val members = memberRepository.getMembersByTree(treeId)
        if (members.isEmpty()) return null

        val relationships = relationshipRepository.getRelationshipsByTree(treeId)
        val memberMap = members.associateBy { it.id }

        val rootMember = rootMemberId?.let { memberMap[it] }
            ?: findBestRootMember(members, relationships, memberMap)
            ?: return null

        return buildTreeNode(rootMember, memberMap, relationships, mutableSetOf(), 0)
    }

    private fun findBestRootMember(
        members: List<FamilyMember>,
        relationships: List<com.famy.tree.domain.model.Relationship>,
        memberMap: Map<Long, FamilyMember>
    ): FamilyMember? {
        val childIds = relationships
            .filter { it.type == RelationshipKind.CHILD }
            .map { it.relatedMemberId }
            .toSet()

        val rootCandidates = members.filter { it.id !in childIds }

        return rootCandidates.minByOrNull { member ->
            member.generation
        } ?: members.firstOrNull()
    }

    private fun buildTreeNode(
        member: FamilyMember,
        memberMap: Map<Long, FamilyMember>,
        relationships: List<com.famy.tree.domain.model.Relationship>,
        visited: MutableSet<Long>,
        depth: Int
    ): TreeNode {
        if (member.id in visited) {
            return TreeNode(member = member, depth = depth)
        }
        visited.add(member.id)

        val memberRelationships = relationships.filter { it.memberId == member.id }

        val spouseIds = memberRelationships
            .filter { it.type == RelationshipKind.SPOUSE }
            .map { it.relatedMemberId }
        val spouses = spouseIds.mapNotNull { memberMap[it] }

        val childIds = memberRelationships
            .filter { it.type == RelationshipKind.CHILD }
            .map { it.relatedMemberId }
        val children = childIds.mapNotNull { memberMap[it] }
            .filter { it.id !in visited }
            .map { child ->
                buildTreeNode(child, memberMap, relationships, visited, depth + 1)
            }

        val siblingIds = memberRelationships
            .filter { it.type == RelationshipKind.SIBLING }
            .map { it.relatedMemberId }
        val siblings = siblingIds.mapNotNull { memberMap[it] }

        return TreeNode(
            member = member,
            spouses = spouses,
            children = children,
            siblings = siblings,
            depth = depth
        )
    }
}

class CalculateTreeLayoutUseCase @Inject constructor() {
    operator fun invoke(
        rootNode: TreeNode,
        config: TreeLayoutConfig
    ): Pair<List<TreeNode>, TreeBounds> {
        val positionedNodes = mutableListOf<TreeNode>()
        var currentX = 0f

        fun positionSubtree(node: TreeNode, y: Float): TreeNode {
            val childNodes = node.children.map { child ->
                positionSubtree(child, y + config.verticalSpacing)
            }

            val subtreeWidth = if (childNodes.isEmpty()) {
                config.nodeWidth
            } else {
                childNodes.sumOf {
                    (config.nodeWidth + config.horizontalSpacing).toDouble()
                }.toFloat() - config.horizontalSpacing
            }

            val nodeX = if (childNodes.isEmpty()) {
                currentX.also { currentX += config.nodeWidth + config.horizontalSpacing }
            } else {
                val firstChildX = childNodes.first().x
                val lastChildX = childNodes.last().x
                (firstChildX + lastChildX) / 2f
            }

            val positionedNode = node.copy(
                x = nodeX,
                y = y,
                children = childNodes
            )

            positionedNodes.add(positionedNode)
            return positionedNode
        }

        val rootPositioned = positionSubtree(rootNode, 0f)
        val bounds = TreeBounds.fromNodes(positionedNodes, config)

        return positionedNodes to bounds
    }
}

class GetTreeStatsUseCase @Inject constructor(
    private val memberRepository: FamilyMemberRepository
) {
    data class TreeStats(
        val memberCount: Int,
        val generations: Int
    )

    fun observe(treeId: Long): Flow<TreeStats> {
        return combine(
            memberRepository.observeMemberCount(treeId),
            memberRepository.observeMaxGeneration(treeId)
        ) { count, maxGen ->
            TreeStats(
                memberCount = count,
                generations = (maxGen ?: 0) + 1
            )
        }
    }
}

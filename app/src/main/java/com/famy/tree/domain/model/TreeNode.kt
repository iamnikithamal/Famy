package com.famy.tree.domain.model

data class TreeNode(
    val member: FamilyMember,
    val parents: List<TreeNode> = emptyList(),
    val spouses: List<FamilyMember> = emptyList(),
    val children: List<TreeNode> = emptyList(),
    val siblings: List<FamilyMember> = emptyList(),
    val x: Float = 0f,
    val y: Float = 0f,
    val depth: Int = 0,
    val isExpanded: Boolean = true
) {
    val hasParents: Boolean get() = parents.isNotEmpty()
    val hasChildren: Boolean get() = children.isNotEmpty()
    val hasSpouses: Boolean get() = spouses.isNotEmpty()
    val hasSiblings: Boolean get() = siblings.isNotEmpty()

    fun flatten(): List<TreeNode> {
        val result = mutableListOf<TreeNode>()
        result.add(this)
        children.forEach { child ->
            result.addAll(child.flatten())
        }
        return result
    }

    fun findNode(memberId: Long): TreeNode? {
        if (member.id == memberId) return this
        parents.forEach { parent ->
            parent.findNode(memberId)?.let { return it }
        }
        children.forEach { child ->
            child.findNode(memberId)?.let { return it }
        }
        return null
    }

    fun countDescendants(): Int {
        var count = 0
        children.forEach { child ->
            count += 1 + child.countDescendants()
        }
        return count
    }

    fun getMaxDepth(): Int {
        if (children.isEmpty()) return depth
        return children.maxOf { it.getMaxDepth() }
    }
}

data class TreeLayoutConfig(
    val horizontalSpacing: Float = 120f,
    val verticalSpacing: Float = 160f,
    val nodeWidth: Float = 100f,
    val nodeHeight: Float = 130f,
    val siblingSpacing: Float = 20f,
    val spouseSpacing: Float = 30f,
    val layoutType: TreeLayoutType = TreeLayoutType.VERTICAL
)

enum class TreeLayoutType {
    VERTICAL,
    HORIZONTAL,
    FAN_CHART,
    PEDIGREE
}

data class TreeBounds(
    val minX: Float,
    val minY: Float,
    val maxX: Float,
    val maxY: Float
) {
    val width: Float get() = maxX - minX
    val height: Float get() = maxY - minY
    val centerX: Float get() = (minX + maxX) / 2f
    val centerY: Float get() = (minY + maxY) / 2f

    companion object {
        val EMPTY = TreeBounds(0f, 0f, 0f, 0f)

        fun fromNodes(nodes: List<TreeNode>, config: TreeLayoutConfig): TreeBounds {
            if (nodes.isEmpty()) return EMPTY
            var minX = Float.MAX_VALUE
            var minY = Float.MAX_VALUE
            var maxX = Float.MIN_VALUE
            var maxY = Float.MIN_VALUE
            nodes.forEach { node ->
                minX = minOf(minX, node.x)
                minY = minOf(minY, node.y)
                maxX = maxOf(maxX, node.x + config.nodeWidth)
                maxY = maxOf(maxY, node.y + config.nodeHeight)
            }
            return TreeBounds(minX, minY, maxX, maxY)
        }
    }
}

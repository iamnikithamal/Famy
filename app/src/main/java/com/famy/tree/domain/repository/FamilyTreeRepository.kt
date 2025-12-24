package com.famy.tree.domain.repository

import com.famy.tree.domain.model.FamilyTree
import kotlinx.coroutines.flow.Flow

interface FamilyTreeRepository {
    fun observeAllTrees(): Flow<List<FamilyTree>>
    fun observeTree(treeId: Long): Flow<FamilyTree?>
    suspend fun getTree(treeId: Long): FamilyTree?
    suspend fun getAllTrees(): List<FamilyTree>
    suspend fun createTree(name: String, description: String? = null): FamilyTree
    suspend fun updateTree(tree: FamilyTree)
    suspend fun deleteTree(treeId: Long)
    suspend fun setRootMember(treeId: Long, memberId: Long?)
    suspend fun getTreeCount(): Int
    fun observeTreeCount(): Flow<Int>
    fun searchTrees(query: String): Flow<List<FamilyTree>>
    suspend fun getMostRecentTree(): FamilyTree?
    suspend fun touchTree(treeId: Long)
    suspend fun clearAllData()
}

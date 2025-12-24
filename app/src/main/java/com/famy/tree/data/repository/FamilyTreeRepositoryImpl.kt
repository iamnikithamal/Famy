package com.famy.tree.data.repository

import com.famy.tree.data.local.dao.FamilyMemberDao
import com.famy.tree.data.local.dao.FamilyTreeDao
import com.famy.tree.data.local.dao.LifeEventDao
import com.famy.tree.data.local.dao.MediaDao
import com.famy.tree.data.local.dao.RelationshipDao
import com.famy.tree.data.local.entity.FamilyTreeEntity
import com.famy.tree.domain.model.FamilyTree
import com.famy.tree.domain.repository.FamilyTreeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FamilyTreeRepositoryImpl @Inject constructor(
    private val familyTreeDao: FamilyTreeDao,
    private val familyMemberDao: FamilyMemberDao,
    private val relationshipDao: RelationshipDao,
    private val lifeEventDao: LifeEventDao,
    private val mediaDao: MediaDao
) : FamilyTreeRepository {

    override fun observeAllTrees(): Flow<List<FamilyTree>> {
        return familyTreeDao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun observeTree(treeId: Long): Flow<FamilyTree?> {
        return familyTreeDao.observeById(treeId).map { it?.toDomain() }
    }

    override suspend fun getTree(treeId: Long): FamilyTree? {
        return familyTreeDao.getById(treeId)?.toDomain()
    }

    override suspend fun getAllTrees(): List<FamilyTree> {
        return familyTreeDao.getAll().map { it.toDomain() }
    }

    override suspend fun createTree(name: String, description: String?): FamilyTree {
        val entity = FamilyTreeEntity(
            name = name,
            description = description
        )
        return familyTreeDao.insertAndReturn(entity).toDomain()
    }

    override suspend fun updateTree(tree: FamilyTree) {
        familyTreeDao.update(tree.toEntity())
    }

    override suspend fun deleteTree(treeId: Long) {
        familyTreeDao.deleteById(treeId)
    }

    override suspend fun setRootMember(treeId: Long, memberId: Long?) {
        familyTreeDao.updateRootMember(treeId, memberId)
    }

    override suspend fun getTreeCount(): Int {
        return familyTreeDao.getCount()
    }

    override fun observeTreeCount(): Flow<Int> {
        return familyTreeDao.observeCount()
    }

    override fun searchTrees(query: String): Flow<List<FamilyTree>> {
        return familyTreeDao.searchByName(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getMostRecentTree(): FamilyTree? {
        return familyTreeDao.getMostRecent()?.toDomain()
    }

    override suspend fun touchTree(treeId: Long) {
        familyTreeDao.touch(treeId)
    }

    override suspend fun clearAllData() {
        mediaDao.deleteAll()
        lifeEventDao.deleteAll()
        relationshipDao.deleteAll()
        familyMemberDao.deleteAll()
        familyTreeDao.deleteAll()
    }

    private fun FamilyTreeEntity.toDomain(): FamilyTree = FamilyTree(
        id = id,
        name = name,
        description = description,
        coverImagePath = coverImagePath,
        rootMemberId = rootMemberId,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun FamilyTree.toEntity(): FamilyTreeEntity = FamilyTreeEntity(
        id = id,
        name = name,
        description = description,
        coverImagePath = coverImagePath,
        rootMemberId = rootMemberId,
        createdAt = createdAt,
        updatedAt = System.currentTimeMillis()
    )
}

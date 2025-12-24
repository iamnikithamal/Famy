package com.famy.tree.data.repository

import com.famy.tree.data.local.dao.FamilyMemberDao
import com.famy.tree.data.local.dao.FamilyTreeDao
import com.famy.tree.data.local.dao.RelationshipDao
import com.famy.tree.data.local.entity.RelationshipEntity
import com.famy.tree.data.local.entity.RelationshipType
import com.famy.tree.domain.model.Relationship
import com.famy.tree.domain.model.RelationshipKind
import com.famy.tree.domain.repository.RelationshipRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RelationshipRepositoryImpl @Inject constructor(
    private val relationshipDao: RelationshipDao,
    private val familyMemberDao: FamilyMemberDao,
    private val familyTreeDao: FamilyTreeDao
) : RelationshipRepository {

    override fun observeRelationships(memberId: Long): Flow<List<Relationship>> {
        return relationshipDao.observeByMemberId(memberId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun observeAllRelationships(memberId: Long): Flow<List<Relationship>> {
        return relationshipDao.observeAllRelationships(memberId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getRelationships(memberId: Long): List<Relationship> {
        return relationshipDao.getByMemberId(memberId).map { it.toDomain() }
    }

    override suspend fun getAllRelationships(memberId: Long): List<Relationship> {
        return relationshipDao.getAllRelationships(memberId).map { it.toDomain() }
    }

    override fun observeRelationshipsByType(memberId: Long, type: RelationshipKind): Flow<List<Relationship>> {
        return relationshipDao.observeByType(memberId, type.name).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getRelationshipsByType(memberId: Long, type: RelationshipKind): List<Relationship> {
        return relationshipDao.getByType(memberId, type.name).map { it.toDomain() }
    }

    override suspend fun createRelationship(
        memberId: Long,
        relatedMemberId: Long,
        type: RelationshipKind,
        startDate: Long?,
        startPlace: String?,
        notes: String?
    ): Relationship {
        val inverseType = RelationshipType.getInverse(type.name)
        val (id, _) = relationshipDao.createBidirectionalRelationship(
            memberId = memberId,
            relatedMemberId = relatedMemberId,
            type = type.name,
            inverseType = inverseType,
            startDate = startDate,
            startPlace = startPlace,
            notes = notes
        )
        touchMemberTree(memberId)
        return relationshipDao.getById(id)!!.toDomain()
    }

    override suspend fun createRelationship(relationship: Relationship): Relationship {
        return createRelationship(
            memberId = relationship.memberId,
            relatedMemberId = relationship.relatedMemberId,
            type = relationship.type,
            startDate = relationship.startDate,
            startPlace = relationship.startPlace,
            notes = relationship.notes
        )
    }

    override suspend fun deleteRelationship(relationshipId: Long) {
        val relationship = relationshipDao.getById(relationshipId) ?: return
        val inverseType = RelationshipType.getInverse(relationship.relationshipType)
        relationshipDao.deleteBidirectionalRelationship(
            memberId = relationship.memberId,
            relatedMemberId = relationship.relatedMemberId,
            type = relationship.relationshipType,
            inverseType = inverseType
        )
        touchMemberTree(relationship.memberId)
    }

    override suspend fun deleteAllRelationships(memberId: Long) {
        relationshipDao.deleteByMemberId(memberId)
        touchMemberTree(memberId)
    }

    override suspend fun relationshipExists(memberId: Long, relatedMemberId: Long, type: RelationshipKind): Boolean {
        return relationshipDao.exists(memberId, relatedMemberId, type.name)
    }

    override suspend fun getRelationshipBetween(memberId: Long, relatedMemberId: Long): List<Relationship> {
        return relationshipDao.getRelationshipBetween(memberId, relatedMemberId).map { it.toDomain() }
    }

    override suspend fun getParentIds(memberId: Long): List<Long> {
        return relationshipDao.getParentIds(memberId)
    }

    override suspend fun getChildIds(memberId: Long): List<Long> {
        return relationshipDao.getChildIds(memberId)
    }

    override suspend fun getSpouseIds(memberId: Long): List<Long> {
        return relationshipDao.getSpouseIds(memberId)
    }

    override suspend fun getSiblingIds(memberId: Long): List<Long> {
        return relationshipDao.getSiblingIds(memberId)
    }

    override fun observeRelationshipsByTree(treeId: Long): Flow<List<Relationship>> {
        return relationshipDao.observeByTreeId(treeId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getRelationshipsByTree(treeId: Long): List<Relationship> {
        return relationshipDao.getByTreeId(treeId).map { it.toDomain() }
    }

    override suspend fun getRelationshipCount(memberId: Long): Int {
        return relationshipDao.getCountByMember(memberId)
    }

    override suspend fun getRelationshipCountByType(memberId: Long, type: RelationshipKind): Int {
        return relationshipDao.getCountByType(memberId, type.name)
    }

    private suspend fun touchMemberTree(memberId: Long) {
        familyMemberDao.getById(memberId)?.let { member ->
            familyTreeDao.touch(member.treeId)
        }
    }

    private fun RelationshipEntity.toDomain(): Relationship = Relationship(
        id = id,
        memberId = memberId,
        relatedMemberId = relatedMemberId,
        type = RelationshipKind.fromString(relationshipType),
        startDate = startDate,
        endDate = endDate,
        startPlace = startPlace,
        notes = notes,
        createdAt = createdAt
    )
}

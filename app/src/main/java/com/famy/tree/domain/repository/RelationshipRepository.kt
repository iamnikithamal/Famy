package com.famy.tree.domain.repository

import com.famy.tree.domain.model.Relationship
import com.famy.tree.domain.model.RelationshipKind
import kotlinx.coroutines.flow.Flow

interface RelationshipRepository {
    fun observeRelationships(memberId: Long): Flow<List<Relationship>>
    fun observeAllRelationships(memberId: Long): Flow<List<Relationship>>
    suspend fun getRelationships(memberId: Long): List<Relationship>
    suspend fun getAllRelationships(memberId: Long): List<Relationship>
    fun observeRelationshipsByType(memberId: Long, type: RelationshipKind): Flow<List<Relationship>>
    suspend fun getRelationshipsByType(memberId: Long, type: RelationshipKind): List<Relationship>
    suspend fun createRelationship(
        memberId: Long,
        relatedMemberId: Long,
        type: RelationshipKind,
        startDate: Long? = null,
        startPlace: String? = null,
        notes: String? = null
    ): Relationship
    suspend fun createRelationship(relationship: Relationship): Relationship
    suspend fun deleteRelationship(relationshipId: Long)
    suspend fun deleteAllRelationships(memberId: Long)
    suspend fun relationshipExists(memberId: Long, relatedMemberId: Long, type: RelationshipKind): Boolean
    suspend fun getRelationshipBetween(memberId: Long, relatedMemberId: Long): List<Relationship>
    suspend fun getParentIds(memberId: Long): List<Long>
    suspend fun getChildIds(memberId: Long): List<Long>
    suspend fun getSpouseIds(memberId: Long): List<Long>
    suspend fun getSiblingIds(memberId: Long): List<Long>
    fun observeRelationshipsByTree(treeId: Long): Flow<List<Relationship>>
    suspend fun getRelationshipsByTree(treeId: Long): List<Relationship>
    suspend fun getRelationshipCount(memberId: Long): Int
    suspend fun getRelationshipCountByType(memberId: Long, type: RelationshipKind): Int
}

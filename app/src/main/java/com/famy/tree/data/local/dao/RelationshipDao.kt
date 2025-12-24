package com.famy.tree.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.famy.tree.data.local.entity.RelationshipEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RelationshipDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(relationship: RelationshipEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(relationships: List<RelationshipEntity>): List<Long>

    @Delete
    suspend fun delete(relationship: RelationshipEntity)

    @Query("DELETE FROM relationships WHERE id = :relationshipId")
    suspend fun deleteById(relationshipId: Long)

    @Query("DELETE FROM relationships WHERE member_id = :memberId OR related_member_id = :memberId")
    suspend fun deleteByMemberId(memberId: Long)

    @Query("DELETE FROM relationships")
    suspend fun deleteAll()

    @Query("SELECT * FROM relationships WHERE id = :id")
    suspend fun getById(id: Long): RelationshipEntity?

    @Query("SELECT * FROM relationships WHERE member_id = :memberId")
    fun observeByMemberId(memberId: Long): Flow<List<RelationshipEntity>>

    @Query("SELECT * FROM relationships WHERE member_id = :memberId")
    suspend fun getByMemberId(memberId: Long): List<RelationshipEntity>

    @Query("SELECT * FROM relationships WHERE member_id = :memberId OR related_member_id = :memberId")
    fun observeAllRelationships(memberId: Long): Flow<List<RelationshipEntity>>

    @Query("SELECT * FROM relationships WHERE member_id = :memberId OR related_member_id = :memberId")
    suspend fun getAllRelationships(memberId: Long): List<RelationshipEntity>

    @Query("""
        SELECT * FROM relationships
        WHERE member_id = :memberId AND relationship_type = :type
    """)
    fun observeByType(memberId: Long, type: String): Flow<List<RelationshipEntity>>

    @Query("""
        SELECT * FROM relationships
        WHERE member_id = :memberId AND relationship_type = :type
    """)
    suspend fun getByType(memberId: Long, type: String): List<RelationshipEntity>

    @Query("""
        SELECT * FROM relationships
        WHERE (member_id = :memberId AND related_member_id = :relatedMemberId)
           OR (member_id = :relatedMemberId AND related_member_id = :memberId)
    """)
    suspend fun getRelationshipBetween(memberId: Long, relatedMemberId: Long): List<RelationshipEntity>

    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM relationships
            WHERE member_id = :memberId
            AND related_member_id = :relatedMemberId
            AND relationship_type = :type
        )
    """)
    suspend fun exists(memberId: Long, relatedMemberId: Long, type: String): Boolean

    @Query("""
        SELECT r.* FROM relationships r
        INNER JOIN family_members m ON r.member_id = m.id OR r.related_member_id = m.id
        WHERE m.tree_id = :treeId
        GROUP BY r.id
    """)
    fun observeByTreeId(treeId: Long): Flow<List<RelationshipEntity>>

    @Query("""
        SELECT r.* FROM relationships r
        INNER JOIN family_members m ON r.member_id = m.id
        WHERE m.tree_id = :treeId
    """)
    suspend fun getByTreeId(treeId: Long): List<RelationshipEntity>

    @Query("SELECT COUNT(*) FROM relationships WHERE member_id = :memberId")
    suspend fun getCountByMember(memberId: Long): Int

    @Query("""
        SELECT COUNT(*) FROM relationships
        WHERE member_id = :memberId AND relationship_type = :type
    """)
    suspend fun getCountByType(memberId: Long, type: String): Int

    @Query("""
        SELECT related_member_id FROM relationships
        WHERE member_id = :memberId AND relationship_type = 'PARENT'
    """)
    suspend fun getParentIds(memberId: Long): List<Long>

    @Query("""
        SELECT related_member_id FROM relationships
        WHERE member_id = :memberId AND relationship_type = 'CHILD'
    """)
    suspend fun getChildIds(memberId: Long): List<Long>

    @Query("""
        SELECT related_member_id FROM relationships
        WHERE member_id = :memberId AND relationship_type = 'SPOUSE'
    """)
    suspend fun getSpouseIds(memberId: Long): List<Long>

    @Query("""
        SELECT related_member_id FROM relationships
        WHERE member_id = :memberId AND relationship_type = 'SIBLING'
    """)
    suspend fun getSiblingIds(memberId: Long): List<Long>

    @Transaction
    suspend fun createBidirectionalRelationship(
        memberId: Long,
        relatedMemberId: Long,
        type: String,
        inverseType: String,
        startDate: Long? = null,
        startPlace: String? = null,
        notes: String? = null
    ): Pair<Long, Long> {
        val rel1 = insert(RelationshipEntity(
            memberId = memberId,
            relatedMemberId = relatedMemberId,
            relationshipType = type,
            startDate = startDate,
            startPlace = startPlace,
            notes = notes
        ))
        val rel2 = insert(RelationshipEntity(
            memberId = relatedMemberId,
            relatedMemberId = memberId,
            relationshipType = inverseType,
            startDate = startDate,
            startPlace = startPlace,
            notes = notes
        ))
        return rel1 to rel2
    }

    @Transaction
    suspend fun deleteBidirectionalRelationship(memberId: Long, relatedMemberId: Long, type: String, inverseType: String) {
        deleteRelationship(memberId, relatedMemberId, type)
        deleteRelationship(relatedMemberId, memberId, inverseType)
    }

    @Query("""
        DELETE FROM relationships
        WHERE member_id = :memberId
        AND related_member_id = :relatedMemberId
        AND relationship_type = :type
    """)
    suspend fun deleteRelationship(memberId: Long, relatedMemberId: Long, type: String)
}

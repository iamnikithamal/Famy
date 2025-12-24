package com.famy.tree.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.famy.tree.data.local.entity.LifeEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LifeEventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: LifeEventEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<LifeEventEntity>): List<Long>

    @Update
    suspend fun update(event: LifeEventEntity)

    @Delete
    suspend fun delete(event: LifeEventEntity)

    @Query("DELETE FROM life_events WHERE id = :eventId")
    suspend fun deleteById(eventId: Long)

    @Query("DELETE FROM life_events WHERE member_id = :memberId")
    suspend fun deleteByMemberId(memberId: Long)

    @Query("DELETE FROM life_events")
    suspend fun deleteAll()

    @Query("SELECT * FROM life_events WHERE id = :id")
    suspend fun getById(id: Long): LifeEventEntity?

    @Query("SELECT * FROM life_events WHERE member_id = :memberId ORDER BY event_date ASC")
    fun observeByMemberId(memberId: Long): Flow<List<LifeEventEntity>>

    @Query("SELECT * FROM life_events WHERE member_id = :memberId ORDER BY event_date ASC")
    suspend fun getByMemberId(memberId: Long): List<LifeEventEntity>

    @Query("SELECT * FROM life_events WHERE member_id = :memberId AND event_type = :eventType ORDER BY event_date ASC")
    fun observeByType(memberId: Long, eventType: String): Flow<List<LifeEventEntity>>

    @Query("""
        SELECT e.* FROM life_events e
        INNER JOIN family_members m ON e.member_id = m.id
        WHERE m.tree_id = :treeId
        ORDER BY e.event_date ASC
    """)
    fun observeByTreeId(treeId: Long): Flow<List<LifeEventEntity>>

    @Query("""
        SELECT e.* FROM life_events e
        INNER JOIN family_members m ON e.member_id = m.id
        WHERE m.tree_id = :treeId
        ORDER BY e.event_date ASC
    """)
    suspend fun getByTreeId(treeId: Long): List<LifeEventEntity>

    @Query("""
        SELECT e.* FROM life_events e
        INNER JOIN family_members m ON e.member_id = m.id
        WHERE m.tree_id = :treeId AND e.event_type = :eventType
        ORDER BY e.event_date ASC
    """)
    fun observeByTreeAndType(treeId: Long, eventType: String): Flow<List<LifeEventEntity>>

    @Query("""
        SELECT e.* FROM life_events e
        INNER JOIN family_members m ON e.member_id = m.id
        WHERE m.tree_id = :treeId
        AND e.event_date BETWEEN :startDate AND :endDate
        ORDER BY e.event_date ASC
    """)
    fun observeByDateRange(treeId: Long, startDate: Long, endDate: Long): Flow<List<LifeEventEntity>>

    @Query("SELECT COUNT(*) FROM life_events WHERE member_id = :memberId")
    suspend fun getCountByMember(memberId: Long): Int

    @Query("""
        SELECT COUNT(*) FROM life_events e
        INNER JOIN family_members m ON e.member_id = m.id
        WHERE m.tree_id = :treeId
    """)
    suspend fun getCountByTree(treeId: Long): Int
}

package com.famy.tree.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.famy.tree.data.local.entity.FamilyMemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FamilyMemberDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(member: FamilyMemberEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(members: List<FamilyMemberEntity>): List<Long>

    @Update
    suspend fun update(member: FamilyMemberEntity)

    @Delete
    suspend fun delete(member: FamilyMemberEntity)

    @Query("DELETE FROM family_members WHERE id = :memberId")
    suspend fun deleteById(memberId: Long)

    @Query("DELETE FROM family_members WHERE tree_id = :treeId")
    suspend fun deleteByTreeId(treeId: Long)

    @Query("DELETE FROM family_members")
    suspend fun deleteAll()

    @Query("SELECT * FROM family_members WHERE id = :id")
    suspend fun getById(id: Long): FamilyMemberEntity?

    @Query("SELECT * FROM family_members WHERE id = :id")
    fun observeById(id: Long): Flow<FamilyMemberEntity?>

    @Query("SELECT * FROM family_members WHERE tree_id = :treeId ORDER BY last_name, first_name")
    fun observeByTreeId(treeId: Long): Flow<List<FamilyMemberEntity>>

    @Query("SELECT * FROM family_members WHERE tree_id = :treeId ORDER BY last_name, first_name")
    suspend fun getByTreeId(treeId: Long): List<FamilyMemberEntity>

    @Query("SELECT * FROM family_members ORDER BY updated_at DESC LIMIT :limit")
    fun observeRecent(limit: Int = 10): Flow<List<FamilyMemberEntity>>

    @Query("SELECT * FROM family_members WHERE tree_id = :treeId ORDER BY updated_at DESC LIMIT :limit")
    fun observeRecentByTree(treeId: Long, limit: Int = 10): Flow<List<FamilyMemberEntity>>

    @Query("""
        SELECT * FROM family_members
        WHERE tree_id = :treeId
        AND (first_name LIKE '%' || :query || '%'
             OR last_name LIKE '%' || :query || '%'
             OR maiden_name LIKE '%' || :query || '%'
             OR nickname LIKE '%' || :query || '%')
        ORDER BY last_name, first_name
    """)
    fun searchInTree(treeId: Long, query: String): Flow<List<FamilyMemberEntity>>

    @Query("""
        SELECT * FROM family_members
        WHERE (first_name LIKE '%' || :query || '%'
               OR last_name LIKE '%' || :query || '%'
               OR maiden_name LIKE '%' || :query || '%'
               OR nickname LIKE '%' || :query || '%')
        ORDER BY last_name, first_name
    """)
    fun searchAll(query: String): Flow<List<FamilyMemberEntity>>

    @Query("SELECT * FROM family_members WHERE tree_id = :treeId AND is_living = :isLiving ORDER BY last_name, first_name")
    fun getByLivingStatus(treeId: Long, isLiving: Boolean): Flow<List<FamilyMemberEntity>>

    @Query("SELECT * FROM family_members WHERE tree_id = :treeId AND generation = :generation ORDER BY last_name, first_name")
    fun getByGeneration(treeId: Long, generation: Int): Flow<List<FamilyMemberEntity>>

    @Query("SELECT * FROM family_members WHERE tree_id = :treeId AND gender = :gender ORDER BY last_name, first_name")
    fun getByGender(treeId: Long, gender: String): Flow<List<FamilyMemberEntity>>

    @Query("SELECT * FROM family_members WHERE tree_id = :treeId AND paternal_line = :paternalLine ORDER BY last_name, first_name")
    fun getByLineage(treeId: Long, paternalLine: Boolean): Flow<List<FamilyMemberEntity>>

    @Query("""
        SELECT * FROM family_members
        WHERE tree_id = :treeId
        AND birth_date BETWEEN :startDate AND :endDate
        ORDER BY birth_date
    """)
    fun getByBirthDateRange(treeId: Long, startDate: Long, endDate: Long): Flow<List<FamilyMemberEntity>>

    @Query("SELECT COUNT(*) FROM family_members WHERE tree_id = :treeId")
    suspend fun getCountByTree(treeId: Long): Int

    @Query("SELECT COUNT(*) FROM family_members WHERE tree_id = :treeId")
    fun observeCountByTree(treeId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM family_members")
    suspend fun getTotalCount(): Int

    @Query("SELECT COUNT(*) FROM family_members")
    fun observeTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM family_members WHERE tree_id = :treeId AND is_living = :isLiving")
    suspend fun getCountByLivingStatus(treeId: Long, isLiving: Boolean): Int

    @Query("SELECT COUNT(*) FROM family_members WHERE tree_id = :treeId AND gender = :gender")
    suspend fun getCountByGender(treeId: Long, gender: String): Int

    @Query("SELECT MAX(generation) FROM family_members WHERE tree_id = :treeId")
    suspend fun getMaxGeneration(treeId: Long): Int?

    @Query("SELECT MAX(generation) FROM family_members WHERE tree_id = :treeId")
    fun observeMaxGeneration(treeId: Long): Flow<Int?>

    @Query("SELECT MIN(generation) FROM family_members WHERE tree_id = :treeId")
    suspend fun getMinGeneration(treeId: Long): Int?

    @Query("UPDATE family_members SET updated_at = :timestamp WHERE id = :memberId")
    suspend fun touch(memberId: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE family_members SET generation = :generation WHERE id = :memberId")
    suspend fun updateGeneration(memberId: Long, generation: Int)

    @Query("UPDATE family_members SET photo_path = :photoPath, updated_at = :timestamp WHERE id = :memberId")
    suspend fun updatePhoto(memberId: Long, photoPath: String?, timestamp: Long = System.currentTimeMillis())

    @Transaction
    suspend fun insertAndReturn(member: FamilyMemberEntity): FamilyMemberEntity {
        val id = insert(member)
        return getById(id)!!
    }
}

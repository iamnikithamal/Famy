package com.famy.tree.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.famy.tree.data.local.entity.FamilyTreeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FamilyTreeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tree: FamilyTreeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(trees: List<FamilyTreeEntity>): List<Long>

    @Update
    suspend fun update(tree: FamilyTreeEntity)

    @Delete
    suspend fun delete(tree: FamilyTreeEntity)

    @Query("DELETE FROM family_trees WHERE id = :treeId")
    suspend fun deleteById(treeId: Long)

    @Query("DELETE FROM family_trees")
    suspend fun deleteAll()

    @Query("SELECT * FROM family_trees WHERE id = :id")
    suspend fun getById(id: Long): FamilyTreeEntity?

    @Query("SELECT * FROM family_trees WHERE id = :id")
    fun observeById(id: Long): Flow<FamilyTreeEntity?>

    @Query("SELECT * FROM family_trees ORDER BY updated_at DESC")
    fun observeAll(): Flow<List<FamilyTreeEntity>>

    @Query("SELECT * FROM family_trees ORDER BY updated_at DESC")
    suspend fun getAll(): List<FamilyTreeEntity>

    @Query("SELECT * FROM family_trees ORDER BY updated_at DESC LIMIT 1")
    suspend fun getMostRecent(): FamilyTreeEntity?

    @Query("SELECT * FROM family_trees WHERE name LIKE '%' || :query || '%' ORDER BY name")
    fun searchByName(query: String): Flow<List<FamilyTreeEntity>>

    @Query("SELECT COUNT(*) FROM family_trees")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM family_trees")
    fun observeCount(): Flow<Int>

    @Query("UPDATE family_trees SET root_member_id = :rootMemberId, updated_at = :timestamp WHERE id = :treeId")
    suspend fun updateRootMember(treeId: Long, rootMemberId: Long?, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE family_trees SET updated_at = :timestamp WHERE id = :treeId")
    suspend fun touch(treeId: Long, timestamp: Long = System.currentTimeMillis())

    @Transaction
    suspend fun insertAndReturn(tree: FamilyTreeEntity): FamilyTreeEntity {
        val id = insert(tree)
        return getById(id)!!
    }
}

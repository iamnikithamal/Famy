package com.famy.tree.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.famy.tree.data.local.entity.MediaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(media: MediaEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(media: List<MediaEntity>): List<Long>

    @Update
    suspend fun update(media: MediaEntity)

    @Delete
    suspend fun delete(media: MediaEntity)

    @Query("DELETE FROM media WHERE id = :mediaId")
    suspend fun deleteById(mediaId: Long)

    @Query("DELETE FROM media WHERE member_id = :memberId")
    suspend fun deleteByMemberId(memberId: Long)

    @Query("DELETE FROM media")
    suspend fun deleteAll()

    @Query("SELECT * FROM media WHERE id = :id")
    suspend fun getById(id: Long): MediaEntity?

    @Query("SELECT * FROM media WHERE member_id = :memberId ORDER BY created_at DESC")
    fun observeByMemberId(memberId: Long): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media WHERE member_id = :memberId ORDER BY created_at DESC")
    suspend fun getByMemberId(memberId: Long): List<MediaEntity>

    @Query("SELECT * FROM media WHERE member_id = :memberId AND media_type = :mediaType ORDER BY created_at DESC")
    fun observeByType(memberId: Long, mediaType: String): Flow<List<MediaEntity>>

    @Query("""
        SELECT med.* FROM media med
        INNER JOIN family_members m ON med.member_id = m.id
        WHERE m.tree_id = :treeId
        ORDER BY med.created_at DESC
    """)
    fun observeByTreeId(treeId: Long): Flow<List<MediaEntity>>

    @Query("""
        SELECT med.* FROM media med
        INNER JOIN family_members m ON med.member_id = m.id
        WHERE m.tree_id = :treeId
        ORDER BY med.created_at DESC
    """)
    suspend fun getByTreeId(treeId: Long): List<MediaEntity>

    @Query("""
        SELECT med.* FROM media med
        INNER JOIN family_members m ON med.member_id = m.id
        WHERE m.tree_id = :treeId AND med.media_type = :mediaType
        ORDER BY med.created_at DESC
    """)
    fun observeByTreeAndType(treeId: Long, mediaType: String): Flow<List<MediaEntity>>

    @Query("""
        SELECT med.* FROM media med
        INNER JOIN family_members m ON med.member_id = m.id
        WHERE m.tree_id = :treeId AND med.media_type = 'PHOTO'
        ORDER BY med.created_at DESC
    """)
    fun observePhotosByTree(treeId: Long): Flow<List<MediaEntity>>

    @Query("SELECT COUNT(*) FROM media WHERE member_id = :memberId")
    suspend fun getCountByMember(memberId: Long): Int

    @Query("""
        SELECT COUNT(*) FROM media med
        INNER JOIN family_members m ON med.member_id = m.id
        WHERE m.tree_id = :treeId
    """)
    suspend fun getCountByTree(treeId: Long): Int

    @Query("SELECT SUM(file_size) FROM media WHERE member_id = :memberId")
    suspend fun getTotalSizeByMember(memberId: Long): Long?

    @Query("""
        SELECT SUM(med.file_size) FROM media med
        INNER JOIN family_members m ON med.member_id = m.id
        WHERE m.tree_id = :treeId
    """)
    suspend fun getTotalSizeByTree(treeId: Long): Long?

    @Query("SELECT file_path FROM media WHERE member_id = :memberId")
    suspend fun getFilePathsByMember(memberId: Long): List<String>

    @Query("SELECT file_path FROM media")
    suspend fun getAllFilePaths(): List<String>
}

package com.famy.tree.domain.repository

import com.famy.tree.domain.model.Media
import com.famy.tree.domain.model.MediaKind
import kotlinx.coroutines.flow.Flow
import java.io.InputStream

interface MediaRepository {
    fun observeMedia(memberId: Long): Flow<List<Media>>
    suspend fun getMedia(memberId: Long): List<Media>
    suspend fun getMediaById(mediaId: Long): Media?
    fun observeMediaByType(memberId: Long, type: MediaKind): Flow<List<Media>>
    suspend fun addMedia(
        memberId: Long,
        inputStream: InputStream,
        fileName: String,
        mimeType: String?,
        title: String? = null,
        description: String? = null,
        dateTaken: Long? = null
    ): Media
    suspend fun updateMedia(media: Media)
    suspend fun deleteMedia(mediaId: Long)
    suspend fun deleteAllMedia(memberId: Long)
    fun observeMediaByTree(treeId: Long): Flow<List<Media>>
    suspend fun getMediaByTree(treeId: Long): List<Media>
    fun observePhotosByTree(treeId: Long): Flow<List<Media>>
    suspend fun getMediaCount(memberId: Long): Int
    suspend fun getMediaCountByTree(treeId: Long): Int
    suspend fun getTotalMediaSize(memberId: Long): Long
    suspend fun getTotalMediaSizeByTree(treeId: Long): Long
    suspend fun cleanupOrphanedFiles()
}

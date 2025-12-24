package com.famy.tree.data.repository

import android.content.Context
import com.famy.tree.data.local.dao.FamilyMemberDao
import com.famy.tree.data.local.dao.FamilyTreeDao
import com.famy.tree.data.local.dao.MediaDao
import com.famy.tree.data.local.entity.MediaEntity
import com.famy.tree.data.local.entity.MediaType
import com.famy.tree.domain.model.Media
import com.famy.tree.domain.model.MediaKind
import com.famy.tree.domain.repository.MediaRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaDao: MediaDao,
    private val familyMemberDao: FamilyMemberDao,
    private val familyTreeDao: FamilyTreeDao
) : MediaRepository {

    private val mediaDir: File
        get() = File(context.filesDir, "media").also { if (!it.exists()) it.mkdirs() }

    override fun observeMedia(memberId: Long): Flow<List<Media>> {
        return mediaDao.observeByMemberId(memberId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getMedia(memberId: Long): List<Media> {
        return mediaDao.getByMemberId(memberId).map { it.toDomain() }
    }

    override suspend fun getMediaById(mediaId: Long): Media? {
        return mediaDao.getById(mediaId)?.toDomain()
    }

    override fun observeMediaByType(memberId: Long, type: MediaKind): Flow<List<Media>> {
        return mediaDao.observeByType(memberId, type.name).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun addMedia(
        memberId: Long,
        inputStream: InputStream,
        fileName: String,
        mimeType: String?,
        title: String?,
        description: String?,
        dateTaken: Long?
    ): Media {
        val mediaType = MediaType.fromMimeType(mimeType)
        val extension = fileName.substringAfterLast('.', "")
        val uniqueName = "${UUID.randomUUID()}.$extension"
        val file = File(mediaDir, uniqueName)

        val fileSize = withContext(Dispatchers.IO) {
            inputStream.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            file.length()
        }

        val entity = MediaEntity(
            memberId = memberId,
            filePath = file.absolutePath,
            mediaType = mediaType,
            title = title,
            description = description,
            dateTaken = dateTaken,
            fileSize = fileSize,
            mimeType = mimeType
        )

        val id = mediaDao.insert(entity)
        touchMemberTree(memberId)
        return mediaDao.getById(id)!!.toDomain()
    }

    override suspend fun updateMedia(media: Media) {
        mediaDao.update(media.toEntity())
        touchMemberTree(media.memberId)
    }

    override suspend fun deleteMedia(mediaId: Long) {
        val media = mediaDao.getById(mediaId) ?: return
        withContext(Dispatchers.IO) {
            File(media.filePath).delete()
            media.thumbnailPath?.let { File(it).delete() }
        }
        mediaDao.deleteById(mediaId)
        touchMemberTree(media.memberId)
    }

    override suspend fun deleteAllMedia(memberId: Long) {
        val paths = mediaDao.getFilePathsByMember(memberId)
        withContext(Dispatchers.IO) {
            paths.forEach { File(it).delete() }
        }
        mediaDao.deleteByMemberId(memberId)
        touchMemberTree(memberId)
    }

    override fun observeMediaByTree(treeId: Long): Flow<List<Media>> {
        return mediaDao.observeByTreeId(treeId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getMediaByTree(treeId: Long): List<Media> {
        return mediaDao.getByTreeId(treeId).map { it.toDomain() }
    }

    override fun observePhotosByTree(treeId: Long): Flow<List<Media>> {
        return mediaDao.observePhotosByTree(treeId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getMediaCount(memberId: Long): Int {
        return mediaDao.getCountByMember(memberId)
    }

    override suspend fun getMediaCountByTree(treeId: Long): Int {
        return mediaDao.getCountByTree(treeId)
    }

    override suspend fun getTotalMediaSize(memberId: Long): Long {
        return mediaDao.getTotalSizeByMember(memberId) ?: 0
    }

    override suspend fun getTotalMediaSizeByTree(treeId: Long): Long {
        return mediaDao.getTotalSizeByTree(treeId) ?: 0
    }

    override suspend fun cleanupOrphanedFiles() {
        val dbPaths = mediaDao.getAllFilePaths().toSet()
        withContext(Dispatchers.IO) {
            mediaDir.listFiles()?.forEach { file ->
                if (file.absolutePath !in dbPaths) {
                    file.delete()
                }
            }
        }
    }

    private suspend fun touchMemberTree(memberId: Long) {
        familyMemberDao.getById(memberId)?.let { member ->
            familyTreeDao.touch(member.treeId)
        }
    }

    private fun MediaEntity.toDomain(): Media = Media(
        id = id,
        memberId = memberId,
        filePath = filePath,
        type = MediaKind.fromString(mediaType),
        title = title,
        description = description,
        dateTaken = dateTaken,
        fileSize = fileSize,
        mimeType = mimeType,
        thumbnailPath = thumbnailPath,
        createdAt = createdAt
    )

    private fun Media.toEntity(): MediaEntity = MediaEntity(
        id = id,
        memberId = memberId,
        filePath = filePath,
        mediaType = type.name,
        title = title,
        description = description,
        dateTaken = dateTaken,
        fileSize = fileSize,
        mimeType = mimeType,
        thumbnailPath = thumbnailPath,
        createdAt = createdAt
    )
}

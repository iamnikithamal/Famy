package com.famy.tree.data.repository

import com.famy.tree.data.local.dao.FamilyMemberDao
import com.famy.tree.data.local.dao.FamilyTreeDao
import com.famy.tree.data.local.dao.RelationshipDao
import com.famy.tree.data.local.entity.FamilyMemberEntity
import com.famy.tree.domain.model.CareerStatus
import com.famy.tree.domain.model.FamilyMember
import com.famy.tree.domain.model.Gender
import com.famy.tree.domain.model.RelationshipStatus
import com.famy.tree.domain.repository.FamilyMemberRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FamilyMemberRepositoryImpl @Inject constructor(
    private val familyMemberDao: FamilyMemberDao,
    private val familyTreeDao: FamilyTreeDao,
    private val relationshipDao: RelationshipDao
) : FamilyMemberRepository {

    override fun observeMembersByTree(treeId: Long): Flow<List<FamilyMember>> {
        return familyMemberDao.observeByTreeId(treeId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun observeMember(memberId: Long): Flow<FamilyMember?> {
        return familyMemberDao.observeById(memberId).map { it?.toDomain() }
    }

    override suspend fun getMember(memberId: Long): FamilyMember? {
        return familyMemberDao.getById(memberId)?.toDomain()
    }

    override suspend fun getMembersByTree(treeId: Long): List<FamilyMember> {
        return familyMemberDao.getByTreeId(treeId).map { it.toDomain() }
    }

    override suspend fun createMember(member: FamilyMember): FamilyMember {
        val entity = member.toEntity()
        val inserted = familyMemberDao.insertAndReturn(entity)
        familyTreeDao.touch(member.treeId)
        return inserted.toDomain()
    }

    override suspend fun updateMember(member: FamilyMember) {
        familyMemberDao.update(member.toEntity())
        familyTreeDao.touch(member.treeId)
    }

    override suspend fun deleteMember(memberId: Long) {
        val member = familyMemberDao.getById(memberId) ?: return
        familyMemberDao.deleteById(memberId)
        familyTreeDao.touch(member.treeId)
    }

    override fun searchMembers(treeId: Long, query: String): Flow<List<FamilyMember>> {
        return familyMemberDao.searchInTree(treeId, query).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun searchAllMembers(query: String): Flow<List<FamilyMember>> {
        return familyMemberDao.searchAll(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun observeRecentMembers(limit: Int): Flow<List<FamilyMember>> {
        return familyMemberDao.observeRecent(limit).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun observeRecentMembersByTree(treeId: Long, limit: Int): Flow<List<FamilyMember>> {
        return familyMemberDao.observeRecentByTree(treeId, limit).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getMembersByLivingStatus(treeId: Long, isLiving: Boolean): Flow<List<FamilyMember>> {
        return familyMemberDao.getByLivingStatus(treeId, isLiving).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getMembersByGeneration(treeId: Long, generation: Int): Flow<List<FamilyMember>> {
        return familyMemberDao.getByGeneration(treeId, generation).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getMembersByGender(treeId: Long, gender: Gender): Flow<List<FamilyMember>> {
        return familyMemberDao.getByGender(treeId, gender.name).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getMembersByLineage(treeId: Long, paternalLine: Boolean): Flow<List<FamilyMember>> {
        return familyMemberDao.getByLineage(treeId, paternalLine).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getMembersByBirthDateRange(
        treeId: Long,
        startDate: Long,
        endDate: Long
    ): Flow<List<FamilyMember>> {
        return familyMemberDao.getByBirthDateRange(treeId, startDate, endDate).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getMemberCount(treeId: Long): Int {
        return familyMemberDao.getCountByTree(treeId)
    }

    override fun observeMemberCount(treeId: Long): Flow<Int> {
        return familyMemberDao.observeCountByTree(treeId)
    }

    override suspend fun getTotalMemberCount(): Int {
        return familyMemberDao.getTotalCount()
    }

    override fun observeTotalMemberCount(): Flow<Int> {
        return familyMemberDao.observeTotalCount()
    }

    override suspend fun getLivingCount(treeId: Long): Int {
        return familyMemberDao.getCountByLivingStatus(treeId, true)
    }

    override suspend fun getDeceasedCount(treeId: Long): Int {
        return familyMemberDao.getCountByLivingStatus(treeId, false)
    }

    override suspend fun getGenderCount(treeId: Long, gender: Gender): Int {
        return familyMemberDao.getCountByGender(treeId, gender.name)
    }

    override suspend fun getMaxGeneration(treeId: Long): Int {
        return familyMemberDao.getMaxGeneration(treeId) ?: 0
    }

    override fun observeMaxGeneration(treeId: Long): Flow<Int?> {
        return familyMemberDao.observeMaxGeneration(treeId)
    }

    override suspend fun getMinGeneration(treeId: Long): Int {
        return familyMemberDao.getMinGeneration(treeId) ?: 0
    }

    override suspend fun updateMemberPhoto(memberId: Long, photoPath: String?) {
        familyMemberDao.updatePhoto(memberId, photoPath)
        familyMemberDao.getById(memberId)?.let { member ->
            familyTreeDao.touch(member.treeId)
        }
    }

    override suspend fun updateMemberGeneration(memberId: Long, generation: Int) {
        familyMemberDao.updateGeneration(memberId, generation)
    }

    private fun FamilyMemberEntity.toDomain(): FamilyMember = FamilyMember(
        id = id,
        treeId = treeId,
        firstName = firstName,
        middleName = middleName,
        lastName = lastName,
        maidenName = maidenName,
        nickname = nickname,
        gender = Gender.fromString(gender),
        photoPath = photoPath,
        birthDate = birthDate,
        birthPlace = birthPlace,
        birthPlaceLatitude = birthPlaceLatitude,
        birthPlaceLongitude = birthPlaceLongitude,
        deathDate = deathDate,
        deathPlace = deathPlace,
        deathPlaceLatitude = deathPlaceLatitude,
        deathPlaceLongitude = deathPlaceLongitude,
        isLiving = isLiving,
        biography = biography,
        occupation = occupation,
        education = education,
        interests = parseInterests(interests),
        careerStatus = CareerStatus.fromString(careerStatus),
        relationshipStatus = RelationshipStatus.fromString(relationshipStatus),
        religion = religion,
        nationality = nationality,
        notes = notes,
        customFields = parseCustomFields(customFields),
        generation = generation,
        paternalLine = paternalLine,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun FamilyMember.toEntity(): FamilyMemberEntity = FamilyMemberEntity(
        id = id,
        treeId = treeId,
        firstName = firstName,
        middleName = middleName,
        lastName = lastName,
        maidenName = maidenName,
        nickname = nickname,
        gender = gender.name,
        photoPath = photoPath,
        birthDate = birthDate,
        birthPlace = birthPlace,
        birthPlaceLatitude = birthPlaceLatitude,
        birthPlaceLongitude = birthPlaceLongitude,
        deathDate = deathDate,
        deathPlace = deathPlace,
        deathPlaceLatitude = deathPlaceLatitude,
        deathPlaceLongitude = deathPlaceLongitude,
        isLiving = isLiving,
        biography = biography,
        occupation = occupation,
        education = education,
        interests = serializeInterests(interests),
        careerStatus = careerStatus.name,
        relationshipStatus = relationshipStatus.name,
        religion = religion,
        nationality = nationality,
        notes = notes,
        customFields = serializeCustomFields(customFields),
        generation = generation,
        paternalLine = paternalLine,
        createdAt = createdAt,
        updatedAt = System.currentTimeMillis()
    )

    private fun parseCustomFields(json: String?): Map<String, String> {
        if (json.isNullOrBlank()) return emptyMap()
        return try {
            kotlinx.serialization.json.Json.decodeFromString(json)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun serializeCustomFields(fields: Map<String, String>): String? {
        if (fields.isEmpty()) return null
        return kotlinx.serialization.json.Json.encodeToString(
            kotlinx.serialization.serializer(),
            fields
        )
    }

    private fun parseInterests(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            kotlinx.serialization.json.Json.decodeFromString(json)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun serializeInterests(interests: List<String>): String? {
        if (interests.isEmpty()) return null
        return kotlinx.serialization.json.Json.encodeToString(
            kotlinx.serialization.serializer(),
            interests
        )
    }
}

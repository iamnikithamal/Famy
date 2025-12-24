package com.famy.tree.domain.repository

import com.famy.tree.domain.model.FamilyMember
import com.famy.tree.domain.model.Gender
import kotlinx.coroutines.flow.Flow

interface FamilyMemberRepository {
    fun observeMembersByTree(treeId: Long): Flow<List<FamilyMember>>
    fun observeMember(memberId: Long): Flow<FamilyMember?>
    suspend fun getMember(memberId: Long): FamilyMember?
    suspend fun getMembersByTree(treeId: Long): List<FamilyMember>
    suspend fun createMember(member: FamilyMember): FamilyMember
    suspend fun updateMember(member: FamilyMember)
    suspend fun deleteMember(memberId: Long)
    fun searchMembers(treeId: Long, query: String): Flow<List<FamilyMember>>
    fun searchAllMembers(query: String): Flow<List<FamilyMember>>
    fun observeRecentMembers(limit: Int = 10): Flow<List<FamilyMember>>
    fun observeRecentMembersByTree(treeId: Long, limit: Int = 10): Flow<List<FamilyMember>>
    fun getMembersByLivingStatus(treeId: Long, isLiving: Boolean): Flow<List<FamilyMember>>
    fun getMembersByGeneration(treeId: Long, generation: Int): Flow<List<FamilyMember>>
    fun getMembersByGender(treeId: Long, gender: Gender): Flow<List<FamilyMember>>
    fun getMembersByLineage(treeId: Long, paternalLine: Boolean): Flow<List<FamilyMember>>
    fun getMembersByBirthDateRange(treeId: Long, startDate: Long, endDate: Long): Flow<List<FamilyMember>>
    suspend fun getMemberCount(treeId: Long): Int
    fun observeMemberCount(treeId: Long): Flow<Int>
    suspend fun getTotalMemberCount(): Int
    fun observeTotalMemberCount(): Flow<Int>
    suspend fun getLivingCount(treeId: Long): Int
    suspend fun getDeceasedCount(treeId: Long): Int
    suspend fun getGenderCount(treeId: Long, gender: Gender): Int
    suspend fun getMaxGeneration(treeId: Long): Int
    fun observeMaxGeneration(treeId: Long): Flow<Int?>
    suspend fun getMinGeneration(treeId: Long): Int
    suspend fun updateMemberPhoto(memberId: Long, photoPath: String?)
    suspend fun updateMemberGeneration(memberId: Long, generation: Int)
}

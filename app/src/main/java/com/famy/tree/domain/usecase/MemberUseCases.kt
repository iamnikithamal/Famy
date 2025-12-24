package com.famy.tree.domain.usecase

import com.famy.tree.domain.model.FamilyMember
import com.famy.tree.domain.model.Gender
import com.famy.tree.domain.model.LifeEvent
import com.famy.tree.domain.model.LifeEventKind
import com.famy.tree.domain.model.Relationship
import com.famy.tree.domain.model.RelationshipKind
import com.famy.tree.domain.model.RelationshipWithMember
import com.famy.tree.domain.repository.FamilyMemberRepository
import com.famy.tree.domain.repository.FamilyTreeRepository
import com.famy.tree.domain.repository.LifeEventRepository
import com.famy.tree.domain.repository.RelationshipRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetMemberUseCase @Inject constructor(
    private val memberRepository: FamilyMemberRepository
) {
    operator fun invoke(memberId: Long): Flow<FamilyMember?> = memberRepository.observeMember(memberId)
    suspend fun get(memberId: Long): FamilyMember? = memberRepository.getMember(memberId)
}

class GetMembersByTreeUseCase @Inject constructor(
    private val memberRepository: FamilyMemberRepository
) {
    operator fun invoke(treeId: Long): Flow<List<FamilyMember>> = memberRepository.observeMembersByTree(treeId)
}

class CreateMemberUseCase @Inject constructor(
    private val memberRepository: FamilyMemberRepository,
    private val treeRepository: FamilyTreeRepository,
    private val lifeEventRepository: LifeEventRepository
) {
    suspend operator fun invoke(
        treeId: Long,
        firstName: String,
        lastName: String? = null,
        gender: Gender = Gender.UNKNOWN,
        birthDate: Long? = null,
        birthPlace: String? = null,
        isLiving: Boolean = true,
        setAsRoot: Boolean = false
    ): FamilyMember {
        val member = memberRepository.createMember(
            FamilyMember(
                treeId = treeId,
                firstName = firstName,
                lastName = lastName,
                gender = gender,
                birthDate = birthDate,
                birthPlace = birthPlace,
                isLiving = isLiving
            )
        )

        if (birthDate != null) {
            lifeEventRepository.createEvent(
                LifeEvent(
                    memberId = member.id,
                    type = LifeEventKind.BIRTH,
                    title = "Born",
                    eventDate = birthDate,
                    eventPlace = birthPlace
                )
            )
        }

        if (setAsRoot) {
            treeRepository.setRootMember(treeId, member.id)
        }

        return member
    }
}

class UpdateMemberUseCase @Inject constructor(
    private val memberRepository: FamilyMemberRepository
) {
    suspend operator fun invoke(member: FamilyMember) {
        memberRepository.updateMember(member)
    }
}

class DeleteMemberUseCase @Inject constructor(
    private val memberRepository: FamilyMemberRepository,
    private val treeRepository: FamilyTreeRepository
) {
    suspend operator fun invoke(memberId: Long) {
        val member = memberRepository.getMember(memberId) ?: return
        val tree = treeRepository.getTree(member.treeId)

        memberRepository.deleteMember(memberId)

        if (tree?.rootMemberId == memberId) {
            treeRepository.setRootMember(member.treeId, null)
        }
    }
}

class GetMemberRelationshipsUseCase @Inject constructor(
    private val relationshipRepository: RelationshipRepository,
    private val memberRepository: FamilyMemberRepository
) {
    suspend operator fun invoke(memberId: Long): List<RelationshipWithMember> {
        val relationships = relationshipRepository.getRelationships(memberId)
        return relationships.mapNotNull { relationship ->
            val relatedMember = memberRepository.getMember(relationship.relatedMemberId)
            relatedMember?.let {
                RelationshipWithMember(relationship, it)
            }
        }
    }

    fun observe(memberId: Long): Flow<List<RelationshipWithMember>> {
        return relationshipRepository.observeRelationships(memberId).map { relationships ->
            relationships.mapNotNull { relationship ->
                val relatedMember = memberRepository.getMember(relationship.relatedMemberId)
                relatedMember?.let {
                    RelationshipWithMember(relationship, it)
                }
            }
        }
    }
}

class AddRelationshipUseCase @Inject constructor(
    private val relationshipRepository: RelationshipRepository,
    private val memberRepository: FamilyMemberRepository
) {
    sealed class Result {
        data class Success(val relationship: Relationship) : Result()
        data class Error(val message: String) : Result()
    }

    suspend operator fun invoke(
        memberId: Long,
        relatedMemberId: Long,
        type: RelationshipKind,
        startDate: Long? = null,
        startPlace: String? = null
    ): Result {
        if (memberId == relatedMemberId) {
            return Result.Error("A person cannot be related to themselves")
        }

        if (relationshipRepository.relationshipExists(memberId, relatedMemberId, type)) {
            return Result.Error("This relationship already exists")
        }

        val member = memberRepository.getMember(memberId)
            ?: return Result.Error("Member not found")
        val relatedMember = memberRepository.getMember(relatedMemberId)
            ?: return Result.Error("Related member not found")

        if (member.treeId != relatedMember.treeId) {
            return Result.Error("Members must be in the same family tree")
        }

        val validationError = validateRelationship(member, relatedMember, type)
        if (validationError != null) {
            return Result.Error(validationError)
        }

        val relationship = relationshipRepository.createRelationship(
            memberId = memberId,
            relatedMemberId = relatedMemberId,
            type = type,
            startDate = startDate,
            startPlace = startPlace
        )

        updateGenerations(memberId, relatedMemberId, type)

        return Result.Success(relationship)
    }

    private suspend fun validateRelationship(
        member: FamilyMember,
        relatedMember: FamilyMember,
        type: RelationshipKind
    ): String? {
        when (type) {
            RelationshipKind.PARENT -> {
                val existingParents = relationshipRepository.getParentIds(member.id)
                if (existingParents.size >= 2) {
                    return "A person can have at most two parents"
                }

                if (relatedMember.birthDate != null && member.birthDate != null) {
                    if (relatedMember.birthDate >= member.birthDate) {
                        return "Parent cannot be born after child"
                    }
                }
            }
            RelationshipKind.CHILD -> {
                if (relatedMember.birthDate != null && member.birthDate != null) {
                    if (relatedMember.birthDate <= member.birthDate) {
                        return "Child cannot be born before parent"
                    }
                }
            }
            RelationshipKind.SPOUSE, RelationshipKind.EX_SPOUSE -> {
                val existingRelationship = relationshipRepository.getRelationshipBetween(member.id, relatedMember.id)
                if (existingRelationship.any { it.type == RelationshipKind.PARENT || it.type == RelationshipKind.CHILD }) {
                    return "Cannot be spouse of a parent or child"
                }
                if (existingRelationship.any { it.type == RelationshipKind.SIBLING }) {
                    return "Cannot be spouse of a sibling"
                }
            }
            RelationshipKind.SIBLING -> {
                val existingRelationship = relationshipRepository.getRelationshipBetween(member.id, relatedMember.id)
                if (existingRelationship.any { it.type == RelationshipKind.SPOUSE || it.type == RelationshipKind.EX_SPOUSE }) {
                    return "Cannot be sibling of a spouse"
                }
            }
        }
        return null
    }

    private suspend fun updateGenerations(memberId: Long, relatedMemberId: Long, type: RelationshipKind) {
        val member = memberRepository.getMember(memberId) ?: return
        val relatedMember = memberRepository.getMember(relatedMemberId) ?: return

        when (type) {
            RelationshipKind.PARENT -> {
                if (relatedMember.generation >= member.generation) {
                    memberRepository.updateMemberGeneration(relatedMemberId, member.generation - 1)
                }
            }
            RelationshipKind.CHILD -> {
                if (relatedMember.generation <= member.generation) {
                    memberRepository.updateMemberGeneration(relatedMemberId, member.generation + 1)
                }
            }
            RelationshipKind.SIBLING -> {
                if (relatedMember.generation != member.generation) {
                    memberRepository.updateMemberGeneration(relatedMemberId, member.generation)
                }
            }
            RelationshipKind.SPOUSE, RelationshipKind.EX_SPOUSE -> {
                if (relatedMember.generation != member.generation) {
                    memberRepository.updateMemberGeneration(relatedMemberId, member.generation)
                }
            }
        }
    }
}

class RemoveRelationshipUseCase @Inject constructor(
    private val relationshipRepository: RelationshipRepository
) {
    suspend operator fun invoke(relationshipId: Long) {
        relationshipRepository.deleteRelationship(relationshipId)
    }
}

class SearchMembersUseCase @Inject constructor(
    private val memberRepository: FamilyMemberRepository
) {
    operator fun invoke(treeId: Long, query: String): Flow<List<FamilyMember>> {
        return memberRepository.searchMembers(treeId, query)
    }

    fun searchAll(query: String): Flow<List<FamilyMember>> {
        return memberRepository.searchAllMembers(query)
    }
}

class GetRecentMembersUseCase @Inject constructor(
    private val memberRepository: FamilyMemberRepository
) {
    operator fun invoke(limit: Int = 10): Flow<List<FamilyMember>> {
        return memberRepository.observeRecentMembers(limit)
    }

    fun forTree(treeId: Long, limit: Int = 10): Flow<List<FamilyMember>> {
        return memberRepository.observeRecentMembersByTree(treeId, limit)
    }
}

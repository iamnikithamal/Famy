package com.famy.tree.data.repository

import com.famy.tree.data.local.dao.FamilyMemberDao
import com.famy.tree.data.local.dao.FamilyTreeDao
import com.famy.tree.data.local.dao.LifeEventDao
import com.famy.tree.data.local.entity.LifeEventEntity
import com.famy.tree.domain.model.LifeEvent
import com.famy.tree.domain.model.LifeEventKind
import com.famy.tree.domain.repository.LifeEventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LifeEventRepositoryImpl @Inject constructor(
    private val lifeEventDao: LifeEventDao,
    private val familyMemberDao: FamilyMemberDao,
    private val familyTreeDao: FamilyTreeDao
) : LifeEventRepository {

    override fun observeEvents(memberId: Long): Flow<List<LifeEvent>> {
        return lifeEventDao.observeByMemberId(memberId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getEvents(memberId: Long): List<LifeEvent> {
        return lifeEventDao.getByMemberId(memberId).map { it.toDomain() }
    }

    override suspend fun getEvent(eventId: Long): LifeEvent? {
        return lifeEventDao.getById(eventId)?.toDomain()
    }

    override fun observeEventsByType(memberId: Long, type: LifeEventKind): Flow<List<LifeEvent>> {
        return lifeEventDao.observeByType(memberId, type.name).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun createEvent(event: LifeEvent): LifeEvent {
        val entity = event.toEntity()
        val id = lifeEventDao.insert(entity)
        touchMemberTree(event.memberId)
        return lifeEventDao.getById(id)!!.toDomain()
    }

    override suspend fun updateEvent(event: LifeEvent) {
        lifeEventDao.update(event.toEntity())
        touchMemberTree(event.memberId)
    }

    override suspend fun deleteEvent(eventId: Long) {
        val event = lifeEventDao.getById(eventId) ?: return
        lifeEventDao.deleteById(eventId)
        touchMemberTree(event.memberId)
    }

    override suspend fun deleteAllEvents(memberId: Long) {
        lifeEventDao.deleteByMemberId(memberId)
        touchMemberTree(memberId)
    }

    override fun observeEventsByTree(treeId: Long): Flow<List<LifeEvent>> {
        return lifeEventDao.observeByTreeId(treeId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getEventsByTree(treeId: Long): List<LifeEvent> {
        return lifeEventDao.getByTreeId(treeId).map { it.toDomain() }
    }

    override fun observeEventsByTreeAndType(treeId: Long, type: LifeEventKind): Flow<List<LifeEvent>> {
        return lifeEventDao.observeByTreeAndType(treeId, type.name).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun observeEventsByDateRange(treeId: Long, startDate: Long, endDate: Long): Flow<List<LifeEvent>> {
        return lifeEventDao.observeByDateRange(treeId, startDate, endDate).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getEventCount(memberId: Long): Int {
        return lifeEventDao.getCountByMember(memberId)
    }

    override suspend fun getEventCountByTree(treeId: Long): Int {
        return lifeEventDao.getCountByTree(treeId)
    }

    private suspend fun touchMemberTree(memberId: Long) {
        familyMemberDao.getById(memberId)?.let { member ->
            familyTreeDao.touch(member.treeId)
        }
    }

    private fun LifeEventEntity.toDomain(): LifeEvent = LifeEvent(
        id = id,
        memberId = memberId,
        type = LifeEventKind.fromString(eventType),
        title = title,
        description = description,
        eventDate = eventDate,
        eventPlace = eventPlace,
        createdAt = createdAt
    )

    private fun LifeEvent.toEntity(): LifeEventEntity = LifeEventEntity(
        id = id,
        memberId = memberId,
        eventType = type.name,
        title = title,
        description = description,
        eventDate = eventDate,
        eventPlace = eventPlace,
        createdAt = createdAt
    )
}

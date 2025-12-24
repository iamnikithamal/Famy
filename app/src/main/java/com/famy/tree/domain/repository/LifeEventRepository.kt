package com.famy.tree.domain.repository

import com.famy.tree.domain.model.LifeEvent
import com.famy.tree.domain.model.LifeEventKind
import kotlinx.coroutines.flow.Flow

interface LifeEventRepository {
    fun observeEvents(memberId: Long): Flow<List<LifeEvent>>
    suspend fun getEvents(memberId: Long): List<LifeEvent>
    suspend fun getEvent(eventId: Long): LifeEvent?
    fun observeEventsByType(memberId: Long, type: LifeEventKind): Flow<List<LifeEvent>>
    suspend fun createEvent(event: LifeEvent): LifeEvent
    suspend fun updateEvent(event: LifeEvent)
    suspend fun deleteEvent(eventId: Long)
    suspend fun deleteAllEvents(memberId: Long)
    fun observeEventsByTree(treeId: Long): Flow<List<LifeEvent>>
    suspend fun getEventsByTree(treeId: Long): List<LifeEvent>
    fun observeEventsByTreeAndType(treeId: Long, type: LifeEventKind): Flow<List<LifeEvent>>
    fun observeEventsByDateRange(treeId: Long, startDate: Long, endDate: Long): Flow<List<LifeEvent>>
    suspend fun getEventCount(memberId: Long): Int
    suspend fun getEventCountByTree(treeId: Long): Int
}

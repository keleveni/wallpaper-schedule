package com.keleveni.wallpaperscheduler.data

import kotlinx.coroutines.flow.Flow

class ScheduleRepository(private val dao: ScheduleDao) {
    val schedules: Flow<List<ScheduleEntity>> = dao.observeAll()

    suspend fun getAll(): List<ScheduleEntity> = dao.getAll()

    suspend fun get(id: Long): ScheduleEntity? = dao.get(id)

    suspend fun save(schedule: ScheduleEntity) {
        if (schedule.id == 0L) dao.insert(schedule) else dao.update(schedule)
    }

    suspend fun setEnabled(id: Long, enabled: Boolean) = dao.setEnabled(id, enabled)

    suspend fun delete(id: Long) = dao.delete(id)
}

package com.termex.app.data.repository

import com.termex.app.data.local.ServerDao
import com.termex.app.data.local.WorkplaceDao
import com.termex.app.data.local.WorkplaceEntity
import com.termex.app.data.local.toDomain
import com.termex.app.domain.Server
import com.termex.app.domain.Workplace
import com.termex.app.domain.WorkplaceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkplaceRepositoryImpl @Inject constructor(
    private val workplaceDao: WorkplaceDao,
    private val serverDao: ServerDao
) : WorkplaceRepository {

    override fun getAllWorkplaces(): Flow<List<Workplace>> {
        return workplaceDao.getAllWorkplaces().map { entities ->
            entities.map { Workplace(id = it.id, name = it.name) }
        }
    }

    override suspend fun getWorkplace(id: String): Workplace? {
        return workplaceDao.getWorkplace(id)?.let {
            Workplace(id = it.id, name = it.name)
        }
    }

    override suspend fun addWorkplace(workplace: Workplace) {
        workplaceDao.insert(WorkplaceEntity(id = workplace.id, name = workplace.name))
    }

    override suspend fun updateWorkplace(workplace: Workplace) {
        workplaceDao.update(WorkplaceEntity(id = workplace.id, name = workplace.name))
    }

    override suspend fun deleteWorkplace(workplace: Workplace) {
        workplaceDao.delete(WorkplaceEntity(id = workplace.id, name = workplace.name))
    }

    override fun getServersForWorkplace(workplaceId: String): Flow<List<Server>> {
        return serverDao.getServersByWorkplace(workplaceId).map { entities ->
            entities.map { it.toDomain() }
        }
    }
}

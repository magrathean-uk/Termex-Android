package com.termex.app.domain

import kotlinx.coroutines.flow.Flow

interface WorkplaceRepository {
    fun getAllWorkplaces(): Flow<List<Workplace>>
    suspend fun getWorkplace(id: String): Workplace?
    suspend fun addWorkplace(workplace: Workplace)
    suspend fun updateWorkplace(workplace: Workplace)
    suspend fun deleteWorkplace(workplace: Workplace)
    fun getServersForWorkplace(workplaceId: String): Flow<List<Server>>
}

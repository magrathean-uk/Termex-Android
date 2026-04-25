package com.termex.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.termex.app.domain.Workplace

@Entity(tableName = "workplaces")
data class WorkplaceEntity(
    @PrimaryKey val id: String,
    val name: String
)

fun WorkplaceEntity.toDomain() = Workplace(id = id, name = name)
fun Workplace.toEntity() = WorkplaceEntity(id = id, name = name)

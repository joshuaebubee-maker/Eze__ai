package com.example.data.repository

import com.example.data.local.MemoryDao
import com.example.data.local.MemoryEntity
import kotlinx.coroutines.flow.Flow

class MemoryRepository(private val memoryDao: MemoryDao) {

    val allMemories: Flow<List<MemoryEntity>> = memoryDao.getAllMemories()

    suspend fun getAllMemoriesList(): List<MemoryEntity> {
        return memoryDao.getAllMemoriesList()
    }

    suspend fun saveMemory(key: String, value: String, category: String = "general"): Long {
        val entity = MemoryEntity(
            key = key.trim(),
            value = value.trim(),
            category = category,
            timestamp = System.currentTimeMillis()
        )
        return memoryDao.insertMemory(entity)
    }

    suspend fun deleteMemory(id: Long) {
        memoryDao.deleteMemoryById(id)
    }

    suspend fun clearAll() {
        memoryDao.clearAllMemories()
    }
}

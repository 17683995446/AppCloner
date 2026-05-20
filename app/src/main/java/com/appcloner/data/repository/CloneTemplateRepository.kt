package com.appcloner.data.repository

import com.appcloner.data.local.dao.CloneTemplateDao
import com.appcloner.data.local.entity.CloneTemplateEntity
import com.appcloner.core.model.CloneTemplate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloneTemplateRepository @Inject constructor(
    private val cloneTemplateDao: CloneTemplateDao
) {
    fun getAllTemplates(): Flow<List<CloneTemplate>> {
        return cloneTemplateDao.getAllTemplates().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    suspend fun getTemplateById(id: Long): CloneTemplate? {
        return cloneTemplateDao.getTemplateById(id)?.toDomainModel()
    }

    suspend fun getDefaultTemplate(): CloneTemplate? {
        return cloneTemplateDao.getDefaultTemplate()?.toDomainModel()
    }

    fun searchTemplates(query: String): Flow<List<CloneTemplate>> {
        return cloneTemplateDao.searchTemplates(query).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    suspend fun saveTemplate(template: CloneTemplate): Long {
        return cloneTemplateDao.insert(template.toEntity())
    }

    suspend fun updateTemplate(template: CloneTemplate) {
        cloneTemplateDao.update(template.toEntity())
    }

    suspend fun deleteTemplate(template: CloneTemplate) {
        cloneTemplateDao.delete(template.toEntity())
    }

    suspend fun incrementUsageCount(id: Long) {
        cloneTemplateDao.incrementUsageCount(id)
    }

    suspend fun getTemplateCount(): Int {
        return cloneTemplateDao.getCount()
    }

    private fun CloneTemplateEntity.toDomainModel(): CloneTemplate {
        return CloneTemplate(
            id = id,
            name = name,
            description = description,
            packageNamePattern = packageNamePattern,
            customSettings = customSettings,
            createdAt = createdAt,
            isDefault = isDefault,
            usageCount = usageCount
        )
    }

    private fun CloneTemplate.toEntity(): CloneTemplateEntity {
        return CloneTemplateEntity(
            id = id,
            name = name,
            description = description,
            packageNamePattern = packageNamePattern,
            customSettings = customSettings,
            createdAt = createdAt,
            isDefault = isDefault,
            usageCount = usageCount
        )
    }
}

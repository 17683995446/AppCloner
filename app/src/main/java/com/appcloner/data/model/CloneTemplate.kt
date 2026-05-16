
package com.appcloner.data.model

import android.content.Context
import android.content.SharedPreferences

data class CloneTemplate(
    val id: String,
    val name: String,
    val description: String,
    val packageSuffix: String,
    val appNameSuffix: String,
    val copyData: Boolean,
    val keepIcon: Boolean,
    val isDefault: Boolean = false
) {
    companion object {
        val DEFAULT_GAME = CloneTemplate(
            id = "default_game",
            name = "游戏克隆",
            description = "适合游戏的克隆配置，保留数据和图标",
            packageSuffix = ".game",
            appNameSuffix = " (Game)",
            copyData = true,
            keepIcon = true,
            isDefault = true
        )

        val DEFAULT_WORK = CloneTemplate(
            id = "default_work",
            name = "工作账号",
            description = "适合需要独立数据的应用",
            packageSuffix = ".work",
            appNameSuffix = " (Work)",
            copyData = true,
            keepIcon = false,
            isDefault = true
        )

        val DEFAULT_TEST = CloneTemplate(
            id = "default_test",
            name = "测试版本",
            description = "适合开发测试",
            packageSuffix = ".test",
            appNameSuffix = " (Test)",
            copyData = false,
            keepIcon = true,
            isDefault = true
        )

        fun getDefaultTemplates(): List<CloneTemplate> = listOf(
            DEFAULT_GAME,
            DEFAULT_WORK,
            DEFAULT_TEST
        )
    }
}

class CloneTemplateManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "clone_templates", Context.MODE_PRIVATE
    )

    private val templates = mutableListOf<CloneTemplate>()

    init {
        loadTemplates()
    }

    private fun loadTemplates() {
        templates.clear()
        templates.addAll(CloneTemplate.getDefaultTemplates())

        val customTemplatesJson = prefs.getString("custom_templates", "") ?: ""
        if (customTemplatesJson.isNotEmpty()) {
            try {
                val customTemplates = parseTemplates(customTemplatesJson)
                templates.addAll(customTemplates)
            } catch (e: Exception) {
                // Ignore parsing errors
            }
        }
    }

    private fun parseTemplates(json: String): List<CloneTemplate> {
        // Simple JSON parsing
        return json.split("|||").filter { it.isNotEmpty() }.mapNotNull { templateStr ->
            try {
                val parts = templateStr.split("|||")
                if (parts.size >= 7) {
                    CloneTemplate(
                        id = parts[0],
                        name = parts[1],
                        description = parts[2],
                        packageSuffix = parts[3],
                        appNameSuffix = parts[4],
                        copyData = parts[5].toBoolean(),
                        keepIcon = parts[6].toBoolean(),
                        isDefault = false
                    )
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun serializeTemplates(templates: List<CloneTemplate>): String {
        return templates
            .filter { !it.isDefault }
            .joinToString("|||") { template ->
                listOf(
                    template.id,
                    template.name,
                    template.description,
                    template.packageSuffix,
                    template.appNameSuffix,
                    template.copyData.toString(),
                    template.keepIcon.toString()
                ).joinToString("|||")
            }
    }

    fun getAllTemplates(): List<CloneTemplate> = templates.toList()

    fun getDefaultTemplates(): List<CloneTemplate> = templates.filter { it.isDefault }

    fun getCustomTemplates(): List<CloneTemplate> = templates.filter { !it.isDefault }

    fun saveTemplate(template: CloneTemplate) {
        if (!templates.any { it.id == template.id }) {
            templates.add(template)
            saveCustomTemplates()
        }
    }

    fun deleteTemplate(templateId: String) {
        templates.removeAll { it.id == templateId && !it.isDefault }
        saveCustomTemplates()
    }

    private fun saveCustomTemplates() {
        val customTemplates = templates.filter { !it.isDefault }
        val json = serializeTemplates(customTemplates)
        prefs.edit().putString("custom_templates", json).apply()
    }

    fun updateTemplate(template: CloneTemplate) {
        val index = templates.indexOfFirst { it.id == template.id }
        if (index >= 0 && !templates[index].isDefault) {
            templates[index] = template
            saveCustomTemplates()
        }
    }

    fun exportTemplates(): String {
        return serializeTemplates(templates.filter { !it.isDefault })
    }

    fun importTemplates(json: String): Boolean {
        return try {
            val imported = parseTemplates(json)
            imported.forEach { template ->
                if (!templates.any { it.id == template.id }) {
                    templates.add(template)
                }
            }
            saveCustomTemplates()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun generatePackageName(originalPackage: String, template: CloneTemplate): String {
        return "$originalPackage$template.packageSuffix"
    }

    fun generateAppName(originalName: String, template: CloneTemplate): String {
        return "$originalName${template.appNameSuffix}"
    }
}

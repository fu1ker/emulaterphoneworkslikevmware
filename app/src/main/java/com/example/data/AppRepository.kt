package com.example.data

import kotlinx.coroutines.flow.Flow
import java.util.UUID

class AppRepository(private val appDao: AppDao) {

    val topLevelApps: Flow<List<AppEntity>> = appDao.getTopLevelAppsFlow()

    fun getSubApps(parentId: String): Flow<List<AppEntity>> = appDao.getSubAppsFlow(parentId)

    fun getAppByIdFlow(appId: String): Flow<AppEntity?> = appDao.getAppByIdFlow(appId)

    suspend fun getAppById(appId: String): AppEntity? = appDao.getAppById(appId)

    fun getComponentsFlow(appId: String): Flow<List<ComponentEntity>> = appDao.getComponentsFlow(appId)

    suspend fun getComponentsList(appId: String): List<ComponentEntity> = appDao.getComponentsList(appId)

    suspend fun saveApp(app: AppEntity) {
        appDao.insertApp(app)
    }

    suspend fun saveComponent(component: ComponentEntity) {
        appDao.insertComponent(component)
    }

    suspend fun saveComponents(components: List<ComponentEntity>) {
        appDao.insertComponents(components)
    }

    suspend fun deleteComponent(componentId: String) {
        appDao.deleteComponent(componentId)
    }

    suspend fun deleteAppRecursively(appId: String) {
        val components = appDao.getComponentsList(appId)
        for (comp in components) {
            if (comp.type == "NESTED_APP" && comp.value.isNotEmpty()) {
                deleteAppRecursively(comp.value)
            }
        }
        appDao.deleteComponentsForApp(appId)
        appDao.deleteApp(appId)
    }

    // Helper to create a new top-level or sub-app with default components
    suspend fun createNewApp(name: String, parentId: String?, color: Long): AppEntity {
        val appId = UUID.randomUUID().toString()
        val app = AppEntity(
            id = appId,
            name = name,
            parentId = parentId,
            primaryColor = color
        )
        appDao.insertApp(app)

        // Create a couple of default components for a nice starting template
        val defaultComponents = listOf(
            ComponentEntity(
                id = UUID.randomUUID().toString(),
                appId = appId,
                type = "TEXT",
                label = "Welcome to ${name}!",
                value = "18", // font size 18sp
                orderIndex = 0
            ),
            ComponentEntity(
                id = UUID.randomUUID().toString(),
                appId = appId,
                type = "SPACER",
                label = "",
                value = "16", // spacing height 16dp
                orderIndex = 1
            )
        )
        appDao.insertComponents(defaultComponents)
        return app
    }
}

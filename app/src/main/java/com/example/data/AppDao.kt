package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM apps WHERE parentId IS NULL ORDER BY createdAt DESC")
    fun getTopLevelAppsFlow(): Flow<List<AppEntity>>

    @Query("SELECT * FROM apps WHERE parentId = :parentId ORDER BY createdAt DESC")
    fun getSubAppsFlow(parentId: String): Flow<List<AppEntity>>

    @Query("SELECT * FROM apps WHERE id = :appId")
    suspend fun getAppById(appId: String): AppEntity?

    @Query("SELECT * FROM apps WHERE id = :appId")
    fun getAppByIdFlow(appId: String): Flow<AppEntity?>

    @Query("SELECT * FROM components WHERE appId = :appId ORDER BY orderIndex ASC")
    fun getComponentsFlow(appId: String): Flow<List<ComponentEntity>>

    @Query("SELECT * FROM components WHERE appId = :appId ORDER BY orderIndex ASC")
    suspend fun getComponentsList(appId: String): List<ComponentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApp(app: AppEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComponent(component: ComponentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComponents(components: List<ComponentEntity>)

    @Query("DELETE FROM apps WHERE id = :appId")
    suspend fun deleteApp(appId: String)

    @Query("DELETE FROM components WHERE appId = :appId")
    suspend fun deleteComponentsForApp(appId: String)

    @Query("DELETE FROM components WHERE id = :componentId")
    suspend fun deleteComponent(componentId: String)
}

package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppEntity
import com.example.data.AppRepository
import com.example.data.ComponentEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class AppViewModel(private val repository: AppRepository) : ViewModel() {

    // List of top-level apps
    val topLevelApps: StateFlow<List<AppEntity>> = repository.topLevelApps
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Navigation Stack representing current "Inception" depth of apps we are building or running
    private val _navigationStack = MutableStateFlow<List<AppEntity>>(emptyList())
    val navigationStack: StateFlow<List<AppEntity>> = _navigationStack.asStateFlow()

    // Map of active component Flows per appId to easily support nested rendering
    private val _componentsMap = mutableMapOf<String, StateFlow<List<ComponentEntity>>>()
    private val _subAppMap = mutableMapOf<String, StateFlow<AppEntity?>>()

    fun getComponentsForApp(appId: String): StateFlow<List<ComponentEntity>> {
        return _componentsMap.getOrPut(appId) {
            repository.getComponentsFlow(appId)
                .distinctUntilChanged()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = emptyList()
                )
        }
    }

    // Palette of vibrant, tech-inspired Material 3 theme colors for sub-apps
    val colorPalette = listOf(
        0xFF3B82F6, // Electric Blue
        0xFF10B981, // Neon Emerald
        0xFF8B5CF6, // Cyber Purple
        0xFFF59E0B, // Solar Amber
        0xFFEF4444, // Crimson Red
        0xFFEC4899, // Cyber Pink
        0xFF06B6D4, // Deep Teal
        0xFFF97316  // Retro Orange
    )

    fun createTopLevelApp(name: String, colorHex: Long) {
        viewModelScope.launch {
            repository.createNewApp(name, parentId = null, color = colorHex)
        }
    }

    fun pushApp(app: AppEntity) {
        _navigationStack.update { it + app }
    }

    fun popApp() {
        _navigationStack.update {
            if (it.isNotEmpty()) it.dropLast(1) else emptyList()
        }
    }

    fun deleteApp(appId: String) {
        viewModelScope.launch {
            repository.deleteAppRecursively(appId)
            // If the deleted app is in our stack, we should pop it
            _navigationStack.update { stack ->
                stack.filter { it.id != appId }
            }
        }
    }

    fun updateAppDetails(appId: String, name: String, colorHex: Long) {
        viewModelScope.launch {
            val app = repository.getAppById(appId)
            if (app != null) {
                val updatedApp = app.copy(name = name, primaryColor = colorHex)
                repository.saveApp(updatedApp)
                
                // Keep navigation stack updated in-place if editing active app properties
                _navigationStack.update { stack ->
                    stack.map { if (it.id == appId) updatedApp else it }
                }
            }
        }
    }

    fun addComponent(appId: String, type: String, label: String, value: String) {
        viewModelScope.launch {
            val currentComponents = repository.getComponentsList(appId)
            val newOrder = currentComponents.size
            val newComponent = ComponentEntity(
                id = UUID.randomUUID().toString(),
                appId = appId,
                type = type,
                label = label,
                value = value,
                orderIndex = newOrder
            )
            repository.saveComponent(newComponent)
        }
    }

    fun updateComponent(component: ComponentEntity) {
        viewModelScope.launch {
            repository.saveComponent(component)
        }
    }

    fun deleteComponent(appId: String, componentId: String) {
        viewModelScope.launch {
            // If the deleted component is a NESTED_APP, delete the nested app recursively
            val components = repository.getComponentsList(appId)
            val compToDelete = components.find { it.id == componentId }
            if (compToDelete != null && compToDelete.type == "NESTED_APP" && compToDelete.value.isNotEmpty()) {
                repository.deleteAppRecursively(compToDelete.value)
            }
            
            repository.deleteComponent(componentId)
            reorderComponents(appId)
        }
    }

    private suspend fun reorderComponents(appId: String) {
        val remaining = repository.getComponentsList(appId)
        val updated = remaining.mapIndexed { index, comp ->
            comp.copy(orderIndex = index)
        }
        repository.saveComponents(updated)
    }

    fun moveComponentUp(appId: String, componentId: String) {
        viewModelScope.launch {
            val components = repository.getComponentsList(appId).toMutableList()
            val index = components.indexOfFirst { it.id == componentId }
            if (index > 0) {
                val temp = components[index]
                components[index] = components[index - 1]
                components[index - 1] = temp
                
                val reordered = components.mapIndexed { i, comp ->
                    comp.copy(orderIndex = i)
                }
                repository.saveComponents(reordered)
            }
        }
    }

    fun moveComponentDown(appId: String, componentId: String) {
        viewModelScope.launch {
            val components = repository.getComponentsList(appId).toMutableList()
            val index = components.indexOfFirst { it.id == componentId }
            if (index in 0 until components.size - 1) {
                val temp = components[index]
                components[index] = components[index + 1]
                components[index + 1] = temp
                
                val reordered = components.mapIndexed { i, comp ->
                    comp.copy(orderIndex = i)
                }
                repository.saveComponents(reordered)
            }
        }
    }

    // Special flow to create and link a nested sub-app to a component
    fun createAndLinkNestedApp(parentAppId: String, componentId: String, subAppName: String, subAppColor: Long) {
        viewModelScope.launch {
            // 1. Create a new sub-app, setting parentId to parentAppId
            val subApp = repository.createNewApp(subAppName, parentId = parentAppId, color = subAppColor)
            
            // 2. Fetch parent app components
            val components = repository.getComponentsList(parentAppId)
            val componentToUpdate = components.find { it.id == componentId }
            
            if (componentToUpdate != null) {
                // Update the NESTED_APP component to hold the newly created sub-app ID in its value field
                val updatedComponent = componentToUpdate.copy(value = subApp.id)
                repository.saveComponent(updatedComponent)
            }
        }
    }

    // Get an app synchronously or via Flow for inline sub-app rendering
    fun getSubAppFlow(subAppId: String): StateFlow<AppEntity?> {
        return _subAppMap.getOrPut(subAppId) {
            repository.getAppByIdFlow(subAppId)
                .distinctUntilChanged()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = null
                )
        }
    }
}

class AppViewModelFactory(private val repository: AppRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AppViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "apps")
data class AppEntity(
    @PrimaryKey val id: String,
    val name: String,
    val parentId: String?, // Null for top-level apps, non-null if nested
    val primaryColor: Long, // Color Hex representation
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "components")
data class ComponentEntity(
    @PrimaryKey val id: String,
    val appId: String, // The App ID this component belongs to
    val type: String, // "TEXT", "BUTTON", "COUNTER", "INPUT", "DIVIDER", "SPACER", "NESTED_APP"
    val label: String, // Text, button label, or counter label
    val value: String, // Config text value (e.g. toast text for buttons, placeholder for input, or nestedAppId for NESTED_APP)
    val orderIndex: Int
)

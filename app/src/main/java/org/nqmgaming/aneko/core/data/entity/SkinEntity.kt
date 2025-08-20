package org.nqmgaming.aneko.core.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "skin")
data class SkinEntity(
    @PrimaryKey
    val packageName: String,
    val name: String,
    val author: String,
    val previewPath: String,
    val isActive: Boolean,
    val isFavorite: Boolean,
    val isBuiltin: Boolean = false,
)
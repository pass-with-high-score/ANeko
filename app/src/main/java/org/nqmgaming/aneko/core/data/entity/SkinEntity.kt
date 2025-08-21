package org.nqmgaming.aneko.core.data.entity

import android.content.Context
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.File

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

fun SkinEntity.previewFile(context: Context): File {
    val root = File(context.filesDir, "skins")
    return File(File(root, packageName), previewPath)
}

fun SkinEntity.previewModel(context: Context): Any {
    return previewFile(context)
}
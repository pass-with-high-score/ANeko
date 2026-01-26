package org.nqmgaming.aneko.core.data

import androidx.room.Database
import androidx.room.RoomDatabase
import org.nqmgaming.aneko.core.data.dao.SkinDao
import org.nqmgaming.aneko.core.data.entity.SkinEntity

@Database(
    entities = [SkinEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AnekoDatabase : RoomDatabase() {
    abstract fun skinDao(): SkinDao
}
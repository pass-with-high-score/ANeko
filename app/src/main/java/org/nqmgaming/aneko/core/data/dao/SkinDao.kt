package org.nqmgaming.aneko.core.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.nqmgaming.aneko.core.data.entity.SkinEntity

@Dao
interface SkinDao {
    @Query("SELECT * FROM skin ORDER BY name")
    fun observeSkins(): Flow<List<SkinEntity>>

    @Query("SELECT * FROM skin WHERE packageName = :pkg LIMIT 1")
    suspend fun getSkin(pkg: String): SkinEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSkin(skin: SkinEntity)

    @Query("UPDATE skin SET isActive = (packageName = :pkg)")
    suspend fun switchActive(pkg: String)

    @Query("UPDATE skin SET isFavorite = NOT isFavorite WHERE packageName = :pkg")
    suspend fun toggleFavorite(pkg: String)

    @Delete
    suspend fun deleteSkin(skin: SkinEntity)

    @Query("SELECT * FROM skin WHERE isActive = 1 LIMIT 1")
    fun getActiveSkin(): SkinEntity?

}
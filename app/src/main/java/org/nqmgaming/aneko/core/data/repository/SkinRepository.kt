package org.nqmgaming.aneko.core.data.repository

import kotlinx.coroutines.flow.Flow
import org.nqmgaming.aneko.core.data.dao.SkinDao
import org.nqmgaming.aneko.core.data.entity.SkinEntity
import timber.log.Timber
import javax.inject.Inject

class SkinRepository @Inject constructor(
    private val skinDao: SkinDao,
) {
    fun observeSkins(): Flow<List<SkinEntity>> = skinDao.observeSkins()

    suspend fun upsertSkin(skin: SkinEntity) = skinDao.upsertSkin(skin)

    suspend fun toggleActive(pkg: String) {
        Timber.d("Toggling active state for skin: $pkg")
        skinDao.toggleActive(pkg)
    }

    suspend fun countActive(): Int = skinDao.countActive()

    suspend fun removeSkin(skin: SkinEntity) = skinDao.deleteSkin(skin)
}

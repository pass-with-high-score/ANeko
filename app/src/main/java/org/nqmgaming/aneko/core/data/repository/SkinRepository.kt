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
    suspend fun switchActive(pkg: String) {
        Timber.d("Switching active skin to package: $pkg")
        skinDao.switchActive(pkg)
    }

    suspend fun removeSkin(skin: SkinEntity) = skinDao.deleteSkin(skin)

    fun getActiveSkin(): SkinEntity? = skinDao.getActiveSkin()

}

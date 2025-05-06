package org.nqmgaming.aneko.data.skin

import kotlinx.serialization.Serializable

@Serializable
data class SkinConfig(
    val motionParams: MotionParams,
    val info: SkinInfo
)

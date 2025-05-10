package org.nqmgaming.aneko.data.skin

import kotlinx.serialization.Serializable

@Serializable
data class SkinInfo(
    val skinId: String,
    val name: String,
    val author: String,
    val icon: String,
    val description: String,
    val createdAt: String,
)

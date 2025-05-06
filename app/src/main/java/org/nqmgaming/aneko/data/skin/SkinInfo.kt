package org.nqmgaming.aneko.data.skin

import kotlinx.serialization.Serializable

@Serializable
data class SkinInfo(
    val name: String,
    val author: String,
    val icon: String,
    val description: String,
    val createdAt: String,
    val tags: List<String> = emptyList(),
    val homepage: String? = null
)

package org.nqmgaming.aneko.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SkinCollection(
    val name: String,
    @SerialName("package")
    val packageName: String,
    val version: String,
    val image: String,
    val url: String,
)
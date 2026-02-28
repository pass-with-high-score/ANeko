package org.nqmgaming.aneko.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SkinCollection(
    val name: String,
    @SerialName("package")
    val packageName: String,
    val version: String,
    val author: String? = null,
    val image: String,
    val url: String,
) {
    val isBuiltIn: Boolean
        get() = author?.equals(OFFICIAL_AUTHOR, ignoreCase = true) == true

    companion object {
        const val OFFICIAL_AUTHOR = "nqmgaming"
    }
}
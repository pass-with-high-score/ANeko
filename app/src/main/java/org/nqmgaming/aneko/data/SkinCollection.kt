package org.nqmgaming.aneko.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.nqmgaming.aneko.core.pet.CodexPetContract

@Serializable
enum class SkinSource {
    ANEKO,
    PETDEX,
}

@Serializable
data class SkinCollection(
    val name: String,
    @SerialName("package")
    val packageName: String,
    val version: String,
    val author: String? = null,
    val image: String,
    val url: String,
    val source: SkinSource = SkinSource.ANEKO,
    val codexPetId: String? = null,
) {
    val isBuiltIn: Boolean
        get() = source == SkinSource.ANEKO &&
            author?.equals(OFFICIAL_AUTHOR, ignoreCase = true) == true

    val isPetdex: Boolean
        get() = source == SkinSource.PETDEX

    companion object {
        const val OFFICIAL_AUTHOR = "nqmgaming"
    }
}

@Serializable
data class PetdexManifest(
    val generatedAt: String,
    val total: Int,
    val pets: List<PetdexPet>,
)

@Serializable
data class PetdexPet(
    val slug: String,
    val displayName: String,
    val kind: String,
    val submittedBy: String? = null,
    val spritesheetUrl: String,
    val petJsonUrl: String,
    val zipUrl: String,
) {
    fun toSkinCollection(): SkinCollection {
        val assetBase = spritesheetUrl.substringBefore("/pets/")
        return SkinCollection(
            name = displayName,
            packageName = CodexPetContract.packageName(slug),
            version = "petdex:${spritesheetUrl.substringBeforeLast('/')}",
            author = submittedBy,
            image = "$assetBase/pets/$slug/preview.webp",
            url = zipUrl,
            source = SkinSource.PETDEX,
            codexPetId = slug,
        )
    }
}

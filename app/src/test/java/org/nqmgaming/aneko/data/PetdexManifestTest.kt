package org.nqmgaming.aneko.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PetdexManifestTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `maps Petdex manifest entries to installable Codex skins`() {
        val manifest = json.decodeFromString<PetdexManifest>(
            """
            {
              "generatedAt": "2026-07-17T07:55:51.498Z",
              "total": 1,
              "pets": [{
                "slug": "boba-2",
                "displayName": "Boba",
                "kind": "creature",
                "submittedBy": "railly",
                "spritesheetUrl": "https://assets.petdex.dev/pets/boba-abc123/sprite.webp",
                "petJsonUrl": "https://assets.petdex.dev/pets/boba-abc123/petjson.json",
                "zipUrl": "https://assets.petdex.dev/pets/boba-abc123/zip.zip"
              }]
            }
            """.trimIndent()
        )

        val skin = manifest.pets.single().toSkinCollection()
        assertEquals("codex.boba-2", skin.packageName)
        assertEquals("boba-2", skin.codexPetId)
        assertEquals("railly", skin.author)
        assertEquals(SkinSource.PETDEX, skin.source)
        assertTrue(skin.version.startsWith("petdex:"))
        assertEquals(
            "https://assets.petdex.dev/pets/boba-2/preview.webp",
            skin.image,
        )
    }

    @Test
    fun `accepts Petdex entries without a submitter`() {
        val pet = json.decodeFromString<PetdexPet>(
            """
            {
              "slug": "anonymous-pet",
              "displayName": "Anonymous Pet",
              "kind": "object",
              "spritesheetUrl": "https://assets.petdex.dev/pets/anonymous-abc/sprite.webp",
              "petJsonUrl": "https://assets.petdex.dev/pets/anonymous-abc/petjson.json",
              "zipUrl": "https://assets.petdex.dev/pets/anonymous-abc/zip.zip"
            }
            """.trimIndent()
        )

        assertNull(pet.toSkinCollection().author)
    }
}

package org.nqmgaming.aneko.core.pet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CodexPetContractTest {
    @Test
    fun `infers both supported sprite versions`() {
        assertEquals(1, CodexPetContract.inferVersion(1536, 1872))
        assertEquals(2, CodexPetContract.inferVersion(1536, 2288))
        assertThrows(IllegalArgumentException::class.java) {
            CodexPetContract.inferVersion(1536, 2080)
        }
    }

    @Test
    fun `maps ANeko directions onto Codex v2 look cells`() {
        assertEquals(CodexPetContract.Cell(9, 0), CodexPetContract.lookCell(0))
        assertEquals(CodexPetContract.Cell(9, 2), CodexPetContract.lookCell(45))
        assertEquals(CodexPetContract.Cell(9, 6), CodexPetContract.lookCell(135))
        assertEquals(CodexPetContract.Cell(10, 0), CodexPetContract.lookCell(180))
        assertEquals(CodexPetContract.Cell(10, 2), CodexPetContract.lookCell(225))
        assertEquals(CodexPetContract.Cell(10, 6), CodexPetContract.lookCell(315))
        assertThrows(IllegalArgumentException::class.java) {
            CodexPetContract.lookCell(46)
        }
    }

    @Test
    fun `sanitizes pet id for the skin package name`() {
        assertEquals("codex.my-pet", CodexPetContract.packageName(" My Pet! "))
        assertEquals("codex.pet", CodexPetContract.packageName("测试宠物"))
    }
}

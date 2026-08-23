package org.nqmgaming.aneko.core.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class PetInstanceReconcilerTest {
    private data class PetInstance(val packageName: String, val position: Int)

    @Test
    fun `retains existing instances when a pet is added`() {
        val first = PetInstance("pet.first", position = 10)
        val second = PetInstance("pet.second", position = 20)

        val result = reconcilePetInstances(
            existing = listOf(first, second),
            desiredKeys = listOf("pet.first", "pet.second", "pet.third"),
            keyOf = PetInstance::packageName,
            create = { packageName -> PetInstance(packageName, position = 99) },
        )

        assertSame(first, result.instances[0])
        assertSame(second, result.instances[1])
        assertEquals(99, result.instances[2].position)
        assertEquals(emptyList<PetInstance>(), result.removed)
    }

    @Test
    fun `retains remaining instances when a pet is removed and slots shift`() {
        val first = PetInstance("pet.first", position = 10)
        val second = PetInstance("pet.second", position = 20)
        val third = PetInstance("pet.third", position = 30)

        val result = reconcilePetInstances(
            existing = listOf(first, second, third),
            desiredKeys = listOf("pet.first", "pet.third"),
            keyOf = PetInstance::packageName,
            create = { null },
        )

        assertSame(first, result.instances[0])
        assertSame(third, result.instances[1])
        assertEquals(listOf(second), result.removed)
    }
}

package org.nqmgaming.aneko.core.service

internal data class PetInstanceReconciliation<T>(
    val instances: List<T>,
    val removed: List<T>,
)

internal fun <T, K> reconcilePetInstances(
    existing: List<T>,
    desiredKeys: List<K>,
    keyOf: (T) -> K,
    create: (K) -> T?,
): PetInstanceReconciliation<T> {
    val remaining = existing.toMutableList()
    val instances = desiredKeys.mapNotNull { desiredKey ->
        val existingIndex = remaining.indexOfFirst { instance ->
            keyOf(instance) == desiredKey
        }
        if (existingIndex >= 0) remaining.removeAt(existingIndex) else create(desiredKey)
    }
    return PetInstanceReconciliation(instances = instances, removed = remaining)
}

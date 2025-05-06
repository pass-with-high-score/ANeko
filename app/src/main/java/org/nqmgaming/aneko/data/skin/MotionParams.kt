package org.nqmgaming.aneko.data.skin

import kotlinx.serialization.Serializable

@Serializable
data class MotionParams(
    val acceleration: Int,
    val awakeState: String,
    val decelerationDistance: Int,
    val initialState: String,
    val maxVelocity: Int,
    val moveStatePrefix: String,
    val proximityDistance: Int,
    val wallStatePrefix: String,
    val motion: List<MotionState>
)

@Serializable
data class MotionState(
    val state: String,
    val nextState: String? = null,
    val checkWall: Boolean? = null,
    val checkMove: Boolean? = null,
    val actions: List<Action>
)

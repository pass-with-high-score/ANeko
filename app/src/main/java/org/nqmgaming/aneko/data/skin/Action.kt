package org.nqmgaming.aneko.data.skin

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class Action {
    @Serializable
    @SerialName("item")
    data class Item(
        val type: String = "item",
        val drawable: String,
        val duration: Int
    ) : Action()

    @Serializable
    @SerialName("repeatItem")
    data class RepeatItem(
        val type: String = "repeatItem",
        val repeatCount: Int? = null,
        val actions: List<Action>
    ) : Action()
}

fun Action.getDrawable(): String? {
    return when (this) {
        is Action.Item -> drawable
        is Action.RepeatItem -> actions.firstNotNullOfOrNull { it.getDrawable() }
    }
}

data class DrawableFrame(
    val drawableName: String,
    val durationMillis: Int
)

fun Action.flattenToDrawableFrames(): List<DrawableFrame> {
    return when (this) {
        is Action.Item -> listOf(DrawableFrame(drawable, duration))
        is Action.RepeatItem -> {
            val repeat = repeatCount ?: 1
            val innerFrames = actions.flatMap { it.flattenToDrawableFrames() }
            List(repeat) { innerFrames }.flatten()
        }
    }
}

fun List<Action>.flattenToDrawableFrames(): List<DrawableFrame> {
    return flatMap { it.flattenToDrawableFrames() }
}

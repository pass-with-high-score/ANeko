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

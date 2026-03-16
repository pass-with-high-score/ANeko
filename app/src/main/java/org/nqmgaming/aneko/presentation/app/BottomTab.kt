package org.nqmgaming.aneko.presentation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.nqmgaming.aneko.R

enum class BottomTab(
    @StringRes val title: Int,
    @DrawableRes val icon: Int
) {
    HOME(
        title = R.string.app_name,
        icon = R.drawable.icon,
    ),
    EXPLORE(
        title = R.string.txt_explore,
        icon = R.drawable.ic_explore,
    )
}
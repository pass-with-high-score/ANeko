package org.nqmgaming.aneko.util.extension

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester

@Composable
private fun useAutoFocus(): FocusRequester {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(null) {
        focusRequester.requestFocus()
    }
    return focusRequester
}

@Composable
fun Modifier.autoFocus() = focusRequester(useAutoFocus())
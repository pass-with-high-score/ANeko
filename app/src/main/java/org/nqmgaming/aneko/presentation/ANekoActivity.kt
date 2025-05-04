package org.nqmgaming.aneko.presentation

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.edit
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import org.nqmgaming.aneko.core.service.AnimationService
import org.nqmgaming.aneko.presentation.home.HomeScreen
import org.nqmgaming.aneko.presentation.ui.theme.ANekoTheme

@AndroidEntryPoint
class ANekoActivity : ComponentActivity() {
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences(packageName + "_preferences", MODE_PRIVATE)

        if (prefs.getBoolean(AnimationService.PREF_KEY_ENABLE, false)) {
            startAnimationService()
        }

        setContent {
            val viewModel: AnekoViewModel = hiltViewModel()
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()

            ANekoTheme(
                darkTheme = isDarkTheme,
                dynamicColor = false
            ) {
                HomeScreen()
            }
        }
    }


    private fun startAnimationService() {
        prefs.edit { putBoolean(AnimationService.PREF_KEY_VISIBLE, true) }
        startService(
            Intent(
                this,
                AnimationService::class.java
            ).setAction(AnimationService.ACTION_START)
        )
    }

}
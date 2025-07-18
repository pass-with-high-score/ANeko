package org.nqmgaming.aneko.presentation

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.core.content.edit
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.generated.NavGraphs
import com.ramcosta.composedestinations.generated.destinations.ExploreSkinScreenDestination
import com.ramcosta.composedestinations.generated.destinations.HomeScreenDestination
import com.ramcosta.composedestinations.rememberNavHostEngine
import dagger.hilt.android.AndroidEntryPoint
import org.nqmgaming.aneko.core.service.AnimationService
import org.nqmgaming.aneko.presentation.ui.theme.ANekoTheme

@AndroidEntryPoint
class ANekoActivity : AppCompatActivity() {
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
            val navController = rememberNavController()
            val navHostEngine = rememberNavHostEngine(
                navHostContentAlignment = Alignment.TopCenter,
            )

            val newBackStackEntry by navController.currentBackStackEntryAsState()
            val route = newBackStackEntry?.destination?.route

            ANekoTheme(
                darkTheme = isDarkTheme,
                dynamicColor = false
            ) {
                StandardScaffold(
                    navController = navController,
                    showBottomBar = route in listOf(
                        HomeScreenDestination.route,
                        ExploreSkinScreenDestination.route,
                    ),
                    content = {
                        DestinationsNavHost(
                            navGraph = NavGraphs.root,
                            navController = navController,
                            engine = navHostEngine,
                        )
                    }
                )

            }
        }
    }

    private fun startAnimationService() {
        prefs.edit { putBoolean(AnimationService.PREF_KEY_VISIBLE, true) }
        startService(
            Intent(this, AnimationService::class.java)
                .setAction(AnimationService.ACTION_START)
        )
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
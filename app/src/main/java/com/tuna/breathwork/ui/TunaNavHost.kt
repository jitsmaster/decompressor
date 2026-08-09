package com.tuna.breathwork.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tuna.breathwork.container
import com.tuna.breathwork.data.Preset
import com.tuna.breathwork.data.TechniquesRepository
import com.tuna.breathwork.ui.session.SessionScreen
import com.tuna.breathwork.ui.session.SessionViewModel

object Routes {
    const val HOME = "home"
    const val LIBRARY = "library"
    const val TECHNIQUE = "technique/{id}"
    const val SESSION = "session/{id}?preset={preset}"
    const val SETTINGS = "settings"
    const val HISTORY = "history"

    fun technique(id: String) = "technique/$id"
    fun session(id: String, preset: Preset) = "session/$id?preset=${preset.name}"
}

@Composable
fun TunaNavHost(onCalmNow: () -> Unit) {
    val nav = rememberNavController()
    val context = LocalContext.current
    val appContainer = (context.applicationContext as com.tuna.breathwork.TunaApp).container

    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onCalmNow = onCalmNow,
                onLibrary = { nav.navigate(Routes.LIBRARY) },
                onHistory = { nav.navigate(Routes.HISTORY) },
                onSettings = { nav.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.LIBRARY) {
            LibraryScreen(onBack = { nav.popBackStack() }) { id -> nav.navigate(Routes.technique(id)) }
        }
        composable(
            Routes.TECHNIQUE,
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString("id") ?: return@composable
            TechniqueDetailScreen(
                techniqueId = id,
                onBack = { nav.popBackStack() },
                onStart = { techniqueId, preset -> nav.navigate(Routes.session(techniqueId, preset)) },
            )
        }
        composable(
            Routes.SESSION,
            arguments = listOf(
                navArgument("id") { type = NavType.StringType },
                navArgument("preset") { type = NavType.StringType; defaultValue = Preset.MEDIUM.name },
            ),
        ) { entry ->
            val id = entry.arguments?.getString("id") ?: return@composable
            val preset = runCatching { Preset.valueOf(entry.arguments?.getString("preset") ?: "") }
                .getOrDefault(Preset.MEDIUM)
            val config = TechniquesRepository.withPreset(TechniquesRepository.byId(id), preset)
            val vm: SessionViewModel = viewModel(
                key = "session_${config.id}_${config.cycles}",
                factory = SessionViewModel.Factory(calmNow = false, techniqueId = id, preset = preset),
            )
            SessionScreen(
                viewModel = vm,
                onFinished = { nav.popBackStack(Routes.HOME, inclusive = false) },
                onAborted = { nav.popBackStack() },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { nav.popBackStack() }, settingsStore = appContainer.settingsStore)
        }
        composable(Routes.HISTORY) {
            HistoryScreen(onBack = { nav.popBackStack() }, logStore = appContainer.logStore)
        }
    }
}

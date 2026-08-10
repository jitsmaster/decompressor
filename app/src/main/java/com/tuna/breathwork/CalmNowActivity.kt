package com.tuna.breathwork

import com.tuna.breathwork.TunaApp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tuna.breathwork.data.TechniquesRepository
import com.tuna.breathwork.ui.session.SessionScreen
import com.tuna.breathwork.ui.session.SessionViewModel
import com.tuna.breathwork.ui.theme.NightGradient
import com.tuna.breathwork.ui.theme.TunaTheme

/**
 * The Calm Now emergency path (SPEC D2): a dedicated task with no back stack,
 * launched from the home-screen widget or quick-settings tile. Auto-starts the
 * 2-minute physiological sigh with theta beats — zero choices, zero menus.
 */
class CalmNowActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applySystemBars(
        com.tuna.breathwork.ui.theme.ThemeMode.fromKey(
            kotlinx.coroutines.runBlocking { (applicationContext as TunaApp).container.currentSettings().themeMode }
        )
    )
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
        }
        enableEdgeToEdge()
        setContent {
            val mode by (applicationContext as TunaApp).container.settingsStore.settings.collectAsState(initial = null)
            TunaTheme(themeMode = com.tuna.breathwork.ui.theme.ThemeMode.fromKey(mode?.themeMode)) {
                CalmNowScreen(onFinished = { finish() }, onAborted = { finish() })
            }
        }
    }

    private fun applySystemBars(mode: com.tuna.breathwork.ui.theme.ThemeMode) {
        val dark = when (mode) {
            com.tuna.breathwork.ui.theme.ThemeMode.DARK -> true
            com.tuna.breathwork.ui.theme.ThemeMode.LIGHT -> false
            com.tuna.breathwork.ui.theme.ThemeMode.SYSTEM ->
                (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                    android.content.res.Configuration.UI_MODE_NIGHT_YES
        }
        val style = if (dark) {
            androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        } else {
            androidx.activity.SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        }
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }

    companion object {
        fun intent(context: Context): Intent =
            Intent(context, CalmNowActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
}

@Composable
private fun CalmNowScreen(onFinished: () -> Unit, onAborted: () -> Unit) {
    val vm: SessionViewModel = viewModel(
        key = "calm_now",
        factory = SessionViewModel.Factory(calmNow = true),
    )
    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().background(NightGradient)) {
        SessionScreen(viewModel = vm, onFinished = onFinished, onAborted = onAborted)
    }
}

package com.tuna.breathwork

import com.tuna.breathwork.TunaApp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.activity.enableEdgeToEdge
import com.tuna.breathwork.ui.TunaNavHost
import com.tuna.breathwork.ui.theme.TunaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()
        applySystemBars(
        com.tuna.breathwork.ui.theme.ThemeMode.fromKey(
            kotlinx.coroutines.runBlocking { (applicationContext as TunaApp).container.currentSettings().themeMode }
        )
    )
        setContent {
            val mode by (applicationContext as TunaApp).container.settingsStore.settings.collectAsState(initial = null)
            TunaTheme(themeMode = com.tuna.breathwork.ui.theme.ThemeMode.fromKey(mode?.themeMode)) {
                TunaNavHost(onCalmNow = { startActivity(CalmNowActivity.intent(this)) })
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

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
        }
    }
}

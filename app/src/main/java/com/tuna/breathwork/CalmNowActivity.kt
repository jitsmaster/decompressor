package com.tuna.breathwork

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
        }
        enableEdgeToEdge()
        setContent {
            TunaTheme {
                CalmNowScreen(onFinished = { finish() }, onAborted = { finish() })
            }
        }
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

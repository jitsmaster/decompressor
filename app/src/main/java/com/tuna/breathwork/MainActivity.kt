package com.tuna.breathwork

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.tuna.breathwork.ui.TunaNavHost
import com.tuna.breathwork.ui.theme.TunaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TunaTheme {
                TunaNavHost(onCalmNow = { startActivity(CalmNowActivity.intent(this)) })
            }
        }
    }
}

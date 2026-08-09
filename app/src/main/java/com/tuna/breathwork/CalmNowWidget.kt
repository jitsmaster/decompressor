package com.tuna.breathwork

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

/**
 * Home-screen widget: one tap → Calm Now. The panic path must be reachable in
 * under three seconds with zero thinking (SPEC D2).
 */
class CalmNowWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                val pending = PendingIntent.getActivity(
                    context,
                    0,
                    CalmNowActivity.intent(context),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(ColorProvider(Color(0xFF4FD1C5)))
                        .padding(16.dp)
                        .clickable { pending.send() },
                    verticalAlignment = Alignment.Vertical.CenterVertically,
                    horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                ) {
                    Text(
                        "吐纳",
                        style = TextStyle(color = ColorProvider(Color(0xFF10131A)), fontWeight = FontWeight.Bold),
                    )
                    Text(
                        "Calm Now",
                        style = TextStyle(color = ColorProvider(Color(0xFF10131A)), fontWeight = FontWeight.Bold),
                    )
                    Text(
                        "one tap · 2 min",
                        style = TextStyle(color = ColorProvider(Color(0xFF10131A).copy(alpha = 0.7f))),
                    )
                }
            }
        }
    }
}

class CalmNowWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CalmNowWidget()
}

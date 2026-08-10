package com.tuna.breathwork.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tuna.breathwork.ui.theme.TextMuted

/** Small static taiji mark — used as a soft watermark behind the Calm Now hero. */
@Composable
fun MiniTaiji(modifier: Modifier = Modifier, color: Color = Color.White, alpha: Float = 0.5f) {
    val tint = color.copy(alpha = alpha)
    Canvas(modifier = modifier) {
        val r = size.minDimension / 2
        val c = Offset(size.width / 2, size.height / 2)
        // Light base, dark left half, opposing lobes and dots (same construction as the glyph)
        drawCircle(color = tint, radius = r, center = c)
        drawArc(color = Color.Transparent, startAngle = -90f, sweepAngle = 180f, useCenter = true,
            topLeft = Offset(c.x - r, c.y - r), size = Size(2 * r, 2 * r))
        drawCircle(color = Color.Transparent, radius = r / 2, center = Offset(c.x, c.y - r / 2))
        // Keep it a simple single-tone mark: light disc with a dark crescent reads best at small size
    }
}

/** Section label with airy spacing — subtle hierarchy, not shouty. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(top = 18.dp, bottom = 10.dp)) {
        Text(
            text.uppercase(),
            color = TextMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 2.5.sp,
        )
    }
}

/** Soft pill chip for use-case tags. */
@Composable
fun UseCaseChip(label: String, accent: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(accent.copy(alpha = 0.14f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = accent, fontSize = 11.sp, letterSpacing = 0.5.sp)
    }
}

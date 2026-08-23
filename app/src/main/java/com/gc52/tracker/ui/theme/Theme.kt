package com.gc52.tracker.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Palette keyed to the #52GameChallenge logo blue
val Bg = Color(0xFF0E1420)
val Surface1 = Color(0xFF161F30)
val Surface2 = Color(0xFF1C2840)
val LogoBlue = Color(0xFF2F6FD2)
val LogoBlueLight = Color(0xFF5A97EC)
val Cream = Color(0xFFF3F5F9)
val Muted = Color(0xFF8FA0BC)
val Good = Color(0xFF43C97B)
val Warn = Color(0xFFE7A03C)

val AccentGradient = Brush.horizontalGradient(listOf(LogoBlue, LogoBlueLight))
val CardGradient = Brush.verticalGradient(listOf(Surface2, Surface1))
val BorderGradient = Brush.linearGradient(listOf(LogoBlueLight.copy(alpha = 0.7f), LogoBlue.copy(alpha = 0.15f)))

fun Modifier.gradientCard(radius: Int = 16): Modifier =
    this.clip(RoundedCornerShape(radius.dp))
        .background(CardGradient)
        .border(1.dp, BorderGradient, RoundedCornerShape(radius.dp))

fun Modifier.gradientButton(radius: Int = 24): Modifier =
    this.clip(RoundedCornerShape(radius.dp)).background(AccentGradient)

private val scheme = darkColorScheme(
    primary = LogoBlueLight,
    onPrimary = Color.White,
    secondary = LogoBlue,
    background = Bg,
    onBackground = Cream,
    surface = Surface1,
    onSurface = Cream,
    surfaceVariant = Surface2,
    onSurfaceVariant = Muted,
    outline = Muted.copy(alpha = 0.4f)
)

@Composable
fun GC52Theme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, content = content)
}

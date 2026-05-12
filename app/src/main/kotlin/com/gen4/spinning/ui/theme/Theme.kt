package com.gen4.spinning.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = SpinColors.LightGreen,
    onPrimary = SpinColors.White,
    primaryContainer = SpinColors.LightGreenDark,
    secondary = SpinColors.Blue,
    onSecondary = SpinColors.White,
    background = SpinColors.Background,
    surface = SpinColors.Surface,
    onBackground = SpinColors.TextPrimary,
    onSurface = SpinColors.TextPrimary,
    error = SpinColors.Red,
)

@Composable
fun Gen4SpinningTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content,
    )
}

package com.recipeapp.syntages.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val Light = lightColorScheme(
    primary = Color(0xFFA6461C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBCD),
    onPrimaryContainer = Color(0xFF380D00),
    secondary = Color(0xFF5E6B2F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE1EFA8),
    onSecondaryContainer = Color(0xFF1B2000),
    tertiary = Color(0xFF7A5900),
    tertiaryContainer = Color(0xFFFFDEA2),
    background = Color(0xFFFFF8F4),
    surface = Color(0xFFFFF8F4),
    surfaceVariant = Color(0xFFF5DED5),
    surfaceContainerLow = Color(0xFFFCF1EC),
    surfaceContainer = Color(0xFFF7EBE5),
    surfaceContainerHigh = Color(0xFFF1E5DF),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFFFB596),
    onPrimary = Color(0xFF5A1B00),
    primaryContainer = Color(0xFF853005),
    onPrimaryContainer = Color(0xFFFFDBCD),
    secondary = Color(0xFFC5D38E),
    secondaryContainer = Color(0xFF465219),
    tertiary = Color(0xFFF2BF48),
    tertiaryContainer = Color(0xFF5C4300),
    background = Color(0xFF1A110E),
    surface = Color(0xFF1A110E),
)

@Composable
fun SyntagesTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> Dark
        else -> Light
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

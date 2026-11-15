package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private val FifaDarkColorScheme = darkColorScheme(
    primary = ElectricCobalt,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF17367A),
    onPrimaryContainer = Color(0xFFD8E6FF),
    secondary = RoyalViolet,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF3E1C78),
    onSecondaryContainer = Color(0xFFEDE4FF),
    tertiary = ChampionshipGold,
    onTertiary = ObsidianVoid,
    tertiaryContainer = Color(0xFF3D2C05),
    onTertiaryContainer = Color(0xFFFFE08A),
    background = ObsidianVoid,
    onBackground = TextPrimary,
    surface = MidnightSurface,
    onSurface = TextPrimary,
    surfaceVariant = ElevatedNavy,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = ObsidianVoid,
    surfaceContainerLow = Color(0xFF0B1020),
    surfaceContainer = MidnightSurface,
    surfaceContainerHigh = ElevatedNavy,
    surfaceContainerHighest = Color(0xFF1E294D),
    outline = CardBorder,
    outlineVariant = Color(0xFF1C2647),
    error = CrimsonLoss,
    onError = Color.White
)

private val FifaShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(26.dp)
)

@Composable
fun FifaTournamentTheme(
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = FifaDarkColorScheme,
            typography = Typography,
            shapes = FifaShapes,
            content = content
        )
    }
}

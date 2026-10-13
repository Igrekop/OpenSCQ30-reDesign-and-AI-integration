package com.oppzippy.openscq30.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object ObsidianDimens {
    val RadiusSmall: Dp = 10.dp
    val RadiusMedium: Dp = 16.dp
    val RadiusLarge: Dp = 22.dp
    val RadiusPill: Dp = 999.dp

    val SpacingXxs: Dp = 4.dp
    val SpacingXs: Dp = 8.dp
    val SpacingSm: Dp = 12.dp
    val SpacingMd: Dp = 16.dp
    val SpacingLg: Dp = 24.dp
    val SpacingXl: Dp = 32.dp
}

val CardShapeLarge = RoundedCornerShape(ObsidianDimens.RadiusLarge)
val CardShapeMedium = RoundedCornerShape(ObsidianDimens.RadiusMedium)
val CardShapeSmall = RoundedCornerShape(ObsidianDimens.RadiusSmall)
val PillShape = RoundedCornerShape(ObsidianDimens.RadiusPill)

@Composable
fun obsidianGlassBorder(
    width: Dp = 1.dp,
    alpha: Float = 0.12f,
): BorderStroke {
    val isDark = MaterialTheme.colorScheme.background == backgroundDark
    val borderColor = if (isDark) {
        Color.White.copy(alpha = alpha)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    }
    return BorderStroke(width, borderColor)
}

@Composable
fun Modifier.obsidianSurface(
    shape: Shape = CardShapeMedium,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
    borderWidth: Dp = 1.dp,
): Modifier {
    return this
        .clip(shape)
        .background(backgroundColor, shape)
        .border(borderWidth, borderColor, shape)
}

package com.oppzippy.openscq30.ui.devicesettings.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oppzippy.openscq30.R
import com.oppzippy.openscq30.ui.devicesettings.Screen
import com.oppzippy.openscq30.ui.devicesettings.ScreenInfo
import com.oppzippy.openscq30.ui.devicesettings.composables.Headphone3DViewer
import com.oppzippy.openscq30.ui.theme.CardShapeLarge
import com.oppzippy.openscq30.ui.theme.CardShapeMedium
import com.oppzippy.openscq30.ui.theme.NeonCyan
import com.oppzippy.openscq30.ui.theme.OpenSCQ30Theme
import com.oppzippy.openscq30.ui.theme.PillShape

@Composable
fun CategorySelectionScreen(
    screens: List<ScreenInfo>,
    modelName: String = "Soundcore Life Q30",
    onNavigation: (Screen) -> Unit,
) {
    val equalizerScreen = screens.find {
        it.baseRoute is Screen.SettingsCategory && (it.baseRoute as Screen.SettingsCategory).categoryId == "equalizer"
    }
    val otherScreens = screens.filter { it != equalizerScreen }
    val context = androidx.compose.ui.platform.LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // 1. Interactive 3D Turntable Hero Section
        item {
            Headphone3DViewer(
                modelName = modelName,
                isConnected = true,
                batteryPercent = 90,
            )
        }

        // 2. Assistant Vocal Gemini Launcher Card
        item {
            AssistantVoiceLauncherCard(
                onClick = {
                    context.startActivity(
                        android.content.Intent(
                            context,
                            com.oppzippy.openscq30.features.assistant.ui.VoiceAssistantActivity::class.java,
                        ),
                    )
                },
            )
        }

        // 3. Ambient Sound ANC Mode Quick Switcher
        item {
            QuickAncModeBar()
        }

        // 3. Featured Equalizer Studio Bento Card
        if (equalizerScreen != null) {
            item {
                FeaturedEqualizerCard(
                    onClick = { onNavigation(equalizerScreen.baseRoute) },
                )
            }
        }

        // 4. Other Modules & Features Cards
        item {
            Text(
                text = "CONTRÔLES & RÉGLAGES",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                modifier = Modifier.padding(start = 6.dp, top = 4.dp, bottom = 4.dp),
            )
        }

        items(otherScreens) { screenInfo ->
            CategoryCardItem(
                screenInfo = screenInfo,
                onClick = { onNavigation(screenInfo.baseRoute) },
            )
        }

        item {
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun QuickAncModeBar() {
    var selectedMode by remember { mutableIntStateOf(0) } // 0: ANC, 1: Transparency, 2: Normal

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShapeLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        border = BorderStroke(1.dp, Color(0x1F00E5FF)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "MODE AMBIANT & BRUIT",
                    style = MaterialTheme.typography.labelMedium.copy(
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = when (selectedMode) {
                        0 -> "RÉDUCTION ACTIVE"
                        1 -> "TRANSPARENCE"
                        else -> "STANDARD"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                    ),
                    color = NeonCyan,
                )
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0C101A), PillShape)
                    .border(1.dp, Color(0x1AFFFFFF), PillShape)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                AncModePill(
                    title = "Réduction",
                    iconRes = R.drawable.headphones_24px,
                    isSelected = selectedMode == 0,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedMode = 0 },
                )
                AncModePill(
                    title = "Transparence",
                    iconRes = R.drawable.speaker_24px,
                    isSelected = selectedMode == 1,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedMode = 1 },
                )
                AncModePill(
                    title = "Normal",
                    iconRes = R.drawable.radio_button_checked_24px,
                    isSelected = selectedMode == 2,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedMode = 2 },
                )
            }
        }
    }
}

@Composable
private fun AncModePill(
    title: String,
    iconRes: Int,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) Color(0x3300E5FF) else Color.Transparent,
        animationSpec = tween(250),
        label = "pillBg",
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) NeonCyan else Color.White.copy(alpha = 0.65f),
        animationSpec = tween(250),
        label = "pillContent",
    )

    Row(
        modifier = modifier
            .clip(PillShape)
            .background(backgroundColor)
            .border(
                width = if (isSelected) 1.dp else 0.dp,
                color = if (isSelected) NeonCyan.copy(alpha = 0.5f) else Color.Transparent,
                shape = PillShape,
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = contentColor,
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = title,
            maxLines = 1,
            softWrap = false,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 11.sp,
            ),
            color = contentColor,
        )
    }
}

@Composable
private fun FeaturedEqualizerCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = CardShapeLarge,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0D121F),
        ),
        border = BorderStroke(
            1.dp,
            Brush.horizontalGradient(
                listOf(
                    NeonCyan.copy(alpha = 0.45f),
                    Color(0x2200E5FF),
                    Color(0x11FFFFFF),
                ),
            ),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0x2200E5FF)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.equalizer_24px),
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Column {
                        Text(
                            text = "Studio Égaliseur",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Signature Soundcore • 8 Bandes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Surface(
                    shape = PillShape,
                    color = Color(0x1A00E5FF),
                    border = BorderStroke(1.dp, Color(0x3300E5FF)),
                ) {
                    Text(
                        text = "OUVRIR",
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            fontSize = 10.sp,
                        ),
                        color = NeonCyan,
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Mini Frequency Curve Visualizer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF070A11))
                    .border(1.dp, Color(0x11FFFFFF), RoundedCornerShape(12.dp))
                    .drawBehind {
                        val width = size.width
                        val height = size.height

                        // Grid lines
                        drawLine(
                            color = Color(0x15FFFFFF),
                            start = Offset(0f, height / 2f),
                            end = Offset(width, height / 2f),
                            strokeWidth = 1f,
                        )

                        // Smooth signature curve
                        val path = Path()
                        val fillPath = Path()

                        val points = listOf(
                            Offset(width * 0.05f, height * 0.45f),
                            Offset(width * 0.20f, height * 0.30f),
                            Offset(width * 0.35f, height * 0.50f),
                            Offset(width * 0.55f, height * 0.60f),
                            Offset(width * 0.75f, height * 0.35f),
                            Offset(width * 0.95f, height * 0.25f),
                        )

                        path.moveTo(points.first().x, points.first().y)
                        fillPath.moveTo(points.first().x, height)
                        fillPath.lineTo(points.first().x, points.first().y)

                        for (i in 0 until points.size - 1) {
                            val p0 = points[i]
                            val p1 = points[i + 1]
                            val cx = (p0.x + p1.x) / 2f
                            path.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                            fillPath.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                        }

                        fillPath.lineTo(points.last().x, height)
                        fillPath.close()

                        // Draw gradient fill
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    NeonCyan.copy(alpha = 0.25f),
                                    Color.Transparent,
                                ),
                            ),
                        )

                        // Draw glowing curve line
                        drawPath(
                            path = path,
                            color = NeonCyan,
                            style = Stroke(width = 2.5.dp.toPx()),
                        )

                        // Frequency Node dots
                        points.forEach { pt ->
                            drawCircle(
                                color = Color.White,
                                radius = 2.5.dp.toPx(),
                                center = pt,
                            )
                        }
                    },
            )
        }
    }
}

@Composable
private fun CategoryCardItem(
    screenInfo: ScreenInfo,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = CardShapeMedium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        border = BorderStroke(1.dp, Color(0x12FFFFFF)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(screenInfo.icon),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Text(
                    text = screenInfo.name.translated(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Icon(
                painter = painterResource(R.drawable.arrow_back_24px),
                contentDescription = null,
                modifier = Modifier
                    .size(16.dp)
                    .drawBehind {
                        // Flip horizontally to serve as a forward chevron
                    },
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
private fun AssistantVoiceLauncherCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = CardShapeLarge,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0F1424),
        ),
        border = BorderStroke(
            1.dp,
            Brush.horizontalGradient(
                listOf(
                    Color(0xFF82C8FF).copy(alpha = 0.5f),
                    NeonCyan.copy(alpha = 0.35f),
                    Color(0x12FFFFFF),
                ),
            ),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f),
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0x2A00E5FF)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.headphones_24px),
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Column {
                    Text(
                        text = "Assistant Vocal Gemini",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = Color.White,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Parlez dans vos écouteurs • Sans écran",
                        style = MaterialTheme.typography.bodySmall,
                        color = NeonCyan.copy(alpha = 0.85f),
                    )
                }
            }

            Surface(
                shape = PillShape,
                color = NeonCyan,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "PARLER",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp,
                        ),
                        color = Color.Black,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun PreviewCategorySelectionScreen() {
    OpenSCQ30Theme(dynamicColor = false) {
        CategorySelectionScreen(
            screens = listOf(
                Screen.SettingsCategory("general").screenInfo(),
                Screen.SettingsCategory("equalizer").screenInfo(),
                Screen.SettingsCategory("buttonConfiguration").screenInfo(),
                Screen.QuickPresets.screenInfo,
            ),
            onNavigation = {},
        )
    }
}

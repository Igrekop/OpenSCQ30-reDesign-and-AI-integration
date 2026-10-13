package com.oppzippy.openscq30.ui.deviceselection.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oppzippy.openscq30.R
import com.oppzippy.openscq30.ui.theme.CardShapeMedium
import com.oppzippy.openscq30.ui.theme.NeonCyan
import com.oppzippy.openscq30.ui.theme.OpenSCQ30Theme
import com.oppzippy.openscq30.ui.theme.PillShape

@Composable
fun ConnectToDeviceCard(
    modifier: Modifier = Modifier,
    name: String,
    model: String,
    macAddress: String,
    isDemo: Boolean,
) {
    val isQ30 = model.contains("3028", ignoreCase = true) ||
        model.contains("3004", ignoreCase = true) ||
        name.contains("q30", ignoreCase = true)

    if (isQ30) {
        SoundcoreHeroDeviceCard(
            modifier = modifier,
            name = name,
            model = model,
            macAddress = macAddress,
            isDemo = isDemo,
        )
    } else {
        StandardDeviceCard(
            modifier = modifier,
            name = name,
            model = model,
            macAddress = macAddress,
            isDemo = isDemo,
        )
    }
}

@Composable
fun SoundcoreHeroDeviceCard(
    modifier: Modifier = Modifier,
    name: String,
    model: String,
    macAddress: String,
    isDemo: Boolean,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "heroBreathing")
    val floatOffsetDp by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "heroFloat",
    )
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowPulse",
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0D121F),
        ),
        border = BorderStroke(
            1.5.dp,
            Brush.verticalGradient(
                listOf(
                    NeonCyan.copy(alpha = 0.6f),
                    Color(0x2200E5FF),
                    Color(0x0AFFFFFF),
                ),
            ),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Top badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = PillShape,
                    color = Color(0x2600E5FF),
                    border = BorderStroke(1.dp, Color(0x3300E5FF)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(NeonCyan, CircleShape),
                        )
                        Text(
                            text = "SOUNDCORE Q30 DETECTED",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp,
                                fontSize = 10.sp,
                            ),
                            color = NeonCyan,
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.45f),
                ) {
                    Text(
                        text = macAddress,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Center: 3D Headphone with ambient glow halo and gentle float
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(175.dp),
                contentAlignment = Alignment.Center,
            ) {
                // Cyan Glow Aura
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .graphicsLayer {
                            scaleX = glowPulse
                            scaleY = glowPulse
                        }
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    NeonCyan.copy(alpha = 0.25f),
                                    Color(0x0C00E5FF),
                                    Color.Transparent,
                                ),
                            ),
                            CircleShape,
                        ),
                )

                // Headphone Render
                Image(
                    painter = painterResource(R.drawable.soundcore_q30_perspective),
                    contentDescription = name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(165.dp)
                        .offset(y = floatOffsetDp.dp),
                )
            }

            Spacer(Modifier.height(8.dp))

            // Title & Subtitle
            Text(
                text = name,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp,
                ),
                color = Color.White,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "HYBRID ACTIVE NOISE CANCELLING",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = NeonCyan.copy(alpha = 0.9f),
            )

            Spacer(Modifier.height(16.dp))

            // Connect action button
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = PillShape,
                color = NeonCyan,
                shadowElevation = 8.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.headphones_24px),
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "CONNECTER & RÉGLER",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.2.sp,
                        ),
                        color = Color.Black,
                    )
                }
            }
        }
    }
}

@Composable
private fun StandardDeviceCard(
    modifier: Modifier = Modifier,
    name: String,
    model: String,
    macAddress: String,
    isDemo: Boolean,
) {
    DeviceCardContainer(
        modifier = modifier,
        icon = { DeviceModelIcon(Modifier.size(36.dp), model) },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = model.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.primary,
                    ),
                )
            }
            if (isDemo) {
                Surface(
                    shape = PillShape,
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.padding(start = 8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.demo).uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                        ),
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
            ) {
                Text(
                    text = macAddress,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                )
                Text(
                    text = stringResource(R.string.connected),
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }
        }
    }
}

@Composable
fun AddDeviceModelCard(modifier: Modifier = Modifier, name: String, model: String) {
    DeviceCardContainer(
        modifier = modifier,
        icon = { DeviceModelIcon(Modifier.size(36.dp), model) },
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = model,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun AddDeviceCard(modifier: Modifier = Modifier, name: String, macAddress: String) {
    DeviceCardContainer(
        modifier = modifier,
        icon = {
            Icon(
                modifier = Modifier.size(32.dp),
                painter = painterResource(R.drawable.bluetooth_24px),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(4.dp))
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
        ) {
            Text(
                text = macAddress,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}

@Composable
private fun DeviceCardContainer(
    modifier: Modifier,
    icon: @Composable (modifier: Modifier) -> Unit,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier),
        shape = CardShapeMedium,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f),
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.surfaceContainerHigh,
                                MaterialTheme.colorScheme.surfaceContainerHighest,
                            ),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                // Subtle radial glow backdrop
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            CircleShape,
                        ),
                )
                icon(Modifier.size(36.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                content()
            }
        }
    }
}

@Composable
private fun DeviceModelIcon(modifier: Modifier, model: String) {
    when (model) {
        "SoundcoreA3004" -> HeadphonesIcon(modifier)
        "SoundcoreA3027" -> HeadphonesIcon(modifier)
        "SoundcoreA3028" -> HeadphonesIcon(modifier)
        "SoundcoreA3029" -> HeadphonesIcon(modifier)
        "SoundcoreA3030" -> HeadphonesIcon(modifier)
        "SoundcoreA3031" -> HeadphonesIcon(modifier)
        "SoundcoreA3033" -> HeadphonesIcon(modifier)
        "SoundcoreA3035" -> HeadphonesIcon(modifier)
        "SoundcoreA3040" -> HeadphonesIcon(modifier)
        "SoundcoreA3116" -> SpeakerIcon(modifier)
        "SoundcoreA3926" -> EarbudsIcon(modifier)
        "SoundcoreA3930" -> EarbudsIcon(modifier)
        "SoundcoreA3931" -> EarbudsIcon(modifier)
        "SoundcoreA3933" -> EarbudsIcon(modifier)
        "SoundcoreA3935" -> EarbudsIcon(modifier)
        "SoundcoreA3936" -> EarbudsIcon(modifier)
        "SoundcoreA3939" -> EarbudsIcon(modifier)
        "SoundcoreA3945" -> EarbudsIcon(modifier)
        "SoundcoreA3947" -> EarbudsIcon(modifier)
        "SoundcoreA3948" -> EarbudsIcon(modifier)
        "SoundcoreA3949" -> EarbudsIcon(modifier)
        "SoundcoreA3951" -> EarbudsIcon(modifier)
        "SoundcoreA3955" -> EarbudsIcon(modifier)
        "SoundcoreA3959" -> EarbudsIcon(modifier)
        "SoundcoreDevelopment" -> HeadphonesIcon(modifier)
        else -> HeadphonesIcon(modifier)
    }
}

@Composable
private fun HeadphonesIcon(modifier: Modifier) {
    Icon(
        modifier = modifier,
        painter = painterResource(R.drawable.headphones_24px),
        tint = MaterialTheme.colorScheme.primary,
        contentDescription = stringResource(R.string.headphones),
    )
}

@Composable
private fun EarbudsIcon(modifier: Modifier) {
    Icon(
        modifier = modifier,
        painter = painterResource(R.drawable.earbuds_2_24px),
        tint = MaterialTheme.colorScheme.primary,
        contentDescription = stringResource(R.string.earbuds),
    )
}

@Composable
private fun SpeakerIcon(modifier: Modifier) {
    Icon(
        modifier = modifier,
        painter = painterResource(R.drawable.speaker_24px),
        tint = MaterialTheme.colorScheme.primary,
        contentDescription = stringResource(R.string.audio_speaker),
    )
}

@Preview(showBackground = true)
@Composable
private fun PreviewDeviceCard() {
    OpenSCQ30Theme {
        ConnectToDeviceCard(
            name = "Soundcore Life Q30",
            model = "SoundcoreA3028",
            macAddress = "AC:12:2F:C8:6E:08",
            isDemo = false,
        )
    }
}

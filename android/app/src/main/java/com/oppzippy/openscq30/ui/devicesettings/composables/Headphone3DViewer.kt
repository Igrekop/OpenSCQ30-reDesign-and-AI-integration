package com.oppzippy.openscq30.ui.devicesettings.composables

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oppzippy.openscq30.R
import com.oppzippy.openscq30.ui.theme.NeonCyan
import com.oppzippy.openscq30.ui.theme.PillShape
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sin

data class SoundRipple(
    val id: Long,
    val centerFraction: Offset,
    val creationTime: Long,
)

@Composable
fun Headphone3DViewer(
    modifier: Modifier = Modifier,
    modelName: String = "Soundcore Life Q30",
    isConnected: Boolean = true,
    batteryPercent: Int = 85,
    onSoundPulse: () -> Unit = {},
) {
    var rawDragX by remember { mutableFloatStateOf(0f) }
    var rawDragY by remember { mutableFloatStateOf(0f) }
    var selectedPresetAngle by remember { mutableIntStateOf(0) } // 0: 3D Free, 1: Front, 2: Perspective
    val ripples = remember { mutableStateListOf<SoundRipple>() }
    val scope = rememberCoroutineScope()

    // Smooth physics springs for rotational angles
    val targetAngleY = when (selectedPresetAngle) {
        1 -> 0f
        2 -> 25f
        else -> (rawDragX / 6f).coerceIn(-40f, 40f)
    }
    val targetAngleX = when (selectedPresetAngle) {
        1 -> 0f
        2 -> 5f
        else -> (-rawDragY / 10f).coerceIn(-18f, 18f)
    }

    val animatedAngleY by animateFloatAsState(
        targetValue = targetAngleY,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "angleY",
    )
    val animatedAngleX by animateFloatAsState(
        targetValue = targetAngleX,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "angleX",
    )

    // Gentle floating / breathing animation
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val floatOffsetDp by infiniteTransition.animateFloat(
        initialValue = -7f,
        targetValue = 7f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "floatOffset",
    )
    val ambientPulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "ambientPulse",
    )

    val density = LocalDensity.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F1420),
                        Color(0xFF07090E),
                    ),
                ),
            )
            .border(
                1.dp,
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0x3300E5FF),
                        Color(0x1100E5FF),
                        Color(0x05FFFFFF),
                    ),
                ),
                RoundedCornerShape(26.dp),
            )
            .padding(top = 18.dp, bottom = 14.dp, start = 16.dp, end = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Top status row: Model Title & Glowing Specs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .background(
                                if (isConnected) NeonCyan else Color.Gray,
                                CircleShape,
                            )
                            .drawBehind {
                                if (isConnected) {
                                    drawCircle(
                                        color = NeonCyan.copy(alpha = 0.45f),
                                        radius = size.minDimension * 1.6f,
                                    )
                                }
                            },
                    )
                    Text(
                        text = modelName.uppercase(),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                            fontSize = 14.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "HI-RES AUDIO WIRELESS • HYBRID ANC",
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp,
                    ),
                    color = NeonCyan.copy(alpha = 0.85f),
                )
            }

            // Battery pill with glowing gauge
            Surface(
                shape = PillShape,
                color = Color(0x2200E5FF),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3300E5FF)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.bolt_24px),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = NeonCyan,
                    )
                    Text(
                        text = "$batteryPercent%",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                        ),
                        color = Color.White,
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // 3D Interactive Turntable Viewport
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            selectedPresetAngle = 0
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            rawDragX += dragAmount.x
                            rawDragY += dragAmount.y
                        },
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { offset ->
                            val ripple = SoundRipple(
                                id = System.currentTimeMillis(),
                                centerFraction = Offset(
                                    offset.x / size.width,
                                    offset.y / size.height,
                                ),
                                creationTime = System.currentTimeMillis(),
                            )
                            ripples.add(ripple)
                            onSoundPulse()
                            scope.launch {
                                delay(900)
                                ripples.remove(ripple)
                            }
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            // Ambient Cyan Acoustic Halo Glow
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .graphicsLayer {
                        scaleX = ambientPulseScale
                        scaleY = ambientPulseScale
                    }
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                NeonCyan.copy(alpha = 0.22f),
                                Color(0x0F00E5FF),
                                Color.Transparent,
                            ),
                        ),
                        CircleShape,
                    ),
            )

            // Dynamic Floor Shadow underneath the headphones
            val shadowWidthFraction = 0.65f - (floatOffsetDp / 100f)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = (-14).dp)
                    .fillMaxWidth(shadowWidthFraction.coerceIn(0.45f, 0.85f))
                    .height(24.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.85f),
                                Color.Black.copy(alpha = 0.4f),
                                Color.Transparent,
                            ),
                        ),
                        CircleShape,
                    ),
            )

            // Acoustic Ripple Waves on Touch
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        val currentTime = System.currentTimeMillis()
                        ripples.forEach { ripple ->
                            val progress = ((currentTime - ripple.creationTime) / 800f).coerceIn(0f, 1f)
                            if (progress < 1f) {
                                val center = Offset(
                                    ripple.centerFraction.x * size.width,
                                    ripple.centerFraction.y * size.height,
                                )
                                val radius = progress * 140.dp.toPx()
                                val alpha = (1f - progress) * 0.6f
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            NeonCyan.copy(alpha = alpha * 0.4f),
                                            NeonCyan.copy(alpha = alpha),
                                            Color.Transparent,
                                        ),
                                        center = center,
                                        radius = radius,
                                    ),
                                    center = center,
                                    radius = radius,
                                )
                            }
                        }
                    },
            )

            // The 3D Headphone Object with Spatial Transform
            val frontAlpha = (1f - (abs(animatedAngleY) / 28f)).coerceIn(0f, 1f)
            val perspectiveAlpha = (abs(animatedAngleY) / 22f).coerceIn(0f, 1f)

            Box(
                modifier = Modifier
                    .size(225.dp)
                    .offset { IntOffset(0, (floatOffsetDp * density.density).toInt()) }
                    .graphicsLayer {
                        rotationY = animatedAngleY
                        rotationX = animatedAngleX
                        cameraDistance = 18f * density.density
                    },
                contentAlignment = Alignment.Center,
            ) {
                // Perspective view (mirrored if rotating to left)
                Image(
                    painter = painterResource(R.drawable.soundcore_q30_perspective),
                    contentDescription = modelName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = perspectiveAlpha
                            scaleX = if (animatedAngleY < 0) -1f else 1f
                        },
                )

                // Symmetrical front view
                Image(
                    painter = painterResource(R.drawable.soundcore_q30_front),
                    contentDescription = modelName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = frontAlpha
                        },
                )

                // Specular Light Glare reflection following the rotation
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawWithContent {
                            drawContent()
                            val lightCenter = Offset(
                                size.width * (0.5f - (animatedAngleY / 90f)),
                                size.height * (0.4f + (animatedAngleX / 70f)),
                            )
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.15f),
                                        NeonCyan.copy(alpha = 0.08f),
                                        Color.Transparent,
                                    ),
                                    center = lightCenter,
                                    radius = size.width * 0.45f,
                                ),
                                center = lightCenter,
                                radius = size.width * 0.45f,
                            )
                        },
                )
            }

            // Subtle Hint overlay for gesture interaction
            if (rawDragX == 0f && selectedPresetAngle == 0) {
                Surface(
                    shape = PillShape,
                    color = Color.Black.copy(alpha = 0.65f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF)),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 4.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.update_24px),
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = NeonCyan,
                        )
                        Text(
                            text = "3D DRAG",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                            ),
                            color = Color.White.copy(alpha = 0.85f),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // View Angle Mode Buttons (Pill Selector)
        Row(
            modifier = Modifier
                .background(Color(0xFF141926), PillShape)
                .border(1.dp, Color(0x22FFFFFF), PillShape)
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnglePresetButton(
                title = "3D Libre",
                iconRes = R.drawable.update_24px,
                isSelected = selectedPresetAngle == 0,
                onClick = {
                    selectedPresetAngle = 0
                    rawDragX = 0f
                    rawDragY = 0f
                },
            )
            AnglePresetButton(
                title = "Face",
                iconRes = null,
                isSelected = selectedPresetAngle == 1,
                onClick = {
                    selectedPresetAngle = 1
                },
            )
            AnglePresetButton(
                title = "Studio 3/4",
                iconRes = null,
                isSelected = selectedPresetAngle == 2,
                onClick = {
                    selectedPresetAngle = 2
                },
            )
        }
    }
}

@Composable
private fun AnglePresetButton(
    title: String,
    iconRes: Int?,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val background = if (isSelected) {
        Brush.horizontalGradient(
            listOf(
                NeonCyan.copy(alpha = 0.25f),
                Color(0x3300E5FF),
            ),
        )
    } else {
        Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
    }

    val contentColor = if (isSelected) NeonCyan else Color.White.copy(alpha = 0.6f)

    Row(
        modifier = Modifier
            .clip(PillShape)
            .background(background)
            .border(
                width = if (isSelected) 1.dp else 0.dp,
                color = if (isSelected) NeonCyan.copy(alpha = 0.5f) else Color.Transparent,
                shape = PillShape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (iconRes != null) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = contentColor,
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 11.sp,
            ),
            color = contentColor,
        )
    }
}

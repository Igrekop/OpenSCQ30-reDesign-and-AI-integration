package com.oppzippy.openscq30.features.assistant.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.oppzippy.openscq30.R
import com.oppzippy.openscq30.features.assistant.AssistantState
import com.oppzippy.openscq30.features.assistant.VoiceAssistantController
import com.oppzippy.openscq30.features.assistant.storage.AssistantPreferences
import com.oppzippy.openscq30.ui.theme.CardShapeLarge
import com.oppzippy.openscq30.ui.theme.NeonCyan
import com.oppzippy.openscq30.ui.theme.OpenSCQ30Theme
import com.oppzippy.openscq30.ui.theme.PillShape
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class VoiceAssistantActivity : ComponentActivity() {

    @Inject
    lateinit var controller: VoiceAssistantController

    @Inject
    lateinit var preferences: AssistantPreferences

    private val requestRecordAudioPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                controller.startListeningSession()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            OpenSCQ30Theme(dynamicColor = false) {
                val state by controller.state.collectAsState()
                val currentApiKey by preferences.apiKeyFlow.collectAsState()
                var currentModel by remember { mutableStateOf(preferences.model) }
                var currentVoice by remember { mutableStateOf(preferences.geminiVoice) }
                var useNativeVoice by remember { mutableStateOf(preferences.useNativeVoice) }

                VoiceAssistantOverlayScreen(
                    state = state,
                    currentApiKey = currentApiKey,
                    currentModel = currentModel,
                    currentVoice = currentVoice,
                    useNativeVoice = useNativeVoice,
                    onSaveApiKey = { newKey ->
                        preferences.apiKey = newKey
                    },
                    onSaveModel = { newModel ->
                        preferences.model = newModel
                        currentModel = newModel
                    },
                    onSaveVoice = { newVoice ->
                        preferences.geminiVoice = newVoice
                        currentVoice = newVoice
                    },
                    onToggleNativeVoice = { enabled ->
                        preferences.useNativeVoice = enabled
                        useNativeVoice = enabled
                    },
                    onTriggerMic = {
                        checkPermissionAndStart()
                    },
                    onCancel = {
                        controller.cancelCurrentSession()
                        finish()
                    },
                )
            }
        }

        checkPermissionAndStart()
    }

    private fun checkPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            controller.startListeningSession()
        } else {
            requestRecordAudioPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        controller.cancelCurrentSession()
    }
}

@Composable
fun VoiceAssistantOverlayScreen(
    state: AssistantState,
    currentApiKey: String?,
    currentModel: String,
    currentVoice: String,
    useNativeVoice: Boolean,
    onSaveApiKey: (String) -> Unit,
    onSaveModel: (String) -> Unit,
    onSaveVoice: (String) -> Unit,
    onToggleNativeVoice: (Boolean) -> Unit,
    onTriggerMic: () -> Unit,
    onCancel: () -> Unit,
) {
    var showKeyDialog by remember { mutableStateOf(currentApiKey.isNullOrBlank()) }
    var inputKey by remember { mutableStateOf(currentApiKey.orEmpty()) }
    var selectedModel by remember { mutableStateOf(currentModel) }
    var selectedVoice by remember { mutableStateOf(currentVoice) }
    var isNativeVoiceEnabled by remember { mutableStateOf(useNativeVoice) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f)),
        contentAlignment = Alignment.BottomCenter,
    ) {
        // Scrim outside card: tapping the background dismisses overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onCancel,
                ),
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { /* Intercept and consume all taps inside the card */ },
                ),
            shape = CardShapeLarge,
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF0C101A),
            ),
            border = BorderStroke(
                1.5.dp,
                Brush.verticalGradient(
                    listOf(
                        NeonCyan.copy(alpha = 0.7f),
                        Color(0x2200E5FF),
                        Color(0x08FFFFFF),
                    ),
                ),
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Surface(
                            shape = PillShape,
                            color = Color(0x2200E5FF),
                            border = BorderStroke(1.dp, Color(0x4400E5FF)),
                        ) {
                            Text(
                                text = "SOUNDCORE AI",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp,
                                    fontSize = 10.sp,
                                ),
                                color = NeonCyan,
                            )
                        }

                        Surface(
                            shape = PillShape,
                            color = Color(0x1800E5FF),
                            border = BorderStroke(1.dp, Color(0x3300E5FF)),
                        ) {
                            Text(
                                text = if (isNativeVoiceEnabled) "⚡ Live • $selectedVoice" else selectedModel,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 10.sp,
                                ),
                                color = Color(0xFF82C8FF),
                            )
                        }
                    }

                    Row {
                        IconButton(onClick = { showKeyDialog = !showKeyDialog }) {
                            Icon(
                                painter = painterResource(R.drawable.settings_24px),
                                contentDescription = "Réglages Clé API & Modèle",
                                tint = if (currentApiKey.isNullOrBlank()) Color(0xFFFF5252) else NeonCyan,
                            )
                        }
                        IconButton(onClick = onCancel) {
                            Icon(
                                painter = painterResource(R.drawable.arrow_back_24px),
                                contentDescription = "Fermer",
                                tint = Color.White.copy(alpha = 0.7f),
                            )
                        }
                    }
                }

                // If API Key is missing or user clicked settings icon, show configuration box
                if (showKeyDialog) {
                    Spacer(Modifier.height(12.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF141926))
                            .border(1.dp, Color(0x3300E5FF), RoundedCornerShape(16.dp))
                            .padding(14.dp),
                    ) {
                        Text(
                            text = "CONFIGURATION GEMINI",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                            ),
                            color = NeonCyan,
                        )
                        Spacer(Modifier.height(10.dp))

                        // Live Native Voice Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    text = "VOIX GEMINI LIVE (NATIVE)",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    color = Color.White.copy(alpha = 0.9f),
                                )
                                Text(
                                    text = "Voix officielle expressive de l'app Gemini",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = isNativeVoiceEnabled,
                                onCheckedChange = {
                                    isNativeVoiceEnabled = it
                                    onToggleNativeVoice(it)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = NeonCyan,
                                ),
                            )
                        }

                        if (isNativeVoiceEnabled) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "PERSONNALITÉ VOCALE (VOIX OFFICIELLE) :",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp,
                                ),
                                color = NeonCyan.copy(alpha = 0.9f),
                            )
                            Spacer(Modifier.height(8.dp))

                            val voicesRow1 = listOf(
                                Triple("Puck", "Puck", "Dynamique • Homme"),
                                Triple("Aoede", "Aoede", "Chaleureuse • Femme"),
                            )
                            val voicesRow2 = listOf(
                                Triple("Charon", "Charon", "Posé • Homme"),
                                Triple("Kore", "Kore", "Douce • Femme"),
                            )

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                listOf(voicesRow1, voicesRow2).forEach { voicePair ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        voicePair.forEach { (voiceId, name, subtitle) ->
                                            val isSelected = selectedVoice == voiceId
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = if (isSelected) NeonCyan.copy(alpha = 0.16f) else Color(0xFF090D14),
                                                border = BorderStroke(
                                                    if (isSelected) 1.5.dp else 1.dp,
                                                    if (isSelected) NeonCyan else Color(0x28FFFFFF),
                                                ),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable {
                                                        selectedVoice = voiceId
                                                        onSaveVoice(voiceId)
                                                    },
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .clip(CircleShape)
                                                            .background(if (isSelected) NeonCyan else Color.White.copy(alpha = 0.3f)),
                                                    )
                                                    Column {
                                                        Text(
                                                            text = name,
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                                                fontSize = 12.sp,
                                                            ),
                                                            color = if (isSelected) NeonCyan else Color.White,
                                                        )
                                                        Text(
                                                            text = subtitle,
                                                            style = MaterialTheme.typography.bodySmall.copy(
                                                                fontSize = 10.sp,
                                                            ),
                                                            color = if (isSelected) NeonCyan.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.5f),
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = "MODÈLE LLM :",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                            ),
                            color = Color.White.copy(alpha = 0.7f),
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            val models = listOf(
                                Triple("gemini-3.5-flash-lite", "3.5 Flash-Lite", "Rapide & Stable"),
                                Triple("gemini-3.8-flash", "3.8 Flash", "Avancé"),
                            )
                            models.forEach { (modelId, name, subtitle) ->
                                val isSelected = selectedModel == modelId
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) NeonCyan.copy(alpha = 0.16f) else Color(0xFF090D14),
                                    border = BorderStroke(
                                        if (isSelected) 1.5.dp else 1.dp,
                                        if (isSelected) NeonCyan else Color(0x28FFFFFF),
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            selectedModel = modelId
                                            onSaveModel(modelId)
                                        },
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) NeonCyan else Color.White.copy(alpha = 0.3f)),
                                        )
                                        Column {
                                            Text(
                                                text = name,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                                    fontSize = 12.sp,
                                                ),
                                                color = if (isSelected) NeonCyan else Color.White,
                                            )
                                            Text(
                                                text = subtitle,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontSize = 10.sp,
                                                ),
                                                color = if (isSelected) NeonCyan.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.5f),
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Clé d'API Google AI Studio :",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = inputKey,
                            onValueChange = { inputKey = it },
                            placeholder = { Text("AIzaSy...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF090D14),
                                unfocusedContainerColor = Color(0xFF090D14),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedIndicatorColor = NeonCyan,
                            ),
                        )
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = {
                                onSaveApiKey(inputKey)
                                onSaveModel(selectedModel)
                                onSaveVoice(selectedVoice)
                                onToggleNativeVoice(isNativeVoiceEnabled)
                                showKeyDialog = false
                                onTriggerMic()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            shape = PillShape,
                        ) {
                            Text(
                                text = "ENREGISTRER & PARLER",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Pulsing Center Glowing Orb / Indicator
                AssistantPulsingOrb(
                    state = state,
                    onClick = onTriggerMic,
                )

                Spacer(Modifier.height(16.dp))

                // Status text label
                val statusText = when (state) {
                    AssistantState.Idle -> "Appuyez sur le micro pour parler"
                    AssistantState.PreparingAudio -> "Connexion au micro Bluetooth..."
                    is AssistantState.Listening -> "Écoute en cours..."
                    is AssistantState.Thinking -> "$selectedModel réfléchit..."
                    is AssistantState.Speaking -> "$selectedModel répond dans l'oreille"
                    is AssistantState.Error -> state.message
                }

                Text(
                    text = statusText.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                    ),
                    color = when (state) {
                        is AssistantState.Error -> Color(0xFFFF5252)
                        is AssistantState.Listening -> NeonCyan
                        is AssistantState.Speaking -> Color(0xFF82C8FF)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )

                // Recognized speech / Answer preview
                val liveText = when (state) {
                    is AssistantState.Listening -> state.partialText
                    is AssistantState.Thinking -> state.prompt
                    is AssistantState.Speaking -> state.spokenText
                    else -> ""
                }

                if (liveText.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF111724),
                        border = BorderStroke(1.dp, Color(0x18FFFFFF)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = liveText,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                            ),
                            color = Color.White,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
            }
        }
    }
}

@Composable
private fun AssistantPulsingOrb(
    state: AssistantState,
    onClick: () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orbPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseScale",
    )

    val isListening = state is AssistantState.Listening
    val isSpeaking = state is AssistantState.Speaking
    val isThinking = state is AssistantState.Thinking

    Box(
        modifier = Modifier
            .size(90.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        // Outer glow halo
        Box(
            modifier = Modifier
                .size(80.dp)
                .graphicsLayer {
                    if (isListening || isSpeaking || isThinking) {
                        scaleX = pulseScale
                        scaleY = pulseScale
                    }
                }
                .background(
                    Brush.radialGradient(
                        listOf(
                            when {
                                isListening -> NeonCyan.copy(alpha = 0.45f)
                                isSpeaking -> Color(0xFF82C8FF).copy(alpha = 0.45f)
                                isThinking -> Color(0xFFBCAEFF).copy(alpha = 0.45f)
                                else -> Color(0x22FFFFFF)
                            },
                            Color.Transparent,
                        ),
                    ),
                    CircleShape,
                ),
        )

        // Center orb button
        Surface(
            shape = CircleShape,
            color = when {
                isListening -> NeonCyan
                isSpeaking -> Color(0xFF82C8FF)
                isThinking -> Color(0xFFBCAEFF)
                else -> Color(0xFF161F2E)
            },
            border = BorderStroke(
                2.dp,
                if (isListening || isSpeaking) Color.White else Color(0x33FFFFFF),
            ),
            modifier = Modifier.size(56.dp),
            shadowElevation = 8.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(
                        when {
                            isSpeaking -> R.drawable.volume_down_24px
                            isThinking -> R.drawable.bolt_24px
                            else -> R.drawable.headphones_24px
                        },
                    ),
                    contentDescription = null,
                    tint = if (isListening || isSpeaking || isThinking) Color.Black else NeonCyan,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
    }
}

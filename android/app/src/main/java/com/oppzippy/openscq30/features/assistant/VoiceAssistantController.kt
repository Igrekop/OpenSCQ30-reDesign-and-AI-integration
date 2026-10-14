package com.oppzippy.openscq30.features.assistant

import android.util.Log
import com.oppzippy.openscq30.features.assistant.audio.BluetoothAudioRouter
import com.oppzippy.openscq30.features.assistant.llm.GeminiLlmProvider
import com.oppzippy.openscq30.features.assistant.stt.AndroidSpeechRecognizerProvider
import com.oppzippy.openscq30.features.assistant.stt.SpeechRecognitionEvent
import com.oppzippy.openscq30.features.assistant.tts.TextToSpeechEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

import com.oppzippy.openscq30.features.assistant.live.GeminiLiveVoiceClient
import com.oppzippy.openscq30.features.assistant.storage.AssistantPreferences

sealed class AssistantState {
    data object Idle : AssistantState()
    data object PreparingAudio : AssistantState()
    data class Listening(val partialText: String = "") : AssistantState()
    data class Thinking(val prompt: String) : AssistantState()
    data class Speaking(val prompt: String, val spokenText: String) : AssistantState()
    data class Error(val message: String) : AssistantState()
}

@Singleton
class VoiceAssistantController @Inject constructor(
    private val audioRouter: BluetoothAudioRouter,
    private val speechToText: AndroidSpeechRecognizerProvider,
    private val llmProvider: GeminiLlmProvider,
    private val textToSpeech: TextToSpeechEngine,
    private val geminiLiveVoice: GeminiLiveVoiceClient,
    private val preferences: AssistantPreferences,
) {
    companion object {
        private const val TAG = "VoiceAssistantController"
    }

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var activeJob: Job? = null

    private val _state = MutableStateFlow<AssistantState>(AssistantState.Idle)
    val state = _state.asStateFlow()

    init {
        // Observe TTS state: when it finishes speaking, release audio and return to Idle
        scope.launch {
            textToSpeech.isSpeakingFlow.collect { isSpeaking ->
                val currentState = _state.value
                if (!isSpeaking && currentState is AssistantState.Speaking) {
                    delay(400)
                    Log.d(TAG, "TTS finished speaking, resetting to Idle")
                    audioRouter.releaseAudio()
                    _state.value = AssistantState.Idle
                }
            }
        }
    }

    private var lastPartialText: String = ""

    /**
     * Start voice interaction: routes audio to Bluetooth SCO and begins listening.
     * If assistant is currently speaking, calling this acts as an instant barge-in / interruption.
     */
    fun startListeningSession() {
        cancelCurrentSession()

        _state.value = AssistantState.PreparingAudio
        lastPartialText = ""

        activeJob = scope.launch {
            // 1. Acquire audio focus so music ducks or pauses
            audioRouter.requestAudioFocus(onFocusLoss = {
                cancelCurrentSession()
            })

            // 2. Route input/output to Bluetooth headset (SCO)
            audioRouter.routeToBluetoothSco()

            // Short stabilization delay so audio driver doesn't drop the initial speech buffer
            delay(200)

            // 3. Start speech recognition
            speechToText.startListening(
                language = "fr-FR",
                onEvent = { event ->
                    handleSpeechEvent(event)
                },
            )
        }
    }

    private fun handleSpeechEvent(event: SpeechRecognitionEvent) {
        when (event) {
            is SpeechRecognitionEvent.ReadyForSpeech -> {
                lastPartialText = ""
                _state.value = AssistantState.Listening("")
            }
            is SpeechRecognitionEvent.PartialResult -> {
                lastPartialText = event.text
                _state.value = AssistantState.Listening(event.text)
            }
            is SpeechRecognitionEvent.FinalResult -> {
                val prompt = event.text.trim().ifBlank { lastPartialText.trim() }
                if (prompt.isBlank()) {
                    Log.d(TAG, "Empty speech recognized, returning to idle")
                    cancelCurrentSession()
                } else {
                    processUserPrompt(prompt)
                }
            }
            is SpeechRecognitionEvent.Error -> {
                Log.w(TAG, "STT error: ${event.message} (code ${event.errorCode})")
                // If partial text was captured before the error/timeout, process it!
                if (lastPartialText.isNotBlank()) {
                    Log.d(TAG, "Recovering from partial text: $lastPartialText")
                    processUserPrompt(lastPartialText.trim())
                    return
                }

                // If error is timeout or no match (silence), smoothly return to idle without annoying voice error
                if (event.errorCode == 7 /* ERROR_NO_MATCH */ || event.errorCode == 6 /* ERROR_SPEECH_TIMEOUT */) {
                    _state.value = AssistantState.Idle
                    audioRouter.releaseAudio()
                } else if (_state.value is AssistantState.Listening || _state.value is AssistantState.PreparingAudio) {
                    speakAndFail(event.message)
                }
            }
            SpeechRecognitionEvent.EndOfSpeech -> {
                val currentText = (_state.value as? AssistantState.Listening)?.partialText.orEmpty().ifBlank { lastPartialText }
                _state.value = AssistantState.Thinking(currentText)
            }
        }
    }

    private fun processUserPrompt(prompt: String) {
        _state.value = AssistantState.Thinking(prompt)

        // Gemini Live WebSocket is only supported by Google on 2.0 Live experimental models.
        // On 3.5 and 3.8, ultra-fast streaming + HD local persona voice is used directly, avoiding any 503/timeout hangs.
        val isLiveModel = preferences.model.contains("gemini-2.0") || preferences.model.contains("live")
        if (preferences.useNativeVoice && isLiveModel) {
            val transcriptAccumulator = StringBuilder()
            var hasStartedSpeaking = false

            geminiLiveVoice.speakPrompt(
                prompt = prompt,
                onTextChunk = { chunk ->
                    transcriptAccumulator.append(chunk)
                    if (!hasStartedSpeaking) {
                        hasStartedSpeaking = true
                    }
                    _state.value = AssistantState.Speaking(prompt, transcriptAccumulator.toString())
                },
                onDone = {
                    Log.d(TAG, "Gemini Live voice completed turn")
                    scope.launch {
                        delay(350)
                        if (_state.value is AssistantState.Speaking) {
                            audioRouter.releaseAudio()
                            _state.value = AssistantState.Idle
                        }
                    }
                },
                onError = { error ->
                    Log.w(TAG, "Gemini Live native audio failed ($error), transparently falling back to Flash-Lite + HD TTS")
                    processUserPromptFallback(prompt)
                },
            )
        } else {
            processUserPromptFallback(prompt)
        }
    }

    private fun processUserPromptFallback(prompt: String) {
        activeJob = scope.launch {
            val responseAccumulator = StringBuilder()
            var hasStartedSpeaking = false

            val result = llmProvider.streamCompletion(
                userMessage = prompt,
                onChunk = { chunk ->
                    responseAccumulator.append(chunk)
                    if (!hasStartedSpeaking) {
                        hasStartedSpeaking = true
                        _state.value = AssistantState.Speaking(prompt, responseAccumulator.toString())
                    } else {
                        _state.value = AssistantState.Speaking(prompt, responseAccumulator.toString())
                    }
                    // Ear-first: stream sentences into TTS as they arrive
                    textToSpeech.appendStreamChunk(chunk)
                },
            )

            result.onSuccess {
                textToSpeech.flushStream()
            }.onFailure { ex ->
                Log.e(TAG, "LLM streaming failed", ex)
                speakAndFail(ex.message ?: "Désolé, une erreur s'est produite lors de la connexion.")
            }
        }
    }

    private fun speakAndFail(errorMessage: String) {
        _state.value = AssistantState.Error(errorMessage)
        textToSpeech.speakImmediate(errorMessage)
        scope.launch {
            delay(3500)
            if (_state.value is AssistantState.Error) {
                cancelCurrentSession()
            }
        }
    }

    /**
     * Stop any ongoing speech or recognition immediately and release audio hardware.
     */
    fun cancelCurrentSession() {
        activeJob?.cancel()
        activeJob = null
        speechToText.stopListening()
        textToSpeech.stop()
        geminiLiveVoice.stop()
        audioRouter.releaseAudio()
        _state.value = AssistantState.Idle
    }
}

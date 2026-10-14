package com.oppzippy.openscq30.features.assistant.live

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Base64
import android.util.Log
import com.oppzippy.openscq30.features.assistant.storage.AssistantPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiLiveVoiceClient @Inject constructor(
    private val preferences: AssistantPreferences,
) {
    companion object {
        private const val TAG = "GeminiLiveVoiceClient"
        private const val SAMPLE_RATE = 24000
    }

    private val httpClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS) // Keep-alive for WebSocket
        .build()

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var webSocket: WebSocket? = null
    private var audioTrack: AudioTrack? = null
    private var timeoutJob: Job? = null
    private var hasReceivedAudio = false
    private var isTurnDone = false
    private var isPlaying = false

    /**
     * Start a Gemini Live conversation turn using native audio streaming.
     *
     * @param prompt The transcribed user speech.
     * @param onTextChunk Receives partial transcript text as Gemini speaks.
     * @param onDone Called when the speech has finished playing.
     * @param onError Called if the WebSocket connection fails, enabling immediate fallback to TTS.
     */
    fun speakPrompt(
        prompt: String,
        onTextChunk: (String) -> Unit,
        onDone: () -> Unit,
        onError: (String) -> Unit,
    ) {
        val apiKey = preferences.apiKey
        if (apiKey.isNullOrBlank()) {
            onError("Clé API Gemini manquante")
            return
        }

        stop()
        hasReceivedAudio = false
        isTurnDone = false

        try {
            initAudioTrack()
        } catch (ex: Exception) {
            Log.e(TAG, "Failed to initialize AudioTrack for 24kHz PCM", ex)
            onError("Erreur initialisation audio")
            return
        }

        // Watchdog timeout: if Gemini Live takes more than 3.8s without returning audio,
        // instantly abort and trigger fallback to 3.5-flash-lite + TTS so user is never stuck
        timeoutJob = scope.launch {
            kotlinx.coroutines.delay(3800)
            if (!hasReceivedAudio && !isTurnDone) {
                Log.w(TAG, "Gemini Live watchdog expired (3.8s) with no audio response. Triggering fallback.")
                stop()
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    onError("Délai Live dépassé, bascule automatique")
                }
            }
        }

        val url = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent?key=$apiKey"
        val request = Request.Builder().url(url).build()

        webSocket = httpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connected, sending setup packet")
                val voiceName = preferences.geminiVoice
                val setupPayload = JSONObject().apply {
                    put("setup", JSONObject().apply {
                        put("model", "models/gemini-3.8-live")
                        put("generationConfig", JSONObject().apply {
                            put("responseModalities", JSONArray().put("AUDIO"))
                            put("speechConfig", JSONObject().apply {
                                put("voiceConfig", JSONObject().apply {
                                    put("prebuiltVoiceConfig", JSONObject().apply {
                                        put("voiceName", voiceName)
                                    })
                                })
                            })
                        })
                        put("systemInstruction", JSONObject().apply {
                            put("parts", JSONArray().put(JSONObject().apply {
                                put("text", preferences.systemPrompt)
                            }))
                        })
                    })
                }

                ws.send(setupPayload.toString())
            }

            override fun onMessage(ws: WebSocket, text: String) {
                try {
                    val root = JSONObject(text)

                    // Check for server-side error object
                    if (root.has("error")) {
                        val errorObj = root.getJSONObject("error")
                        val errorMessage = errorObj.optString("message", "Erreur serveur Live")
                        Log.e(TAG, "Gemini Live API returned error: $errorMessage")
                        timeoutJob?.cancel()
                        stop()
                        scope.launch(Dispatchers.Main) {
                            onError(errorMessage)
                        }
                        return
                    }

                    // Check for setupComplete confirmation
                    if (root.has("setupComplete")) {
                        Log.d(TAG, "Setup complete, sending user prompt: $prompt")

                        val clientContent = JSONObject().apply {
                            put("clientContent", JSONObject().apply {
                                put("turns", JSONArray().put(JSONObject().apply {
                                    put("role", "user")
                                    put("parts", JSONArray().put(JSONObject().apply {
                                        put("text", prompt)
                                    }))
                                }))
                                put("turnComplete", true)
                            })
                        }
                        ws.send(clientContent.toString())
                        return
                    }

                    // Process incoming server response
                    if (root.has("serverContent")) {
                        val serverContent = root.getJSONObject("serverContent")
                        if (serverContent.has("modelTurn")) {
                            val modelTurn = serverContent.getJSONObject("modelTurn")
                            val parts = modelTurn.optJSONArray("parts") ?: JSONArray()

                            for (i in 0 until parts.length()) {
                                val part = parts.getJSONObject(i)

                                // Check for text transcript
                                if (part.has("text")) {
                                    val textChunk = part.getString("text")
                                    if (textChunk.isNotBlank()) {
                                        scope.launch(Dispatchers.Main) {
                                            onTextChunk(textChunk)
                                        }
                                    }
                                }

                                // Check for raw PCM audio data
                                if (part.has("inlineData")) {
                                    val inlineData = part.getJSONObject("inlineData")
                                    val base64Data = inlineData.optString("data")
                                    if (base64Data.isNotBlank()) {
                                        hasReceivedAudio = true
                                        timeoutJob?.cancel()
                                        val pcmBytes = Base64.decode(base64Data, Base64.DEFAULT)
                                        playAudioChunk(pcmBytes)
                                    }
                                }
                            }
                        }

                        // Check if model has completed its turn
                        val turnComplete = serverContent.optBoolean("turnComplete", false)
                        if (turnComplete) {
                            Log.d(TAG, "Turn complete from Gemini Live")
                            isTurnDone = true
                            timeoutJob?.cancel()
                            scope.launch(Dispatchers.Main) {
                                onDone()
                            }
                        }
                    }
                } catch (ex: Exception) {
                    Log.e(TAG, "Error parsing Gemini Live WebSocket message", ex)
                }
            }

            override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                Log.w(TAG, "Gemini Live WebSocket closing: code=$code, reason=$reason")
                if (code != 1000 && !hasReceivedAudio && !isTurnDone) {
                    timeoutJob?.cancel()
                    scope.launch(Dispatchers.Main) {
                        onError("Connexion interrompue: $reason ($code)")
                    }
                }
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.w(TAG, "Gemini Live WebSocket failure: ${t.message}, http code: ${response?.code}")
                timeoutJob?.cancel()
                if (!hasReceivedAudio && !isTurnDone) {
                    scope.launch(Dispatchers.Main) {
                        onError(t.message ?: "Échec de connexion Live API")
                    }
                }
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $code $reason")
                if (code != 1000 && !hasReceivedAudio && !isTurnDone) {
                    timeoutJob?.cancel()
                    scope.launch(Dispatchers.Main) {
                        onError("Connexion fermée: $reason ($code)")
                    }
                }
            }
        })
    }

    private fun initAudioTrack() {
        val minBufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANT)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()

        val audioFormat = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(SAMPLE_RATE)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(audioAttributes)
            .setAudioFormat(audioFormat)
            .setBufferSizeInBytes(minBufferSize * 4)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack?.play()
        isPlaying = true
    }

    private fun playAudioChunk(pcmBytes: ByteArray) {
        audioTrack?.let { track ->
            if (track.playState != AudioTrack.PLAYSTATE_PLAYING) {
                track.play()
            }
            track.write(pcmBytes, 0, pcmBytes.size)
        }
    }

    /**
     * Instantly halt audio playback and disconnect (barge-in / interrupt).
     */
    fun stop() {
        try {
            timeoutJob?.cancel()
            timeoutJob = null

            webSocket?.close(1000, "Turn cancelled")
            webSocket = null

            audioTrack?.let { track ->
                try {
                    track.pause()
                    track.flush()
                    track.stop()
                    track.release()
                } catch (ex: Exception) {
                    Log.w(TAG, "Error releasing AudioTrack", ex)
                }
            }
            audioTrack = null
            isPlaying = false
        } catch (ex: Exception) {
            Log.w(TAG, "Error stopping Gemini Live client", ex)
        }
    }
}

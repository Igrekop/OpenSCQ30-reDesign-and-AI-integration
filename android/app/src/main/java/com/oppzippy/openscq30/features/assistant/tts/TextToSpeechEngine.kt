package com.oppzippy.openscq30.features.assistant.tts

import android.content.Context
import android.media.AudioAttributes
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.oppzippy.openscq30.features.assistant.storage.AssistantPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TextToSpeechEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: AssistantPreferences,
) {
    companion object {
        private const val TAG = "TextToSpeechEngine"
    }

    private var textToSpeech: TextToSpeech? = null
    private var isInitialized = false
    private val pendingUtteranceIds = mutableSetOf<String>()

    private val _isSpeakingFlow = MutableStateFlow(false)
    val isSpeakingFlow = _isSpeakingFlow.asStateFlow()

    // Sentence buffering state for ear-first streaming
    private val sentenceBuffer = StringBuilder()

    init {
        initTts()
    }

    private fun initTts() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.let { tts ->
                    val result = tts.setLanguage(Locale.FRANCE)
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.w(TAG, "Locale.FRANCE not fully supported, trying Locale.FRENCH")
                        tts.language = Locale.FRENCH
                    }

                    val audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                    tts.setAudioAttributes(audioAttributes)

                    applyPersonalityVoice()

                    tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            _isSpeakingFlow.value = true
                        }

                        override fun onDone(utteranceId: String?) {
                            synchronized(pendingUtteranceIds) {
                                pendingUtteranceIds.remove(utteranceId)
                                if (pendingUtteranceIds.isEmpty()) {
                                    _isSpeakingFlow.value = false
                                }
                            }
                        }

                        override fun onError(utteranceId: String?) {
                            Log.e(TAG, "TTS utterance error: $utteranceId")
                            synchronized(pendingUtteranceIds) {
                                pendingUtteranceIds.remove(utteranceId)
                                if (pendingUtteranceIds.isEmpty()) {
                                    _isSpeakingFlow.value = false
                                }
                            }
                        }
                    })
                    isInitialized = true
                    Log.d(TAG, "TextToSpeech initialized successfully")
                }
            } else {
                Log.e(TAG, "TextToSpeech initialization failed with status $status")
            }
        }
    }

    /**
     * Apply distinct vocal persona (Puck, Aoede, Charon, Kore) mapped to local offline Metropolitan France voices.
     * Strictly avoids network voices that cause buffer size=0 errors on Android Bluetooth SCO.
     */
    fun applyPersonalityVoice() {
        val tts = textToSpeech ?: return
        val selectedPersonality = preferences.geminiVoice

        try {
            tts.language = Locale.FRANCE
        } catch (_: Exception) {}

        try {
            val allVoices = tts.voices?.toList().orEmpty()
            Log.d(TAG, "Available TTS voices count: ${allVoices.size}")

            // Strictly filter for Metropolitan France (fr-FR), excluding Canada (-ca-), Belgium (-be-), etc.
            val franceVoices = allVoices.filter { voice ->
                val loc = voice.locale
                val isFrenchLanguage = loc.language.equals("fr", ignoreCase = true)
                val isFranceCountry = loc.country.equals("FR", ignoreCase = true) ||
                    voice.name.contains("fr-fr", ignoreCase = true) ||
                    voice.name.contains("fr_fr", ignoreCase = true)
                val isNotForeignAccent = !voice.name.contains("-ca-", ignoreCase = true) &&
                    !loc.country.equals("CA", ignoreCase = true) &&
                    !voice.name.contains("-be-", ignoreCase = true) &&
                    !loc.country.equals("BE", ignoreCase = true)

                isFrenchLanguage && isFranceCountry && isNotForeignAccent
            }

            // Strictly prioritize local offline voices to avoid network buffer errors (size=0)
            val offlineFranceVoices = franceVoices.filter { !it.isNetworkConnectionRequired }
            val candidateVoices = if (offlineFranceVoices.isNotEmpty()) offlineFranceVoices else franceVoices

            val maleVoices = candidateVoices.filter {
                it.name.contains("-fra-", ignoreCase = true) ||
                    it.name.contains("-frc-", ignoreCase = true) ||
                    it.name.contains("male", ignoreCase = true)
            }
            val femaleVoices = candidateVoices.filter {
                it.name.contains("-frb-", ignoreCase = true) ||
                    it.name.contains("-frd-", ignoreCase = true) ||
                    it.name.contains("-fre-", ignoreCase = true) ||
                    it.name.contains("female", ignoreCase = true)
            }

            var chosenVoice = candidateVoices.firstOrNull()
            var pitch = 1.0f
            var rateMultiplier = 1.0f

            when (selectedPersonality) {
                "Puck" -> {
                    // Puck: Homme vif, dynamique et énergique
                    chosenVoice = maleVoices.firstOrNull { it.name.contains("-fra-", ignoreCase = true) }
                        ?: maleVoices.firstOrNull()
                        ?: candidateVoices.firstOrNull()
                    pitch = 1.02f
                    rateMultiplier = 1.14f
                }
                "Aoede" -> {
                    // Aoede: Femme claire, dynamique et agréable
                    chosenVoice = femaleVoices.firstOrNull { it.name.contains("-frb-", ignoreCase = true) }
                        ?: femaleVoices.firstOrNull()
                        ?: candidateVoices.firstOrNull()
                    pitch = 1.12f
                    rateMultiplier = 1.08f
                }
                "Charon" -> {
                    // Charon: Homme grave, calme et posé
                    chosenVoice = maleVoices.firstOrNull { it.name.contains("-frc-", ignoreCase = true) }
                        ?: maleVoices.firstOrNull()
                        ?: candidateVoices.firstOrNull()
                    pitch = 0.78f
                    rateMultiplier = 0.92f
                }
                "Kore" -> {
                    // Kore: Femme douce, chaleureuse et bienveillante
                    chosenVoice = femaleVoices.firstOrNull { it.name.contains("-frd-", ignoreCase = true) }
                        ?: femaleVoices.firstOrNull { it.name.contains("-fre-", ignoreCase = true) }
                        ?: femaleVoices.firstOrNull()
                        ?: candidateVoices.firstOrNull()
                    pitch = 1.22f
                    rateMultiplier = 0.96f
                }
            }

            if (chosenVoice != null) {
                tts.voice = chosenVoice
                Log.d(TAG, "Selected persona voice '$selectedPersonality': ${chosenVoice.name} (offline: ${!chosenVoice.isNetworkConnectionRequired})")
            } else {
                tts.language = Locale.FRANCE
                Log.d(TAG, "Fallback to default Locale.FRANCE for persona '$selectedPersonality'")
            }

            val effectiveRate = (preferences.speechRate * rateMultiplier).coerceIn(0.6f, 2.0f)
            tts.setSpeechRate(effectiveRate)
            tts.setPitch(pitch)
        } catch (ex: Exception) {
            Log.w(TAG, "Failed to apply voice personality: ${ex.message}")
            tts.language = Locale.FRANCE
            tts.setSpeechRate(preferences.speechRate)
            tts.setPitch(1.0f)
        }
    }

    /**
     * Feed incoming streaming chunks from LLM.
     * Automatically extracts complete sentences and starts speaking them in the earphones immediately.
     */
    fun appendStreamChunk(chunk: String) {
        synchronized(sentenceBuffer) {
            sentenceBuffer.append(chunk)

            // Look for sentence terminators (. ! ? : or newline)
            var text = sentenceBuffer.toString()
            var delimiterIndex = findFirstSentenceEnd(text)

            while (delimiterIndex != -1) {
                val sentence = text.substring(0, delimiterIndex + 1).trim()
                text = text.substring(delimiterIndex + 1)
                sentenceBuffer.setLength(0)
                sentenceBuffer.append(text)

                if (sentence.isNotBlank()) {
                    speakSentence(sentence)
                }

                delimiterIndex = findFirstSentenceEnd(text)
            }
        }
    }

    /**
     * Flush any remaining text in the buffer after streaming ends.
     */
    fun flushStream() {
        synchronized(sentenceBuffer) {
            val remaining = sentenceBuffer.toString().trim()
            sentenceBuffer.setLength(0)
            if (remaining.isNotBlank()) {
                speakSentence(remaining)
            }
        }
    }

    /**
     * Speak a complete text immediately, clearing previous queue.
     */
    fun speakImmediate(text: String) {
        stop()
        speakSentence(text)
    }

    private fun speakSentence(sentence: String) {
        if (!isInitialized || textToSpeech == null) {
            Log.w(TAG, "TTS not ready yet, dropping sentence: $sentence")
            return
        }

        applyPersonalityVoice()

        val utteranceId = UUID.randomUUID().toString()
        synchronized(pendingUtteranceIds) {
            pendingUtteranceIds.add(utteranceId)
        }

        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }

        textToSpeech?.speak(sentence, TextToSpeech.QUEUE_ADD, params, utteranceId)
    }

    /**
     * Stop speech playback immediately (for barge-in / user interruption).
     */
    fun stop() {
        synchronized(sentenceBuffer) {
            sentenceBuffer.setLength(0)
        }
        synchronized(pendingUtteranceIds) {
            pendingUtteranceIds.clear()
        }
        textToSpeech?.stop()
        _isSpeakingFlow.value = false
    }

    fun shutdown() {
        stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        isInitialized = false
    }

    private fun findFirstSentenceEnd(text: String): Int {
        for (i in text.indices) {
            val c = text[i]
            if (c == '.' || c == '!' || c == '?' || c == ':' || c == '\n') {
                // Ensure numbers like 3.14 don't trigger premature split
                if (c == '.' && i + 1 < text.length && text[i + 1].isDigit()) {
                    continue
                }
                return i
            }
        }
        return -1
    }
}

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
                    val result = tts.setLanguage(Locale.FRENCH)
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.w(TAG, "French language not supported, using default locale")
                        tts.language = Locale.getDefault()
                    }

                    val audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                    tts.setAudioAttributes(audioAttributes)
                    tts.setSpeechRate(preferences.speechRate)

                    // Auto-select highest quality French neural/network voice available on device
                    try {
                        val frenchVoices = tts.voices?.filter {
                            it.locale.language == Locale.FRENCH.language
                        }.orEmpty()

                        val bestVoice = frenchVoices.firstOrNull {
                            it.name.contains("network", ignoreCase = true) || it.name.contains("neural", ignoreCase = true)
                        } ?: frenchVoices.maxByOrNull { it.quality }

                        if (bestVoice != null) {
                            tts.voice = bestVoice
                            Log.d(TAG, "Selected best French voice: ${bestVoice.name} (quality: ${bestVoice.quality})")
                        }
                    } catch (ex: Exception) {
                        Log.w(TAG, "Could not select advanced voice: ${ex.message}")
                    }

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

        val utteranceId = UUID.randomUUID().toString()
        synchronized(pendingUtteranceIds) {
            pendingUtteranceIds.add(utteranceId)
        }

        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }

        textToSpeech?.setSpeechRate(preferences.speechRate)
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

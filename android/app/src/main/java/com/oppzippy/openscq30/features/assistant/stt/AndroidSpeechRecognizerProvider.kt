package com.oppzippy.openscq30.features.assistant.stt

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidSpeechRecognizerProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : SpeechToTextProvider {

    companion object {
        private const val TAG = "AndroidSpeechRecognizer"
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    override fun startListening(
        language: String,
        onEvent: (SpeechRecognitionEvent) -> Unit,
    ) {
        mainHandler.post {
            try {
                stopListeningInternal()

                if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                    Log.e(TAG, "Speech recognition is not available on this device")
                    onEvent(SpeechRecognitionEvent.Error("Reconnaissance vocale non disponible sur cet appareil", -1))
                    return@post
                }

                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            Log.d(TAG, "onReadyForSpeech")
                            onEvent(SpeechRecognitionEvent.ReadyForSpeech("Prêt à écouter"))
                        }

                        override fun onBeginningOfSpeech() {
                            Log.d(TAG, "onBeginningOfSpeech")
                        }

                        override fun onRmsChanged(rmsdB: Float) {}

                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {
                            Log.d(TAG, "onEndOfSpeech")
                            onEvent(SpeechRecognitionEvent.EndOfSpeech)
                        }

                        override fun onError(error: Int) {
                            val errorMessage = getErrorText(error)
                            Log.w(TAG, "Speech recognition error: $errorMessage ($error)")
                            isListening = false
                            onEvent(SpeechRecognitionEvent.Error(errorMessage, error))
                        }

                        override fun onResults(results: Bundle?) {
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val recognizedText = matches?.firstOrNull().orEmpty()
                            Log.d(TAG, "onResults: $recognizedText")
                            isListening = false
                            onEvent(SpeechRecognitionEvent.FinalResult(recognizedText))
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val partialText = matches?.firstOrNull().orEmpty()
                            if (partialText.isNotBlank()) {
                                Log.d(TAG, "onPartialResults: $partialText")
                                onEvent(SpeechRecognitionEvent.PartialResult(partialText))
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                    // Generous silence thresholds so user can speak naturally without being cut off
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 3000L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
                }

                speechRecognizer?.startListening(intent)
                isListening = true
                Log.d(TAG, "Started speech recognition in language $language")
            } catch (ex: Exception) {
                Log.e(TAG, "Failed to start speech recognition", ex)
                onEvent(SpeechRecognitionEvent.Error("Erreur lors de l'initialisation du micro: ${ex.message}", -1))
            }
        }
    }

    override fun stopListening() {
        mainHandler.post {
            stopListeningInternal()
        }
    }

    private fun stopListeningInternal() {
        if (isListening) {
            try {
                speechRecognizer?.stopListening()
            } catch (ex: Exception) {
                Log.w(TAG, "Error stopping speech recognition", ex)
            }
            isListening = false
        }
    }

    override fun destroy() {
        mainHandler.post {
            try {
                speechRecognizer?.destroy()
                speechRecognizer = null
                isListening = false
            } catch (ex: Exception) {
                Log.w(TAG, "Error destroying speech recognizer", ex)
            }
        }
    }

    private fun getErrorText(errorCode: Int): String = when (errorCode) {
        SpeechRecognizer.ERROR_AUDIO -> "Erreur d'enregistrement audio"
        SpeechRecognizer.ERROR_CLIENT -> "Erreur du client de reconnaissance"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permissions microphone manquantes"
        SpeechRecognizer.ERROR_NETWORK -> "Erreur réseau"
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Délai réseau dépassé"
        SpeechRecognizer.ERROR_NO_MATCH -> "Aucune parole reconnue"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Reconnaissance vocale occupée"
        SpeechRecognizer.ERROR_SERVER -> "Erreur du serveur de reconnaissance"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Aucun son détecté"
        else -> "Erreur de reconnaissance inconnue ($errorCode)"
    }
}

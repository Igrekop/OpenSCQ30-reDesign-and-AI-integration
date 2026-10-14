package com.oppzippy.openscq30.features.assistant.stt

sealed class SpeechRecognitionEvent {
    data class ReadyForSpeech(val info: String = "") : SpeechRecognitionEvent()
    data class PartialResult(val text: String) : SpeechRecognitionEvent()
    data class FinalResult(val text: String) : SpeechRecognitionEvent()
    data class Error(val message: String, val errorCode: Int) : SpeechRecognitionEvent()
    data object EndOfSpeech : SpeechRecognitionEvent()
}

interface SpeechToTextProvider {
    fun startListening(
        language: String = "fr-FR",
        onEvent: (SpeechRecognitionEvent) -> Unit,
    )
    fun stopListening()
    fun destroy()
}

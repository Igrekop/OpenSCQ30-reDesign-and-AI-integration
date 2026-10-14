package com.oppzippy.openscq30.features.assistant.service

import android.os.Bundle
import android.service.voice.VoiceInteractionService
import android.service.voice.VoiceInteractionSession
import android.util.Log

class OpenSCQ30VoiceInteractionService : VoiceInteractionService() {
    companion object {
        private const val TAG = "OpenSCQ30VoiceService"
    }

    override fun onReady() {
        super.onReady()
        Log.d(TAG, "OpenSCQ30VoiceInteractionService is ready")
    }

    override fun onLaunchVoiceAssistFromKeyguard() {
        super.onLaunchVoiceAssistFromKeyguard()
        Log.d(TAG, "Launched voice assist from keyguard (headset button press)")
        showSession(Bundle(), VoiceInteractionSession.SHOW_SOURCE_ASSIST_GESTURE)
    }
}

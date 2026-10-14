package com.oppzippy.openscq30.features.assistant.service

import android.content.Context
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService
import android.util.Log
import com.oppzippy.openscq30.features.assistant.VoiceAssistantController
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class OpenSCQ30VoiceSessionService : VoiceInteractionSessionService() {
    companion object {
        private const val TAG = "VoiceSessionService"
    }

    @Inject
    lateinit var assistantController: VoiceAssistantController

    override fun onNewSession(args: Bundle?): VoiceInteractionSession {
        Log.d(TAG, "Creating new voice interaction session")
        return OpenSCQ30VoiceSession(this, assistantController)
    }
}

class OpenSCQ30VoiceSession(
    context: Context,
    private val assistantController: VoiceAssistantController,
) : VoiceInteractionSession(context) {

    companion object {
        private const val TAG = "OpenSCQ30VoiceSession"
    }

    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        Log.d(TAG, "Voice interaction session shown, starting listening session")
        assistantController.startListeningSession()
    }

    override fun onHide() {
        super.onHide()
        Log.d(TAG, "Voice interaction session hidden")
        assistantController.cancelCurrentSession()
    }
}

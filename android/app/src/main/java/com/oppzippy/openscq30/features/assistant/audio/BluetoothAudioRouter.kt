package com.oppzippy.openscq30.features.assistant.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BluetoothAudioRouter @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val TAG = "BluetoothAudioRouter"
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var isRoutingToBluetooth = false

    /**
     * Request audio focus so background media pauses or ducks while the assistant is active.
     */
    fun requestAudioFocus(onFocusLoss: () -> Unit = {}): Boolean {
        return try {
            val playbackAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(playbackAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener { focusChange ->
                    if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                        Log.d(TAG, "Audio focus lost permanently: $focusChange")
                        onFocusLoss()
                    }
                }
                .build()

            audioFocusRequest = request
            val result = audioManager.requestAudioFocus(request)
            result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } catch (ex: Exception) {
            Log.e(TAG, "Failed to request audio focus", ex)
            false
        }
    }

    /**
     * Route audio input/output to the connected Bluetooth SCO headset (earphones microphone).
     */
    fun routeToBluetoothSco(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val availableDevices = audioManager.availableCommunicationDevices
                val bluetoothScoDevice = availableDevices.find {
                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                        it.type == AudioDeviceInfo.TYPE_BLE_HEADSET
                }

                if (bluetoothScoDevice != null) {
                    val success = audioManager.setCommunicationDevice(bluetoothScoDevice)
                    isRoutingToBluetooth = success
                    Log.d(TAG, "Set communication device to Bluetooth SCO: $success (${bluetoothScoDevice.productName})")
                    success
                } else {
                    Log.w(TAG, "No Bluetooth SCO device found among available communication devices, using default")
                    false
                }
            } else {
                @Suppress("DEPRECATION")
                audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                @Suppress("DEPRECATION")
                audioManager.startBluetoothSco()
                @Suppress("DEPRECATION")
                audioManager.isBluetoothScoOn = true
                isRoutingToBluetooth = true
                Log.d(TAG, "Legacy startBluetoothSco called")
                true
            }
        } catch (ex: Exception) {
            Log.e(TAG, "Failed to route audio to Bluetooth SCO", ex)
            false
        }
    }

    /**
     * Release Bluetooth audio routing and abandon audio focus so normal playback resumes.
     */
    fun releaseAudio() {
        try {
            if (isRoutingToBluetooth) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    audioManager.clearCommunicationDevice()
                } else {
                    @Suppress("DEPRECATION")
                    audioManager.isBluetoothScoOn = false
                    @Suppress("DEPRECATION")
                    audioManager.stopBluetoothSco()
                    @Suppress("DEPRECATION")
                    audioManager.mode = AudioManager.MODE_NORMAL
                }
                isRoutingToBluetooth = false
            }

            audioFocusRequest?.let {
                audioManager.abandonAudioFocusRequest(it)
                audioFocusRequest = null
            }
            Log.d(TAG, "Audio released and focus abandoned")
        } catch (ex: Exception) {
            Log.e(TAG, "Error while releasing audio routing", ex)
        }
    }
}

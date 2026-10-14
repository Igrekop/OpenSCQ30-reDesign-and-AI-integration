package com.oppzippy.openscq30.features.assistant.storage

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssistantPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val TAG = "AssistantPreferences"
        private const val ENCRYPTED_PREFS_NAME = "assistant_secure_prefs"
        private const val PREF_GEMINI_API_KEY = "gemini_api_key"
        private const val PREF_MODEL = "assistant_model"
        private const val PREF_SYSTEM_PROMPT = "assistant_system_prompt"
        private const val PREF_SPEECH_RATE = "assistant_speech_rate"

        const val DEFAULT_MODEL = "gemini-3.8-flash"
        const val DEFAULT_SYSTEM_PROMPT =
            "Tu es l'assistant vocal intelligent de l'utilisateur dans ses écouteurs Soundcore. " +
                "Tes réponses sont destinées à être lues à haute voix dans l'oreille. " +
                "Sois toujours très concis, percutant, chaleureux et naturel. " +
                "N'utilise jamais de syntaxe markdown, d'astérisques ou de puces. " +
                "Formule des phrases courtes et fluides adaptées à la synthèse vocale."
    }

    private val securePrefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                ENCRYPTED_PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (ex: Exception) {
            Log.e(TAG, "Failed to initialize EncryptedSharedPreferences, falling back to private prefs", ex)
            context.getSharedPreferences(ENCRYPTED_PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    private val _apiKeyFlow = MutableStateFlow(apiKey)
    val apiKeyFlow = _apiKeyFlow.asStateFlow()

    var apiKey: String?
        get() = securePrefs.getString(PREF_GEMINI_API_KEY, null)?.takeIf { it.isNotBlank() }
        set(value) {
            securePrefs.edit {
                if (value.isNullOrBlank()) {
                    remove(PREF_GEMINI_API_KEY)
                } else {
                    putString(PREF_GEMINI_API_KEY, value.trim())
                }
            }
            _apiKeyFlow.value = apiKey
        }

    var model: String
        get() {
            val saved = securePrefs.getString(PREF_MODEL, null)
            if (saved == null || saved.startsWith("gemini-1.") || saved.startsWith("gemini-2.")) {
                return DEFAULT_MODEL
            }
            return saved
        }
        set(value) {
            securePrefs.edit { putString(PREF_MODEL, value) }
        }

    var systemPrompt: String
        get() = securePrefs.getString(PREF_SYSTEM_PROMPT, DEFAULT_SYSTEM_PROMPT) ?: DEFAULT_SYSTEM_PROMPT
        set(value) {
            securePrefs.edit { putString(PREF_SYSTEM_PROMPT, value) }
        }

    var speechRate: Float
        get() = securePrefs.getFloat(PREF_SPEECH_RATE, 1.05f)
        set(value) {
            securePrefs.edit { putFloat(PREF_SPEECH_RATE, value) }
        }
}

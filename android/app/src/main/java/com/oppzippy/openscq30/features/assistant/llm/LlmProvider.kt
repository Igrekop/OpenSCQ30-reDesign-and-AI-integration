package com.oppzippy.openscq30.features.assistant.llm

interface LlmProvider {
    suspend fun streamCompletion(
        userMessage: String,
        systemPrompt: String? = null,
        onChunk: (String) -> Unit,
    ): Result<String>
}

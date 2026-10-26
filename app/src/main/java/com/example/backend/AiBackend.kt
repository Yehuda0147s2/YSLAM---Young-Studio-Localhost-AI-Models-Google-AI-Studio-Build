package com.example.backend

import kotlinx.coroutines.flow.Flow

enum class BackendType(val displayName: String) {
    GEMINI("Google Gemini (Cloud)"),
    LOCAL("Localhost (Ollama / llama.cpp)")
}

data class GenerationConfig(
    val temperature: Float = 0.7f,
    val topP: Float = 0.95f,
    val maxTokens: Int = 2048,
    val systemInstruction: String? = null
)

data class MessagePayload(
    val role: String, // "user", "assistant" / "model", "system"
    val content: String
)

data class GenerationRequest(
    val messages: List<MessagePayload>,
    val config: GenerationConfig = GenerationConfig(),
    val modelName: String? = null
)

data class StreamChunk(
    val delta: String,
    val isComplete: Boolean = false,
    val finishReason: String? = null,
    val error: String? = null
)

data class HealthStatus(
    val isHealthy: Boolean,
    val latencyMs: Long,
    val message: String,
    val availableModels: List<String> = emptyList()
)

interface AiBackend {
    val type: BackendType
    val name: String
    suspend fun generateStream(request: GenerationRequest): Flow<StreamChunk>
    suspend fun checkHealth(): HealthStatus
    suspend fun listModels(): List<String>
}

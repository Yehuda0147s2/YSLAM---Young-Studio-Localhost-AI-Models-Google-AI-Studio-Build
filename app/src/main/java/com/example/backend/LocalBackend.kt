package com.example.backend

import com.example.backend.ollama.OllamaChatRequest
import com.example.backend.ollama.OllamaClient
import com.example.backend.ollama.OllamaMessage
import com.example.backend.ollama.OllamaOptions
import kotlinx.coroutines.flow.Flow

/**
 * Retained for backward-compatibility with existing callers and tests.
 */
class LocalServerUnreachableException(endpointUrl: String, cause: Throwable? = null) :
    IllegalStateException(
        "Local inference server at '$endpointUrl' is unreachable. " +
        "Ensure Ollama, llama.cpp, or KoboldCpp is running on your host machine. " +
        "For Android Emulator, use 'http://10.0.2.2:11434'. " +
        "For physical devices, use your workstation's LAN IP (e.g., 'http://192.168.1.X:11434').",
        cause
    )

/**
 * Local AI Backend implementing AiBackend using a Retrofit-based Ollama client.
 * Connects to Ollama / llama.cpp REST endpoints with configurable timeouts,
 * connection error handling, and streaming flows.
 */
class LocalBackend(
    var endpointUrl: String = "http://10.0.2.2:11434",
    var selectedModel: String = ""
) : AiBackend {

    override val type: BackendType = BackendType.LOCAL
    override val name: String = "Localhost (Ollama / llama.cpp)"

    /**
     * Retrofit client instance connected to the specified Ollama endpoint.
     */
    val ollamaClient: OllamaClient = OllamaClient(
        initialEndpointUrl = endpointUrl,
        connectTimeoutSeconds = 8L,
        readTimeoutSeconds = 120L
    )

    fun updateEndpoint(url: String, model: String? = null) {
        val cleaned = url.trim()
        if (cleaned.isEmpty() || !OllamaClient.isValidBaseUrl(cleaned)) {
            return
        }

        endpointUrl = cleaned.removeSuffix("/")
        ollamaClient.updateEndpoint(endpointUrl)
        if (!model.isNullOrBlank()) {
            selectedModel = model
        }
    }

    override suspend fun generateStream(request: GenerationRequest): Flow<StreamChunk> {
        val targetModel = request.modelName ?: selectedModel

        val messagesList = mutableListOf<OllamaMessage>()
        request.config.systemInstruction?.takeIf { it.isNotBlank() }?.let { sys ->
            messagesList.add(OllamaMessage(role = "system", content = sys))
        }
        request.messages.forEach { msg ->
            messagesList.add(OllamaMessage(role = msg.role, content = msg.content))
        }

        val options = OllamaOptions(
            temperature = request.config.temperature,
            topP = request.config.topP,
            numPredict = request.config.maxTokens
        )

        val chatRequest = OllamaChatRequest(
            model = targetModel,
            messages = messagesList,
            stream = true,
            options = options
        )

        return ollamaClient.streamChat(chatRequest)
    }

    override suspend fun checkHealth(): HealthStatus {
        val baseHealth = ollamaClient.checkHealth()
        if (!baseHealth.isHealthy) return baseHealth

        val availableModels = baseHealth.availableModels
        if (availableModels.isEmpty()) {
            return HealthStatus(
                isHealthy = false,
                latencyMs = baseHealth.latencyMs,
                message = "Ollama is running but has no models installed. In Termux: ollama pull qwen2.5-coder:1.5b",
                availableModels = emptyList()
            )
        }

        val modelToCheck = selectedModel.trim()
        if (modelToCheck.isBlank()) {
            return HealthStatus(
                isHealthy = false,
                latencyMs = baseHealth.latencyMs,
                message = "No model selected. Pick one in Settings.",
                availableModels = availableModels
            )
        }

        if (!availableModels.contains(modelToCheck)) {
            return HealthStatus(
                isHealthy = false,
                latencyMs = baseHealth.latencyMs,
                message = "Model '$modelToCheck' is not installed. Available: ${availableModels.joinToString()}. Pull it or switch models.",
                availableModels = availableModels
            )
        }

        val inference = ollamaClient.verifyInference(modelToCheck)
        return if (inference.isHealthy) {
            HealthStatus(
                isHealthy = true,
                latencyMs = baseHealth.latencyMs + inference.latencyMs,
                message = "Connected ($endpointUrl). ${availableModels.size} models. Inference verified in ${inference.latencyMs}ms.",
                availableModels = availableModels
            )
        } else {
            HealthStatus(
                isHealthy = false,
                latencyMs = baseHealth.latencyMs,
                message = inference.message,
                availableModels = availableModels
            )
        }
    }

    override suspend fun listModels(): List<String> {
        return ollamaClient.listModels()
    }
}

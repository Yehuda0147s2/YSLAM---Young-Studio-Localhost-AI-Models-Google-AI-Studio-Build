package com.example.backend

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import kotlin.system.measureTimeMillis

class GeminiBackend : AiBackend {
    override val type: BackendType = BackendType.GEMINI
    override val name: String = "Google Gemini (Cloud)"

    private val defaultModel = "gemini-3.8-flash"

    override suspend fun generateStream(request: GenerationRequest): Flow<StreamChunk> = flow {
        val targetModel = request.modelName ?: defaultModel

        val genConfig = generationConfig {
            temperature = request.config.temperature
            topP = request.config.topP
            maxOutputTokens = request.config.maxTokens
        }

        val systemInstructionContent = request.config.systemInstruction?.takeIf { it.isNotBlank() }?.let { sys ->
            content("system") { text(sys) }
        }

        val generativeModel = Firebase.ai.generativeModel(
            modelName = targetModel,
            generationConfig = genConfig,
            systemInstruction = systemInstructionContent
        )

        try {
            val nonSystemMessages = request.messages.filter { it.role != "system" }

            if (nonSystemMessages.isEmpty()) {
                emit(StreamChunk(delta = "", isComplete = true, error = "Prompt is empty"))
                return@flow
            }

            if (nonSystemMessages.size == 1) {
                // Single prompt (Playground mode or initial chat)
                val prompt = nonSystemMessages.first().content
                withTimeout(60_000L) {
                    generativeModel.generateContentStream(prompt).collect { chunk ->
                        val text = chunk.text.orEmpty()
                        if (text.isNotEmpty()) {
                            emit(StreamChunk(delta = text, isComplete = false))
                        }
                    }
                }
            } else {
                // Multi-turn conversation
                val history = mutableListOf<Content>()
                for (i in 0 until (nonSystemMessages.size - 1)) {
                    val msg = nonSystemMessages[i]
                    val role = if (msg.role == "user") "user" else "model"
                    history.add(content(role) { text(msg.content) })
                }

                val lastPrompt = nonSystemMessages.last().content
                val chat = generativeModel.startChat(history = history)

                withTimeout(60_000L) {
                    chat.sendMessageStream(lastPrompt).collect { chunk ->
                        val text = chunk.text.orEmpty()
                        if (text.isNotEmpty()) {
                            emit(StreamChunk(delta = text, isComplete = false))
                        }
                    }
                }
            }

            emit(StreamChunk(delta = "", isComplete = true))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("GeminiBackend", "Generation issue: ${e.message}")
            val errorMessage = e.localizedMessage ?: "Gemini inference failed"
            emit(StreamChunk(delta = "", isComplete = true, error = errorMessage))
        }
    }

    override suspend fun checkHealth(): HealthStatus {
        return try {
            val probeModel = Firebase.ai.generativeModel(defaultModel)
            var latency = 0L
            withTimeout(15_000L) {
                latency = measureTimeMillis {
                    probeModel.generateContent("ping")
                }
            }
            HealthStatus(
                isHealthy = true,
                latencyMs = latency,
                message = "Connected to Gemini Cloud (${defaultModel})",
                availableModels = listModels()
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            HealthStatus(
                isHealthy = false,
                latencyMs = 0L,
                message = e.localizedMessage ?: "Cannot connect to Gemini API",
                availableModels = listModels()
            )
        }
    }

    override suspend fun listModels(): List<String> {
        return listOf(
            "gemini-3.8-flash",
            "gemini-3.5-flash-lite"
        )
    }
}

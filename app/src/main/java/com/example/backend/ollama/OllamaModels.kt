package com.example.backend.ollama

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OllamaMessage(
    @Json(name = "role") val role: String,
    @Json(name = "content") val content: String
)

@JsonClass(generateAdapter = true)
data class OllamaOptions(
    @Json(name = "temperature") val temperature: Float? = null,
    @Json(name = "top_p") val topP: Float? = null,
    @Json(name = "num_predict") val numPredict: Int? = null
)

@JsonClass(generateAdapter = true)
data class OllamaChatRequest(
    @Json(name = "model") val model: String,
    @Json(name = "messages") val messages: List<OllamaMessage>,
    @Json(name = "stream") val stream: Boolean = true,
    @Json(name = "options") val options: OllamaOptions? = null
)

@JsonClass(generateAdapter = true)
data class OllamaChatResponse(
    @Json(name = "model") val model: String? = null,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "message") val message: OllamaMessage? = null,
    @Json(name = "done") val done: Boolean = false,
    @Json(name = "total_duration") val totalDuration: Long? = null
)

@JsonClass(generateAdapter = true)
data class OllamaGenerateRequest(
    @Json(name = "model") val model: String,
    @Json(name = "prompt") val prompt: String,
    @Json(name = "system") val system: String? = null,
    @Json(name = "stream") val stream: Boolean = true,
    @Json(name = "options") val options: OllamaOptions? = null
)

@JsonClass(generateAdapter = true)
data class OllamaGenerateResponse(
    @Json(name = "model") val model: String? = null,
    @Json(name = "response") val response: String? = null,
    @Json(name = "done") val done: Boolean = false
)

@JsonClass(generateAdapter = true)
data class OllamaModelDetails(
    @Json(name = "format") val format: String? = null,
    @Json(name = "family") val family: String? = null,
    @Json(name = "parameter_size") val parameterSize: String? = null,
    @Json(name = "quantization_level") val quantizationLevel: String? = null
)

@JsonClass(generateAdapter = true)
data class OllamaModelInfo(
    @Json(name = "name") val name: String,
    @Json(name = "model") val model: String? = null,
    @Json(name = "size") val size: Long? = null,
    @Json(name = "digest") val digest: String? = null,
    @Json(name = "details") val details: OllamaModelDetails? = null
)

@JsonClass(generateAdapter = true)
data class OllamaTagsResponse(
    @Json(name = "models") val models: List<OllamaModelInfo> = emptyList()
)

@JsonClass(generateAdapter = true)
data class OllamaVersionResponse(
    @Json(name = "version") val version: String? = null
)

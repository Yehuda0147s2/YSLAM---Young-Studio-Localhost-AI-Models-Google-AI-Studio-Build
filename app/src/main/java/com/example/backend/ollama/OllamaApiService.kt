package com.example.backend.ollama

import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Streaming

/**
 * Retrofit REST service interface for Ollama API endpoints.
 * Reference: https://github.com/ollama/ollama/blob/main/docs/api.md
 */
interface OllamaApiService {

    /**
     * Lists local models available on the Ollama host (GET /api/tags).
     */
    @GET("api/tags")
    suspend fun getTags(): OllamaTagsResponse

    /**
     * Returns the Ollama server version (GET /api/version).
     */
    @GET("api/version")
    suspend fun getVersion(): OllamaVersionResponse

    /**
     * Streams chat completion chunks (POST /api/chat with stream: true).
     * Response is NDJSON (Newline Delimited JSON).
     */
    @Streaming
    @POST("api/chat")
    suspend fun chatStream(
        @Body request: OllamaChatRequest
    ): ResponseBody

    /**
     * Non-streaming chat completion (POST /api/chat with stream: false).
     */
    @POST("api/chat")
    suspend fun chat(
        @Body request: OllamaChatRequest
    ): OllamaChatResponse

    /**
     * Streams raw text/code completion chunks (POST /api/generate with stream: true).
     */
    @Streaming
    @POST("api/generate")
    suspend fun generateStream(
        @Body request: OllamaGenerateRequest
    ): ResponseBody

    /**
     * Non-streaming raw text/code completion (POST /api/generate with stream: false).
     */
    @POST("api/generate")
    suspend fun generate(
        @Body request: OllamaGenerateRequest
    ): OllamaGenerateResponse
}

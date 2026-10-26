package com.example.backend.ollama

import android.util.Log
import com.example.backend.HealthStatus
import com.example.backend.StreamChunk
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

private object SafeLog {
    fun d(tag: String, msg: String) {
        try {
            android.util.Log.d(tag, msg)
        } catch (_: Throwable) {
            println("[$tag] $msg")
        }
    }
    fun i(tag: String, msg: String) {
        try {
            android.util.Log.i(tag, msg)
        } catch (_: Throwable) {
            println("[$tag] $msg")
        }
    }
    fun w(tag: String, msg: String, tr: Throwable? = null) {
        try {
            android.util.Log.w(tag, msg, tr)
        } catch (_: Throwable) {
            println("[$tag] WARN: $msg ${tr?.localizedMessage ?: ""}")
        }
    }
    fun e(tag: String, msg: String, tr: Throwable? = null) {
        try {
            android.util.Log.e(tag, msg, tr)
        } catch (_: Throwable) {
            println("[$tag] ERROR: $msg ${tr?.localizedMessage ?: ""}")
        }
    }
}

/**
 * High-level Retrofit-based client for connecting to local Ollama endpoints.
 * Includes configurable connection and read timeouts, error translation,
 * health status checks, and streaming response parsing.
 */
class OllamaClient(
    initialEndpointUrl: String = DEFAULT_ENDPOINT_URL,
    private val connectTimeoutSeconds: Long = DEFAULT_CONNECT_TIMEOUT_SECONDS,
    private val readTimeoutSeconds: Long = DEFAULT_READ_TIMEOUT_SECONDS,
    private val writeTimeoutSeconds: Long = DEFAULT_WRITE_TIMEOUT_SECONDS
) {

    companion object {
        const val DEFAULT_ENDPOINT_URL = "http://10.0.2.2:11434"
        const val DEFAULT_CONNECT_TIMEOUT_SECONDS = 8L
        const val DEFAULT_READ_TIMEOUT_SECONDS = 120L
        const val DEFAULT_WRITE_TIMEOUT_SECONDS = 30L
        private const val TAG = "OllamaClient"

        fun normalizeBaseUrl(url: String): String {
            val trimmed = url.trim()
            val withScheme = if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
                "http://$trimmed"
            } else {
                trimmed
            }
            return if (withScheme.endsWith("/")) withScheme else "$withScheme/"
        }
    }

    var endpointUrl: String = normalizeBaseUrl(initialEndpointUrl)
        private set

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(connectTimeoutSeconds, TimeUnit.SECONDS)
        .readTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
        .writeTimeout(writeTimeoutSeconds, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor { message ->
            if (message.startsWith("-->") || message.startsWith("<--")) {
                SafeLog.d(TAG, message)
            }
        }.apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    private var retrofit: Retrofit = buildRetrofit(endpointUrl)
    private var apiService: OllamaApiService = retrofit.create(OllamaApiService::class.java)

    private fun buildRetrofit(url: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(url)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    /**
     * Updates the target Ollama host URL and rebuilds the Retrofit service instance.
     */
    @Synchronized
    fun updateEndpoint(newUrl: String) {
        val normalized = normalizeBaseUrl(newUrl)
        if (normalized != endpointUrl) {
            endpointUrl = normalized
            retrofit = buildRetrofit(endpointUrl)
            apiService = retrofit.create(OllamaApiService::class.java)
            SafeLog.i(TAG, "Ollama endpoint updated to: $endpointUrl")
        }
    }

    /**
     * Returns the active Retrofit service instance.
     */
    fun service(): OllamaApiService = apiService

    /**
     * Performs a health check by calling GET /api/tags via Retrofit.
     * Accurately distinguishes between connection timeouts and server unreachable states.
     */
    suspend fun checkHealth(): HealthStatus = withContext(Dispatchers.IO) {
        val targetUrl = endpointUrl.removeSuffix("/")
        try {
            var models: List<OllamaModelInfo> = emptyList()
            val latency = measureTimeMillis {
                val response = apiService.getTags()
                models = response.models
            }

            val modelNames = models.map { it.name }.filter { it.isNotBlank() }
            HealthStatus(
                isHealthy = true,
                latencyMs = latency,
                message = "Connected to Ollama ($targetUrl). ${modelNames.size} local models available.",
                availableModels = if (modelNames.isNotEmpty()) modelNames else listOf("llama3", "codellama", "mistral")
            )
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            val translated = translateOllamaException(t, targetUrl, connectTimeoutSeconds)
            SafeLog.d(TAG, "Health check probe for $targetUrl: ${translated.message}")

            val userMessage = when (translated) {
                is OllamaTimeoutException ->
                    "Connection timed out (${connectTimeoutSeconds}s) connecting to Ollama at '$targetUrl'. " +
                    "Ensure Ollama is running. (On Emulator: http://10.0.2.2:11434)"
                is OllamaUnreachableException ->
                    if (targetUrl.contains("localhost") || targetUrl.contains("127.0.0.1")) {
                        "Cannot reach Ollama at '$targetUrl'. Inside the Android Emulator, use 'http://10.0.2.2:11434' instead of 'localhost'."
                    } else {
                        "Cannot reach Ollama at '$targetUrl'. Check that 'ollama serve' is running on your host machine."
                    }
                is OllamaHttpException ->
                    "Ollama returned HTTP error ${translated.statusCode}: ${translated.errorBody}"
                else ->
                    "Ollama connection check: ${translated.localizedMessage ?: "Unknown connection failure"}"
            }

            HealthStatus(
                isHealthy = false,
                latencyMs = 0L,
                message = userMessage,
                availableModels = emptyList()
            )
        }
    }

    /**
     * Queries available model tags using the Retrofit service.
     */
    suspend fun listModels(): List<String> = withContext(Dispatchers.IO) {
        val health = checkHealth()
        if (health.isHealthy && health.availableModels.isNotEmpty()) {
            health.availableModels
        } else {
            listOf("llama3", "codellama", "mistral", "qwen2.5-coder", "deepseek-coder", "phi3")
        }
    }

    /**
     * Returns the server version using GET /api/version.
     */
    suspend fun getVersion(): String? = withContext(Dispatchers.IO) {
        try {
            apiService.getVersion().version
        } catch (e: Exception) {
            SafeLog.d(TAG, "Could not fetch Ollama version: ${e.message}")
            null
        }
    }

    /**
     * Streams chat responses line-by-line from POST /api/chat.
     * Converts network exceptions into user-friendly StreamChunks with diagnostic tips.
     */
    fun streamChat(request: OllamaChatRequest): Flow<StreamChunk> = flow {
        val targetUrl = endpointUrl.removeSuffix("/")
        try {
            val responseBody = apiService.chatStream(request)
            val inputStream = responseBody.byteStream()
            val reader = BufferedReader(InputStreamReader(inputStream))

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val lineContent = line?.trim().orEmpty()
                if (lineContent.isNotEmpty()) {
                    try {
                        val json = JSONObject(lineContent)
                        val messageObj = json.optJSONObject("message")
                        val delta = messageObj?.optString("content") ?: json.optString("response", "")
                        val isDone = json.optBoolean("done", false)

                        if (delta.isNotEmpty()) {
                            emit(StreamChunk(delta = delta, isComplete = false))
                        }
                        if (isDone) {
                            emit(StreamChunk(delta = "", isComplete = true))
                            break
                        }
                    } catch (e: Exception) {
                        SafeLog.w(TAG, "Failed parsing NDJSON line: $lineContent", e)
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            val translated = translateOllamaException(t, targetUrl, connectTimeoutSeconds)
            SafeLog.w(TAG, "Streaming chat issue for $targetUrl: ${translated.message}")
            emit(StreamChunk(delta = "", isComplete = true, error = translated.message))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Streams raw text/code completions from POST /api/generate.
     */
    fun streamGenerate(request: OllamaGenerateRequest): Flow<StreamChunk> = flow {
        val targetUrl = endpointUrl.removeSuffix("/")
        try {
            val responseBody = apiService.generateStream(request)
            val inputStream = responseBody.byteStream()
            val reader = BufferedReader(InputStreamReader(inputStream))

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val lineContent = line?.trim().orEmpty()
                if (lineContent.isNotEmpty()) {
                    try {
                        val json = JSONObject(lineContent)
                        val delta = json.optString("response", "")
                        val isDone = json.optBoolean("done", false)

                        if (delta.isNotEmpty()) {
                            emit(StreamChunk(delta = delta, isComplete = false))
                        }
                        if (isDone) {
                            emit(StreamChunk(delta = "", isComplete = true))
                            break
                        }
                    } catch (e: Exception) {
                        SafeLog.w(TAG, "Failed parsing NDJSON generate line: $lineContent", e)
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            val translated = translateOllamaException(t, targetUrl, connectTimeoutSeconds)
            SafeLog.w(TAG, "Streaming generate issue for $targetUrl: ${translated.message}")
            emit(StreamChunk(delta = "", isComplete = true, error = translated.message))
        }
    }.flowOn(Dispatchers.IO)
}

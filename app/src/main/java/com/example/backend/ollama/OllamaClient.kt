package com.example.backend.ollama

import android.util.Log
import com.example.backend.HealthStatus
import com.example.backend.StreamChunk
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
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

        fun isValidBaseUrl(url: String): Boolean {
            val trimmed = url.trim()
            if (trimmed.isEmpty() || trimmed.contains(Regex("\\s+"))) {
                return false
            }

            return try {
                val candidate = if (trimmed.contains("://")) {
                    val scheme = trimmed.substringBefore("://").lowercase()
                    if (scheme !in setOf("http", "https")) {
                        return false
                    }
                    trimmed
                } else {
                    "http://$trimmed"
                }

                val uri = java.net.URI(candidate)
                val scheme = uri.scheme?.lowercase()
                val host = uri.host
                scheme in setOf("http", "https") && !host.isNullOrBlank()
            } catch (_: Exception) {
                false
            }
        }

        fun normalizeBaseUrl(url: String): String {
            val trimmed = url.trim()
            if (trimmed.isEmpty()) {
                return DEFAULT_ENDPOINT_URL + "/"
            }

            val withScheme = if (trimmed.contains("://")) {
                trimmed
            } else {
                "http://$trimmed"
            }

            if (!isValidBaseUrl(withScheme)) {
                return DEFAULT_ENDPOINT_URL + "/"
            }

            return if (withScheme.endsWith("/")) withScheme else "$withScheme/"
        }
    }

    var endpointUrl: String = normalizeBaseUrl(initialEndpointUrl)
        private set

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
    private val streamRecordAdapter = moshi.adapter(OllamaStreamRecord::class.java)

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

    internal fun parseChatStreamLine(line: String): StreamChunk {
        val record = parseStreamRecord(line)
        if (record.message == null && !record.done && record.error.isNullOrBlank()) {
            throw OllamaProtocolException("Ollama returned an unexpected chat stream record.")
        }
        return StreamChunk(
            delta = record.message?.content.orEmpty(),
            isComplete = record.done,
            error = record.error?.takeIf { it.isNotBlank() }
        )
    }

    internal fun parseGenerateStreamLine(line: String): StreamChunk {
        val record = parseStreamRecord(line)
        if (record.response == null && !record.done && record.error.isNullOrBlank()) {
            throw OllamaProtocolException("Ollama returned an unexpected generate stream record.")
        }
        return StreamChunk(
            delta = record.response.orEmpty(),
            isComplete = record.done,
            error = record.error?.takeIf { it.isNotBlank() }
        )
    }

    private fun parseStreamRecord(line: String): OllamaStreamRecord {
        return try {
            streamRecordAdapter.fromJson(line)
                ?: throw OllamaProtocolException("Ollama returned an empty stream record.")
        } catch (e: OllamaProtocolException) {
            throw e
        } catch (e: Exception) {
            throw OllamaProtocolException(
                "Ollama returned unparseable data. Version mismatch?",
                e
            )
        }
    }

    internal fun streamFromBody(
        responseBody: ResponseBody,
        parseLine: (String) -> StreamChunk
    ): Flow<StreamChunk> = flow {
        var completed = false
        responseBody.use { body ->
            BufferedReader(InputStreamReader(body.byteStream())).use { reader ->
                while (true) {
                    val line = reader.readLine() ?: break
                    if (line.isBlank()) continue

                    val chunk = parseLine(line)
                    if (chunk.error != null) {
                        emit(StreamChunk(delta = "", isComplete = true, error = chunk.error))
                        completed = true
                        break
                    }
                    if (chunk.delta.isNotEmpty()) {
                        emit(chunk.copy(isComplete = false))
                    }
                    if (chunk.isComplete) {
                        emit(StreamChunk(delta = "", isComplete = true))
                        completed = true
                        break
                    }
                }
            }
        }
        if (!completed) {
            emit(
                StreamChunk(
                    delta = "",
                    isComplete = true,
                    error = "Ollama stream ended before completion; the response may be incomplete."
                )
            )
        }
    }

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
            if (modelNames.isEmpty()) {
                HealthStatus(
                    isHealthy = false,
                    latencyMs = latency,
                    message = "Ollama is running but has no models installed. In Termux: ollama pull qwen2.5-coder:1.5b",
                    availableModels = emptyList()
                )
            } else {
                HealthStatus(
                    isHealthy = true,
                    latencyMs = latency,
                    message = "Connected to Ollama ($targetUrl). ${modelNames.size} local models available.",
                    availableModels = modelNames
                )
            }
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

    suspend fun verifyInference(modelName: String, timeoutSeconds: Long = 15L): HealthStatus = withContext(Dispatchers.IO) {
        val targetModel = modelName.trim()
        val targetUrl = endpointUrl.removeSuffix("/")
        if (targetModel.isBlank()) {
            return@withContext HealthStatus(
                isHealthy = false,
                latencyMs = 0L,
                message = "No model selected. Pick one in Settings.",
                availableModels = emptyList()
            )
        }

        try {
            val startedAt = System.currentTimeMillis()
            val response = withTimeout(TimeUnit.SECONDS.toMillis(timeoutSeconds)) {
                apiService.generate(
                    OllamaGenerateRequest(
                        model = targetModel,
                        prompt = "ping",
                        stream = false,
                        options = OllamaOptions(
                            temperature = 0f,
                            topP = 0.9f,
                            numPredict = 1
                        )
                    )
                )
            }
            val latency = System.currentTimeMillis() - startedAt
            val content = response.response.orEmpty()
            if (content.isBlank()) {
                HealthStatus(
                    isHealthy = false,
                    latencyMs = latency,
                    message = "Model '$targetModel' loaded but returned no output. Check OOM in Termux.",
                    availableModels = emptyList()
                )
            } else {
                HealthStatus(
                    isHealthy = true,
                    latencyMs = latency,
                    message = "Inference verified with '$targetModel' in ${latency}ms",
                    availableModels = emptyList()
                )
            }
        } catch (e: TimeoutCancellationException) {
            HealthStatus(
                isHealthy = false,
                latencyMs = 0L,
                message = "Inference timed out after ${timeoutSeconds}s. Model may be OOM or too large for this device.",
                availableModels = emptyList()
            )
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            val translated = translateOllamaException(t, targetUrl, timeoutSeconds)
            HealthStatus(
                isHealthy = false,
                latencyMs = 0L,
                message = "Inference failed: ${translated.message}",
                availableModels = emptyList()
            )
        }
    }

    /**
     * Queries available model tags using the Retrofit service.
     */
    suspend fun listModels(): List<String> = withContext(Dispatchers.IO) {
        try {
            apiService.getTags().models.map { it.name }.filter { it.isNotBlank() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            SafeLog.w(TAG, "Could not fetch Ollama models: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Returns the server version using GET /api/version.
     */
    suspend fun getVersion(): String? = withContext(Dispatchers.IO) {
        try {
            apiService.getVersion().version
        } catch (e: CancellationException) {
            throw e
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
            emitAll(streamFromBody(apiService.chatStream(request), ::parseChatStreamLine))
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
            emitAll(streamFromBody(apiService.generateStream(request), ::parseGenerateStreamLine))
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            val translated = translateOllamaException(t, targetUrl, connectTimeoutSeconds)
            SafeLog.w(TAG, "Streaming generate issue for $targetUrl: ${translated.message}")
            emit(StreamChunk(delta = "", isComplete = true, error = translated.message))
        }
    }.flowOn(Dispatchers.IO)
}

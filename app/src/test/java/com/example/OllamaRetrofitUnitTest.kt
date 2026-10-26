package com.example

import com.example.backend.BackendType
import com.example.backend.LocalBackend
import com.example.backend.ollama.OllamaChatRequest
import com.example.backend.ollama.OllamaClient
import com.example.backend.ollama.OllamaMessage
import com.example.backend.ollama.OllamaOptions
import com.example.backend.ollama.OllamaTagsResponse
import com.example.backend.ollama.OllamaTimeoutException
import com.example.backend.ollama.OllamaUnreachableException
import com.example.backend.ollama.translateOllamaException
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class OllamaRetrofitUnitTest {

    @Test
    fun testUrlNormalization() {
        assertEquals("http://10.0.2.2:11434/", OllamaClient.normalizeBaseUrl("http://10.0.2.2:11434"))
        assertEquals("http://10.0.2.2:11434/", OllamaClient.normalizeBaseUrl("http://10.0.2.2:11434/"))
        assertEquals("http://localhost:11434/", OllamaClient.normalizeBaseUrl("localhost:11434"))
        assertEquals("https://my-ollama.internal:11434/", OllamaClient.normalizeBaseUrl("https://my-ollama.internal:11434/"))
    }

    @Test
    fun testSocketTimeoutExceptionTranslation() {
        val socketTimeout = SocketTimeoutException("connect timed out")
        val translated = translateOllamaException(
            throwable = socketTimeout,
            endpointUrl = "http://10.0.2.2:11434",
            timeoutSeconds = 8L
        )

        assertTrue(translated is OllamaTimeoutException)
        val timeoutEx = translated as OllamaTimeoutException
        assertEquals(8L, timeoutEx.timeoutSeconds)
        assertEquals("http://10.0.2.2:11434", timeoutEx.endpointUrl)
        assertTrue(timeoutEx.message!!.contains("timed out after 8s"))
        assertTrue(timeoutEx.message!!.contains("http://10.0.2.2:11434"))
    }

    @Test
    fun testConnectExceptionTranslation() {
        val connectException = ConnectException("Failed to connect to /10.0.2.2:11434")
        val translated = translateOllamaException(
            throwable = connectException,
            endpointUrl = "http://10.0.2.2:11434",
            timeoutSeconds = 8L
        )

        assertTrue(translated is OllamaUnreachableException)
        val unreachableEx = translated as OllamaUnreachableException
        assertEquals("http://10.0.2.2:11434", unreachableEx.endpointUrl)
        assertTrue(unreachableEx.message!!.contains("Cannot connect to Ollama server"))
    }

    @Test
    fun testUnknownHostExceptionTranslation() {
        val unknownHost = UnknownHostException("Unable to resolve host: fake.ollama.lan")
        val translated = translateOllamaException(
            throwable = unknownHost,
            endpointUrl = "http://fake.ollama.lan:11434",
            timeoutSeconds = 5L
        )

        assertTrue(translated is OllamaUnreachableException)
    }

    @Test
    fun testMoshiModelsSerialization() {
        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

        val request = OllamaChatRequest(
            model = "llama3",
            messages = listOf(
                OllamaMessage(role = "user", content = "Write a quicksort in Kotlin")
            ),
            stream = true,
            options = OllamaOptions(
                temperature = 0.7f,
                topP = 0.9f,
                numPredict = 1024
            )
        )

        val adapter = moshi.adapter(OllamaChatRequest::class.java)
        val json = adapter.toJson(request)
        assertTrue(json.contains("\"model\":\"llama3\""))
        assertTrue(json.contains("\"messages\":[{\"role\":\"user\""))
        assertTrue(json.contains("\"temperature\":0.7"))

        val parsed = adapter.fromJson(json)
        assertNotNull(parsed)
        assertEquals("llama3", parsed?.model)
        assertEquals(1, parsed?.messages?.size)
        assertEquals("user", parsed?.messages?.first()?.role)
    }

    @Test
    fun testMoshiTagsResponseDeserialization() {
        val json = """
            {
              "models": [
                {
                  "name": "llama3:latest",
                  "model": "llama3:latest",
                  "size": 4661224676,
                  "details": {
                    "format": "gguf",
                    "family": "llama",
                    "parameter_size": "8.0B"
                  }
                },
                {
                  "name": "codellama:7b",
                  "model": "codellama:7b",
                  "size": 3825819648
                }
              ]
            }
        """.trimIndent()

        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

        val adapter = moshi.adapter(OllamaTagsResponse::class.java)
        val response = adapter.fromJson(json)

        assertNotNull(response)
        assertEquals(2, response?.models?.size)
        assertEquals("llama3:latest", response?.models?.get(0)?.name)
        assertEquals("8.0B", response?.models?.get(0)?.details?.parameterSize)
        assertEquals("codellama:7b", response?.models?.get(1)?.name)
    }

    @Test
    fun testOllamaClientAndLocalBackendIntegration() {
        val backend = LocalBackend("http://10.0.2.2:11434", "llama3")
        assertEquals(BackendType.LOCAL, backend.type)
        assertNotNull(backend.ollamaClient)
        assertEquals("http://10.0.2.2:11434/", backend.ollamaClient.endpointUrl)

        backend.updateEndpoint("http://192.168.1.100:11434", "qwen2.5-coder")
        assertEquals("http://192.168.1.100:11434", backend.endpointUrl)
        assertEquals("http://192.168.1.100:11434/", backend.ollamaClient.endpointUrl)
        assertEquals("qwen2.5-coder", backend.selectedModel)
        assertNotNull(backend.ollamaClient.service())
    }
}

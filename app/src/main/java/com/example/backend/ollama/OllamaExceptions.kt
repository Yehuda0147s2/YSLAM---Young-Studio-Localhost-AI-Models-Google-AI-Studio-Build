package com.example.backend.ollama

import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import retrofit2.HttpException

/**
 * Base exception for Ollama network and protocol errors.
 */
open class OllamaException(
    message: String,
    cause: Throwable? = null
) : IOException(message, cause)

/**
 * Thrown when a connection to the local Ollama server times out.
 */
class OllamaTimeoutException(
    val endpointUrl: String,
    val timeoutSeconds: Long,
    cause: Throwable? = null
) : OllamaException(
    message = "Connection timed out after ${timeoutSeconds}s connecting to Ollama at '$endpointUrl'. " +
            "Ensure the Ollama server is running. " +
            "On Android Emulator, use 'http://10.0.2.2:11434'. " +
            "On physical Android devices, use your host PC's Wi-Fi IP (e.g. 'http://192.168.1.X:11434').",
    cause = cause
)

/**
 * Thrown when the local Ollama server cannot be reached (e.g. connection refused, route unreachable).
 */
class OllamaUnreachableException(
    val endpointUrl: String,
    cause: Throwable? = null
) : OllamaException(
    message = if (endpointUrl.contains("localhost") || endpointUrl.contains("127.0.0.1")) {
        "Cannot connect to Ollama server at '$endpointUrl'. Inside an Android environment/emulator, 'localhost' refers to the Android device. Use 'http://10.0.2.2:11434' to connect to Ollama running on your development computer."
    } else {
        "Cannot connect to Ollama server at '$endpointUrl'. Ensure 'ollama serve' is running and accessible on your host machine."
    },
    cause = cause
)

/**
 * Thrown when Ollama returns an HTTP error code (e.g. 404 Model Not Found, 500 Out of Memory).
 */
class OllamaHttpException(
    val statusCode: Int,
    val errorBody: String
) : OllamaException("Ollama HTTP $statusCode error: $errorBody")

class OllamaProtocolException(message: String, cause: Throwable? = null) :
    OllamaException(message, cause)

/**
 * Utility to convert raw network exceptions into structured Ollama exceptions.
 */
fun translateOllamaException(
    throwable: Throwable,
    endpointUrl: String,
    timeoutSeconds: Long = 10L
): Exception {
    return when (throwable) {
        is OllamaException -> throwable
        is SocketTimeoutException -> OllamaTimeoutException(endpointUrl, timeoutSeconds, throwable)
        is ConnectException -> OllamaUnreachableException(endpointUrl, throwable)
        is UnknownHostException -> OllamaUnreachableException(endpointUrl, throwable)
        is HttpException -> {
            val code = throwable.code()
            val errorBody = throwable.response()?.errorBody()?.string().orEmpty()
            OllamaHttpException(code, errorBody)
        }
        is IOException -> {
            val msg = throwable.message.orEmpty().lowercase()
            if (msg.contains("timeout") || msg.contains("timed out")) {
                OllamaTimeoutException(endpointUrl, timeoutSeconds, throwable)
            } else if (msg.contains("refused") || msg.contains("failed to connect") || msg.contains("unreachable")) {
                OllamaUnreachableException(endpointUrl, throwable)
            } else {
                OllamaException("I/O error communicating with Ollama ($endpointUrl): ${throwable.localizedMessage}", throwable)
            }
        }
        else -> OllamaException("Unexpected error communicating with Ollama ($endpointUrl): ${throwable.localizedMessage}", throwable)
    }
}

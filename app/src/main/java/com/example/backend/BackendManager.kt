package com.example.backend

import android.content.Context
import android.content.SharedPreferences
import com.example.backend.ollama.OllamaClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BackendManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("yslam_backend_config", Context.MODE_PRIVATE)

    val geminiBackend = GeminiBackend()
    val localBackend = LocalBackend()

    private val _activeBackendType = MutableStateFlow(
        try {
            BackendType.valueOf(
                prefs.getString("active_backend", BackendType.GEMINI.name) ?: BackendType.GEMINI.name
            )
        } catch (_: Exception) {
            BackendType.GEMINI
        }
    )
    val activeBackendType: StateFlow<BackendType> = _activeBackendType.asStateFlow()

    private val _activeModelName = MutableStateFlow(
        prefs.getString("active_model", "gemini-3.8-flash") ?: "gemini-3.8-flash"
    )
    val activeModelName: StateFlow<String> = _activeModelName.asStateFlow()

    private val _localEndpoint = MutableStateFlow(
        prefs.getString("local_endpoint", "http://10.0.2.2:11434") ?: "http://10.0.2.2:11434"
    )
    val localEndpoint: StateFlow<String> = _localEndpoint.asStateFlow()

    init {
        localBackend.updateEndpoint(_localEndpoint.value, _activeModelName.value)
    }

    fun getActiveBackend(): AiBackend {
        return when (_activeBackendType.value) {
            BackendType.GEMINI -> geminiBackend
            BackendType.LOCAL -> localBackend
        }
    }

    fun setBackend(type: BackendType) {
        _activeBackendType.value = type
        prefs.edit().putString("active_backend", type.name).apply()
        if (type == BackendType.GEMINI && !_activeModelName.value.startsWith("gemini")) {
            setModel("gemini-3.8-flash")
        } else if (type == BackendType.LOCAL && _activeModelName.value.startsWith("gemini")) {
            _activeModelName.value = ""
            prefs.edit().putString("active_model", "").apply()
            localBackend.selectedModel = ""
        }
    }

    fun setModel(model: String) {
        _activeModelName.value = model
        prefs.edit().putString("active_model", model).apply()
        localBackend.updateEndpoint(_localEndpoint.value, model)
    }

    fun setLocalEndpoint(endpoint: String) {
        val cleaned = endpoint.trim()
        if (cleaned.isBlank() || !OllamaClient.isValidBaseUrl(cleaned)) {
            return
        }

        _localEndpoint.value = cleaned
        prefs.edit().putString("local_endpoint", cleaned).apply()
        localBackend.updateEndpoint(cleaned, _activeModelName.value)
    }
}

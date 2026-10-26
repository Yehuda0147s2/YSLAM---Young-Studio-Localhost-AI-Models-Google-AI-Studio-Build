package com.example.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.backend.AiBackend
import com.example.backend.BackendManager
import com.example.backend.BackendType
import com.example.backend.GenerationConfig
import com.example.backend.GenerationRequest
import com.example.backend.HealthStatus
import com.example.backend.MessagePayload
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ChatSessionEntity
import com.example.data.local.SavedSnippetEntity
import com.example.data.local.YslamDatabase
import com.example.data.repository.ChatFirestoreRepository
import com.example.data.repository.UserRepository
import com.example.util.AuthManager
import com.example.util.SecureKeyStorage
import com.example.util.permission.HapticFeedbackHelper
import com.example.util.permission.NotificationHelper
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.system.measureTimeMillis

data class PlaygroundUiState(
    val userPrompt: String = "// Implement an LRU Cache in Kotlin with O(1) operations\nclass LruCache<K, V>(val capacity: Int) {",
    val systemPrompt: String = "You are an expert software engineer. Provide clean, production-ready, idiomatic code with clear explanations and Big-O analysis.",
    val language: String = "kotlin",
    val temperature: Float = 0.4f,
    val topP: Float = 0.95f,
    val maxTokens: Int = 2048,
    val generatedCode: String = "",
    val isGenerating: Boolean = false,
    val error: String? = null,
    val latencyMs: Long? = null
)

class YslamViewModel(application: Application) : AndroidViewModel(application) {
    private val db = YslamDatabase.getDatabase(application)
    private val chatDao = db.chatDao()
    private val snippetDao = db.snippetDao()

    val secureKeyStorage = SecureKeyStorage(application)
    val authManager = AuthManager(application)
    val userRepository = UserRepository(application)
    val chatFirestoreRepository = ChatFirestoreRepository(application)

    val backendManager = BackendManager(application)
    val activeBackend = backendManager.activeBackendType
    val activeModel = backendManager.activeModelName
    val localEndpoint = backendManager.localEndpoint

    val sessions: StateFlow<List<ChatSessionEntity>> = chatDao.getAllSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val snippets: StateFlow<List<SavedSnippetEntity>> = snippetDao.getAllSnippets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSessionId: StateFlow<String?> = _currentSessionId.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessageEntity>>(emptyList())
    val messages: StateFlow<List<ChatMessageEntity>> = _messages.asStateFlow()

    private val _isGeneratingChat = MutableStateFlow(false)
    val isGeneratingChat: StateFlow<Boolean> = _isGeneratingChat.asStateFlow()

    private val _chatStreamingText = MutableStateFlow("")
    val chatStreamingText: StateFlow<String> = _chatStreamingText.asStateFlow()

    private val _chatError = MutableStateFlow<String?>(null)
    val chatError: StateFlow<String?> = _chatError.asStateFlow()

    private val _systemInstruction = MutableStateFlow(
        "You are YSLAM AI, a high-performance code assistant and programmer workspace. You provide idiomatic code, clear explanations, and modern practices."
    )
    val systemInstruction: StateFlow<String> = _systemInstruction.asStateFlow()

    private val _healthStatus = MutableStateFlow<HealthStatus?>(null)
    val healthStatus: StateFlow<HealthStatus?> = _healthStatus.asStateFlow()

    private val _isPinging = MutableStateFlow(false)
    val isPinging: StateFlow<Boolean> = _isPinging.asStateFlow()

    private val _playgroundState = MutableStateFlow(PlaygroundUiState())
    val playgroundState: StateFlow<PlaygroundUiState> = _playgroundState.asStateFlow()

    private var chatGenerationJob: Job? = null
    private var playgroundJob: Job? = null
    private var messageObserveJob: Job? = null
    private var healthMonitorJob: Job? = null

    private fun isRetryable(e: Throwable): Boolean {
        val msg = e.message.orEmpty().lowercase()
        val nonRetryable = listOf(
            "401", "403", "invalid api key", "unauthorized",
            "model not found", "404"
        )
        return nonRetryable.none { msg.contains(it) }
    }

    private suspend fun streamWithRetry(
        backend: AiBackend,
        request: GenerationRequest,
        maxAttempts: Int = 3,
        onAccumulated: (String) -> Unit
    ): Pair<String, Long> {
        val delays = listOf(0L, 2_000L, 5_000L)
        var lastException: Throwable? = null

        for (attempt in 0 until maxAttempts) {
            if (attempt > 0) delay(delays.getOrElse(attempt) { delays.last() })
            try {
                val startedAt = System.currentTimeMillis()
                val buffer = StringBuilder()
                var streamError: String? = null

                backend.generateStream(request).collect { chunk ->
                    if (chunk.error != null) {
                        streamError = chunk.error
                    } else if (chunk.delta.isNotEmpty()) {
                        buffer.append(chunk.delta)
                        onAccumulated(buffer.toString())
                    }
                }

                if (streamError != null) throw Exception(streamError)
                return buffer.toString() to (System.currentTimeMillis() - startedAt)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                lastException = e
                if (!isRetryable(e)) throw e
            }
        }
        throw lastException ?: Exception("Stream failed after $maxAttempts attempts")
    }

    private fun startHealthMonitoring() {
        healthMonitorJob?.cancel()
        healthMonitorJob = viewModelScope.launch {
            while (isActive) {
                delay(5_000)
                if (!_isGeneratingChat.value && !_playgroundState.value.isGenerating) {
                    try {
                        val status = backendManager.getActiveBackend().checkHealth()
                        val current = _healthStatus.value
                        if (current?.isHealthy != status.isHealthy || current?.message != status.message) {
                            _healthStatus.value = status
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        // Silent — next poll will try again.
                    }
                }
            }
        }
    }

    init {
        viewModelScope.launch {
            // Create default session if empty
            val initial = chatDao.getAllSessions()
            initial.collect { sessionList ->
                if (sessionList.isEmpty()) {
                    createNewSession()
                } else if (_currentSessionId.value == null) {
                    selectSession(sessionList.first().id)
                }
            }
        }
        checkBackendHealth()
        startHealthMonitoring()
    }

    fun selectSession(sessionId: String) {
        _currentSessionId.value = sessionId
        messageObserveJob?.cancel()
        messageObserveJob = viewModelScope.launch {
            chatDao.getMessagesForSession(sessionId).collect { list ->
                _messages.value = list
            }
        }
    }

    fun createNewSession(title: String = "Code Session") {
        viewModelScope.launch {
            val newId = UUID.randomUUID().toString()
            val session = ChatSessionEntity(
                id = newId,
                title = title,
                backendType = activeBackend.value.name,
                modelName = activeModel.value
            )
            chatDao.insertSession(session)
            selectSession(newId)
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            chatFirestoreRepository.deleteCloudSession(sessionId)
            chatDao.deleteMessagesForSession(sessionId)
            chatDao.deleteSession(sessionId)
            if (_currentSessionId.value == sessionId) {
                _currentSessionId.value = null
            }
        }
    }

    fun renameSession(sessionId: String, newTitle: String) {
        viewModelScope.launch {
            val session = chatDao.getSessionById(sessionId)
            if (session != null) {
                val updated = session.copy(title = newTitle)
                chatDao.insertSession(updated)
                chatFirestoreRepository.saveSessionToCloud(updated, _messages.value)
            }
        }
    }

    fun syncSessionToCloud(session: ChatSessionEntity) {
        viewModelScope.launch {
            chatFirestoreRepository.saveSessionToCloud(session, _messages.value)
        }
    }

    fun syncUserProfileToFirestore(user: FirebaseUser) {
        viewModelScope.launch {
            userRepository.syncUserProfile(user)
        }
    }

    fun updateSystemInstruction(prompt: String) {
        _systemInstruction.value = prompt
    }

    fun sendChatMessage(prompt: String) {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty() || _isGeneratingChat.value) return

        val sessionId = _currentSessionId.value ?: return

        viewModelScope.launch {
            _chatError.value = null
            _isGeneratingChat.value = true
            _chatStreamingText.value = ""

            val userMsg = ChatMessageEntity(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId,
                role = "user",
                content = trimmed
            )
            chatDao.insertMessage(userMsg)

            // Auto-rename session if it's default
            val currentSession = chatDao.getSessionById(sessionId)
            if (currentSession != null && (currentSession.title == "Code Session" || currentSession.title == "New Chat")) {
                val shortTitle = if (trimmed.length > 28) trimmed.take(28) + "..." else trimmed
                chatDao.insertSession(currentSession.copy(title = shortTitle))
            }

            val history = _messages.value.map {
                MessagePayload(role = if (it.role == "user") "user" else "assistant", content = it.content)
            } + MessagePayload(role = "user", content = trimmed)

            val backend = backendManager.getActiveBackend()
            val request = GenerationRequest(
                messages = history,
                config = GenerationConfig(
                    systemInstruction = _systemInstruction.value
                ),
                modelName = activeModel.value
            )

            val fullResponse = StringBuilder()
            var latency = 0L

            chatGenerationJob = launch {
                try {
                    val (responseText, responseLatency) = streamWithRetry(backend, request) { accumulated ->
                        _chatStreamingText.value = accumulated
                    }
                    latency = responseLatency
                    fullResponse.append(responseText)

                    if (fullResponse.isNotEmpty()) {
                        HapticFeedbackHelper.vibrateSuccess(getApplication())
                        val aiMsg = ChatMessageEntity(
                            id = UUID.randomUUID().toString(),
                            sessionId = sessionId,
                            role = "assistant",
                            content = fullResponse.toString(),
                            modelUsed = activeModel.value,
                            latencyMs = latency
                        )
                        chatDao.insertMessage(aiMsg)

                        val currentSess = chatDao.getSessionById(sessionId)
                        if (currentSess != null && authManager.currentUser.value != null) {
                            val updatedMsgs = _messages.value + aiMsg
                            chatFirestoreRepository.saveSessionToCloud(currentSess, updatedMsgs)
                        }
                    } else if (_chatError.value == null) {
                        _chatError.value = "Model produced no output."
                    }
                } catch (e: CancellationException) {
                    // Preserved cancellation
                } catch (e: Exception) {
                    HapticFeedbackHelper.vibrateError(getApplication())
                    Log.w("YslamViewModel", "Inference issue: ${e.message}")
                    _chatError.value = e.localizedMessage ?: "Generation failed"
                } finally {
                    _isGeneratingChat.value = false
                    _chatStreamingText.value = ""
                }
            }
        }
    }

    fun stopChatGeneration() {
        chatGenerationJob?.cancel()
        _isGeneratingChat.value = false
        _chatStreamingText.value = ""
    }

    // Playground Actions
    fun updatePlaygroundPrompt(prompt: String) {
        _playgroundState.value = _playgroundState.value.copy(userPrompt = prompt)
    }

    fun updatePlaygroundSystemPrompt(sys: String) {
        _playgroundState.value = _playgroundState.value.copy(systemPrompt = sys)
    }

    fun updatePlaygroundLanguage(lang: String) {
        _playgroundState.value = _playgroundState.value.copy(language = lang)
    }

    fun updatePlaygroundParams(temp: Float, topP: Float, maxTokens: Int) {
        _playgroundState.value = _playgroundState.value.copy(
            temperature = temp,
            topP = topP,
            maxTokens = maxTokens
        )
    }

    fun loadPlaygroundTemplate(title: String, prompt: String, lang: String) {
        _playgroundState.value = _playgroundState.value.copy(
            userPrompt = prompt,
            language = lang,
            generatedCode = "",
            error = null
        )
    }

    fun runPlaygroundPrompt() {
        val state = _playgroundState.value
        if (state.userPrompt.isBlank() || state.isGenerating) return

        playgroundJob?.cancel()
        _playgroundState.value = state.copy(isGenerating = true, error = null, generatedCode = "")

        val backend = backendManager.getActiveBackend()
        val request = GenerationRequest(
            messages = listOf(
                MessagePayload(role = "system", content = state.systemPrompt),
                MessagePayload(role = "user", content = state.userPrompt)
            ),
            config = GenerationConfig(
                temperature = state.temperature,
                topP = state.topP,
                maxTokens = state.maxTokens,
                systemInstruction = state.systemPrompt
            ),
            modelName = activeModel.value
        )

        val fullOutput = StringBuilder()
        var latency = 0L

        playgroundJob = viewModelScope.launch {
            try {
                val (responseText, responseLatency) = streamWithRetry(backend, request) { accumulated ->
                    _playgroundState.value = _playgroundState.value.copy(generatedCode = accumulated)
                }
                latency = responseLatency
                fullOutput.append(responseText)
                _playgroundState.value = _playgroundState.value.copy(
                    isGenerating = false,
                    latencyMs = latency
                )
                if (fullOutput.isNotEmpty()) {
                    HapticFeedbackHelper.vibrateSuccess(getApplication())
                    NotificationHelper.sendGenerationCompleteNotification(
                        context = getApplication(),
                        title = "YSLAM Synthesis Complete",
                        message = "Playground code generated in ${state.language} (${latency}ms). Tap to view."
                    )
                }
            } catch (e: CancellationException) {
                // Preserved
            } catch (e: Exception) {
                HapticFeedbackHelper.vibrateError(getApplication())
                _playgroundState.value = _playgroundState.value.copy(
                    isGenerating = false,
                    error = e.localizedMessage ?: "Playground execution failed"
                )
            }
        }
    }

    fun stopPlayground() {
        playgroundJob?.cancel()
        _playgroundState.value = _playgroundState.value.copy(isGenerating = false)
    }

    fun saveCurrentSnippet(title: String) {
        val state = _playgroundState.value
        if (state.generatedCode.isBlank()) return

        viewModelScope.launch {
            val snippet = SavedSnippetEntity(
                id = UUID.randomUUID().toString(),
                title = title.ifBlank { "Snippet ${System.currentTimeMillis() % 1000}" },
                language = state.language,
                systemPrompt = state.systemPrompt,
                userPrompt = state.userPrompt,
                generatedCode = state.generatedCode,
                temperature = state.temperature,
                topP = state.topP,
                modelUsed = activeModel.value
            )
            snippetDao.insertSnippet(snippet)
        }
    }

    fun deleteSnippet(id: String) {
        viewModelScope.launch {
            snippetDao.deleteSnippet(id)
        }
    }

    // Backend & Settings Actions
    fun setBackend(type: BackendType) {
        backendManager.setBackend(type)
        viewModelScope.launch {
            if (backendManager.activeModelName.value.isBlank()) {
                val models = backendManager.getActiveBackend().listModels()
                if (models.isNotEmpty()) {
                    backendManager.setModel(models.first())
                } else {
                    _chatError.value = "No models found. In Termux: ollama pull qwen2.5-coder:1.5b"
                }
            }
            checkBackendHealth()
        }
    }

    fun setModel(model: String) {
        backendManager.setModel(model)
        checkBackendHealth()
    }

    fun setLocalEndpoint(url: String) {
        backendManager.setLocalEndpoint(url)
        checkBackendHealth()
    }

    fun checkBackendHealth() {
        viewModelScope.launch {
            _isPinging.value = true
            val backend = backendManager.getActiveBackend()
            try {
                val status = backend.checkHealth()
                _healthStatus.value = status
            } catch (e: Exception) {
                _healthStatus.value = HealthStatus(
                    isHealthy = false,
                    latencyMs = 0L,
                    message = e.localizedMessage ?: "Health check failed"
                )
            } finally {
                _isPinging.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        healthMonitorJob?.cancel()
        chatGenerationJob?.cancel()
        playgroundJob?.cancel()
        messageObserveJob?.cancel()
    }
}

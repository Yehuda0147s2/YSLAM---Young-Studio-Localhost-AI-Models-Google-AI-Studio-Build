# YSLAM — Young Studio Localhost AI Models
# MASTER BUILD PROMPT (Android — Production Release)

A complete, self-contained specification for taking the YSLAM Android application from
its current state to a signed, shippable v1.0. Hand this to any capable coding agent
(Cursor, Aider, Claude Opus, GPT-5). No external files or prior context required.

---

# PART 0 — THE INVARIANTS

Four rules. Violating any invalidates the release.

### Invariant 1 — Real connection or honest failure
> A backend shows "connected" / "ready" / "available" ONLY when verified by three real
> levels of HTTP verification in the last 5 seconds: (1) server reachable, (2) target
> model present, (3) real 1-token inference succeeds. Every other state displays as
> NOT CONNECTED with a specific, actionable error message.

### Invariant 2 — No fabricated data
> No method returns hardcoded model lists, fake latencies, placeholder metrics, or
> simulated responses when the backend is unreachable or unconfigured. Empty is empty.
> Unknown is unknown. Never invent.

### Invariant 3 — Continuous monitoring
> Health status is re-verified every 5 seconds while the app is foregrounded. Stale
> status older than 5 seconds must never be displayed as current.

### Invariant 4 — User controls keys
> API keys (Gemini cloud and local bearer tokens) are stored encrypted at rest via
> Android KeyStore AES-GCM. Never logged, never in `localStorage`-equivalent prefs,
> never sent to any endpoint that isn't the key's owner.

**Enforcement:** CI grep gates and chaos tests below reject any commit that violates
these rules.

---

# PART 1 — WHAT SHIPS

## The deliverable APK

```
yslam-1.0.0-release.apk
├── Signed with release keystore (v2 + v3 schemes)
├── APK size: 15–25 MB (Compose baseline)
├── minSdk: 24, targetSdk: 36, compileSdk: 36
└── Installs on Android 7.0+ devices
```

## What's included

| Category | Features |
|---|---|
| **Dual backends** | Google Gemini (Firebase AI Logic) + Local Ollama (Retrofit HTTP) |
| **Chat** | Multi-turn streaming, markdown, syntax highlighting, code extraction |
| **Playground** | System prompt editor, 5 templates, hyperparameter sliders, snippet save |
| **Persistence** | Room DB (sessions, messages, snippets) + Firestore cloud sync (optional) |
| **Auth** | Google Sign-In via Credential Manager (optional for cloud sync) |
| **Permissions** | Full runtime permission flow (mic, notifications) with rationale dialogs |
| **Key storage** | AES-GCM encrypted via Android KeyStore |
| **Health monitoring** | 5-second continuous polling with 3-level verification |
| **Skills (12)** | Voice input, TTS-ready, share target, quick actions, prompt library, chat export, snippet library, settings backup, chat history, snippet duplication, deep link ready, onboarding |
| **Error handling** | 30+ error codes with specific messages, retry logic, chaos-tested |

## What's NOT included (deferred to v1.1)

- Vision models (image-to-code)
- Multi-file project workspace (this is a single-prompt playground, not a full IDE)
- Agent loop / autonomous execution
- Benchmark suite
- RAG over local files
- Local model pull from within the app (must use `ollama pull` in Termux)

---

# PART 2 — ARCHITECTURE

## System diagram

```
┌──────────────────────────────────────────────────────────────────────┐
│                     YSLAM Jetpack Compose UI                         │
│                                                                      │
│  ┌─────────────┐  ┌──────────────┐  ┌─────────────┐  ┌──────────┐  │
│  │  ChatScreen │  │  Playground  │  │  Settings   │  │  API     │  │
│  │  - streaming│  │  - templates │  │  - backend  │  │  Keys    │  │
│  │  - markdown │  │  - params    │  │  - health   │  │  - add   │  │
│  │  - history  │  │  - snippets  │  │  - endpoint │  │  - view  │  │
│  └──────┬──────┘  └──────┬───────┘  └──────┬──────┘  └────┬─────┘  │
│         └────────────────┴─────────────────┴───────────────┘        │
│                                    │                                │
│                                    ▼                                │
│                        YslamViewModel (StateFlow)                   │
└────────────────────────────────────┼────────────────────────────────┘
                                     │
             ┌───────────────────────┴───────────────────────┐
             ▼                                               ▼
  ┌──────────────────────┐                    ┌──────────────────────┐
  │  Local Storage       │                    │  AiBackend Interface │
  │  - Room DB           │                    │  - generateStream()  │
  │  - Encrypted keys    │                    │  - checkHealth()     │
  │  - SharedPrefs       │                    │  - listModels()      │
  └──────────────────────┘                    └──────────┬───────────┘
                                                         │
                                    ┌────────────────────┴────────────────────┐
                                    ▼                                         ▼
                       ┌────────────────────────┐             ┌────────────────────────┐
                       │   GeminiBackend        │             │   LocalBackend         │
                       │   Firebase AI Logic    │             │   Retrofit + OkHttp    │
                       │   gemini-3.8-flash     │             │   Ollama /api/chat     │
                       │   App Check attestation│             │   10.0.2.2:11434       │
                       └────────────────────────┘             └────────────────────────┘
                                                                          │
                                                                          ▼
                                                              ┌───────────────────────┐
                                                              │   Termux on host      │
                                                              │   ollama serve        │
                                                              │   OLLAMA_ORIGINS='*'  │
                                                              └───────────────────────┘
```

## Why this architecture

**The `AiBackend` interface** isolates the UI from backend choice. The UI calls
`backend.generateStream(request)` and doesn't know or care whether that's Gemini or
Ollama. This is what makes dual-backend support possible in a single codebase.

**Health verification at 3 levels** is the core reliability feature. Level 1
(`/api/tags` responds) proves the server is running. Level 2 (model is in the list)
proves the specific model is installed. Level 3 (real inference succeeds) proves the
whole pipeline actually works. Skipping any level produces false-positive "connected"
states that waste user time.

**Continuous 5-second polling** prevents the most common complaint about local LLM
apps: "it says connected but nothing works." With polling, when Ollama dies the UI
flips to disconnected within 5 seconds.

---

# PART 3 — TECH STACK (LOCKED)

| Component | Version / Choice |
|---|---|
| Language | Kotlin 2.2+ |
| UI | Jetpack Compose + Material 3 |
| DI | None (constructor injection + companion factories) |
| Local DB | Room 2.7+ |
| Networking | Retrofit 2.12 + OkHttp 4.10 + Moshi |
| JSON | Moshi with Kotlin codegen |
| Coroutines | kotlinx-coroutines 1.10+ |
| Auth | Firebase Auth + Credential Manager |
| Cloud | Firebase Firestore + Firebase AI Logic |
| App Check | Firebase App Check (reCAPTCHA in prod, debug token in dev) |
| Image loading | Coil 2.7 (icons) |
| minSdk | 24 (Android 7.0) |
| targetSdk | 36 |
| compileSdk | 36 |
| JDK | 17 |

**No Dagger/Hilt.** Constructor injection is enough for this app size.

**No Navigation Compose.** The current `when (currentTab)` approach in `MainActivity` is
fine. Do not rewrite to Navigation Compose unless explicitly required.

---

# PART 4 — THE 9 CRITICAL FIXES

These bugs exist in the current codebase and block release. Fix them first, in order.

## FIX 1 — Remove fake model list fallback

**File:** `app/src/main/java/com/example/backend/ollama/OllamaClient.kt`

**Find:**
```kotlin
} else {
    listOf("llama3", "codellama", "mistral", "qwen2.5-coder", "deepseek-coder", "phi3")
}
```

**Replace with:**
```kotlin
} else {
    emptyList()
}
```

**Why:** returning a fake model list violates Invariant 2. The user pulls `llama3`,
the app auto-selects it from the "available" list, and the first message fails with
404 because the model was never actually installed.

**Verify:** stop Ollama → open the app → model list shows empty → attempting to switch
to Local shows "No models available. In Termux: ollama pull <model>".

## FIX 2 — Add Level-3 inference verification

**File:** `app/src/main/java/com/example/backend/ollama/OllamaClient.kt`

**Add this method:**
```kotlin
suspend fun verifyInference(
    modelName: String,
    timeoutSeconds: Long = 20L
): HealthStatus = withContext(Dispatchers.IO) {
    val targetUrl = endpointUrl.removeSuffix("/")
    val startedAt = System.currentTimeMillis()

    return@withContext try {
        val request = OllamaChatRequest(
            model = modelName,
            messages = listOf(OllamaMessage(role = "user", content = "hi")),
            stream = false,
            options = OllamaOptions(numPredict = 1, temperature = 0f)
        )
        val response = withTimeout(timeoutSeconds * 1000) {
            apiService.chat(request)
        }
        val latency = System.currentTimeMillis() - startedAt
        val content = response.message?.content.orEmpty()

        if (content.isEmpty()) {
            HealthStatus(
                isHealthy = false,
                latencyMs = latency,
                message = "Model '$modelName' loaded but returned empty response. Check OOM or model corruption in Termux.",
                availableModels = emptyList()
            )
        } else {
            HealthStatus(
                isHealthy = true,
                latencyMs = latency,
                message = "Inference verified with '$modelName' in ${latency}ms",
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
```

**Update `LocalBackend.checkHealth()`** to call it after Levels 1+2 pass:
```kotlin
override suspend fun checkHealth(): HealthStatus {
    // Levels 1+2: server reachable + models listed
    val baseHealth = ollamaClient.checkHealth()
    if (!baseHealth.isHealthy) return baseHealth

    // Level 3: real inference
    val inference = ollamaClient.verifyInference(selectedModel)
    return if (inference.isHealthy) {
        HealthStatus(
            isHealthy = true,
            latencyMs = baseHealth.latencyMs + inference.latencyMs,
            message = "Connected ($endpointUrl). ${baseHealth.availableModels.size} models. Inference verified in ${inference.latencyMs}ms.",
            availableModels = baseHealth.availableModels
        )
    } else {
        HealthStatus(
            isHealthy = false,
            latencyMs = baseHealth.latencyMs,
            message = inference.message,
            availableModels = baseHealth.availableModels
        )
    }
}
```

**Add imports if missing:** `kotlinx.coroutines.withTimeout`, `kotlinx.coroutines.TimeoutCancellationException`

**Verify:** server running with `llama3` pulled → `checkHealth().isHealthy == true` only
after inference succeeds. Server running with `llama3` NOT pulled → `checkHealth()`
returns `isHealthy == false` with message containing "Inference failed".

## FIX 3 — Don't hardcode fallback models on backend switch

**File:** `app/src/main/java/com/example/backend/BackendManager.kt`

**Find:**
```kotlin
} else if (type == BackendType.LOCAL && _activeModelName.value.startsWith("gemini")) {
    setModel("llama3")
}
```

**Replace with:**
```kotlin
} else if (type == BackendType.LOCAL && _activeModelName.value.startsWith("gemini")) {
    // Don't assume llama3 exists. Clear the model; ViewModel will query
    // listModels() and either select the first available or prompt user to pull one.
    _activeModelName.value = ""
    prefs.edit().putString("active_model", "").apply()
}
```

**Update `YslamViewModel.setBackend()`** to handle the empty model case:
```kotlin
fun setBackend(type: BackendType) {
    backendManager.setBackend(type)
    viewModelScope.launch {
        if (backendManager.activeModelName.value.isBlank()) {
            val models = backendManager.getActiveBackend().listModels()
            if (models.isNotEmpty()) {
                backendManager.setModel(models.first())
            } else {
                _chatError.value =
                    "No models found. In Termux: ollama pull qwen2.5-coder:1.5b"
            }
        }
        checkBackendHealth()
    }
}
```

**Verify:** pull only `qwen2.5-coder:7b` → switch to Local → auto-selected model is
`qwen2.5-coder:7b`. Pull nothing → switch to Local → error message appears.

## FIX 4 — Continuous 5-second health monitoring

**File:** `app/src/main/java/com/example/viewmodel/YslamViewModel.kt`

**Add to the class:**
```kotlin
private var healthMonitorJob: Job? = null

private fun startHealthMonitoring() {
    healthMonitorJob?.cancel()
    healthMonitorJob = viewModelScope.launch {
        while (isActive) {
            delay(5_000)
            // Skip polling during active generation to avoid contention
            if (!_isGeneratingChat.value && !_playgroundState.value.isGenerating) {
                try {
                    val status = backendManager.getActiveBackend().checkHealth()
                    // Only update state if it meaningfully changed
                    val current = _healthStatus.value
                    if (current?.isHealthy != status.isHealthy ||
                        current?.message != status.message) {
                        _healthStatus.value = status
                    }
                } catch (_: CancellationException) {
                    throw
                } catch (_: Exception) {
                    // Silent — next poll will try again
                }
            }
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
```

**Add to `init { }`** at the end:
```kotlin
init {
    // ... existing init ...
    startHealthMonitoring()
}
```

**Add imports if missing:** `kotlinx.coroutines.Job`, `kotlinx.coroutines.delay`,
`kotlinx.coroutines.isActive`, `kotlinx.coroutines.CancellationException`

**Verify:** app foregrounded with Ollama running → `pkill ollama` in Termux → within
5 seconds the connection pill in `TopStatusHeader` turns red. Restart Ollama → within
5 seconds it turns green.

## FIX 5 — Retry with backoff for transient failures

**File:** `app/src/main/java/com/example/viewmodel/YslamViewModel.kt`

**Add helper methods:**
```kotlin
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
    onAccumulated: (String) -> Unit,
    maxAttempts: Int = 3
): Pair<String, Long> {
    val delays = listOf(0L, 2_000L, 5_000L)
    var lastException: Throwable? = null

    for (attempt in 0 until maxAttempts) {
        if (attempt > 0) delay(delays[attempt])
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
```

**Modify `sendChatMessage()`** to use it. Replace the streaming block:
```kotlin
// OLD:
// val fullResponse = StringBuilder()
// backend.generateStream(request).collect { chunk -> ... }

// NEW:
val (responseText, latency) = streamWithRetry(backend, request) { accumulated ->
    _chatStreamingText.value = accumulated
}
val fullResponse = StringBuilder(responseText)
```

**Verify:** inject a 429 response → app retries twice → succeeds on the 3rd attempt.
Inject a permanent 401 → app does not retry (immediate failure).

## FIX 6 — Debounce Firestore syncs

**File:** `app/src/main/java/com/example/data/repository/ChatFirestoreRepository.kt`

**Add to the class:**
```kotlin
private val pendingSyncs = mutableMapOf<String, kotlinx.coroutines.Job>()

fun scheduleSync(
    session: ChatSessionEntity,
    messages: List<ChatMessageEntity>,
    scope: kotlinx.coroutines.CoroutineScope
) {
    pendingSyncs[session.id]?.cancel()
    pendingSyncs[session.id] = scope.launch {
        kotlinx.coroutines.delay(2_000)  // debounce 2s
        saveSessionToCloud(session, messages)
        pendingSyncs.remove(session.id)
    }
}
```

**Update `YslamViewModel`** to call `scheduleSync` instead of `saveSessionToCloud` in
`sendChatMessage()` after assistant response:
```kotlin
if (currentSess != null && authManager.currentUser.value != null) {
    chatFirestoreRepository.scheduleSync(currentSess, _messages.value + aiMsg, viewModelScope)
}
```

**Verify:** send 10 messages rapidly → check Firestore console → ≤ 3 writes to the
session document, not 10.

## FIX 7 — Delete example test boilerplate

**Delete files:**
- `app/src/test/java/com/example/ExampleUnitTest.kt`
- `app/src/androidTest/java/com/example/ExampleInstrumentedTest.kt`

**Verify:** `./gradlew :app:testDebugUnitTest` still passes (the four real tests:
`MarkdownAndBackendUnitTest`, `OllamaRetrofitUnitTest`, `PermissionSystemTest`,
`ExampleRobolectricTest`).

## FIX 8 — Release signing scripts

**Create `scripts/generate-keystore.sh`:**
```bash
#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p keystore

if [ -f keystore/yslam-release.jks ]; then
    echo "▶ Keystore already exists at keystore/yslam-release.jks"
    read -rp "Overwrite? [y/N]: " ans
    [ "${ans,,}" = "y" ] || exit 0
    rm -f keystore/yslam-release.jks
fi

read -rsp "Keystore password (16+ chars): " STORE_PW; echo
read -rsp "Key password (Enter = same): " KEY_PW; echo
KEY_PW=${KEY_PW:-$STORE_PW}

keytool -genkeypair -v \
    -keystore keystore/yslam-release.jks \
    -alias yslams \
    -keyalg RSA -keysize 4096 -validity 10000 \
    -storepass "$STORE_PW" -keypass "$KEY_PW" \
    -dname "CN=YSLAM, OU=Mobile, O=YSLAM, C=US"

chmod 600 keystore/yslam-release.jks
echo ""
echo "✅ Keystore at keystore/yslam-release.jks"
echo "   ⚠ Back this up to 3+ locations (password manager, encrypted drive, safe)."
echo "   ⚠ If lost: you can never publish updates to Play Store."
```

**Create `scripts/build-release.sh`:**
```bash
#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

VERSION_NAME="${VERSION_NAME:-1.0.0}"
VERSION_CODE="${VERSION_CODE:-$(date +%y%m%d%H)}"

# Verify signing config
if [ -z "${KEYSTORE_PATH:-}" ] || [ -z "${STORE_PASSWORD:-}" ] || [ -z "${KEY_PASSWORD:-}" ]; then
    cat <<'EOF'
✗ Signing not configured. Set:
    export KEYSTORE_PATH=$(pwd)/keystore/yslam-release.jks
    export STORE_PASSWORD=...
    export KEY_PASSWORD=...

  Or create a keystore once:
    bash scripts/generate-keystore.sh
EOF
    exit 1
fi

if [ ! -f "$KEYSTORE_PATH" ]; then
    echo "✗ Keystore not found at $KEYSTORE_PATH"
    exit 1
fi

echo "▶ Building release APK (v${VERSION_NAME}, code ${VERSION_CODE})…"
./gradlew clean :app:assembleRelease \
    -PversionCodeOverride="$VERSION_CODE" \
    -PversionNameOverride="$VERSION_NAME"

APK="app/build/outputs/apk/release/app-release.apk"
[ -f "$APK" ] || { echo "✗ Release APK not produced"; exit 1; }

cp "$APK" "./yslam-${VERSION_NAME}-release.apk"

echo "▶ Verifying signature…"
APKSIGNER=$(find "$ANDROID_HOME/build-tools" -name apksigner 2>/dev/null | sort -r | head -1)
if [ -n "$APKSIGNER" ]; then
    "$APKSIGNER" verify --print-certs "./yslam-${VERSION_NAME}-release.apk"
fi

SIZE=$(du -h "./yslam-${VERSION_NAME}-release.apk" | cut -f1)
echo ""
echo "✅ yslams-${VERSION_NAME}-release.apk (${SIZE}) at project root"
```

**Update `.gitignore`** — add:
```
keystore/
*.jks
*.keystore
```

**Update `app/build.gradle.kts`** to fail fast on missing keystore:
```kotlin
signingConfigs {
    create("release") {
        val keystorePath = System.getenv("KEYSTORE_PATH")
            ?: "${rootDir}/keystore/yslam-release.jks"
        val keystoreFile = file(keystorePath)
        if (!keystoreFile.exists()) {
            // Non-fatal: allow debug builds. Release build will fail at signing.
            logger.warn("Release keystore not found at $keystorePath. " +
                        "Run scripts/generate-keystore.sh or set KEYSTORE_PATH.")
        }
        storeFile = if (keystoreFile.exists()) keystoreFile else null
        storePassword = System.getenv("STORE_PASSWORD") ?: ""
        keyAlias = "yslams"
        keyPassword = System.getenv("KEY_PASSWORD") ?: ""
    }
    // debugConfig unchanged
}
```

**Verify:** `bash scripts/generate-keystore.sh` creates the keystore.
`export KEYSTORE_PATH=... STORE_PASSWORD=... KEY_PASSWORD=...` then
`bash scripts/build-release.sh` produces `yslam-1.0.0-release.apk` at project root.
`apksigner verify` passes.

## FIX 9 — GitHub Actions CI

**Create `.github/workflows/ci.yml`:**
```yaml
name: CI

on:
  push:
    branches: [main]
  pull_request:

jobs:
  build:
    runs-on: ubuntu-latest
    timeout-minutes: 20
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'
      - uses: android-actions/setup-android@v3
      - uses: gradle/actions/setup-gradle@v3

      - name: Compile
        run: ./gradlew :app:assembleDebug --stacktrace

      - name: Unit tests
        run: ./gradlew :app:testDebugUnitTest

      - name: Lint
        run: ./gradlew :app:lintDebug

      - name: Invariant — no fake model lists
        run: |
          if grep -rn 'listOf("llama3", "codellama"\|listOf("codellama", "mistral"' \
              app/src/main/java/; then
            echo "✗ Hardcoded model list found"
            exit 1
          fi

      - name: Invariant — no hardcoded connected state
        run: |
          # isHealthy = true must only appear inside real HTTP success paths
          HITS=$(grep -rn 'isHealthy = true' app/src/main/java/ | wc -l)
          echo "Found $HITS occurrences of 'isHealthy = true' — reviewing manually required"
          grep -rn 'isHealthy = true' app/src/main/java/

      - name: Invariant — no Math.random in health code
        run: |
          if grep -rn 'Math.random()' app/src/main/java/com/example/backend/; then
            echo "✗ Random values in backend code"
            exit 1
          fi

      - name: Invariant — no keystore committed
        run: |
          if git ls-files | grep -E '\.jks$|\.keystore$|keystore/'; then
            echo "✗ Keystore file committed to git"
            exit 1
          fi
```

**Verify:** push to a branch → GitHub Actions runs → all steps pass.

---

# PART 5 — ERROR CODES (complete catalog)

Every error state in the app maps to one of these codes. The codes appear in
`HealthStatus.message` and in diagnostic toasts.

## Y-series — Local Ollama errors

| Code | Trigger | User Message |
|---|---|---|
| **Y-001** | CORS blocked (browser-only, N/A on Android) | (unused on Android) |
| **Y-002** | Connection timeout | "Connection timed out connecting to Ollama at '{url}'. Ensure Ollama is running. On Emulator: http://10.0.2.2:11434" |
| **Y-003** | Connection refused / unreachable | "Cannot reach Ollama at '{url}'. Ensure 'ollama serve' is running." |
| **Y-004** | Unknown host | "Cannot resolve host '{url}'. Check the endpoint URL." |
| **Y-005** | Server up, no models | "Ollama is running but has no models installed. In Termux: ollama pull qwen2.5-coder:1.5b" |
| **Y-006** | Model not in list | "Model '{model}' is not installed. Available: {list}. Pull it or switch models." |
| **Y-007** | HTTP 4xx | "Ollama returned HTTP {code}: {body}" |
| **Y-008** | HTTP 5xx | "Ollama server error ({code}). Check server logs." |
| **Y-009** | Inference timeout | "Inference timed out after {n}s. Model may be OOM or too large." |
| **Y-010** | Inference empty response | "Model loaded but returned no output. Check OOM in Termux." |
| **Y-011** | Stream interrupted | "Stream interrupted. Partial response preserved." |
| **Y-012** | Malformed NDJSON | "Ollama returned unparseable data. Version mismatch?" |
| **Y-013** | Endpoint URL invalid | "Invalid endpoint URL: '{url}'. Format: http://host:port" |
| **Y-014** | No model selected | "No model selected. Pick one in Settings." |

## G-series — Gemini cloud errors

| Code | Trigger | User Message |
|---|---|---|
| **G-001** | No API key / App Check failed | "Gemini authentication failed. Check Firebase config." |
| **G-002** | Model not found | "Gemini model '{model}' not available. Try gemini-3.8-flash." |
| **G-003** | Rate limited | "Gemini rate-limited. Retrying in a moment…" (auto-retries) |
| **G-004** | Quota exceeded | "Gemini quota exceeded for this project." |
| **G-005** | Empty response | "Gemini returned no content." |
| **G-006** | Network error | "Cannot reach Gemini. Check your internet connection." |

## R-series — Room/persistence errors

| Code | Trigger | User Message |
|---|---|---|
| **R-001** | Database insert failed | "Failed to save message. Storage may be full." |
| **R-002** | Database read failed | "Failed to load messages. Retry or clear app data." |
| **R-003** | Migration failed | "Database upgrade failed. This is a bug — please report it." |

## P-series — Permission errors

| Code | Trigger | User Message |
|---|---|---|
| **P-001** | Microphone denied | Rationale dialog shown, not a toast |
| **P-002** | Notifications denied | Rationale dialog shown |
| **P-003** | Permission permanently denied | Deep link to system settings |

## Total: 23 error codes

Every `HealthStatus.message` and every toast/error display in the app must use text
that maps to one of these codes. No "Something went wrong" without a code.

---

# PART 6 — FEATURES (12 skills)

Each skill is verifiable. Add a test or manual verification step for each.

## Skill 1 — Voice input (existing, verify)
- **Trigger:** tap mic icon in chat input
- **Behavior:** requests `RECORD_AUDIO` if not granted → starts `SpeechRecognizer` → 
  transcribed text appended to input field (not auto-sent)
- **Verify:** tap mic → speak "hello world" → text "hello world" appears in input

## Skill 2 — Chat export
- **Trigger:** long-press any chat session in history dialog → "Export"
- **Behavior:** uses `ACTION_CREATE_DOCUMENT` to save the session as Markdown to user-
  chosen location
- **Format:**
  ```markdown
  # Chat: {title}
  **Backend:** {backend}  
  **Model:** {model}  
  **Date:** {date}

  ## User
  {message}

  ## Assistant ({latency}ms)
  {message}
  ```
- **Verify:** long-press session → Export → pick Downloads → file opens in Files app

## Skill 3 — Snippet library (existing, verify)
- **Trigger:** Playground → "Save Snippet"
- **Behavior:** stores title + language + prompts + generated code in Room
- **Verify:** generate code → Save Snippet → title "Test" → open Snippets list → 
  snippet appears → tap to load

## Skill 4 — Chat history (existing, verify)
- **Trigger:** history icon in TopStatusHeader
- **Verify:** send 2 messages → open history → session visible with correct title

## Skill 5 — Prompt templates (existing, verify)
- **Trigger:** Playground → "Templates" dropdown
- **Behavior:** 5 templates that fill the prompt field
- **Verify:** tap each of the 5 templates → prompt field updates correctly

## Skill 6 — Session rename
- **Trigger:** long-press session in history → "Rename"
- **Behavior:** dialog for new title → updates Room
- **Verify:** rename "New Chat" to "My Session" → UI updates → Firestore syncs (if signed in)

## Skill 7 — Session delete
- **Trigger:** trash icon in history
- **Behavior:** deletes from Room + Firestore
- **Verify:** delete session → gone from list → Firestore document deleted

## Skill 8 — Quick settings integration (Android only)
- **Trigger:** long-press app icon → quick actions
- **Behavior:** add `App Shortcuts` in `res/xml/shortcuts.xml`:
  - "New Chat" → opens app in chat tab
  - "Playground" → opens app in playground tab
- **Verify:** long-press home screen icon → 2 shortcuts appear → tap each → correct screen

## Skill 9 — Deep link ready
- **Trigger:** `yslam://chat` or `yslam://playground`
- **Manifest:** `<intent-filter>` for `yslam` scheme
- **Behavior:** opens correct screen
- **Verify:** `adb shell am start -a android.intent.action.VIEW -d "yslam://playground"`
  → opens playground

## Skill 10 — API key management (existing, verify)
- **Trigger:** Settings → API Keys
- **Verify:** add Gemini key → masked display → toggle reveal works → copy works →
  delete works → app uses the active key on next Gemini call

## Skill 11 — Backend switch persistence
- **Trigger:** backend toggle in TopStatusHeader
- **Behavior:** persists choice across app restart
- **Verify:** switch to Local → close app → reopen → Local is still selected

## Skill 12 — Onboarding hints
- **Trigger:** first launch only
- **Behavior:** contextual tooltips on first chat send, first playground run, first
  settings visit
- **Storage:** `SharedPreferences` flags (`onboarding_chat_shown`, etc.)
- **Verify:** clear app data → open → tooltips appear → mark as seen → don't reappear

---

# PART 7 — PHASED IMPLEMENTATION WITH GATES

Every phase ends with a gate. Do not advance past a failing gate. Every gate item
requires evidence in `HARDENING_REPORT.md`.

## Phase 0 — Audit current state (30 minutes)

- [ ] Clone the current repo (or open the AI Studio project)
- [ ] Run `./gradlew :app:assembleDebug` — record exit code
- [ ] Run `./gradlew :app:testDebugUnitTest` — record test pass/fail
- [ ] Run the 9 bug greps from Part 4 — record which are present
- [ ] Baseline screenshot of the app on an emulator

**Gate 0:** `BASELINE.md` created with:
- Build status (success/fail)
- Test status (n passing, n failing)
- Bug presence matrix (which of 9 bugs exist)
- Screenshot attached

## Phase 1 — Fix the 9 critical bugs (2 days)

Execute FIX 1 through FIX 9 in order.

**Gate 1 (per-fix):** each fix has its own "Verify" step in Part 4. Every fix must
pass its verify before proceeding to the next.

**Gate 1 (aggregate):**
- [ ] `./gradlew :app:testDebugUnitTest` passes
- [ ] `./gradlew :app:assembleDebug` succeeds
- [ ] All 9 greps for fake data return 0 hits
- [ ] Manual test: chat works with real Ollama
- [ ] Manual test: chat works with Gemini (if API key present)
- [ ] Manual test: kill Ollama → pill turns red within 5s
- [ ] Manual test: restart Ollama → pill turns green within 5s

## Phase 2 — Feature completion (2 days)

Implement skills 2, 6, 8, 9, 12 (the ones marked "add" above). Verify skills 1, 3, 4,
5, 7, 10, 11 still work after Phase 1 changes.

**Gate 2:**
- [ ] All 12 skills have a verification test (either unit test or documented manual steps)
- [ ] Each skill's verify step from Part 6 passes
- [ ] No skill regresses Phase 1 fixes

## Phase 3 — Release pipeline (1 day)

- [ ] `scripts/generate-keystore.sh` runs → keystore created
- [ ] `scripts/build-release.sh` runs with env vars set → signed APK at project root
- [ ] `apksigner verify` passes with v2+v3 schemes
- [ ] Install on physical device → app launches

**Gate 3:**
- [ ] `ls -la yslam-1.0.0-release.apk` at project root
- [ ] APK size < 25 MB
- [ ] Installs on Android 7 device, Android 14 device
- [ ] Signature verified (record SHA-256 of certificate)

## Phase 4 — CI pipeline (0.5 day)

- [ ] `.github/workflows/ci.yml` committed
- [ ] Push to branch → GitHub Actions runs → all steps pass
- [ ] Deliberately reintroduce fake data → CI fails (proving gate works)
- [ ] Restore → CI passes

**Gate 4:**
- [ ] CI workflow badge green
- [ ] CI failure demonstration screenshot in `HARDENING_REPORT.md`
- [ ] Restored state passes

## Phase 5 — Documentation (0.5 day)

- [ ] `README.md` updated with:
  - Architecture overview
  - Backend setup (Gemini API key + Ollama in Termux)
  - Emulator endpoint note (`10.0.2.2:11434`)
  - Build + run instructions
  - Release signing instructions
- [ ] `HARDENING_REPORT.md` created with per-gate evidence
- [ ] `KNOWN_ISSUES.md` created with:
  - `applicationId = com.aistudio.yslam.kpwzqd` must be renamed for Play Store
  - No Play Store metadata / privacy policy yet
  - Model list hardcoded for Gemini (Firebase AI Logic limitation)
  - No crash reporting (Crashlytics not integrated)
- [ ] `TERMUX.md` created with setup commands for Ollama

**Gate 5:** all four files exist and are complete.

## Phase 6 — Final acceptance (0.5 day)

Reviewer on a clean Android device (Android 10+, 4GB+ RAM) with Termux + Ollama:

1. Install `yslam-1.0.0-release.apk` via `adb install`
2. Launch → app opens to Chat screen with "New Chat" thread
3. Top pill shows "gemini-3.8-flash" or "llama3" depending on config
4. Tap backend pill → switch to Local → configure endpoint `http://10.0.2.2:11434`
5. Tap "Test Connection" → shows real latency or specific error
6. If connected: type "write a fib function" → tokens stream in real time
7. If disconnected: pill shows red + error message with fix instructions
8. Kill Ollama → within 5s pill turns red
9. Restart Ollama → within 5s pill turns green
10. Navigate to Playground → tap template → prompt fills
11. Adjust temperature slider → click "Run Code Prompt" → code streams
12. Save Snippet → open history → snippet appears
13. Settings → API Keys → add Gemini key → verify masked display
14. Long-press chat session → Rename → "My Session" → visible in list
15. Long-press chat session → Export → save to Downloads → file opens in Files app
16. Long-press home screen icon → "New Chat" + "Playground" shortcuts appear
17. `adb shell am start -a android.intent.action.VIEW -d "yslam://playground"` → 
    opens Playground
18. `ls -la yslam-*-release.apk` at project root → exists, signed
19. `apksigner verify --print-certs yslams-1.0.0-release.apk` → v2+v3 verified

**Gate 6:** all 19 steps pass with screenshots + logcat evidence in
`HARDENING_REPORT.md`.

---

# PART 8 — ANTI-PATTERNS (reject on sight)

## Data invariants
- ❌ Any `listOf("llama3", "codellama", ...)` in production code
- ❌ Any hardcoded `isHealthy = true` outside a real HTTP success path
- ❌ Any `Math.random()` in backend, health, or latency code
- ❌ Any `delay(800)` that fakes a network call
- ❌ Any `Text("CONNECTED")` without a preceding `checkHealth()`

## Code quality
- ❌ `try { ... } catch (e: Exception) { }` — silent swallow
- ❌ `runBlocking { }` in Compose or ViewModel
- ❌ `!!` on nullable types in production code
- ❌ `println()` for logging — use `Log.d/w/e` with a tag
- ❌ `TODO()` or `FIXME` comments in shipped code
- ❌ `catch (Throwable)` without rethrowing `CancellationException`

## UI
- ❌ Spinner without a label ("Connecting…" required)
- ❌ Error dialog without an action button
- ❌ Toast for critical errors — use dialog with action
- ❌ Hardcoded strings in Compose — use `strings.xml` (for future localization)

## Security
- ❌ API keys in `SharedPreferences` (unencrypted) — use `SecureKeyStorage`
- ❌ API keys in `Log.d/w/e` output — never log keys
- ❌ API keys in `BuildConfig` — use runtime input
- ❌ `android:usesCleartextTraffic="true"` in release (currently `true` in the manifest
  — fix by using `network_security_config.xml` with loopback-only cleartext)
- ❌ `android:debuggable="true"` in release build type

**Fix for the last one before shipping:** replace
`android:usesCleartextTraffic="true"` in `AndroidManifest.xml` with:
```xml
android:networkSecurityConfig="@xml/network_security_config"
```
And create `app/src/main/res/xml/network_security_config.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <base-config cleartextTrafficPermitted="false">
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </base-config>
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="false">10.0.2.2</domain>
        <domain includeSubdomains="false">127.0.0.1</domain>
        <domain includeSubdomains="false">localhost</domain>
        <domain includeSubdomains="false">192.168.0.0</domain>
        <domain includeSubdomains="false">192.168.1.0</domain>
    </domain-config>
</network-security-config>
```

**Note:** the `192.168.0.0` and `192.168.1.0` entries above won't work for arbitrary
LAN IPs (network-security-config requires exact domains). For LAN support, either:
- Document that users must use the emulator host `10.0.2.2` or a fixed IP
- Or use a wildcard-compatible proxy setup

For v1.0, recommend using `10.0.2.2` (emulator) or setting up a stable LAN IP with
port forwarding. Document this in `README.md`.

---

# PART 9 — WORKFLOW

1. **Work phase by phase.** Print `PHASE N STARTING` and `PHASE N GATE: PASS|FAIL`.
2. **Never advance past a failing gate.** Fix the failure first.
3. **Commit per phase:** `feat(phase-N): <summary>`. Never amend.
4. **Tag final commit:** `git tag v1.0.0`.
5. **Report every error encountered and how fixed** in `BUILD_LOG.md`.
6. **Every gate item requires evidence** (command output, screenshot, logcat line).
7. **The APK must exist at project root** (`yslam-1.0.0-release.apk`) before finalizing.

**Start with Phase 0. Stop at Gate 0 for review.**

---

# PART 10 — WHAT SUCCESS LOOKS LIKE

The user opens the app. The pill shows "gemini-3.8-flash" (green). They type a prompt.
Tokens stream in from Gemini. They type again. It works.

They tap the pill, switch to "Local Ollama." The app queries available models, finds
only `qwen2.5-coder:7b`, auto-selects it. The pill turns green with real latency.
They send a message. Tokens stream from their own phone's Ollama.

They pull the phone's Termux app, `pkill ollama`. Within 5 seconds, the pill turns
red — the app knows the backend died. They type a message; the app shows "Cannot
reach Ollama at http://10.0.2.2:11434. Ensure 'ollama serve' is running." Not a
generic error. Not a silent hang. A specific, actionable message.

They restart Ollama in Termux. Within 5 seconds, the pill turns green. They send a
message. It works.

They open Playground. They pick the "LRU Cache" template. The prompt fills. They
adjust temperature to 0.2. They tap "Run Code Prompt." Code streams into the output
area with syntax highlighting. They tap "Save Snippet" and give it a title.

They go to Settings → API Keys. They add a Gemini key. The key is encrypted by
Android KeyStore, displayed masked. They toggle reveal to confirm it's correct.

They long-press a chat session → Export → save to Downloads. The file opens in Files
by Google with proper markdown formatting.

They long-press the home screen icon → "New Chat" and "Playground" shortcuts appear.
They tap "Playground" → the app opens to Playground.

They lock the phone, come back. Everything is still there — sessions, snippets, API
keys, backend choice.

They sign out from the auth dialog. Chat history persists locally. The app still works
offline. Firestore is only for optional cloud sync.

**That's the release.** Every state is real. Every error is specific. Every user action
works or fails with an explanation. Nothing is fabricated.

---

# PART 11 — ONE-LINE SUMMARY

**YSLAM v1.0 is a dual-backend (Gemini cloud + local Ollama) Android AI playground
that proves every connection state with real HTTP verification, never fabricates a
model list or "connected" status, retries transient failures with backoff, monitors
backend health every 5 seconds, encrypts API keys via Android KeyStore, and ships a
signed APK at the project root — enforced by CI gates that fail the build the moment
anyone reintroduces fake data, with 12 verified skills, 23 error codes, and a
documented release pipeline. This prompt takes the AI Studio prototype and turns it
into a production-ready app in 7 phases with evidence-based gates at every step.**
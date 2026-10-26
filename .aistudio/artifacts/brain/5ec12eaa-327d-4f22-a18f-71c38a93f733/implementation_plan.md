# YSLAM — Young Studio Localhost AI Models (Android)

A dark-mode AI code playground and streaming chat workspace for Android, engineered with a unified dual-backend architecture supporting both cloud Gemini models and local Ollama / llama.cpp / KoboldCpp inference servers.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following core product decisions have been confirmed and incorporated into this architectural specification:

- **Platform Architecture**: Native Android application built with Jetpack Compose and Material 3, optimized for both vertical handheld devices and wide/tablet form factors.
- **Backend Strategy**: Dual-backend abstraction (`AiBackend`) with `GeminiBackend` (utilizing Firebase AI Logic and `gemini-3.8-flash`) and `LocalBackend` (connecting via HTTP to configurable local inference endpoints such as Ollama at `http://10.0.2.2:11434` on emulators or LAN IPs on physical devices).
- **Workspace Navigation**: Tabbed workspace layout with three primary surfaces: **Chat** (multi-turn streaming assistant with code extraction), **Playground** (code prompt workbench with parameter sliders and execution mockups), and **Model Inspector / Settings** (backend toggle, endpoint URL config, connection tester, and token telemetry).

---

## 1. Overview & Core Concept

- **What It Does**: YSLAM provides developers, AI researchers, and students with a portable developer-grade AI environment on Android. Users can chat with frontier models, iterate on code snippets, test system prompts, tweak inference hyperparameters (temperature, top-p), and switch seamlessly between cloud-hosted Gemini and locally running LLM engines (Ollama, llama.cpp, KoboldCpp).
- **Target Audience**: Mobile developers, prompt engineers, and self-hosters wanting a unified client for local and cloud models.
- **Key Value**: Zero lock-in between cloud and local AI; zero-latency token streaming; full local persistence of prompt engineering snippets and chat history.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **First-Run & Backend Selection**:
   - The user opens YSLAM to an ambient dark IDE theme.
   - A top status bar indicates the active backend (`[Gemini Cloud • gemini-3.8-flash]` or `[Localhost Ollama • llama3]`) with real-time ping/health status.
   - If Localhost is selected, the user can test the connection to `http://10.0.2.2:11434` (or custom LAN address) with a single tap.

2. **Multi-Turn Chat Workspace**:
   - User enters prompts or code-generation tasks.
   - AI responses stream token-by-token with smooth cursor animation.
   - Code blocks are detected, highlighted, formatted with line numbers, and accompanied by a one-tap "Copy Code" button and "Send to Playground" action.
   - Users can create new chat threads, rename sessions, and clear history.

3. **Code Playground & Workbench**:
   - Multi-line system prompt input and user task prompt.
   - Preset coding templates: *Algorithm Optimization*, *Unit Test Generator*, *Regex & Parsing*, *API Client Scaffolder*, and *Refactor to Idiomatic Kotlin*.
   - Hyperparameter dock: Temperature (0.0 to 2.0), Top-P (0.1 to 1.0), Max Output Tokens slider.
   - Run button triggers generation with live output display and latency / token metrics.
   - Snippet manager: save, tag, and recall code experiments into Room local storage.

4. **Model Inspector & Configuration**:
   - Switch active backend: Google Gemini vs. Localhost (Ollama / llama.cpp / KoboldCpp).
   - Localhost Host Address input (preset quick buttons for Android Emulator `10.0.2.2:11434`, Device Host `localhost:11434`, and custom WiFi LAN IP `192.168.x.x`).
   - "Test Connection" button providing ping latency, HTTP status, and available local model tags.
   - Gemini Model selection: `gemini-3.8-flash` (default) and `gemini-3.5-flash-lite`.

### Visual Identity & Theme

- **Aesthetic Direction**: High-tech Cyber-Terminal / Minimalist IDE Dark Mode.
- **Color Palette**:
  - Background Base: `#0B0F17` (Deep Obsidian Void)
  - Surface Container: `#121826` (Subtle Slate Blue)
  - Surface Container High: `#1A2333` (Card and panel backgrounds)
  - Accent Primary: `#00F0FF` (Electric Cyan for primary actions and active tabs)
  - Accent Secondary: `#A855F7` (Vibrant Purple for model badges and tokens)
  - Code Green: `#22C55E` (Syntax strings and success ping indicators)
  - Warning/Local Amber: `#F59E0B` (Localhost status chip)
- **Typography**:
  - Monospaced typography for code blocks, token counts, parameters, and model badges.
  - Clean sans-serif hierarchy for chat bubbles and configuration labels.
- **Component Styling**:
  - Crisp borders (`1.dp` solid with subtle alpha) around cards and code editors.
  - Glowing status pills showing latency and model type.
  - Floating bottom navigation with haptic/ripple feedback.

---

## 3. Key Product Decisions & Trade-Offs

### Decision 1: Unified `AiBackend` Interface Abstraction
- **Chosen Approach**: Define a clean Kotlin interface `AiBackend` implemented by `GeminiBackend` (using Firebase AI Logic SDK) and `LocalBackend` (using OkHttp / Moshi client for Ollama API format).
- **Why**: Allows instant switching in the UI with zero code duplication; exactly satisfies the user's architectural spec where the UI remains backend-agnostic.
- **Alternatives Considered**: Direct tightly coupled API calls scattered in ViewModels. Rejected for poor maintainability.

### Decision 2: Local Persistence via Android Room
- **Chosen Approach**: Use Room database to store chat sessions, message histories, playground snippets, and server configuration settings.
- **Why**: Since this is a developer playground and local AI workspace, privacy and device-local durability are paramount. Room allows full offline functionality, fast queries, and zero sign-in barrier.
- **Alternatives Considered**: In-memory state only (lost on process kill) or SharedPreferences (unsuitable for message history and structured snippets).

### Decision 3: Localhost Networking in Android Emulator
- **Chosen Approach**: Provide configurable endpoint settings with `http://10.0.2.2:11434` as the default emulator host address, along with clear options for custom LAN IPs (`http://192.168.x.x:11434`) and localhost.
- **Why**: Android emulators route `localhost` to the internal Android Linux loopback, whereas `10.0.2.2` reaches the host development machine where Ollama / llama.cpp typically runs. Providing both a smart default and custom input prevents connection failures.

---

## 4. Technical Architecture & Data Strategy

### System Architecture Diagram

```
┌────────────────────────────────────────────────────────────────────────┐
│                        YSLAM Compose UI Layer                          │
│                                                                        │
│   ┌─────────────────────┐  ┌─────────────────────┐  ┌───────────────┐  │
│   │   ChatScreen        │  │  PlaygroundScreen   │  │ SettingsView  │  │
│   │  - Streaming Chat   │  │  - Code Prompt Dock │  │ - Backend Sw. │  │
│   │  - Syntax Highl't   │  │  - Sliders (Temp/P) │  │ - Host Ping   │  │
│   │  - Session History  │  │  - Templates / Run  │  │ - Model List  │  │
│   └──────────┬──────────┘  └──────────┬──────────┘  └───────┬───────┘  │
│              │                        │                     │          │
│              └────────────────────────┼─────────────────────┘          │
│                                       ▼                                │
│                     YslamViewModel (Shared UI State)                   │
└───────────────────────────────────────┬────────────────────────────────┘
                                        │
                    ┌───────────────────┴───────────────────┐
                    ▼                                       ▼
       ┌─────────────────────────┐             ┌─────────────────────────┐
       │     Room Local Store    │             │   AiBackend Interface   │
       │  - ChatSessionEntity    │             │  - generateStream()     │
       │  - ChatMessageEntity    │             │  - checkHealth()        │
       │  - SavedSnippetEntity   │             │  - listModels()         │
       │  - AppSettingsEntity    │             └───────────┬─────────────┘
       └─────────────────────────┘                         │
                                    ┌──────────────────────┴──────────────────────┐
                                    ▼                                             ▼
                      ┌───────────────────────────┐                 ┌───────────────────────────┐
                      │       GeminiBackend       │                 │       LocalBackend        │
                      │  - Firebase AI Logic      │                 │  - OkHttp / Moshi Client  │
                      │  - gemini-3.8-flash       │                 │  - Ollama /api/generate   │
                      │  - Cloud Attestation      │                 │  - Localhost (10.0.2.2)   │
                      └───────────────────────────┘                 └───────────────────────────┘
```

### Data Entities & Schema

```kotlin
// Chat Session
@Entity(tableName = "chat_sessions")
data class ChatSession(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long,
    val backendType: String, // "GEMINI" or "LOCAL"
    val modelName: String
)

// Chat Message
@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey val id: String,
    val sessionId: String,
    val sender: String, // "USER" or "MODEL"
    val content: String,
    val timestamp: Long,
    val isStreaming: Boolean = false
)

// Saved Playground Snippet
@Entity(tableName = "playground_snippets")
data class PlaygroundSnippet(
    @PrimaryKey val id: String,
    val title: String,
    val language: String,
    val systemPrompt: String,
    val userPrompt: String,
    val generatedCode: String,
    val temperature: Float,
    val topP: Float,
    val modelUsed: String,
    val createdAt: Long
)
```

### Component Breakdown & Execution Plan

1. **Backend Integration & App Check**:
   - Implement `configureAppCheck` in `MainActivity` with launch intent extra handling.
   - Create `AiBackend` contract and model definitions (`AiGenerationRequest`, `AiChunk`, `HealthStatus`).
   - Implement `GeminiBackend` with `Firebase.ai.generativeModel("gemini-3.8-flash")` and streaming flow.
   - Implement `LocalBackend` with OkHttp streaming client connecting to Ollama `/api/chat` or `/api/generate` with informative connection diagnostics.
2. **Local Persistence (Room)**:
   - Setup `YslamDatabase`, DAOs for sessions, messages, and playground snippets.
   - Data repository providing Flows to the ViewModel.
3. **UI & Design Polish**:
   - Dark IDE theme styling in `Color.kt` and `Theme.kt`.
   - Reusable `CodeBlock` component with syntax highlighting, line numbering, and copy functionality.
   - `ChatScreen` with multi-turn messages, auto-scroll, and markdown formatting.
   - `PlaygroundScreen` with system prompt editor, code prompt templates, hyperparameter controls, and generation runner.
   - `SettingsScreen` with endpoint diagnostics, ping tests, backend selector, and default endpoint quick-switches.
   - Sync `metadata.json` ("YSLAM") and `strings.xml`.

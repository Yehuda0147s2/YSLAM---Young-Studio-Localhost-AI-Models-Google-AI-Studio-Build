# YSLAM

YSLAM is a Jetpack Compose Android app for dual-backend AI coding workflows:

- Google Gemini via Firebase AI Logic
- Local Ollama models over HTTP on the local network

It includes chat, playground generation, snippets, settings, encrypted API key storage, and backend health checks.

## Prerequisites

- Android Studio or Android SDK with Android 36 platform support
- Java 17
- An Ollama host for local inference
- A Firebase project if using Gemini cloud

## Local model setup

For Android Emulator use:

- `http://10.0.2.2:11434`

For a physical device, use a stable LAN host IP instead of `localhost`:

- `http://192.168.1.X:11434`

Then pull a model on the host machine:

```bash
ollama pull qwen2.5-coder:1.5b
```

## Release signing

Generate a release keystore and build the APK:

```bash
bash scripts/generate-keystore.sh
export KEYSTORE_PATH=$(pwd)/keystore/yslam-release.jks
export STORE_PASSWORD=...
export KEY_PASSWORD=...
bash scripts/build-release.sh
```

The script writes the APK to the project root as `yslam-1.0.0-release.apk`. Set
`VERSION_NAME` and `VERSION_CODE` before running it to override the APK metadata as well as the
output filename.

## CI

The repository includes a GitHub Actions workflow in `.github/workflows/ci.yml` to compile the app, run unit tests, and validate basic invariants.

## Notes

- Local backend health is verified with live HTTP checks and real inference, not fabricated status.
- API keys are stored via Android KeyStore protections and are never written to plain preferences.
- A model is only treated as connected after the server responds and a real inference succeeds.
- Gemini model choices are curated options, not live discovery; availability depends on the Firebase project.

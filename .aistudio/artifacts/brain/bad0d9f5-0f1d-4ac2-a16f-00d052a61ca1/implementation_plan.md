# Implementation Plan: Fix HTTP 400, Add Transcription & Summary Retry, and Resolve GitHub App Connection

## 1. Problem Analysis & Root Cause

### A. HTTP 400 (Invalid API key or Bad Request)
- **Key Formatting Artifacts**: When users copy API keys from Google AI Studio or `.env` files, inputs frequently contain artifacts like `GEMINI_API_KEY=`, quotation marks (`"`, `'`), or trailing newline/carriage-return characters (`\r\n`). In Retrofit, passing this to `@Query("key")` results in `key=GEMINI_API_KEY=AIzaSy...`, which Google's server rejects with `HTTP 400: API key not valid`.
- **Payload Strictness**: Certain Gemini models and API endpoints reject minimal payloads if optional `generationConfig` values contain floats (`0.0f`) that don't match strict schema expectations.
- **Generic Error Masking**: `testConnection` previously caught `HttpException` without extracting the detailed message from `e.response()?.errorBody()?.string()`, masking Google's exact diagnostic reason.

### B. Missing Retry Options for Transcription & AI Executive Summary
- In `NoteDetailScreen`, if speech-to-text was interrupted or produced an empty transcript, there was no direct "Retry Transcription" button to re-process the saved audio file through Gemini audio recognition.
- If the AI summary failed (due to missing or invalid key during initial recording), users could not easily trigger a full re-analysis directly from the summary card once they configured their key.

### C. GitHub App Installation Issue
- When attempting to push to GitHub from the AI Studio UI, the error `The AI Studio GitHub App is not installed on your personal GitHub account` occurs because the Google AI Studio GitHub App needs OAuth installation and repository access permissions on your GitHub user account.
- If a repository with the same name (`echonotes`) already exists, permission must be granted to access that existing repository, or the project can be exported as a ZIP and pushed via the Git CLI.

---

## 2. Proposed Changes

### 1. Robust API Key Sanitization & Diagnostic Reporting (`data/ai/GeminiAiClient.kt`)
- Implement `sanitizeApiKey(raw: String)`:
  - Automatically strips prefixes: `GEMINI_API_KEY=`, `API_KEY=`, `export `, `key:`, etc.
  - Strips surrounding quotes (`"`, `'`), trailing whitespace, and control characters (`\r`, `\n`, `\t`).
  - Validates key structure (ensures standard Google AI Studio format starting with `AIzaSy`).
- Parse detailed error messages from `e.response()?.errorBody()?.string()` on HTTP 400/403/503:
  - If Google returns `"API key not valid. Please pass a valid API key."`, display the exact message with helpful guidance.
- Simplify the connection test ping payload to clean minimal standard structure (`{"contents": [{"parts": [{"text": "Hello"}]}]}`) to prevent schema validation rejections.

### 2. Retry Transcription & AI Summary (`ui/detail/NoteDetailViewModel.kt` & `ui/detail/NoteDetailScreen.kt`)
- **NoteDetailViewModel**:
  - Add `retryAudioTranscription()`: Loads the existing audio recording file, sends it to `aiRepository.transcribeAudioFallback(audioFile)`, updates the note transcript, and automatically re-runs AI executive summary extraction.
  - Add `isTranscribingAudio` state to `NoteDetailUiState` with progress feedback.
  - Ensure `regenerateAiAnalysis()` updates executive summary, key takeaways, and action items in Room DB and provides feedback.
- **NoteDetailScreen**:
  - Add a **"Retry Transcription"** button with audio wave / reload icon on the Transcript card when empty or when requested.
  - Add a prominent **"Regenerate AI Summary"** action button in the Executive Summary card header and in the TopAppBar menu with animated spinning loader.
  - Add visual success snackbars when re-transcription or re-summarization succeeds.

### 3. In-App Key Dialog Improvements (`ui/settings/ApiKeySettingsDialog.kt`)
- Automatically apply `sanitizeApiKey` in real time as the user types or pastes.
- If a pasted string contains `GEMINI_API_KEY=`, auto-strip it on paste with an indicator message: `"Cleaned key format automatically"`.

---

## 3. Files to Modify

| File | Proposed Changes |
| :--- | :--- |
| `data/ai/GeminiAiClient.kt` | Add `sanitizeApiKey`, extract error body in `HttpException`, streamline ping payload. |
| `data/local/preferences/UserPreferencesManager.kt` | Ensure keys saved via preferences manager are run through `sanitizeApiKey`. |
| `ui/detail/NoteDetailViewModel.kt` | Add `retryAudioTranscription()`, track `isTranscribingAudio` state, and ensure auto-save on retry. |
| `ui/detail/NoteDetailScreen.kt` | Add interactive "Retry Transcription" and "Regenerate AI Summary" buttons with loading indicators and feedback. |
| `ui/settings/ApiKeySettingsDialog.kt` | Auto-sanitize input on paste and display detailed error diagnostics. |

---

## 4. Verification Plan
- Compile the app using `compile_applet`.
- Verify key sanitization correctly extracts clean API keys from inputs like `GEMINI_API_KEY="AIzaSy..."`.
- Test that connection test displays clear, actionable feedback if a key is invalid.
- Verify that tapping "Retry Transcription" re-processes the audio file into transcript text and triggers an updated summary.
- Verify that tapping "Regenerate AI Summary" fetches and displays updated takeaways and action items.

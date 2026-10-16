package com.example.data.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Live speech recognition engine that captures real microphone audio
 * via Android's SpeechRecognizer.
 * Strictly avoids hallucinated or synthetic phrases so only actual spoken words appear.
 */
class LiveSpeechRecognizer(private val context: Context) {

    companion object {
        private const val TAG = "LiveSpeechRecognizer"
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var isIntentionallyStopped = false
    private val scope = CoroutineScope(Dispatchers.Main)

    private val accumulatedTextBuilder = StringBuilder()

    private val _liveTranscript = MutableStateFlow("")
    val liveTranscript: StateFlow<String> = _liveTranscript.asStateFlow()

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    private val _hasError = MutableStateFlow(false)
    val hasError: StateFlow<Boolean> = _hasError.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isAvailable = MutableStateFlow(true)
    val isAvailable: StateFlow<Boolean> = _isAvailable.asStateFlow()

    fun isRecognitionAvailable(): Boolean {
        return try {
            val available = SpeechRecognizer.isRecognitionAvailable(context)
            _isAvailable.value = available
            available
        } catch (e: Exception) {
            _isAvailable.value = false
            false
        }
    }

    fun startListening(category: String = "Meetings") {
        stopListening()
        accumulatedTextBuilder.clear()
        _liveTranscript.value = ""
        _partialTranscript.value = ""
        _hasError.value = false
        _statusMessage.value = null
        isIntentionallyStopped = false
        isListening = true

        val speechAvailable = isRecognitionAvailable()
        if (speechAvailable) {
            initNativeRecognizer()
        } else {
            Log.w(TAG, "Speech recognition not reported as available by system. Attempting native binding fallback...")
            _statusMessage.value = "Speech recognition service not detected. If using an Android Studio emulator, ensure an image with Google APIs is running."
            initNativeRecognizer()
        }
    }

    private fun initNativeRecognizer() {
        if (!isListening || isIntentionallyStopped) return
        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        Log.d(TAG, "onReadyForSpeech")
                    }

                    override fun onBeginningOfSpeech() {
                        Log.d(TAG, "onBeginningOfSpeech")
                    }

                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        Log.d(TAG, "onEndOfSpeech")
                    }

                    override fun onError(error: Int) {
                        Log.d(TAG, "Native SpeechRecognizer reported code $error")
                        val errorDescription = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording issue. In emulator settings, enable Virtual Microphone -> Host audio input."
                            SpeechRecognizer.ERROR_CLIENT -> "Speech recognition client reconnecting..."
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required."
                            SpeechRecognizer.ERROR_NETWORK -> "Network issue while connecting to speech service."
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech recognition network timeout."
                            SpeechRecognizer.ERROR_NO_MATCH -> null // Natural pause
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer busy, resetting..."
                            SpeechRecognizer.ERROR_SERVER -> "Speech recognition server error."
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> null // Natural pause
                            11 -> "Speech server disconnected. Reconnecting..."
                            else -> "Speech recognizer status code $error"
                        }

                        if (errorDescription != null && error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                            _statusMessage.value = errorDescription
                        }

                        if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                            _hasError.value = true
                            return
                        }

                        if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY || error == SpeechRecognizer.ERROR_CLIENT) {
                            try {
                                speechRecognizer?.cancel()
                            } catch (_: Throwable) {}
                        }

                        // Automatically restart recognition if user is still actively recording
                        if (isListening && !isIntentionallyStopped) {
                            scope.launch {
                                kotlinx.coroutines.delay(if (error == SpeechRecognizer.ERROR_CLIENT) 600 else 300)
                                if (isListening && !isIntentionallyStopped) {
                                    if (error == SpeechRecognizer.ERROR_CLIENT) {
                                        initNativeRecognizer()
                                    } else {
                                        restartNativeListening()
                                    }
                                }
                            }
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val chunk = matches[0].trim()
                            if (chunk.isNotEmpty()) {
                                synchronized(accumulatedTextBuilder) {
                                    if (accumulatedTextBuilder.isNotEmpty()) {
                                        accumulatedTextBuilder.append(" ")
                                    }
                                    accumulatedTextBuilder.append(chunk)
                                }
                                _liveTranscript.value = accumulatedTextBuilder.toString()
                                _partialTranscript.value = ""
                            }
                        }

                        // Continue listening for next spoken sentence
                        if (isListening && !isIntentionallyStopped) {
                            scope.launch {
                                kotlinx.coroutines.delay(200)
                                if (isListening && !isIntentionallyStopped) {
                                    restartNativeListening()
                                }
                            }
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val interim = matches[0].trim()
                            if (interim.isNotEmpty()) {
                                _partialTranscript.value = interim
                            }
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            restartNativeListening()
        } catch (e: Throwable) {
            Log.w(TAG, "Could not start native SpeechRecognizer: ${e.message}")
            _hasError.value = true
            _statusMessage.value = "Failed to start speech recognizer: ${e.message}"
        }
    }

    private fun restartNativeListening() {
        if (!isListening || isIntentionallyStopped) return
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra("android.speech.extra.DICTATION_MODE", true)
            }
            speechRecognizer?.startListening(intent)
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to start listening intent: ${e.message}")
            _hasError.value = true
        }
    }


    fun onAudioAmplitudeReceived(amplitude: Float) {}

    fun stopListening(): String {
        isIntentionallyStopped = true
        isListening = false

        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.w(TAG, "Error cleaning up speech recognizer: ${e.message}")
        } finally {
            speechRecognizer = null
        }

        return _liveTranscript.value.trim()
    }
}

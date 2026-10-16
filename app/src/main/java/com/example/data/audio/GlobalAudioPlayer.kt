package com.example.data.audio

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

/**
 * Singleton Audio Player ensuring strictly single-instance audio playback across the entire app.
 * Automatically halts any active audio playback when switching screens, starting recordings,
 * or leaving note views.
 */
object GlobalAudioPlayer {
    private const val TAG = "GlobalAudioPlayer"

    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _playerState = MutableStateFlow(AudioPlayerState())
    val playerState: StateFlow<AudioPlayerState> = _playerState.asStateFlow()

    fun loadAudio(context: Context, file: File) {
        if (!file.exists()) {
            stopAndRelease()
            return
        }

        // If the same file is already loaded and ready, do not re-create
        if (_playerState.value.currentFilePath == file.absolutePath && mediaPlayer != null) {
            return
        }

        stopAndRelease()

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context.applicationContext, Uri.fromFile(file))
                setOnPreparedListener { mp ->
                    val duration = mp.duration.toLong().coerceAtLeast(0L)
                    _playerState.value = AudioPlayerState(
                        isPlaying = false,
                        currentPositionMs = 0L,
                        durationMs = duration,
                        isReady = true,
                        currentFilePath = file.absolutePath
                    )
                }
                setOnCompletionListener {
                    _playerState.value = _playerState.value.copy(
                        isPlaying = false,
                        currentPositionMs = 0L
                    )
                    progressJob?.cancel()
                }
                setOnErrorListener { _, what, extra ->
                    Log.w(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    stopAndRelease()
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load audio: ${e.message}", e)
            stopAndRelease()
        }
    }

    fun play() {
        mediaPlayer?.let { player ->
            try {
                if (!player.isPlaying) {
                    player.start()
                    _playerState.value = _playerState.value.copy(isPlaying = true)
                    startProgressTracker()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to play: ${e.message}", e)
            }
        }
    }

    fun pause() {
        mediaPlayer?.let { player ->
            try {
                if (player.isPlaying) {
                    player.pause()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to pause: ${e.message}", e)
            } finally {
                _playerState.value = _playerState.value.copy(isPlaying = false)
                progressJob?.cancel()
            }
        }
    }

    fun togglePlayPause() {
        if (_playerState.value.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.let { player ->
            try {
                player.seekTo(positionMs.toInt())
                _playerState.value = _playerState.value.copy(currentPositionMs = positionMs)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to seek: ${e.message}", e)
            }
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && mediaPlayer?.isPlaying == true) {
                try {
                    val current = mediaPlayer?.currentPosition?.toLong() ?: 0L
                    _playerState.value = _playerState.value.copy(currentPositionMs = current)
                } catch (e: Exception) {
                    break
                }
                delay(80)
            }
        }
    }

    /**
     * Halts all audio playback immediately and releases system audio resources.
     * Guaranteed to silence any running audio.
     */
    fun stopAndRelease() {
        progressJob?.cancel()
        progressJob = null
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.reset()
                player.release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping player: ${e.message}")
        } finally {
            mediaPlayer = null
            _playerState.value = AudioPlayerState()
        }
    }
}

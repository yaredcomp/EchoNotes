package com.example.data.audio

import android.content.Context
import kotlinx.coroutines.flow.StateFlow
import java.io.File

data class AudioPlayerState(
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isReady: Boolean = false,
    val currentFilePath: String? = null
)

/**
 * Adapter over [GlobalAudioPlayer] for backward compatibility and unified lifecycle management.
 */
class AudioPlayerManager(private val context: Context) {

    val playerState: StateFlow<AudioPlayerState> = GlobalAudioPlayer.playerState

    fun loadAudio(file: File) {
        GlobalAudioPlayer.loadAudio(context, file)
    }

    fun play() {
        GlobalAudioPlayer.play()
    }

    fun pause() {
        GlobalAudioPlayer.pause()
    }

    fun togglePlayPause() {
        GlobalAudioPlayer.togglePlayPause()
    }

    fun seekTo(positionMs: Long) {
        GlobalAudioPlayer.seekTo(positionMs)
    }

    fun release() {
        GlobalAudioPlayer.stopAndRelease()
    }
}

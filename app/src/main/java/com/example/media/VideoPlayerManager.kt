package com.example.media

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class VideoPlayerManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) {
    private var exoPlayer: ExoPlayer? = null
    private var positionPollJob: Job? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private var trimStartMs: Long = 0L
    private var trimEndMs: Long = 0L
    private var isLoopingTrim: Boolean = false

    fun getPlayer(): ExoPlayer {
        if (exoPlayer == null) {
            val player = ExoPlayer.Builder(context)
                .setSeekParameters(SeekParameters.EXACT)
                .build()

            player.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    _isPlaying.value = playing
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_BUFFERING -> _isBuffering.value = true
                        Player.STATE_READY -> {
                            _isBuffering.value = false
                            val dur = player.duration
                            if (dur > 0) {
                                _durationMs.value = dur
                            }
                        }
                        Player.STATE_ENDED -> {
                            _isPlaying.value = false
                            if (isLoopingTrim && trimStartMs < trimEndMs) {
                                player.seekTo(trimStartMs)
                                player.play()
                            } else {
                                player.seekTo(0)
                            }
                        }
                        else -> _isBuffering.value = false
                    }
                }
            })
            exoPlayer = player
            startPositionPolling()
        }
        return exoPlayer!!
    }

    fun loadVideo(uri: Uri, startFromMs: Long = 0L) {
        val player = getPlayer()
        player.stop()
        player.clearMediaItems()
        val mediaItem = MediaItem.fromUri(uri)
        player.setMediaItem(mediaItem)
        player.prepare()
        if (startFromMs > 0) {
            player.seekTo(startFromMs)
        }
    }

    fun setTrimRange(startMs: Long, endMs: Long, enableLoop: Boolean = true) {
        trimStartMs = startMs
        trimEndMs = endMs
        isLoopingTrim = enableLoop
    }

    fun play() {
        val player = exoPlayer ?: return
        if (trimEndMs > 0 && player.currentPosition >= trimEndMs) {
            player.seekTo(trimStartMs)
        }
        player.play()
    }

    fun pause() {
        exoPlayer?.pause()
    }

    fun togglePlayPause() {
        val player = exoPlayer ?: return
        if (player.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
        _currentPositionMs.value = positionMs
    }

    fun fastSeek(offsetMs: Long) {
        val player = exoPlayer ?: return
        val newPos = (player.currentPosition + offsetMs).coerceIn(0L, _durationMs.value)
        seekTo(newPos)
    }

    private fun startPositionPolling() {
        positionPollJob?.cancel()
        positionPollJob = coroutineScope.launch(Dispatchers.Main) {
            while (isActive) {
                exoPlayer?.let { player ->
                    val pos = player.currentPosition
                    _currentPositionMs.value = pos
                    val dur = player.duration
                    if (dur > 0 && _durationMs.value != dur) {
                        _durationMs.value = dur
                    }

                    // Check trim bounds if looping
                    if (isLoopingTrim && trimEndMs > trimStartMs && pos >= trimEndMs) {
                        player.seekTo(trimStartMs)
                    }
                }
                delay(33) // ~30 fps update
            }
        }
    }

    fun release() {
        positionPollJob?.cancel()
        positionPollJob = null
        exoPlayer?.release()
        exoPlayer = null
    }
}

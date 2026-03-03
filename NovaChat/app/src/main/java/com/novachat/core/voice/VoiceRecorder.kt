package com.novachat.core.voice

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

enum class RecordingState { IDLE, RECORDING, PLAYING }

@Singleton
class VoiceRecorder @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private var currentFile: File? = null

    private val _state = MutableStateFlow(RecordingState.IDLE)
    val state: StateFlow<RecordingState> = _state.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private var startTime = 0L

    fun startRecording(): File? {
        val file = File(context.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
        currentFile = file

        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(44100)
            setAudioEncodingBitRate(128000)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }

        startTime = System.currentTimeMillis()
        _state.value = RecordingState.RECORDING
        return file
    }

    fun stopRecording(): Pair<File, Long>? {
        val duration = System.currentTimeMillis() - startTime
        _durationMs.value = duration
        recorder?.apply {
            stop()
            release()
        }
        recorder = null
        _state.value = RecordingState.IDLE
        return currentFile?.let { it to duration }
    }

    fun cancelRecording() {
        recorder?.apply {
            stop()
            release()
        }
        recorder = null
        currentFile?.delete()
        currentFile = null
        _state.value = RecordingState.IDLE
    }

    fun playVoice(file: File, onComplete: () -> Unit = {}) {
        player?.release()
        player = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            prepare()
            start()
            setOnCompletionListener {
                _state.value = RecordingState.IDLE
                onComplete()
            }
        }
        _state.value = RecordingState.PLAYING
    }

    fun stopPlaying() {
        player?.apply {
            stop()
            release()
        }
        player = null
        _state.value = RecordingState.IDLE
    }

    fun release() {
        recorder?.release()
        player?.release()
        recorder = null
        player = null
        _state.value = RecordingState.IDLE
    }
}

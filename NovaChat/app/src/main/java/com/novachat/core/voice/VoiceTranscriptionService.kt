package com.novachat.core.voice

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.novachat.core.database.dao.VoiceTranscriptionDao
import com.novachat.core.database.entity.VoiceTranscriptionEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class VoiceTranscriptionService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val transcriptionDao: VoiceTranscriptionDao
) {

    suspend fun getTranscription(messageId: Long): String? {
        return transcriptionDao.getTranscription(messageId)?.transcription
    }

    suspend fun transcribe(messageId: Long, audioUri: String): Result<String> = withContext(Dispatchers.Main) {
        val cached = transcriptionDao.getTranscription(messageId)
        if (cached != null) return@withContext Result.success(cached.transcription)

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            return@withContext Result.failure(Exception("Speech recognition not available on this device"))
        }

        try {
            val transcription = recognizeSpeech(Uri.parse(audioUri))
            if (transcription.isNotBlank()) {
                transcriptionDao.insertTranscription(
                    VoiceTranscriptionEntity(
                        messageId = messageId,
                        transcription = transcription
                    )
                )
            }
            Result.success(transcription)
        } catch (e: Exception) {
            Log.e("VoiceTranscription", "Transcription failed", e)
            Result.failure(e)
        }
    }

    private suspend fun recognizeSpeech(audioUri: Uri): String = suspendCancellableCoroutine { cont ->
        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val result = matches?.firstOrNull() ?: ""
                recognizer.destroy()
                if (cont.isActive) cont.resume(result)
            }

            override fun onError(error: Int) {
                recognizer.destroy()
                if (cont.isActive) cont.resume("")
            }

            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra("android.speech.extra.AUDIO_SOURCE", audioUri)
        }

        cont.invokeOnCancellation { recognizer.destroy() }
        recognizer.startListening(intent)
    }

    suspend fun getDuration(audioUri: String): Long = withContext(Dispatchers.IO) {
        try {
            val player = MediaPlayer()
            player.setDataSource(context, Uri.parse(audioUri))
            player.prepare()
            val duration = player.duration.toLong()
            player.release()
            duration
        } catch (e: Exception) {
            0L
        }
    }
}
